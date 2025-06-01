package Frame;

import Frame.Boards.Board;
import Frame.Memento.GameCaretaker;
import Frame.Memento.GameMemento;
import GameEntities.Player;
import GameEntities.Worker;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Frame with a 5×5 board (or custom dimension),
 * Supports either the default internal background or a custom file, and optional WAV music.
 */
public class GameFrame extends JFrame {
    private TileButton[][] buttons;
    private BackgroundLabel[][] backLabels;
    private JButton saveButton;

    private JTextArea infoLabel;

    private static final String DEFAULT_BG = "/assets/gameboard.jpg";

    private Clip musicClip = null;

    private JLayeredPane layered;


    /**
     * Construct with the default built‐in background image and no music.
     */
    public GameFrame(Board board, Controller controller) {
        super("Santorini");
        initCommon(board, controller);
        loadResourceBackground(DEFAULT_BG);

        layered.validate();
        layered.repaint();
    }

    /**
     * Construct with a custom background image file (JPEG) and optional WAV file for music.
     * Both files can be null to fall back to defaults.
     */
    public GameFrame(Board board, Controller controller,
                     File backgroundImageFile, File musicWavFile) {
        super("Santorini");
        initCommon(board, controller);

        if (backgroundImageFile != null && backgroundImageFile.exists()) {
            loadFileBackground(backgroundImageFile);
        } else {
            loadResourceBackground(DEFAULT_BG);
        }

        if (musicWavFile != null && musicWavFile.exists()) {
            playBackgroundMusic(musicWavFile);
        }

        layered.validate();
        layered.repaint();
    }

