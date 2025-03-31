def call(String serviceName, String environment, Map params = [:]) {
    def config = loadLibraryConfig()
    def serviceConfig = config.services[serviceName]
    if (!serviceConfig) {
        error "Service '${serviceName}' not found in configuration."
    }

    def envConfig = serviceConfig.environments[environment]
    if (!envConfig) {
        error "Environment '${environment}' not found for service '${serviceName}'."
    }

    // Override configuration with passed parameters
    def appName = params.get('APP_NAME', serviceName.toLowerCase())
    def repoUrl = params.get('REPO_URL', envConfig.repoUrl)
    def credentialsId = params.get('CREDENTIALS_ID', envConfig.credentialsId)
    def branch = params.get('BRANCH', envConfig.branch)

    pipeline {
        agent { label envConfig.agentName }

        stages {
            stage('Checkout') {
                steps {
                    script {
                        echo "Checking out repository: ${repoUrl}, Branch: ${branch}"
                        checkoutFromGit(branch, repoUrl, credentialsId)
                    }
                }
            }

            stage('Build Docker Image') {
                steps {
                    script {
                        echo "Building new Docker image: ${appName}"
                        sh "docker build --no-cache -t ${appName}:latest ."
                    }
                }
            }

            stage('Deploy with Docker Compose') {
                steps {
                    script {
                        echo "Deploying with Docker Compose..."
                        def composeEnvVars = envConfig.envVars.collect { key, value -> "${key}=${value}" }.join(' ')
                        sh "env ${composeEnvVars} docker compose up -d --force-recreate"
                    }
                }
            }

            stage('Cleanup Unused Docker Images') {
                steps {
                    script {
                        echo "Cleaning up unused Docker images..."
                        sh "docker image prune -af"
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

def checkoutFromGit(String branch, String repoUrl, String credentialsId) {
    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${branch}"]],
        userRemoteConfigs: [[url: repoUrl, credentialsId: credentialsId]]
    ])
}

def loadLibraryConfig() {
    return libraryResource('config.groovy').evaluate()
}
