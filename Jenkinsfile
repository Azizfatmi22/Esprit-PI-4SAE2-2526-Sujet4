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

        // ── INFRASTRUCTURE ────────────────────────────────────────
        stage('Setup Infrastructure') {
            parallel {

                stage('Database') {
                    steps {
                        script {
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
                            echo '⏳ Attendre MySQL...'
                            sleep(time: 30, unit: 'SECONDS')
                            echo '✅ MySQL démarré'
                        }
                    }
                }

                stage('Monitoring Stack') {
                    steps {
                        script {
                            echo '📈 Démarrage Prometheus + Grafana...'
                            bat '''
                                docker rm -f prometheus 2>nul || echo ok
                                docker rm -f grafana    2>nul || echo ok

                                docker run -d ^
                                    --name prometheus ^
                                    -p 9090:9090 ^
                                    prom/prometheus:latest

                                docker run -d ^
                                    --name grafana ^
                                    -p 3000:3000 ^
                                    -e GF_SECURITY_ADMIN_PASSWORD=admin ^
                                    grafana/grafana:latest
                            '''
                            echo '✅ Prometheus : http://localhost:9090'
                            echo '✅ Grafana    : http://localhost:3000'
                        }
                    }
                }

                stage('SonarQube') {
                    steps {
                        script {
                            echo '🔍 Démarrage SonarQube...'
                            bat '''
                                docker rm -f sonarqube 2>nul || echo ok
                                docker run -d ^
                                    --name sonarqube ^
                                    -p 9000:9000 ^
                                    sonarqube:latest
                            '''
                            echo '⏳ Attendre SonarQube...'
                            sleep(time: 40, unit: 'SECONDS')
                            echo '✅ SonarQube : http://localhost:9000'
                        }
                    }
                }
            }
        }

        // ── BUILD JAR ─────────────────────────────────────────────
        stage('Build JAR') {
            steps {
                echo '🔨 Compilation Maven...'
                bat 'mvn clean package -DskipTests -Dnet.bytebuddy.experimental=true'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar',
                                     fingerprint: true
                    echo '✅ JAR archivé'
                }
                failure {
                    echo '❌ Compilation échouée !'
                }
            }
        }

        // ── TESTS UNITAIRES ───────────────────────────────────────
        stage('Tests Unitaires') {
            steps {
                echo '🧪 Exécution des tests...'
                bat 'mvn test -Dnet.bytebuddy.experimental=true'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: true
                    echo '📊 Rapport tests généré'
                }
                success {
                    echo '✅ Tous les tests passent !'
                }
                failure {
                    echo '❌ Tests échoués !'
                }
            }
        }

        // ── SONARQUBE ANALYSE ─────────────────────────────────────
        stage('SonarQube Analysis') {
            steps {
                echo '🔍 Analyse SonarQube...'
                withSonarQubeEnv('SonarQube') {
                    bat """
                        mvn sonar:sonar ^
                        -Dsonar.projectKey=reclamation-service ^
                        -Dsonar.projectName="Reclamation Service"
                    """
                }
            }
        }
        // ── QUALITY GATE ──────────────────────────────────────────
        stage('Quality Gate') {
            steps {
                echo '🚦 Vérification Quality Gate...'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
            post {
                success { echo '✅ Quality Gate passé !' }
                failure { echo '⚠️ Quality Gate échoué — on continue' }
            }
        }

        // ── DOCKER BUILD & PUSH ───────────────────────────────────
        stage('Docker Build and Push') {
            steps {
                echo '🐳 Construction et push image Docker...'
                script {
                    bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    bat "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                    echo "✅ Image créée : ${DOCKER_IMAGE}:${DOCKER_TAG}"

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
                        bat "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                        bat "docker push ${DOCKER_IMAGE}:latest"
                        echo '✅ Image poussée sur Docker Hub'
                    }
                }
            }
        }

        // ── DEPLOY ────────────────────────────────────────────────
        stage('Deploy') {
            steps {
                echo '🚀 Déploiement du microservice Réclamation...'
                script {
                    bat """
                        docker stop ${CONTAINER} 2>nul || echo ok
                        docker rm   ${CONTAINER} 2>nul || echo ok
                        docker run -d ^
                            --name ${CONTAINER} ^
                            --link mysql-db:mysql ^
                            -p ${PORT}:${PORT} ^
                            -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/formini_reclamation_db?createDatabaseIfNotExist=true ^
                            -e SPRING_DATASOURCE_USERNAME=root ^
                            -e SPRING_DATASOURCE_PASSWORD=root ^
                            -e SPRING_JPA_HIBERNATE_DDL_AUTO=update ^
                            -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ ^
                            -e APP_ADMIN_EMAIL=inesjlassi245@gmail.com ^
                            -e SPRING_MAIL_HOST=smtp.gmail.com ^
                            -e SPRING_MAIL_USERNAME=inesjlasi588@gmail.com ^
                            -e "SPRING_MAIL_PASSWORD=awof cxoj auid oxcf" ^
                            ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                    echo "✅ Conteneur lancé sur le port ${PORT}"
                }
            }
        }

        // ── HEALTH CHECK ──────────────────────────────────────────
        stage('Verify Health and Metrics') {
            steps {
                echo '🏥 Vérification santé du service...'
                sleep(time: 60, unit: 'SECONDS')
                script {
                    bat "docker ps -a --filter name=${CONTAINER}"
                    bat "docker logs ${CONTAINER} --tail 80"

                    def status = bat(
                        script: "curl -f http://localhost:${PORT}/msreclamation/health",
                        returnStatus: true
                    )

                    if (status != 0) {
                        bat "docker logs ${CONTAINER}"
                        error '❌ Health check échoué — voir les logs ci-dessus'
                    }

                    echo '✅ Service Réclamation opérationnel !'
                    echo "🌐 API        : http://localhost:${PORT}/msreclamation"
                    echo "📊 Prometheus : http://localhost:9090"
                    echo "📈 Grafana    : http://localhost:3000 (admin/admin)"
                    echo "🔍 SonarQube  : http://localhost:9000"
                }
            }
        }
    }

    post {
        success {
            echo '''
            ╔══════════════════════════════════════════╗
            ║   ✅ PIPELINE RÉCLAMATION RÉUSSI         ║
            ║                                          ║
            ║   📦 Build JAR       : OK                ║
            ║   🧪 Tests unitaires : OK                ║
            ║   🔍 SonarQube       : OK                ║
            ║   🐳 Docker Build    : OK                ║
            ║   📤 Docker Push     : OK                ║
            ║   🚀 Deploy          : OK                ║
            ║   🏥 Health Check    : OK                ║
            ║                                          ║
            ║   🌐 http://localhost:8093               ║
            ╚══════════════════════════════════════════╝
            '''
        }
        failure {
            echo '''
            ╔══════════════════════════════════════════╗
            ║   ❌ PIPELINE RÉCLAMATION ÉCHOUÉ         ║
            ║   Vérifiez les logs Jenkins              ║
            ╚══════════════════════════════════════════╝
            '''
        }
        always {
            echo '🧹 Nettoyage workspace...'
            cleanWs()
        }
    }
}