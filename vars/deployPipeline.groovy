package vars

import groovy.json.JsonSlurper

def checkoutFromGit(String branch, String repoUrl, String credentialsId) {
    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${branch}"]],
        userRemoteConfigs: [[url: repoUrl, credentialsId: credentialsId]]
    ])
}

def buildDockerImage(String imageName) {
    sh "docker build -t ${imageName}:latest ."
}

def removeContainer(String containerName) {
    sh "docker rm -f ${containerName} || true"
}

def deployWithDockerCompose() {
    sh "docker-compose up -d"
}

def deleteUnusedDockerImages() {
    sh "docker image prune -f"
}
