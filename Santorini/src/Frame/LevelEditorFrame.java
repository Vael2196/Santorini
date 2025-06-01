package Frame;

import Frame.Boards.Board;
import GameEntities.Player;
import GameEntities.Worker;
import GameEntities.Gods.*;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;


public class LevelEditorFrame extends JFrame {

    private static final String DEFAULT_BG = "/assets/gameboard.jpg";
    private static final String ASSETS_DIR  = "assets";

    private static final int FRAME_W = 1275, FRAME_H = 865;
    private static final int BOARD_W = 750, BOARD_H = 750;
    private static final int INFO_W  = 250, INFO_H  = 700;

    private final JLayeredPane layeredPane;
    private final JPanel boardPanel;
    private final JScrollPane infoScrollPane;
    private final JLabel bgLabel;

    private int numRows = 5, numCols = 5;
    private final TileState[][] tileStates;
    private final JPanel[][] tileContainers;

    private final List<Point> placedWorkersP1 = new ArrayList<>();
    private final List<Point> placedWorkersP2 = new ArrayList<>();

    private int editorMode = 0;

    private final JCheckBox allowMoveChk;
    private final JCheckBox allowBuildChk;
    private final JComboBox<Integer> numPlayersBox;
    private final JComboBox<Integer> numWorkersBox;
    private final JCheckBox allowGodChk;
    private final JCheckBox allowGodSelectChk;
    private final JComboBox<String> god1Box;
    private final JComboBox<String> god2Box;
    private final JComboBox<String> coreModeBox;
    private final JButton bgUploadBtn;
    private final JButton musicUploadBtn;
    private final JTextField modeNameField;
    private final JButton saveModeBtn;

    private final JToggleButton removeTileModeBtn;
    private final JToggleButton buildModeBtn;
    private final JToggleButton workerModeBtn;
    private final ButtonGroup modeToggleGroup;

    private File customBgFile = null;
    private File customMusicFile = null;
    private Clip musicClip = null;

    private static final List<String> AVAILABLE_GODS = List.of(
            "Artemis", "Demeter", "Triton"
    );

    /**
     * Holds per‐tile state in the editor:
     *  present: whether tile exists (true) or “hole” (false)
     *  level: 0‐3
     *  dome: true/false
     *  occupant: 0=none, 1=player1, 2=player2
     */
    private static class TileState {
        boolean present = true;
        int level = 0;
        boolean dome = false;
        int occupant = 0;

        void cycleLevel() {
            if (!present) return;
            if (!dome) {
                if (level < 3) level++;
                else dome = true;
            } else {
                dome = false;
                level = 0;
            }
        }
    }

