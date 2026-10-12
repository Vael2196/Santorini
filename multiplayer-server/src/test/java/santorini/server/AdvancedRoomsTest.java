package santorini.server;
import online.protocol.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdvancedRoomsTest {
    @Autowired AccountService accounts;
    @Autowired RankingService rankings;
    @Autowired JdbcTemplate db;
    static class Player implements Peer {
        final String id=UUID.randomUUID().toString();
        final ServerEvent.Identity identity;
        final List<ServerEvent> events=new CopyOnWriteArrayList<>();
        Player(ServerEvent.Identity identity){this.identity=identity;}
        public String id(){return id;} public ServerEvent.Identity identity(){return identity;}
        public void send(ServerEvent e){events.add(e);}public void close(){}
        ServerEvent last(String type){return events.stream().filter(e->e.type().equals(type)).reduce((a,b)->b).orElseThrow();}
        RoomState state(){return last("STATE").room();}
    }
    private RoomService service(RoomServiceTest.TestClock clock){return new RoomService(clock,60,100,new NameRegistry(),rankings);}
    private Player guest(RoomService s){return enter(s,null);}
    private Player account(RoomService s,int rating){
        var account=accounts.register("Test-"+UUID.randomUUID().toString().substring(0,8),"a long test passphrase").session().account();
        db.update("UPDATE player_accounts SET rating = ? WHERE id = ?",rating,account.id());
        return enter(s,new ServerEvent.Identity(account.id(),account.name(),false));
    }
    private Player enter(RoomService s,ServerEvent.Identity identity){Player p=new Player(identity);s.receive(p,new ClientCommand("HELLO",null,null,"Guest-"+p.id.substring(0,8),null,null,null,null,null));return p;}
    private void send(RoomService s,Player p,String type,String mode,boolean ranked,String code,String text){
        Long version=p.events.stream().anyMatch(e->e.type().equals("STATE"))?p.state().version():null;
        s.receive(p,new ClientCommand(type,UUID.randomUUID().toString(),null,null,code,"CLASSIC",null,version,null,mode,ranked,text));
    }
    @Test void capacitiesQueuesAndFreeForAllAreIsolated(){
        var s=service(new RoomServiceTest.TestClock());Player a=guest(s),b=guest(s),c=guest(s),d=guest(s);
        send(s,a,"CREATE","THREE",false,null,null);send(s,b,"JOIN",null,false,a.state().id(),null);send(s,c,"JOIN",null,false,a.state().id(),null);
        send(s,d,"JOIN",null,false,a.state().id(),null);assertEquals("ROOM_FULL",d.last("ERROR").code());
        for(Player p:List.of(a,b,c))send(s,p,"READY",null,false,null,null);
        assertEquals(3,a.state().capacity());send(s,b,"RESIGN",null,false,null,null);assertEquals("PLAYING",a.state().status());
        send(s,d,"QUEUE","FOUR_FFA",false,null,null);assertEquals(1,d.state().players().size());
    }
    @Test void soloStartsComputerAndDoesNotUseGuestNameRegistry(){
        var s=service(new RoomServiceTest.TestClock());Player a=guest(s);send(s,a,"SOLO","DUEL",false,null,null);
        assertTrue(a.state().players().get(1).bot());send(s,a,"READY",null,false,null,null);
        for(int col=0;col<2;col++)s.receive(a,new ClientCommand("ACTION",null,null,null,null,null,null,a.state().version(),GameAction.at("PLACE",0,col)));
        s.computerTurns();s.computerTurns();assertEquals(0,a.state().game().turn());assertEquals(4,a.state().game().cells().stream().filter(c->c.worker()!=null).count());
        send(s,a,"LEAVE",null,false,null,null);assertEquals("LEFT",a.last("LEFT").type());
    }
    @Test void rankedQueueFavoursNearbyRatingsAndSettlesExactlyOnce(){
        var clock=new RoomServiceTest.TestClock();var s=service(clock);
        Player a=account(s,1000),far=account(s,1400),near=account(s,1030),guest=guest(s);
        send(s,a,"QUEUE","DUEL",true,null,null);send(s,far,"QUEUE","DUEL",true,null,null);send(s,near,"QUEUE","DUEL",true,null,null);
        assertEquals(a.state().id(),near.state().id());assertNotEquals(a.state().id(),far.state().id());
        send(s,guest,"JOIN",null,false,a.state().id(),null);assertEquals("RANKED_QUEUE_ONLY",guest.last("ERROR").code());
        send(s,a,"READY",null,false,null,null);send(s,near,"READY",null,false,null,null);send(s,near,"RESIGN",null,false,null,null);
        var change=a.state().ratingChange();assertTrue(change.delta()>16);assertEquals(1,rankings.profile(a.identity.id()).games());
        rankings.finish(a.state().matchId(),0);assertEquals(change.after(),rankings.profile(a.identity.id()).rating());assertEquals(1,rankings.profile(a.identity.id()).games());
        Player farther=account(s,1800);send(s,farther,"QUEUE","DUEL",true,null,null);clock.now+=100_000;s.cleanup();assertEquals(far.state().id(),farther.state().id());
    }
    @Test void eloRewardsUpsetsAndGuestsCannotEnterRanked(){
        var s=service(new RoomServiceTest.TestClock());Player guest=guest(s);send(s,guest,"QUEUE","DUEL",true,null,null);assertEquals("ACCOUNT_REQUIRED",guest.last("ERROR").code());
        Player low=account(s,900),high=account(s,1600);
        int lastDelta=0;
        for(int i=0;i<3;i++){String id=UUID.randomUUID().toString();rankings.begin(id,low.identity.id(),high.identity.id());lastDelta=rankings.finish(id,0).get(0).delta();}
        assertTrue(lastDelta>16);assertFalse(rankings.canPair(low.identity.id(),high.identity.id()));
        assertThrows(IllegalArgumentException.class,()->rankings.begin(UUID.randomUUID().toString(),low.identity.id(),high.identity.id()));
        assertEquals(3,rankings.profile(low.identity.id()).games());
    }
    @Test void chatIsRoomScopedBoundedFilteredAndRateLimited(){
        var clock=new RoomServiceTest.TestClock();var s=service(clock);Player a=guest(s),b=guest(s),outsider=guest(s);
        send(s,a,"CREATE","DUEL",false,null,null);send(s,b,"JOIN",null,false,a.state().id(),null);
        send(s,a,"CHAT",null,false,null,"<img src=x> hello");assertEquals("<img src=x> hello",b.last("CHAT").chat().text());
        assertTrue(outsider.events.stream().noneMatch(e->e.type().equals("CHAT")));
        send(s,a,"CHAT",null,false,null,"hello again");assertEquals("CHAT_LIMIT",a.last("ERROR").code());
        clock.now+=1001;send(s,a,"CHAT",null,false,null,"shit");assertEquals("••••",b.last("CHAT").chat().text());
        clock.now+=1001;send(s,a,"CHAT",null,false,null,"SHIT");assertEquals("CHAT_LIMIT",a.last("ERROR").code());
        for(String text:List.of("https://example.org","a@example.org","a".repeat(281),"x\u0001y","x\u202ey"))assertThrows(IllegalArgumentException.class,()->ChatFilter.clean(text));
        for(int i=0;i<8;i++){clock.now+=1001;send(s,a,"CHAT",null,false,null,"message "+i);}
        clock.now+=1001;send(s,a,"CHAT",null,false,null,"last message");assertEquals("CHAT_LIMIT",a.last("ERROR").code());
    }
}
