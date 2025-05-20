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

        // Map to store actual secret values
        def secretsMap = [:]

        stage('Setup Environment Variables') {
            echo "Fetching secrets from Jenkins Credentials..."

            // For each env var, get its secret value using withCredentials
            envConfig.envVars.each { key, credId ->
                withCredentials([string(credentialsId: credId, variable: 'SECRET_VALUE')]) {
                    secretsMap["${key}_DEV"] = SECRET_VALUE
                }
            }
        }

        stage('Checkout') {
            echo "Checking out branch '${branch}' from repo '${envConfig.repoUrl}'"
            checkoutFromGit(branch, envConfig.repoUrl, envConfig.credentialsId)
        }

        stage('Docker Login') {
            echo "Logging into Docker Hub using Jenkins credentials..."
            withCredentials([usernamePassword(
                credentialsId: 'dockerhub-credentials',
                usernameVariable: 'DOCKER_USER',
                passwordVariable: 'DOCKER_PASS'
            )]) {
                bat """
                echo Logging in as %DOCKER_USER%
                docker login -u %DOCKER_USER% -p %DOCKER_PASS%
                """
            }
        }

        stage('Docker Build Image') {
            echo "Building Docker image: ${imageFullName}"
            bat "docker build --no-cache -t ${imageFullName} ."
        }

        stage('Prepare .env file') {
            echo "Generating .env file for Docker Compose with real secrets..."

            def envFileContent = secretsMap.collect { k, v ->
                "${k}=${v}"
            }.join('\r\n')

            writeFile file: '.env', text: envFileContent
        }

        stage('Docker Compose Deploy') {
            echo "Running docker compose up -d --force-recreate"
            bat 'docker compose up -d --force-recreate'
        }

        stage('Docker Cleanup') {
            echo "Cleaning up unused Docker images..."
            bat "docker image prune -af"
        }
    }
}
