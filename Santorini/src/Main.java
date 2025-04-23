import Frame.Boards.Board;
import Frame.*;
import GameEntities.Gods.*;
import GameEntities.*;
import javax.swing.*;

/**
 * This code just makes game frame to appear on the screen (makes a new instance, if you wanna be more technical), nothing fancy
 */
public class Main {
    public static void main(String[] args) {
        // Initialise a 5×5 square board
        Board board = Board.createSquare(5);

        // Make two players
        Player p1 = new Player("Blue");
        Player p2 = new Player("Red");

        // Assign default god cards (for the testing purposes)
        p1.setGodCard(new Artemis());
        p2.setGodCard(new Demeter());

        // Force place workers
        Worker w1 = new Worker(p1, board.getTile(0, 0));
        Worker w2 = new Worker(p1, board.getTile(0, 1));
        p1.addWorker(w1); p1.addWorker(w2);

        Worker w3 = new Worker(p2, board.getTile(4, 4));
        Worker w4 = new Worker(p2, board.getTile(4, 3));
        p2.addWorker(w3); p2.addWorker(w4);

        // Launch GUI
        SwingUtilities.invokeLater(() -> new GameFrame(board));
    }

}