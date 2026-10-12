package online.core;
import online.protocol.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MultiplayerTest {
    @Test void threePlayersHaveTwoWorkersAndEliminationSkipsTheirTurn() {
        Match m=new Match(List.of("None","None","None"),GameMode.THREE);
        for(int seat=0;seat<3;seat++)for(int col=0;col<2;col++)m.apply(seat,GameAction.at("PLACE",seat*2,col));
        assertEquals(6,m.snapshot().cells().stream().filter(c->c.worker()!=null).count());
        assertEquals(2,m.snapshot().cells().get(20).owner());
        m.resign(0); assertNull(m.snapshot().winner());assertEquals(1,m.snapshot().turn());
        assertEquals(List.of(0),m.snapshot().eliminated());
        assertEquals(4,m.snapshot().cells().stream().filter(c->c.worker()!=null).count());
        m.resign(1);assertEquals(List.of(2),m.winners());
    }
    @Test void fourPlayerTeamsShareWinsAndUseOneWorkerEach() {
        Match m=new Match(Collections.nCopies(4,"None"),GameMode.TEAMS);
        for(int seat=0;seat<4;seat++)m.apply(seat,GameAction.at("PLACE",seat,0));
        assertEquals(1,m.snapshot().workersPerPlayer());assertEquals(4,m.snapshot().cells().stream().filter(c->c.worker()!=null).count());
        m.resign(0);assertEquals(List.of(1,3),m.winners());
    }
    @Test void fourPlayerFreeForAllContinuesAfterAForfeit() {
        Match m=new Match(Collections.nCopies(4,"None"),GameMode.FOUR_FFA);
        for(int seat=0;seat<4;seat++)m.apply(seat,GameAction.at("PLACE",seat,0));
        m.resign(1);assertNull(m.snapshot().winner());assertEquals(3,m.snapshot().cells().stream().filter(c->c.worker()!=null).count());
        m.resign(2);m.resign(3);assertEquals(List.of(0),m.winners());
    }
    @Test void leavingDuringPlacementDoesNotSkipUnplacedWorkers() {
        Match m=new Match(Collections.nCopies(3,"None"),GameMode.THREE);
        m.resign(0);
        for(int seat=1;seat<3;seat++)for(int col=0;col<2;col++)m.apply(seat,GameAction.at("PLACE",seat,col));
        assertEquals("SELECT",m.snapshot().phase());assertEquals(1,m.snapshot().turn());
    }
    @Test void blockedPlayerLeavesFreeForAllAndComputerTakesAWinningStep() throws Exception {
        Match m=new Match(Collections.nCopies(4,"None"),GameMode.FOUR_FFA);
        m.apply(0,GameAction.at("PLACE",0,0));m.apply(1,GameAction.at("PLACE",0,4));m.apply(2,GameAction.at("PLACE",4,0));
        var field=Match.class.getDeclaredField("board");field.setAccessible(true);
        var board=(Frame.Boards.Board)field.get(m);
        for(int[] tile:new int[][]{{0,1},{1,0},{1,1}})for(int i=0;i<4;i++)board.getTile(tile[0],tile[1]).getBuilding().build();
        m.apply(3,GameAction.at("PLACE",4,4));assertEquals(List.of(0),m.snapshot().eliminated());assertEquals(1,m.snapshot().turn());assertNull(m.snapshot().winner());
        for(int i=0;i<2;i++)board.getTile(0,4).getBuilding().build();
        for(int i=0;i<3;i++)board.getTile(1,4).getBuilding().build();
        m.apply(1,GameAction.select(1));GameAction chosen=ComputerPlayer.choose(m,0);
        assertEquals(GameAction.at("MOVE",1,4),chosen);m.apply(1,chosen);assertEquals(List.of(1),m.winners());
    }
    @Test void computerAlwaysChoosesLegalActionsAndBoundsOptionalTurns() {
        for(List<String> gods:List.of(List.of("None","None"),List.of("Demeter","Triton"))) {
            Match m=new Match(gods);int previous=-1,actions=0;
            for(int i=0;i<2500 && m.snapshot().winner()==null;i++) {
                int turn=m.snapshot().turn();if(turn!=previous){previous=turn;actions=0;}
                GameAction a=ComputerPlayer.choose(m,actions++);assertTrue(m.legalActions().contains(a));m.apply(turn,a);
                assertTrue(actions<30,"Optional moves must finish");
            }
        }
    }
}
