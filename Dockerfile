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
# The JVM sizes its heap from the container limit (Render instances can have 512 MB). On the free plan the
# CPU is tiny, so the JIT is limited to C1 and the serial GC is used: it starts noticeably faster.
ENV SPRING_PROFILES_ACTIVE=render \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -XX:CICompilerCount=1 -Xss512k -Xshare:auto"
EXPOSE 8080
USER fitmentor
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
