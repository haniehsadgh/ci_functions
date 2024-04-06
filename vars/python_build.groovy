def call(dockerRepoName, imageName, portNum) {
    pipeline {
        agent any
        // parameters {
        //     booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
        //     string(defaultValue: 'staging', description: '', name: 'DEPLOY_ENV')
        // }

        stages {
            stage('Lint') {
                steps {
                    script {
                        def currentDir = pwd().split('/').last()
                        sh "pylint --fail-under 5.0 ${currentDir}/*.py" 
                    }
                }
            }
            stage('Security') {
                steps {
                    script {
                        def currentDir = pwd().split('/').last()
                        sh """
                            python3 -m venv .venv
                        """
                        sh """
                            . .venv/bin/activate
                            pip install bandit
                            bandit -r ${currentDir}/*.py
                        """
                    }
                }
            }
            stage('Package') {
                // when {
                //     expression { env.GIT_BRANCH == 'origin/main' }
                // }
                steps {
                    withCredentials([string(credentialsId: 'DockerH', variable: 'TOKEN')]) {
                        sh "echo $TOKEN | docker login -u haniehgh --password-stdin docker.io"
                        script {
                            def currentDir = pwd().split('/').last()
                            sh "docker build -t ${dockerRepoName}:latest --tag haniehgh/${dockerRepoName}:${imageName} ${currentDir}/."
                        }
                        sh "docker push haniehgh/${dockerRepoName}:${imageName}"
                    }
                }
            }
            stage('Deploy') {
                when {
                    expression { params.DEPLOY }
                }
                steps {
                    sshagent(credentials: ['Kafka']) {
                        sh """
                            [ -d ~/.ssh ] || mkdir ~/.ssh && chmod 0700 ~/.ssh
                            ssh-keyscan -t rsa,dsa 20.81.210.156 >> ~/.ssh/known_hosts
                        """
                        sh "ssh azureuser@20.81.210.156 'docker pull haniehgh/${dockerRepoName}:${imageName}'"
                        sh "ssh azureuser@20.81.210.156 'docker compose up -d'"
                    }
                }
            }
        }
    }
}
