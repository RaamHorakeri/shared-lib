// vars/falcon.groovy
 
def checkoutFromGit(String branch, String repoUrl, String credentialsId) {

    try {

        checkout([

            $class: 'GitSCM',

            branches: [[name: "*/${branch}"]],

            userRemoteConfigs: [[url: repoUrl, credentialsId: credentialsId]]

        ])

        echo "Successfully checked out branch ${branch} from ${repoUrl}"

    } catch (Exception e) {

        error("Checkout failed: ${e.message}")

    }

}
 
def buildDockerImage(String imageName) {

    try {

        sh "docker build -t ${imageName}:latest ."

        echo "Docker image ${imageName}:latest built successfully"

    } catch (Exception e) {

        error("Building Docker image failed: ${e.message}")

    }

}
 
def removeContainer(String containerName) {

    try {

        sh "docker rm -f ${containerName}"

        echo "Existing container ${containerName} removed successfully"

    } catch (Exception e) {

        echo "No existing container found or error removing: ${e.message}"

    }

}
 
def deployWithDockerCompose() {

    try {

        sh "docker-compose up -d --build"

        echo "Deployment with Docker Compose completed successfully"

    } catch (Exception e) {

        error("Deployment with Docker Compose failed: ${e.message}")

    }

}
 
def deleteUnusedDockerImages() {

    try {

        sh "docker image prune -f"

        echo "Unused Docker images deleted successfully"

    } catch (Exception e) {

        echo "Error deleting unused images: ${e.message}"

    }

}

 
