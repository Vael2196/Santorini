

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;




public class StartingScreen {
    public static void main(String[] args) {
        JFrame startingFrame = new JFrame();
        startingFrame.setVisible(true);
        startingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startingFrame.setSize(500,500);
        startingFrame.setTitle("Santorini Board Game");
        startingFrame.getContentPane().setBackground(new Color(135,206,235));
        startingFrame.setLocationRelativeTo(null);

        Border border = BorderFactory.createLineBorder(new Color(135,206,235));

        JLabel textlabel = new JLabel("Welcome to Santorini Board Game");
        textlabel.setForeground(new Color(0,60,90));
        textlabel.setFont(new Font("Arial", Font.BOLD, 20 ));
        textlabel.setBounds(50,150,400,100);
        textlabel.setBorder(border);
        textlabel.setHorizontalAlignment(JLabel.CENTER);
        textlabel.setVerticalAlignment(JLabel.CENTER);
        startingFrame.add(textlabel);

        JButton startButton = new JButton("Start");
        startButton.setBounds(200, 400,100,50);
        //startingframe.add(startButton);



    }
}
