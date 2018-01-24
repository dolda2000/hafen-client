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

public class Gob implements Sprite.Owner, Skeleton.ModOwner, Rendered {
    public Coord2d rc;
    public Coord sc;
    public Coord3f sczu;
    public double a;
    public boolean virtual = false;
    int clprio = 0;
    public long id;
    public int frame;
    public final Glob glob;
    Map<Class<? extends GAttrib>, GAttrib> attr = new HashMap<Class<? extends GAttrib>, GAttrib>();
    public Collection<Overlay> ols = new LinkedList<Overlay>() {
	public boolean add(Overlay item) {
	    /* XXX: Remove me once local code is changed to use addol(). */
	    if(glob.oc.getgob(id) != null)
		glob.oc.changed(Gob.this);
	    return(super.add(item));
	}
    };
    private final Collection<ResAttr.Cell<?>> rdata = new LinkedList<ResAttr.Cell<?>>();
    private final Collection<ResAttr.Load> lrdata = new LinkedList<ResAttr.Load>();

    public static class Overlay implements Rendered {
	public Indir<Resource> res;
	public MessageBuf sdt;
	public Sprite spr;
	public int id;
	public boolean delign = false;

	public Overlay(int id, Indir<Resource> res, Message sdt) {
	    this.id = id;
	    this.res = res;
	    this.sdt = new MessageBuf(sdt);
	    spr = null;
	}

	public Overlay(Sprite spr) {
	    this.id = -1;
	    this.res = null;
	    this.sdt = null;
	    this.spr = spr;
	}

	public static interface CDel {
	    public void delete();
	}

	public static interface CUpd {
	    public void update(Message sdt);
	}

	public static interface SetupMod {
	    public void setupgob(GLState.Buffer buf);
	    public void setupmain(RenderList rl);
	}

	public void draw(GOut g) {}
	public boolean setup(RenderList rl) {
	    if(spr != null)
		rl.add(spr, null);
	    return(false);
	}

	public Object staticp() {
	    return((spr == null)?null:spr.staticp());
	}
    }

    /* XXX: This whole thing didn't turn out quite as nice as I had
     * hoped, but hopefully it can at least serve as a source of
     * inspiration to redo attributes properly in the future. There
     * have already long been arguments for remaking GAttribs as
     * well. */
    public static class ResAttr {
	public boolean update(Message dat) {
	    return(false);
	}

	public void dispose() {
	}

	public static class Cell<T extends ResAttr> {
	    final Class<T> clsid;
	    Indir<Resource> resid = null;
	    MessageBuf odat;
	    public T attr = null;

	    public Cell(Class<T> clsid) {
		this.clsid = clsid;
	    }

	    public void set(ResAttr attr) {
		if(this.attr != null)
		    this.attr.dispose();
		this.attr = clsid.cast(attr);
	    }
	}

	private static class Load {
	    final Indir<Resource> resid;
	    final MessageBuf dat;

	    Load(Indir<Resource> resid, Message dat) {
		this.resid = resid;
		this.dat = new MessageBuf(dat);
	    }
	}

	@Resource.PublishedCode(name = "gattr", instancer = FactMaker.class)
	public static interface Factory {
	    public ResAttr mkattr(Gob gob, Message dat);
	}

	public static class FactMaker implements Resource.PublishedCode.Instancer {
	    public Factory make(Class<?> cl) throws InstantiationException, IllegalAccessException {
		if(Factory.class.isAssignableFrom(cl))
		    return(cl.asSubclass(Factory.class).newInstance());
		if(ResAttr.class.isAssignableFrom(cl)) {
		    try {
			final java.lang.reflect.Constructor<? extends ResAttr> cons = cl.asSubclass(ResAttr.class).getConstructor(Gob.class, Message.class);
			return(new Factory() {
				public ResAttr mkattr(Gob gob, Message dat) {
				    return(Utils.construct(cons, gob, dat));
				}
			    });
		    } catch(NoSuchMethodException e) {
		    }
		}
		return(null);
	    }
	}
    }

    public static class Static {}
    public static class SemiStatic {}

    public Gob(Glob glob, Coord2d c, long id, int frame) {
	this.glob = glob;
	this.rc = c;
	this.id = id;
	this.frame = frame;
	loc.tick();
    }

    public Gob(Glob glob, Coord2d c) {
	this(glob, c, -1, 0);
    }

    public static interface ANotif<T extends GAttrib> {
	public void ch(T n);
    }

    public void ctick(int dt) {
	for(GAttrib a : attr.values())
	    a.ctick(dt);
	for(Iterator<Overlay> i = ols.iterator(); i.hasNext();) {
	    Overlay ol = i.next();
	    if(ol.spr == null) {
		try {
		    ol.spr = Sprite.create(this, ol.res.get(), ol.sdt.clone());
		} catch(Loading e) {}
	    } else {
		boolean done = ol.spr.tick(dt);
		if((!ol.delign || (ol.spr instanceof Overlay.CDel)) && done)
		    i.remove();
	    }
	}
	if(virtual && ols.isEmpty())
	    glob.oc.remove(id);
    }

