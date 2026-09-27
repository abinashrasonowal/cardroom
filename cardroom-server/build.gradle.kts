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
    runtimeOnly(project(":games-hearts"))
    runtimeOnly(project(":games-gin-rummy"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// The React build is bundled as static/.
//   -PskipUi      builds the jar with no UI at all (Java-only work, tests).
//   -PprebuiltUi  bundles an existing cardroom-ui/dist without running npm (the Docker build,
//                 where a Node stage has already produced it).
if (!project.hasProperty("skipUi")) {
    val ui = project(":cardroom-ui")
    tasks.processResources {
        if (!project.hasProperty("prebuiltUi")) dependsOn(ui.tasks.named("buildReact"))
        inputs.dir(ui.file("dist"))
        from(ui.file("dist")) { into("static") }
    }
}
