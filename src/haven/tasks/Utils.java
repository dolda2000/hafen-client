package haven.tasks;

import haven.*;

import java.util.Collection;
import java.util.Collections;

class Utils {

    public static boolean isItemName(GItem item, Collection<String> names) {
        try {
            Resource res = item.resource();
            if (res != null)
                for (String name : names)
                    if (res.name.contains(name))
                        return true;
            return false;
        } catch (Loading e) {
            return false;
        }
    }

    public static boolean isItemName(GItem item, String name) {
        return isItemName(item, Collections.singleton(name));
    }

    public static boolean isObjectName(Gob gob, String name) {
        try {
            Resource res = gob.getres();
            return (res != null) && res.name.contains(name);
        } catch (Loading e) {
            return false;
        }
    }

    public static void takeItem(Widget item) {
        item.wdgmsg("take", Coord.z);
    }
}
