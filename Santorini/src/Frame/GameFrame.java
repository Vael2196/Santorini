package Frame;

import Frame.Boards.Board;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;

/**
 * Frame with a 5x5 board (game screen)
 * Just played around with some values in a code, for the most part the thing is from StackOverflow
 */
public class GameFrame extends JFrame {
    private final TileButton[][] buttons;

    public GameFrame(Board board, GameController controller) {
        setTitle("Santorini");
        setSize(1000, 800);
        setContentPane(new ImagePanel("C:/Users/colli/OneDrive/Desktop/Monash/YR2025/programming/santorini/project/Santorini/src/assets/background.jpg"));
        int rows = board.getRows(), cols = board.getCols();
        buttons = new TileButton[rows][cols];
        setLayout(new GridLayout(rows, cols));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Load the image
                ImageIcon icon = new ImageIcon("C:/Users/colli/OneDrive/Desktop/Monash/YR2025/programming/santorini/project/Santorini/src/assets/tile_blank.png");
                TileButton newButton = new TileButton(board.getTile(r, c), controller, icon);
                // Remove default button styling (optional)
                newButton.setBorderPainted(false);
                newButton.setFocusPainted(false);
                newButton.setContentAreaFilled(false);
                buttons[r][c] = newButton;
                add(buttons[r][c]);
            }
        }
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void refresh() {
        for (TileButton[] row : buttons)
            for (TileButton b : row)
                b.updateAppearance();
    }

    class ImagePanel extends JPanel {
        private Image image;

        public ImagePanel(String imagePath) {
            try {
                image = ImageIO.read(new File(imagePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }
}