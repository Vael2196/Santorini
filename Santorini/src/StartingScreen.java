import Frame.*;
import Frame.Boards.Board;
import Frame.Memento.*;
import GameEntities.Gods.*;
import GameEntities.Player;
import GameEntities.Worker;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class StartingScreen {

    private static final String STARTSCREEN = "/assets/startscreen.jpg";
    private static final String ASSETS_DIR = "assets";

    public static void main(String[] args) {
        JFrame startingFrame = new JFrame();
        URL url = StartingScreen.class.getResource(STARTSCREEN);
        ImageIcon icon = new ImageIcon(url);
        JPanel imgPanel = new ImagePanel(icon);
        startingFrame.setVisible(true);
        startingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startingFrame.setSize(500, 500);
        startingFrame.setResizable(false);
        startingFrame.setTitle("Santorini Board Game");
        startingFrame.setLocationRelativeTo(null);
        startingFrame.setContentPane(imgPanel);

        JLabel textLabel = new JLabel("Santorini Board Game", SwingConstants.CENTER);
        textLabel.setForeground(new Color(0, 60, 90));
        textLabel.setFont(new Font("Arial", Font.BOLD, 24));
        textLabel.setBounds(50, 20, 400, 50);
        imgPanel.add(textLabel);


        int btnWidth = 200;
        int btnHeight = 40;
        int centerX = (500 - btnWidth) / 2;
        int baseY = 120;
        int spacing = 60;


        JButton startButton = new JButton("Start");
        startButton.setFont(new Font("Arial", Font.BOLD, 20));
        startButton.setBounds(centerX, baseY, btnWidth, btnHeight);
        startButton.setOpaque(true);
        startButton.setBackground(Color.WHITE);
        startButton.setForeground(Color.BLACK);
        imgPanel.add(startButton);

        JButton loadButton = new JButton("Load");
        loadButton.setFont(new Font("Arial", Font.BOLD, 20));
        loadButton.setBounds(centerX, baseY + spacing, btnWidth, btnHeight);
        loadButton.setOpaque(true);
        loadButton.setBackground(Color.WHITE);
        loadButton.setForeground(Color.BLACK);
        imgPanel.add(loadButton);

        JButton levelEditorButton = new JButton("Level Editor");
        levelEditorButton.setFont(new Font("Arial", Font.BOLD, 20));
        levelEditorButton.setBounds(centerX, baseY + spacing * 2, btnWidth, btnHeight);
        levelEditorButton.setOpaque(true);
        levelEditorButton.setBackground(Color.WHITE);
        levelEditorButton.setForeground(Color.BLACK);
        imgPanel.add(levelEditorButton);

        JButton exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Arial", Font.BOLD, 20));
        exitButton.setBounds(centerX, baseY + spacing * 3, btnWidth, btnHeight);
        exitButton.setOpaque(true);
        exitButton.setBackground(Color.WHITE);
        exitButton.setForeground(Color.BLACK);
        imgPanel.add(exitButton);

        levelEditorButton.addActionListener(a -> {
            new LevelEditorFrame();
        });

        startButton.addActionListener(a -> {
            File assetsFolder = new File(ASSETS_DIR);
            String[] modeNames = assetsFolder.exists()
                    ? Arrays.stream(Objects.requireNonNull(assetsFolder.listFiles()))
                    .filter(File::isDirectory)
                    .map(File::getName)
                    .toArray(String[]::new)
                    : new String[0];

            List<String> choicesMod = new ArrayList<>();
            choicesMod.add("Standard");
            choicesMod.add("Classic");
            Collections.addAll(choicesMod, modeNames);

            String selectedMode = (String) JOptionPane.showInputDialog(
                    null,
                    "Select Game Mode:",
                    "Mode Selection",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    choicesMod.toArray(),
                    choicesMod.get(0)
            );
            if (selectedMode == null) {
                return;
            }

            if (!selectedMode.equals("Standard") && !selectedMode.equals("Classic")) {
                try {
                    // Load the game mode’s config.properties
                    File modeFolder = new File(ASSETS_DIR + "/" + selectedMode);
                    File cfg = new File(modeFolder, "config.properties");
                    Properties props = new Properties();
                    try (FileReader fr = new FileReader(cfg)) {
                        props.load(fr);
                    }

                    int rows = Integer.parseInt(props.getProperty("rows"));
                    int cols = Integer.parseInt(props.getProperty("cols"));
                    Board board = Board.createSquare(rows);

                    Set<Point> holeSet = new HashSet<>();
                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            boolean present = Boolean.parseBoolean(
                                    props.getProperty("tile." + r + "." + c + ".present", "true"));
                            int level = Integer.parseInt(
                                    props.getProperty("tile." + r + "." + c + ".level", "0"));
                            boolean dome = Boolean.parseBoolean(
                                    props.getProperty("tile." + r + "." + c + ".dome", "false"));
                            int occupant = Integer.parseInt(
                                    props.getProperty("tile." + r + "." + c + ".occupant", "0"));

                            Tile tile = board.getTile(r, c);
                            if (!present) {
                                holeSet.add(new Point(r, c));
                            } else {
                                for (int lev = 0; lev < level; lev++) {
                                    tile.getBuilding().build();
                                }
                                if (dome && tile.getBuilding().getLevel() == 3) {
                                    tile.getBuilding().build();
                                }
                            }
                        }
                    }

                    int nPlayers = Integer.parseInt(props.getProperty("numPlayers", "2"));
                    int wPerPlayer = Integer.parseInt(
                            props.getProperty("workersPerPlayer", "2"));

                    List<Player> players = new ArrayList<>();
                    if (nPlayers == 1) {
                        Player p1 = new Player("Red");
                        players.add(p1);
                    } else {
                        Player p1 = new Player("Red");
                        Player p2 = new Player("Blue");
                        players.add(p1);
                        players.add(p2);
                    }

                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            int occupant = Integer.parseInt(
                                    props.getProperty("tile." + r + "." + c + ".occupant", "0"));
                            if (occupant == 1 && players.size() >= 1) {
                                Player p1 = players.get(0);
                                if (p1.getWorkers().size() < wPerPlayer) {
                                    Worker w = new Worker(p1, board.getTile(r, c));
                                    p1.addWorker(w);
                                }
                            }
                            if (occupant == 2 && players.size() == 2) {
                                Player p2 = players.get(1);
                                if (p2.getWorkers().size() < wPerPlayer) {
                                    Worker w = new Worker(p2, board.getTile(r, c));
                                    p2.addWorker(w);
                                }
                            }
                        }
                    }

                    boolean allowMove = Boolean.parseBoolean(props.getProperty("allowMove", "true"));
                    boolean allowBuild = Boolean.parseBoolean(props.getProperty("allowBuild", "true"));
                    boolean allowGod = Boolean.parseBoolean(props.getProperty("allowGodPower", "true"));
                    boolean allowSelect = Boolean.parseBoolean(props.getProperty("allowGodSelect", "true"));
                    String god1Name = props.getProperty("god1", "Random");
                    String god2Name = props.getProperty("god2", "Random");
                    String coreMode  = props.getProperty("coreMode", "Standard");

                    GameController coreController =
                            new GameController(board, null, players, wPerPlayer);
                    coreController.setHoles(holeSet);

                    Controller gameController;

                    if (coreMode.equals("Tag")) {
                        for (Player p : players) {
                            p.setGodPower(new Artemis());
                        }
                        TagController tagCtrl =
                                new TagController(board,
                                        null,
                                        players,
                                        1,
                                        holeSet);
                        gameController = tagCtrl;
                    }
                    else if (allowGod && !coreMode.equals("Classic")) {
                        Map<String, Supplier<GodPower>> godMap = new LinkedHashMap<>();
                        godMap.put("Artemis", Artemis::new);
                        godMap.put("Demeter", Demeter::new);
                        godMap.put("Triton",  Triton::new);

                        GodPower gp1 = godMap.containsKey(god1Name)
                                ? godMap.get(god1Name).get()
                                : godMap.get("Artemis").get();
                        players.get(0).setGodPower(gp1);

                        if (players.size() == 2) {
                            GodPower gp2 = godMap.containsKey(god2Name)
                                    ? godMap.get(god2Name).get()
                                    : godMap.get("Demeter").get();
                            players.get(1).setGodPower(gp2);
                        }

                        GodPowerController godController =
                                new GodPowerController(coreController, players);
                        coreController.setHoles(holeSet);
                        gameController = godController;
                    }
                    else {
                        gameController = coreController;
                    }

                    File bgFile = new File(ASSETS_DIR + "/" + selectedMode + "/gameboard.jpg");
                    if (! bgFile.exists()) bgFile = null;

                    File musicFile = new File(ASSETS_DIR + "/" + selectedMode + "/music.wav");
                    if (! musicFile.exists()) musicFile = null;

                    GameFrame frame =
                            new GameFrame(board, gameController, bgFile, musicFile);
                    coreController.setFrame(frame);
                    if (gameController instanceof GodPowerController) {
                        ((GodPowerController) gameController).setFrame(frame);
                    }
                    if (gameController instanceof TagController) {
                        ((TagController) gameController).setFrame(frame);
                    }

                    frame.validate();
                    frame.repaint();

                    startingFrame.dispose();
                    return;
                }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Failed to load custom mode \"" + selectedMode + "\": " + ex.getMessage()
                    );
                    return;
                }
            }


            if (selectedMode.equals("Classic")) {
                Player p1 = new Player("Red");
                Player p2 = new Player("Blue");
                List<Player> players = Arrays.asList(p1, p2);

                p1.addWorker(new Worker(p1));
                p1.addWorker(new Worker(p1));
                p2.addWorker(new Worker(p2));
                p2.addWorker(new Worker(p2));

                GameController coreController =
                        new GameController(Board.createSquare(5), null, players);
                GameFrame frame = new GameFrame(Board.createSquare(5), coreController);
                coreController.setFrame(frame);

                frame.validate();
                frame.repaint();

                startingFrame.dispose();
                return;
            }
            else {
                Map<String, Supplier<GodPower>> godMap = new LinkedHashMap<>();
                godMap.put("Artemis", Artemis::new);
                godMap.put("Demeter", Demeter::new);
                godMap.put("Triton",  Triton::new);

                List<String> choices = new ArrayList<>();
                choices.add("Random");
                choices.addAll(godMap.keySet());

                Random rand = new Random();

                Player p1 = new Player("Blue");
                Player p2 = new Player("Red");
                List<Player> players = Arrays.asList(p1, p2);

                for (Player p : players) {
                    String selected = (String) JOptionPane.showInputDialog(
                            null,
                            p.getColor() + " — select your God power:",
                            "God Selection",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            choices.toArray(),
                            choices.get(0)
                    );
                    if (selected == null || selected.equals("Random")) {
                        List<String> names = new ArrayList<>(godMap.keySet());
                        selected = names.get(rand.nextInt(names.size()));
                    }
                    Supplier<GodPower> sup = godMap.get(selected);
                    p.setGodPower(sup.get());
                }

                Board board = Board.createSquare(5);
                p1.addWorker(new Worker(p1));
                p1.addWorker(new Worker(p1));
                p2.addWorker(new Worker(p2));
                p2.addWorker(new Worker(p2));

                GameController coreController =
                        new GameController(board, null, players);
                GodPowerController godController =
                        new GodPowerController(coreController, players);

                GameFrame frame = new GameFrame(board, godController);
                coreController.setFrame(frame);
                godController.setFrame(frame);

                frame.validate();
                frame.repaint();

                SwingUtilities.invokeLater(() -> {});
            }
        });

        loadButton.addActionListener(a -> {
            JFileChooser chooser = new JFileChooser();
            int ret = chooser.showOpenDialog(startingFrame);
            if (ret != JFileChooser.APPROVE_OPTION) return;
            File file = chooser.getSelectedFile();
            try {
                Object[] state = GameCaretaker.loadFromFile(file);
                Board loadedBoard = (Board) state[0];
                @SuppressWarnings("unchecked")
                List<Player> loadedPlayers = (List<Player>) state[1];
                int currentIdx = (int) state[2];

                GameController coreController =
                        new GameController(loadedBoard, null, loadedPlayers);
                GodPowerController godController =
                        new GodPowerController(coreController, loadedPlayers);
                coreController.setCurrentPlayerIndex(currentIdx);

                GameFrame frame = new GameFrame(loadedBoard, godController);
                coreController.setFrame(frame);
                godController.setFrame(frame);

                frame.validate();
                frame.repaint();

                startingFrame.dispose();
                SwingUtilities.invokeLater(() -> {});
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        startingFrame,
                        "Load failed: " + ex.getMessage()
                );
            }
        });

        exitButton.addActionListener(a -> {
            System.exit(0);
        });
    }
}