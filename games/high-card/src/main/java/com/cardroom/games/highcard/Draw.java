package com.cardroom.games.highcard;

import com.cardroom.contract.Intent;

/** The only move in the game: take the top card. Wire form is {@code {"type":"draw"}}. */
public record Draw() implements Intent {}
