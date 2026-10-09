package santorini.server;

import online.protocol.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RoomServiceTest {
    static class TestPeer implements Peer {
        final String id = UUID.randomUUID().toString();
        final List<ServerEvent> events = new CopyOnWriteArrayList<>();
        boolean closed;
        public String id() { return id; }
        public void send(ServerEvent event) { events.add(event); }
        public void close() { closed = true; }
        ServerEvent last(String type) { return events.stream().filter(e -> e.type().equals(type)).reduce((a,b) -> b).orElseThrow(); }
        RoomState state() { return last("STATE").room(); }
    }
    static class TestClock extends Clock {
        long now = 1_000_000;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now); }
    }
    private final TestClock clock = new TestClock();
    private final RoomService service = new RoomService(clock, 60, 100);
    private TestPeer player(String name) {
        TestPeer peer = new TestPeer();
        service.receive(peer, new ClientCommand("HELLO", null, null, name, null, null, null, null, null));
        return peer;
    }
    private void command(TestPeer peer, String type, String code, String rules) {
        service.receive(peer, new ClientCommand(type, UUID.randomUUID().toString(), null, null, code, rules, null,
                peer.events.stream().anyMatch(e -> e.type().equals("STATE")) ? peer.state().version() : null, null));
    }
    private void start(TestPeer first, TestPeer second) {
        command(first,"CREATE",null,"CLASSIC"); command(second,"JOIN",first.state().id(),null);
        command(first,"READY",null,null); command(second,"READY",null,null);
        assertEquals("PLAYING", first.state().status());
    }
    @Test void publicQueueNeverFindsPrivateRoomsAndSeparatesRuleProfiles() {
        TestPeer privateHost = player("Private"), first = player("First"), second = player("Second"), gods = player("Gods");
        command(privateHost,"CREATE",null,"CLASSIC"); command(first,"QUEUE",null,"CLASSIC");
        command(gods,"QUEUE",null,"GODS"); command(second,"QUEUE",null,"CLASSIC");
        assertNotEquals(privateHost.state().id(), first.state().id());
        assertNotEquals(gods.state().id(), first.state().id());
        assertEquals(first.state().id(), second.state().id());
        assertEquals(2, first.state().players().size());
    }
    @Test void privateCodesAreRequiredAndFullRoomsRejectThirdPlayer() {
        TestPeer first = player("A"), second = player("B"), third = player("C");
        command(first,"CREATE",null,"CLASSIC"); command(second,"JOIN","WRONG123",null);
        assertEquals("ROOM_NOT_FOUND", second.last("ERROR").code());
        command(second,"JOIN",first.state().id().toLowerCase(Locale.ROOT),null);
        command(third,"JOIN",first.state().id(),null);
        assertEquals("ROOM_FULL", third.last("ERROR").code());
    }
    @Test void actionsRequireOwnershipAndStaleVersionsCannotApplyTwice() {
        TestPeer first = player("A"), second = player("B"); start(first,second);
        long version = first.state().version();
        var action = GameAction.at("PLACE",0,0);
        service.receive(second,new ClientCommand("ACTION",null,null,null,null,null,null,version,action));
        assertEquals("ILLEGAL_ACTION", second.last("ERROR").code());
        var command = new ClientCommand("ACTION","same-request",null,null,null,null,null,version,action);
        service.receive(first,command); service.receive(first,command);
        assertEquals("STALE_STATE", first.last("ERROR").code());
        assertEquals(1, first.state().game().cells().stream().filter(c -> c.worker() != null).count());
    }
    @Test void reconnectPausesRestoresSeatAndReplacesOldConnection() {
        TestPeer first = player("A"), second = player("B"); start(first,second);
        String token = first.last("WELCOME").token();
        service.disconnected(first); assertTrue(second.state().paused());
        TestPeer resumed = new TestPeer();
        service.receive(resumed,new ClientCommand("HELLO",null,token,null,null,null,null,null,null));
        assertFalse(second.state().paused()); assertEquals(0,resumed.state().yourSeat());
        TestPeer replacement = new TestPeer();
        service.receive(replacement,new ClientCommand("HELLO",null,token,null,null,null,null,null,null));
        assertTrue(resumed.closed);
        service.disconnected(resumed); assertFalse(second.state().paused());
    }
    @Test void expiredReservationForfeitsTheMatch() {
        TestPeer first = player("A"), second = player("B"); start(first,second);
        service.disconnected(first); clock.now += 60_001; service.cleanup();
        assertEquals("FINISHED",second.state().status()); assertEquals(1,second.state().game().winner());
    }
    @Test void concurrentQueueRequestsPairWithoutOverbooking() throws Exception {
        List<TestPeer> peers = new ArrayList<>(); for (int i = 0; i < 20; i++) peers.add(player("P"+i));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> jobs = new ArrayList<>();
            for (TestPeer peer : peers) jobs.add(pool.submit(() -> command(peer,"QUEUE",null,"CLASSIC")));
            for (Future<?> job : jobs) job.get(10, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        Map<String,Integer> counts = new HashMap<>();
        for (TestPeer peer : peers) counts.merge(peer.state().id(),1,Integer::sum);
        assertEquals(10,counts.size()); assertTrue(counts.values().stream().allMatch(n -> n == 2));
    }
    @Test void newGodPlayerGetsAvailableCardAndChangingItRevokesReady() {
        TestPeer first = player("A"), second = player("B"); command(first,"CREATE",null,"GODS");
        service.receive(first,new ClientCommand("CHOOSE_GOD",null,null,null,null,null,"Demeter",first.state().version(),null));
        command(first,"READY",null,null); command(second,"JOIN",first.state().id(),null);
        assertEquals("Artemis",second.state().players().get(1).god());
        service.receive(first,new ClientCommand("CHOOSE_GOD",null,null,null,null,null,"Triton",first.state().version(),null));
        assertFalse(first.state().players().get(0).ready());
    }
    @Test void waitingPlayerCanCancelQueueAndReuseConnection() {
        TestPeer first = player("A"); command(first,"QUEUE",null,"CLASSIC"); command(first,"LEAVE",null,null);
        assertEquals("LEFT",first.last("LEFT").type()); command(first,"CREATE",null,"GODS");
        assertFalse(first.state().publicRoom());
    }
    @Test void guestNamesAreUniqueAcrossCaseAndWhitespaceAndReleaseAfterSessionEnds() {
        TestPeer first = player("  Alice  "), duplicate = player("alice");
        assertEquals("NAME_TAKEN", duplicate.last("ERROR").code());
        command(first, "GOODBYE", null, null);
        assertTrue(first.closed);
        TestPeer replacement = player("ALICE");
        assertEquals("ALICE", replacement.last("WELCOME").identity().name());
        assertTrue(replacement.last("WELCOME").identity().guest());
    }
    @Test void namesStayReservedOnlyThroughTheReconnectWindow() {
        TestPeer first = player("Reserved");
        service.disconnected(first);
        assertEquals("NAME_TAKEN", player("reserved").last("ERROR").code());
        clock.now += 60_001; service.cleanup();
        assertNotNull(player("Reserved").last("WELCOME").token());
    }
    @Test void expiredMatchNamesReleaseWhileOpponentKeepsTheFinishedBoard() {
        TestPeer first = player("Builder"), second = player("Neighbour"); start(first, second);
        service.disconnected(first); clock.now += 60_001; service.cleanup();
        TestPeer fresh = player("builder");
        assertNotNull(fresh.last("WELCOME").token());
        assertEquals("FINISHED", second.state().status());
        assertEquals("Builder", second.state().players().get(0).name());
    }
    @Test void concurrentIdenticalNamesAdmitExactlyOneGuest() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Future<TestPeer>> attempts = new ArrayList<>();
            for (int i = 0; i < 12; i++) attempts.add(pool.submit(() -> player("One name")));
            int welcomes = 0;
            for (var result : attempts) if (result.get().events.stream().anyMatch(e -> e.type().equals("WELCOME"))) welcomes++;
            assertEquals(1, welcomes);
        } finally { pool.shutdownNow(); }
    }

}
