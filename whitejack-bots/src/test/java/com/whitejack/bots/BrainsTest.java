package com.whitejack.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whitejack.bots.brain.GinBrain;
import com.whitejack.bots.brain.HeartsBrain;
import com.whitejack.bots.brain.HighCardBrain;
import com.whitejack.bots.view.GinView;
import com.whitejack.bots.view.HcView;
import com.whitejack.bots.view.HeartsView;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Views written the way the server serializes them; intents checked against each parseIntent. */
class BrainsTest {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static <V> V view(String json, Class<V> type) throws Exception {
        return JSON.readValue(json.replace('\'', '"'), type);
    }

    private static String card(String rank, String suit) {
        return "{'rank':'" + rank + "','suit':'" + suit + "'}";
    }

    @Test
    void highCardDrawsOnlyWhenOnTheClock() throws Exception {
        HighCardBrain brain = new HighCardBrain();
        HcView mine = view("{'seats':[],'handComplete':false,'winner':null,'onClock':'me'}", HcView.class);
        Choice choice = brain.choose(mine, "me").orElseThrow();
        assertTrue(choice.isForced());
        assertEquals("draw", choice.intent(choice.fallback()).path("type").asText());
        assertTrue(brain.choose(mine, "you").isEmpty());
    }

    @Test
    void heartsPassIsThreeDistinctCardsAndDefaultsToTheHighest() throws Exception {
        String hand = String.join(",", card("TWO", "CLUBS"), card("QUEEN", "SPADES"), card("ACE", "HEARTS"),
                card("FIVE", "DIAMONDS"), card("KING", "SPADES"));
        HeartsView v = view("{'phase':'PASSING','hand':0,'passDirection':'LEFT','seats':[],'myHand':[" + hand
                + "],'legal':[" + hand + "],'trick':[],'onClock':'someone-else'}", HeartsView.class);
        Choice choice = new HeartsBrain().choose(v, "me").orElseThrow();
        assertEquals(3, choice.picks());
        JsonNode pass = choice.intent(choice.fallback());
        assertEquals("pass", pass.path("type").asText());
        List<String> ranks = List.of(pass.path("cards").get(0).path("rank").asText(),
                pass.path("cards").get(1).path("rank").asText(), pass.path("cards").get(2).path("rank").asText());
        assertTrue(ranks.containsAll(List.of("ACE", "KING", "QUEEN")), ranks.toString());
    }

    @Test
    void heartsPlayDefaultsToTheLowestLegalCard() throws Exception {
        String legal = String.join(",", card("KING", "CLUBS"), card("THREE", "CLUBS"));
        HeartsView v = view("{'phase':'PLAYING','hand':0,'tricksPlayed':2,'seats':[],'myHand':[" + legal
                + "],'legal':[" + legal + "],'trick':[],'onClock':'me'}", HeartsView.class);
        Choice choice = new HeartsBrain().choose(v, "me").orElseThrow();
        JsonNode play = choice.intent(choice.fallback());
        assertEquals("play", play.path("type").asText());
        assertEquals("THREE", play.path("card").path("rank").asText());
        assertTrue(new HeartsBrain().choose(v, "not-me").isEmpty());
    }

    @Test
    void ginDrawSourcesAreLowercasedForParseIntent() throws Exception {
        GinView v = view("{'phase':'DRAW','hand':0,'onClock':'me','seats':[],'myHand':[],'stockCount':31,"
                + "'discardTop':" + card("NINE", "HEARTS") + ",'drawSources':['STOCK','DISCARD'],"
                + "'discards':[],'knockDiscards':[],'ginDiscards':[]}", GinView.class);
        Choice choice = new GinBrain().choose(v, "me").orElseThrow();
        assertEquals(2, choice.options().size());
        assertEquals("stock", choice.intent(choice.fallback()).path("source").asText());
        assertEquals("discard", choice.intent(List.of(1)).path("source").asText());
    }

    @Test
    void ginPrefersGinThenKnockThenHeaviestDeadwood() throws Exception {
        String four = card("FOUR", "CLUBS");
        String king = card("KING", "HEARTS");
        String base = "{'phase':'DISCARD','hand':0,'onClock':'me','seats':[],'myHand':[" + four + "," + king + "],"
                + "'myMelds':{'melds':[],'deadwood':[" + four + "," + king + "],'deadwoodPoints':14},"
                + "'stockCount':30,'drawSources':[],'discards':[" + four + "," + king + "],";
        GinBrain brain = new GinBrain();

        GinView plain = view(base + "'knockDiscards':[],'ginDiscards':[]}", GinView.class);
        JsonNode shed = brain.choose(plain, "me").orElseThrow().intent(brain.choose(plain, "me").orElseThrow().fallback());
        assertEquals("KING", shed.path("card").path("rank").asText());
        assertFalse(shed.path("knock").asBoolean());

        GinView knock = view(base + "'knockDiscards':[" + king + "],'ginDiscards':[]}", GinView.class);
        Choice k = brain.choose(knock, "me").orElseThrow();
        assertTrue(k.intent(k.fallback()).path("knock").asBoolean());

        GinView gin = view(base + "'knockDiscards':[" + four + "," + king + "],'ginDiscards':[" + four + "]}", GinView.class);
        Choice g = brain.choose(gin, "me").orElseThrow();
        JsonNode goGin = g.intent(g.fallback());
        assertEquals("FOUR", goGin.path("card").path("rank").asText());
        assertTrue(goGin.path("knock").asBoolean());
    }

    @Test
    void everyGameTheServerShipsHasABrain() {
        for (String game : List.of("high-card", "hearts", "gin-rummy")) {
            assertTrue(Brains.forGame(game).isPresent(), game);
        }
    }
}
