// vars/deploy.groovy

def deploy() {
    pipeline {
        agent any

        stages {
            stage('Checkout') {
                steps {
                    script {
                        checkoutFromGit('staging', 'https://github.com/RaamHorakeri/public-web.git', 'git')
                    }
                }
            }

            stage('Build Docker Image') {
                steps {
                    script {
                        buildDockerImage('public-web')
                    }
                }
            }

            stage('Remove Existing Container') {
                steps {
                    script {
                        removeContainer('public-web')
                    }
                }
            }

            stage('Deploy with Docker Compose') {
                steps {
                    script {
                        deployWithDockerCompose([:]) // No environment variables needed
                    }
                }
            }

            stage('Deleting Unused Docker Images') {
                steps {
                    script {
                        deleteUnusedDockerImages()
                    }
                }
            }
        }

        post {
            success {
                echo 'Deployment successful!'
            }
            failure {
                echo 'Deployment failed!'
            }
        }
    }
}
