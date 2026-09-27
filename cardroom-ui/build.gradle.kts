plugins {
    base
}

val npmInstall = tasks.register<Exec>("npmInstall") {
    workingDir = file(".")
    commandLine("npm", "install")
    inputs.file("package.json")
    outputs.dir("node_modules")
}

val buildReact = tasks.register<Exec>("buildReact") {
    dependsOn(npmInstall)
    workingDir = file(".")
    commandLine("npm", "run", "build")
    inputs.dir("src")
    inputs.file("package.json")
    inputs.file("vite.config.ts")
    inputs.file("tsconfig.json")
    inputs.file("index.html")
    outputs.dir("dist")
}

tasks.assemble {
    dependsOn(buildReact)
}

tasks.clean {
    delete("dist")
}
