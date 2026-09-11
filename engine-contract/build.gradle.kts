// parseIntent(JsonNode) is the JSON boundary, so the contract needs Jackson.
// It is the only non-JDK dependency any game module inherits.
dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.22.2")
}