    /* Intended for local code. Server changes are handled via OCache. */
    public void addol(Overlay ol) {
	ols.add(ol);
    }
    public void addol(Sprite ol) {
	addol(new Overlay(ol));
    }

    public Overlay findol(int id) {
	for(Overlay ol : ols) {
	    if(ol.id == id)
		return(ol);
	}
	return(null);
    }

    public void tick() {
	for(GAttrib a : attr.values())
	    a.tick();
	loadrattr();
    }

    public void dispose() {
	for(GAttrib a : attr.values())
	    a.dispose();
	for(ResAttr.Cell rd : rdata) {
	    if(rd.attr != null)
		rd.attr.dispose();
	}
    }

    public void move(Coord2d c, double a) {
	Moving m = getattr(Moving.class);
	if(m != null)
	    m.move(c);
	this.rc = c;
	this.a = a;
    }

    public Coord3f getc() {
	Moving m = getattr(Moving.class);
	Coord3f ret = (m != null)?m.getc():getrc();
	DrawOffset df = getattr(DrawOffset.class);
	if(df != null)
	    ret = ret.add(df.off);
	return(ret);
    }

    public Coord3f getrc() {
	return(glob.map.getzp(rc));
    }

    private Class<? extends GAttrib> attrclass(Class<? extends GAttrib> cl) {
	while(true) {
	    Class<?> p = cl.getSuperclass();
	    if(p == GAttrib.class)
		return(cl);
	    cl = p.asSubclass(GAttrib.class);
	}
    }

    public void setattr(GAttrib a) {
	Class<? extends GAttrib> ac = attrclass(a.getClass());
	attr.put(ac, a);
    }

    public <C extends GAttrib> C getattr(Class<C> c) {
	GAttrib attr = this.attr.get(attrclass(c));
	if(!c.isInstance(attr))
	    return(null);
	return(c.cast(attr));
    }

    public void delattr(Class<? extends GAttrib> c) {
	attr.remove(attrclass(c));
    }

    private Class<? extends ResAttr> rattrclass(Class<? extends ResAttr> cl) {
	while(true) {
	    Class<?> p = cl.getSuperclass();
	    if(p == ResAttr.class)
		return(cl);
	    cl = p.asSubclass(ResAttr.class);
	}
    }

    @SuppressWarnings("unchecked")
    public <T extends ResAttr> ResAttr.Cell<T> getrattr(Class<T> c) {
	for(ResAttr.Cell<?> rd : rdata) {
	    if(rd.clsid == c)
		return((ResAttr.Cell<T>)rd);
	}
	ResAttr.Cell<T> rd = new ResAttr.Cell<T>(c);
	rdata.add(rd);
	return(rd);
    }

    public static <T extends ResAttr> ResAttr.Cell<T> getrattr(Object obj, Class<T> c) {
	if(!(obj instanceof Gob))
	    return(new ResAttr.Cell<T>(c));
	return(((Gob)obj).getrattr(c));
    }

    private void loadrattr() {
	boolean upd = false;
	for(Iterator<ResAttr.Load> i = lrdata.iterator(); i.hasNext();) {
	    ResAttr.Load rd = i.next();
	    ResAttr attr;
	    try {
		attr = rd.resid.get().getcode(ResAttr.Factory.class, true).mkattr(this, rd.dat.clone());
	    } catch(Loading l) {
		continue;
	    }
	    ResAttr.Cell<?> rc = getrattr(rattrclass(attr.getClass()));
	    if(rc.resid == null)
		rc.resid = rd.resid;
	    else if(rc.resid != rd.resid)
		throw(new RuntimeException("Conflicting resattr resource IDs on " + rc.clsid + ": " + rc.resid + " -> " + rd.resid));
	    rc.odat = rd.dat;
	    rc.set(attr);
	    i.remove();
	    upd = true;
	}
	if(upd) {
	    if(glob.oc.getgob(id) != null)
		glob.oc.changed(this);
	}
    }

    public void setrattr(Indir<Resource> resid, Message dat) {
	for(Iterator<ResAttr.Cell<?>> i = rdata.iterator(); i.hasNext();) {
	    ResAttr.Cell<?> rd = i.next();
	    if(rd.resid == resid) {
		if(dat.equals(rd.odat))
		    return;
		if((rd.attr != null) && rd.attr.update(dat))
		    return;
		break;
	    }
	}
	for(Iterator<ResAttr.Load> i = lrdata.iterator(); i.hasNext();) {
	    ResAttr.Load rd = i.next();
	    if(rd.resid == resid) {
		i.remove();
		break;
	    }
	}
	lrdata.add(new ResAttr.Load(resid, dat));
	loadrattr();
    }

