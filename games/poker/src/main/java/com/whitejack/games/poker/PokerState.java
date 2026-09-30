package com.whitejack.games.poker;

import com.whitejack.contract.Card;
import com.whitejack.contract.PlayerId;
import com.whitejack.contract.Seat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deeply immutable, as engine invariant 2 requires: every collection is re-copied on
 * construction.
 *
 * <p>Who is dealt in is {@code holes.keySet()}: a player with no chips at the deal gets no
 * cards and sits the hand out. Who is still live is dealt in minus {@code folded}; who can
 * still bet is live with chips left.
 *
 * @param seats every seat, in table order; position in this list is the seat
 * @param hand zero-based hand number; -1 before the first deal
 * @param dealer position of the button
 * @param board all five community cards, dealt up front; only {@code street.boardCards()} are face up
 * @param stacks chips in front of each player, not counting what they have bet this hand
 * @param bets chips put in on the current street
 * @param contributed chips put in over the whole hand, blinds included, which is what the pots are built from
 * @param acted who has acted on this street since the last raise
 * @param lastActions each player's latest action on this street, for the table ("Call 40")
 * @param currentBet the street total everyone must match to stay in
 * @param minRaise the smallest legal raise increment: the big blind, or the last full raise
 * @param toAct whose move it is; null unless BETTING
 * @param lastHand how the previous hand ended; null before the first one finishes
 */
public record PokerState(
        List<Seat> seats,
        int hand,
        PokerPhase phase,
        Street street,
        int dealer,
        int smallBlind,
        int bigBlind,
        Map<PlayerId, List<Card>> holes,
        List<Card> board,
        Map<PlayerId, Integer> stacks,
        Map<PlayerId, Integer> bets,
        Map<PlayerId, Integer> contributed,
        Set<PlayerId> folded,
        Set<PlayerId> acted,
        Map<PlayerId, String> lastActions,
        int currentBet,
        int minRaise,
        PlayerId toAct,
        HandResult lastHand) {

    public PokerState {
        seats = List.copyOf(seats);
        holes = copyHoles(holes);
        board = List.copyOf(board);
        stacks = Map.copyOf(stacks);
        bets = Map.copyOf(bets);
        contributed = Map.copyOf(contributed);
        folded = Set.copyOf(folded);
        acted = Set.copyOf(acted);
        lastActions = Map.copyOf(lastActions);
    }

    /** Before the first deal: everyone on a starting stack. {@code createInitialState} folds a Dealt onto this. */
    static PokerState seated(List<Seat> seats, int stack) {
        Map<PlayerId, Integer> stacks = new LinkedHashMap<>();
        for (Seat seat : seats) stacks.put(seat.id(), stack);
        return new PokerState(seats, -1, PokerPhase.SETTLED, Street.PREFLOP, seats.size() - 1, 0, 0, Map.of(),
                List.of(), stacks, Map.of(), Map.of(), Set.of(), Set.of(), Map.of(), 0, 0, null, null);
    }

    static Map<PlayerId, List<Card>> copyHoles(Map<PlayerId, List<Card>> holes) {
        Map<PlayerId, List<Card>> copy = new LinkedHashMap<>();
        holes.forEach((player, cards) -> copy.put(player, List.copyOf(cards)));
        return Map.copyOf(copy);
    }

    /** Position in {@link #seats}, or -1 for someone not at the table. */
    int positionOf(PlayerId player) {
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).id().equals(player)) return i;
        }
        return -1;
    }

    PlayerId idAt(int position) {
        return seats.get(position).id();
    }

    int stackOf(PlayerId player) {
        return stacks.getOrDefault(player, 0);
    }

    int betOf(PlayerId player) {
        return bets.getOrDefault(player, 0);
    }

    boolean isDealtIn(PlayerId player) {
        return holes.containsKey(player);
    }

    boolean isLive(PlayerId player) {
        return isDealtIn(player) && !folded.contains(player);
    }

    /** Live with chips behind: the players who still have decisions to make. */
    boolean canBet(PlayerId player) {
        return isLive(player) && stackOf(player) > 0;
    }

    /** Community cards face up right now. */
    List<Card> visibleBoard() {
        return board.subList(0, Math.min(street.boardCards(), board.size()));
    }

    int pot() {
        return contributed.values().stream().mapToInt(Integer::intValue).sum();
    }
}
