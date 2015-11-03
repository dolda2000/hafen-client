package haven;

public class InventoryProxy extends Inventory {
    private final Inventory inv;
    private boolean locked;

    public InventoryProxy(Inventory inv) {
        super(inv.isz);
        this.inv = inv;
    }

    @Override
    public void draw(GOut g) {
        isz = inv.isz;
        super.draw(g);
        for (WItem item : inv.children(WItem.class)) {
            GOut g2 = g.reclip(xlate(item.c, true), item.sz);
            item.draw(g2);
        }
    }

    @Override
    public boolean drop(Coord cc, Coord ul) {
        inv.wdgmsg("drop", ul.add(sqsz.div(2)).div(invsq.sz()));
        return(true);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if (locked)
            return false;
        WItem item = itemat(c);
        return (item != null) && item.mousedown(c.sub(item.c), button);
    }

    @Override
    public void mousemove(Coord c) {
        WItem item = itemat(c);
        if (item != null)
            item.mousemove(c.sub(item.c));
        else
            super.mousemove(c);
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
        if (!locked)
            inv.mousewheel(c, amount);
        return true;
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        WItem item = itemat(c);
        return (item != null) ? item.tooltip(c.sub(item.c), (prev == this) ? item : prev) : null;
    }

    private WItem itemat(Coord c) {
        for (WItem item : inv.children(WItem.class)) {
            if (c.isect(xlate(item.c, true), item.sz))
                return item;
        }
        return null;
    }

    public void setLocked(boolean value) {
        locked = value;
    }
}
