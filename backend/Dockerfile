## Start with a base image

FROM maven:latest AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ARG PROJECT_VERSION= 0.0.1
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=builder /app/dawn-web/target/dawn-web-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8888
ENTRYPOINT [ "java", "-jar", "/app/app.jar" ]