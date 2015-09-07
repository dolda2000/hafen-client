package haven;

import java.awt.image.BufferedImage;

public class MinimapWnd extends Window {

    static final BufferedImage grip = Resource.loadimg("gfx/hud/gripbr");
    static final Coord gzsz = new Coord(20, 20);
    static final Coord minsz = new Coord(150, 125);

    private final MapView map;
    private final LocalMiniMap minimap;
    private final GameUI gui;
    private IButton vclaimButton;
    private IButton pclaimButton;
    private IButton centerButton;
    private IButton radiusButton;
    private Coord doff;
    private boolean folded;
    private UI.Grab resizegrab = null;
    private final Window trees, bushes, bumlings;

    public MinimapWnd(Coord c, Coord sz, GameUI gui, MapView map, LocalMiniMap minimap) {
        super(sz, "Minimap");
        this.map = map;
        this.minimap = minimap;
        this.gui = gui;
        this.c = c;

        add(minimap, 0, 0);
        trees = createIconWindow("Toggle Trees", "trees");
        bushes = createIconWindow("Toggle Bushes", "bushes");
        bumlings = createIconWindow("Toggle Rocks", "bumlings");

        initbuttons();
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

    @Override
    public void destroy() {
        bushes.destroy();
        trees.destroy();
        bumlings.destroy();
        super.destroy();
    }

    public boolean type(char key, java.awt.event.KeyEvent ev) {
        if(key == 27) {
            wdgmsg(cbtn, "click");
            return(true);
        }
        return(super.type(key, ev));
    }

    public void togglefold() {
        folded = !folded;
        minimap.visible = !folded;
        vclaimButton.visible = !folded;
        pclaimButton.visible = !folded;
        centerButton.visible = !folded;
        radiusButton.visible = !folded;
        if (folded) {
            resize(new Coord(minimap.sz.x, 0));
        } else {
            resize(Config.getMinimapSize());
        }
    }

    private void initbuttons() {
        vclaimButton = add(new IButton("gfx/hud/lbtn-vil", "", "-d", "-h") {
            { tooltip = Text.render("Display personal claims");  }

            public void click() {
                if ((map != null) && !map.visol(0))
                    map.enol(0, 1);
                else
                    map.disol(0, 1);
            }
        }, -6, -5);

        pclaimButton = add(new IButton("gfx/hud/lbtn-claim", "", "-d", "-h") {
            { tooltip = Text.render("Display village claims"); }

            public void click() {
                if ((map != null) && !map.visol(2))
                    map.enol(2, 3);
                else
                    map.disol(2, 3);
            }
        }, -6, -10);

        centerButton = add(new IButton("gfx/hud/buttons/center", "-u", "-d", "-d") {
            { tooltip = Text.render("Center map"); }

            public void click() {
                minimap.setOffset(Coord.z);
            }
        }, 53, 3);

        radiusButton = add(new IButton("gfx/hud/buttons/dispradius", "", "", "") {
            { tooltip = Text.render("Show view radius"); }

            public void click() {
                minimap.toggleRadius();
            }
        }, 78, 3);

        int x = 65;
        int y = -18;
        add(createIconButton(trees, "gfx/hud/treebutton", "Toggle Trees on minimap"), x, y);
        x += 20;
        add(createIconButton(bushes, "gfx/hud/bushbutton", "Toggle Bushes on minimap"), x, y);
        x += 20;
        add(createIconButton(bumlings, "gfx/hud/rockbutton", "Toggle Rocks on minimap"), x, y);
    }

    private IButton createIconButton(final Window window, final String res, final String tt) {
        return new IButton(res, "", "", "") {
            { tooltip = Text.render(tt); }

            public void click() {
                if (window.visible)
                    window.hide();
                else
                    window.show();
            }
        };
    }

    private Window createIconWindow(String cap, String type) {
        Window wnd = new MinimapIconsWindow(Coord.z, cap, type);
        gui.add(wnd, 100, 100);
        wnd.pack();
        wnd.hide();
        return wnd;
    }
}