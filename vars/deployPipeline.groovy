def call(String imageName, String environment, String imageTag, String branch) {
    node {
        def config = loadConfig()
        def envConfig = config.services[imageName]?.environments[environment]

        if (!envConfig) {
            error("Configuration not found for service: ${imageName}, environment: ${environment}")
        }

        pipeline {
            agent { label envConfig.agentName ?: '' }

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
                            echo "Checking out repository: ${envConfig.repoUrl}, Branch: ${branch}"
                            checkoutFromGit(branch, envConfig.repoUrl, envConfig.credentialsId)
                        }
                    }
                }

                stage('Build Docker Image') {
                    steps {
                        script {
                            def imageFullName = "${imageName}-web:${imageTag}"
                            echo "Building Docker image: ${imageFullName}"
                            bat "docker build --no-cache -t ${imageFullName} ."
                        }
                    }
                }

                stage('Deploy with Docker Compose') {
                    steps {
                        script {
                            echo "Deploying service with Docker Compose..."
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
