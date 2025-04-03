def call(String service, String env) {
    def config = loadConfig()
    def envConfig = config.services[service]?.environments[env]

    if (!envConfig) {
        error "Configuration not found for service: ${service}, environment: ${env}"
    }

    deployPipeline(service, envConfig)
}

def loadConfig() {
    return load 'resources/config.groovy'  // ✅ Loading from resources directory
}
