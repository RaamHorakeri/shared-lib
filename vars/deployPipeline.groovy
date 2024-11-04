def call(Map config = [:]) {
    // Debugging: Print out the received configuration for verification
    echo "Branch: ${config.branch}"
    echo "Image Name: ${config.imageName}"
    echo "Container Name: ${config.containerName}"
    echo "Environment Variables: ${config.envVariables}"

    // Checkout the code from the specified branch
    checkoutFromGit(config.branch)

    // Build the Docker image
    buildDockerImage(config.imageName)

    // Remove any existing Docker container
    removeContainer(config.containerName)

    // Deploy the application using Docker Compose
    deployWithDockerCompose(config.envVariables)

    // Clean up unused Docker images
    deleteUnusedDockerImages()
}

def checkoutFromGit(String branch) {
    def repoUrl = 'https://github.com/RaamHorakeri/public-web.git'
    def credentialsId = 'git'
    
    // Perform checkout
    git branch: branch, credentialsId: credentialsId, url: repoUrl
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
