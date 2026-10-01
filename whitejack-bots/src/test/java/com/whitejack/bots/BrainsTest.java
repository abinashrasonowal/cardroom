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
import com.whitejack.bots.brain.PokerBrain;
import com.whitejack.bots.view.GinView;
import com.whitejack.bots.view.HcView;
import com.whitejack.bots.view.HeartsView;
import com.whitejack.bots.view.PokerView;
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
    void ginPrefersGinThenKnockThenLeastDeadwoodLeft() throws Exception {
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
    void pokerOffersOnlyTheListedMovesAndFoldsJunkToABigBet() throws Exception {
        PokerBrain brain = new PokerBrain();
        String base = pokerBase("PREFLOP", 220, 200, 180, "[]");

        PokerView junk = view(base + "'myCards':[" + card("SEVEN", "CLUBS") + "," + card("TWO", "DIAMONDS") + "],"
                + "'legal':['fold','call','raise'],'minRaiseTo':380,'maxRaiseTo':920}", PokerView.class);
        Choice choice = brain.choose(junk, "me").orElseThrow();
        List<String> labels = choice.options().stream().map(Move::label).toList();
        assertEquals(List.of("fold", "call 180 (pot becomes 400)", "raise to 380 (adds 360, pot becomes 580)",
                "raise to 400 (adds 380, pot becomes 600)", "raise to 600 (adds 580, pot becomes 800)",
                "all-in: raise to 920 (adds 900, pot becomes 1120)"), labels);
        assertEquals("fold", choice.intent(choice.fallback()).path("type").asText());
        assertEquals(920, choice.intent(List.of(5)).path("to").asInt());
        assertTrue(choice.state().contains("needs 45% equity"), choice.state());

        PokerView aces = view(base + "'myCards':[" + card("ACE", "CLUBS") + "," + card("ACE", "DIAMONDS") + "],"
                + "'legal':['fold','call'],'minRaiseTo':0,'maxRaiseTo':0}", PokerView.class);
        Choice call = brain.choose(aces, "me").orElseThrow();
        assertEquals(2, call.options().size(), "no raise sizes when raising is not listed");
        assertEquals("call", call.intent(call.fallback()).path("type").asText());

        PokerView waiting = view(base.replace("'onClock':'me'", "'onClock':'jo'") + "'myCards':[],'legal':[]}", PokerView.class);
        assertTrue(brain.choose(waiting, "me").isEmpty());
    }

    @Test
    void pokerRaisesTheNutsChecksTrashAndNeverOffersAFreeFold() throws Exception {
        PokerBrain brain = new PokerBrain();
        String board = "[" + String.join(",", card("ACE", "SPADES"), card("KING", "SPADES"), card("SEVEN", "HEARTS"))
                + "]";
        String base = pokerBase("FLOP", 400, 0, 0, board);
        String legal = "'legal':['check','raise'],'minRaiseTo':20,'maxRaiseTo':700}";

        PokerView set = view(base + "'myCards':[" + card("ACE", "CLUBS") + "," + card("ACE", "DIAMONDS") + "]," + legal,
                PokerView.class);
        Choice raise = brain.choose(set, "me").orElseThrow();
        assertEquals("check", raise.options().get(0).label(), "no fold option when checking is free");
        assertEquals("raise", raise.intent(raise.fallback()).path("type").asText());
        assertEquals(200, raise.intent(raise.fallback()).path("to").asInt(), "half the pot");
        assertTrue(raise.state().contains("Your hand: three aces."), raise.state());

        PokerView trash = view(base + "'myCards':[" + card("TWO", "CLUBS") + "," + card("EIGHT", "DIAMONDS") + "],"
                + legal, PokerView.class);
        Choice check = brain.choose(trash, "me").orElseThrow();
        assertEquals("check", check.intent(check.fallback()).path("type").asText());
    }

    private static String pokerBase(String street, int pot, int currentBet, int toCall, String board) {
        String seats = "'seats':[{'index':0,'id':'me','nick':'me','stack':900,'bet':" + (currentBet - toCall)
                + ",'inHand':true},{'index':1,'id':'jo','nick':'jo','stack':800,'bet':" + currentBet
                + ",'inHand':true,'lastAction':'Raise to 200'}]";
        return "{'phase':'BETTING','street':'" + street + "','hand':3,'smallBlind':10,'bigBlind':20,'dealer':'jo',"
                + seats + ",'board':" + board + ",'pot':" + pot + ",'currentBet':" + currentBet
                + ",'onClock':'me','toCall':" + toCall + ",";
    }

    @Test
    void heartsDucksUnderTheWinningCardAndDumpsTheQueenWhenVoid() throws Exception {
        HeartsBrain brain = new HeartsBrain();
        String follow = String.join(",", card("THREE", "CLUBS"), card("NINE", "CLUBS"), card("KING", "CLUBS"));
        HeartsView duck = view("{'phase':'PLAYING','hand':0,'tricksPlayed':3,'seats':[],'myHand':[" + follow
                + "],'legal':[" + follow + "],'trick':[{'player':'jo','card':" + card("TEN", "CLUBS")
                + "}],'onClock':'me'}", HeartsView.class);
        Choice c = brain.choose(duck, "me").orElseThrow();
        assertEquals("NINE", c.intent(c.fallback()).path("card").path("rank").asText());

        String void_ = String.join(",", card("TWO", "HEARTS"), card("QUEEN", "SPADES"), card("ACE", "HEARTS"));
        HeartsView dump = view("{'phase':'PLAYING','hand':0,'tricksPlayed':3,'seats':[],'myHand':[" + void_
                + "],'legal':[" + void_ + "],'trick':[{'player':'jo','card':" + card("TEN", "CLUBS")
                + "}],'onClock':'me'}", HeartsView.class);
        Choice d = brain.choose(dump, "me").orElseThrow();
        JsonNode queen = d.intent(d.fallback()).path("card");
        assertEquals("QUEEN", queen.path("rank").asText());
        assertEquals("SPADES", queen.path("suit").asText());
    }

    @Test
    void heartsKeepsAGuardedQueenInThePass() throws Exception {
        String hand = String.join(",", card("TWO", "SPADES"), card("THREE", "SPADES"), card("FIVE", "SPADES"),
                card("EIGHT", "SPADES"), card("QUEEN", "SPADES"), card("ACE", "HEARTS"), card("KING", "HEARTS"),
                card("KING", "DIAMONDS"));
        HeartsView v = view("{'phase':'PASSING','hand':0,'passDirection':'LEFT','seats':[],'myHand':[" + hand
                + "],'legal':[" + hand + "],'trick':[],'onClock':'someone-else'}", HeartsView.class);
        Choice choice = new HeartsBrain().choose(v, "me").orElseThrow();
        JsonNode cards = choice.intent(choice.fallback()).path("cards");
        for (JsonNode card : cards) {
            assertFalse(card.path("suit").asText().equals("SPADES"), "kept the guarded queen: " + cards);
        }
    }

    @Test
    void ginTakesADiscardThatCompletesAMeldAndShedsTheLeastUsefulCard() throws Exception {
        String hand = String.join(",", card("FIVE", "HEARTS"), card("SIX", "HEARTS"), card("KING", "CLUBS"),
                card("KING", "DIAMONDS"), card("TWO", "SPADES"), card("NINE", "CLUBS"), card("JACK", "DIAMONDS"),
                card("THREE", "CLUBS"), card("ACE", "DIAMONDS"), card("QUEEN", "SPADES"));
        String draw = "{'phase':'DRAW','hand':0,'onClock':'me','seats':[],'myHand':[" + hand + "],'stockCount':31,"
                + "'drawSources':['STOCK','DISCARD'],'discards':[],'knockDiscards':[],'ginDiscards':[],'discardTop':";
        GinBrain brain = new GinBrain();
        Choice meld = brain.choose(view(draw + card("SEVEN", "HEARTS") + "}", GinView.class), "me").orElseThrow();
        assertEquals("discard", meld.intent(meld.fallback()).path("source").asText());
        Choice junk = brain.choose(view(draw + card("EIGHT", "SPADES") + "}", GinView.class), "me").orElseThrow();
        assertEquals("stock", junk.intent(junk.fallback()).path("source").asText());

        // K♣ K♦ K♠ is a set, so the lone Q♠ is the card to shed rather than any king.
        String eleven = String.join(",", card("KING", "CLUBS"), card("KING", "DIAMONDS"), card("KING", "SPADES"),
                card("QUEEN", "SPADES"), card("TWO", "CLUBS"));
        GinView discard = view("{'phase':'DISCARD','hand':0,'onClock':'me','seats':[],'myHand':[" + eleven
                + "],'stockCount':30,'drawSources':[],'discards':[" + eleven + "],'knockDiscards':[],'ginDiscards':[]}",
                GinView.class);
        Choice shed = brain.choose(discard, "me").orElseThrow();
        JsonNode card = shed.intent(shed.fallback()).path("card");
        assertEquals("QUEEN", card.path("rank").asText());
        assertTrue(shed.options().get(0).label().endsWith("(leaves deadwood 32)"), shed.options().get(0).label());
    }

    @Test
    void everyGameTheServerShipsHasABrain() {
        for (String game : List.of("high-card", "hearts", "gin-rummy", "poker")) {
            assertTrue(Brains.forGame(game).isPresent(), game);
        }
    }
}
