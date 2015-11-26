package haven;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class CustomBelt extends DraggableBelt {
    private static final Tex bg = Window.bg;
    private static final Tex sq = Resource.loadtex("gfx/hud/belt/custom/sq");

    private final String accountName;
    private final String charName;
    private final List<Indir<Resource>> slots;

    public CustomBelt(String name, String accountName, String charName, int slotCount) {
        super(name, sq.sz());
        this.accountName = accountName;
        this.charName = charName;
        this.slots = new ArrayList<Indir<Resource>>();
        List<Indir<Resource>> savedSlots = BeltConfig.getSlots(name, accountName, charName);
        for (int i = 0; i < slotCount; i++) {
            this.slots.add(i < savedSlots.size() ? savedSlots.get(i) : null);
        }
    }

    private void setSlot(int index, Indir<Resource> res) {
        slots.set(index, res);
        BeltConfig.saveSlots(name, accountName, charName, slots);
    }

    public static class FKeys extends CustomBelt {
        private static final int keys[] = {
                KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4, KeyEvent.VK_F5, KeyEvent.VK_F6,
                KeyEvent.VK_F7, KeyEvent.VK_F8, KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12 };

        public FKeys(String name, String accountName, String charName) {
            super(name, accountName, charName, keys.length);
            List<Slot> slots = new ArrayList<Slot>(keys.length);
            for (int i = 0; i < keys.length; i++)
                slots.add(this.new CustomSlot(i, keys[i], 0, String.format("F%d", i + 1)));
            addSlots(slots);
        }
    }

    class CustomSlot extends Slot {
        private final int index;

        public CustomSlot(int index, int key, int mods, String text) {
            super(key, mods, text);
            this.index = index;
        }

        @Override
        public boolean click(Coord c, int button) {
            switch (button) {
                case 1:
                    Indir<Resource> slot = slots.get(index);
                    if (slot != null) {
                        ui.gui.menu.use(new Glob.Pagina(slot));
                    }
                    break;
                case 3:
                    setSlot(index, null);
                    break;
            }
            return true;
        }

        @Override
        public void keyact() {
            Indir<Resource> slot = slots.get(index);
            if (slot != null) {
                ui.gui.menu.use(new Glob.Pagina(slot));
            }
        }

        @Override
        public void draw(GOut g) {
            g.rimage(bg, Coord.z, sz);
            g.image(sq, Coord.z);
            Indir<Resource> slot = slots.get(index);
            try {
                if (slot != null)
                    g.image(slot.get().layer(Resource.imgc).tex(), new Coord(1, 1));
            } catch (Loading e) {
            }
        }

        @Override
        public boolean drop(Object thing) {
            if (thing instanceof Resource) {
                Resource res = (Resource)thing;
                if (res.layer(Resource.action) != null) {
                    setSlot(index, res.indir());
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return slots.get(index) == null;
        }

        private int slot() {
            return index;
        }
    }
}
