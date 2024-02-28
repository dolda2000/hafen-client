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

import java.awt.Color;

public class ISBox extends Widget implements DTarget {
    public static final Color bgcol = new Color(43, 51, 44, 127);
    public static final IBox box = new IBox("gfx/hud/bosq", "tl", "tr", "bl", "br", "el", "er", "et", "eb") {
	    public void draw(GOut g, Coord tl, Coord sz) {
		super.draw(g, tl, sz);
		g.chcolor(bgcol);
		g.frect(tl.add(ctloff()), sz.sub(cisz()));
		g.chcolor();
	    }
	};
    public static final Coord defsz = UI.scale(145, 42);
    public static final Text.Foundry lf = new Text.Foundry(Text.fraktur, 22, Color.WHITE).aa(true);
    private final Indir<Resource> res;
    private Text label;

    @RName("isbox")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Indir<Resource> res;
	    if(args[0] instanceof String)
		res = Resource.remote().load((String)args[0]);
	    else
		res = ui.sess.getresv(args[0]);
	    return(new ISBox(res, Utils.iv(args[1]), Utils.iv(args[2]), Utils.iv(args[3])));
	}
    }

    private void setlabel(int rem, int av, int bi) {
	if(bi < 0)
	    label = lf.renderf("%d/%d", rem, av);
	else
	    label = lf.renderf("%d/%d/%d", rem, av, bi);
    }

    public ISBox(Indir<Resource> res, int rem, int av, int bi) {
        super(defsz);
        this.res = res;
        setlabel(rem, av, bi);
    }

    public void draw(GOut g) {
	box.draw(g, Coord.z, sz);
	try {
            Tex t = res.get().flayer(Resource.imgc).tex();
            Coord dc = Coord.of(UI.scale(6), (sz.y - t.sz().y) / 2);
            g.image(t, dc);
        } catch(Loading e) {}
        g.image(label.tex(), new Coord(UI.scale(40), (sz.y - label.sz().y) / 2));
    }

    public Object tooltip(Coord c, Widget prev) {
	try {
	    if(res.get().layer(Resource.tooltip) != null)
		return(res.get().layer(Resource.tooltip).t);
	} catch(Loading e) {}
	return(null);
    }

    public boolean mousedown(Coord c, int button) {
        if(button == 1) {
            if(ui.modshift)
                wdgmsg("xfer");
            else
                wdgmsg("click");
            return(true);
        }
        return(false);
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
        if(msg == "chnum") {
            setlabel(Utils.iv(args[0]), Utils.iv(args[1]), Utils.iv(args[2]));
        } else {
            super.uimsg(msg, args);
        }
    }
}
