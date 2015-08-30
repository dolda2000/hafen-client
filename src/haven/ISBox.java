/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ISBox extends Widget implements DTarget {
    static Tex bg = Resource.loadtex("gfx/hud/bosq");
    static Text.Foundry lf;
    private Indir<Resource> res;
    private Text label;

    private Value value;
    private Button take;

    static {
        lf = new Text.Foundry(Text.fraktur, 22, java.awt.Color.WHITE);
        lf.aa = true;
    }

    private int rem;
    private int av;

    @RName("isbox")
    public static class $_ implements Factory {
	public Widget create(Widget parent, Object[] args) {
	    Indir<Resource> res;
	    if(args[0] instanceof String)
		res = Resource.remote().load((String)args[0]);
	    else
		res = parent.ui.sess.getres((Integer)args[0]);
	    return(new ISBox(res, (Integer)args[1], (Integer)args[2], (Integer)args[3]));
	}
    }
    
    private void setlabel(int rem, int av, int bi) {
	if(bi < 0)
	    label = lf.renderf("%d/%d", rem, av);
	else
	    label = lf.renderf("%d/%d/%d", rem, av, bi);
    }
    
    @Override
    protected void added() {
	if(parent instanceof Window) {
	    boolean isStockpile = "Stockpile".equals(((Window) parent).caption());
	    if(isStockpile) {
		value = new Value(40, "");
		add(value).c = new Coord(70, 46);
		value.canactivate = true;

		take = new Button(35, "Take");
		add(take).c = new Coord(110, 44);
		take.canactivate = true;

		sz = sz.add(0, 25);
	    }
	}
    }

    public ISBox(Indir<Resource> res, int rem, int av, int bi) {
        super(bg.sz());
	this.rem = rem;
	this.av = av;
        this.res = res;
        setlabel(rem, av, bi);
    }
    
    public void draw(GOut g) {
        g.image(bg, Coord.z);
	try {
            Tex t = res.get().layer(Resource.imgc).tex();
            Coord dc = new Coord(6, (bg.sz().y / 2) - (t.sz().y / 2));
            g.image(t, dc);
        } catch(Loading ignored) {}
        g.image(label.tex(), new Coord(40, (bg.sz().y / 2) - (label.tex().sz().y / 2)));
	super.draw(g);
    }
    
    public Object tooltip(Coord c, Widget prev) {
	try {
	    if(res.get().layer(Resource.tooltip) != null)
		return(res.get().layer(Resource.tooltip).t);
	} catch(Loading ignored) {}
	return(null);
    }
    
    public boolean mousedown(Coord c, int button) {
	Coord cc = xlate(take.c, true);
	if(c.isect(cc, take.sz)) {
	    return take.mousedown(c.sub(cc), button);
	}
	if (button == 1) {
	    if (ui.modshift ^ ui.modctrl) {           //SHIFT or CTRL means pull
		int dir = ui.modctrl ? -1 : 1;        //CTRL means pull out, SHIFT pull in
		int all = (dir > 0) ? av - rem : rem; //count depends on direction
		int k = ui.modmeta ? all : 1;         //ALT means pull all
		transfer(dir, k);
	    } else {
		wdgmsg("click");
	    }
	    return (true);
	}
	return (false);
    }

    public void transfer(int dir, int amount) {
	for (int i = 0; i < amount; i++) {
	    wdgmsg("xfer2", dir, 1); //modflags set to 1 to emulate only SHIFT pressed
	}
    }
    
    public boolean mousewheel(Coord c, int amount) {
	if(amount < 0)
	    wdgmsg("xfer2", -1, ui.modflags());
	if(amount > 0)
	    wdgmsg("xfer2", 1, ui.modflags());
	return(true);
    }
    
    public boolean drop(Coord cc, Coord ul) {
        wdgmsg("drop");
        return(true);
    }
    
    public boolean iteminteract(Coord cc, Coord ul) {
        wdgmsg("iact");
        return(true);
    }
    
    public void uimsg(String msg, Object... args) {
        if(msg.equals("chnum")) {
            setlabel((Integer)args[0], (Integer)args[1], (Integer)args[2]);
        } else {
            super.uimsg(msg, args);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if (sender == value || sender == take) {
	    int amount = 0;
	    try {
		amount = Integer.parseInt(value.text);
	    } catch (Exception ignored) {
	    }
	    if (amount > rem) {
		amount = rem;
	    }
	    if (amount > 0) {
		transfer(-1, amount);
	    }
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    private static class Value extends TextEntry {
	private static final Set<Integer> ALLOWED_KEYS = new HashSet<Integer>(Arrays.asList(
		KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
		KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9,
		KeyEvent.VK_NUMPAD0, KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, KeyEvent.VK_NUMPAD4,
		KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD7, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9,
		KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
		KeyEvent.VK_ENTER, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE
	));

	public Value(int w, String deftext) {
	    super(w, deftext);
	}

	@Override
	public boolean type(char c, KeyEvent ev) {
	    int keyCode = ev.getKeyCode();
	    if(keyCode == 0){
		keyCode = ev.getKeyChar();
	    }
	    if (ALLOWED_KEYS.contains(keyCode)) {
		return super.type(c, ev);
	    } else {
		ui.root.globtype(c, ev);
		return false;
	    }
	}

    }
}
