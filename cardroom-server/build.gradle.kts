plugins {
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation(project(":engine-core"))

    // Games reach the runtime classpath only; the compiler never sees one (§13).
    runtimeOnly(project(":games-high-card"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// The React build is bundled as static/. -PskipUi builds the jar without npm.
if (!project.hasProperty("skipUi")) {
    val ui = project(":cardroom-ui")
    tasks.processResources {
        dependsOn(ui.tasks.named("buildReact"))
        inputs.dir(ui.file("dist"))
        from(ui.file("dist")) { into("static") }
    }
}
