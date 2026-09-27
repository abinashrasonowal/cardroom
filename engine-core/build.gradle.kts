dependencies {
    api(project(":engine-contract"))

    // Discovery is tested against a real provider jar, which the compiler never sees.
    testRuntimeOnly(project(":games-high-card"))
}