    public LevelEditorFrame() {
        super("Santorini Level Editor");
        setSize(FRAME_W, FRAME_H);
        setResizable(false);
        setLayout(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        tileStates = new TileState[20][20];
        tileContainers = new JPanel[20][20];
        for (int r = 0; r < 20; r++) {
            for (int c = 0; c < 20; c++) {
                tileStates[r][c] = new TileState();
                tileContainers[r][c] = new JPanel();
            }
        }

        layeredPane = new JLayeredPane();
        layeredPane.setBounds(0, 0, FRAME_W, FRAME_H);

        URL bgUrl = getClass().getResource(DEFAULT_BG);
        ImageIcon rawIcon = new ImageIcon(bgUrl);
        Image bgImg = rawIcon.getImage().getScaledInstance(
                FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
        bgLabel = new JLabel(new ImageIcon(bgImg));
        bgLabel.setBounds(0, 0, FRAME_W, FRAME_H);
        layeredPane.add(bgLabel, Integer.valueOf(0));

        int boardX = (FRAME_W - BOARD_W) / 2;
        int boardY = (FRAME_H - BOARD_H) / 2;
        boardPanel = new JPanel(null);
        boardPanel.setOpaque(false);
        boardPanel.setBounds(boardX, boardY, BOARD_W, BOARD_H);
        layeredPane.add(boardPanel, Integer.valueOf(1));

        buildBoardUI(numRows, numCols);

        JPanel settingsCard = new JPanel();
        settingsCard.setLayout(new BoxLayout(settingsCard, BoxLayout.Y_AXIS));
        settingsCard.setOpaque(true);
        settingsCard.setBackground(new Color(255, 255, 255, 200));
        settingsCard.setBorder(new LineBorder(Color.DARK_GRAY, 2));
        settingsCard.setPreferredSize(new Dimension(INFO_W, INFO_H));

        allowMoveChk = new JCheckBox("Allow Move", true);
        allowBuildChk = new JCheckBox("Allow Build", true);

        numPlayersBox = new JComboBox<>(new Integer[]{1, 2});
        numPlayersBox.setSelectedItem(2);
        numWorkersBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        numWorkersBox.setSelectedItem(2);

        allowGodChk = new JCheckBox("Allow God Power", true);
        allowGodSelectChk = new JCheckBox("Allow God Selection", true);

        god1Box = new JComboBox<>(AVAILABLE_GODS.toArray(new String[0]));
        god1Box.insertItemAt("Random", 0);
        god1Box.setSelectedIndex(0);

        god2Box = new JComboBox<>(AVAILABLE_GODS.toArray(new String[0]));
        god2Box.insertItemAt("Random", 0);
        god2Box.setSelectedIndex(0);

        coreModeBox = new JComboBox<>(new String[]{"Standard", "Classic", "Tag"});
        coreModeBox.setSelectedItem("Standard");

        bgUploadBtn = new JButton("Upload Background…");
        musicUploadBtn = new JButton("Upload Music… (WAV only)");

        modeNameField = new JTextField();
        modeNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        modeNameField.setBorder(new LineBorder(Color.GRAY, 1));
        modeNameField.setText("MyCustomMode");

        saveModeBtn = new JButton("Save Mode");

        settingsCard.add(Box.createVerticalStrut(10));
        settingsCard.add(allowMoveChk);
        settingsCard.add(Box.createVerticalStrut(10));
        settingsCard.add(allowBuildChk);
        settingsCard.add(Box.createVerticalStrut(10));

        settingsCard.add(new JLabel("Number of Players:"));
        settingsCard.add(numPlayersBox);
        settingsCard.add(Box.createVerticalStrut(10));

        settingsCard.add(new JLabel("Workers per Player:"));
        settingsCard.add(numWorkersBox);
        settingsCard.add(Box.createVerticalStrut(10));

        settingsCard.add(allowGodChk);
        settingsCard.add(Box.createVerticalStrut(10));
        settingsCard.add(allowGodSelectChk);
        settingsCard.add(Box.createVerticalStrut(10));

        settingsCard.add(new JLabel("God for Player 1:"));
        settingsCard.add(god1Box);
        settingsCard.add(Box.createVerticalStrut(10));

        settingsCard.add(new JLabel("God for Player 2:"));
        settingsCard.add(god2Box);
        settingsCard.add(Box.createVerticalStrut(10));

        settingsCard.add(new JLabel("Core Game Mode:"));
        settingsCard.add(coreModeBox);
        settingsCard.add(Box.createVerticalStrut(10));

        settingsCard.add(bgUploadBtn);
        settingsCard.add(Box.createVerticalStrut(10));
        settingsCard.add(musicUploadBtn);
        settingsCard.add(Box.createVerticalStrut(10));

        settingsCard.add(new JLabel("Mode Name:"));
        settingsCard.add(modeNameField);
        settingsCard.add(Box.createVerticalStrut(10));
        settingsCard.add(saveModeBtn);
        settingsCard.add(Box.createVerticalGlue());

        infoScrollPane = new JScrollPane(settingsCard);
        infoScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        infoScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        infoScrollPane.setBounds(10, boardY + (BOARD_H - INFO_H)/2, INFO_W, INFO_H);
        layeredPane.add(infoScrollPane, Integer.valueOf(2));

        JPanel modeTogglePanel = new JPanel();
        modeTogglePanel.setLayout(new BoxLayout(modeTogglePanel, BoxLayout.Y_AXIS));
        modeTogglePanel.setOpaque(true);
        modeTogglePanel.setBackground(new Color(255,255,255,200));
        modeTogglePanel.setBorder(new LineBorder(Color.DARK_GRAY, 2));
        modeTogglePanel.setBounds(boardX + BOARD_W + 10, boardY + 100, 200, 150);

        removeTileModeBtn = new JToggleButton("Tile‐Remove Mode", true);
        buildModeBtn = new JToggleButton("Build Mode");
        workerModeBtn = new JToggleButton("Worker‐Placement Mode");

        modeToggleGroup = new ButtonGroup();
        modeToggleGroup.add(removeTileModeBtn);
        modeToggleGroup.add(buildModeBtn);
        modeToggleGroup.add(workerModeBtn);

        modeTogglePanel.add(removeTileModeBtn);
        modeTogglePanel.add(Box.createVerticalStrut(10));
        modeTogglePanel.add(buildModeBtn);
        modeTogglePanel.add(Box.createVerticalStrut(10));
        modeTogglePanel.add(workerModeBtn);
        layerAddListeners();

        layeredPane.add(modeTogglePanel, Integer.valueOf(2));

        add(layeredPane);
        setLocationRelativeTo(null);
        setVisible(true);

        attachControlListeners();
    }

    /** Builds the  i×j grid of tiles (tileContainers) inside boardPanel. */
    private void buildBoardUI(int rows, int cols) {
        boardPanel.removeAll();

        int tileW = BOARD_W / cols;
        int tileH = BOARD_H / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JPanel container = tileContainers[r][c];
                container.setLayout(null);
                container.setBounds(c * tileW, r * tileH, tileW, tileH);
                container.setBorder(new LineBorder(Color.GRAY, 1));
                container.setBackground(Color.LIGHT_GRAY);

                // Initial paint based on tileStates[r][c]
                paintTileState(r, c);

                final int rr = r, cc = c;
                container.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleTileClick(rr, cc);
                    }
                });

