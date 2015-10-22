package haven.tasks;

import haven.*;

class TaskContext {
    private final UI ui;

    public TaskContext(UI ui) {
        this.ui = ui;
    }

    public GameUI gui() {
        return ui.gui;
    }

    public void info(String message) {
        ui.gui.msg(message);
    }

    public void error(String message) {
        ui.gui.error(message);
    }

    public GItem getItemAtHand() {
        for (GameUI.DraggedItem item : ui.gui.hand)
            return item.item;
        for (GameUI.DraggedItem item : ui.gui.handSave)
            return item.item;
        return null;
    }

    public Gob findObjectByName(int radius, String name) {
        Coord plc = player().rc;
        double min = radius;
        Gob nearest = null;
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                double dist = gob.rc.dist(plc);
                boolean matches = Utils.isObjectName(gob, name);
                if (matches && dist < min) {
                    min = dist;
                    nearest = gob;
                }
            }
        }
        return nearest;
    }

    public void mapInteractClick(Gob gob, int mod) {
        ui.gui.map.wdgmsg("itemact", Coord.z, gob.rc, mod, 0, (int)gob.id, gob.rc, 0, -1);
    }

    public Gob player() {
        return ui.gui.map.player();
    }

    public Inventory playerInventory() {
        return ui.gui.maininv;
    }
}
