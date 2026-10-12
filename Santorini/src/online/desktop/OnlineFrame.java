package online.desktop;

import online.protocol.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/** Swing network view; all match validation and state transitions run on Spring Boot. */
public final class OnlineFrame extends JFrame {
    private final JTextField server = new JTextField("http://localhost:8080");
    private final JTextField name = new JTextField("Desktop player");
    private final JComboBox<String> identity = new JComboBox<>(new String[]{"Guest", "Log in", "Create account"});
    private final JPasswordField password = new JPasswordField();
    private final JTextField code = new JTextField();
    private final JComboBox<String> rules = new JComboBox<>(new String[]{"GODS", "CLASSIC"});
    private final JComboBox<String> modes = new JComboBox<>(new String[]{"DUEL","THREE","FOUR_FFA","TEAMS"});
    private final JCheckBox ranked = new JCheckBox("Ranked Classic duel");
    private final JButton solo = new JButton("Computer practice"), chatSend = new JButton("Send message");
    private final JTextArea chatLog = new JTextArea(8,22);
    private final JTextField chatText = new JTextField();
    private final java.util.LinkedHashMap<String,ChatMessage> messages = new java.util.LinkedHashMap<>();
    private String chatRequest;
    private boolean account;
    private final JComboBox<String> gods = new JComboBox<>(new String[]{"Artemis", "Demeter", "Triton"});
    private final JLabel connection = new JLabel("Offline"), information = new JLabel("Connect, then create a room or find an opponent.");
    private final JLabel roomCode = new JLabel("No room"), players = new JLabel(" ");
    private final JButton connect = new JButton("Connect"), queue = new JButton("Find opponent"), create = new JButton("Private room"), join = new JButton("Join code");
    private final JButton ready = new JButton("Ready"), leave = new JButton("Leave room"), resign = new JButton("Resign");
    private final JButton disconnect = new JButton("End visit / log out");
    private final JPanel extra = new JPanel();
    private final JButton[] cells = new JButton[25];
    private final NetworkClient client;
    private final Timer timeout, chatTimeout;
    private RoomState room;
    private boolean connected, busy, rendering;

