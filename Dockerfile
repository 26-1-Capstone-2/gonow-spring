FROM eclipse-temurin:21-jdk

ENV TZ=Asia/Seoul

WORKDIR /app

COPY build/libs/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
