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

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import haven.Resource.Tile;
import java.awt.Color;
import java.util.*;
import java.lang.reflect.*;

public class MapView extends PView {
    public int plgob = -1;
    public Coord cc;
    private final Glob glob;
    float dist = 500.0f;
    float elev = (float)Math.PI / 4.0f;
    float angl = 0.0f;
    int view = 1;
    
    private class Camera extends Transform {
	public void apply(GOut g) {
	    Coord3f cc = getcc();
	    PointedCam.apply(g.gl, cc, dist, elev, angl);
	}
    }
    
    static {
	Widget.addtype("mapview", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    Coord sz = (Coord)args[0];
		    Coord mc = (Coord)args[1];
		    int pgob = -1;
		    if(args.length > 2)
			pgob = (Integer)args[2];
 		    return(new MapView(c, sz, parent, mc, pgob));
		}
	    });
    }
    
    public MapView(Coord c, Coord sz, Widget parent, Coord cc, int plgob) {
	super(c, sz, parent);
	glob = ui.sess.glob;
	this.cc = cc;
	this.plgob = plgob;
 	camera = new Camera();
    }
    
    private void setupmap(RenderList rl) {
	Coord cc = this.cc.div(tilesz).div(MCache.cutsz);
	Coord o = new Coord();
	for(o.y = -view; o.y <= view; o.y++) {
	    for(o.x = -view; o.x <= view; o.x++) {
		Coord pc = cc.add(o).mul(MCache.cutsz).mul(tilesz);
		MapMesh cut = glob.map.getcut(cc.add(o));
		rl.add(cut, Transform.xlate(new Coord3f(pc.x, -pc.y, 0)));
	    }
	}
    }

    private void setupgobs(RenderList rl) {
	synchronized(glob.oc) {
	    for(Gob g : glob.oc) {
		rl.add(TestView.tmesh[0], Transform.xlate(g.getc()));
	    }
	}
    }

    public void setup(RenderList rl) {
	this.cc = new Coord(getcc());
 	setupmap(rl);
	setupgobs(rl);
    }
    
    public Gob player() {
	return(glob.oc.getgob(plgob));
    }
    
    public Coord3f getcc() {
	Gob pl = player();
	if(pl != null)
	    return(pl.getc());
	else
	    return(new Coord3f(cc.x, cc.y, glob.map.getcz(cc)));
    }

    public void draw(GOut g) {
	glob.map.sendreqs();
	try {
	    super.draw(g);
	} catch(MCache.LoadingMap e) {
	    String text = "Loading...";
	    g.chcolor(Color.BLACK);
	    g.frect(Coord.z, sz);
	    g.chcolor(Color.WHITE);
	    g.atext(text, sz.div(2), 0.5, 0.5);
	}
    }
    
    private Coord dragorig = null;
    private float elevorig, anglorig;
    
    public boolean mousedown(Coord c, int button) {
	if(button == 2) {
	    elevorig = elev;
	    anglorig = angl;
	    dragorig = c;
	}
	return(true);
    }
    
    public void mousemove(Coord c) {
	if(dragorig != null) {
	    elev = elevorig - ((float)(c.y - dragorig.y) / 100.0f);
	    if(elev < 0.0f) elev = 0.0f;
	    if(elev > (Math.PI / 2.0)) elev = (float)Math.PI / 2.0f;
	    angl = anglorig + ((float)(c.x - dragorig.x) / 100.0f);
	    angl = angl % ((float)Math.PI * 2.0f);
	}
    }
    
    public boolean mouseup(Coord c, int button) {
	if(button == 2) {
	    dragorig = null;
	}
	return(true);
    }

    public boolean mousewheel(Coord c, int amount) {
	float d = dist + (amount * 5);
	if(d < 5)
	    d = 5;
	dist = d;
	return(true);
    }
    
    public boolean globtype(char c, java.awt.event.KeyEvent ev) {
	if(c >= '1' && c <= '9') {
	    view = c - '1';
	    return(true);
	}
	return(false);
    }
}
