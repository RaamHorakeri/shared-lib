def call(String agentName, String environment, String helmReleaseName,
         String helmNamespace, String chartRepoUrl, String chartRepoBranch,
         String chartRepoCredentialsId, String secretYamlPath, String secretYamlCredentialsId,
         String kubeconfigSecretId = null) {

    node(agentName) {
        def buildFailed = false

        try {
            // Stage 1: Checkout Helm Chart repository
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

            // Stage 2: Deploy Helm Chart with Secrets
            def credentialsList = [file(credentialsId: secretYamlCredentialsId, variable: 'RAW_SECRET_YAML')]
            if (kubeconfigSecretId) {
                credentialsList << file(credentialsId: kubeconfigSecretId, variable: 'KUBECONFIG')
            }

            stage('Deploy Helm Chart with Secrets') {
                withCredentials(credentialsList) {
                    bat """
                    REM Set chart directory from secret YAML path
                    for %%I in ("${secretYamlPath}") do set CHART_DIR=%%~dpI

                    echo 🚀 Deploying Helm release '${helmReleaseName}' in namespace '${helmNamespace}'...
                    echo 📂 Using chart directory: %CHART_DIR%

                    helm upgrade --install ${helmReleaseName} "%CHART_DIR%" ^
                        --namespace ${helmNamespace} ^
                        --create-namespace ^
                        --atomic ^
                        --wait ^
                        -f "%RAW_SECRET_YAML%" ^
                        --set environment=${environment}
                    """
                }
            }

            // Stage 3: Verify Deployment Rollout
            stage('Check Deployment Rollout') {
                bat """
                kubectl rollout status deployment/${helmReleaseName} -n ${helmNamespace} --timeout=300s
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
