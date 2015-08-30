package haven;

import java.awt.image.BufferedImage;

public class MinimapPanel extends Window {

    static final BufferedImage grip = Resource.loadimg("gfx/hud/gripbr");
    static final Coord gzsz = new Coord(30, 40);
    static final Coord minsz = new Coord(150, 125);

    Coord doff;
    boolean folded;
    Widget mm;
    UI.Grab resizegrab = null;

    public MinimapPanel(Coord c, Coord sz, MapView map, Widget mm) {
        super(sz, "Minimap");
        this.mm = mm;
        this.c = c;
    }

    public void draw(GOut g) {
        super.draw(g);
        if (!folded) {
            g.image(grip, sz.sub(gzsz));
        }
    }

    public boolean mousedown(Coord c, int button) {
        if(folded)
            return super.mousedown(c, button);
        parent.setfocus(this);
        raise();
        if (button == 1) {
            doff = c;
            if(c.isect(sz.sub(gzsz), gzsz)) {
                resizegrab = ui.grabmouse(this);
                return true;
            }
        }
        return super.mousedown(c, button);
    }

    public boolean mouseup(Coord c, int button) {
        if (isGrabbed()) {
            Config.setMinimapPosition(this.c);
        }
        if (resizegrab != null) {
            resizegrab.remove();
            resizegrab = null;
                Config.setMinimapSize(mm.sz);
        } else {
            super.mouseup(c, button);
        }
        return (true);
    }

    public void mousemove(Coord c) {
        if (resizegrab != null) {
            Coord d = c.sub(doff);
            mm.sz = mm.sz.add(d);
            mm.sz.x = Math.max(minsz.x, mm.sz.x);
            mm.sz.y = Math.max(minsz.y, mm.sz.y);
            doff = c;
            pack();
        } else {
            super.mousemove(c);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(sender == cbtn) {
            togglefold();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public boolean type(char key, java.awt.event.KeyEvent ev) {
        if(key == 27) {
            wdgmsg(cbtn, "click");
            return(true);
        }
        return(super.type(key, ev));
    }

    private void togglefold() {
        folded = !folded;
        mm.visible = !folded;
        if (folded) {
            resize(new Coord(mm.sz.x, 0));
        } else {
            resize(Config.getMinimapSize());
        }
    }
}