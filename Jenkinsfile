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
        SONAR_URL    = 'http://localhost:9000'
    }

    stages {

        // ─────────────────────────────────────────────
        // INFRASTRUCTURE
        // ─────────────────────────────────────────────
        stage('Setup Infrastructure') {
            parallel {

                stage('Database') {
                    steps {
                        echo '📦 Démarrage MySQL...'
                        bat '''
                            docker rm -f mysql-db 2>nul || echo ok
                            docker run -d ^
                              --name mysql-db ^
                              -e MYSQL_ROOT_PASSWORD=root ^
                              -e MYSQL_DATABASE=formini_reclamation_db ^
                              -p 3306:3306 ^
                              mysql:8.0
                        '''
                        sleep(time: 30, unit: 'SECONDS')
                        echo '✅ MySQL prêt'
                    }
                }

                stage('Monitoring Stack') {
                    steps {
                        echo '📈 Démarrage Prometheus + Grafana...'
                        bat '''
                            docker rm -f prometheus 2>nul || echo ok
                            docker rm -f grafana 2>nul || echo ok

                            docker run -d ^
                              --name prometheus ^
                              -p 9090:9090 ^
                              prom/prometheus

                            docker run -d ^
                              --name grafana ^
                              -p 3000:3000 ^
                              -e GF_SECURITY_ADMIN_PASSWORD=admin ^
                              grafana/grafana
                        '''
                        echo '✅ Monitoring prêt'
                    }
                }

                stage('SonarQube') {
                    steps {
                        echo '🔍 Démarrage SonarQube...'
                        bat '''
                            docker rm -f sonarqube 2>nul || echo ok
                            docker run -d ^
                              --name sonarqube ^
                              -p 9000:9000 ^
                              sonarqube:lts-community
                        '''
                        sleep(time: 70, unit: 'SECONDS')
                        echo '✅ SonarQube prêt'
                    }
                }
            }
        }

        // ─────────────────────────────────────────────
        // BUILD
        // ─────────────────────────────────────────────
        stage('Build JAR') {
            steps {
                echo '🔨 Build Maven...'
                bat 'mvn clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    echo '✅ JAR généré'
                }
            }
        }

        // ─────────────────────────────────────────────
        // TESTS
        // ─────────────────────────────────────────────
        stage('Tests Unitaires') {
            steps {
                echo '🧪 Lancement des tests...'
                bat 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    echo '📊 Rapport tests publié'
                }
            }
        }

        // ─────────────────────────────────────────────
        // SONARQUBE
        // ─────────────────────────────────────────────
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        bat """
                        mvn clean verify sonar:sonar ^
                        -Dsonar.projectKey=reclamation-service ^
                        -Dsonar.host.url=http://localhost:9000 ^
                        -Dsonar.token=%SONAR_TOKEN% ^
                        -Dsonar.java.binaries=target/classes ^
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        """
                    }
                }
            }
        }
        // ─────────────────────────────────────────────
        // QUALITY GATE (optionnel)
        // ─────────────────────────────────────────────
        stage('Quality Gate') {
            steps {
                echo '🚦 Quality Gate ignoré pour pipeline local'
            }
        }

        // ─────────────────────────────────────────────
        // DOCKER BUILD + PUSH
        // ─────────────────────────────────────────────
        stage('Docker Build and Push') {
            steps {
                echo '🐳 Build image Docker...'

                bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                bat "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"

                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {

                    bat 'docker login -u %DOCKER_USER% -p %DOCKER_PASS%'
                    bat "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    bat "docker push ${DOCKER_IMAGE}:latest"
                }

                echo '✅ Image Docker publiée'
            }
        }

        // ─────────────────────────────────────────────
        // DEPLOY
        // ─────────────────────────────────────────────
        stage('Deploy') {
            steps {
                echo '🚀 Déploiement...'

                bat """
                    docker stop ${CONTAINER} 2>nul || echo ok
                    docker rm ${CONTAINER} 2>nul || echo ok

                    docker run -d ^
                      --name ${CONTAINER} ^
                      --link mysql-db:mysql ^
                      -p ${PORT}:8093 ^
                      -e SERVER_PORT=8093 ^
                      -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/formini_reclamation_db?createDatabaseIfNotExist=true ^
                      -e SPRING_DATASOURCE_USERNAME=root ^
                      -e SPRING_DATASOURCE_PASSWORD=root ^
                      -e SPRING_JPA_HIBERNATE_DDL_AUTO=update ^
                      -e EUREKA_CLIENT_ENABLED=false ^
                      ${DOCKER_IMAGE}:${DOCKER_TAG}
                """
            }
        }

        // ─────────────────────────────────────────────
        // HEALTH CHECK
        // ─────────────────────────────────────────────
        stage('Verify Health') {
            steps {
                sleep(time: 45, unit: 'SECONDS')

                script {
                    def status = bat(
                        script: "curl -f http://localhost:${PORT}/actuator/health",
                        returnStatus: true
                    )

                    if (status != 0) {
                        bat "docker logs ${CONTAINER}"
                        error('❌ Service non disponible')
                    }

                    echo '✅ Microservice UP'
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // POST
    // ─────────────────────────────────────────────
    post {

        success {
            echo '''
╔════════════════════════════════════╗
║   ✅ PIPELINE RÉUSSI              ║
║   Build / Test / Sonar / Docker   ║
║   http://localhost:8093           ║
╚════════════════════════════════════╝
'''
        }

        failure {
            echo '''
╔════════════════════════════════════╗
║   ❌ PIPELINE ÉCHOUÉ              ║
║   Vérifiez Jenkins logs           ║
╚════════════════════════════════════╝
'''
        }

        always {
            cleanWs()
        }
    }
}