package haven;

import static haven.MCache.cutsz;

public abstract class MapOverlay implements Rendered {
    protected final MCache map;
    private boolean visible = true;
    private Coord lastCut;

    public MapOverlay(MCache map) {
        this.map = map;
    }

    abstract void update(Coord cc);

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean value) {
        visible = value;
    }

    protected final boolean mapPositionChanged(Coord c) {
        Coord tile = c.div(MCache.tilesz);
        Coord cut = tile.div(cutsz);
        if (!cut.equals(lastCut)) {
            lastCut = cut;
            return true;
        }
        return false;
    }
}
