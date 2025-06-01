package Frame;

import Frame.Boards.Board;
import Frame.Memento.GameCaretaker;
import Frame.Memento.GameMemento;
import GameEntities.Player;
import GameEntities.Worker;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Frame with a 5x5 board (game screen)
 */
public class GameFrame extends JFrame {
    private final TileButton[][] buttons;
    private final BackgroundLabel[][] backLabels;
    private static final String GAMEBOARD = "/assets/gameboard.jpg";

    private JTextArea infoLabel;
    private final JButton saveButton;

    public GameFrame(Board board, Controller controller) {
        setTitle("Santorini");
        setSize(1215, 865);
        setResizable(false);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLayeredPane layered = new JLayeredPane();
        layered.setBounds(0, 0, 1215, 865);

        URL bgUrl = GameFrame.class.getResource(GAMEBOARD);
        ImageIcon rawIcon = new ImageIcon(bgUrl);
        Image rawImg = rawIcon.getImage();
        Image scaledImg = rawImg.getScaledInstance(1215, 865, Image.SCALE_SMOOTH);
        ImageIcon bgIcon = new ImageIcon(scaledImg);

        JLabel bgLabel = new JLabel(bgIcon);
        bgLabel.setBounds(0, 0, 1215, 865);
        layered.add(bgLabel, Integer.valueOf(0));

        int boardWidth  = 750;
        int boardHeight = 750;
        int boardX = (1215 - boardWidth) / 2;
        int boardY = (865  - boardHeight) / 2;

        int rows = board.getRows(), cols = board.getCols();
        JPanel boardPanel = new JPanel(new GridLayout(rows, cols, 15, 15));
        boardPanel.setOpaque(false);
        boardPanel.setBounds(boardX, boardY, boardWidth, boardHeight);

        buttons    = new TileButton[rows][cols];
        backLabels = new BackgroundLabel[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                TileButton btn = new TileButton(board.getTile(r, c), controller);
                BackgroundLabel lbl = new BackgroundLabel(board.getTile(r, c));
                backLabels[r][c] = lbl;
                lbl.add(btn);
                buttons[r][c] = btn;
                boardPanel.add(lbl);
            }
        }
        layered.add(boardPanel, Integer.valueOf(1));

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