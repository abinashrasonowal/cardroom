package com.whitejack.games.highcard;

import com.whitejack.contract.Intent;

/** The only move in the game: take the top card. Wire form is {@code {"type":"draw"}}. */
public record Draw() implements Intent {}
