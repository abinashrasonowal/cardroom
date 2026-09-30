package com.whitejack.games.poker;

import com.whitejack.contract.Intent;

/**
 * The four moves in Hold'em. Wire forms: {@code {"type":"fold"}}, {@code {"type":"check"}},
 * {@code {"type":"call"}} and {@code {"type":"raise","to":120}}. A raise names the street total
 * the player is raising <em>to</em>, not the increment; an opening bet is a raise from zero, and
 * all-in is a raise to everything the player has in front of them.
 */
public sealed interface PokerIntent extends Intent {

    record Fold() implements PokerIntent {}

    record Check() implements PokerIntent {}

    record Call() implements PokerIntent {}

    record Raise(int to) implements PokerIntent {}
}
