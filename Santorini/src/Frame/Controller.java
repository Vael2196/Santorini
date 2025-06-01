package Frame;

import java.awt.*;
import java.util.Set;

public interface Controller {
    /**
     * A gateway to push actions from the user to the controller
     * @param tile
     */
    void onTileClicked(Tile tile);

    /**
     * Set controller as a listener for the frame.
     * For advanced controllers, set both core controller and the respective controller
     * to listen to the same frame
     * @param frame
     */
    void setFrame(GameFrame frame);

    default Set<Point> getHoles() {
        return java.util.Collections.emptySet();
    }
}
