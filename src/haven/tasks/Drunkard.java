package haven.tasks;

import haven.*;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.TreeSet;

public class Drunkard extends FsmTask {
    private static final Set<String> containers = new TreeSet<String>();
    {
        containers.add("gfx/invobjs/waterflask");
        containers.add("gfx/invobjs/waterskin");
        containers.add("gfx/invobjs/teapot-full");
    }

    @Override
    protected State getInitialState() {
        return new FindDrink();
    }

    private class FindDrink extends State {
        @Override
        public void tick(double dt) {
            WItem item = findDrink(context().playerInventory());
            if (item != null) {
                item.item.wdgmsg("iact", Coord.z, 3);
                waitMenu("Drink", 1, new Callback<Boolean>() {
                    public void done(Boolean success) {
                        if (success)
                            stop();
                        else
                            stop("Couldn't drink for some reason");
                    }
                });
            } else
                stop("Nothing to drink");
        }
    }

    private static WItem findDrink(Inventory inv) {
        for (WItem item : inv.children(WItem.class)) {
            if (isDrink(item) && !isEmpty(item))
                return item;
        }
        return null;
    }

    private static boolean isDrink(WItem item) {
        if (Utils.isItemName(item.item, containers)) {
            return true;
        } else if (Utils.isItemName(item.item, "lib/layspr")) {
            // sizzling teapot uses layered sprite defined in resources, so reflection is needed
            try {
                Class cl = item.item.spr().getClass();
                Field fl = cl.getDeclaredField("name");
                if (fl != null) {
                    String name = (String)fl.get(item.item.spr());
                    if ("Teapot".equals(name))
                        return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean isEmpty(WItem item) {
        for (ItemInfo info : item.item.info())
            if (info instanceof ItemInfo.Contents)
                return false;
        return true;
    }
}
