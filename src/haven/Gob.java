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
import haven.render.*;

public class Gob implements RenderTree.Node, Sprite.Owner, Skeleton.ModOwner {
    public Coord2d rc;
    public double a;
    public boolean virtual = false;
    int clprio = 0;
    public long id;
    public final Glob glob;
    Map<Class<? extends GAttrib>, GAttrib> attr = new HashMap<Class<? extends GAttrib>, GAttrib>();
    private Collection<Overlay> ols = new ArrayList<Overlay>();
    public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    private final Collection<ResAttr.Cell<?>> rdata = new LinkedList<ResAttr.Cell<?>>();
    private final Collection<ResAttr.Load> lrdata = new LinkedList<ResAttr.Load>();

    public static class Overlay {
	public final Gob gob;
	public final Indir<Resource> res;
	public MessageBuf sdt;
	public Sprite spr;
	public int id;
	public boolean delign = false;

	public Overlay(Gob gob, int id, Indir<Resource> res, Message sdt) {
	    this.gob = gob;
	    this.id = id;
	    this.res = res;
	    this.sdt = new MessageBuf(sdt);
	    this.spr = null;
	}

	public Overlay(Sprite spr) {
	    this.gob = null;
	    this.id = -1;
	    this.res = null;
	    this.sdt = null;
	    this.spr = spr;
	}

	public static interface SetupMod {
	    /* XXXRENDER
	    public void setupgob(GLState.Buffer buf);
	    public void setupmain(RenderList rl);
	    */
	}

	/* XXXRENDER
	public void draw(GOut g) {}
	public boolean setup(RenderList rl) {
	    if(spr != null)
		rl.add(spr, null);
	    return(false);
	}

	public Object staticp() {
	    return((spr == null)?null:spr.staticp());
	}
	*/

	public void remove() {
	    gob.ols.remove(this);
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

    /* XXXRENDER: Remove */
    public static class Static {}
    public static class SemiStatic {}

    public Gob(Glob glob, Coord2d c, long id) {
	this.glob = glob;
	this.rc = c;
	this.id = id;
	if(id < 0)
	    virtual = true;
	placed.tick();
    }

    public Gob(Glob glob, Coord2d c) {
	this(glob, c, -1);
    }

    public void ctick(double dt) {
	for(GAttrib a : attr.values())
	    a.ctick(dt);
	loadrattr();
	placed.tick();
	for(Iterator<Overlay> i = ols.iterator(); i.hasNext();) {
	    Overlay ol = i.next();
	    if(ol.spr == null) {
		try {
		    ol.spr = Sprite.create(this, ol.res.get(), ol.sdt.clone());
		} catch(Loading e) {}
	    } else {
		boolean done = ol.spr.tick(dt);
		if((!ol.delign || (ol.spr instanceof Sprite.CDel)) && done)
		    i.remove();
	    }
	}
	if(virtual && ols.isEmpty())
	    glob.oc.remove(id);
    }

    public void gtick(Render g) {
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    d.gtick(g);
	for(Overlay ol : ols) {
	    if(ol.spr != null)
		ol.spr.gtick(g);
	}
    }

    /* Intended for local code. Server changes are handled via OCache. */
    public void addol(Overlay ol) {
	ols.add(ol);
    }
    public void addol(Sprite ol) {
	addol(new Overlay(ol));
    }
    public void addol(Indir<Resource> res, Message sdt) {
	addol(new Overlay(this, -1, res, sdt));
    }

    public Overlay findol(int id) {
	for(Overlay ol : ols) {
	    if(ol.id == id)
		return(ol);
	}
	return(null);
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

    public <C extends GAttrib> C getattr(Class<C> c) {
	GAttrib attr = this.attr.get(attrclass(c));
	if(!c.isInstance(attr))
	    return(null);
	return(c.cast(attr));
    }

    private void setattr(Class<? extends GAttrib> ac, GAttrib a) {
	GAttrib prev = (a != null) ? attr.put(ac, a) : attr.remove(ac);
	if(prev != null) {
	    if(ac == Drawable.class)
		((Drawable)prev).drawremove();
	    prev.dispose();
	}
	if(a != null) {
	    if(ac == Drawable.class)
		((Drawable)a).drawadd(slots);
	}
    }

    public void setattr(GAttrib a) {
	setattr(attrclass(a.getClass()), a);
    }

    public void delattr(Class<? extends GAttrib> c) {
	setattr(attrclass(c), null);
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

    public static class GobClick extends Clickable {
	public final Gob gob;

	public GobClick(Gob gob) {
	    this.gob = gob;
	}

	public Object[] clickargs(ClickData cd) {
	    Object[] ret = {0, (int)gob.id, gob.rc.floor(OCache.posres), 0, -1};
	    for(Object node : cd.array()) {
		if(node instanceof Gob.Overlay) {
		    ret[0] = 1;
		    ret[3] = ((Gob.Overlay)node).id;
		}
		if(node instanceof FastMesh.ResourceMesh)
		    ret[4] = ((FastMesh.ResourceMesh)node).id;
	    }
	    return(ret);
	}
    }

    public void added(RenderTree.Slot slot) {
	if(!virtual)
	    slot.ostate(new GobClick(this));
	synchronized(this) {
	    slots.add(slot);
	}
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    d.drawadd(Collections.singletonList(slot));
    }

    public void removed(RenderTree.Slot slot) {
	synchronized(this) {
	    slots.remove(slot);
	}
    }

    /* XXXRENDER
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
    */

    void changed() {
	// seq = null; XXXRENDER
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

    /* XXXRENDER
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
    */

    /*
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
    */

    public class Placed implements RenderTree.Node {
	private final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
	private Coord3f c = null;
	private double a = Double.NaN;
	private Location xl, rot;

	private Placed() {}

	public void tick() {
	    boolean upd = false;
	    try {
		Coord3f c = getc();
		c.y = -c.y;
		if(!Utils.eq(this.c, c)) {
		    xl = new Location(Transform.makexlate(new Matrix4f(), this.c = c), "gobx");
		    upd = true;
		}
		if(this.a != Gob.this.a) {
		    rot = new Location(Transform.makerot(new Matrix4f(), Coord3f.zu, (float)-(this.a = Gob.this.a)), "gob");
		    upd = true;
		}
	    } catch(Loading l) {}
	    if(upd)
		update();
	}

	private Pipe.Op state() {
	    return(Pipe.Op.compose(xl, rot));
	}

	private void update() {
	    Pipe.Op state = state();
	    for(RenderTree.Slot slot : slots)
		slot.ostate(state);
	}

	public void added(RenderTree.Slot slot) {
	    /* XXX: xl and rot have not been set yet, so state() is
	     * undefined and will incur an unnecessary def-mask update
	     * on the first tick. */
	    slot.ostate(state());
	    slot.add(Gob.this);
	    slots.add(slot);
	}

	public void removed(RenderTree.Slot slot) {
	    slots.remove(slot);
	}

	public Coord3f getc() {
	    return(this.c);
	}
    }
    public final Placed placed = new Placed();
}
