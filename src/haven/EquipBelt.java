package haven;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class EquipBelt extends DraggableBelt {
    private static final Tex sq = Resource.loadtex("gfx/hud/belt/custom/eqsq");
    private static final int keys[] = {
        KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
        KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0 };

    public EquipBelt(String name, int... slotIndexes) {
        super(name, sq.sz());
        List<Slot> slots = new ArrayList<Slot>(slotIndexes.length);
        for (int i = 0; i < slotIndexes.length; i++)
            slots.add(new EquipSlot(slotIndexes[i], keys[i], 1, "Shift " + (i + 1) % 10));
        slots.add(new HandSlot(KeyEvent.VK_E, 0, "E"));
        addSlots(slots);
    }

    private static Coord sqroff(Coord c){
        return c.div(sq.sz().add(-1, -1));
    }

    private class EquipSlot extends Slot {
        private final int index;

        public EquipSlot(int index, int key, int mods, String text) {
            super(key, mods, text);
            this.index = index;
        }

        @Override
        public boolean click(Coord c, int button) {
            Equipory e = ui.gui.getEquipory();
            if(e != null){
                WItem w = e.slots[index];
                if(w != null){
                    w.mousedown(c.add(-sqroff(c).x * sq.sz().x, 0), button);
                }
            }
            return true;
        }

        @Override
        public void draw(GOut g) {
            Equipory e = ui.gui.getEquipory();
            if(e != null){
                g.rimage(Window.bg, Coord.z, sz);
                g.image(sq, Coord.z);
                WItem w = e.slots[index];
                if (w != null){
                    w.draw(g.reclipl(sq.sz().sub(w.sz).div(2), g.sz));
                }
            }
        }

        @Override
        public boolean drop() {
            Equipory e = ui.gui.getEquipory();
            if (e != null) {
                e.wdgmsg("drop", index);
                return true;
            }
            return false;
        }

        @Override
        public boolean interact(Coord cc, Coord ul) {
            Equipory e = ui.gui.getEquipory();
            if(e != null){
                WItem w = e.slots[index];
                if(w != null) {
                    return w.iteminteract(cc, ul);
                }
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            Equipory e = ui.gui.getEquipory();
            return (e == null) || (e.slots[index] == null);
        }

        @Override
        public Object tooltip(Coord c, Widget prev, boolean again) {
            Equipory e = ui.gui.getEquipory();
            if (e != null){
                WItem w = e.slots[index];
                if(w != null) {
                    return w.tooltip(c, again ? w : prev);
                }
            }
            return null;
        }

        @Override
        public void keyact() {
            if (isEmpty()) {
                // try to put item in hand to the slot
                Equipory e = ui.gui.getEquipory();
                if(e != null) {
                    e.wdgmsg("drop", index);
                }
            } else {
                // try to take item
                Equipory e = ui.gui.getEquipory();
                if(e != null){
                    WItem w = e.slots[index];
                    if(w != null){
                        w.item.wdgmsg("take", w.sz.div(2));
                    }
                }
            }
        }
    }

    private class HandSlot extends Slot {

        public HandSlot(int key, int mods, String text) {
            super(key, mods, text);
        }

        @Override
        public boolean click(Coord c, int button) {
            if (button == 1) {
                ui.gui.swapHand();
                return true;
            }
            return false;
        }

        @Override
        public void draw(GOut g) {
            Equipory e = ui.gui.getEquipory();
            if(e != null){
                g.rimage(Window.bg, Coord.z, sz);
                g.image(sq, Coord.z);
                if (ui.gui.handSave.size() > 0) {
                    for (GameUI.DraggedItem di : ui.gui.handSave) {
                        GSprite spr = di.item.spr();
                        if (spr != null) {
                            int w = Math.min(spr.sz().x, sq.sz().x - 5);
                            int h = Math.min(spr.sz().y, sq.sz().y - 5);
                            spr.draw(g.reclipl(sq.sz().sub(w, h).div(2), g.sz.sub(5, 5)));
                        }
                        break;
                    }
                }
            }
        }

        @Override
        public boolean drop() {
            if (ui.gui.handSave.size() == 0) {
                ui.gui.swapHand();
            }
            return true;
        }

        @Override
        public boolean isEmpty() {
            return ui.gui.handSave.size() == 0;
        }

        @Override
        public void keyact() {
            ui.gui.swapHand();
        }
    }
}
