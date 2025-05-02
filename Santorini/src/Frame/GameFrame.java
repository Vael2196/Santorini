package Frame;

import Frame.Boards.Board;
import GameEntities.Player;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Objects;

/**
 * Frame with a 5x5 board (game screen)
 */
public class GameFrame extends JFrame {
    private final TileButton[][] buttons;
    private final BackgroundLabel[][] backLabels;
    private static final String GAMEBOARD = "../project/Santorini/src/assets/gameboard.jpg";
    private JTextArea infoLabel;

    public GameFrame(Board board, Controller controller) {
        Dimension screenSize = new Dimension(1215, 865);
        if (screenSize.getHeight() > Toolkit.getDefaultToolkit().getScreenSize().getHeight() || screenSize.getWidth() > Toolkit.getDefaultToolkit().getScreenSize().getWidth()) {
            screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        }
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();
        setTitle("Santorini");
        setSize(screenSize);
        setResizable(false);

        JPanel centeredPanel = new ImagePanel(GAMEBOARD);
        JPanel content;
        centeredPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        centeredPanel.setPreferredSize(screenSize);

        int rows = board.getRows(), cols = board.getCols();
        buttons = new TileButton[rows][cols];
        backLabels = new BackgroundLabel[rows][cols];

        Border border = BorderFactory.createLineBorder(new Color(0, 0, 0));
        content = new JPanel(new GridLayout(2, 1));
        //content.setPreferredSize(new Dimension((int) (screenSize.getWidth()/6), (int) screenSize.getHeight()));
        infoLabel = new JTextArea("Place your workers in any 2 empty tiles.\n \nPlayer 1's Turn");
        infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        infoLabel.setBorder(border);
        infoLabel.setBackground(Color.WHITE);
        infoLabel.setEditable(false); // Make it non-editable
        infoLabel.setLineWrap(true);
        infoLabel.setWrapStyleWord(true);
        //constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.weightx = 0.5;
        constraints.anchor = GridBagConstraints.LAST_LINE_START;
        constraints.insets = new Insets(0, 10, 0, 0);
        content.add(infoLabel);
        content.setBackground(new Color(0, 0, 0, 0));
        centeredPanel.add(content, constraints);

        content = getjPanel(rows, cols);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0;
        constraints.gridx = 1;
        constraints.insets = new Insets(35, 0, 20, 60);
        //contentPanel.setBorder(new EmptyBorder(62, 20, 10, 10));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Load the image
                TileButton newButton = new TileButton(board.getTile(r, c), controller);
                BackgroundLabel newLabel = new BackgroundLabel(board.getTile(r, c));
                // Remove default button styling (optional)
                backLabels[r][c] = newLabel;
                backLabels[r][c].add(newButton);
                buttons[r][c] = newButton;
                content.add(backLabels[r][c]);
            }
        }

        centeredPanel.add(content, constraints);

        content = new JPanel(new FlowLayout(FlowLayout.LEADING));
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 2;
        constraints.insets = new Insets(0, 40, 0, 0);
        //content.setPreferredSize(new Dimension((int) (screenSize.getWidth()/6), (int) screenSize.getHeight()));
        content.setBackground(new Color(0, 0, 0, 0));
        centeredPanel.add(content, constraints);



        //centeredPanel.add(Box.createVerticalGlue());
        //centeredPanel.add(Box.createHorizontalStrut(100));
        //centeredPanel.add(Box.createVerticalStrut(100));
        //centeredPanel.add(contentPanel);
        //centeredPanel.add(Box.createHorizontalStrut(100));
       // centeredPanel.add(Box.createVerticalGlue());
        add(centeredPanel);
        //background.add(panel);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void updateLabels(GameController.Phase phase, int playerIndex) {
        String playerString = (playerIndex == 0)? "Player 1's Turn" : "Player 2's Turn";
        switch (phase) {
            case SETUP -> infoLabel.setText("Place your workers in any 2 empty tiles.\n \n" + playerString);
            case SELECT -> infoLabel.setText("Select one of your workers.\n \n" + playerString);
            case MOVE -> infoLabel.setText("Select an adjacent tile to move into.\n \n" + playerString);
            case BUILD -> infoLabel.setText("Select an adjacent tile to build on.\n \n" + playerString);
        }
    }



    private JPanel getjPanel(int rows, int cols) {
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setBackground(new Color(0, 0, 0, 0));
        //contentPanel.setMinimumSize(new Dimension(400, 400)); // Set minimum height
        contentPanel.setPreferredSize(new Dimension(750, 750));
        GridLayout gridLayout = new GridLayout(rows, cols);
        gridLayout.setHgap(15);
        gridLayout.setVgap(15);
        contentPanel.setLayout(gridLayout);// Set preferred height
        return contentPanel;
    }

    public void refresh() {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                buttons[row][col].updateAppearance();
                backLabels[row][col].updateAppearance();
            }
        }
        /*for (TileButton[] row : buttons)
            for (TileButton b : row)
                b.updateAppearance();*/
    }


}