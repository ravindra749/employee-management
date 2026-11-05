# Two stages: build the jar with the JDK, then run it on a smaller JRE image.
# Tests are not run here; CI runs the full suite before this image is built.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first, so they are cached until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an ordinary user, not root.
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /workspace/target/demo-0.0.1-SNAPSHOT.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
