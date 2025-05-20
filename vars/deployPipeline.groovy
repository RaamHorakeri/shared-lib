// def call(String imageName, String environment, String imageTag, String branch) {
//     node {
//         def config = loadConfig()

//         if (!config) {
//             error("Configuration not loaded. Please check loadConfig() function.")
//         }

//         def serviceConfig = config.services[imageName]
//         if (!serviceConfig) {
//             error("Service configuration not found for: ${imageName}")
//         }

//         def envConfig = serviceConfig.environments[environment]
//         if (!envConfig) {
//             error("Environment configuration not found for service: ${imageName}, environment: ${environment}")
//         }

//         pipeline {
//             agent { label envConfig.agentName ?: '' }

//             stages {
//                 stage('Setup Environment Variables') {
//                     steps {
//                         script {
//                             if (envConfig.envVars) {
//                                 envConfig.envVars.each { key, value -> 
//                                     env[key] = credentials(value)
//                                 }
//                             } else {
//                                 echo "No environment variables configured."
//                             }
//                         }
//                     }
//                 }

//                 stage('Checkout') {
//                     steps {
//                         script {
//                             echo "Checking out repository: ${envConfig.repoUrl}, Branch: ${branch}"
//                             checkoutFromGit(branch, envConfig.repoUrl, envConfig.credentialsId)
//                         }
//                     }
//                 }

//                 stage('Docker Login & Build Image') {
//                     steps {
//                         script {
//                             def imageFullName = "${imageName}:${imageTag}"
//                             // echo "Logging in to Docker Hub..."
//                             // withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
//                             //     bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
//                             // }

//                             echo "Building Docker image: ${imageFullName}"
//                             // bat "docker build --no-cache -t ${imageFullName} ."
//                                // bat "docker build --pull=never -t ${imageFullName} ."
//                             // bat "docker build -t ${imageFullName} ."
//                              sh "docker build -t ${imageFullName} ."


//                         }
//                     }
//                 }

//                 stage('Deploy with Docker Compose') {
//                     steps {
//                         script {
//                             // echo "Deploying service with Docker Compose..."
//                             // def composeEnvVars = envConfig.envVars.collect { key, _ -> "set ${key}=%${key}%" }.join(' & ')
//                             // sh """
//                             // ${composeEnvVars} &
//                             // docker compose up -d --force-recreate
//                             // """
//                             def composeEnvVars = envConfig.envVars.collect { key, _ -> "export ${key}=\$${key}" }.join(' && ')
//                             sh """
//                             ${composeEnvVars} &&
//                             docker compose up -d --force-recreate
//                             """
//                         }
//                     }
//                 }

//                 stage('Cleanup Unused Docker Images') {
//                     steps {
//                         script {
//                             echo "Cleaning up unused Docker images..."
//                             sh "docker image prune -af"
//                         }
//                     }
//                 }
//             }

//             post {
//                 always {
//                     echo 'Deployment complete.'
//                 }
//                 success {
//                     echo 'Deployment succeeded.'
//                 }
//                 failure {
//                     echo 'Deployment failed!'
//                 }
//             }
//         }
//     }
// }


def call(String imageName, String environment, String imageTag, String branch) {
    node {
        def config = loadConfig()
        def envConfig = config.services[imageName]?.environments[environment]

        if (!envConfig) {
            error("Configuration not found for service: ${imageName}, environment: ${environment}")
        }

        def imageFullName = "${imageName}:${imageTag}"

        pipeline {
            agent { label envConfig.agentName ?: '' }

            stages {
                stage('Setup Environment Variables') {
                    steps {
                        script {
                            echo "Setting up environment variables..."
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

                stage('Docker Login') {
                    steps {
                        script {
                            echo "Logging into Docker Hub..."

                            // ✅ Use secure credentials management in production
                            withCredentials([usernamePassword(
                                credentialsId: 'dockerhub-credentials',
                                usernameVariable: 'DOCKER_USER',
                                passwordVariable: 'DOCKER_PASS'
                            )]) {
                                bat 'docker login -u %DOCKER_USER% -p %DOCKER_PASS%'
                            }

                            // ⛔ Temporary fallback (REMOVE before pushing to repo)
                            // bat 'docker login -u raam2023 -p dckr_pat_GqMQ-qjOiVaWnwLtdQP-5m902CQ'
                        }
                    }
                }

                stage('Docker Build Image') {
                    steps {
                        script {
                            echo "Building Docker image: ${imageFullName}"
                            bat "docker build --no-cache -t ${imageFullName} ."
                        }
                    }
                }

                stage('Docker Compose Deploy') {
                    steps {
                        script {
                            echo "Generating .env file for Docker Compose..."

                            // Generate .env file content from loaded credentials
                            def envFileContent = envConfig.envVars.collect { key, _ ->
                                "${key}=${env[key]}"
                            }.join('\n')

                            // Write the .env file to the workspace
                            writeFile file: '.env', text: envFileContent

                            echo "Running Docker Compose with generated .env file..."
                            bat 'docker compose up -d --force-recreate'
                        }
                    }
                }

                stage('Docker Cleanup') {
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
