def call() {
    def config = [
        services: [
            public: [
                environments: [
                    dev: [
                        agentName: '',
                        repoUrl: 'https://github.com/eskeon/public-web.git',
                        branch: 'staging',
                        credentialsId: 'git-eskeon-creds',
                        envVars: [
                            SENDGRID_KEY: 'SENDGRID_KEY_DEV',
                            MONGO_CONNECTION_STRING: 'MONGO_CONNECTION_STRING_DEV',
                            BIFROST_ACCOUNT_PROFILE_API: 'BIFROST_ACCOUNT_PROFILE_API_DEV',
                            ODIN_SECRET: 'ODIN_SECRET_DEV',
                            ODIN_HOST: 'ODIN_HOST_DEV'
                        ]
                    ]
                ]
            ],
            private: [
                environments: [
                    dev: [
                        agentName: '',
                        repoUrl: 'https://github.com/eskeon/private-web.git',
                        branch: 'dashboard',
                        credentialsId: 'git-eskeon-creds',
                        envVars: [
                            SENDGRID_KEY: 'SENDGRID_KEY_DEV',
                            MONGO_CONNECTION_STRING: 'MONGO_CONNECTION_STRING_DEV',
                            BIFROST_ACCOUNT_PROFILE_API: 'BIFROST_ACCOUNT_PROFILE_API_DEV',
                            ODIN_SECRET: 'ODIN_SECRET_DEV',
                            ODIN_HOST: 'ODIN_HOST_DEV'
                        ]
                    ]
                ]
            ],
            static: [
                environments: [
                    dev: [
                        agentName: '',
                        repoUrl: 'https://github.com/eskeon/static-web.git',
                        branch: 'static',
                        credentialsId: 'git-eskeon-creds',
                        envVars: [
                            SENDGRID_KEY: 'SENDGRID_KEY_DEV',
                            MONGO_CONNECTION_STRING: 'MONGO_CONNECTION_STRING_DEV',
                            BIFROST_ACCOUNT_PROFILE_API: 'BIFROST_ACCOUNT_PROFILE_API_DEV',
                            ODIN_SECRET: 'ODIN_SECRET_DEV',
                            ODIN_HOST: 'ODIN_HOST_DEV'
                        ]
                    ]
                ]
            ]
        ]
    ]

    return config
}
