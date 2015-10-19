package haven;

public class EquipProxy extends Widget implements DTarget{
    public static final Tex sq = Resource.loadtex("gfx/hud/eqproxysq");
    private int[] slots;

    public EquipProxy(int[] slots){
	super();
	setSlots(slots);
    }

    public void setSlots(int[] slots){
        this.slots = slots;
        Coord slotsz = new Coord(slots.length, 1);
        sz = sq.sz().add(-1, -1).mul(slotsz).add(1, 1);
    }

    private int slot(Coord c){
	int slot = sqroff(c).x;
	if(slot < 0){slot = 0;}
	if(slot >= slots.length){slot = slots.length -1;}
	return slots[slot];
    }

    @Override
    public boolean mousedown(Coord c, int button) {
	Equipory e = ui.gui.getEquipory();
	if(e != null){
	    WItem w = e.slots[slot(c)];
	    if(w != null){
		w.mousedown(c.add(-sqroff(c).x * sq.sz().x, 0), button);
		return true;
	    }
	}
	return false;
    }

    @Override
    public void draw(GOut g) {
	super.draw(g);
	Equipory e = ui.gui.getEquipory();
	if(e != null){
	    int k = 0;
	    Coord c0 = new Coord(0, 0);
	    for (int slot : slots){
		c0.x = k;
		Coord c1 = sqoff(c0);
		g.image(sq, c1);
		WItem w = e.slots[slot];
		if(w != null){
		    w.draw(g.reclipl(c1.add(sq.sz().sub(w.sz).div(2)), g.sz));
		}
		k++;
	    }
	}
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
	Equipory e = ui.gui.getEquipory();
	if(e != null){
	    WItem w = e.slots[slot(c)];
	    if(w != null){
		return w.tooltip(c, (prev == this)?w:prev);
	    }
	}
	return super.tooltip(c, prev);
    }

    @Override
    public boolean drop(Coord cc, Coord ul) {
	Equipory e = ui.gui.getEquipory();
	if(e != null){
	    e.wdgmsg("drop", slot(cc));
	    return true;
	}
	return false;
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
	Equipory e = ui.gui.getEquipory();
	if(e != null){
	    WItem w = e.slots[slot(cc)];
	    if(w != null){
		return w.iteminteract(cc, ul);
	    }
	}
	return false;
    }

    private static Coord sqroff(Coord c){
        return c.div(sq.sz().add(-1, -1));
    }

    private static Coord sqoff(Coord c){
        return c.mul(sq.sz().add(-1, -1));
    }
}
