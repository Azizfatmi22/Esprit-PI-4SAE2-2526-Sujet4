pipeline {
    agent any

    environment {
        DOCKER_CREDENTIALS_ID = 'dockerhub-credentials'
        GITHUB_CREDENTIALS_ID = 'github-credentials'
        DOCKER_IMAGE          = 'hazemkhemiri/ms-course'
        DOCKER_TAG            = "${env.BUILD_NUMBER}"
        CONTAINER_NAME        = 'ms-course'
        APP_PORT              = '8083'
        GITHUB_REPO           = 'https://github.com/hazemkhemiri/MS-Course.git'
        BRANCH                = 'main'
    }

    tools {
        maven 'Maven-3.8'
        jdk   'JDK-17'
    }

    triggers {
        // GitHub webhook triggers this on every push to main
        githubPush()
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 Cloning MS-Course from GitHub...'
                git(
                    url: "${GITHUB_REPO}",
                    branch: "${BRANCH}",
                    credentialsId: "${GITHUB_CREDENTIALS_ID}"
                )
            }
        }

        stage('Build') {
            steps {
                echo '🔨 Compiling MS-Course...'
                sh 'mvn clean compile -q'
            }
        }

        stage('Test') {
            steps {
                echo '🧪 Running unit tests...'
                sh 'mvn test -Dnet.bytebuddy.experimental=true'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                echo '📦 Packaging JAR...'
                sh 'mvn package -DskipTests -q'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                echo '🐳 Building Docker image...'
                sh """
                    docker build \
                        -t ${DOCKER_IMAGE}:${DOCKER_TAG} \
                        -t ${DOCKER_IMAGE}:latest \
                        .
                """
            }
        }

        stage('Docker Push') {
            steps {
                echo '🚀 Pushing to Docker Hub...'
                withCredentials([usernamePassword(
                    credentialsId: "${DOCKER_CREDENTIALS_ID}",
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker push ${DOCKER_IMAGE}:latest
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                echo '🟢 Deploying MS-Course...'
                sh '''
                    docker stop  ${CONTAINER_NAME} || true
                    docker rm    ${CONTAINER_NAME} || true
                    docker pull  ${DOCKER_IMAGE}:latest
                    docker run -d \
                        --name    ${CONTAINER_NAME} \
                        --network microservices-network \
                        -p ${APP_PORT}:${APP_PORT} \
                        -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-db:3306/pidev_db?createDatabaseIfNotExist=true \
                        -e SPRING_DATASOURCE_USERNAME=root \
                        -e SPRING_DATASOURCE_PASSWORD=root \
                        -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/ \
                        -e FILE_UPLOAD_DIR=/app/uploads \
                        ${DOCKER_IMAGE}:latest
                '''
            }
        }

        stage('Health Check') {
            steps {
                echo '❤️ Checking service health...'
                retry(10) {
                    sleep(time: 6, unit: 'SECONDS')
                    sh "curl -sf http://localhost:${APP_PORT}/actuator/health | grep -q UP"
                }
            }
        }
    }

    post {
        success {
            echo "✅ MS-Course build #${env.BUILD_NUMBER} deployed successfully."
            // Optional: notify GitHub commit status
            githubNotify(
                status: 'SUCCESS',
                description: 'Build and deploy succeeded',
                context: 'ci/jenkins/ms-course'
            )
        }
        failure {
            echo "❌ MS-Course build #${env.BUILD_NUMBER} failed."
            githubNotify(
                status: 'FAILURE',
                description: 'Build or deploy failed',
                context: 'ci/jenkins/ms-course'
            )
        }
        always {
            // Remove the versioned image to save disk space; keep latest
            sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true"
            cleanWs()
        }
    }
}
