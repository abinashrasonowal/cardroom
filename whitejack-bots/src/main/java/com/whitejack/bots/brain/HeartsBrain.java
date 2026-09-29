package com.whitejack.bots.brain;

import static com.whitejack.bots.brain.Cards.orEmpty;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whitejack.bots.Choice;
import com.whitejack.bots.GameBrain;
import com.whitejack.bots.Move;
import com.whitejack.bots.view.HeartsView;
import com.whitejack.contract.Card;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Options come straight from the view's {@code legal} list. The fallback mirrors
 * {@code HeartsGame.onTimeout}: pass the three highest cards, play the lowest legal one.
 */
public final class HeartsBrain implements GameBrain<HeartsView> {

    @Override
    public String gameId() {
        return "hearts";
    }

    @Override
    public Class<HeartsView> viewType() {
        return HeartsView.class;
    }

    @Override
    public String rules() {
        return "The game is Hearts with 4 players. Each heart taken is 1 point and the queen of spades is 13; "
                + "LOW score wins, and the game ends when someone reaches 100. Taking every heart and the queen "
                + "of spades (shooting the moon) gives 26 to each opponent instead. You must follow suit if you can. "
                + "Hearts cannot be led until broken. Before each hand you pass 3 cards; passing high spades "
                + "(Q, K, A) and high hearts is usually wise.";
    }

    @Override
    public Optional<Choice> choose(HeartsView view, String me) {
        List<Card> legal = orEmpty(view.legal()).stream().sorted().toList();
        if (legal.isEmpty()) return Optional.empty();
        List<Move> options = legal.stream()
                .map(card -> new Move(card.toString(), Cards.json(card)))
                .toList();

        // PASSING is simultaneous: onClock points at only one of the seats still to pass, so a
        // non-empty legal list is the trigger, not the clock.
        if ("PASSING".equals(view.phase()) && legal.size() >= 3) {
            List<Integer> highest = IntStream.range(0, legal.size()).boxed()
                    .sorted(Comparator.<Integer, Card>comparing(legal::get).reversed())
                    .limit(3)
                    .toList();
            String state = describe(view, me) + "\nPass 3 cards " + view.passDirection() + ".";
            return Optional.of(new Choice("pass:" + view.hand(), state, options, 3, highest, HeartsBrain::pass));
        }
        if ("PLAYING".equals(view.phase()) && me.equals(view.onClock())) {
            String key = "play:" + view.hand() + ":" + view.tricksPlayed() + ":" + orEmpty(view.trick()).size();
            return Optional.of(new Choice(key, describe(view, me), options, 1, List.of(0),
                    picked -> Cards.NODES.objectNode().put("type", "play").set("card", picked.get(0).value())));
        }
        return Optional.empty();
    }

    private static ObjectNode pass(List<Move> picked) {
        ObjectNode intent = Cards.NODES.objectNode().put("type", "pass");
        ArrayNode cards = intent.putArray("cards");
        picked.forEach(move -> cards.add(move.value()));
        return intent;
    }

    private static String describe(HeartsView view, String me) {
        List<String> lines = new ArrayList<>();
        lines.add("Hand " + (view.hand() + 1) + ", tricks played " + view.tricksPlayed()
                + ", hearts " + (view.heartsBroken() ? "broken" : "not broken") + ".");
        lines.add("Your cards: " + Cards.list(view.myHand()));
        String scores = orEmpty(view.seats()).stream()
                .map(seat -> (seat.id().equals(me) ? "you" : seat.nick()) + " " + seat.score()
                        + " (+" + seat.handPoints() + " this hand)")
                .collect(Collectors.joining(", "));
        lines.add("Scores: " + scores);
        List<HeartsView.PlayedCard> trick = orEmpty(view.trick());
        if (!trick.isEmpty()) {
            lines.add("Current trick, in order: " + trick.stream()
                    .map(play -> nick(view, play.player(), me) + " " + play.card())
                    .collect(Collectors.joining(", ")));
        } else if ("PLAYING".equals(view.phase())) {
            lines.add("You lead this trick.");
        }
        return String.join("\n", lines);
    }

    private static String nick(HeartsView view, String player, String me) {
        if (player.equals(me)) return "you";
        return orEmpty(view.seats()).stream().filter(seat -> seat.id().equals(player))
                .map(HeartsView.SeatView::nick).findFirst().orElse("?");
    }

    @Override
    public boolean isOver(HeartsView view) {
        return "GAME_OVER".equals(view.phase());
    }
}
