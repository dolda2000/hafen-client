package haven;

import java.awt.event.KeyEvent;

public class TileGrabber extends GrabberWnd {
    static final String title = "Tile selection";
    static final String defaultText = "Selected tile: (none)";
    Coord c;
    MCache.Overlay ol;
    final MCache map;

    public TileGrabber(String title, GameUI gui) {
        super(title, defaultText, gui);
        this.map = gui.ui.sess.glob.map;
    }

    @Override
    public void destroy() {
        if (this.ol != null)
            this.ol.destroy();
        super.destroy();
    }

    @Override
    protected void done() {
        done(c);
    }

    protected void done(Coord tile) {
    }

    @Override
    public boolean mmousedown(Coord mc, int button) {
        if (isDone())
            return false;
        c = mc.div(MCache.tilesz);
        if (this.ol != null)
            this.ol.destroy();
        this.ol = map.new Overlay(c, c, 1 << 16);
        setLabel(String.format("Selected tile: (%d, %d)", c.x, c.y));
        return true;
    }

    @Override
    public boolean mmouseup(Coord mc, int button) {
        return false;
    }

    @Override
    public boolean mmousewheel(Coord mc, int amount) {
        return false;
    }

    @Override
    public void mmousemove(Coord mc) {
    }

    @Override
    public void uimsg(String msg, Object... args) {
        if (msg.equals("reset")) {
            this.ol.destroy();
            this.ol = null;
            this.c = null;
        }
    }
}
