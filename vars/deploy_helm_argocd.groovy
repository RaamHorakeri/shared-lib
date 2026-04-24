def call(String agentName, String imageRegistry, String imageName, String imageTag, String appRepo, String appBranch, String appRepoCredentialsId, String manifestRepo,
         String manifestBranch, String manifestRepoCredentialsId, String manifestCloneDir, String valuesFile, String dockerCredentialsId, String gitConfigCredentialsId, String k8sDeploymentName, String k8sNamespace) {

    node(agentName) {

        def finalTag    = imageTag?.trim() ? imageTag : env.BUILD_NUMBER
        def fullImage   = "${imageRegistry}/${imageName}:${finalTag}"
        def buildFailed = false

        try {

            stage('Checkout Application Repo') {
                echo "Checking out source code..."
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: appBranch]],
                    userRemoteConfigs: [[
                        url: appRepo,
                        credentialsId: appRepoCredentialsId
                    ]]
                ])
            }

            stage('Build Docker Image') {
                echo "Building image ${fullImage}"
                sh "docker build -t ${fullImage} ."
            }

            stage('Push Docker Image') {
                withCredentials([
                    usernamePassword(
                        credentialsId: dockerCredentialsId,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh """
                    echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                    docker push ${fullImage}
                    docker logout
                    """
                }
            }

            stage('Checkout Manifest Repo') {
                dir(manifestCloneDir) {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: manifestBranch]],
                        userRemoteConfigs: [[
                            url: manifestRepo,
                            credentialsId: manifestRepoCredentialsId
                        ]]
                    ])
                }
            }

            stage('Update values.yaml and Push') {
                dir(manifestCloneDir) {
                    withCredentials([
                        file(
                            credentialsId: gitConfigCredentialsId,
                            variable: 'GIT_CONFIG_FILE'
                        ),
                        sshUserPrivateKey(
                            credentialsId: manifestRepoCredentialsId,
                            keyFileVariable: 'SSH_KEY'
                        )
                    ]) {
                        sh """
                        cp \$GIT_CONFIG_FILE ~/.gitconfig
                        export GIT_SSH_COMMAND="ssh -i \$SSH_KEY -o StrictHostKeyChecking=no"

                        git checkout ${manifestBranch} || git checkout -b ${manifestBranch}
                        git pull origin ${manifestBranch}

                        sed -i 's|repository:.*|repository: "${imageRegistry}/${imageName}"|g' ${valuesFile}
                        sed -i 's|tag:.*|tag: "${finalTag}"|g' ${valuesFile}

                        git add ${valuesFile}
                        git commit -m "Updated image ${finalTag}" || echo "No changes to commit"
                        git push origin ${manifestBranch}
                        """
                    }
                }
            }

            stage('Wait For Rollout Completion') {
                sh """
                kubectl rollout status deployment/${k8sDeploymentName} -n ${k8sNamespace} --timeout=300s
                """
            }

            stage('Wait For Pod Stabilization') {
                sh "sleep 30"
            }

            stage('Verify Latest Image Deployed') {
                sh """
                kubectl get pods -n ${k8sNamespace}

                DEPLOYED_IMAGE=\$(kubectl get deployment ${k8sDeploymentName} -n ${k8sNamespace} -o jsonpath='{.spec.template.spec.containers[0].image}')

                echo "Expected Image : ${fullImage}"
                echo "Actual Image   : \$DEPLOYED_IMAGE"

                if [ "\$DEPLOYED_IMAGE" = "${fullImage}" ]; then
                    echo "Latest image deployed successfully."
                else
                    echo "Old image still deployed."
                    exit 1
                fi
                """
            }

            stage('Cleanup Docker') {
                sh "docker image prune -af || true"
            }

        } catch (err) {

            buildFailed = true
            echo "Deployment failed: ${err.getMessage()}"
            throw err

        } finally {

            stage('Post Actions') {
                if (buildFailed) {
                    echo "Deployment failed."
                } else {
                    echo "Deployment succeeded."
                }

                cleanWs()
            }
        }
    }
}
