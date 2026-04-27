def call(String agentName, String imageRegistry, String imageName, String imageTag, String appRepo, String appBranch, String appRepoCredentialsId, String manifestRepo,
         String manifestBranch, String manifestRepoCredentialsId, String manifestCloneDir, String valuesFile, String dockerCredentialsId, String gitConfigCredentialsId,
         String k8sDeploymentName, String k8sNamespace, String k8sContext, String k8sCluster, String teamsWebhookUrl) {

    node(agentName) {

        def finalTag    = imageTag?.trim() ? imageTag : env.BUILD_NUMBER
        def fullImage   = "${imageRegistry}/${imageName}:${finalTag}"
        def buildFailed = false

        try {

            stage('Set Kubernetes Context') {
                sh """
                echo "Using Kubernetes Cluster : ${k8sCluster}"
                echo "Using Kubernetes Context : ${k8sContext}"

                kubectl config get-contexts
                kubectl config use-context ${k8sContext}
                kubectl config current-context
                """
            }

            stage('Checkout Application Repo') {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: appBranch]],
                    userRemoteConfigs: [[
                        url: appRepo,
                        credentialsId: appRepoCredentialsId
                    ]]
                ])

                env.commitId     = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                env.commitMsg    = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                env.gitCommitter = sh(script: "git log -1 --pretty=%an", returnStdout: true).trim()

                def userCause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)
                def scmCause  = currentBuild.rawBuild.getCause(hudson.triggers.SCMTrigger$SCMTriggerCause)

                if (userCause) {
                    env.committer = userCause.getUserName() ?: env.gitCommitter
                    env.commitMsg = "Manually triggered from Jenkins"
                } else if (scmCause) {
                    env.committer = env.gitCommitter
                } else {
                    env.committer = env.gitCommitter
                }
            }

            stage('Build Docker Image') {
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
                        file(credentialsId: gitConfigCredentialsId, variable: 'GIT_CONFIG_FILE'),
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
                kubectl --context=${k8sContext} rollout status deployment/${k8sDeploymentName} -n ${k8sNamespace} --timeout=300s
                """
            }

            stage('Wait For Pod Stabilization') {
                sh "sleep 30"
            }

            stage('Verify Latest Image Deployed') {
                sh """
                DEPLOYED_IMAGE=\$(kubectl --context=${k8sContext} get deployment ${k8sDeploymentName} -n ${k8sNamespace} -o jsonpath='{.spec.template.spec.containers[0].image}')

                if [ "\$DEPLOYED_IMAGE" != "${fullImage}" ]; then
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
            currentBuild.result = "FAILURE"
            throw err

        } finally {

            stage('Post Actions') {
                script {

                    def buildResult = currentBuild.result ?: currentBuild.currentResult ?: (buildFailed ? "FAILURE" : "SUCCESS")
                    def committer   = env.committer ?: "Unknown"
                    def commitId    = env.commitId ?: "N/A"
                    def commitMsg   = env.commitMsg ?: "No message"

                    def millis  = currentBuild.duration ?: 0
                    def seconds = (millis / 1000) as int
                    def minutes = (seconds / 60) as int
                    seconds = seconds % 60
                    def duration = "${minutes} min ${seconds} sec"

                    def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('Asia/Kolkata'))

                    def status = (buildResult == "SUCCESS") ? "✅ SUCCESS" :
                                 (buildResult == "FAILURE") ? "❌ FAILURE" :
                                 (buildResult == "ABORTED") ? "🚫 ABORTED" :
                                 "⚠️ ${buildResult}"

                    currentBuild.displayName = "#${env.BUILD_NUMBER} - ${appBranch}"

                    currentBuild.description = """${status}<br>
🔥 Triggered by: <strong>${committer}</strong><br>
🌿 Branch: <strong>${appBranch}</strong><br>
🧱 Commit: <strong>${commitId}</strong><br>
💬 Message: <strong>${commitMsg}</strong><br>
📦 Image: <strong>${fullImage}</strong><br>
🔢 Build Number: <strong>#${env.BUILD_NUMBER}</strong><br>
⏱ Duration: <strong>${duration}</strong><br>
🕒 Time: <strong>${timestamp}</strong>"""

                    def teamsMsg = currentBuild.description

                    def webhookValue = ""
                    try {
                        withCredentials([string(credentialsId: teamsWebhookUrl, variable: 'TEAMS_WEBHOOK')]) {
                            webhookValue = TEAMS_WEBHOOK
                        }
                    } catch (Exception e) {
                        webhookValue = teamsWebhookUrl
                    }

                    try {
                        if (webhookValue?.trim()) {
                            office365ConnectorSend(
                                webhookUrl: webhookValue,
                                message: teamsMsg,
                                status: buildResult
                            )
                        }
                    } catch (Exception ex) {
                        echo "Teams notification skipped."
                    }

                    cleanWs()
                }
            }
        }
    }
}
