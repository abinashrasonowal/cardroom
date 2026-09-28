// whitejack-ui is an npm build driven by Exec tasks; it has no Java sources.
configure(subprojects - project(":whitejack-ui")) {
    apply(plugin = "java-library")

    group = "com.whitejack"

    repositories { mavenCentral() }

    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion = JavaLanguageVersion.of(17) }
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.14.4")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}

// Architecture §3, rule 1: a game compiles against engine-contract only — that is what lets
// a game be written without the engine on its classpath. Test configurations stay open.
subprojects.filter { it.name.startsWith("games-") }.forEach { game ->
    game.afterEvaluate {
        configurations.matching { it.name in setOf("api", "implementation", "compileOnly") }.all {
            dependencies.withType<ProjectDependency>().all {
                check(path == ":engine-contract") {
                    "${game.path} may depend only on :engine-contract, not $path"
                }
            }
        }
    }
}
