pipeline {
    agent { label envConfig.agentName }

    environment {
        IMAGE_NAME = "${appName}"
        CONTAINER_NAME = "${appName}"
    }

    stages {
        stage('Setup Environment Variables') {
            steps {
                script {
                    // Dynamically load environment variables from configuration
                    envConfig.envVars.each { key, value -> 
                        env[key] = credentials(value)
                    }
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
                    echo "Building new Docker image: ${IMAGE_NAME}"
                    sh "docker build --no-cache -t ${IMAGE_NAME}:latest ."
                }
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                script {
                    echo "Deploying with Docker Compose..."
                    def composeEnvVars = envConfig.envVars.collect { key, _ -> "${key}=${env[key]}" }.join(' \\')
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
