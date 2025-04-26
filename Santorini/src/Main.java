import Frame.Boards.Board;
import Frame.*;
import GameEntities.Gods.*;
import GameEntities.*;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Board board = Board.createSquare(5);

        Player p1 = new Player("Blue");
        Player p2 = new Player("Red");
        p1.setGodCard(new Artemis());
        p2.setGodCard(new Demeter());

        Worker w1 = new Worker(p1, board.getTile(0,0));
        Worker w2 = new Worker(p1, board.getTile(0,1));
        p1.addWorker(w1);
        p1.addWorker(w2);
        Worker w3 = new Worker(p2, board.getTile(4,4));
        Worker w4 = new Worker(p2, board.getTile(4,3));
        p2.addWorker(w3);
        p2.addWorker(w4);

        List<Player> players = Arrays.asList(p1, p2);
        GameController controller = new GameController(board, null, players);
        GameFrame frame = new GameFrame(board, controller);
        controller.setFrame(frame);

        SwingUtilities.invokeLater(() -> {});
    }

}