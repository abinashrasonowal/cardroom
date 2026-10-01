package com.whitejack.bots.brain;

import static com.whitejack.bots.brain.Cards.orEmpty;

import com.whitejack.bots.Choice;
import com.whitejack.bots.GameBrain;
import com.whitejack.bots.Move;
import com.whitejack.bots.view.GinView;
import com.whitejack.contract.Card;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Options come from the view's {@code drawSources} and discard lists, so every one is legal. Each
 * option tells the advisor the deadwood it leaves. The fallback takes the top discard only when it
 * lands in a meld and lowers deadwood, goes gin or knocks when the view says it may, and otherwise
 * throws the card that leaves the least deadwood.
 */
public final class GinBrain implements GameBrain<GinView> {

    @Override
    public String gameId() {
        return "gin-rummy";
    }

    @Override
    public Class<GinView> viewType() {
        return GinView.class;
    }

    @Override
    public String rules() {
        return "The game is 2-player Gin Rummy to 100 points. Aces are low. Melds are 3+ of a rank or 3+ in "
                + "sequence of one suit; unmatched cards are deadwood (face cards 10, ace 1). Each turn you draw "
                + "(stock or the top discard) then discard. You may knock with 10 or less deadwood, and going gin "
                + "(no deadwood) scores a bonus; knocking early usually wins, but a defender who lays off to "
                + "lower deadwood undercuts you. Only take the discard when it completes or extends a meld, since "
                + "taking it shows your opponent what you collect. Keep low cards and near-melds, shed high "
                + "unmatched cards, and avoid discarding cards your opponent has been picking up.";
    }

    @Override
    public Optional<Choice> choose(GinView view, String me) {
        if (!me.equals(view.onClock())) return Optional.empty();
        List<Card> hand = orEmpty(view.myHand());
        String key = view.hand() + ":" + view.phase() + ":" + view.stockCount() + ":" + hand.size();

        List<String> sources = orEmpty(view.drawSources());
        if ("DRAW".equals(view.phase()) && !sources.isEmpty()) {
            int now = GinMelds.minDeadwood(hand);
            Card top = view.discardTop();
            int withTop = -1;
            boolean melds = false;
            if (top != null && hand.size() >= 3) {
                List<Card> taken = new ArrayList<>(hand);
                taken.add(top);
                withTop = GinMelds.afterBestDiscard(taken, top);
                // The top card only helps if the best arrangement after taking it uses it in a meld.
                melds = bestKeeps(taken, top, withTop);
            }
            List<Move> options = new ArrayList<>();
            int stock = 0;
            int discard = -1;
            for (String source : sources) {
                // The view spells DrawSource as the enum name; parseIntent wants lowercase.
                String wire = source.toLowerCase(Locale.ROOT);
                String label;
                if (wire.equals("stock")) {
                    stock = options.size();
                    label = "draw the unknown top card of the stock";
                } else {
                    discard = options.size();
                    label = "take the top discard " + top
                            + (withTop >= 0 ? " (deadwood " + withTop + " after your best discard, now " + now + ")" : "");
                }
                options.add(new Move(label, Cards.NODES.objectNode().put("type", "draw").put("source", wire)));
            }
            int fallback = discard >= 0 && melds && withTop < now ? discard : stock;
            return Optional.of(Choice.single("draw:" + key, describe(view, hand),
                    "Which draw gives you the best chance to complete melds and lower your deadwood?", options, fallback));
        }

        List<Card> discards = orEmpty(view.discards());
        if ("DISCARD".equals(view.phase()) && !discards.isEmpty()) {
            Set<Card> gin = Set.copyOf(orEmpty(view.ginDiscards()));
            Set<Card> knock = Set.copyOf(orEmpty(view.knockDiscards()));
            Map<Card, Integer> left = new HashMap<>();
            for (Card card : discards) {
                List<Card> rest = new ArrayList<>(hand);
                rest.remove(card);
                left.put(card, GinMelds.minDeadwood(rest));
            }
            List<Move> options = new ArrayList<>();
            for (Card card : discards) {
                options.add(discard(card, false, "discard " + card + " (leaves deadwood " + left.get(card) + ")"));
            }
            for (Card card : discards) {
                if (gin.contains(card)) options.add(discard(card, true, "discard " + card + " and go gin"));
                else if (knock.contains(card)) {
                    options.add(discard(card, true, "discard " + card + " and knock with deadwood " + left.get(card)));
                }
            }
            return Optional.of(Choice.single("discard:" + key, describe(view, hand),
                    "Which is the strongest play? Knock or go gin when it wins the hand; otherwise shed the card you need least.",
                    options, fallback(discards, options, left)));
        }
        return Optional.empty();
    }

    /** Whether some least-deadwood discard from {@code taken} leaves {@code top} melded. */
    private static boolean bestKeeps(List<Card> taken, Card top, int target) {
        for (Card card : taken) {
            if (card.equals(top)) continue;
            List<Card> rest = new ArrayList<>(taken);
            rest.remove(card);
            if (GinMelds.minDeadwood(rest) == target && !GinMelds.deadwood(rest).contains(top)) return true;
        }
        return false;
    }

    private static int fallback(List<Card> discards, List<Move> options, Map<Card, Integer> left) {
        int gin = indexOf(options, " and go gin");
        if (gin >= 0) return gin;
        int knock = indexOf(options, " and knock");
        if (knock >= 0) return knock;
        Card best = discards.stream()
                .min(Comparator.comparingInt((Card card) -> left.get(card))
                        .thenComparing(Comparator.comparingInt(GinMelds::points).reversed())
                        .thenComparing(Comparator.reverseOrder()))
                .orElseThrow();
        return discards.indexOf(best); // plain discards come first in options, in the same order
    }

    private static int indexOf(List<Move> options, String marker) {
        for (int i = 0; i < options.size(); i++) if (options.get(i).label().contains(marker)) return i;
        return -1;
    }

    private static Move discard(Card card, boolean knock, String label) {
        return new Move(label, Cards.NODES.objectNode().put("type", "discard").put("knock", knock)
                .set("card", Cards.json(card)));
    }

    private static String describe(GinView view, List<Card> hand) {
        List<String> lines = new ArrayList<>();
        lines.add("Hand " + (view.hand() + 1) + ". Stock has " + view.stockCount() + " cards; discard pile "
                + view.discardCount() + ". Top discard: " + (view.discardTop() == null ? "none" : view.discardTop()) + ".");
        lines.add("Your cards: " + Cards.list(hand));
        if (view.myMelds() != null) {
            String melds = orEmpty(view.myMelds().melds()).stream().map(Cards::list).collect(Collectors.joining(" | "));
            lines.add("Best melds: " + (melds.isEmpty() ? "none" : melds) + "; deadwood "
                    + Cards.list(view.myMelds().deadwood()) + " = " + view.myMelds().deadwoodPoints() + " points.");
        }
        if (view.takenFromDiscard() != null) {
            lines.add("You took " + view.takenFromDiscard() + " from the discard pile and may not throw it back.");
        }
        lines.add("Scores: " + orEmpty(view.seats()).stream()
                .map(seat -> seat.nick() + " " + seat.score() + " (" + seat.cardCount() + " cards)")
                .collect(Collectors.joining(", ")));
        return String.join("\n", lines);
    }

    @Override
    public boolean isOver(GinView view) {
        return "GAME_OVER".equals(view.phase());
    }
}
