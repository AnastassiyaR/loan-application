FROM gradle:8.13-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY src src
RUN gradle test --no-daemon --tests "com.loanapp.backend.service.*" --tests "com.loanapp.backend.validation.*"
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]