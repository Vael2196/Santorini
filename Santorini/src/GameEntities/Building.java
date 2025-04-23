package GameEntities;

public class Building {

    private int level;
    private boolean dome;

    public Building() {
        this.level = 0;
        this.dome = false;
    }

    public int getLevel() {
        return level;
    }
    public boolean hasDome() {
        return dome;
    }

    /**
     * Builds a single level or a dome if on top of 3
     **/
    public void build() {
        if (dome) throw new IllegalStateException("Cannot build on a dome");
        if (level < 3) level++;
        else dome = true;
    }

}