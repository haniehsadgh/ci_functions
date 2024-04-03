def call(dockerRepoName, imageName, portNum) {
    pipeline {
        agent any
        parameters {
            booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
            string(defaultValue: 'staging', description: '', name: 'DEPLOY_ENV')
        }

        stages {
            stage('Lint') {
                steps {
                    sh 'pylint --fail-under 5.0 *.py'
                }
            }
            stage('Security') {
                steps {
                    sh "docker tag redis haniehgh/${dockerRepoName}:latest"
                    sh "bandit -r *.py"
            }
            stage('Package') {
                when {
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
                steps {
                    withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                        sh "docker login -u 'haniehgh' -p '$TOKEN' docker.io"
                        sh "docker build -t ${dockerRepoName}:latest --tag haniehgh/${dockerRepoName}:${imageName} ."
                        sh "docker push haniehgh/${dockerRepoName}:${imageName}"
                    }
                }
            }
            stage('Deliver') {
                when {
                    expression { params.DEPLOY }
                }
                steps {
                    sshCommand remote: 'Kafka', command 'docker compose up -d'
                }
            }
        }
    }
}

