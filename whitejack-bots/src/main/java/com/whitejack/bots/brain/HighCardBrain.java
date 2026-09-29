package com.whitejack.bots.brain;

import com.whitejack.bots.Choice;
import com.whitejack.bots.GameBrain;
import com.whitejack.bots.Move;
import com.whitejack.bots.view.HcView;
import java.util.List;
import java.util.Optional;

/** One legal move, ever: draw when on the clock. The advisor is never asked. */
public final class HighCardBrain implements GameBrain<HcView> {

    @Override
    public String gameId() {
        return "high-card";
    }

    @Override
    public Class<HcView> viewType() {
        return HcView.class;
    }

    @Override
    public String rules() {
        return "High card: everyone draws one card, the highest wins.";
    }

    @Override
    public Optional<Choice> choose(HcView view, String me) {
        if (view.handComplete() || !me.equals(view.onClock())) return Optional.empty();
        Move draw = new Move("draw a card", Cards.NODES.objectNode().put("type", "draw"));
        return Optional.of(Choice.single("draw", "Your turn to draw.", "Draw?", List.of(draw), 0));
    }

    /** High card deals no second hand, so a finished hand is the end of the bot's work. */
    @Override
    public boolean isOver(HcView view) {
        return view.handComplete();
    }
}
