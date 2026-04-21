pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk   'JDK-21'
    }

    environment {
        DOCKER_IMAGE = 'inesjlasi/reclamation'
        DOCKER_TAG   = '1.0.0'
        CONTAINER    = 'reclamation-service'
        PORT         = '8093'
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 Récupération du code...'
                checkout scm
            }
        }

        stage('Build JAR') {
            steps {
                echo '🔨 Compilation Maven...'
                bat 'mvn clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar'
                }
            }
        }

        stage('Tests') {
            steps {
                echo '🧪 Tests unitaires...'
                bat 'mvn test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo '🐳 Construction image Docker...'
                bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
            }
        }

        stage('Docker Push') {
            steps {
                echo '📤 Push sur Docker Hub...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
                    bat "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                }
            }
        }

        stage('Deploy') {
            steps {
                echo '🚀 Déploiement du container...'
                bat """
                    docker stop ${CONTAINER} 2>nul || echo ok
                    docker rm   ${CONTAINER} 2>nul || echo ok
                    docker run -d ^
                        --name ${CONTAINER} ^
                        -p ${PORT}:${PORT} ^
                        -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/formini_reclamation_db ^
                        -e SPRING_DATASOURCE_USERNAME=root ^
                        -e SPRING_DATASOURCE_PASSWORD= ^
                        -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ ^
                        -e APP_ADMIN_EMAIL=inesjlassi245@gmail.com ^
                        ${DOCKER_IMAGE}:${DOCKER_TAG}
                """
            }
        }

        stage('Health Check') {
            steps {
                echo '🏥 Vérification santé...'
                sleep(time: 40, unit: 'SECONDS')
                bat 'docker exec reclamation-service curl -f http://localhost:8093/msreclamation/health'
            }
        }

    post {
        success { echo '✅ Pipeline réussi !' }
        failure { echo '❌ Pipeline échoué !' }
        always  { cleanWs() }
    }
}