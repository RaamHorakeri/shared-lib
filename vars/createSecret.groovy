// def call(String agentName, String environment, String helmReleaseName,
//          String helmNamespace, String chartRepoUrl, String chartRepoBranch, String chartCloneDir,
//          String chartPathInsideRepo, String chartRepoCredentialsId, String secretYamlCredentialsId) {

//     node(agentName) {
//         def buildFailed = false

//         try {
//             stage('Checkout Helm Chart Repo') {
//                 dir(chartCloneDir) {
//                     echo "🔁 Checking out Helm repo '${chartRepoUrl}' branch '${chartRepoBranch}'"
//                     checkoutFromGit(chartRepoBranch, chartRepoUrl, chartRepoCredentialsId)
//                 }
//             }

//             stage('Deploy Common Resources') {
//                 dir(chartCloneDir) {
//                     // ✅ Use the credentials ID passed as parameter
//                     withCredentials([file(credentialsId: secretYamlCredentialsId, variable: 'RAW_SECRET_YAML')]) {
//                         echo "🔐 Reading custom secret YAML and converting to valid Kubernetes Secret..."

//                         sh """
//                             cp \$RAW_SECRET_YAML converted-secret.yaml

//                             # Install yq if not already present
//                             if ! command -v yq >/dev/null 2>&1; then
//                               echo "🔧 Installing yq..."
//                               wget -qO /usr/local/bin/yq https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64
//                               chmod +x /usr/local/bin/yq
//                             fi

//                             # Extract name and keys
//                             secret_name=\$(yq e '.secret.name' converted-secret.yaml)
//                             keys=\$(yq e '.secret.data | keys | .[]' converted-secret.yaml)

//                             # Build the Kubernetes Secret YAML
//                             echo "apiVersion: v1" > final-secret.yaml
//                             echo "kind: Secret" >> final-secret.yaml
//                             echo "metadata:" >> final-secret.yaml
//                             echo "  name: \$secret_name" >> final-secret.yaml
//                             echo "  namespace: ${helmNamespace}" >> final-secret.yaml
//                             echo "type: Opaque" >> final-secret.yaml
//                             echo "data:" >> final-secret.yaml

//                             for key in \$keys; do
//                               value=\$(yq e ".secret.data.\$key" converted-secret.yaml)
//                               b64=\$(echo -n "\$value" | base64 | tr -d '\\n')
//                               echo "  \$key: \$b64" >> final-secret.yaml
//                             done

//                             echo "✅ Final base64-encoded secret:"
//                             cat final-secret.yaml

//                             # Apply the Secret
//                             kubectl apply -f final-secret.yaml

//                             # Run Helm install/upgrade
//                             echo "🚀 Deploying Helm chart with additional config..."
//                             helm upgrade --install ${helmReleaseName} ./${chartPathInsideRepo}/commons-svc \\
//                               --namespace ${helmNamespace} -f \$RAW_SECRET_YAML
//                         """
//                     }
//                 }
//             }

//         } catch (err) {
//             buildFailed = true
//             echo "❌ Error occurred: ${err.getMessage()}"
//             throw err
//         } finally {
//             stage('Post Actions') {
//                 if (buildFailed) {
//                     echo '📛 Deployment failed!'
//                 } else {
//                     echo '✅ Deployment succeeded.'
//                 }

//                 cleanWs()
//             }
//         }
//     }
// }

def call(String agentName, String environment, String namespace, String secretYamlCredentialsId) {

    node(agentName) {
        def buildFailed = false

        try {
            stage('Create Kubernetes Secret') {
                withCredentials([file(credentialsId: secretYamlCredentialsId, variable: 'RAW_SECRET_YAML')]) {
                    echo "🔐 Creating/Updating Kubernetes Secret from Jenkins secret file..."

                    sh """
                        # Apply the secret
                        kubectl apply -f \$RAW_SECRET_YAML -n ${namespace}

                        echo "✅ Secret applied successfully in namespace '${namespace}'"
                    """
                }
            }

            stage('Verify Kubernetes Secret') {
                sh """
                    echo "🔎 Verifying Secret in namespace '${namespace}'..."
                    kubectl get secret -n ${namespace}
                    kubectl describe secret -n ${namespace} || echo "⚠️ Secret description failed!"
                """
            }

        } catch (err) {
            buildFailed = true
            echo "❌ Secret deployment failed: ${err.getMessage()}"
            throw err
        } finally {
            stage('Post Actions') {
                if (buildFailed) {
                    echo '📛 Secret update failed!'
                } else {
                    echo '✅ Secret update succeeded.'
                }
                cleanWs()
            }
        }
    }
}