    public OnlineFrame() {
        super("Santorini Online · Desktop ↔ Browser");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); setSize(900, 740); setLocationRelativeTo(null);
        client = new NetworkClient(event -> SwingUtilities.invokeLater(() -> receive(event)),
                status -> SwingUtilities.invokeLater(() -> {
                    connection.setText(status); connected = status.equals("Connected");
                    if (!connected) busy = false; render();
                }));
        timeout = new Timer(8000, e -> { busy = false; information.setText("No response yet. Wait for the refreshed board before retrying."); render(); });
        timeout.setRepeats(false);
        chatTimeout = new Timer(8000, e -> { chatRequest=null; render(); information.setText("Your message took too long to send. Please try again."); });
        chatTimeout.setRepeats(false);
        JPanel root = new JPanel(new BorderLayout(12, 12)); root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18)); setContentPane(root);
        JPanel top = new JPanel(new GridLayout(0, 2, 8, 6));
        top.add(new JLabel("Server URL")); top.add(server);
        top.add(new JLabel("How to enter")); top.add(identity);
        top.add(new JLabel("Player name")); top.add(name);
        top.add(new JLabel("Password (accounts only)")); top.add(password);
        password.setEnabled(false);
        identity.addActionListener(e -> { password.setEnabled(identity.getSelectedIndex() != 0 && !connected); password.setText(""); });
        top.add(new JLabel("Players / teams")); top.add(modes); top.add(ranked); top.add(solo);
        top.add(connection); top.add(connect); top.add(new JLabel("Rules")); top.add(rules); top.add(queue); top.add(create);
        top.add(code); top.add(join); top.add(roomCode); top.add(players); top.add(gods); top.add(ready); root.add(top, BorderLayout.NORTH);
        JPanel board = new JPanel(new GridLayout(5, 5, 6, 6));
        for (int i = 0; i < 25; i++) {
            final int index = i; cells[i] = new JButton(); cells[i].setFont(new Font("SansSerif", Font.BOLD, 14));
            cells[i].setOpaque(true); cells[i].addActionListener(e -> click(index)); board.add(cells[i]);
        }
        root.add(board, BorderLayout.CENTER);
        JPanel chatPanel = new JPanel(new BorderLayout(4,4)); chatLog.setEditable(false); chatLog.setLineWrap(true); chatLog.setWrapStyleWord(true);
        chatPanel.add(new JScrollPane(chatLog),BorderLayout.CENTER); JPanel chatEntry = new JPanel(new BorderLayout());chatEntry.add(chatText);chatEntry.add(chatSend,BorderLayout.SOUTH);chatPanel.add(chatEntry,BorderLayout.SOUTH);root.add(chatPanel,BorderLayout.EAST);
        chatSend.addActionListener(e -> sendChat()); chatText.addActionListener(e -> sendChat());
        ranked.addActionListener(e -> { if(ranked.isSelected()){modes.setSelectedItem("DUEL");rules.setSelectedItem("CLASSIC");}render(); });
        modes.addActionListener(e -> { if(!rendering && (modes.getSelectedIndex()>1))rules.setSelectedItem("CLASSIC");render(); });
        solo.addActionListener(e -> send("SOLO",null,null));
        JPanel bottom = new JPanel(new BorderLayout()); bottom.add(information, BorderLayout.NORTH);
        JPanel buttons = new JPanel(); buttons.add(extra); buttons.add(leave); buttons.add(resign); buttons.add(disconnect); bottom.add(buttons, BorderLayout.SOUTH); root.add(bottom, BorderLayout.SOUTH);
        connect.addActionListener(e -> {
            try {
                connect.setEnabled(false);
                if (identity.getSelectedIndex() == 0) client.connect(server.getText(), name.getText().strip());
                else {
                    char[] secret = password.getPassword(); password.setText("");
                    client.connectAccount(server.getText(), name.getText().strip(), secret, identity.getSelectedIndex() == 2)
                            .exceptionally(error -> {
                                SwingUtilities.invokeLater(() -> { render(); information.setText(error.getCause() == null ? error.getMessage() : error.getCause().getMessage()); });
                                return null;
                            });
                }
            } catch (IllegalArgumentException invalid) { connect.setEnabled(true); information.setText(invalid.getMessage()); }
        });
        queue.addActionListener(e -> send("QUEUE", null, null)); create.addActionListener(e -> send("CREATE", null, null));
        join.addActionListener(e -> send("JOIN", code.getText().strip(), null)); ready.addActionListener(e -> send("READY", null, null));
        gods.addActionListener(e -> { if (!rendering && connected && room != null) send("CHOOSE_GOD", null, null); });
        leave.addActionListener(e -> { if (room == null || !room.status().equals("PLAYING") || confirm("Leave this match? Your workers leave play; this may end the match.")) send("LEAVE", null, null); });
        disconnect.addActionListener(e -> {
            if (room != null && room.status().equals("PLAYING") && !confirm("End this visit? Your workers leave play; this may end the match.")) return;
            busy = true; render();
            client.disconnect().whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
                connected = false; busy = false; room = null; timeout.stop(); render();
                if (error != null) information.setText(error.getCause() == null ? error.getMessage() : error.getCause().getMessage());
            }));
        });
        resign.addActionListener(e -> { if (confirm("Resign this match?")) send("RESIGN", null, null); });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // Closing the window disconnects; the server reserves this seat for the grace period.
                timeout.stop(); chatTimeout.stop(); client.close();
            }
        });
        render(); setVisible(true);
    }

    private boolean confirm(String message) { return JOptionPane.showConfirmDialog(this, message, "Santorini", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }
    private void send(String type, String roomId, GameAction action) {
        if (!connected || busy) return;
        busy = true; timeout.restart();
        client.send(new ClientCommand(type,java.util.UUID.randomUUID().toString(),null,null,roomId,(String)rules.getSelectedItem(),(String)gods.getSelectedItem(),room==null?null:room.version(),action,(String)modes.getSelectedItem(),type.equals("QUEUE") && ranked.isSelected(),null));
        render();
    }
    private void sendChat() {
        if (!connected || room == null || chatRequest != null || chatText.getText().isBlank()) return;
        chatRequest = java.util.UUID.randomUUID().toString();
        client.send(new ClientCommand("CHAT",chatRequest,null,null,null,null,null,null,null,null,false,chatText.getText()));
        chatSend.setEnabled(false);
        chatTimeout.restart();
    }
    private void renderChat() {
        chatLog.setText(String.join("\n",messages.values().stream().map(m -> m.name()+": "+m.text()).toList()));
        chatLog.setCaretPosition(chatLog.getDocument().getLength());
    }
    private void receive(ServerEvent event) {
        if(event.type().equals("WELCOME"))account=event.identity()!=null && !event.identity().guest();
        if(event.type().equals("CHAT") && room != null && room.id().equals(event.roomId())) {
            messages.put(event.chat().id(),event.chat());while(messages.size()>50)messages.remove(messages.keySet().iterator().next());
            if(event.requestId()!=null && event.requestId().equals(chatRequest)){chatRequest=null;chatTimeout.stop();chatText.setText("");}renderChat();render();return;
        }
        if(event.type().equals("CHAT_HISTORY") && room != null && room.id().equals(event.roomId())) {messages.clear();for(ChatMessage m:event.history())messages.put(m.id(),m);renderChat();return;}
        if(event.type().equals("ERROR") && event.requestId()!=null && event.requestId().equals(chatRequest)){chatRequest=null;chatTimeout.stop();render();information.setText(event.message());return;}
        if (event.type().equals("STATE") && event.room() != null) {
            if(room==null || !room.id().equals(event.room().id())){messages.clear();renderChat();}
            if (room == null || !room.id().equals(event.room().id()) || event.room().version() >= room.version()) room = event.room();
            busy = false; timeout.stop();
        } else if (event.type().equals("LEFT")) { room = null; busy = false; timeout.stop(); }
        else if (event.type().equals("BYE")) { connected = false; room = null; busy = false; timeout.stop(); }
        else if (event.type().equals("ERROR")) {
            busy = false; timeout.stop();
            if ("SESSION_EXPIRED".equals(event.code())) { connected = false; room = null; }
            render(); information.setText(event.message()); return;
        }
        render();
    }
    private void click(int index) {
        if (room == null || room.game() == null) return;
        MatchSnapshot.Cell cell = room.game().cells().get(index);
        room.game().legalActions().stream().filter(a -> a.type().equals("SELECT") ? a.worker().equals(cell.worker())
                : a.row() != null && a.row() == cell.row() && a.col() == cell.col()).findFirst().ifPresent(a -> send("ACTION", null, a));
    }

    private void render() {
        rendering = true;
        boolean available = connected && !busy;
        connect.setEnabled(!connected && connection.getText().equals("Offline"));
        server.setEnabled(!connected); name.setEnabled(!connected); identity.setEnabled(!connected); password.setEnabled(!connected && identity.getSelectedIndex() != 0);
        queue.setEnabled(available && room == null); create.setEnabled(available && room == null); join.setEnabled(available && room == null);
        ranked.setEnabled(room==null && available && account); if(!account)ranked.setSelected(false);
        modes.setEnabled(room==null && available && !ranked.isSelected());
        solo.setEnabled(room==null && available && !ranked.isSelected() && modes.getSelectedIndex()==0);
        create.setEnabled(create.isEnabled() && !ranked.isSelected());
        chatSend.setEnabled(available && chatRequest==null && room!=null && room.players().stream().noneMatch(RoomState.Seat::bot));
        chatText.setEnabled(chatSend.isEnabled());
        rules.setEnabled(room == null && available && !ranked.isSelected() && modes.getSelectedIndex()<2); code.setEnabled(room == null);
        disconnect.setEnabled(available);
        ready.setEnabled(available && room != null && room.status().equals("WAITING"));
        gods.setEnabled(ready.isEnabled() && room.rules().equals("GODS"));
        leave.setEnabled(available && room != null); resign.setEnabled(available && room != null && room.status().equals("PLAYING") && !room.game().eliminated().contains(room.yourSeat()));
        if (room == null) {
            roomCode.setText("No room"); players.setText(" "); information.setText("Connect, then create a room or find an opponent.");
        } else {
            roomCode.setText((room.publicRoom() ? "Public" : "Private") + " · " + room.id());
            players.setText("Players: " + String.join(" vs ", room.players().stream().map(p -> p.name() + " (" + p.god() + (room.mode().equals("TEAMS")?", team "+(p.team()+1):"") + (p.rating()!=null?", rating "+p.rating():"") + ")").toList()));
            RoomState.Seat me = room.players().get(room.yourSeat());
            gods.setSelectedItem(me.god()); ready.setText(me.ready() ? "Not ready" : "Ready");
            information.setText(room.game() == null ? "Waiting for " + room.capacity() + " ready players."
                    : room.game().winner() != null ? "Winner: " + String.join(" and ",room.game().winners().stream().map(i -> room.players().get(i).name()).toList()) + " · " + room.result()
                    : room.paused() ? "Paused: opponent has 60 seconds to reconnect."
                    : room.game().turn() == room.yourSeat() ? "Your turn · " + room.game().phase() : "Opponent's turn · " + room.game().phase());
        }
        List<GameAction> actions = room != null && room.game() != null && room.game().turn() == room.yourSeat()
                && !room.paused() && available ? room.game().legalActions() : List.of();
        extra.removeAll();
        for (GameAction action : actions) if (action.row() == null && !action.type().equals("SELECT")) {
            JButton button = new JButton(switch (action.type()) { case "CANCEL" -> "Choose another worker"; case "END_MOVE" -> "Continue to build"; default -> "End turn"; });
            button.addActionListener(e -> send("ACTION", null, action)); extra.add(button);
        }
        for (int i = 0; i < 25; i++) {
            int row = i / 5, col = i % 5;
            MatchSnapshot.Cell cell = room == null || room.game() == null ? null : room.game().cells().get(i);
            String coordinate = "" + (char) ('A' + col) + (row + 1);
            cells[i].setText("<html><center>" + coordinate + "<br>" + (cell == null ? "" : cell.dome() ? "DOME" : "Level " + cell.level())
                    + (cell != null && cell.worker() != null ? "<br>" + "Player " + (cell.owner()+1) + " · " + (cell.worker() % room.game().workersPerPlayer() + 1) : "") + "</center></html>");
            cells[i].setBackground(cell != null && cell.owner() != null ? new Color[]{new Color(209,161,126),new Color(151,181,181),new Color(155,175,130),new Color(172,140,164)}[cell.owner()] : new Color(235, 222, 192));
            cells[i].setEnabled(actions.stream().anyMatch(a -> a.type().equals("SELECT") ? cell != null && a.worker().equals(cell.worker()) : a.row() != null && a.row() == row && a.col() == col));
        }
        rendering = false; extra.revalidate(); extra.repaint();
    }
}
