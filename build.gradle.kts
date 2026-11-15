plugins {
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    java
}

group = "io.github.jean202.assetradar"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    // webhook-notify is published to GitHub Packages, which requires authentication
    // even for public packages. Supply a token with read:packages via gpr.user/gpr.key
    // in ~/.gradle/gradle.properties, or GITHUB_ACTOR/GITHUB_TOKEN in CI.
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/jean202/webhook-notify")
        credentials {
            username = providers.gradleProperty("gpr.user")
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .orNull
            password = providers.gradleProperty("gpr.key")
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .orNull
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.6")
    implementation("io.github.jean202:webhook-notify-core:0.1.1")

    runtimeOnly("org.postgresql:r2dbc-postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:r2dbc")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // Docker Engine 29+ 는 MinAPIVersion 1.40 미만 요청을 400으로 거부한다.
    // docker-java 기본 협상 버전이 그보다 낮아 Testcontainers가 데몬을 못 찾으므로 명시한다.
    val dockerApiVersion = providers.environmentVariable("DOCKER_API_VERSION").getOrElse("1.44")
    environment("DOCKER_API_VERSION", dockerApiVersion)
    systemProperty("api.version", dockerApiVersion)
}
