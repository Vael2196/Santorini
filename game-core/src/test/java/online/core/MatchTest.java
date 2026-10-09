package online.core;

import Frame.Boards.Board;
import online.protocol.GameAction;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MatchTest {
    private Match placed(String first, String second) {
        Match match = new Match(List.of(first, second));
        match.apply(0, GameAction.at("PLACE", 0, 0));
        match.apply(0, GameAction.at("PLACE", 0, 1));
        match.apply(1, GameAction.at("PLACE", 4, 4));
        match.apply(1, GameAction.at("PLACE", 4, 3));
        return match;
    }
    private Board board(Match match) throws Exception {
        Field field = Match.class.getDeclaredField("board"); field.setAccessible(true); return (Board) field.get(match);
    }
    @Test void setupRejectsOccupiedSpacesAndWrongPlayersWithoutMutation() {
        Match match = new Match(List.of("None", "None"));
        match.apply(0, GameAction.at("PLACE", 0, 0));
        var before = match.snapshot();
        assertThrows(IllegalArgumentException.class, () -> match.apply(1, GameAction.at("PLACE", 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> match.apply(0, GameAction.at("PLACE", 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> match.apply(0, GameAction.at("PLACE", -1, 99)));
        assertEquals(before, match.snapshot());
    }
    @Test void classicRequiresOneMoveAndOneBuildWithOwnedWorker() {
        Match match = placed("None", "None");
        assertThrows(IllegalArgumentException.class, () -> match.apply(0, GameAction.select(2)));
        match.apply(0, GameAction.select(0));
        assertThrows(IllegalArgumentException.class, () -> match.apply(0, GameAction.simple("END_MOVE")));
        match.apply(0, GameAction.at("MOVE", 1, 0));
        assertEquals("BUILD", match.snapshot().phase());
        assertThrows(IllegalArgumentException.class, () -> match.apply(0, GameAction.simple("END_TURN")));
        match.apply(0, GameAction.at("BUILD", 0, 0));
        assertEquals(1, match.snapshot().turn());
        assertEquals("SELECT", match.snapshot().phase());
        assertEquals(1, match.snapshot().cells().get(0).level());
    }
    @Test void artemisCannotReturnToOriginAndMaySkipExtraMove() {
        Match match = placed("Artemis", "None");
        match.apply(0, GameAction.select(0)); match.apply(0, GameAction.at("MOVE", 1, 0));
        assertEquals("MOVE", match.snapshot().phase());
        assertThrows(IllegalArgumentException.class, () -> match.apply(0, GameAction.at("MOVE", 0, 0)));
        match.apply(0, GameAction.simple("END_MOVE"));
        assertEquals("BUILD", match.snapshot().phase());
    }
    @Test void artemisSecondMoveEndsMovement() {
        Match match = placed("Artemis", "None");
        match.apply(0, GameAction.select(0)); match.apply(0, GameAction.at("MOVE", 1, 0));
        match.apply(0, GameAction.at("MOVE", 1, 1));
        assertEquals("BUILD", match.snapshot().phase());
    }
    @Test void demeterSecondBuildMustUseDifferentSpaceAndIsOptional() {
        Match match = placed("Demeter", "None");
        match.apply(0, GameAction.select(0)); match.apply(0, GameAction.at("MOVE", 1, 0));
        match.apply(0, GameAction.at("BUILD", 0, 0));
        assertEquals("BUILD", match.snapshot().phase());
        assertTrue(match.legalActions().contains(GameAction.simple("END_TURN")));
        var before = match.snapshot();
        assertThrows(IllegalArgumentException.class, () -> match.apply(0, GameAction.at("BUILD", 0, 0)));
        assertEquals(before, match.snapshot());
        match.apply(0, GameAction.at("BUILD", 2, 0));
        assertEquals(1, match.snapshot().turn());
    }
    @Test void tritonRetainsOriginalSameHeightVariant() throws Exception {
        Match match = placed("Triton", "None");
        board(match).getTile(1, 1).getBuilding().build();
        match.apply(0, GameAction.select(0)); match.apply(0, GameAction.at("MOVE", 1, 0));
        assertEquals("MOVE", match.snapshot().phase());
        match.apply(0, GameAction.at("MOVE", 1, 1));
        assertEquals("BUILD", match.snapshot().phase());
    }
    @Test void climbToLevelThreeWinsWithoutExitingTheProcess() throws Exception {
        Match match = placed("None", "None");
        for (int i = 0; i < 2; i++) board(match).getTile(0, 0).getBuilding().build();
        for (int i = 0; i < 3; i++) board(match).getTile(1, 0).getBuilding().build();
        match.apply(0, GameAction.select(0)); match.apply(0, GameAction.at("MOVE", 1, 0));
        assertEquals(0, match.snapshot().winner()); assertEquals("FINISHED", match.snapshot().phase());
        assertTrue(match.legalActions().isEmpty());
    }
    @Test void blockedPlayersLoseAndDomesCannotBeEntered() throws Exception {
        Match match = new Match(List.of("None", "None"));
        match.apply(0, GameAction.at("PLACE", 0, 0)); match.apply(0, GameAction.at("PLACE", 0, 1));
        match.apply(1, GameAction.at("PLACE", 4, 4));
        for (int[] position : new int[][]{{1,0},{1,1},{1,2},{0,2}}) {
            for (int i = 0; i < 4; i++) board(match).getTile(position[0], position[1]).getBuilding().build();
        }
        match.apply(1, GameAction.at("PLACE", 4, 3));
        assertEquals(1, match.snapshot().winner());
    }
    @Test void originalDescentLimitAndIndependentMatchInstancesArePreserved() throws Exception {
        Match first = placed("None", "None"), second = placed("None", "None");
        for (int i = 0; i < 2; i++) board(first).getTile(0,0).getBuilding().build();
        board(first).getTile(1,1).getBuilding().build();
        first.apply(0, GameAction.select(0));
        assertFalse(first.legalActions().contains(GameAction.at("MOVE",1,0)));
        assertEquals("SELECT", second.snapshot().phase());
        assertEquals(0, second.snapshot().cells().get(0).level());
    }
}
