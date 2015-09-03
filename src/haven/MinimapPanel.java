package haven;

import java.awt.image.BufferedImage;

public class MinimapPanel extends Window {

    static final BufferedImage grip = Resource.loadimg("gfx/hud/gripbr");
    static final Coord gzsz = new Coord(30, 40);
    static final Coord minsz = new Coord(150, 125);

    private final MapView map;
    private final Widget minimap;
    private IButton vclaimButton;
    private IButton pclaimButton;
    private Coord doff;
    private boolean folded;
    private UI.Grab resizegrab = null;

    public MinimapPanel(Coord c, Coord sz, MapView map, Widget minimap) {
        super(sz, "Minimap");
        this.map = map;
        this.minimap = minimap;
        this.c = c;
        add(minimap, 0, 0);
        mapbuttons();
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
                Config.setMinimapSize(minimap.sz);
        } else {
            super.mouseup(c, button);
        }
        return (true);
    }

    public void mousemove(Coord c) {
        if (resizegrab != null) {
            Coord d = c.sub(doff);
            minimap.sz = minimap.sz.add(d);
            minimap.sz.x = Math.max(minsz.x, minimap.sz.x);
            minimap.sz.y = Math.max(minsz.y, minimap.sz.y);
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
        minimap.visible = !folded;
        vclaimButton.visible = !folded;
        pclaimButton.visible = !folded;
        if (folded) {
            resize(new Coord(minimap.sz.x, 0));
        } else {
            resize(Config.getMinimapSize());
        }
    }

    private void mapbuttons() {
        vclaimButton = add(new IButton("gfx/hud/lbtn-vil", "", "-d", "-h") {
            {
                tooltip = Text.render("Display personal claims");
            }

            public void click() {
                if ((map != null) && !map.visol(0))
                    map.enol(0, 1);
                else
                    map.disol(0, 1);
            }
        }, -6, -5);

        pclaimButton = add(new IButton("gfx/hud/lbtn-claim", "", "-d", "-h") {
            {
                tooltip = Text.render("Display village claims");
            }

            public void click() {
                if ((map != null) && !map.visol(2))
                    map.enol(2, 3);
                else
                    map.disol(2, 3);
            }
        }, -31, 15);
    }
}