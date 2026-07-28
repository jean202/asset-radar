FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY gradlew gradle.properties* ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
# webhook-notify-core resolves from GitHub Packages, which requires auth even for
# public packages. Mounted as a build secret so the token never lands in a layer.
RUN --mount=type=secret,id=gpr_user \
    --mount=type=secret,id=gpr_token \
    GITHUB_ACTOR="$(cat /run/secrets/gpr_user 2>/dev/null || true)" \
    GITHUB_TOKEN="$(cat /run/secrets/gpr_token 2>/dev/null || true)" \
    ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
