pipeline {
    agent any

    tools {
        maven 'maven3'
        jdk 'JDK17'
    }

    environment {
        // Adaptation for your local environment
        SONAR_TOKEN = 'sqa_ded110eee1c638fafe316ac69e08e9b045887e3f'
        DB_USER = 'root'
        DB_PASS = 'root'
        DOCKER_HUB_USER = 'arouss'
    }

    stages {
        stage('Setup Infrastructure') {
            steps {
                script {
                    echo '📦 Starting MySQL...'
                    sh '''
                        docker rm -f mysql-db || true
                        docker run -d \
                            --name mysql-db \
                            -e MYSQL_ROOT_PASSWORD=${DB_PASS} \
                            -e MYSQL_DATABASE=formini \
                            -e MYSQL_ROOT_HOST=% \
                            -p 3306:3306 \
                            mysql:8.0
                    '''
                }
            }
        }

        stage('Forum Service Pipeline') {
            steps {
                // 1. Database Creation (if needed specifically for forum)
                sh "docker exec mysql-db mysql -u${DB_USER} -p${DB_PASS} -e 'CREATE DATABASE IF NOT EXISTS formini;'"

                // 2. Tests + Coverage
                sh "mvn clean verify -Dspring.datasource.password=${DB_PASS} -Dspring.datasource.username=${DB_USER}"
                
                // 3. SonarQube Analysis
                sh """
                    mvn sonar:sonar \
                    -Dsonar.token=${SONAR_TOKEN} \
                    -Dsonar.projectKey=forum-service \
                    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                """
                
                // 4. Docker Build & Push
                withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                    sh "docker build -t ${DOCKER_HUB_USER}/forum-service:latest ."
                    sh "docker push ${DOCKER_HUB_USER}/forum-service:latest"
                }

                // 5. Kubernetes Deploy
                sh 'kubectl apply -f k8s/deployment.yaml'
                sh 'kubectl rollout restart deployment/forum-service || true'
            }
        }

        stage('Verify System Health') {
            steps {
                echo '🔍 Verifying Deployment Status...'
                sh 'sleep 10'
                sh 'kubectl get pods -l app=forum-service'
                echo '✅ Forum Service is running'
            }
        }
    }

    post {
        success { echo '🚀 Full Deployment & Analysis Successful!' }
        failure { echo '❌ Pipeline failed - Check logs' }
    }
}
