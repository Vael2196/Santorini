import javax.swing.*;
import java.awt.*;
 //Reclone repository

public class RestaurantBooking {
    private JFrame frame;
    private JPanel cardPanel;
    private CardLayout cardLayout;


    public RestaurantBooking(){
        //creating the frame
        frame = new JFrame("Booking");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);
        frame.setLocationRelativeTo(null);
        //creating the card panel and card layout
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        //adding the panel
        cardPanel.add(createHomePanel(),"Home Panel");

        frame.add(cardPanel);
        frame.setVisible(true);
    }

    private JPanel createHomePanel(){
        //Creating the panel
        JPanel panel = new JPanel(new BorderLayout());
        //Creating the label for the message
        JLabel label = new JLabel ("Welcome to Santorini", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 30));
        //Creating the button for the booking
        JButton bookingButton = new JButton("Leave contact for booking");
        //To display the message and the booking button
        panel.add(label, BorderLayout.CENTER);
        panel.add(bookingButton, BorderLayout.SOUTH);
        return panel;
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new RestaurantBooking();
            }
        }
        );
    }
}

