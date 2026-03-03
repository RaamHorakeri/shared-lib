pipeline {
    agent { label 'temp' }

    parameters {

        // ---------------- Docker / Image ----------------
        string(name: 'IMAGE_NAME', defaultValue: 'web-calculator')
        string(name: 'IMAGE_TAG', defaultValue: 'latest')
        string(name: 'IMAGE_REGISTRY', defaultValue: 'raamcloudops')
        string(name: 'DOCKER_CREDENTIALS_ID', defaultValue: 'dockerhub-credentials')

        // ---------------- Application Source ----------------
        string(name: 'APP_REPO_URL', defaultValue: 'https://github.com/RaamHorakeri/simple-calculator.git')
        string(name: 'APP_BRANCH', defaultValue: 'main')
        string(name: 'APP_GIT_CREDENTIALS_ID', defaultValue: 'github-credentials')

        // ---------------- Helm Manifest Repo ----------------
        string(name: 'HELM_REPO_URL', defaultValue: 'https://github.com/RaamHorakeri/helm-manifest.git')
        string(name: 'HELM_BRANCH', defaultValue: 'main')
        string(name: 'HELM_GIT_CREDENTIALS_ID', defaultValue: 'github-credentials')
        string(name: 'HELM_APP_PATH', defaultValue: 'helm/webcalculator')

        // ---------------- Kubernetes ----------------
        string(name: 'APP_NAME', defaultValue: 'web-calculator')
        string(name: 'K8S_NAMESPACE', defaultValue: 'default')

        // ---------------- Hidden Git Identity ----------------
        password(name: 'GIT_COMMIT_EMAIL', defaultValue: 'jenkins@ci.local')
        password(name: 'GIT_COMMIT_NAME', defaultValue: 'Jenkins')

        // ---------------- Hidden SSH Push Settings ----------------
        password(name: 'SSH_GIT_URL', defaultValue: '')
        password(name: 'SSH_CREDENTIAL_ID', defaultValue: '')
    }

    environment {
        IMAGE_FULL = "${params.IMAGE_REGISTRY}/${params.IMAGE_NAME}:${params.IMAGE_TAG}"
        APP_SOURCE_DIR = "app-source"
        MANIFEST_DIR = "helm-manifest"
    }

    stages {

        stage('Checkout Application Source') {
            steps {
                dir("${APP_SOURCE_DIR}") {
                    git branch: "${params.APP_BRANCH}",
                        url: "${params.APP_REPO_URL}",
                        credentialsId: "${params.APP_GIT_CREDENTIALS_ID}"
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir("${APP_SOURCE_DIR}") {
                    sh "docker build -t ${IMAGE_FULL} ."
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "${params.DOCKER_CREDENTIALS_ID}",
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                    echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                    docker push ${IMAGE_FULL}
                    docker logout
                    """
                }
            }
        }

        stage('Checkout Helm Manifest Repo') {
            steps {
                dir("${MANIFEST_DIR}") {
                    git branch: "${params.HELM_BRANCH}",
                        url: "${params.HELM_REPO_URL}",
                        credentialsId: "${params.HELM_GIT_CREDENTIALS_ID}"
                }
            }
        }

        stage('Update Helm values.yaml') {
            steps {
                dir("${MANIFEST_DIR}/${params.HELM_APP_PATH}") {
                    sh """
                    sed -i 's|^\\s*repository:.*|  repository: "${params.IMAGE_REGISTRY}/${params.IMAGE_NAME}"|' values.yaml
                    sed -i 's|^\\s*tag:.*|  tag: "${params.IMAGE_TAG}"|' values.yaml
                    """
                }
            }
        }

        stage('Commit & Push Helm Changes') {
            steps {
                dir("${MANIFEST_DIR}") {
                    script {

                        if (params.SSH_GIT_URL?.trim()) {

                            // -------- SSH Push --------
                            sshagent(credentials: ["${params.SSH_CREDENTIAL_ID}"]) {
                                sh """
                                git config user.email "${params.GIT_COMMIT_EMAIL}"
                                git config user.name "${params.GIT_COMMIT_NAME}"
                                git add ${params.HELM_APP_PATH}/values.yaml
                                git commit -m "Update image to ${IMAGE_FULL}" || echo "No changes"
                                git push ${params.SSH_GIT_URL} ${params.HELM_BRANCH}
                                """
                            }

                        } else {

                            // -------- HTTPS Push --------
                            withCredentials([usernamePassword(
                                credentialsId: "${params.HELM_GIT_CREDENTIALS_ID}",
                                usernameVariable: 'GIT_USER',
                                passwordVariable: 'GIT_PASS'
                            )]) {
                                sh """
                                git config user.email "${params.GIT_COMMIT_EMAIL}"
                                git config user.name "${params.GIT_COMMIT_NAME}"
                                git add ${params.HELM_APP_PATH}/values.yaml
                                git commit -m "Update image to ${IMAGE_FULL}" || echo "No changes"
                                git push https://\$GIT_USER:\$GIT_PASS@${params.HELM_REPO_URL.replace('https://','')} ${params.HELM_BRANCH}
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Wait For Kubernetes Rollout') {
            steps {
                sh """
                kubectl rollout status deployment/${params.APP_NAME} \
                -n ${params.K8S_NAMESPACE} --timeout=300s
                """
            }
        }

        stage('Health Check Pods') {
            steps {
                sh """
                kubectl wait \
                --for=condition=ready pod \
                -l app=${params.APP_NAME} \
                -n ${params.K8S_NAMESPACE} \
                --timeout=180s
                """
            }
        }

        stage('Confirm Running Image') {
            steps {
                script {
                    def expected = IMAGE_FULL
                    timeout(time: 5, unit: 'MINUTES') {
                        waitUntil {
                            def currentImage = sh(
                                script: """
                                kubectl get deployment ${params.APP_NAME} \
                                -n ${params.K8S_NAMESPACE} \
                                -o jsonpath='{.spec.template.spec.containers[0].image}'
                                """,
                                returnStdout: true
                            ).trim()

                            echo "Current Image: ${currentImage}"
                            echo "Expected Image: ${expected}"

                            return currentImage == expected
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Deployment Successful - Verified Image ${IMAGE_FULL}"
        }
        failure {
            echo "❌ Deployment Failed"
        }
    }
}
