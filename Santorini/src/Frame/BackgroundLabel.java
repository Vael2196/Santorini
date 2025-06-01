package Frame;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * A single cell on the board.  If (tile.x,tile.y) is in 'holes',
 * fill dark gray and hide its TileButton; otherwise it stays fully transparent.
 */
public class BackgroundLabel extends JLabel {
    private final Tile tile;
    private final Set<Point> holes;
    private TileButton childButton;

    /**
     * @param tile  the model Tile at (x,y)
     * @param holes the set of Points marking “hole” coordinates
     */
    public BackgroundLabel(Tile tile, Set<Point> holes) {
        super();
        this.tile  = tile;
        this.holes = holes;
        setOpaque(false);
    }

    @Override
    public Component add(Component comp) {
        super.add(comp);
        if (comp instanceof TileButton) {
            childButton = (TileButton) comp;
        }
        return comp;
    }


    public void updateAppearance() {
        Point p = new Point(tile.getX(), tile.getY());

        if (holes.contains(p)) {
            setOpaque(true);
            setBackground(Color.DARK_GRAY);
            if (childButton != null) {
                childButton.setVisible(false);
            }
        } else {
            setOpaque(false);
            setBackground(null);
            if (childButton != null) {
                childButton.setVisible(true);
            }
        }

        repaint();
    }
}
