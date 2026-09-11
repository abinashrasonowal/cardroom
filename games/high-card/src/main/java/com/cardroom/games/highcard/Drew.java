package com.cardroom.games.highcard;

import com.cardroom.contract.Card;
import com.cardroom.contract.GameEvent;
import com.cardroom.contract.PlayerId;

/**
 * Carries the card by construction, which is exactly why events never go over the wire:
 * this one is hidden information until the hand ends. {@code project} is the only filter (§7).
 */
public record Drew(PlayerId player, Card card) implements GameEvent {}
