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
                    script {
                        def currentDir = pwd().split('/').last()
                        sh "pylint --fail-under 5.0 ${currentDir}/*.py" 
                    }
                }
            }
            stage('Package') {
                when {
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
                steps {
                    withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                        sh "echo $TOKEN | docker login -u haniehgh --password-stdin docker.io"
                        // sh "docker login -u 'haniehgh' -p '$TOKEN' docker.io"
                        sh "docker build -t ${dockerRepoName}:latest --tag haniehgh/${dockerRepoName}:${imageName} ."
                        sh "docker push haniehgh/${dockerRepoName}:${imageName}"
                    }
                }
            }
            stage('Security') {
                steps {
                    script {
                        def currentDir = pwd().split('/').last()
                        sh "bandit -r ${currentDir}/*.py" 
                    }
                    // sh "docker tag redis haniehgh/${dockerRepoName}:latest"
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
