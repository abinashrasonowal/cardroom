// Headless players that speak the public /ws protocol (§14). The contract supplies Card and
// Jackson; nothing here may reach into engine-core or a game — a bot sees only the wire.
dependencies {
    api(project(":engine-contract"))
}
