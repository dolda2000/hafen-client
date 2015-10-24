package haven;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static haven.Inventory.invsq;

public abstract class DefaultBelt extends DraggableBelt {
    private int currentBelt;

    public DefaultBelt(String name) {
        super(name, Inventory.invsq.sz());
    }

    public void setCurrentBelt(int value) {
        currentBelt = value;
    }

    public static class NKeys extends DefaultBelt {
        private static final int keys[] = {
            KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
            KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0 };

        public NKeys(String name) {
            super(name);
            List<Slot> slots = new ArrayList<Slot>(keys.length);
            for (int i = 0; i < keys.length; i++)
                slots.add(new DefaultSlot(i, keys[i], 0, String.format("%d", (i + 1) % 10)));
            addSlots(slots);
        }
    }

    public static class FKeys extends DefaultBelt {
        private static final int keys[] = {
            KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4, KeyEvent.VK_F5, KeyEvent.VK_F6,
            KeyEvent.VK_F7, KeyEvent.VK_F8, KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12 };

        public FKeys(String name) {
            super(name);
            List<Slot> slots = new ArrayList<Slot>(keys.length);
            for (int i = 0; i < keys.length; i++)
                slots.add(new DefaultSlot(i, keys[i], 0, String.format("F%d", i + 1)));
            addSlots(slots);
        }
    }

    private class DefaultSlot extends Slot {
        private final int index;

        public DefaultSlot(int index, int key, int mods, String text) {
            super(key, mods, text);
            this.index = index;
        }

        @Override
        public boolean click(Coord c, int button) {
            switch (button) {
                case 1:
                    ui.gui.wdgmsg("belt", slot(), 1, ui.modflags());
                    break;
                case 3:
                    ui.gui.wdgmsg("setbelt", slot(), 1);
                    break;
            }
            return true;
        }

        @Override
        public void keyact() {
            MapView map = ui.gui.map;
            if (map == null)
                return;
            Coord mvc = map.rootxlate(ui.mc);
            if(mvc.isect(Coord.z, map.sz)) {
                map.delay(map.new Hittest(mvc) {
                    protected void hit(Coord pc, Coord mc, MapView.ClickInfo inf) {
                        if (inf == null)
                            ui.gui.wdgmsg("belt", slot(), 1, ui.modflags(), mc);
                        else
                            ui.gui.wdgmsg("belt", slot(), 1, ui.modflags(), mc, (int) inf.gob.id, inf.gob.rc);
                    }

                    protected void nohit(Coord pc) {
                        ui.gui.wdgmsg("belt", slot(), 1, ui.modflags());
                    }
                });
            }
        }

        @Override
        public void draw(GOut g) {
            g.image(invsq, Coord.z);
            Indir<Resource> slot = ui.gui.belt[slot()];
            try {
                if (slot != null)
                    g.image(slot.get().layer(Resource.imgc).tex(), new Coord(1, 1));
            } catch (Loading e) {}
        }

        @Override
        public boolean drop() {
            ui.gui.wdgmsg("setbelt", slot(), 0);
            return true;
        }

        @Override
        public boolean drop(Object thing) {
            if(thing instanceof Resource) {
                Resource res = (Resource)thing;
                if(res.layer(Resource.action) != null) {
                    ui.gui.wdgmsg("setbelt", slot(), res.name);
                    return(true);
                }
            }
            return (false);
        }

        @Override
        public boolean isEmpty() {
            return ui.gui.belt[slot()] == null;
        }

        private int slot() {
            return index + (currentBelt * 12);
        }
    }
}
