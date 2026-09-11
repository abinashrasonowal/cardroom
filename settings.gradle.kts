rootProject.name = "cardroom"

include("engine-contract", "engine-core", "games-high-card")

// Flat project names, nested directories: no empty ":games" container project.
project(":games-high-card").projectDir = file("games/high-card")
