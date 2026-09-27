rootProject.name = "cardroom"

include("engine-contract", "engine-core", "games-high-card", "games-hearts", "games-gin-rummy")
include("cardroom-server", "cardroom-ui")

// Flat project names, nested directories: no empty ":games" container project.
project(":games-high-card").projectDir = file("games/high-card")
project(":games-hearts").projectDir = file("games/hearts")
project(":games-gin-rummy").projectDir = file("games/gin-rummy")
