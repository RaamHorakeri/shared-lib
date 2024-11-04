package vars

def checkoutFromGit(String branch, String repoUrl, String credentialsId) {
    echo "Checking out branch ${branch} from ${repoUrl}"
    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${branch}"]],
        userRemoteConfigs: [[url: repoUrl, credentialsId: credentialsId]]
    ])
}

def buildDockerImage(String imageName) {
    try {
        echo "Building Docker image ${imageName}:latest"
        bat "docker build -t ${imageName}:latest ."
    } catch (Exception e) {
        echo "Failed to build Docker image: ${e.message}"
        throw e
    }
}

def removeContainer(String containerName) {
    try {
        echo "Removing existing container ${containerName} if it exists"
        bat "docker rm -f ${containerName} || exit 0"
    } catch (Exception e) {
        echo "Failed to remove container: ${e.message}"
        throw e
    }
}

def deployWithDockerCompose(Map<String, String> envVars) {
    try {
        echo "Deploying with Docker Compose"
        
        // Check if envVars map is empty
        if (envVars.isEmpty()) {
            // No environment variables, just run docker-compose up -d
            sh "docker-compose up -d"
        } else {
            // Construct the environment variable string
            def envVarString = envVars.collect { key, value -> "${key}=${value}" }.join(' ')
            sh "${envVarString} docker-compose up -d"
        }
    } catch (Exception e) {
        echo "Deployment failed: ${e.message}"
        throw e
    }
}


def deleteUnusedDockerImages() {
    try {
        echo "Deleting unused Docker images"
        bat "docker image prune -f"
    } catch (Exception e) {
        echo "Failed to delete unused images: ${e.message}"
        throw e
    }
}
