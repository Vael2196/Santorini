package Frame.Memento;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;

/**
 * The “Caretaker” that saves a GameMemento to a file or loads it back.
 */
public class GameCaretaker {

    /**
     * Save the given memento to the specified file path.
     * @param m
     * @param file
     * @throws IOException
     */
    public static void saveToFile(GameMemento m, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
            writer.write("CURRENT_PLAYER:" + m.getCurrentPlayerIndex());
            writer.newLine();
            writer.newLine();

            for (int i = 0; i < m.getNumPlayers(); i++) {
                writer.write("PLAYER:"
                        + m.getPlayerColor(i) + ":"
                        + m.getPlayerGodName(i));
                writer.newLine();
            }
            writer.newLine();

            for (String wline : m.getWorkerLines()) {
                writer.write("WORKER:" + wline);
                writer.newLine();
            }
            writer.newLine();

            for (String tline : m.getTileLines()) {
                writer.write("TILE:" + tline);
                writer.newLine();
            }
        }
    }

    /**
     * Load from file, parse out a GameMemento, then call Originator.restore()
     * to recreate board+players. Returns an Object[]: { Board, List<Player>, currentPlayerIndex }.
     * @param file
     * @return
     * @throws Exception
     */
    public static Object[] loadFromFile(File file) throws Exception {
        GameMemento m = new GameMemento();

        for (String raw : Files.readAllLines(file.toPath())) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("CURRENT_PLAYER:")) {
                int idx = Integer.parseInt(line.split(":")[1]);
                m.setCurrentPlayerIndex(idx);

            } else if (line.startsWith("PLAYER:")) {
                String[] parts = line.split(":");
                m.addPlayer(parts[1], parts[2]);

            } else if (line.startsWith("WORKER:")) {
                m.addWorkerLine(line.substring("WORKER:".length()));

            } else if (line.startsWith("TILE:")) {
                m.addTileLine(line.substring("TILE:".length()));
            }
        }

        return GameOriginator.restore(m);
    }
}