package com.whitejack.bots.brain;

import static com.whitejack.bots.brain.Cards.orEmpty;

import com.whitejack.bots.Choice;
import com.whitejack.bots.GameBrain;
import com.whitejack.bots.Move;
import com.whitejack.bots.view.GinView;
import com.whitejack.contract.Card;
import com.whitejack.contract.Rank;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Options come from the view's {@code drawSources} and discard lists, so every one is legal.
 * The fallback follows {@code GinRummyGame.onTimeout}'s spirit: draw from the stock, go gin or
 * knock when the view says you may, else shed the heaviest deadwood.
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
                + "(no deadwood) scores a bonus. Keep low cards and near-melds; shed high unmatched cards.";
    }

    @Override
    public Optional<Choice> choose(GinView view, String me) {
        if (!me.equals(view.onClock())) return Optional.empty();
        String key = view.hand() + ":" + view.phase() + ":" + view.stockCount() + ":" + orEmpty(view.myHand()).size();

        List<String> sources = orEmpty(view.drawSources());
        if ("DRAW".equals(view.phase()) && !sources.isEmpty()) {
            List<Move> options = new ArrayList<>();
            int stock = 0;
            for (String source : sources) {
                // The view spells DrawSource as the enum name; parseIntent wants lowercase.
                String wire = source.toLowerCase(Locale.ROOT);
                if (wire.equals("stock")) stock = options.size();
                String label = wire.equals("stock") ? "draw the unknown top card of the stock"
                        : "take the top discard " + view.discardTop();
                options.add(new Move(label, Cards.NODES.objectNode().put("type", "draw").put("source", wire)));
            }
            return Optional.of(Choice.single("draw:" + key, describe(view), options, stock));
        }

        List<Card> discards = orEmpty(view.discards());
        if ("DISCARD".equals(view.phase()) && !discards.isEmpty()) {
            Set<Card> gin = Set.copyOf(orEmpty(view.ginDiscards()));
            Set<Card> knock = Set.copyOf(orEmpty(view.knockDiscards()));
            List<Move> options = new ArrayList<>();
            for (Card card : discards) options.add(discard(card, false, "discard " + card));
            for (Card card : discards) {
                if (gin.contains(card)) options.add(discard(card, true, "discard " + card + " and go gin"));
                else if (knock.contains(card)) options.add(discard(card, true, "discard " + card + " and knock"));
            }
            return Optional.of(Choice.single("discard:" + key, describe(view), options, fallback(view, options)));
        }
        return Optional.empty();
    }

    private static int fallback(GinView view, List<Move> options) {
        int gin = indexOf(options, " and go gin");
        if (gin >= 0) return gin;
        int knock = indexOf(options, " and knock");
        if (knock >= 0) return knock;
        Set<Card> deadwood = view.myMelds() == null ? Set.of() : Set.copyOf(orEmpty(view.myMelds().deadwood()));
        List<Card> discards = orEmpty(view.discards());
        Card heaviest = discards.stream()
                .max(Comparator.comparing((Card card) -> deadwood.contains(card))
                        .thenComparingInt(GinBrain::points)
                        .thenComparing(Comparator.naturalOrder()))
                .orElseThrow();
        return discards.indexOf(heaviest); // plain discards come first in options, in the same order
    }

    private static int indexOf(List<Move> options, String suffix) {
        for (int i = 0; i < options.size(); i++) if (options.get(i).label().endsWith(suffix)) return i;
        return -1;
    }

    private static Move discard(Card card, boolean knock, String label) {
        return new Move(label, Cards.NODES.objectNode().put("type", "discard").put("knock", knock)
                .set("card", Cards.json(card)));
    }

    static int points(Card card) {
        if (card.rank() == Rank.ACE) return 1;
        return Math.min(card.rank().value(), 10);
    }

    private static String describe(GinView view) {
        List<String> lines = new ArrayList<>();
        lines.add("Hand " + (view.hand() + 1) + ". Stock has " + view.stockCount() + " cards. Top discard: "
                + (view.discardTop() == null ? "none" : view.discardTop()) + ".");
        lines.add("Your cards: " + Cards.list(view.myHand()));
        if (view.myMelds() != null) {
            String melds = orEmpty(view.myMelds().melds()).stream().map(Cards::list).collect(Collectors.joining(" | "));
            lines.add("Best melds: " + (melds.isEmpty() ? "none" : melds) + "; deadwood "
                    + Cards.list(view.myMelds().deadwood()) + " = " + view.myMelds().deadwoodPoints() + " points.");
        }
        if (view.takenFromDiscard() != null) {
            lines.add("You took " + view.takenFromDiscard() + " from the discard pile and may not throw it back.");
        }
        lines.add("Scores: " + orEmpty(view.seats()).stream().map(seat -> seat.nick() + " " + seat.score())
                .collect(Collectors.joining(", ")));
        return String.join("\n", lines);
    }

    @Override
    public boolean isOver(GinView view) {
        return "GAME_OVER".equals(view.phase());
    }
}
