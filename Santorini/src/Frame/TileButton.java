package Frame;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;


public class TileButton extends JButton {

    private final Tile tile;
    private final Controller controller;

    private final String LEVEL0 = "/assets/level0.png";
    private final String LEVEL0_P1 = "/assets/level0_p1.png";
    private final String LEVEL0_P2 = "/assets/level0_p2.png";
    private final String LEVEL0_DOME = "/assets/level0_dome.png";
    private final String LEVEL1 = "/assets/level1.png";
    private final String LEVEL1_P1 = "/assets/level1_p1.png";
    private final String LEVEL1_P2 = "/assets/level1_p2.png";
    private final String LEVEL1_DOME = "/assets/level1_dome.png";
    private final String LEVEL2 = "/assets/level2.png";
    private final String LEVEL2_P1 = "/assets/level2_p1.png";
    private final String LEVEL2_P2 = "/assets/level2_p2.png";
    private final String LEVEL2_DOME = "/assets/level2_dome.png";
    private final String LEVEL3 = "/assets/level3.png";
    private final String LEVEL3_P1 = "/assets/level3_p1.png";
    private final String LEVEL3_P2 = "/assets/level3_p2.png";
    private final String LEVEL3_DOME = "/assets/level3_dome.png";



    public TileButton(Tile tile, Controller controller) {
        super();
        this.tile = tile;
        this.controller = controller;
        setBorder(new LineBorder(Color.WHITE, 10));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setSize(new Dimension(125, 125));
        updateAppearance();
        addActionListener(e -> controller.onTileClicked(this.tile));
        //setIcon(new ImageIcon(LEVEL0_P1));
    }

    @Override
    public Icon getIcon() {
        if (super.getIcon() instanceof ImageIcon) {
            ImageIcon imgIcon = (ImageIcon) super.getIcon();
            Image img = imgIcon.getImage();
            Image newimg = img.getScaledInstance( 105, 105,  java.awt.Image.SCALE_SMOOTH ) ;
            imgIcon = new ImageIcon( newimg );
            return imgIcon;
        } else {
            return super.getIcon();
        }

    }

    public void updateAppearance() {
        String iconPath;
        switch (tile.getBuilding().getLevel()) {
            case 0:
                if (tile.getBuilding().hasDome()) {
                    iconPath = LEVEL0_DOME;
                } else {
                    iconPath = LEVEL0;
                    if (tile.isOccupied()) {
                        if (tile.getOccupant().getPlayer().getColor() == "Blue" || tile.getOccupant().getPlayer().getColor().equals("Blue")) {
                            iconPath = LEVEL0_P1;
                        } else {
                            iconPath = LEVEL0_P2;
                        }
                    }
                }
                break;
            case 1:
                if (tile.getBuilding().hasDome()) {
                    iconPath = LEVEL1_DOME;
                } else {
                    iconPath = LEVEL1;
                    if (tile.isOccupied()) {
                        if (tile.getOccupant().getPlayer().getColor() == "Blue" || tile.getOccupant().getPlayer().getColor().equals("Blue")) {
                            iconPath = LEVEL1_P1;
                        } else {
                            iconPath = LEVEL1_P2;
                        }
                    }
                }
                break;
            case 2:
                if (tile.getBuilding().hasDome()) {
                    iconPath = LEVEL2_DOME;
                } else {
                    iconPath = LEVEL2;
                    if (tile.isOccupied()) {
                        if (tile.getOccupant().getPlayer().getColor() == "Blue" || tile.getOccupant().getPlayer().getColor().equals("Blue")) {
                            iconPath = LEVEL2_P1;
                        } else {
                            iconPath = LEVEL2_P2;
                        }
                    }
                }
                break;
            case 3:
                if (tile.getBuilding().hasDome()) {
                    iconPath = LEVEL3_DOME;
                } else {
                    iconPath = LEVEL3;
                    if (tile.isOccupied()) {
                        if (tile.getOccupant().getPlayer().getColor() == "Blue" || tile.getOccupant().getPlayer().getColor().equals("Blue")) {
                            iconPath = LEVEL3_P1;
                        } else {
                            iconPath = LEVEL3_P2;
                        }
                    }
                }
                break;
            default:
                iconPath = LEVEL0;
        }
        URL url = TileButton.class.getResource(iconPath);
        this.setIcon(new ImageIcon(url));
        //TODO: Set background color based on occupant
    }
}