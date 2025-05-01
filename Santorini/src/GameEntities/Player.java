package GameEntities;

import GameEntities.Gods.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private final String color;
    private GodPower godPower;
    private final List<Worker> workers = new ArrayList<>();

    public Player(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
    public GodPower getGodPower() {
        return godPower;
    }
    public void setGodPower(GodPower godPower) {
        this.godPower = godPower;
    }

    public void addWorker(Worker w) {
        workers.add(w);
    }
    public List<Worker> getWorkers() {
        return Collections.unmodifiableList(workers);
    }
}
