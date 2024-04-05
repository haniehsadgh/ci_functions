def call(dockerRepoName, imageName, portNum) {
    pipeline {
        agent any
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
                steps {
                    withCredentials([string(credentialsId: 'DockerH', variable: 'TOKEN')]) {
                        sh "echo $TOKEN | docker login -u haniehgh --password-stdin docker.io"
                        // sh "docker login -u 'haniehgh' -p '$TOKEN' docker.io"
                        script {
                            def currentDir = pwd().split('/').last()
                            sh "docker build -t ${dockerRepoName}:latest --tag haniehgh/${dockerRepoName}:${imageName} ${currentDir}/."
                        }
                        sh "docker push haniehgh/${dockerRepoName}:${imageName}"
                    }
                }
            }
            stage('Deliver') {
                steps {
                    // Remote deployment to VM
                    sshagent(credentials: ['Kafka']) {
                        sh "ssh azureuser@20.81.210.156 'docker-compose up -d'"
                    }
                }
            }
        }
    }
}
