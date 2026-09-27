rootProject.name = "cardroom"

include("engine-contract", "engine-core", "games-high-card", "games-hearts")
include("cardroom-server", "cardroom-ui")

// Flat project names, nested directories: no empty ":games" container project.
project(":games-high-card").projectDir = file("games/high-card")
project(":games-hearts").projectDir = file("games/hearts")
