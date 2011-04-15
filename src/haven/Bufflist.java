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

public class Bufflist extends Widget {
    static Tex frame = Resource.loadtex("gfx/hud/buffs/frame");
    static Tex cframe = Resource.loadtex("gfx/hud/buffs/cframe");
    static final Coord imgoff = new Coord(3, 3);
    static final Coord ameteroff = new Coord(3, 36);
    static final Coord ametersz = new Coord(30, 2);
    static final int margin = 2;
    static final int num = 5;
    
    static {
        Widget.addtype("buffs", new WidgetFactory() {
            public Widget create(Coord c, Widget parent, Object[] args) {
                return(new Bufflist(c, parent));
            }
        });
    }
    
    public Bufflist(Coord c, Widget parent) {
	super(c, new Coord((num * frame.sz().x) + ((num - 1) * margin), cframe.sz().y), parent);
    }
    
    public void draw(GOut g) {
	int i = 0;
	int w = frame.sz().x + margin;
	long now = System.currentTimeMillis();
	synchronized(ui.sess.glob.buffs) {
	    for(Buff b : ui.sess.glob.buffs.values()) {
		if(!b.major)
		    continue;
		Coord bc = new Coord(i * w, 0);
		if(b.ameter >= 0) {
		    g.image(cframe, bc);
		    g.chcolor(Color.BLACK);
		    g.frect(bc.add(ameteroff), ametersz);
		    g.chcolor(Color.WHITE);
		    g.frect(bc.add(ameteroff), new Coord((b.ameter * ametersz.x) / 100, ametersz.y));
		    g.chcolor();
		} else {
		    g.image(frame, bc);
		}
		try {
		    Tex img = b.res.get().layer(Resource.imgc).tex();
		    g.image(img, bc.add(imgoff));
		    if(b.nmeter >= 0) {
			Tex ntext = b.nmeter();
			g.image(ntext, bc.add(imgoff).add(img.sz()).add(ntext.sz().inv()).add(-1, -1));
		    }
		    if(b.cmeter >= 0) {
			double m = b.cmeter / 100.0;
			if(b.cticks >= 0) {
			    double ot = b.cticks * 0.06;
			    double pt = ((double)(now - b.gettime)) / 1000.0;
			    m *= (ot - pt) / ot;
			}
			g.chcolor(0, 0, 0, 128);
			g.fellipse(bc.add(imgoff).add(img.sz().div(2)), img.sz().div(2), 90, (int)(90 + (360 * m)));
			g.chcolor();
		    }
		} catch(Loading e) {}
		if(++i >= 5)
		    break;
	    }
	}
    }
    
    public Object tooltip(Coord c, boolean again) {
	int i = 0;
	int w = frame.sz().x + margin;
	synchronized(ui.sess.glob.buffs) {
	    for(Buff b : ui.sess.glob.buffs.values()) {
		if(!b.major)
		    continue;
		Coord bc = new Coord(i * w, 0);
		if(c.isect(bc, frame.sz())) {
		    Resource.Tooltip tt;
		    if(b.tt != null)
			return(b.tt);
		    else if((tt = b.res.get().layer(Resource.tooltip)) != null)
			return(tt.t);
		}
		if(++i >= 5)
		    break;
	    }
	}
	return(null);
    }
}
