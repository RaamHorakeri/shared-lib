package vars

def checkoutFromGit(String branch, String repoUrl, String credentialsId) {
    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${branch}"]],
        userRemoteConfigs: [[url: repoUrl, credentialsId: credentialsId]]
    ])
}

def buildDockerImage(String imageName) {
    // Use the Docker command directly without nohup
    sh "docker build -t ${imageName}:latest ."
}

def removeContainer(String containerName) {
    // Forcibly remove the container if it exists
    sh "docker rm -f ${containerName} || true"
}

def deployWithDockerCompose() {
    // Use docker-compose directly, make sure docker-compose is installed
    sh "docker-compose up -d"
}

def deleteUnusedDockerImages() {
    // Prune unused images
    sh "docker image prune -f"
}
