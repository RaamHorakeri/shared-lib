def call(String agentName, String environment, String helmReleaseName,
         String helmNamespace, String chartRepoUrl, String chartRepoBranch,
         String chartRepoCredentialsId, String secretYamlPath, String secretYamlCredentialsId) {

    node(agentName) {
        def buildFailed = false

        try {
            stage('Checkout Helm Chart Repo') {
                echo "🔁 Checking out Helm repo '${chartRepoUrl}' branch '${chartRepoBranch}'"
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${chartRepoBranch}"]],
                    userRemoteConfigs: [[
                        url: chartRepoUrl,
                        credentialsId: chartRepoCredentialsId
                    ]]
                ])
            }

            def credentialsList = [file(credentialsId: secretYamlCredentialsId, variable: 'RAW_SECRET_YAML')]

            stage('Deploy Helm Chart with Secrets') {
                withCredentials(credentialsList) {
                    sh """
                    # Set chart directory from secret YAML path
                    for file in "${secretYamlPath}"; do CHART_DIR=\$(dirname "\$file"); done
                    CHART_DIR=\${CHART_DIR%/}

                    echo "📂 Using chart directory: \$CHART_DIR"
                    echo "🚀 Deploying Helm release '${helmReleaseName}' in namespace '${helmNamespace}'..."

                    # ✅ Use default kubeconfig from the agent (no override needed)

                    helm upgrade --install ${helmReleaseName} "\$CHART_DIR" \\
                        --namespace ${helmNamespace} \\
                        --create-namespace \\
                        --atomic \\
                        --wait \\
                        -f "\$RAW_SECRET_YAML" \\
                        --set environment=${environment}

                    kubectl rollout status deployment/${helmReleaseName} -n ${helmNamespace} --timeout=300s
                    """
                }
            }

        } catch (err) {
            buildFailed = true
            echo "❌ Deployment failed: ${err.getMessage()}"
            throw err
        } finally {
            stage('Post Actions') {
                if (buildFailed) {
                    echo '📛 Deployment failed!'
                } else {
                    echo '✅ Deployment succeeded.'
                }
                cleanWs()
            }
        }
    }
}
