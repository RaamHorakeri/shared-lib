// vars/deployPipeline.groovy

def call(Map config = [:]) {
    checkoutFromGit(config.branch, config.repoUrl, config.credentialsId)
    buildDockerImage(config.imageName)
    removeContainer(config.containerName)
    deployWithDockerCompose(config.envVariables)
    deleteUnusedDockerImages()
}

def checkoutFromGit(String branch, String repoUrl, String credentialsId) {
    git branch: branch, credentialsId: credentialsId, url: repoUrl
}

def buildDockerImage(String imageName) {
    sh "docker build -t ${imageName}:latest ."
}

def removeContainer(String containerName) {
    sh "docker rm -f ${containerName} || true"
}

def deployWithDockerCompose(String envVariables = '') {
    sh "${envVariables} docker compose up -d"
}

def deleteUnusedDockerImages() {
    sh "docker image prune -af"
}
