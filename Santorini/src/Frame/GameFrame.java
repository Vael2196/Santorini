package Frame;

import Frame.Boards.Board;

import javax.swing.*;
import java.awt.*;

/**
 * Frame with a 5x5 board (game screen)
 * Just played around with some values in a code, for the most part the thing is from StackOverflow
 */
public class GameFrame extends JFrame {
    private final Board board;
    private final TileButton[][] buttons;

    public GameFrame(Board board) {
        this.board = board;
        int rows = board.getRows(), cols = board.getCols();
        buttons = new TileButton[rows][cols];
        setTitle("Santorini");
        setLayout(new GridLayout(rows, cols));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                buttons[r][c] = new TileButton(board.getTile(r, c));
                add(buttons[r][c]);
            }
        }
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** Call every time after something happens on the board to refresh UI **/
    public void refresh() {
        for (TileButton[] row : buttons)
            for (TileButton b : row)
                b.updateAppearance();
    }
}