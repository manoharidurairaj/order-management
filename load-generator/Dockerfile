FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/load-generator-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
