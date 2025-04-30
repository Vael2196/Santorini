package GameEntities;

import GameEntities.Gods.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private final String color;
    private GodCardInterface godCard;
    private final List<Worker> workers = new ArrayList<>();

    public Player(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
    public GodCardInterface getGodCard() {
        return godCard;
    }
    public void setGodCard(GodCardInterface godCard) {
        this.godCard = godCard;
    }

    public void addWorker(Worker w) {
        workers.add(w);
    }
    public List<Worker> getWorkers() {
        return Collections.unmodifiableList(workers);
    }
}
