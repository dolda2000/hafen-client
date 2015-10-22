package haven.tasks;

import haven.*;

class Utils {

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
