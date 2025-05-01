import Frame.Boards.Board;
import Frame.*;
import GameEntities.Gods.*;
import GameEntities.*;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Board board   = Board.createSquare(5);
        Player p1     = new Player("Blue");
        Player p2     = new Player("Red");
        p1.setGodPower(new Artemis());
        p2.setGodPower(new Demeter());

        //Create and add workers to players
        p1.addWorker(new Worker(p1, board.getTile(0,0)));
        p1.addWorker(new Worker(p1, board.getTile(0,1)));
        p2.addWorker(new Worker(p2, board.getTile(4,4)));
        p2.addWorker(new Worker(p2, board.getTile(4,3)));

        //Create list of players
        List<Player> players = Arrays.asList(p1, p2);
        GameController coreController = new GameController(board, null, players);
        GodPowerController godController = new GodPowerController(coreController, players);

        GameFrame frame = new GameFrame(board, godController);
        coreController.setFrame(frame);
        godController.setFrame(frame);

        SwingUtilities.invokeLater(() -> {});
    }

}