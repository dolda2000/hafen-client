package haven;

import static haven.MCache.cmaps;
import static haven.MCache.cutsz;

public abstract class MapOverlay implements Rendered {
    protected final MCache map;
    private boolean visible = true;
    private Coord lastCut;
    private long lastGridId;

    public MapOverlay(MCache map) {
        this.map = map;
    }

    public final void update(Coord c) {
        Coord tile = c.div(MCache.tilesz);
        Coord cut = tile.div(cutsz);
        try {
            if (!cut.equals(lastCut)) {
                refresh(cut);
                lastCut = cut;
            } else {
                // try to detect area transition
                Coord gc = tile.div(cmaps);
                if (gc.equals(0, 0)) {
                    MCache.Grid g = map.getgrid(gc);
                    if (g.id != lastGridId) {
                        refresh(cut);
                        lastGridId = g.id;
                    }
                }
            }
        } catch (Loading e) {}
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean value) {
        visible = value;
    }

    protected abstract void refresh(Coord cut);
}
