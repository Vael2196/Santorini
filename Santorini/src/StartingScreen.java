

import javax.swing.*;
import java.awt.*;


public class StartingScreen {
    public static void main(String[] args) {
        JFrame startingframe = new JFrame();
        startingframe.setVisible(true);
        startingframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startingframe.setSize(400,400);
        startingframe.setTitle("Santorini Board Game");
        startingframe.getContentPane().setBackground(new Color(135,206,235));

        JLabel textlabel = new JLabel("Welcome to Santorini Board Game", 0);
        textlabel.setForeground(new Color(0,60,90));
        textlabel.setFont(new Font("Arial", Font.BOLD, 20 ));
        startingframe.add(textlabel);

    }
}
