import Frame.Boards.Board;
import Frame.*;
import GameEntities.Gods.*;
import GameEntities.*;

import javax.swing.*;
import java.util.*;
import java.util.function.Supplier;


public class Main {
    public static void main(String[] args) {
        Map<String, Supplier<GodPower>> godMap = new LinkedHashMap<>();
        godMap.put("Artemis", Artemis::new);
        godMap.put("Demeter", Demeter::new);

        List<String> choices = new ArrayList<>();
        choices.add("Random");
        choices.addAll(godMap.keySet());

        Random rand = new Random();

        Player p1 = new Player("Blue");
        Player p2 = new Player("Red");
        List<Player> players = Arrays.asList(p1, p2);

        for (Player p : players) {
            String selected = (String) JOptionPane.showInputDialog(null, p.getColor() + " — select your God power:", "God Selection", JOptionPane.PLAIN_MESSAGE, null, choices.toArray(), choices.get(0));

            if (selected == null || selected.equals("Random")) {
                List<String> names = new ArrayList<>(godMap.keySet());
                selected = names.get(rand.nextInt(names.size()));
            }
            Supplier<GodPower> sup = godMap.get(selected);
            p.setGodPower(sup.get());
        }

        Board board = Board.createSquare(5);

        //Create and add workers to players
        p1.addWorker(new Worker(p1, board.getTile(0,0)));
        p1.addWorker(new Worker(p1, board.getTile(0,1)));
        p2.addWorker(new Worker(p2, board.getTile(4,4)));
        p2.addWorker(new Worker(p2, board.getTile(4,3)));

        GameController coreController = new GameController(board, null, players);
        GodPowerController godController = new GodPowerController(coreController, players);

        GameFrame frame = new GameFrame(board, godController);
        coreController.setFrame(frame);
        godController.setFrame(frame);

        SwingUtilities.invokeLater(() -> {});
    }

}