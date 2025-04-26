package Frame;

import Frame.Boards.Board;

import javax.swing.*;
import java.awt.*;

/**
 * Frame with a 5x5 board (game screen)
 * Just played around with some values in a code, for the most part the thing is from StackOverflow
 */
public class GameFrame extends JFrame {
    private final TileButton[][] buttons;

    public GameFrame(Board board, GameController controller) {
        int rows = board.getRows(), cols = board.getCols();
        buttons = new TileButton[rows][cols];
        setLayout(new GridLayout(rows, cols));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                buttons[r][c] = new TileButton(board.getTile(r, c), controller);
                add(buttons[r][c]);
            }
        }
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void refresh() {
        for (TileButton[] row : buttons)
            for (TileButton b : row)
                b.updateAppearance();
    }
}