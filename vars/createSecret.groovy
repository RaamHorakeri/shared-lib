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

            stage('Deploy Helm Chart with Secrets') {
                withCredentials([file(credentialsId: secretYamlCredentialsId, variable: 'RAW_SECRET_YAML')]) {
                    bat """
                        set +x  # 🔒 Hide sensitive output

                        echo "🚀 Deploying Helm release '${helmReleaseName}' in namespace '${helmNamespace}'..."

                        CHART_DIR=\$(dirname ${secretYamlPath})
                        echo "📂 Using chart directory: \$CHART_DIR"

                        helm upgrade --install ${helmReleaseName} \$CHART_DIR \
                            --namespace ${helmNamespace} \
                            --create-namespace \
                            --atomic \
                            --wait \
                            -f \$RAW_SECRET_YAML \
                            --set environment=${environment}

                        set -x
                    """
                }
            }

            stage('Check Deployment Rollout') {
                bat """
                    kubectl rollout status deployment/${helmReleaseName} \
                        -n ${helmNamespace} --timeout=300s
                """
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
