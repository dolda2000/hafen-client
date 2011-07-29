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

public class MapView extends PView implements DTarget {
    public long plgob = -1;
    public Coord cc;
    private final Glob glob;
    private int view = 2;
    private Collection<Delayed> delayed = new LinkedList<Delayed>();
    public static int lighting = 0;
    private Camera camera = new FollowCam();
    private Plob placing = null;
    private int[] visol = new int[32];
    private Grabber grab;
    
    private interface Delayed {
	public void run(GOut g);
    }

    public interface Grabber {
	boolean mmousedown(Coord mc, int button);
	boolean mmouseup(Coord mc, int button);
	void mmousemove(Coord mc);
    }
    
    private abstract class Camera extends haven.Camera {
	public Camera() {
	    super(Matrix4f.identity());
	}
	
	public boolean click(Coord sc) {
	    return(false);
	}
	public void drag(Coord sc) {}
	public void release() {}
	public boolean wheel(Coord sc, int amount) {
	    return(false);
	}
	
	public void resized() {
	}
	
	public abstract Matrix4f compute();
	
	public Matrix4f fin(Matrix4f p) {
	    update(compute());
	    return(super.fin(p));
	}
    }
    
    private class FollowCam extends Camera {
	private final float fr = 0.0f, h = 10.0f;
	private float ca, cd, da;
	private Coord3f curc = null;
	private float elev = (float)Math.PI / 4.0f;
	private float angl = 0.0f;
	private Coord dragorig = null;
	private float anglorig;
	
	public void resized() {
	    ca = (float)sz.y / (float)sz.x;
	    cd = 400.0f * ca;
	    da = (float)Math.atan(ca * 0.5f);
	}
	{resized();}
	
	public boolean click(Coord c) {
	    anglorig = angl;
	    dragorig = c;
	    return(true);
	}
	
	public void drag(Coord c) {
	    angl = anglorig + ((float)(c.x - dragorig.x) / 100.0f);
	    angl = angl % ((float)Math.PI * 2.0f);
	}

	private float dist(float elev) {
	    return((float)(((cd - (h / Math.tan(elev))) * Math.sin(elev - da) / Math.sin(da)) - (h / Math.sin(elev))));
	}

	public Matrix4f compute() {
	    Coord3f cc = getcc();
	    cc.y = -cc.y;
	    if(curc == null)
		curc = cc;
	    float dx = cc.x - curc.x, dy = cc.y - curc.y;
	    if(Math.sqrt((dx * dx) + (dy * dy)) > fr) {
		Coord3f oc = curc;
		float pd = (float)Math.cos(elev) * dist(elev);
		Coord3f cambase = new Coord3f(curc.x + ((float)Math.cos(angl) * pd), curc.y + ((float)Math.sin(angl) * pd), 0.0f);
		float a = cc.xyangle(curc);
		float nx = cc.x + ((float)Math.cos(a) * fr), ny = cc.y + ((float)Math.sin(a) * fr);
		curc = new Coord3f(nx, ny, cc.z);
		angl = curc.xyangle(cambase);
	    }
	    return(PointedCam.compute(curc.add(0.0f, 0.0f, h), dist(elev), elev, angl));
	}
	
	private static final float maxang = (float)(Math.PI / 2 - 0.1);
	private static final float mindist = 10.0f;
	public boolean wheel(Coord c, int amount) {
	    float fe = elev;
	    elev += amount * elev * 0.02f;
	    if(elev > maxang)
		elev = maxang;
	    if(dist(elev) < mindist)
		elev = fe;
	    return(true);
	}
    }

    private class FreeCam extends Camera {
	private float dist = 50.0f;
	private float elev = (float)Math.PI / 4.0f;
	private float angl = 0.0f;
	private Coord dragorig = null;
	private float elevorig, anglorig;

	public Matrix4f compute() {
	    Coord3f cc = getcc();
	    cc.y = -cc.y;
	    return(PointedCam.compute(cc.add(0.0f, 0.0f, 15f), dist, elev, angl));
	}
	
	public boolean click(Coord c) {
	    elevorig = elev;
	    anglorig = angl;
	    dragorig = c;
	    return(true);
	}
	
