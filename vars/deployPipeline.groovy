// vars/deployPipeline.groovy

def call(Map config = [:]) {
    checkoutFromGit(config.branch)
    buildDockerImage(config.imageName)
    removeContainer(config.containerName)
    deployWithDockerCompose(config.envVariables)
    deleteUnusedDockerImages()
}

def checkoutFromGit(String branch) {
    def repoUrl = 'https://github.com/RaamHorakeri/public-web.git'
    def credentialsId = 'git'
    
    git branch: branch, credentialsId: credentialsId, url: repoUrl
}

def buildDockerImage(String imageName) {
    executeCommand("docker build -t ${imageName}:latest .")
}

def removeContainer(String containerName) {
    executeCommand("docker rm -f ${containerName} || true")
}

def deployWithDockerCompose(String envVariables = '') {
    executeCommand("${envVariables} docker-compose up -d")
}

def deleteUnusedDockerImages() {
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
