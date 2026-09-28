package com.whitejack.games.highcard;

import com.whitejack.contract.Card;
import com.whitejack.contract.GameEvent;
import com.whitejack.contract.PlayerId;

/**
 * Carries the card by construction, which is exactly why events never go over the wire:
 * this one is hidden information until the hand ends. {@code project} is the only filter (§7).
 */
public record Drew(PlayerId player, Card card) implements GameEvent {}
