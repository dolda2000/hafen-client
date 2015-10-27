package haven;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class DraggableBelt extends Widget {
    private static final Tex vertGripTex = Resource.loadtex("gfx/hud/belt/custom/grip-v");
    private static final Tex horzGripTex = Resource.loadtex("gfx/hud/belt/custom/grip-h");
    private static final BufferedImage rotateImg = Resource.loadimg("gfx/hud/belt/custom/rotate");
    private static final BufferedImage rotateHoveredImg = Resource.loadimg("gfx/hud/belt/custom/rotate-h");
    private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 10);

    protected final String name;
    private final Grip grip;
    private final List<SlotWidget> slots;
    private final Coord slotSize;
    private Orientation orientation;
    private UI.Grab dm = null;
    private Coord doff;

    public DraggableBelt(String name, Coord slotSize) {
        this.name = name;
        this.c = BeltConfig.getBeltPosition(name, Coord.z);
        this.orientation = BeltConfig.getBeltOrienation(name, Orientation.Horizontal);;
        this.grip = add(new Grip(this.orientation));
        this.slots = new ArrayList<SlotWidget>();
        this.slotSize = slotSize;
        updateLayout();
        setHovered(false);
    }

    public void setOrientation(Orientation value) {
        BeltConfig.setBeltOrienation(name, value);
        orientation = value;
        grip.setOrientation(value);
        updateLayout();
    }

    @Override
    public void move(Coord c) {
        super.move(clipToParent(c));
        BeltConfig.setBeltPosition(name, this.c);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("drag")) {
            drag((Coord)args[0]);
        } else if (sender == grip && msg.equals("rotate")) {
            setOrientation(orientation.invert());
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        if(dm != null) {
            dm.remove();
            dm = null;
            BeltConfig.setBeltPosition(name, this.c);
        } else {
            super.mouseup(c, button);
        }
        return true;
    }

    @Override
    public void mousemove(Coord c) {
        if (dm != null) {
            setHovered(true);
            this.c = clipToParent(this.c.add(c.add(doff.inv())));
        } else {
            setHovered(c.isect(Coord.z, sz));
            super.mousemove(c);
        }
    }

    @Override
    public boolean globtype(char key, KeyEvent ev) {
        if (!visible || key != 0)
            return false;
        for (SlotWidget widget : slots) {
            Slot slot = widget.slot;
            if (slot.key == ev.getKeyCode() && slot.mods == ui.modflags()) {
                slot.keyact();
                return true;
            }
        }
        return super.globtype(key, ev);
    }

    protected void addSlots(Iterable<Slot> slots) {
        for (Slot slot : slots)
            this.slots.add(add(new SlotWidget(slot, slotSize), Coord.z));
        updateLayout();
    }

    private void drag(Coord c) {
        dm = ui.grabmouse(this);
        doff = c;
        parent.setfocus(this);
        raise();
    }

    private void setHovered(boolean value) {
        grip.show(value);
    }

    private void updateLayout() {
        switch (orientation) {
            case Horizontal:
                int x = grip.sz.x;
                for (SlotWidget slot : slots) {
                    slot.c = new Coord(x, 0);
                    x += slot.sz.x - 1;
                }
                break;
            case Vertical:
                int y = grip.sz.y;
                for (SlotWidget slot : slots) {
                    slot.c = new Coord(0, y);
                    y += slot.sz.y - 1;
                }
                break;
        }
        pack();
    }

    private Coord clipToParent(Coord c) {
        Coord clipped = new Coord(c);
        clipped.x = Math.min(parent.sz.x - sz.x, c.x);
        clipped.y = Math.min(parent.sz.y - sz.y, c.y);
        clipped.x = Math.max(0, clipped.x);
        clipped.y = Math.max(0, clipped.y);
        return clipped;
    }

    private class Grip extends Widget {
        private final IButton rotateButton;
        private Orientation orientation;
        private Tex tex;

        public Grip(Orientation orientation) {
            super(Coord.z);
            this.rotateButton = add(new IButton(rotateImg, rotateHoveredImg, rotateHoveredImg, false) {
                public void click() {
                    Grip.this.wdgmsg("rotate");
                }
            }, Coord.z);
            setOrientation(orientation);
        }

        public void setOrientation(Orientation value) {
            this.orientation = value;
            if (orientation == Orientation.Horizontal) {
                tex = horzGripTex;
                resize(tex.sz());
                rotateButton.c = new Coord(5, (sz.y - rotateImg.getHeight()) / 2);
            } else {
                tex = vertGripTex;
                resize(tex.sz());
                rotateButton.c = new Coord((sz.x - rotateImg.getWidth()) / 2, 5);
            }
        }

        @Override
        public void draw(GOut g) {
            g.image(tex, Coord.z);
            super.draw(g);
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if (super.mousedown(c, button))
                return true;
            if (button == 1) {
                wdgmsg("drag", c);
            }
            return true;
        }
    }

    public static class Slot {
        public final int key;
        public final int mods;
        public final Text text;

        public Slot(int key, int mods, String text) {
            this.key = key;
            this.mods = mods;
            this.text =  fnd.renderstroked(text, new Color(200, 200, 200), Color.BLACK);
        }

        public boolean click(Coord c, int button) { return false; }
        public void draw(GOut g) {}
        public boolean drop() { return false; }
        public boolean drop(Object thing) { return false; }
        public boolean interact(Coord cc, Coord ul) { return false; }
        public boolean isEmpty() { return true; }
        public void keyact() {}
        public Object tooltip(Coord c, Widget prev, boolean again) { return null; }
    }

    private static class SlotWidget extends Widget implements DTarget, DropTarget {
        public final Slot slot;

        public SlotWidget(Slot slot, Coord slotSize) {
            super(slotSize);
            this.slot = slot;
        }

        @Override
        public void draw(GOut g) {
            slot.draw(g);
            g.aimage(slot.text.tex(), sz.sub(2, 0), 1, 1);
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if (button == 1 && slot.isEmpty()) {
                wdgmsg("drag", this.c.add(c));
            } else {
                slot.click(c, button);
            }
            return true;
        }

        @Override
        public boolean drop(Coord cc, Coord ul) {
            return slot.drop();
        }

        @Override
        public boolean iteminteract(Coord cc, Coord ul) {
            return slot.interact(cc, ul);
        }

        @Override
        public boolean dropthing(Coord cc, Object thing) {
            return slot.drop(thing);
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            return slot.tooltip(c, prev, prev == this);
        }
    }
}
