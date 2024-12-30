FROM openjdk:11-jdk-slim

WORKDIR /app

COPY build/libs/ground-0.0.1.jar ground.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "ground.jar", "&"]