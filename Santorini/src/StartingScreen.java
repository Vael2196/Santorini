

import Frame.Boards.Board;
import Frame.GameController;
import Frame.GameFrame;
import Frame.GodPowerController;
import Frame.ImagePanel;
import Frame.Memento.*;
import GameEntities.Gods.*;
import GameEntities.Player;
import GameEntities.Worker;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;


public class StartingScreen {

    private static final String STARTSCREEN = "/assets/startscreen.jpg";

    public static void main(String[] args) {
        JFrame startingFrame = new JFrame();
        URL url = StartingScreen.class.getResource(STARTSCREEN);
        ImageIcon icon = new ImageIcon(url);
        JPanel imgPanel = new ImagePanel(icon);
        startingFrame.setVisible(true);
        startingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startingFrame.setSize(500, 500);
        startingFrame.setResizable(false);
        startingFrame.setTitle("Santorini Board Game");
        //startingFrame.getContentPane().setBackground(STARTSCREEN);
        startingFrame.setLocationRelativeTo(null);
        startingFrame.setContentPane(imgPanel);

        JLabel textLabel = new JLabel("Santorini Board Game");
        textLabel.setForeground(new Color(0, 60, 90));
        textLabel.setFont(new Font("Arial", Font.BOLD, 24));
        textLabel.setBounds(50, 20, 400, 50);
        textLabel.setVerticalAlignment(JLabel.TOP);
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        startingFrame.add(textLabel);

        JButton startButton = new JButton("Start");
        startButton.setBounds(200, 335, 100, 50);
        startButton.setBorderPainted(false);
        startButton.setBackground(new Color(0, 0, 0, 0));
        startButton.setOpaque(false);
        startButton.setFont(new Font("Arial", Font.BOLD, 20));
        //startButton.setBounds(200, 400, 100, 50);
        imgPanel.add(startButton);

        JButton loadButton = new JButton("Load");
        loadButton.setBounds(200, 360, 100, 50);
        loadButton.setBorderPainted(false);
        loadButton.setBackground(new Color(0, 0, 0, 0));
        loadButton.setOpaque(false);
        loadButton.setFont(new Font("Arial", Font.BOLD, 20));
        imgPanel.add(loadButton);

        JButton exitButton =  new JButton("Exit");
        exitButton.setBounds(200,390,100,50);
        exitButton.setBorderPainted(false);
        exitButton.setBackground(new Color(0, 0, 0, 0));
        exitButton.setOpaque(false);
        exitButton.setFont(new Font("Arial", Font.BOLD, 20));
        imgPanel.add(exitButton);

        startButton.addActionListener( a ->{
            Map<String, Supplier<GodPower>> godMap = new LinkedHashMap<>();
            godMap.put("Artemis", Artemis::new);
            godMap.put("Demeter", Demeter::new);
            godMap.put("Triton",  Triton::new);

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
            p1.addWorker(new Worker(p1));
            p1.addWorker(new Worker(p1));
            p2.addWorker(new Worker(p2));
            p2.addWorker(new Worker(p2));

            GameController coreController = new GameController(board, null, players);
            GodPowerController godController = new GodPowerController(coreController, players);

            GameFrame frame = new GameFrame(board, godController);
            coreController.setFrame(frame);
            godController.setFrame(frame);

            SwingUtilities.invokeLater(() -> {});
        });

        loadButton.addActionListener(a -> {
            JFileChooser chooser = new JFileChooser();
            int ret = chooser.showOpenDialog(startingFrame);
            if (ret != JFileChooser.APPROVE_OPTION) return;
            File file = chooser.getSelectedFile();
            try {
                // Load the saved state
                Object[] state = GameCaretaker.loadFromFile(file);
                Board loadedBoard = (Board) state[0];
                @SuppressWarnings("unchecked")
                List<Player> loadedPlayers = (List<Player>) state[1];
                int currentIdx = (int) state[2];

                GameController coreController = new GameController(loadedBoard, null, loadedPlayers);
                GodPowerController godController = new GodPowerController(coreController, loadedPlayers);
                coreController.setCurrentPlayerIndex(currentIdx);
                GameFrame frame = new GameFrame(loadedBoard, godController);
                coreController.setFrame(frame);
                godController.setFrame(frame);

                startingFrame.dispose();
                SwingUtilities.invokeLater(() -> {});
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(startingFrame, "Load failed: " + ex.getMessage());
            }
        });

        exitButton.addActionListener( a -> {
            System.exit(0);
        });


    }


}
