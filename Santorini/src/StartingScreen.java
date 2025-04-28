

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;




public class StartingScreen {
    public static void main(String[] args) {
        JFrame startingframe = new JFrame();
        startingframe.setVisible(true);
        startingframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startingframe.setSize(500,500);
        startingframe.setTitle("Santorini Board Game");
        startingframe.getContentPane().setBackground(new Color(135,206,235));
        startingframe.setLocationRelativeTo(null);

        Border border = BorderFactory.createLineBorder(new Color(135,206,235));


        JLabel textlabel = new JLabel("Welcome to Santorini Board Game");
        textlabel.setForeground(new Color(0,60,90));
        textlabel.setFont(new Font("Arial", Font.BOLD, 20 ));
        textlabel.setBounds(50,150,400,100);
        textlabel.setBorder(border);
        textlabel.setHorizontalAlignment(JLabel.CENTER);
        textlabel.setVerticalAlignment(JLabel.CENTER);
        startingframe.add(textlabel);

        JButton button = new JButton("Start");
        button.setBounds(200, 400,100,50);
        //startingframe.add(button);



    }
}
