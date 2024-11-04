// Define your shared library functions
def checkoutFromGit(String branch) {
    // Perform checkout
    git branch: branch, credentialsId: GIT_CREDENTIALS_ID, url: REPO_URL
}

def buildDockerImage(String imageName) {
    // Build the Docker image using the specified name
    executeCommand("docker build -t ${imageName}:latest .")
}

def removeContainer(String containerName) {
    // Remove the specified container forcefully
    executeCommand("docker rm -f ${containerName} || true")
}

def deployWithDockerCompose(String envVariables = '') {
    // Construct the command based on whether environment variables are provided
    def command = envVariables ? "${envVariables} docker-compose up -d" : "docker-compose up -d"
    executeCommand(command)
}

def deleteUnusedDockerImages() {
    // Clean up unused Docker images
    executeCommand("docker image prune -af")
}

// Helper method to execute commands conditionally based on the OS
def executeCommand(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}
