

import Frame.Boards.Board;
import Frame.GameController;
import Frame.GameFrame;
import Frame.GodPowerController;
import Frame.ImagePanel;
import GameEntities.Gods.Artemis;
import GameEntities.Gods.Demeter;
import GameEntities.Player;
import GameEntities.Worker;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;


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
        startingFrame.setTitle("Santorini Board Game");
        //startingFrame.getContentPane().setBackground(STARTSCREEN);
        startingFrame.setLocationRelativeTo(null);
        startingFrame.setContentPane(imgPanel);

        Border border = BorderFactory.createLineBorder(new Color(135, 206, 235));

        JLabel textLabel = new JLabel("Santorini Board Game");
        textLabel.setForeground(new Color(0, 60, 90));
        textLabel.setFont(new Font("Arial", Font.BOLD, 24));
        textLabel.setBounds(50, 150, 400, 100);
        textLabel.setBorder(border);
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setVerticalAlignment(JLabel.CENTER);
        startingFrame.add(textLabel);

        JButton startButton = new JButton("Start");
        startButton.setBounds(200, 350, 100, 50);
        //startButton.setBounds(200, 400, 100, 50);
        textLabel.add(startButton);

        JButton exitButton =  new JButton("Exit");
        exitButton.setBounds(200,400,100,50);
        textLabel.add(exitButton);

        startButton.addActionListener( a ->{
            Board board = Board.createSquare(5);

            Player p1 = new Player("Blue");
            Player p2 = new Player("Red");
            p1.setGodPower(new Artemis());
            p2.setGodPower(new Demeter());

            Worker w1 = new Worker(p1, board.getTile(0,0));
            Worker w2 = new Worker(p1, board.getTile(0,1));
            p1.addWorker(w1);
            p1.addWorker(w2);
            Worker w3 = new Worker(p2, board.getTile(4,4));
            Worker w4 = new Worker(p2, board.getTile(4,3));
            p2.addWorker(w3);
            p2.addWorker(w4);

            List<Player> players = Arrays.asList(p1, p2);
            GameController coreController = new GameController(board, null, players);
            GodPowerController godController = new GodPowerController(coreController, players);

            GameFrame frame = new GameFrame(board, godController);
            coreController.setFrame(frame);
            godController.setFrame(frame);

            SwingUtilities.invokeLater(() -> {});
        });

        exitButton.addActionListener( a -> {
            System.exit(0);
        });


    }


}
