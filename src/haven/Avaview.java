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
import java.util.*;

public class Avaview extends PView {
    public static final Coord dasz = new Coord(74, 74);
    public static final Coord unborder = new Coord(2, 2);
    public static final Tex missing = Resource.loadtex("gfx/hud/equip/missing");
    public Color color = Color.WHITE;
    private long avagob;
    private Coord asz;
    private Composited comp;
    private List<Composited.MD> cmod = null;
    private List<Composited.ED> cequ = null;
	
    static {
	Widget.addtype("av", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new Avaview(c, parent, (Integer)args[0]));
		}
	    });
    }
	
    private Avaview(Coord c, Widget parent, Coord asz) {
	super(c, asz.add(Window.wbox.bisz()).add(unborder.mul(2).inv()), parent);
	this.asz = asz;
    }
        
    public Avaview(Coord c, Widget parent, long avagob, Coord asz) {
	this(c, parent, asz);
	this.avagob = avagob;
    }
	
    public Avaview(Coord c, Widget parent, long avagob) {
	this(c, parent, avagob, dasz);
    }
        
    public void uimsg(String msg, Object... args) {
	if(msg == "upd") {
	    this.avagob = (long)(Integer)args[0];
	    return;
	}
	super.uimsg(msg, args);
    }
        
    private boolean missed = false;
    private final Camera cam;
    {
	PointedCam cam = new PointedCam();
	cam.base = new Coord3f(0, 0, 10);
	cam.dist = 20;
	cam.e = 0.4f;
	cam.a = -0.2f;
	this.cam = cam;
    }

    protected Camera camera() {
	return(cam);
    }

    protected void setup(RenderList rl) {
	Gob gob = ui.sess.glob.oc.getgob(avagob);
	missed = false;
	if(gob == null) {
	    missed = true;
	    return;
	}
	Drawable d = gob.getattr(Drawable.class);
	if(!(d instanceof Composite)) {
	    missed = true;
	    return;
	}
	Composite gc = (Composite)d;
	if(gc.comp == null) {
	    missed = true;
	    return;
	}
	if((comp == null) || (comp.skel != gc.comp.skel))
	    comp = new Composited(gc.comp.skel);
	if(gc.comp.cmod != this.cmod)
	    comp.chmod(this.cmod = gc.comp.cmod);
	if(gc.comp.cequ != this.cequ)
	    comp.chequ(this.cequ = gc.comp.cequ);
	rl.add(comp, null);
	rl.add(new DirLight(Color.WHITE, Color.WHITE, Color.WHITE, new Coord3f(1, 1, 1).norm()), null);
    }

    public void draw(GOut g) {
	/*
	g.chcolor(Color.BLACK);
	g.frect(Coord.z, sz);
	g.chcolor();
	*/
	super.draw(g);
	if(missed) {
	    GOut g2 = g.reclip(Window.wbox.tloff().add(unborder.inv()), asz);
	    g2.image(missing, Coord.z);
	}
	g.chcolor(color);
	Window.wbox.draw(g, Coord.z, asz.add(Window.wbox.bisz()).add(unborder.mul(2).inv()));
    }
	
    public boolean mousedown(Coord c, int button) {
	wdgmsg("click", button);
	return(true);
    }
}
