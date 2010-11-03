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
import javax.media.opengl.*;

public class MapView extends PView {
    public int plgob = -1;
    public Coord cc;
    private final Glob glob;
    private float dist = 500.0f;
    private float elev = (float)Math.PI / 4.0f;
    private float angl = 0.0f;
    private int view = 1;
    private Coord clickc = null;
    private int clickb;
    public static int lighting = 0;
    
    private class Camera extends Transform {
	public void apply(GOut g) {
	    Coord3f cc = getcc();
	    cc.y = -cc.y;
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
		Coord3f c = g.getc();
		c.y = -c.y;
		rl.add(g, Transform.seq(Transform.xlate(c), Transform.rot(Coord3f.zu, -(float)(360.0 * g.a / (Math.PI * 2)))));
	    }
	}
    }

    public void setup(RenderList rl) {
	this.cc = new Coord(getcc());
	if(lighting == 0) {
	    rl.add(new DirLight(new Color(128, 128, 128), Color.WHITE, Color.WHITE, new Coord3f(2.0f, 1.0f, 5.0f)));
	} else if(lighting == 1) {
	    rl.add(new DirLight(new Color(255, 192, 64), new Coord3f(2.0f, 1.0f, 1.0f)));
	}
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

    private static class Clicklist implements Renderer {
	private Map<Color, Rendered> rmap = new HashMap<Color, Rendered>();
	private int i = 1;
	
	public void render(GOut g, Rendered r) {
	    if(r instanceof FRendered) {
		GL gl = g.gl;
		int cr = i & 0xff,
		    cg = (i & 0xff00) >> 8,
		    cb = (i & 0xff0000) >> 16;
		Color col = new Color(cr, cg, cb);
		i++;
		rmap.put(col, r);
		gl.glColor3f(cr / 256.0f, cg / 256.0f, cb / 256.0f);
		((FRendered)r).drawflat(g);
	    }
	}
	
	public Rendered get(GOut g, Coord c) {
	    return(rmap.get(g.getpixel(c)));
	}
    }

    private Coord checkmapclick(GOut g, Coord c) {
	GL gl = g.gl;
	try {
	    g.texsel(-1);
	    RenderList rl = new RenderList();
	    setupmap(rl);
	    Rendered hit;
	    {
		Clicklist cl = new Clicklist();
		renderlist(g, rl, cl);
		hit = cl.get(g, c);
		if((hit == null) || !(hit instanceof MapMesh))
		    return(null);
	    }
	    final MapMesh mesh = (MapMesh)hit;
	    Coord tile;
	    {
		renderlist(g, rl, new Renderer() {
			public void render(GOut g, Rendered r) {
			    if(r == mesh) {
				mesh.clickmode = 1;
				mesh.drawflat(g);
			    }
			}
		    });
		Color hitcol = g.getpixel(c);
		tile = new Coord(hitcol.getRed() - 1, hitcol.getGreen() - 1);
		if(!tile.isect(Coord.z, mesh.sz))
		    return(null);
	    }
	    Coord pixel;
	    {
		renderlist(g, rl, new Renderer() {
			public void render(GOut g, Rendered r) {
			    if(r == mesh) {
				mesh.clickmode = 2;
				mesh.drawflat(g);
			    }
			}
		    });
		Color hitcol = g.getpixel(c);
		if(hitcol.getBlue() != 0)
		    return(null);
		pixel = new Coord((hitcol.getRed() * tilesz.x) / 255, (hitcol.getGreen() * tilesz.y) / 255);
	    }
	    return(mesh.ul.add(tile).mul(tilesz).add(pixel));
	} finally {
	    gl.glClear(gl.GL_DEPTH_BUFFER_BIT | gl.GL_COLOR_BUFFER_BIT);
	}
    }

    protected void render(GOut g) {
	Coord mapcl = null;
	if(clickc != null)
	    mapcl = checkmapclick(g, clickc);
	super.render(g);
	if(mapcl != null) {
	    wdgmsg("click", clickc, mapcl, clickb, ui.modflags());
	}
	clickc = null;
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
	} else {
	    clickb = button;
	    clickc = c;
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
	/*
	if(c >= '1' && c <= '9') {
	    view = c - '1';
	    return(true);
	}
	*/
	if((c >= '0') && (c <= '2')) {
	    lighting = c - '0';
	    return(true);
	}
	return(false);
    }
}
