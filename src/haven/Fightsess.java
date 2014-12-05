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

import java.util.*;
import java.awt.event.KeyEvent;

public class Fightsess extends Widget {
    public static final int actpitch = 50;
    public final Indir<Resource>[] actions;
    public int use = -1;
    public Coord pcc;
    public int pho;
    private final Fightview fv;

    @RName("fsess")
    public static class $_ implements Factory {
	public Widget create(Coord c, Widget parent, Object[] args) {
	    int nact = (Integer)args[0];
	    return(new Fightsess(parent, nact));
	}
    }

    @SuppressWarnings("unchecked")
    public Fightsess(Widget parent, int nact) {
	super(Coord.z, parent.sz, parent);
	this.fv = getparent(GameUI.class).fv;
	pcc = sz.div(2);
	pho = -40;
	this.actions = (Indir<Resource>[])new Indir[nact];
    }

    private void updatepos() {
	MapView map;
	Gob pl;
	if(((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null) || (pl.sc == null))
	    return;
	pcc = pl.sc;
	pho = (int)(pl.sczu.mul(20f).y) - 20;
    }

    public void draw(GOut g) {
	updatepos();
	double now = System.currentTimeMillis() / 1000.0;

	for(Buff buff : fv.buffs.children(Buff.class))
	    buff.draw(g.reclip(pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
	if(fv.current != null) {
	    for(Buff buff : fv.current.buffs.children(Buff.class))
		buff.draw(g.reclip(pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
	}

	if(now < fv.atkct) {
	    int w = (int)((fv.atkct - now) * 20);
	    g.chcolor(255, 0, 128, 255);
	    g.frect(pcc.add(-w, 20), new Coord(w * 2, 15));
	    g.chcolor();
	}
	Coord ca = pcc.add(-(actions.length * actpitch) / 2, 45);
	for(int i = 0; i < actions.length; i++) {
	    Indir<Resource> act = actions[i];
	    try {
		if(act != null) {
		    Tex img = act.get().layer(Resource.imgc).tex();
		    g.image(img, ca);
		    if(i == use) {
			g.chcolor(255, 0, 128, 255);
			Coord cc = ca.add(img.sz().x / 2, img.sz().y + 5);
			g.frect(cc.sub(2, 2), new Coord(5, 5));
			g.chcolor();
		    }
		}
	    } catch(Loading l) {}
	    ca.x += actpitch;
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "act") {
	    int n = (Integer)args[0];
	    Indir<Resource> res = (args.length > 1)?ui.sess.getres((Integer)args[1]):null;
	    actions[n] = res;
	} else if(msg == "use") {
	    this.use = (Integer)args[0];
	} else if(msg == "used") {
	} else {
	    super.uimsg(msg, args);
	}
    }

    public boolean globtype(char key, KeyEvent ev) {
	int c = ev.getKeyChar();
	if((key == 0) && (c >= KeyEvent.VK_1) && (key < KeyEvent.VK_1 + actions.length)) {
	    wdgmsg("use", c - KeyEvent.VK_1);
	    return(true);
	}
	return(super.globtype(key, ev));
    }
}
