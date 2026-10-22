# Multi-stage build: the JDK compiles and tests with the Maven wrapper, the JRE runs the jar.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn ./.mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 fitmentor
COPY --from=build /app/target/*.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=render \
    PORT=8080
EXPOSE 8080
USER fitmentor
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
