package haven;

public abstract class MapOverlay implements Rendered {
    private boolean visible = true;

    abstract void update(Coord cc);

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean value) {
        visible = value;
    }
}
