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
    private Collection<Delayed> delayed2 = new LinkedList<Delayed>();
    public Camera camera = new FollowCam();
    private Plob placing = null;
    private int[] visol = new int[32];
    private Grabber grab;
    
    public interface Delayed {
	public void run(GOut g);
    }

    public interface Grabber {
	boolean mmousedown(Coord mc, int button);
	boolean mmouseup(Coord mc, int button);
	void mmousemove(Coord mc);
    }
    
    public abstract class Camera extends GLState.Abstract {
	protected haven.Camera view = new haven.Camera(Matrix4f.identity());
	protected Projection proj = new Projection(Matrix4f.identity());
	
	public Camera() {
	    resized();
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
	    float field = 0.5f;
	    float aspect = ((float)sz.y) / ((float)sz.x);
	    proj.update(Projection.makefrustum(new Matrix4f(), -field, field, -aspect * field, aspect * field, 1, 5000));
	}

	public void prep(Buffer buf) {
	    proj.prep(buf);
	    view.prep(buf);
	}
	
	public abstract float angle();
	public abstract void tick(double dt);
    }
    
    private class FollowCam extends Camera {
	private final float fr = 0.0f, h = 10.0f;
	private float ca, cd;
	private Coord3f curc = null;
	private float elev, telev;
	private float angl, tangl;
	private Coord dragorig = null;
	private float anglorig;
	
	private FollowCam() {
	    elev = telev = (float)Math.PI / 6.0f;
	    angl = tangl = 0.0f;
	}
	
	public void resized() {
	    ca = (float)sz.y / (float)sz.x;
	    cd = 400.0f * ca;
	}
	
	public boolean click(Coord c) {
	    anglorig = tangl;
	    dragorig = c;
	    return(true);
	}
	
	public void drag(Coord c) {
	    tangl = anglorig + ((float)(c.x - dragorig.x) / 100.0f);
	    tangl = tangl % ((float)Math.PI * 2.0f);
	}

	private double f0 = 0.2, f1 = 0.5, f2 = 0.9;
	private double fl = Math.sqrt(2);
	private double fa = ((fl * (f1 - f0)) - (f2 - f0)) / (fl - 2);
	private double fb = ((f2 - f0) - (2 * (f1 - f0))) / (fl - 2);
	private float field(float elev) {
	    double a = elev / (Math.PI / 4);
	    return((float)(f0 + (fa * a) + (fb * Math.sqrt(a))));
	}

	private float dist(float elev) {
	    float da = (float)Math.atan(ca * field(elev));
	    return((float)(((cd - (h / Math.tan(elev))) * Math.sin(elev - da) / Math.sin(da)) - (h / Math.sin(elev))));
	}

	public void tick(double dt) {
	    elev += (telev - elev) * (float)(1.0 - Math.pow(500, -dt));
	    if(Math.abs(telev - elev) < 0.0001)
		elev = telev;
	    
	    float dangl = tangl - angl;
	    while(dangl >  Math.PI) dangl -= (float)(2 * Math.PI);
	    while(dangl < -Math.PI) dangl += (float)(2 * Math.PI);
	    angl += dangl * (float)(1.0 - Math.pow(500, -dt));
	    if(Math.abs(tangl - angl) < 0.0001)
		angl = tangl;
	    
	    Coord3f cc = getcc();
	    cc.y = -cc.y;
	    if(curc == null)
		curc = cc;
	    float dx = cc.x - curc.x, dy = cc.y - curc.y;
	    float dist = (float)Math.sqrt((dx * dx) + (dy * dy));
	    if(dist > 250) {
		curc = cc;
	    } else if(dist > fr) {
		Coord3f oc = curc;
		float pd = (float)Math.cos(elev) * dist(elev);
		Coord3f cambase = new Coord3f(curc.x + ((float)Math.cos(tangl) * pd), curc.y + ((float)Math.sin(tangl) * pd), 0.0f);
		float a = cc.xyangle(curc);
		float nx = cc.x + ((float)Math.cos(a) * fr), ny = cc.y + ((float)Math.sin(a) * fr);
		Coord3f tgtc = new Coord3f(nx, ny, cc.z);
		curc = curc.add(tgtc.sub(curc).mul((float)(1.0 - Math.pow(500, -dt))));
		if(curc.dist(tgtc) < 0.01)
		    curc = tgtc;
		tangl = curc.xyangle(cambase);
	    }
	    
	    float field = field(elev);
	    view.update(PointedCam.compute(curc.add(0.0f, 0.0f, h), dist(elev), elev, angl));
	    proj.update(Projection.makefrustum(new Matrix4f(), -field, field, -ca * field, ca * field, 1, 5000));
	}

	public float angle() {
	    return(angl);
	}
	
	private static final float maxang = (float)(Math.PI / 2 - 0.1);
	private static final float mindist = 50.0f;
	public boolean wheel(Coord c, int amount) {
	    float fe = telev;
	    telev += amount * telev * 0.02f;
	    if(telev > maxang)
		telev = maxang;
	    if(dist(telev) < mindist)
		telev = fe;
	    return(true);
	}

	public String toString() {
	    return(String.format("%f %f %f", elev, dist(elev), field(elev)));
	}
    }

    private class FreeCam extends Camera {
	private float dist = 50.0f;
	private float elev = (float)Math.PI / 4.0f;
	private float angl = 0.0f;
	private Coord dragorig = null;
	private float elevorig, anglorig;

	public void tick(double dt) {
	    Coord3f cc = getcc();
	    cc.y = -cc.y;
	    view.update(PointedCam.compute(cc.add(0.0f, 0.0f, 15f), dist, elev, angl));
	}
	
	public float angle() {
	    return(angl);
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
	    
	    public boolean setup(RenderList rl) {
		Coord cc = MapView.this.cc.div(tilesz).div(MCache.cutsz);
		Coord o = new Coord();
		for(o.y = -view; o.y <= view; o.y++) {
		    for(o.x = -view; o.x <= view; o.x++) {
			Coord pc = cc.add(o).mul(MCache.cutsz).mul(tilesz);
			MapMesh cut = glob.map.getcut(cc.add(o));
			rl.add(cut, Location.xlate(new Coord3f(pc.x, -pc.y, 0)));
			Collection<Gob> fol;
			try {
			    fol = glob.map.getfo(cc.add(o));
			} catch(Loading e) {
			    fol = Collections.emptyList();
			}
			for(Gob fo : fol)
			    addgob(rl, fo);
		    }
		}
		return(false);
	    }
	};
    
    private final Rendered mapol = new Rendered() {
	    private final GLState[] mats;
	    {
		mats = new GLState[32];
		mats[0] = new Material(new Color(255, 0, 128, 32));
		mats[1] = new Material(new Color(0, 0, 255, 32));
		mats[2] = new Material(new Color(255, 0, 0, 32));
		mats[3] = new Material(new Color(128, 0, 255, 32));
		mats[16] = new Material(new Color(0, 255, 0, 32));
		mats[17] = new Material(new Color(255, 255, 0, 32));
	    }
	    
	    public void draw(GOut g) {}
	    
	    public boolean setup(RenderList rl) {
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
		return(false);
	    }
	};
    
    void addgob(RenderList rl, final Gob gob) {
	Coord3f c = gob.getc();
	c.y = -c.y;
	GLState xf;
	try {
	    xf = Following.xf(gob);
	} catch(Loading e) {
	    xf = null;
	}
	if(xf != null)
	    rl.add(gob, GLState.compose(xf, gob.olmod, gob.save));
	else
	    rl.add(gob, GLState.compose(gob.loc, gob.olmod, gob.save));
    }

    private final Rendered gobs = new Rendered() {
	    public void draw(GOut g) {}
	    
	    public boolean setup(RenderList rl) {
		synchronized(glob.oc) {
		    for(Gob gob : glob.oc)
			addgob(rl, gob);
		}
		return(false);
	    }
	};

    public GLState camera()         {return(camera);}
    protected Projection makeproj() {return(null);}

    private Coord3f smapcc = null;
    private Light.PSLights.ShadowMap smap = null;
    public void setup(RenderList rl) {
	Gob pl = player();
	if(pl != null)
	    this.cc = new Coord(pl.getc());
	synchronized(glob) {
	    if(glob.lightamb != null) {
		DirLight light = new DirLight(glob.lightamb, glob.lightdif, glob.lightspc, Coord3f.o.sadd((float)glob.lightelev, (float)glob.lightang, 1f));
		rl.add(light, null);
		if(rl.cfg.pref.light.val == GLSettings.Lights.PSLIGHT) {
		    if(smap == null)
			smap = new Light.PSLights.ShadowMap(new Coord(2048, 2048), 750, 5000);
		    smap.light = light;
		    Coord3f dir = new Coord3f(-light.dir[0], -light.dir[1], -light.dir[2]);
		    Coord3f cc = getcc();
		    cc.y = -cc.y;
		    if((smapcc == null) || (smapcc.dist(cc) > 50)) {
			smapcc = cc;
			smap.setpos(smapcc.add(dir.neg().mul(1000f)), dir);
		    }
		    rl.prepc(smap);
		} else {
		    if(smap != null)
			smap.dispose();
		    smap = null;
		    smapcc = null;
		}
	    }
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
	private GLState.Buffer plain, bk;
	
	abstract protected T map(Rendered r);
	
	private Clicklist(GLState.Buffer plain) {
	    super(plain.cfg);
	    this.plain = plain;
	    this.bk = new GLState.Buffer(plain.cfg);
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
	    s.os.copy(bk);
	    plain.copy(s.os);
	    bk.copy(s.os, GLState.Slot.Type.GEOM);
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
	rl.fin();
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
	rl.fin();
	rl.render(g);
	return(rl.get(g, c));
    }
    
    public void delay(Delayed d) {
	synchronized(delayed) {
	    delayed.add(d);
	}
    }

    public void delay2(Delayed d) {
	synchronized(delayed2) {
	    delayed2.add(d);
	}
    }

    protected void undelay(Collection<Delayed> list, GOut g) {
	synchronized(list) {
	    for(Delayed d : list)
		d.run(g);
	    list.clear();
	}
    }

    private static final Text.Foundry polownertf;
    static {
	polownertf = new Text.Foundry("serif", 20);
	polownertf.aa = true;
    }
    private Text polownert = null;
    private long polchtm = 0;

    public void setpoltext(String text) {
	polownert = polownertf.render(text);
	polchtm = System.currentTimeMillis();
    }

    private void poldraw(GOut g) {
	long now = System.currentTimeMillis();
	long poldt = now - polchtm;
	if((polownert != null) && (poldt < 6000)) {
	    int a;
	    if(poldt < 1000)
		a = (int)((255 * poldt) / 1000);
	    else if(poldt < 4000)
		a = 255;
	    else
		a = (int)((255 * (2000 - (poldt - 4000))) / 2000);
	    g.chcolor(255, 255, 255, a);
	    g.aimage(polownert.tex(), sz.div(2), 0.5, 0.5);
	    g.chcolor();
	}
    }
    
    private void drawarrow(GOut g, double a) {
	Coord hsz = sz.div(2);
	double ca = -Coord.z.angle(hsz);
	Coord ac;
	if((a > ca) && (a < -ca)) {
	    ac = new Coord(sz.x, hsz.y - (int)(Math.tan(a) * hsz.x));
	} else if((a > -ca) && (a < Math.PI + ca)) {
	    ac = new Coord(hsz.x - (int)(Math.tan(a - Math.PI / 2) * hsz.y), 0);
	} else if((a > -Math.PI - ca) && (a < ca)) {
	    ac = new Coord(hsz.x + (int)(Math.tan(a + Math.PI / 2) * hsz.y), sz.y);
	} else {
	    ac = new Coord(0, hsz.y + (int)(Math.tan(a) * hsz.x));
	}
	Coord bc = ac.add(Coord.sc(a, -10));
	g.line(bc, bc.add(Coord.sc(a, -40)), 2);
	g.line(bc, bc.add(Coord.sc(a + Math.PI / 4, -10)), 2);
	g.line(bc, bc.add(Coord.sc(a - Math.PI / 4, -10)), 2);
    }

    private void partydraw(GOut g) {
	Coord3f cc;
	Projection proj;
	Matrix4f cam;
	try {
	    Gob plgob = player();
	    if(plgob == null)
		return;
	    proj = plgob.save.proj;
	    cam = plgob.save.cam;
	    if((proj == null) || (cam == null))
		return;
	    cc = plgob.getc();
	} catch(Loading e) {
	    return;
	}
	for(Party.Member m : ui.sess.glob.party.memb.values()) {
	    if(m.gobid == this.plgob)
		continue;
	    try {
		Coord mc = m.getc();
		if(mc == null)
		    continue;
		Coord3f ploc = new Coord3f(mc.x, -mc.y, cc.z);
		Coord3f sloc = proj.tonorm(cam.mul4(ploc));
		if(sloc.z < 0)
		    sloc = sloc.inv();
		if((sloc.x < -1) || (sloc.x > 1) || (sloc.y < -1) || (sloc.y > 1)) {
		    g.chcolor(m.col);
		    drawarrow(g, Coord3f.o.xyangle(sloc));
		}
	    } catch(Loading e) {
	    }
	}
	g.chcolor();
    }

    private boolean camload = false;
    public void draw(GOut g) {
	glob.map.sendreqs();
	if((olftimer != 0) && (olftimer < System.currentTimeMillis()))
	    unflashol();
	try {
	    if(camload)
		throw(new MCache.LoadingMap());
	    undelay(delayed, g);
	    super.draw(g);
	    undelay(delayed2, g);
	    poldraw(g);
	    partydraw(g);
	    glob.map.reqarea(cc.div(tilesz).sub(MCache.cutsz.mul(view + 1)),
			     cc.div(tilesz).add(MCache.cutsz.mul(view + 1)));
	} catch(Loading e) {
	    String text = "Loading...";
	    g.chcolor(Color.BLACK);
	    g.frect(Coord.z, sz);
	    g.chcolor(Color.WHITE);
	    g.atext(text, sz.div(2), 0.5, 0.5);
	}
    }
    
    public void tick(double dt) {
	camload = false;
	try {
	    camera.tick(dt);
	} catch(Loading e) {
	    camload = true;
	}
	if(placing != null)
	    placing.ctick((int)(dt * 1000));
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
		delay(new Adjust(ui.mc.sub(rootpos()), false));
	    }
	}

	private class Adjust implements Delayed {
	    Coord mouse;
	    boolean adjust;
	    
	    Adjust(Coord c, boolean ta) {
		mouse = c;
		adjust = ta;
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
		if(adjust)
		    rc = rc.div(tilesz).mul(tilesz).add(tilesz.div(2));
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
	    } else {
		nohit(clickc);
	    }
	}
	
	protected abstract void hit(Coord pc, Coord mc, Gob gob, Rendered tgt);
	protected void nohit(Coord pc) {}
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
	    delay(new Click(c, button));
	}
	return(true);
    }
    
    public void mousemove(Coord c) {
	if(camdrag) {
	    ((Camera)camera).drag(c);
	} else if(grab != null) {
	    delay(new Hittest(c) {
		    public void hit(Coord pc, Coord mc, Gob gob, Rendered tgt) {
			grab.mmousemove(mc);
		    }
		});
	} else if(placing != null) {
	    if((placing.lastmc == null) || !placing.lastmc.equals(c)) {
		delay(placing.new Adjust(c, !ui.modctrl));
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
	    delay(new Hittest(c) {
		    public void hit(Coord pc, Coord mc, Gob gob, Rendered tgt) {
			grab.mmouseup(mc, button);
		    }
		});
	}
	return(true);
    }

    public boolean mousewheel(Coord c, int amount) {
	if(ui.modshift) {
	    if(placing != null) {
		placing.freerot = true;
		if(!ui.modctrl)
		    placing.a = (Math.PI / 4) * Math.round((placing.a + (amount * Math.PI / 4)) / (Math.PI / 4));
		else
		    placing.a += amount * Math.PI / 16;
	    }
	    return(true);
	}
	return(((Camera)camera).wheel(c, amount));
    }
    
    public boolean drop(final Coord cc, final Coord ul) {
	delay(new Hittest(cc) {
		public void hit(Coord pc, Coord mc, Gob gob, Rendered tgt) {
		    wdgmsg("drop", pc, mc, ui.modflags());
		}
	    });
	return(true);
    }
    
    public boolean iteminteract(Coord cc, Coord ul) {
	delay(new Hittest(cc) {
		public void hit(Coord pc, Coord mc, Gob gob, Rendered tgt) {
		    if(gob == null)
			wdgmsg("itemact", pc, mc, ui.modflags());
		    else
			wdgmsg("itemact", pc, mc, ui.modflags(), (int)gob.id, gob.rc, getid(tgt));
		}
	    });
	return(true);
    }

    public boolean globtype(char c, java.awt.event.KeyEvent ev) {
	return(false);
    }
}
