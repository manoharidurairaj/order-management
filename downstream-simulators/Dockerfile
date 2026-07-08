FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/downstream-simulators-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
