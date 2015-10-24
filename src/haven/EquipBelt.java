package haven;

import java.util.ArrayList;
import java.util.List;

public class EquipBelt extends DraggableBelt {
    public static final Tex sq = Resource.loadtex("gfx/hud/eqproxysq");

    public EquipBelt(String name, int... slotIndexes) {
        super(name, sq.sz());
        List<Slot> slot = new ArrayList<Slot>(slotIndexes.length);
        for (int i = 0; i < slotIndexes.length; i++)
            slot.add(new EquipSlot(slotIndexes[i]));
        addSlots(slot);
    }

    private static Coord sqroff(Coord c){
        return c.div(sq.sz().add(-1, -1));
    }

    private class EquipSlot extends Slot {
        private final int index;

        public EquipSlot(int index) {
            super(-1, 0, "");
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
    }
}
