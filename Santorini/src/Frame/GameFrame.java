package Frame;

import javax.swing.*;
import java.awt.*;

/**
 * Frame with a 5x5 board (game screen)
 */
public class GameFrame extends JFrame {
    private Board board;
    private TileButton[][] buttons;

    public GameFrame() {
        board = new Board();
        buttons = new TileButton[Board.SIZE][Board.SIZE];

        setTitle("Santorini");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(Board.SIZE, Board.SIZE));

        // Makes each tile interactable (puts tile buttons on the board)
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                buttons[row][col] = new TileButton(row, col, board.getCell(row, col));
                add(buttons[row][col]);
            }
        }
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
