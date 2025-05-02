package Frame;

import javax.swing.*;
import java.awt.*;

public class BackgroundLabel extends JLabel {

    private Tile tile;

    public BackgroundLabel(Tile tile) {
        super();
        setBackground(new Color(0, 0, 0, 0));
        this.tile = tile;
    }


    public void updateAppearance() {
        if (tile.isSelected()) {
            setBackground(new Color(0, 255, 255, 100));
        } else {
            setBackground(new Color(0, 0, 0, 0));
        }
    }



}