    public void delrattr(Indir<Resource> resid) {
	for(Iterator<ResAttr.Cell<?>> i = rdata.iterator(); i.hasNext();) {
	    ResAttr.Cell<?> rd = i.next();
	    if(rd.resid == resid) {
		i.remove();
		rd.attr.dispose();
		break;
	    }
	}
	for(Iterator<ResAttr.Load> i = lrdata.iterator(); i.hasNext();) {
	    ResAttr.Load rd = i.next();
	    if(rd.resid == resid) {
		i.remove();
		break;
	    }
	}
    }

    public void draw(GOut g) {}

    public boolean setup(RenderList rl) {
	loc.tick();
	for(Overlay ol : ols)
	    rl.add(ol, null);
	for(Overlay ol : ols) {
	    if(ol.spr instanceof Overlay.SetupMod)
		((Overlay.SetupMod)ol.spr).setupmain(rl);
	}
	GobHealth hlt = getattr(GobHealth.class);
	if(hlt != null)
	    rl.prepc(hlt.getfx());
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    d.setup(rl);
	Speaking sp = getattr(Speaking.class);
	if(sp != null)
	    rl.add(sp.fx, null);
	KinInfo ki = getattr(KinInfo.class);
	if(ki != null)
	    rl.add(ki.fx, null);
	return(false);
    }

    private static final Object DYNAMIC = new Object();
    private Object seq = null;
    public Object staticp() {
	if(seq == null) {
	    int rs = 0;
	    for(GAttrib attr : attr.values()) {
		Object as = attr.staticp();
		if(as == Rendered.CONSTANS) {
		} else if(as instanceof Static) {
		} else if(as == SemiStatic.class) {
		    rs = Math.max(rs, 1);
		} else {
		    rs = 2;
		    break;
		}
	    }
	    for(Overlay ol : ols) {
		Object os = ol.staticp();
		if(os == Rendered.CONSTANS) {
		} else if(os instanceof Static) {
		} else if(os == SemiStatic.class) {
		    rs = Math.max(rs, 1);
		} else {
		    rs = 2;
		    break;
		}
	    }
	    switch(rs) {
	    case 0: seq = new Static(); break;
	    case 1: seq = new SemiStatic(); break;
	    default: seq = null; break;
	    }
	}
	return((seq == DYNAMIC)?null:seq);
    }

    void changed() {
	seq = null;
    }

    public Random mkrandoom() {
	return(Utils.mkrandoom(id));
    }

    public Resource getres() {
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    return(d.getres());
	return(null);
    }

    private static final ClassResolver<Gob> ctxr = new ClassResolver<Gob>()
	.add(Glob.class, g -> g.glob)
	.add(Session.class, g -> g.glob.sess);
    public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

    @Deprecated
    public Glob glob() {return(context(Glob.class));}

    /* Because generic functions are too nice a thing for Java. */
    public double getv() {
	Moving m = getattr(Moving.class);
	if(m == null)
	    return(0);
	return(m.getv());
    }

    public final GLState olmod = new GLState() {
	    public void apply(GOut g) {}
	    public void unapply(GOut g) {}
	    public void prep(Buffer buf) {
		for(Overlay ol : ols) {
		    if(ol.spr instanceof Overlay.SetupMod) {
			((Overlay.SetupMod)ol.spr).setupgob(buf);
		    }
		}
	    }
	};

    public class Save extends GLState.Abstract {
	public Matrix4f cam = new Matrix4f(), wxf = new Matrix4f(),
	    mv = new Matrix4f();
	public Projection proj = null;
	boolean debug = false;

	public void prep(Buffer buf) {
	    mv.load(cam.load(buf.get(PView.cam).fin(Matrix4f.id))).mul1(wxf.load(buf.get(PView.loc).fin(Matrix4f.id)));
	    Projection proj = buf.get(PView.proj);
	    PView.RenderState wnd = buf.get(PView.wnd);
	    Coord3f s = proj.toscreen(mv.mul4(Coord3f.o), wnd.sz());
	    Gob.this.sc = new Coord(s);
	    Gob.this.sczu = proj.toscreen(mv.mul4(Coord3f.zu), wnd.sz()).sub(s);
	    this.proj = proj;
	}
    }

    public final Save save = new Save();
    public class GobLocation extends GLState.Abstract {
	private Coord3f c = null;
	private double a = 0.0;
	private Matrix4f update = null;
	private final Location xl = new Location(Matrix4f.id, "gobx"), rot = new Location(Matrix4f.id, "gob");

	public void tick() {
	    try {
		Coord3f c = getc();
		c.y = -c.y;
		if((this.c == null) || !c.equals(this.c))
		    xl.update(Transform.makexlate(new Matrix4f(), this.c = c));
		if(this.a != Gob.this.a)
		    rot.update(Transform.makerot(new Matrix4f(), Coord3f.zu, (float)-(this.a = Gob.this.a)));
	    } catch(Loading l) {}
	}

	public void prep(Buffer buf) {
	    xl.prep(buf);
	    rot.prep(buf);
	}
    }
    public final GobLocation loc = new GobLocation();
}