	public void drag(Coord c) {
	    elev = elevorig - ((float)(c.y - dragorig.y) / 100.0f);
	    if(elev < 0.0f) elev = 0.0f;
	    if(elev > (Math.PI / 2.0)) elev = (float)Math.PI / 2.0f;
	    angl = anglorig + ((float)(c.x - dragorig.x) / 100.0f);
	    angl = angl % ((float)Math.PI * 2.0f);
	}

	public boolean wheel(Coord c, int amount) {
	    float d = dist + (amount * 5);
	    if(d < 5)
		d = 5;
	    dist = d;
	    return(true);
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
    
    public MapView(Coord c, Coord sz, Widget parent, Coord cc, long plgob) {
	super(c, sz, parent);
	glob = ui.sess.glob;
	this.cc = cc;
	this.plgob = plgob;
	setcanfocus(true);
    }
    
    public void enol(int... overlays) {
	for(int ol : overlays)
	    visol[ol]++;
    }
	
    public void disol(int... overlays) {
	for(int ol : overlays)
	    visol[ol]--;
    }
	
    private final Rendered map = new Rendered() {
	    public void draw(GOut g) {}
	    
	    public Order setup(RenderList rl) {
		Coord cc = MapView.this.cc.div(tilesz).div(MCache.cutsz);
		Coord o = new Coord();
		for(o.y = -view; o.y <= view; o.y++) {
		    for(o.x = -view; o.x <= view; o.x++) {
			Coord pc = cc.add(o).mul(MCache.cutsz).mul(tilesz);
			MapMesh cut = glob.map.getcut(cc.add(o));
			rl.add(cut, Location.xlate(new Coord3f(pc.x, -pc.y, 0)));
		    }
		}
		return(null);
	    }
	};
    
    private final Rendered mapol = new Rendered() {
	    private final GLState[] mats;
	    {
		mats = new GLState[32];
		mats[0] = new Material(new Color(255, 0, 128, 32));
		mats[1] = new Material(new Color(0, 0, 255, 32));
		mats[16] = new Material(new Color(0, 255, 0, 32));
		mats[17] = new Material(new Color(255, 255, 0, 32));
	    }
	    
	    public void draw(GOut g) {}
	    
	    public Order setup(RenderList rl) {
		Coord cc = MapView.this.cc.div(tilesz).div(MCache.cutsz);
		Coord o = new Coord();
		for(o.y = -view; o.y <= view; o.y++) {
		    for(o.x = -view; o.x <= view; o.x++) {
			Coord pc = cc.add(o).mul(MCache.cutsz).mul(tilesz);
			for(int i = 0; i < visol.length; i++) {
			    if(mats[i] == null)
				continue;
			    if(visol[i] > 0) {
				Rendered olcut;
				olcut = glob.map.getolcut(i, cc.add(o));
				if(olcut != null)
				    rl.add(olcut, GLState.compose(Location.xlate(new Coord3f(pc.x, -pc.y, 0)), mats[i]));
			    }
			}
		    }
		}
		return(null);
	    }
	};
    
    void addgob(RenderList rl, final Gob gob) {
	Coord3f c = gob.getc();
	c.y = -c.y;
	Following flw = gob.getattr(Following.class);
	if(flw != null) {
	    try {
		rl.add(gob, GLState.compose(flw.xf(), gob.save));
	    } catch(Loading e) {}
	} else {
	    rl.add(gob, GLState.compose(gob.loc, gob.save));
	}
    }

    private final Rendered gobs = new Rendered() {
	    public void draw(GOut g) {}
	    
	    public Order setup(RenderList rl) {
		synchronized(glob.oc) {
		    for(Gob gob : glob.oc)
			addgob(rl, gob);
		}
		return(null);
	    }
	};

    public Camera camera() {
	return(camera);
    }

    public void setup(RenderList rl) {
	Gob pl = player();
	if(pl != null)
	    this.cc = new Coord(pl.getc());
	if(lighting == 0) {
	    rl.add(new DirLight(new Color(128, 128, 128), Color.WHITE, Color.WHITE, new Coord3f(2.0f, 1.0f, 5.0f)), null);
	} else if(lighting == 1) {
	    rl.add(new DirLight(new Color(255, 192, 64), new Coord3f(2.0f, 1.0f, 1.0f)), null);
	}
	rl.add(map, null);
	rl.add(mapol, null);
	rl.add(gobs, null);
	if(placing != null)
	    addgob(rl, placing);
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

    private abstract static class Clicklist<T> extends RenderList {
	private Map<Color, T> rmap = new HashMap<Color, T>();
	private int i = 1;
	private GLState.Buffer plain;
	
	abstract protected T map(Rendered r);
	
	private Clicklist(GLState.Buffer plain) {
	    super(plain.cfg);
	    this.plain = plain;
	}
	
	protected Color newcol(T t) {
	    int cr = ((i & 0x00000f) << 4) | ((i & 0x00f000) >> 12),
		cg = ((i & 0x0000f0) << 0) | ((i & 0x0f0000) >> 16),
		cb = ((i & 0x000f00) >> 4) | ((i & 0xf00000) >> 20);
	    Color col = new Color(cr, cg, cb);
	    i++;
	    rmap.put(col, t);
	    return(col);
	}

	protected void render(GOut g, Rendered r) {
	    if(r instanceof FRendered)
		((FRendered)r).drawflat(g);
	}
	
	public T get(GOut g, Coord c) {
	    return(rmap.get(g.getpixel(c)));
	}
	
	protected void setup(Slot s, Rendered r) {
	    T t = map(r);
	    super.setup(s, r);
	    Location loc = s.os.get(PView.loc);
	    plain.copy(s.os);
	    s.os.put(PView.loc, loc);
	    if(t != null) {
		Color col = newcol(t);
		new States.ColState(col).prep(s.os);
	    }
	}
    }
    
    private static class Maplist extends Clicklist<MapMesh> {
	private int mode = 0;
	private MapMesh limit = null;
	
	private Maplist(GLState.Buffer plain) {
	    super(plain);
	}
	
	protected MapMesh map(Rendered r) {
	    if(r instanceof MapMesh)
		return((MapMesh)r);
	    return(null);
	}
	
	protected void render(GOut g, Rendered r) {
	    if(r instanceof MapMesh) {
		MapMesh m = (MapMesh)r;
		if(mode != 0)
		    g.st.put(States.color, null);
		if((limit == null) || (limit == m))
		    m.drawflat(g, mode);
	    }
	}
    }

    private Coord checkmapclick(GOut g, Coord c) {
	Maplist rl = new Maplist(basic(g));
	rl.setup(map, basic(g));
	rl.sort();
	{
	    rl.render(g);
	    MapMesh hit = rl.get(g, c);
	    if(hit == null)
		return(null);
	    rl.limit = (MapMesh)hit;
	}
	Coord tile;
	{
	    rl.mode = 1;
	    rl.render(g);
	    Color hitcol = g.getpixel(c);
	    tile = new Coord(hitcol.getRed() - 1, hitcol.getGreen() - 1);
	    if(!tile.isect(Coord.z, rl.limit.sz))
		return(null);
	}
	Coord pixel;
	{
	    rl.mode = 2;
	    rl.render(g);
	    Color hitcol = g.getpixel(c);
	    if(hitcol.getBlue() != 0)
		return(null);
	    pixel = new Coord((hitcol.getRed() * tilesz.x) / 255, (hitcol.getGreen() * tilesz.y) / 255);
	}
	return(rl.limit.ul.add(tile).mul(tilesz).add(pixel));
    }
    
    private static class ClickInfo {
	Gob gob;
	Rendered r;
	
	ClickInfo(Gob gob, Rendered r) {
	    this.gob = gob; this.r = r;
	}
    }

    private ClickInfo checkgobclick(GOut g, Coord c) {
	Clicklist<ClickInfo> rl = new Clicklist<ClickInfo>(basic(g)) {
		Gob curgob;
		ClickInfo curinfo;
		public ClickInfo map(Rendered r) {
		    return(curinfo);
		}
		
		public void add(Rendered r, GLState t) {
		    if(r instanceof Gob)
			curgob = (Gob)r;
		    if((curgob == null) || !(r instanceof FRendered))
			curinfo = null;
		    else
			curinfo = new ClickInfo(curgob, r);
		    super.add(r, t);
		}
	    };
	rl.setup(gobs, basic(g));
	rl.sort();
	rl.render(g);
	return(rl.get(g, c));
    }

    protected void undelay(GOut g) {
	synchronized(delayed) {
	    for(Delayed d : delayed)
		d.run(g);
	    delayed.clear();
	}
    }

    public void draw(GOut g) {
	glob.map.sendreqs();
	if((olftimer != 0) && (olftimer < System.currentTimeMillis()))
	    unflashol();
	try {
	    undelay(g);
	    super.draw(g);
	} catch(MCache.LoadingMap e) {
	    String text = "Loading...";
	    g.chcolor(Color.BLACK);
	    g.frect(Coord.z, sz);
	    g.chcolor(Color.WHITE);
	    g.atext(text, sz.div(2), 0.5, 0.5);
	}
	long now = System.currentTimeMillis();
	synchronized(glob.oc) {
	    for(Gob gob : glob.oc) {
		Speaking sp = gob.getattr(Speaking.class);
		if((sp != null) && (gob.sc != null))
		    sp.draw(g, gob.sc.add(new Coord(gob.sczu.mul(sp.zo))));
		KinInfo k = gob.getattr(KinInfo.class);
		if((k != null) && (gob.sc != null)) {
		    Coord sc = gob.sc.add(new Coord(gob.sczu.mul(15)));
		    if(sc.isect(Coord.z, sz)) {
			if(k.seen == 0)
			    k.seen = now;
			int tm = (int)(now - k.seen);
			Color show = null;
			boolean auto = (k.type & 1) == 0;
			if(auto && (tm < 7500)) {
			    show = Utils.clipcol(255, 255, 255, 255 - ((255 * tm) / 7500));
			}
			if(show != null) {
			    Tex t = k.rendered();
			    g.chcolor(show);
			    g.aimage(t, sc, 0.5, 1.0);
			    g.chcolor();
			}
		    } else {
			k.seen = 0;
		    }
		}
	    }
	}
    }
    
    public void resize(Coord sz) {
	super.resize(sz);
	camera.resized();
    }

    private class Plob extends Gob {
	Coord lastmc = null;
	boolean freerot = false;
	
	private Plob(Resource res) {
	    super(MapView.this.glob, Coord.z);
	    setattr(new ResDrawable(this, res));
	    if(ui.mc.isect(rootpos(), sz)) {
		synchronized(delayed) {
		    delayed.add(new Adjust(ui.mc.sub(rootpos())));
		}
	    }
	}

	private class Adjust implements Delayed {
	    Coord mouse;
	    
	    Adjust(Coord c) {
		mouse = c;
	    }
	    
	    public void run(GOut g) {
		GLState.Buffer bk = g.st.copy();
		Coord mc;
		try {
		    GL gl = g.gl;
		    g.st.set(basic(g));
		    g.apply();
		    gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
		    mc = checkmapclick(g, mouse);
		} finally {
		    g.st.set(bk);
		}
		if(mc != null)
		    rc = mc;
		Gob pl = player();
		if((pl != null) && !freerot)
		    a = rc.angle(pl.rc);
		lastmc = mouse;
	    }
	}
    }

    private int olflash;
    private long olftimer;

    private void unflashol() {
	for(int i = 0; i < visol.length; i++) {
	    if((olflash & (1 << i)) != 0)
		visol[i]--;
	}
	olflash = 0;
	olftimer = 0;
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "place") {
	    Resource res = Resource.load((String)args[0], (Integer)args[1]);
	    placing = new Plob(res);
	} else if(msg == "unplace") {
	    placing = null;
	} else if(msg == "move") {
	    cc = (Coord)args[0];
	} else if(msg == "flashol") {
	    unflashol();
	    olflash = (Integer)args[0];
	    for(int i = 0; i < visol.length; i++) {
		if((olflash & (1 << i)) != 0)
		    visol[i]++;
	    }
	    olftimer = System.currentTimeMillis() + (Integer)args[1];
	} else {
	    super.uimsg(msg, args);
	}
    }

    private boolean camdrag = false;
    
    public abstract class Hittest implements Delayed {
	private final Coord clickc;
	
	public Hittest(Coord c) {
	    clickc = c;
	}
	
	public void run(GOut g) {
	    GLState.Buffer bk = g.st.copy();
	    Coord mapcl;
	    ClickInfo gobcl;
	    try {
		GL gl = g.gl;
		g.st.set(basic(g));
		g.apply();
		gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
		mapcl = checkmapclick(g, clickc);
		g.st.set(bk);
		g.st.set(basic(g));
		g.apply();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gobcl = checkgobclick(g, clickc);
	    } finally {
		g.st.set(bk);
	    }
	    if(mapcl != null) {
		if(gobcl == null)
		    hit(clickc, mapcl, null, null);
		else
		    hit(clickc, mapcl, gobcl.gob, gobcl.r);
	    }
	}
	
	protected abstract void hit(Coord pc, Coord mc, Gob gob, Rendered tgt);
    }

    private static int getid(Rendered tgt) {
	if(tgt instanceof FastMesh.ResourceMesh)
	    return(((FastMesh.ResourceMesh)tgt).id);
	return(-1);
    }

    private class Click extends Hittest {
	int clickb;
	
	private Click(Coord c, int b) {
	    super(c);
	    clickb = b;
	}
	
	protected void hit(Coord pc, Coord mc, Gob gob, Rendered tgt) {
	    if(grab != null) {
		if(grab.mmousedown(mc, clickb))
		    return;
	    }
	    if(gob == null)
		wdgmsg("click", pc, mc, clickb, ui.modflags());
	    else
		wdgmsg("click", pc, mc, clickb, ui.modflags(), (int)gob.id, gob.rc, getid(tgt));
	}
    }
    
    public void grab(Grabber grab) {
	this.grab = grab;
    }
    
    public void release(Grabber grab) {
	if(this.grab == grab)
	    this.grab = null;
    }
    
    public boolean mousedown(Coord c, int button) {
	parent.setfocus(this);
	if(button == 2) {
	    if(((Camera)camera).click(c)) {
		ui.grabmouse(this);
		camdrag = true;
	    }
	} else if(placing != null) {
	    if(placing.lastmc != null)
		wdgmsg("place", placing.rc, (int)(placing.a * 180 / Math.PI), button, ui.modflags());
	} else {
	    synchronized(delayed) {
		delayed.add(new Click(c, button));
	    }
	}
	return(true);
    }
    
    public void mousemove(Coord c) {
	if(camdrag) {
	    ((Camera)camera).drag(c);
	} else if(grab != null) {
	    synchronized(delayed) {
		delayed.add(new Hittest(c) {
			public void hit(Coord pc, Coord mc, Gob gob, Rendered tgt) {
			    grab.mmousemove(mc);
			}
		    });
	    }
	} else if(placing != null) {
	    if((placing.lastmc == null) || !placing.lastmc.equals(c)) {
		synchronized(delayed) {
		    delayed.add(placing.new Adjust(c));
		}
	    }
	}
    }
    
    public boolean mouseup(Coord c, final int button) {
	if(button == 2) {
	    if(camdrag) {
		((Camera)camera).release();
		ui.grabmouse(null);
		camdrag = false;
	    }
	} else if(grab != null) {
	    synchronized(delayed) {
		delayed.add(new Hittest(c) {
			public void hit(Coord pc, Coord mc, Gob gob, Rendered tgt) {
			    grab.mmouseup(mc, button);
			}
		    });
	    }
	}
	return(true);
    }

    public boolean mousewheel(Coord c, int amount) {
	if(ui.modshift) {
	    if(placing != null) {
		placing.freerot = true;
		placing.a += amount * 0.2;
	    }
	    return(true);
	}
	return(((Camera)camera).wheel(c, amount));
    }
    
    public boolean drop(final Coord cc, final Coord ul) {
	synchronized(delayed) {
	    delayed.add(new Hittest(cc) {
		    public void hit(Coord pc, Coord mc, Gob gob, Rendered tgt) {
			wdgmsg("drop", pc, mc, ui.modflags());
		    }
		});
	}
	return(true);
    }
    
    public boolean iteminteract(Coord cc, Coord ul) {
	synchronized(delayed) {
	    delayed.add(new Hittest(cc) {
		    public void hit(Coord pc, Coord mc, Gob gob, Rendered tgt) {
			if(gob == null)
			    wdgmsg("itemact", pc, mc, ui.modflags());
			else
			    wdgmsg("itemact", pc, mc, ui.modflags(), (int)gob.id, gob.rc, getid(tgt));
		    }
		});
	}
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
