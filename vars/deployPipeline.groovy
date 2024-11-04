// Define your shared library functions
def checkoutFromGit(String branch) {
    try {
        // Perform checkout
        git branch: branch, credentialsId: env.GIT_CREDENTIALS_ID, url: env.REPO_URL
        echo "Checked out branch: ${branch}"
    } catch (Exception e) {
        error "Failed to checkout branch: ${branch}. Error: ${e.message}"
    }
}

def buildDockerImage(String imageName) {
    try {
        // Build the Docker image using the specified name
        executeCommand("docker build -t ${imageName}:latest .")
        echo "Docker image built: ${imageName}:latest"
    } catch (Exception e) {
        error "Failed to build Docker image: ${imageName}. Error: ${e.message}"
    }
}

def removeContainer(String containerName) {
    try {
        // Remove the specified container forcefully
        executeCommand("docker rm -f ${containerName} || true")
        echo "Removed container: ${containerName}"
    } catch (Exception e) {
        error "Failed to remove container: ${containerName}. Error: ${e.message}"
    }
}

def deployWithDockerCompose(String envVariables = '') {
    try {
        // Construct the command based on whether environment variables are provided
        def command = envVariables ? "${envVariables} docker-compose up -d" : "docker-compose up -d"
        executeCommand(command)
        echo "Deployment initiated with Docker Compose."
    } catch (Exception e) {
        error "Failed to deploy with Docker Compose. Error: ${e.message}"
    }
}

def deleteUnusedDockerImages() {
    try {
        // Clean up unused Docker images
        executeCommand("docker image prune -af")
        echo "Deleted unused Docker images."
    } catch (Exception e) {
        error "Failed to delete unused Docker images. Error: ${e.message}"
    }
}

// Helper method to execute commands conditionally based on the OS
def executeCommand(String command) {
    echo "Executing command: ${command}"
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}
