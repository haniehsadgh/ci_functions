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
            // stage('Package') {
            //     when {
            //         expression { env.GIT_BRANCH == 'origin/main' }
            //     }
            //     steps {
            //         withCredentials([string(credentialsId: 'DockerH', variable: 'TOKEN')]) {
            //             sh "echo $TOKEN | docker login -u haniehgh --password-stdin docker.io"
            //             // sh "docker login -u 'haniehgh' -p '$TOKEN' docker.io"
            //             script {
            //                 def currentDir = pwd().split('/').last()
            //                 sh "docker build -t ${dockerRepoName}:latest --tag haniehgh/${dockerRepoName}:${imageName} ${currentDir}/."
            //             }
            //             sh "docker push haniehgh/${dockerRepoName}:${imageName}"
            //         }
            //     }
            // }
            stage('Security') {
                steps {
                    script {
                        def currentDir = pwd().split('/').last()
                        // sh "bandit -r ${currentDir}/*.py"
                        sh """
                            . .venv/bin/activate
                            pip install bandit
                            
                            bandit -r ${currentDir}/*.py
                        """
                    }
                }
            }
            stage('Deliver') {
                when {
                    expression { params.DEPLOY }
                }
                steps {
                    sh "docker stop ${dockerRepoName} || true && docker rm ${dockerRepoName} || true"
                    sh "docker run -d -p ${portNum}:${portNum} --name ${dockerRepoName} ${dockerRepoName}:latest"
                }
            }
        }
    }
}
