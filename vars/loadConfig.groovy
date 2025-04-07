def call() {
    def config = [
        services: [
            "public-web": [
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
            "private-web": [
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
            "static-web": [
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
            ],
            "web-calculator": [
                environments: [
                    dev: [
                        agentName: '',
                        repoUrl: 'https://github.com/RaamHorakeri/simple-calculator.git',
                        branch: 'main',
                        credentialsId: 'gittoken',
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
            "community": [
                environments: [
                    dev: [
                        agentName: '',
                        repoUrl: 'https://github.com/eskeon/community.git',
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
            ]
            "raven": [
                environments: [
                    dev: [
                        agentName: '',
                        repoUrl: 'https://github.com/eskeon/raven.git',
                        branch: 'main',
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
