package haven;

public class Autoloader {

    public static void replace(Inventory inv, GItem item) {
        if (isArrow(item)) {
            // check if there are other arrows
            for (GItem other : inv.children(GItem.class))
                if (other != item && isArrow(other))
                    return;

            Inventory quiver = findQuiver(inv.ui);
            if (quiver != null) {
                quiver.wdgmsg("invxf", inv.wdgid(), 1);
            }
        }
    }

    private static boolean isArrow(GItem item) {
        try {
            String resName = item.resname();
            return (resName != null) && resName.startsWith("gfx/invobjs/arrow-");
        } catch (Loading e) {
            return false; }
    }

    private static Inventory findQuiver(UI ui) {
        for (Widget w = ui.gui.child; w != null; w = w.next) {
            if (w instanceof Window) {
                Window window = (Window)w;
                if ("Quiver".equals(window.cap.text))
                    return window.findchild(Inventory.class);
            }
        }
        return null;
    }
}
