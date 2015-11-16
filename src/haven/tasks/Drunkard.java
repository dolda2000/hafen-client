package haven.tasks;

import haven.*;

import java.util.regex.Pattern;

public class Drunkard extends FsmTask {
    private static final Pattern liquidPattern;

    static {
        String liquids =  haven.Utils.join("|", new String[] { "Water", "Piping Hot Tea", "Tea" });
        String pattern = String.format("[0-9.]+ l of (%s)", liquids);
        liquidPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
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
                waitMenu(1, "Drink", new Callback<Boolean>() {
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
            if (canDrinkFrom(item))
                return item;
        }
        return null;
    }

    private static boolean canDrinkFrom(WItem item) {
        ItemInfo.Contents contents = getContents(item);
        if (contents != null && contents.sub != null) {
            for (ItemInfo info : contents.sub) {
                if (info instanceof ItemInfo.Name) {
                    ItemInfo.Name name = (ItemInfo.Name) info;
                    if (name.str != null && liquidPattern.matcher(name.str.text).matches())
                        return true;
                }
            }
        }
        return false;
    }

    private static ItemInfo.Contents getContents(WItem item) {
        try {
            for (ItemInfo info : item.item.info())
                if (info instanceof ItemInfo.Contents)
                    return (ItemInfo.Contents)info;
        } catch (Loading ignored) {}
        return null;
    }
}