                boardPanel.add(container);
            }
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    /**
     * Repaints a single tile’s UI to match tileStates[r][c].
     */
    private void paintTileState(int r, int c) {
        TileState ts = tileStates[r][c];
        JPanel container = tileContainers[r][c];
        container.removeAll();

        if (!ts.present) {
            container.setBackground(new Color(200, 200, 200));
            return;
        } else {
            container.setBackground(new Color(245, 245, 245));
        }

        if (ts.dome) {
            JLabel domeLbl = new JLabel("D");
            domeLbl.setFont(new Font("Arial", Font.BOLD, 24));
            domeLbl.setForeground(Color.BLACK);
            domeLbl.setBounds(0, 0, container.getWidth(), container.getHeight());
            domeLbl.setHorizontalAlignment(SwingConstants.CENTER);
            container.add(domeLbl);
        } else if (ts.level > 0) {
            JLabel levelLbl = new JLabel("" + ts.level);
            levelLbl.setFont(new Font("Arial", Font.BOLD, 24));
            levelLbl.setForeground(Color.BLUE);
            levelLbl.setBounds(0, 0, container.getWidth(), container.getHeight());
            levelLbl.setHorizontalAlignment(SwingConstants.CENTER);
            container.add(levelLbl);
        }

        if (ts.occupant == 1) {
            JLabel w1 = new JLabel("\u2659"); // white chess pawn
            w1.setFont(new Font("Serif", Font.BOLD, 32));
            w1.setForeground(Color.RED);
            w1.setBounds(0, 0, container.getWidth(), container.getHeight());
            w1.setHorizontalAlignment(SwingConstants.CENTER);
            container.add(w1);
        } else if (ts.occupant == 2) {
            JLabel w2 = new JLabel("\u2659");
            w2.setFont(new Font("Serif", Font.BOLD, 32));
            w2.setForeground(Color.BLUE);
            w2.setBounds(0, 0, container.getWidth(), container.getHeight());
            w2.setHorizontalAlignment(SwingConstants.CENTER);
            container.add(w2);
        }
    }

    /**
     * Handles what to do when a tile (r,c) is clicked, depending on editorMode
     */
    private void handleTileClick(int r, int c) {
        TileState ts = tileStates[r][c];

        switch (editorMode) {
            case 0 -> {
                ts.present = !ts.present;
                if (!ts.present) {
                    ts.level = 0;
                    ts.dome = false;
                    ts.occupant = 0;
                }
            }
            case 1 -> {
                if (ts.present) ts.cycleLevel();
            }
            case 2 -> {
                if (!ts.present) break;
                int occupant = ts.occupant;
                if (occupant == 0) {
                    int maxWorkers = (int) numWorkersBox.getSelectedItem();
                    int numPlayers = (int) numPlayersBox.getSelectedItem();
                    if (numPlayers == 1) {
                        if (placedWorkersP1.size() < maxWorkers) {
                            ts.occupant = 1;
                            placedWorkersP1.add(new Point(r, c));
                        }
                    } else {
                        if (placedWorkersP1.size() < maxWorkers) {
                            ts.occupant = 1;
                            placedWorkersP1.add(new Point(r, c));
                        } else if (placedWorkersP2.size() < maxWorkers) {
                            ts.occupant = 2;
                            placedWorkersP2.add(new Point(r, c));
                        }
                    }
                } else {
                    if (occupant == 1) {
                        placedWorkersP1.removeIf(pt -> pt.x == r && pt.y == c);
                    } else {
                        placedWorkersP2.removeIf(pt -> pt.x == r && pt.y == c);
                    }
                    ts.occupant = 0;
                }
            }
        }

        paintTileState(r, c);
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    /**
     * Wire up toggles on the right for switching editorMode.
     */
    private void layerAddListeners() {
        removeTileModeBtn.addActionListener(e -> {
            if (removeTileModeBtn.isSelected()) {
                editorMode = 0;
            }
        });
        buildModeBtn.addActionListener(e -> {
            if (buildModeBtn.isSelected()) {
                editorMode = 1;
            }
        });
        workerModeBtn.addActionListener(e -> {
            if (workerModeBtn.isSelected()) {
                editorMode = 2;
            }
        });
    }

    /**
     * Wire up left‐side controls: uploads, dependency enabling/disabling, saving config.
     */
    private void attachControlListeners() {
        numPlayersBox.addActionListener(e -> {
            int n = (Integer) numPlayersBox.getSelectedItem();
            if (n == 1) {
                god2Box.setEnabled(false);
                god2Box.setSelectedIndex(0);
            } else {
                god2Box.setEnabled(true);
            }
        });

        allowGodChk.addActionListener(e -> {
            boolean v = allowGodChk.isSelected();
            allowGodSelectChk.setEnabled(v);
            if (!v) {
                allowGodSelectChk.setSelected(false);
                god1Box.setEnabled(false);
                god2Box.setEnabled(false);
            } else {
                allowGodSelectChk.setEnabled(true);
                god1Box.setEnabled(allowGodSelectChk.isSelected());
                god2Box.setEnabled(allowGodSelectChk.isSelected()
                        && (Integer)numPlayersBox.getSelectedItem() == 2);
            }
        });

        allowGodSelectChk.addActionListener(e -> {
            boolean v = allowGodSelectChk.isSelected();
            god1Box.setEnabled(v);
            god2Box.setEnabled(v && (Integer)numPlayersBox.getSelectedItem() == 2);
        });

        coreModeBox.addActionListener(e -> {
            String mode = (String) coreModeBox.getSelectedItem();
            if (mode.equals("Classic")) {
                allowGodChk.setSelected(false);
                allowGodChk.setEnabled(false);
                allowGodSelectChk.setEnabled(false);
                god1Box.setEnabled(false);
                god2Box.setEnabled(false);
            } else if (mode.equals("Tag")) {
                numPlayersBox.setSelectedItem(2);
                numPlayersBox.setEnabled(false);
                numWorkersBox.setSelectedItem(1);
                numWorkersBox.setEnabled(false);
                allowGodChk.setSelected(true);
                allowGodChk.setEnabled(false);
                allowGodSelectChk.setEnabled(false);
                god1Box.setSelectedItem("Artemis");
                god1Box.setEnabled(false);
                god2Box.setSelectedItem("Artemis");
                god2Box.setEnabled(false);
                allowMoveChk.setSelected(true);
                allowBuildChk.setSelected(true);
            } else {
                numPlayersBox.setEnabled(true);
                numWorkersBox.setEnabled(true);
                allowGodChk.setEnabled(true);
                allowGodSelectChk.setEnabled(allowGodChk.isSelected());
                god1Box.setEnabled(allowGodChk.isSelected() && allowGodSelectChk.isSelected());
                god2Box.setEnabled(allowGodChk.isSelected() && allowGodSelectChk.isSelected()
                        && (Integer) numPlayersBox.getSelectedItem() == 2);
            }
        });

        bgUploadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "JPEG Images", "jpg", "jpeg"));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File chosen = chooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(chosen);
                Image scaled = img.getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
                bgLabel.setIcon(new ImageIcon(scaled));
                bgLabel.revalidate();
                bgLabel.repaint();
                customBgFile = chosen;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load image: " + ex.getMessage());
            }
        });

        musicUploadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "WAV Files", "wav"));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File chosen = chooser.getSelectedFile();
            playBackgroundMusic(chosen);
            customMusicFile = chosen;
        });

        saveModeBtn.addActionListener(e -> {
            String modeName = modeNameField.getText().trim();
            if (modeName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a valid mode name.");
                return;
            }
            File modeFolder = new File(ASSETS_DIR + "/" + modeName);
            if (modeFolder.exists()) {
                int ans = JOptionPane.showConfirmDialog(this,
                        "Mode already exists. Overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ans != JOptionPane.YES_OPTION) return;
                try {
                    Files.walk(modeFolder.toPath())
                            .map(Path::toFile)
                            .sorted(Comparator.reverseOrder())
                            .forEach(File::delete);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to overwrite old mode.");
                    return;
                }
            }
            modeFolder.mkdirs();

            try {
                if (customBgFile != null) {
                    Files.copy(customBgFile.toPath(),
                            new File(modeFolder, "gameboard.jpg").toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } else {
                    try (InputStream is = getClass().getResourceAsStream(DEFAULT_BG)) {
                        Files.copy(is, new File(modeFolder, "gameboard.jpg").toPath());
                    }
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save background: " + ex.getMessage());
                return;
            }

            if (customMusicFile != null) {
                try {
                    Files.copy(customMusicFile.toPath(),
                            new File(modeFolder, "music.wav").toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Failed to save music: " + ex.getMessage());
                    return;
                }
            }

            try (PrintWriter pw = new PrintWriter(
                    new File(modeFolder, "config.properties"))) {
                pw.println("rows=" + numRows);
                pw.println("cols=" + numCols);
                pw.println("allowMove=" + allowMoveChk.isSelected());
                pw.println("allowBuild=" + allowBuildChk.isSelected());
                int nPlayers = (Integer) numPlayersBox.getSelectedItem();
                pw.println("numPlayers=" + nPlayers);
                pw.println("workersPerPlayer=" + numWorkersBox.getSelectedItem());
                pw.println("allowGodPower=" + allowGodChk.isSelected());
                pw.println("allowGodSelect=" + allowGodSelectChk.isSelected());
                pw.println("god1=" + (String) god1Box.getSelectedItem());
                pw.println("god2=" + (String) god2Box.getSelectedItem());
                pw.println("coreMode=" + (String) coreModeBox.getSelectedItem());

                pw.println("tileRows=" + numRows);
                pw.println("tileCols=" + numCols);
                for (int r = 0; r < numRows; r++) {
                    for (int c = 0; c < numCols; c++) {
                        TileState ts = tileStates[r][c];
                        pw.printf("tile.%d.%d.present=%b%n", r, c, ts.present);
                        pw.printf("tile.%d.%d.level=%d%n", r, c, ts.level);
                        pw.printf("tile.%d.%d.dome=%b%n", r, c, ts.dome);
                        pw.printf("tile.%d.%d.occupant=%d%n", r, c, ts.occupant);
                    }
                }
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }

            JOptionPane.showMessageDialog(this, "Mode “" + modeName + "” saved in assets/" + modeName);
            dispose();
        });
    }

    /**
     *Plays/loops the given WAV in the background using Java Sound Clip.
     */
    private void playBackgroundMusic(File wavFile) {
        // Stop any existing clip
        if (musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
            musicClip.close();
        }
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile)) {
            musicClip = AudioSystem.getClip();
            musicClip.open(ais);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to play WAV: " + ex.getMessage());
        }
    }
}