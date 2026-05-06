pipeline {
    agent any

    environment {
        REPO_URL      = 'git@github.com:Azizfatmi22/PIDEV4EME.git'
        SONAR_TOKEN   = 'squ_323362c5ffd038691f0bab2375129356f4517cda'
        DOCKER_IMAGE  = 'benarfa/formini-frontend'
        NODE_VERSION  = '20'
    }

    tools {
        nodejs 'NodeJS20'
    }

    stages {

        // ─────────────────────────────────────────────────────────────
        // 1. Checkout the frontend branch
        // ─────────────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/Front-Formini']],
                    userRemoteConfigs: [[url: "${REPO_URL}"]]
                ])
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 2. Install npm dependencies
        // ─────────────────────────────────────────────────────────────
        stage('Install Dependencies') {
            steps {
                dir('PIDEV4EME-Front-Formini') {
                    sh 'npm install --legacy-peer-deps'
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 3. Run Angular Unit Tests (Karma + JUnit + Coverage)
        // ─────────────────────────────────────────────────────────────
        stage('Unit Tests') {
            steps {
                dir('PIDEV4EME-Front-Formini') {
                    sh '''
                        npm run test -- \
                            --watch=false \
                            --browsers=ChromeHeadlessCI \
                            --code-coverage
                    '''
                }
            }
            post {
                always {
                    // Publish JUnit XML report from Karma
                    junit 'PIDEV4EME-Front-Formini/reports/junit/test-results.xml'
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 4. SonarQube Analysis (reads sonar-project.properties)
        // ─────────────────────────────────────────────────────────────
        stage('SonarQube Analysis') {
            steps {
                dir('PIDEV4EME-Front-Formini') {
                    withSonarQubeEnv('SonarQube') {
                        sh """
                            npx sonar-scanner \
                                -Dsonar.token=${SONAR_TOKEN} \
                                -Dsonar.host.url=http://localhost:9000
                        """
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 5. Build production bundle
        // ─────────────────────────────────────────────────────────────
        stage('Build Production') {
            steps {
                dir('PIDEV4EME-Front-Formini') {
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 6. Build Docker Image
        // ─────────────────────────────────────────────────────────────
        stage('Build Docker Image') {
            steps {
                dir('PIDEV4EME-Front-Formini') {
                    sh "docker build -t ${DOCKER_IMAGE}:${env.BUILD_ID} -t ${DOCKER_IMAGE}:latest ."
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 7. Push to Docker Hub
        // ─────────────────────────────────────────────────────────────
        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('', 'dockerhub-credentials') {
                        sh "docker push ${DOCKER_IMAGE}:${env.BUILD_ID}"
                        sh "docker push ${DOCKER_IMAGE}:latest"
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 8. Deploy to Kubernetes
        // ─────────────────────────────────────────────────────────────
        stage('Deploy to Kubernetes') {
            steps {
                sh 'kubectl apply -f k8s/frontend.yaml'
                sh 'kubectl rollout restart deployment/formini-app || true'
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 9. Verify Deployment
        // ─────────────────────────────────────────────────────────────
        stage('Verify Deployment') {
            steps {
                sh 'sleep 10'
                sh 'kubectl get pods -l app=formini-app'
                echo '✅ Frontend deployed successfully'
            }
        }
    }

    post {
        success {
            echo '🚀 Frontend Pipeline succeeded — Tests passed, SonarQube analyzed, deployed!'
        }
        failure {
            echo '❌ Frontend Pipeline failed — check Angular test logs and SonarQube report'
        }
    }
}
