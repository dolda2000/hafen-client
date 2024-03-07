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
import java.util.function.*;
import haven.render.*;

public class Gob implements RenderTree.Node, Sprite.Owner, Skeleton.ModOwner, EquipTarget, RandomSource {
    public Coord2d rc;
    public double a;
    public boolean virtual = false;
    int clprio = 0;
    public long id;
    public boolean removed = false;
    public final Glob glob;
    Map<Class<? extends GAttrib>, GAttrib> attr = new HashMap<Class<? extends GAttrib>, GAttrib>();
    public final Collection<Overlay> ols = new ArrayList<Overlay>();
    public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    public int updateseq = 0;
    private final Collection<SetupMod> setupmods = new ArrayList<>();
    private final LinkedList<Runnable> deferred = new LinkedList<>();
    private Loader.Future<?> deferral = null;

    public static class Overlay implements RenderTree.Node {
	public final int id;
	public final Gob gob;
	public final Sprite.Mill<?> sm;
	public Sprite spr;
	public boolean delign = false, old = false;
	private Collection<RenderTree.Slot> slots = null;
	private boolean added = false;

	public Overlay(Gob gob, int id, Sprite.Mill<?> sm) {
	    this.gob = gob;
	    this.id = id;
	    this.sm = sm;
	    this.spr = null;
	}

	public Overlay(Gob gob, Sprite.Mill<?> sm) {
	    this(gob, -1, sm);
	}

	public Overlay(Gob gob, int id, Indir<Resource> res, Message sdt) {
	    this(gob, id, owner -> Sprite.create(owner, res.get(), sdt));
	}

	public Overlay(Gob gob, Sprite spr) {
	    this.gob = gob;
	    this.id = -1;
	    this.sm = null;
	    this.spr = spr;
	}

	private void init() {
	    if(spr == null) {
		spr = sm.create(gob);
		if(old)
		    spr.age();
		if(added && (spr instanceof SetupMod))
		    gob.setupmods.add((SetupMod)spr);
	    }
	    if(slots == null)
		RUtils.multiadd(gob.slots, this);
	}

	private void add0() {
	    if(added)
		throw(new IllegalStateException());
	    if(spr instanceof SetupMod)
		gob.setupmods.add((SetupMod)spr);
	    added = true;
	}

	private void remove0() {
	    if(!added)
		throw(new IllegalStateException());
	    if(slots != null) {
		RUtils.multirem(new ArrayList<>(slots));
		slots = null;
	    }
	    if(spr instanceof SetupMod)
		gob.setupmods.remove(spr);
	    added = false;
	}

	public void remove(boolean async) {
	    if(async) {
		gob.defer(() -> remove(false));
		return;
	    }
	    remove0();
	    gob.ols.remove(this);
	    removed();
	}

	public void remove() {
	    remove(true);
	}

	protected void removed() {
	}

	public boolean tick(double dt) {
	    if(spr == null)
		return(false);
	    return(spr.tick(dt));
	}

	public void added(RenderTree.Slot slot) {
	    slot.add(spr);
	    if(slots == null)
		slots = new ArrayList<>(1);
	    slots.add(slot);
	}

	public void removed(RenderTree.Slot slot) {
	    if(slots != null)
		slots.remove(slot);
	}
    }

    public static interface SetupMod {
	public default Pipe.Op gobstate() {return(null);}
	public default Pipe.Op placestate() {return(null);}
    }

    public static interface Placer {
	/* XXX: *Quite* arguably, the distinction between getc and
	 * getr should be abolished and a single transform matrix
	 * should be used instead, but that requires first abolishing
	 * the distinction between the gob/gobx location IDs. */
	public Coord3f getc(Coord2d rc, double ra);
	public Matrix4f getr(Coord2d rc, double ra);
    }

    public static interface Placing {
	public Placer placer();
    }

    public static class DefaultPlace implements Placer {
	public final MCache map;
	public final MCache.SurfaceID surf;

	public DefaultPlace(MCache map, MCache.SurfaceID surf) {
	    this.map = map;
	    this.surf = surf;
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    return(map.getzp(surf, rc));
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    return(Transform.makerot(new Matrix4f(), Coord3f.zu, -(float)ra));
	}
    }

