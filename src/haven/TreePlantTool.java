package haven;

public class TreePlantTool extends TileGrabber {
    private boolean clicked;
    private double t;

    public TreePlantTool(GameUI gui) {
        super("Select tile...", gui);
    }

    @Override
    protected void added() {
        if (ui.gui.hand.isEmpty() && ui.gui.handSave.isEmpty()) {
            ui.gui.error("There are no items in the hand!");
            destroy();
        }
    }

    @Override
    protected void done(Coord tile) {
        if (tile != null) {
            ui.gui.map.wdgmsg("itemact", Coord.z, tile.mul(MCache.tilesz).add(MCache.tilesz.div(2)), ui.modflags());
            clicked = true;
            hide();
        } else {
            ui.gui.error("No tile selected!");
        }
    }

    @Override
    public void tick(double dt) {
        if (clicked) {
            FlowerMenu menu = ui.root.findchild(FlowerMenu.class);
            if (menu != null) {
                FlowerMenu.Petal opt = getPlantTreeOption(menu);
                if (opt != null) {
                    menu.choose(menu.opts[0]);
                    clicked = false;
                    destroy();
                }
            } else {
                t += dt;
                // wait for a response
                if (t > 1) {
                    ui.gui.error("Couldn't plant tree!");
                    clicked = false;
                    destroy();
                }
            }
        }
    }

    @Override
    public void destroy() {
        // allow widget to be destroyed only when there is no waiting for a click response
        if (!clicked)
            super.destroy();
    }

    private static FlowerMenu.Petal getPlantTreeOption(FlowerMenu menu) {
        return menu.opts.length > 0 && "Plant tree".equals(menu.opts[0].name) ? menu.opts[0] : null;
    }
}