    private void initCommon(Board board, Controller controller) {
        setSize(1215, 865);
        setResizable(false);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        layered = new JLayeredPane();
        layered.setBounds(0, 0, 1215, 865);

        Set<Point> holeSet = Collections.emptySet();
        if (controller instanceof TagController) {
            holeSet = ((TagController) controller).getHoles();
        } else if (controller instanceof GodPowerController) {
            GameController core = ((GodPowerController) controller).getCore();
            if (core != null) {
                holeSet = core.getHoles();
            }
        } else if (controller instanceof GameController) {
            holeSet = ((GameController) controller).getHoles();
        }

        int boardWidth  = 750;
        int boardHeight = 750;
        int boardX = (1215 - boardWidth) / 2;
        int boardY = (865 - boardHeight) / 2;

        int rows = board.getRows(), cols = board.getCols();
        JPanel boardPanel = new JPanel(new GridLayout(rows, cols, 15, 15));
        boardPanel.setOpaque(false);
        boardPanel.setBounds(boardX, boardY, boardWidth, boardHeight);

        buttons = new TileButton[rows][cols];
        backLabels = new BackgroundLabel[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                TileButton btn = new TileButton(board.getTile(r, c), controller);
                BackgroundLabel lbl = new BackgroundLabel(board.getTile(r, c), holeSet);
                backLabels[r][c] = lbl;
                lbl.add(btn);
                buttons[r][c] = btn;
                boardPanel.add(lbl);
            }
        }
        layered.add(boardPanel, Integer.valueOf(1));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                backLabels[r][c].updateAppearance();
            }
        }

        // ─── Info Label (left) ───
        infoLabel = new JTextArea("Place your workers in any 2 empty tiles.\n\nPlayer 1's Turn");
        infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        infoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        infoLabel.setBackground(Color.WHITE);
        infoLabel.setEditable(false);
        infoLabel.setLineWrap(true);
        infoLabel.setWrapStyleWord(true);

        JScrollPane infoScroll = new JScrollPane(infoLabel);
        infoScroll.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        infoScroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        int infoW = 200, infoH = 200;
        int infoX = boardX - infoW - 10;
        int infoY = boardY + (boardHeight - infoH) / 2;
        infoScroll.setBounds(infoX, infoY, infoW, infoH);
        layered.add(infoScroll, Integer.valueOf(2));


        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        savePanel.setOpaque(false);
        savePanel.setBounds(boardX + boardWidth + 10, boardY + 10, 120, 40);

        saveButton = new JButton("Save Game");
        saveButton.setFont(new Font("Arial", Font.BOLD, 16));
        saveButton.addActionListener(e -> attemptSave(controller));
        savePanel.add(saveButton);
        layered.add(savePanel, Integer.valueOf(2));


        add(layered);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Load a built‐in resource background (from inside the JAR) at layer 0.
     * (Now adds directly to our `layered` field.)
     */
    private void loadResourceBackground(String resourcePath) {
        URL bgUrl = getClass().getResource(resourcePath);
        if (bgUrl == null) {
            getContentPane().setBackground(Color.LIGHT_GRAY);
            return;
        }
        ImageIcon rawIcon = new ImageIcon(bgUrl);
        Image rawImg = rawIcon.getImage();
        Image scaledImg = rawImg.getScaledInstance(
                getWidth(), getHeight(), Image.SCALE_SMOOTH);
        ImageIcon bgIcon = new ImageIcon(scaledImg);

        JLabel bgLabel = new JLabel(bgIcon);
        bgLabel.setBounds(0, 0, getWidth(), getHeight());
        layered.add(bgLabel, Integer.valueOf(0));
    }

    /**
     * Load a custom image file from disk as the background
     */
    private void loadFileBackground(File imageFile) {
        try {
            Image rawImg = ImageIO.read(imageFile);
            Image scaledImg = rawImg.getScaledInstance(
                    getWidth(), getHeight(), Image.SCALE_SMOOTH);
            JLabel bgLabel = new JLabel(new ImageIcon(scaledImg));
            bgLabel.setBounds(0, 0, getWidth(), getHeight());
            layered.add(bgLabel, Integer.valueOf(0));
        } catch (IOException ex) {
            loadResourceBackground(DEFAULT_BG);
        }
    }

    /**
     * Plays/loops the given WAV file continuously in the background.
     */
    private void playBackgroundMusic(File wavFile) {
        if (musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
            musicClip.close();
        }

        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile)) {
            musicClip = AudioSystem.getClip();
            musicClip.open(ais);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to play music: " + ex.getMessage());
        }
    }


    public void updateLabels(GameController.Phase phase, int playerIndex) {
        String playerString = (playerIndex == 0) ? "Player 1's Turn" : "Player 2's Turn";
        switch (phase) {
            case SETUP -> infoLabel.setText("Place your workers in any 2 empty tiles.\n\n" + playerString);
            case SELECT -> infoLabel.setText("Select one of your workers.\n\n" + playerString);
            case MOVE -> infoLabel.setText("Select an adjacent tile to move into.\n\n" + playerString);
            case BUILD -> infoLabel.setText("Select an adjacent tile to build on.\n\n" + playerString);
        }
    }

    public void refresh() {
        for (int r = 0; r < buttons.length; r++) {
            for (int c = 0; c < buttons[r].length; c++) {
                buttons[r][c].updateAppearance();
                backLabels[r][c].updateAppearance();
            }
        }
    }

    private void attemptSave(Controller controller) {
        if (!(controller instanceof GodPowerController)) {
            JOptionPane.showMessageDialog(this, "Cannot save: invalid controller.");
            return;
        }
        GodPowerController godCtrl = (GodPowerController) controller;
        if (godCtrl.getCorePhase() != GameController.Phase.SELECT) {
            JOptionPane.showMessageDialog(this,
                    "You can only save at the start of your turn (before moving).");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int ret = chooser.showSaveDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try {
            GameMemento m = new GameMemento();
            coreToMemento(godCtrl, m);
            GameCaretaker.saveToFile(m, file);
            JOptionPane.showMessageDialog(this, "Game saved successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    private void coreToMemento(GodPowerController godCtrl, GameMemento m) {
        int idx = godCtrl.getCurrentPlayerIndex();
        m.setCurrentPlayerIndex(idx);

        List<Player> players = godCtrl.getPlayers();
        for (Player p : players) {
            m.addPlayer(p.getColor(), p.getGodPower().getName());
        }

        for (Player p : players) {
            for (Worker w : p.getWorkers()) {
                Tile t = w.getCurrentTile();
                m.addWorkerLine(p.getColor() + ":" + t.getX() + "," + t.getY());
            }
        }

        Board board = godCtrl.getCoreBoard();
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Tile tile = board.getTile(r, c);
                int level = tile.getBuilding().getLevel();
                int dome  = tile.getBuilding().hasDome() ? 1 : 0;
                String occupant = tile.isOccupied()
                        ? tile.getOccupant().getPlayer().getColor()
                        : "none";
                m.addTileLine(r + "," + c + ":" + level + "," + dome + "," + occupant);
            }
        }
    }
}