    public static class InclinePlace extends DefaultPlace {
	public InclinePlace(MCache map, MCache.SurfaceID surf) {
	    super(map, surf);
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    Matrix4f ret = super.getr(rc, ra);
	    Coord3f norm = map.getnorm(surf, rc);
	    norm.y = -norm.y;
	    Coord3f rot = Coord3f.zu.cmul(norm);
	    float sin = rot.abs();
	    if(sin > 0) {
		Matrix4f incl = Transform.makerot(new Matrix4f(), rot.mul(1 / sin), sin, (float)Math.sqrt(1 - (sin * sin)));
		ret = incl.mul(ret);
	    }
	    return(ret);
	}
    }

    public static class BasePlace extends DefaultPlace {
	public final Coord2d[][] obst;
	private Coord2d cc;
	private double ca;
	private int seq = -1;
	private float z;

	public BasePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] obst) {
	    super(map, surf);
	    this.obst = obst;
	}

	public BasePlace(MCache map, MCache.SurfaceID surf, Resource res, String id) {
	    this(map, surf, res.flayer(Resource.obst, id).p);
	}

	public BasePlace(MCache map, MCache.SurfaceID surf, Resource res) {
	    this(map, surf, res, "");
	}

	private float getz(Coord2d rc, double ra) {
	    Coord2d[][] no = this.obst, ro = new Coord2d[no.length][];
	    {
		double s = Math.sin(ra), c = Math.cos(ra);
		for(int i = 0; i < no.length; i++) {
		    ro[i] = new Coord2d[no[i].length];
		    for(int o = 0; o < ro[i].length; o++)
			ro[i][o] = Coord2d.of((no[i][o].x * c) - (no[i][o].y * s), (no[i][o].y * c) + (no[i][o].x * s)).add(rc);
		}
	    }
	    float ret = Float.NaN;
	    for(int i = 0; i < no.length; i++) {
		for(int o = 0; o < ro[i].length; o++) {
		    Coord2d a = ro[i][o], b = ro[i][(o + 1) % ro[i].length];
		    for(Coord2d c : new Line2d.GridIsect(a, b, MCache.tilesz, false)) {
			double z = map.getz(surf, c);
			if(Float.isNaN(ret) || (z < ret))
			    ret = (float)z;
		    }
		}
	    }
	    return(ret);
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    int mseq = map.chseq;
	    if((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
		this.z = getz(rc, ra);
		this.seq = mseq;
		this.cc = rc;
		this.ca = ra;
	    }
	    return(Coord3f.of((float)rc.x, (float)rc.y, this.z));
	}
    }

    public static class LinePlace extends DefaultPlace {
	public final double max, min;
	public final Coord2d k;
	private Coord3f c;
	private Matrix4f r = Matrix4f.id;
	private int seq = -1;
	private Coord2d cc;
	private double ca;

	public LinePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] points, Coord2d k) {
	    super(map, surf);
	    Line2d l = Line2d.from(Coord2d.z, k);
	    double max = 0, min = 0;
	    for(int i = 0; i < points.length; i++) {
		for(int o = 0; o < points[i].length; o++) {
		    int p = (o + 1) % points[i].length;
		    Line2d edge = Line2d.twixt(points[i][o], points[i][p]);
		    Coord2d t = l.cross(edge);
		    if((t.y >= 0) && (t.y <= 1)) {
			max = Math.max(t.x, max);
			min = Math.min(t.x, min);
		    }
		}
	    }
	    if((max == 0) || (min == 0))
		throw(new RuntimeException("illegal bounds for LinePlace"));
	    this.k = k;
	    this.max = max;
	    this.min = min;
	}

	public LinePlace(MCache map, MCache.SurfaceID surf, Resource res, String id, Coord2d k) {
	    this(map, surf, res.flayer(Resource.obst, id).p, k);
	}

	public LinePlace(MCache map, MCache.SurfaceID surf, Resource res, Coord2d k) {
	    this(map, surf, res, "", k);
	}

	private void recalc(Coord2d rc, double ra) {
	    Coord2d rk = k.rot(ra);
	    double maxz = map.getz(surf, rc.add(rk.mul(max)));
	    double minz = map.getz(surf, rc.add(rk.mul(min)));
	    Coord3f rax = Coord3f.of((float)-rk.y, (float)-rk.x, 0);
	    float dz = (float)(maxz - minz);
	    float dx = (float)(max - min);
	    float hyp = (float)Math.sqrt((dx * dx) + (dz * dz));
	    float sin = dz / hyp, cos = dx / hyp;
	    c = Coord3f.of((float)rc.x, (float)rc.y, (float)minz + (dz * (float)(-min / (max - min))));
	    r = Transform.makerot(new Matrix4f(), rax, sin, cos).mul(super.getr(rc, ra));
	}

	private void check(Coord2d rc, double ra) {
	    int mseq = map.chseq;
	    if((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
		recalc(rc, ra);
		this.seq = mseq;
		this.cc = rc;
		this.ca = ra;
	    }
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    check(rc, ra);
	    return(c);
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    check(rc, ra);
	    return(r);
	}
    }

    public static class PlanePlace extends DefaultPlace {
	public final Coord2d[] points;
	private Coord3f c;
	private Matrix4f r = Matrix4f.id;
	private int seq = -1;
	private Coord2d cc;
	private double ca;

	public static Coord2d[] flatten(Coord2d[][] points) {
	    int n = 0;
	    for(int i = 0; i < points.length; i++)
		n += points[i].length;
	    Coord2d[] ret = new Coord2d[n];
	    for(int i = 0, o = 0; i < points.length; o += points[i++].length)
		System.arraycopy(points[i], 0, ret, o, points[i].length);
	    return(ret);
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Coord2d[] points) {
	    super(map, surf);
	    this.points = points;
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] points) {
	    this(map, surf, flatten(points));
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Resource res, String id) {
	    this(map, surf, res.flayer(Resource.obst, id).p);
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Resource res) {
	    this(map, surf, res, "");
	}

	private void recalc(Coord2d rc, double ra) {
	    double s = Math.sin(ra), c = Math.cos(ra);
	    Coord3f[] pp = new Coord3f[points.length];
	    for(int i = 0; i < pp.length; i++) {
		Coord2d rv = Coord2d.of((points[i].x * c) - (points[i].y * s), (points[i].y * c) + (points[i].x * s));
		pp[i] = map.getzp(surf, rv.add(rc));
	    }
	    int I = 0, O = 1, U = 2;
	    Coord3f mn = Coord3f.zu;
	    double ma = 0;
	    for(int i = 0; i < pp.length - 2; i++) {
		for(int o = i + 1; o < pp.length - 1; o++) {
		    plane: for(int u = o + 1; u < pp.length; u++) {
			Coord3f n = pp[o].sub(pp[i]).cmul(pp[u].sub(pp[i])).norm();
			for(int p = 0; p < pp.length; p++) {
			    if((p == i) || (p == o) || (p == u))
				continue;
			    float pz = (((n.x * (pp[i].x - pp[p].x)) + (n.y * (pp[i].y - pp[p].y))) / n.z) + pp[i].z;
			    if(pz < pp[p].z - 0.01)
				continue plane;
			}
			double a = n.cmul(Coord3f.zu).abs();
			if(a > ma) {
			    mn = n;
			    ma = a;
			    I = i; O = o; U = u;
			}
		    }
		}
	    }
	    this.c = Coord3f.of((float)rc.x, (float)rc.y, (((mn.x * (pp[I].x - (float)rc.x)) + (mn.y * (pp[I].y - (float)rc.y))) / mn.z) + pp[I].z);
	    this.r = Transform.makerot(new Matrix4f(), Coord3f.zu, -(float)ra);
	    mn.y = -mn.y;
	    Coord3f rot = Coord3f.zu.cmul(mn);
	    float sin = rot.abs();
	    if(sin > 0) {
		Matrix4f incl = Transform.makerot(new Matrix4f(), rot.mul(1 / sin), sin, (float)Math.sqrt(1 - (sin * sin)));
		this.r = incl.mul(this.r);
	    }
	}

	private void check(Coord2d rc, double ra) {
	    int mseq = map.chseq;
	    if((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
		recalc(rc, ra);
		this.seq = mseq;
		this.cc = rc;
		this.ca = ra;
	    }
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    check(rc, ra);
	    return(this.c);
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    return(this.r);
	}
    }

    public Gob(Glob glob, Coord2d c, long id) {
	this.glob = glob;
	this.rc = c;
	this.id = id;
	if(id < 0)
	    virtual = true;
    }

    public Gob(Glob glob, Coord2d c) {
	this(glob, c, -1);
    }

    public void ctick(double dt) {
	for(GAttrib a : attr.values())
	    a.ctick(dt);
	for(Iterator<Overlay> i = ols.iterator(); i.hasNext();) {
	    Overlay ol = i.next();
	    if(ol.slots == null) {
		try {
		    ol.init();
		} catch(Loading e) {}
	    } else {
		boolean done = ol.tick(dt);
		if((!ol.delign || (ol.spr instanceof Sprite.CDel)) && done) {
		    ol.remove0();
		    i.remove();
		}
	    }
	}
	updstate();
	if(virtual && ols.isEmpty() && (getattr(Drawable.class) == null))
	    glob.oc.remove(this);
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

    void removed() {
	removed = true;
    }

    private void deferred() {
	while(true) {
	    Runnable task;
	    synchronized(deferred) {
		task = deferred.peek();
		if(task == null) {
		    deferral = null;
		    return;
		}
	    }
	    synchronized(this) {
		if(!removed)
		    task.run();
	    }
	    if(task instanceof Disposable)
		((Disposable)task).dispose();
	    synchronized(deferred) {
		if(deferred.poll() != task)
		    throw(new RuntimeException());
	    }
	}
    }

    public void defer(Runnable task) {
	synchronized(deferred) {
	    deferred.add(task);
	    if(deferral == null)
		deferral = glob.loader.defer(this::deferred, null);
	}
    }

    public void addol(Overlay ol, boolean async) {
	if(async) {
	    defer(() -> addol(ol, false));
	    return;
	}
	ol.init();
	ol.add0();
	ols.add(ol);
    }
    public void addol(Overlay ol) {
	addol(ol, true);
    }
    public void addol(Sprite ol) {
	addol(new Overlay(this, ol));
    }
    public void addol(Indir<Resource> res, Message sdt) {
	addol(new Overlay(this, -1, res, sdt));
    }
    public void addol(Sprite.Mill<?> ol) {
	addol(new Overlay(this, ol));
    }
    public <S extends Sprite> S addolsync(Sprite.Mill<S> sm) {
	Overlay ol = new Overlay(this, sm);
	addol(ol, false);
	@SuppressWarnings("unchecked") S ret = (S)ol.spr;
	return(ret);
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
    }

    public void move(Coord2d c, double a) {
	Moving m = getattr(Moving.class);
	if(m != null)
	    m.move(c);
	this.rc = c;
	this.a = a;
    }

    public Placer placer() {
	Drawable d = getattr(Drawable.class);
	if(d != null) {
	    Placer ret = d.placer();
	    if(ret != null)
		return(ret);
	}
	return(glob.map.mapplace);
    }

    public Coord3f getc() {
	Moving m = getattr(Moving.class);
	Coord3f ret = (m != null) ? m.getc() : getrc();
	DrawOffset df = getattr(DrawOffset.class);
	if(df != null)
	    ret = ret.add(df.off);
	return(ret);
    }

    public Coord3f getrc() {
	return(placer().getc(rc, a));
    }

    protected Pipe.Op getmapstate(Coord3f pc) {
	Tiler tile = glob.map.tiler(glob.map.gettile(new Coord2d(pc).floor(MCache.tilesz)));
	return(tile.drawstate(glob, pc));
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
	GAttrib prev = attr.remove(ac);
	if(prev != null) {
	    if((prev instanceof RenderTree.Node) && (prev.slots != null))
		RUtils.multirem(new ArrayList<>(prev.slots));
	    if(prev instanceof SetupMod)
		setupmods.remove(prev);
	}
	if(a != null) {
	    if(a instanceof RenderTree.Node) {
		try {
		    RUtils.multiadd(this.slots, (RenderTree.Node)a);
		} catch(Loading l) {
		    if(prev instanceof RenderTree.Node) {
			RUtils.multiadd(this.slots, (RenderTree.Node)prev);
			attr.put(ac, prev);
		    }
		    if(prev instanceof SetupMod)
			setupmods.add((SetupMod)prev);
		    throw(l);
		}
	    }
	    if(a instanceof SetupMod)
		setupmods.add((SetupMod)a);
	    attr.put(ac, a);
	}
	if(prev != null)
	    prev.dispose();
    }

    public void setattr(GAttrib a) {
	setattr(attrclass(a.getClass()), a);
    }

    public void delattr(Class<? extends GAttrib> c) {
	setattr(attrclass(c), null);
    }

    public Supplier<? extends Pipe.Op> eqpoint(String nm, Message dat) {
	for(GAttrib attr : this.attr.values()) {
	    if(attr instanceof EquipTarget) {
		Supplier<? extends Pipe.Op> ret = ((EquipTarget)attr).eqpoint(nm, dat);
		if(ret != null)
		    return(ret);
	    }
	}
	return(null);
    }

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

	public String toString() {
	    return(String.format("#<gob-click %s>", gob));
	}
    }

    protected void obstate(Pipe buf) {
    }

    private class GobState implements Pipe.Op {
	final Pipe.Op mods;

	private GobState() {
	    if(setupmods.isEmpty()) {
		this.mods = null;
	    } else {
		Pipe.Op[] mods = new Pipe.Op[setupmods.size()];
		int n = 0;
		for(SetupMod mod : setupmods) {
		    if((mods[n] = mod.gobstate()) != null)
			n++;
		}
		this.mods = (n > 0) ? Pipe.Op.compose(mods) : null;
	    }
	}

	public void apply(Pipe buf) {
	    if(!virtual)
		buf.prep(new GobClick(Gob.this));
	    buf.prep(new TickList.Monitor(Gob.this));
	    obstate(buf);
	    if(mods != null)
		buf.prep(mods);
	}

	public boolean equals(GobState that) {
	    return(Utils.eq(this.mods, that.mods));
	}
	public boolean equals(Object o) {
	    return((o instanceof GobState) && equals((GobState)o));
	}
    }
    private GobState curstate = null;
    private GobState curstate() {
	if(curstate == null)
	    curstate = new GobState();
	return(curstate);
    }

    private void updstate() {
	GobState nst;
	try {
	    nst = new GobState();
	} catch(Loading l) {
	    return;
	}
	if(!Utils.eq(nst, curstate)) {
	    try {
		for(RenderTree.Slot slot : slots)
		    slot.ostate(nst);
		this.curstate = nst;
	    } catch(Loading l) {
	    }
	}
    }

    public void added(RenderTree.Slot slot) {
	slot.ostate(curstate());
	for(Overlay ol : ols) {
	    if(ol.slots != null)
		slot.add(ol);
	}
	for(GAttrib a : attr.values()) {
	    if(a instanceof RenderTree.Node)
		slot.add((RenderTree.Node)a);
	}
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }

    private Waitable.Queue updwait = null;
    void updated() {
	synchronized(this) {
	    updateseq++;
	    if(updwait != null)
		updwait.wnotify();
	}
    }

    public void updwait(Runnable callback, Consumer<Waitable.Waiting> reg) {
	/* Caller should probably synchronize on this already for a
	 * call like this to even be meaningful, but just in case. */
	synchronized(this) {
	    if(updwait == null)
		updwait = new Waitable.Queue();
	    reg.accept(updwait.add(callback));
	}
    }

    public static class DataLoading extends Loading {
	public final transient Gob gob;
	public final int updseq;

	/* It would be assumed that the caller has synchronized on gob
	 * while creating this exception. */
	public DataLoading(Gob gob, String message) {
	    super(message);
	    this.gob = gob;
	    this.updseq = gob.updateseq;
	}

	public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
	    synchronized(gob) {
		if(gob.updateseq != this.updseq) {
		    reg.accept(Waitable.Waiting.dummy);
		    callback.run();
		} else {
		    gob.updwait(callback, reg);
		}
	    }
	}
    }

    public Random mkrandoom() {
	return(Utils.mkrandoom(id));
    }

    @Deprecated
    public Resource getres() {
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    return(d.getres());
	return(null);
    }

    private static final ClassResolver<Gob> ctxr = new ClassResolver<Gob>()
	.add(Gob.class, g -> g)
	.add(Glob.class, g -> g.glob)
	.add(Session.class, g -> g.glob.sess);
    public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

    /* Because generic functions are too nice a thing for Java. */
    public double getv() {
	Moving m = getattr(Moving.class);
	if(m == null)
	    return(0);
	return(m.getv());
    }

    public Collection<Location.Chain> getloc() {
	Collection<Location.Chain> ret = new ArrayList<>(slots.size());
	for(RenderTree.Slot slot : slots) {
	    Location.Chain loc = slot.state().get(Homo3D.loc);
	    if(loc != null)
		ret.add(loc);
	}
	return(ret);
    }

    public class Placed implements RenderTree.Node, TickList.Ticking, TickList.TickNode {
	/* XXX: Using a COW list is far from an ideal solution. It
	 * should work for the specific case of flavobjs (which are
	 * added asynchronously in a way that makes it difficult to
	 * lock on each individually), but it's certainly not a
	 * general solution, and it would be nice with something that
	 * is in fact more general. */
	private final Collection<RenderTree.Slot> slots = new java.util.concurrent.CopyOnWriteArrayList<>();
	private Placement cur;

	private Placed() {}

	private class Placement implements Pipe.Op {
	    final Pipe.Op flw, tilestate, mods;
	    final Coord3f oc, rc;
	    final Matrix4f rot;

	    Placement() {
		try {
		    Following flw = Gob.this.getattr(Following.class);
		    Pipe.Op flwxf = (flw == null) ? null : flw.xf();
		    Pipe.Op tilestate = null;
		    if(flwxf == null) {
			Coord3f oc = Gob.this.getc();
			Coord3f rc = new Coord3f(oc);
			rc.y = -rc.y;
			this.flw = null;
			this.oc = oc;
			this.rc = rc;
			this.rot = Gob.this.placer().getr(Coord2d.of(oc), Gob.this.a);
			tilestate = Gob.this.getmapstate(oc);
		    } else {
			this.flw = flwxf;
			this.oc = this.rc = null;
			this.rot = null;
		    }
		    this.tilestate = tilestate;
		    if(setupmods.isEmpty()) {
			this.mods = null;
		    } else {
			Pipe.Op[] mods = new Pipe.Op[setupmods.size()];
			int n = 0;
			for(SetupMod mod : setupmods) {
			    if((mods[n] = mod.placestate()) != null)
				n++;
			}
			this.mods = (n > 0) ? Pipe.Op.compose(mods) : null;
		    }
		} catch(Loading bl) {
		    throw(new Loading(bl) {
			    public String getMessage() {return(bl.getMessage());}

			    public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
				Waitable.or(callback, reg, bl, Gob.this::updwait);
			    }
			});
		}
	    }

	    public boolean equals(Placement that) {
		if(this.flw != null) {
		    if(!Utils.eq(this.flw, that.flw))
			return(false);
		} else {
		    if(!(Utils.eq(this.oc, that.oc) && Utils.eq(this.rot, that.rot)))
			return(false);
		}
		if(!Utils.eq(this.tilestate, that.tilestate))
		    return(false);
		if(!Utils.eq(this.mods, that.mods))
		    return(false);
		return(true);
	    }

	    public boolean equals(Object o) {
		return((o instanceof Placement) && equals((Placement)o));
	    }

	    Pipe.Op gndst = null;
	    public void apply(Pipe buf) {
		if(this.flw != null) {
		    this.flw.apply(buf);
		} else {
		    if(gndst == null)
			gndst = Pipe.Op.compose(new Location(Transform.makexlate(new Matrix4f(), this.rc), "gobx"),
						new Location(rot, "gob"));
		    gndst.apply(buf);
		}
		if(tilestate != null)
		    tilestate.apply(buf);
		if(mods != null)
		    mods.apply(buf);
	    }
	}

	public Pipe.Op placement() {
	    return(new Placement());
	}

	public void autotick(double dt) {
	    synchronized(Gob.this) {
		Placement np;
		try {
		    np = new Placement();
		} catch(Loading l) {
		    return;
		}
		if(!Utils.eq(this.cur, np))
		    update(np);
	    }
	}

	private void update(Placement np) {
	    for(RenderTree.Slot slot : slots)
		slot.ostate(np);
	    this.cur = np;
	}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(curplace());
	    slot.add(Gob.this);
	    slots.add(slot);
	}

	public void removed(RenderTree.Slot slot) {
	    slots.remove(slot);
	}

	public Pipe.Op curplace() {
	    if(cur == null)
		cur = new Placement();
	    return(cur);
	}

	public Coord3f getc() {
	    return((this.cur != null) ? this.cur.oc : null);
	}

	public TickList.Ticking ticker() {return(this);}
    }
    public final Placed placed = new Placed();

    public String toString() {
	return(String.format("#<ob %d %s>", id, getattr(Drawable.class)));
    }
}
