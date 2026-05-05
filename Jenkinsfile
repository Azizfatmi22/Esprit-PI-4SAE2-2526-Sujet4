pipeline {
    agent any

    environment {
        // For Jenkins running in Docker to reach SonarQube on your host machine,
        // 'host.docker.internal' is used. If Jenkins runs natively on Windows, change it to 'localhost'
        SONAR_HOST_URL = 'http://host.docker.internal:9000'
        
        // This expects a Jenkins "Secret text" credential named 'sonar-token'
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {
        stage('Build & Test') {
            steps {
                script {
                    if (isUnix()) {
                        sh './mvnw -B clean verify'
                    } else {
                        bat '.\\mvnw.cmd -B clean verify'
                    }
                }
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    if (isUnix()) {
                        sh './mvnw -B sonar:sonar -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.token=${SONAR_TOKEN} -Dsonar.projectKey=forum-service -Dsonar.projectName=forum-service'
                    } else {
                        bat '.\\mvnw.cmd -B sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=forum-service -Dsonar.projectName=forum-service'
                    }
                }
            }
        }
    }
}
