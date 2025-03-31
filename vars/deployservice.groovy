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
            stage('Setup Environment Variables') {
                steps {
                    script {
                        env.IMAGE_NAME = "${appName}"
                        env.CONTAINER_NAME = "${appName}"
                    }
                }
            }

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
                        echo "Building new Docker image: ${env.IMAGE_NAME}"
                        sh "docker build --no-cache -t ${env.IMAGE_NAME}:latest ."
                    }
                }
            }

            stage('Deploy with Docker Compose') {
                steps {
                    script {
                        echo "Deploying with Docker Compose..."
                        def composeEnvVars = envConfig.envVars.collect { key, value -> "${key}=${value}" }.join(' \\\n')
                        sh """
                        ${composeEnvVars} \\
                        docker compose up -d --force-recreate
                        """
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

// Function to checkout code from Git
def checkoutFromGit(String branch, String repoUrl, String credentialsId) {
    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${branch}"]],
        userRemoteConfigs: [[url: repoUrl, credentialsId: credentialsId]]
    ])
}

// Function to load the configuration file
def loadLibraryConfig() {
    def scriptContent = libraryResource('config.groovy')
    return evaluate(scriptContent)
}
