package com.whitejack.bots.brain;

import static com.whitejack.bots.brain.Cards.orEmpty;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whitejack.bots.Choice;
import com.whitejack.bots.GameBrain;
import com.whitejack.bots.Move;
import com.whitejack.bots.view.PokerView;
import com.whitejack.contract.Card;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Options come straight from the view: fold (unless checking is free), check or call, and — when
 * {@code legal} allows a raise — a few sizes between {@code minRaiseTo} and {@code maxRaiseTo}, so
 * every option is a move the server already said is legal. The advisor is told the made hand, a
 * Monte Carlo equity against random hands, the pot odds and position; the fallback plays from
 * the same numbers: raise well above a fair share, call when equity beats the price, else check
 * or fold.
 */
public final class PokerBrain implements GameBrain<PokerView> {

    private static final int ITERATIONS = 1500;
    /** Equity over a fair share (1 / players) at which the fallback raises for value. */
    private static final double RAISE_SHARE = 1.3;
    /** Facing a bet of half the pot or more, a re-raise needs more. */
    private static final double RERAISE_SHARE = 1.6;
    /** Equity above the break-even price before the fallback calls. */
    private static final double CALL_MARGIN = 0.05;

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
                + "double every 10 hands, so waiting forever is not free. Call when your equity beats the pot odds; "
                + "raise strong hands for value; fold when a big bet leaves you behind the price. Equity is quoted "
                + "against random hands, and a player who bets or raises usually holds better than random, so "
                + "discount it against aggression. Acting later is an advantage. Do not bluff off your whole stack.";
    }

    @Override
    public Optional<Choice> choose(PokerView view, String me) {
        List<String> legal = orEmpty(view.legal());
        if (!"BETTING".equals(view.phase()) || !me.equals(view.onClock()) || legal.isEmpty()) return Optional.empty();

        PokerView.SeatView mine = seat(view, me);
        int myBet = mine == null ? 0 : mine.bet();
        int myStack = mine == null ? 0 : mine.stack();
        int opponents = (int) live(view).stream().filter(s -> !s.id().equals(me)).count();
        double equity = PokerOdds.equity(orEmpty(view.myCards()), orEmpty(view.board()), Math.max(1, opponents),
                ITERATIONS, ThreadLocalRandom.current());

        List<Move> options = new ArrayList<>();
        boolean free = legal.contains("check");
        int fold = -1;
        if (!free) {
            fold = options.size();
            options.add(new Move("fold", intent("fold")));
        }
        int passive = options.size();
        options.add(free ? new Move("check", intent("check"))
                : new Move("call " + view.toCall() + " (pot becomes " + (view.pot() + view.toCall()) + ")", intent("call")));

        int half = -1;
        int allIn = -1;
        int minRaise = -1;
        if (legal.contains("raise")) {
            // Minimum, half-pot, pot and all-in; sizes outside the legal range or duplicated collapse.
            int after = view.pot() + view.toCall();
            int halfPot = view.currentBet() + after / 2;
            int potSized = view.currentBet() + after;
            Set<Integer> sizes = new LinkedHashSet<>();
            sizes.add(view.minRaiseTo());
            if (halfPot > view.minRaiseTo() && halfPot < view.maxRaiseTo()) sizes.add(halfPot);
            if (potSized > view.minRaiseTo() && potSized < view.maxRaiseTo()) sizes.add(potSized);
            sizes.add(view.maxRaiseTo());
            for (int to : sizes) {
                if (to == view.minRaiseTo()) minRaise = options.size();
                if (to == halfPot) half = options.size();
                if (to == view.maxRaiseTo()) allIn = options.size();
                int adds = to - myBet;
                String label = (to == view.maxRaiseTo() ? "all-in: raise to " : "raise to ") + to
                        + " (adds " + adds + ", pot becomes " + (view.pot() + adds) + ")";
                options.add(new Move(label, intent("raise").put("to", to)));
            }
        }

        double share = equity * (Math.max(1, opponents) + 1);
        double price = free ? 0 : (double) view.toCall() / (view.pot() + view.toCall());
        boolean facingBigBet = view.toCall() * 2 >= view.pot();
        int fallback;
        if (allIn >= 0 && share >= (facingBigBet ? RERAISE_SHARE : RAISE_SHARE)) {
            fallback = myStack <= view.pot() ? allIn : half >= 0 ? half : minRaise;
        } else if (free || equity >= price + CALL_MARGIN) {
            fallback = passive;
        } else {
            fallback = fold >= 0 ? fold : passive;
        }

        String key = "act:" + view.hand() + ":" + view.street() + ":" + view.pot() + ":" + view.currentBet();
        return Optional.of(Choice.single(key, describe(view, me, equity, opponents, myStack),
                "Which move wins the most chips over the long run? Weigh your equity against the pot odds.",
                options, fallback));
    }

    private static ObjectNode intent(String type) {
        return Cards.NODES.objectNode().put("type", type);
    }

    private static PokerView.SeatView seat(PokerView view, String id) {
        return orEmpty(view.seats()).stream().filter(s -> s.id().equals(id)).findFirst().orElse(null);
    }

    /** Still contesting the pot, in seat order. */
    private static List<PokerView.SeatView> live(PokerView view) {
        return orEmpty(view.seats()).stream().filter(s -> s.inHand() && !s.folded()).toList();
    }

    private static String describe(PokerView view, String me, double equity, int opponents, int myStack) {
        List<Card> cards = orEmpty(view.myCards());
        List<Card> board = orEmpty(view.board());
        List<String> lines = new ArrayList<>();
        lines.add("Hand " + (view.hand() + 1) + ", " + view.street().toLowerCase() + ", blinds " + view.smallBlind()
                + "/" + view.bigBlind() + ".");
        lines.add("Your cards: " + Cards.list(cards) + startingHand(cards));
        lines.add("Board: " + Cards.list(board));
        if (cards.size() == 2 && !board.isEmpty()) {
            List<Card> all = new ArrayList<>(cards);
            all.addAll(board);
            boolean onBoard = board.size() >= 3
                    && PokerOdds.category(PokerOdds.score(all)) == PokerOdds.category(PokerOdds.score(board));
            lines.add("Your hand: " + PokerOdds.describe(all) + (onBoard ? " (all on the board; your cards only kick)" : "") + ".");
        }
        if (cards.size() == 2) {
            int players = Math.max(1, opponents) + 1;
            lines.add("Estimated equity against " + Math.max(1, opponents) + " random hand" + (opponents > 1 ? "s" : "")
                    + ": " + Cards.pct(equity) + " (a fair share is " + Cards.pct(1.0 / players) + ").");
        }
        if (view.toCall() == 0) {
            lines.add("Pot " + view.pot() + ", nothing to call.");
        } else {
            int win = view.pot() + view.toCall();
            lines.add("Pot " + view.pot() + ", " + view.toCall() + " to call: calling " + view.toCall() + " to win "
                    + win + " needs " + Cards.pct((double) view.toCall() / win) + " equity.");
        }
        if (view.pot() > 0) {
            lines.add("Your stack is " + String.format("%.1f", (double) myStack / view.pot()) + "x the pot.");
        }
        lines.add(position(view, me));
        String table = orEmpty(view.seats()).stream()
                .filter(PokerView.SeatView::inHand)
                .map(seat -> (seat.id().equals(me) ? "you" : seat.nick()) + " " + seat.stack() + " chips"
                        + (seat.folded() ? " (folded)" : seat.allIn() ? " (all-in)" : "")
                        + (seat.bet() > 0 ? ", bet " + seat.bet() : "")
                        + (seat.lastAction() != null && !seat.id().equals(me) ? ", last: " + seat.lastAction() : "")
                        + (seat.id().equals(view.dealer()) ? ", button" : ""))
                .collect(Collectors.joining("; "));
        lines.add("Table: " + table);
        return String.join("\n", lines);
    }

    private static String startingHand(List<Card> cards) {
        if (cards.size() != 2) return "";
        if (cards.get(0).rank() == cards.get(1).rank()) return " (pocket pair)";
        return cards.get(0).suit() == cards.get(1).suit() ? " (suited)" : " (offsuit)";
    }

    /** Where {@code me} sits in the post-flop order, which starts left of the button. */
    private static String position(PokerView view, String me) {
        List<PokerView.SeatView> live = live(view);
        List<PokerView.SeatView> all = orEmpty(view.seats());
        int button = 0;
        for (int i = 0; i < all.size(); i++) if (all.get(i).id().equals(view.dealer())) button = all.get(i).index();
        int b = button;
        List<PokerView.SeatView> order = live.stream()
                .sorted(Comparator.comparingInt((PokerView.SeatView s) -> Math.floorMod(s.index() - b - 1, Math.max(1, all.size()))))
                .toList();
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).id().equals(me)) {
                return "Position: you act " + ordinal(i + 1) + " of " + order.size() + " after the flop"
                        + (i == order.size() - 1 ? " (last, the best seat)." : ".");
            }
        }
        return "Position: unknown.";
    }

    private static String ordinal(int n) {
        return n + (n == 1 ? "st" : n == 2 ? "nd" : n == 3 ? "rd" : "th");
    }

    @Override
    public boolean isOver(PokerView view) {
        return "GAME_OVER".equals(view.phase());
    }
}
