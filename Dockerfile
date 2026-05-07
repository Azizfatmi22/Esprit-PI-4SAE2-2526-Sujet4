FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the JAR built by Jenkins instead of building it again
COPY target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]