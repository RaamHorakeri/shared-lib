def call(String service, String environment) {
    node {
        def config = loadConfig()
        def envConfig = config.services[service]?.environments[environment]

        if (!envConfig) {
            error("Configuration not found for service: ${service}, environment: ${environment}")
        }

        pipeline {
            agent { label envConfig.agentName ?: 'default' }

            stages {
                stage('Setup Environment Variables') {
                    steps {
                        script {
                            envConfig.envVars.each { key, value -> 
                                env[key] = credentials(value)
                            }
                        }
                    }
                }

                stage('Checkout') {
                    steps {
                        script {
                            echo "Checking out repository: ${envConfig.repoUrl}, Branch: ${envConfig.branch}"
                            checkoutFromGit(envConfig.branch, envConfig.repoUrl, envConfig.credentialsId)
                        }
                    }
                }

                stage('Build Docker Image') {
                    steps {
                        script {
                            echo "Building Docker image: ${service}-web"
                            bat "docker build --no-cache -t ${service}-web:latest ."
                        }
                    }
                }

                stage('Deploy with Docker Compose') {
                    steps {
                        script {
                            echo "Deploying service..."
                            def composeEnvVars = envConfig.envVars.collect { key, _ -> "set ${key}=%${key}%" }.join(' & ')
                            bat """
                            ${composeEnvVars} &
                            docker compose up -d --force-recreate
                            """
                        }
                    }
                }

                stage('Cleanup Unused Docker Images') {
                    steps {
                        script {
                            echo "Cleaning up unused Docker images..."
                            bat "docker image prune -af"
                        }
                    }
                }
            }

            post {
                always {
                    echo 'Deployment complete.'
                }
                success {
                    echo 'Deployment succeeded.'
                }
                failure {
                    echo 'Deployment failed!'
                }
            }
        }
    }
}

def loadConfig() {
    return load("${env.WORKSPACE}/resources/config.groovy")
}

return this
