package Frame;

import Frame.Boards.Board;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Frame with a 5x5 board (game screen)
 */
public class GameFrame extends JFrame {
    private final TileButton[][] buttons;
    private static final String GAMEBOARD = "C:/Users/colli/Monash/git/fit3077/project/Santorini/src/assets/gameboard.jpg";

    public GameFrame(Board board, Controller controller) {
        Dimension screenSize = new Dimension(1215, 865);
        if (screenSize.getHeight() > Toolkit.getDefaultToolkit().getScreenSize().getHeight() || screenSize.getWidth() > Toolkit.getDefaultToolkit().getScreenSize().getWidth()) {
            screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        }
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();
        setTitle("Santorini");
        setSize(screenSize);
        //setExtendedState(JFrame.MAXIMIZED_BOTH);
        //background.setLayout(new FlowLayout(FlowLayout.CENTER));
        //JPanel panel = new JPanel();
        //panel.setSize((int) (width), (int) (height));

        int rows = board.getRows(), cols = board.getCols();
        buttons = new TileButton[rows][cols];
        //setLayout(new BorderLayout());
        JPanel centeredPanel = new ImagePanel(GAMEBOARD);
        centeredPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        centeredPanel.setPreferredSize(screenSize);

        JPanel contentPanel = getjPanel(rows, cols);
        contentPanel.setBorder(new EmptyBorder(62, 20, 10, 10));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Load the image
                TileButton newButton = new TileButton(board.getTile(r, c), controller);
                // Remove default button styling (optional)
                buttons[r][c] = newButton;
                contentPanel.add(buttons[r][c]);
            }
        }
        //centeredPanel.add(Box.createVerticalGlue());
        //centeredPanel.add(Box.createHorizontalStrut(100));
        //centeredPanel.add(Box.createVerticalStrut(100));
        centeredPanel.add(contentPanel);
        //centeredPanel.add(Box.createHorizontalStrut(100));
       // centeredPanel.add(Box.createVerticalGlue());
        add(centeredPanel, BorderLayout.CENTER);
        //background.add(panel);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }



    private JPanel getjPanel(int rows, int cols) {
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setBackground(new Color(0, 0, 0, 0));
        //contentPanel.setMinimumSize(new Dimension(400, 400)); // Set minimum height
        contentPanel.setPreferredSize(new Dimension(750, 750));
        GridLayout gridLayout = new GridLayout(rows, cols);
        gridLayout.setHgap(16);
        gridLayout.setVgap(25);
        contentPanel.setLayout(gridLayout);// Set preferred height
        return contentPanel;
    }

    public void refresh() {
        for (TileButton[] row : buttons)
            for (TileButton b : row)
                b.updateAppearance();
    }


}