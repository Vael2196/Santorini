import Frame.Boards.Board;
import Frame.*;
import GameEntities.Gods.*;
import GameEntities.*;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        //Create Board (Square)
        Board board = Board.createSquare(5);

        //Instantiate Players
        Player p1 = new Player("Purple");
        Player p2 = new Player("Red");

        //Create and Set God Cards
        p1.setGodCard(new Artemis());
        p2.setGodCard(new Demeter());

        //Create and add workers to players
        p1.addWorker(new Worker(p1, board.getTile(0,0)));
        p1.addWorker(new Worker(p1, board.getTile(0,1)));
        p2.addWorker(new Worker(p2, board.getTile(4,4)));
        p2.addWorker(new Worker(p2, board.getTile(4,3)));

        //Create list of players
        List<Player> players = Arrays.asList(p1, p2);
        GameController controller = new GameController(board, null, players);
        GameFrame frame = new GameFrame(board, controller);
        controller.setFrame(frame);

        SwingUtilities.invokeLater(() -> {});
    }

}