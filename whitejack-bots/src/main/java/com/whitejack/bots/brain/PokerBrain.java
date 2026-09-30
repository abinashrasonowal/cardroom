package com.whitejack.bots.brain;

import static com.whitejack.bots.brain.Cards.orEmpty;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whitejack.bots.Choice;
import com.whitejack.bots.GameBrain;
import com.whitejack.bots.Move;
import com.whitejack.bots.view.PokerView;
import com.whitejack.contract.Card;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Options come straight from the view: fold, check or call, and — when {@code legal} allows a
 * raise — a few sizes between {@code minRaiseTo} and {@code maxRaiseTo}, so every option is a
 * move the server already said is legal. The fallback is deliberately tight: continue with a
 * pair or better (or big cards before the flop), check when it is free, fold otherwise.
 */
public final class PokerBrain implements GameBrain<PokerView> {

    @Override
    public String gameId() {
        return "poker";
    }

    @Override
    public Class<PokerView> viewType() {
        return PokerView.class;
    }

    @Override
    public String rules() {
        return "The game is no-limit Texas Hold'em, played until one player has every chip. You hold 2 private "
                + "cards; 5 community cards come out over the flop (3), turn (1) and river (1), with a betting round "
                + "before each and after the last. Your hand is the best 5 of your 2 cards plus the board. On your "
                + "move you may fold, check (when nothing is owed), call, or raise to a new street total. Blinds "
                + "double every 10 hands, so waiting forever is not free. Fold weak hands facing big bets, value-bet "
                + "strong ones, and do not bluff off your whole stack.";
    }

    @Override
    public Optional<Choice> choose(PokerView view, String me) {
        List<String> legal = orEmpty(view.legal());
        if (!"BETTING".equals(view.phase()) || !me.equals(view.onClock()) || legal.isEmpty()) return Optional.empty();

        List<Move> options = new ArrayList<>();
        options.add(new Move("fold", intent("fold")));
        boolean free = legal.contains("check");
        options.add(free ? new Move("check", intent("check")) : new Move("call " + view.toCall(), intent("call")));
        if (legal.contains("raise")) {
            // A minimum raise, a pot-sized one, and all-in; duplicates collapse when the stack is short.
            Set<Integer> sizes = new LinkedHashSet<>();
            sizes.add(view.minRaiseTo());
            int potSized = view.currentBet() + view.toCall() + view.pot();
            if (potSized > view.minRaiseTo() && potSized < view.maxRaiseTo()) sizes.add(potSized);
            sizes.add(view.maxRaiseTo());
            for (int to : sizes) {
                String label = to == view.maxRaiseTo() ? "all-in (raise to " + to + ")" : "raise to " + to;
                options.add(new Move(label, intent("raise").put("to", to)));
            }
        }

        int fallback = strong(view) || free || view.toCall() <= view.bigBlind() ? 1 : 0;
        String key = "act:" + view.hand() + ":" + view.street() + ":" + view.pot() + ":" + view.currentBet();
        return Optional.of(Choice.single(key, describe(view, me),
                "What should you do to win the most chips over the long run?", options, fallback));
    }

    private static ObjectNode intent(String type) {
        return Cards.NODES.objectNode().put("type", type);
    }

    /** A pair or better with the board, or before the flop a pocket pair or two cards ten or higher. */
    private static boolean strong(PokerView view) {
        List<Card> mine = orEmpty(view.myCards());
        List<Card> board = orEmpty(view.board());
        if (mine.size() < 2) return false;
        if (mine.get(0).rank() == mine.get(1).rank()) return true;
        if (board.isEmpty()) return mine.stream().allMatch(card -> card.rank().value() >= 10);
        return mine.stream().anyMatch(card -> board.stream().anyMatch(b -> b.rank() == card.rank()));
    }

    private static String describe(PokerView view, String me) {
        List<String> lines = new ArrayList<>();
        lines.add("Hand " + (view.hand() + 1) + ", " + view.street().toLowerCase() + ", blinds " + view.smallBlind()
                + "/" + view.bigBlind() + ".");
        lines.add("Your cards: " + Cards.list(view.myCards()));
        lines.add("Board: " + Cards.list(view.board()));
        lines.add("Pot " + view.pot() + ", " + (view.toCall() == 0 ? "nothing to call" : view.toCall() + " to call") + ".");
        String table = orEmpty(view.seats()).stream()
                .filter(PokerView.SeatView::inHand)
                .map(seat -> (seat.id().equals(me) ? "you" : seat.nick()) + " " + seat.stack() + " chips"
                        + (seat.folded() ? " (folded)" : seat.allIn() ? " (all-in)" : "")
                        + (seat.bet() > 0 ? ", bet " + seat.bet() : "")
                        + (seat.id().equals(view.dealer()) ? ", button" : ""))
                .collect(Collectors.joining("; "));
        lines.add("Table: " + table);
        return String.join("\n", lines);
    }

    @Override
    public boolean isOver(PokerView view) {
        return "GAME_OVER".equals(view.phase());
    }
}
