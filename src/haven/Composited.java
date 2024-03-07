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
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;

public class Composited implements RenderTree.Node, EquipTarget {
    public final Skeleton skel;
    public final Pose pose;
    public Collection<Model> mod = new ArrayList<Model>();
    public Collection<Equ> equ = new ArrayList<Equ>();
    public Poses poses = new Poses();
    public List<MD> cmod = new LinkedList<MD>();
    public List<ED> cequ = new LinkedList<ED>();
    public Sprite.Owner eqowner = null;
    private final Collection<RenderTree.Slot> slots = new ArrayList<>(1);

    public class Poses {
	public final PoseMod[] mods;
	Pose old;
	float ipold = 0.0f, ipol = 0.0f;
	public float limit = -1.0f;
	public boolean stat, ldone;
	private Random srnd = new Random();
	private float rsmod = (srnd.nextFloat() * 0.1f) + 0.95f;

	public Poses() {
	    this.mods = new PoseMod[0];
	}

	public Poses(List<? extends PoseMod> mods) {
	    this.mods = mods.toArray(new PoseMod[0]);
	    stat = true;
	    for(PoseMod mod : this.mods) {
		if(!mod.stat()) {
		    stat = false;
		    break;
		}
	    }
	}

	private void rebuild() {
	    pose.reset();
	    for(PoseMod m : mods)
		m.apply(pose);
	    if(ipold > 0.0f)
		pose.blend(old, ipold);
	    pose.gbuild();
	}

	public void set(float ipol) {
	    if((this.ipol = ipol) > 0) {
		this.old = skel.new Pose(pose);
		this.ipold = 1.0f;
	    }
	    Composited.this.poses = this;
	    rebuild();
	}

	public void tick(float dt) {
	    rsmod = Utils.clip(rsmod + (srnd.nextFloat() * 0.005f) - 0.0025f, 0.90f, 1.10f);
	    dt *= rsmod;
	    boolean build = false;
	    if(limit >= 0) {
		if((limit -= dt) < 0)
		    ldone = true;
	    }
	    boolean done = ldone;
	    for(PoseMod m : mods) {
		m.tick(dt);
		if(!m.done())
		    done = false;
	    }
	    if(!stat)
		build = true;
	    if(ipold > 0.0f) {
		if((ipold -= (dt / ipol)) < 0.0f) {
		    ipold = 0.0f;
		    old = null;
		}
		build = true;
	    }
	    if(build)
		rebuild();
	    if(done)
		done();
	}

	protected void done() {}
    }

    public Composited(Skeleton skel) {
	this.skel = skel;
	this.pose = skel.new Pose(skel.bindpose);
    }

    public static class ModOrder extends Rendered.Order<ModOrder> {
	public final int z1, z2;

	public ModOrder(int z1, int z2) {
	    this.z1 = z1;
	    this.z2 = z2;
	}

	public int mainorder() {
	    return(1);
	}

	private static final Comparator<ModOrder> cmp = new Comparator<ModOrder>() {
	    public int compare(ModOrder a, ModOrder b) {
		if(a.z1 != b.z1)
		    return(a.z1 - b.z1);
		return(a.z2 - b.z2);
	    }
	};

	public Comparator<ModOrder> comparator() {return(cmp);}
    };

    public class Model implements RenderTree.Node, TickList.TickNode, TickList.Ticking {
	public final FastMesh m;
	public final PoseMorph morph;
	public final int id;
	private final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
	private int z = 0, lz = 0;

	public class Layer implements RenderTree.Node {
	    public final Material mat;
	    public final ModOrder order;

	    private Layer(Material mat, int z1, int z2) {
		this.mat = mat;
		this.order = new ModOrder(z1, z2);
	    }

	    public void added(RenderTree.Slot slot) {
		slot.ostate(Pipe.Op.compose(mat, order, (order.z2 == 0) ? null : (p -> p.put(Clickable.slot, null))));
		slot.lockstate();
		slot.add(m);
	    }
	}
	public final List<Layer> lay = new ArrayList<Layer>();

	private Model(FastMesh m, int id) {
	    this.m = m;
	    this.morph = new PoseMorph(pose, m);
	    this.id = id;
	}
	
	private void addlay(Material mat) {
	    lay.add(new Layer(mat, z, lz++));
	}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(morph.state());
	    for(Layer lay : this.lay)
		slot.add(lay);
	    slots.add(slot);
	}

	public void removed(RenderTree.Slot slot) {
	    slots.remove(slot);
	}

	public TickList.Ticking ticker() {return(this);}
	public void autotick(double dt) {
	    Pipe.Op nst = morph.state();
	    for(RenderTree.Slot slot : slots)
		slot.ostate(nst);
	}
    }

    private static final OwnerContext.ClassResolver<SpriteEqu> eqctxr = new OwnerContext.ClassResolver<SpriteEqu>()
	.add(SpriteEqu.class, eq -> eq)
	.add(Composited.class, eq -> eq.comp());
    public class SpriteEqu extends Equ<Sprite> implements Sprite.Owner, RandomSource {
	private SpriteEqu(ED ed) {
	    super(Sprite.create(eqowner, ed.res.res.get(), ed.res.sdt.clone()), ed);
	}

	public void tick(double dt) {
	    super.tick(dt);
	    r.tick(dt);
	}

	public void gtick(Render g) {
	    super.gtick(g);
	    r.gtick(g);
	}

	public <T> T context(Class<T> cl) {
	    return(OwnerContext.orparent(cl, eqctxr.context(cl, this, false), eqowner));
	}

	@Deprecated
	public Resource getres() {
	    return(r.res);
	}

	public Random mkrandoom() {
	    return((eqowner != null) ? eqowner.mkrandoom() : new Random());
	}

	public Composited comp() {
	    return(Composited.this);
	}
    }

    public class LightEqu extends Equ<Light> {
	private LightEqu(ED ed) {
	    super(ed.res.res.get().flayer(Light.Res.class).make(), ed);
	}
    }

    public abstract class Equ<R extends RenderTree.Node> extends RUtils.StateNode<R> {
	private final Supplier<Pipe.Op> et;
	public final ED desc;
	public final int id;
	
	private Equ(R r, ED ed) {
	    super(r);
	    this.desc = ed.clone();
	    this.id = desc.id;
	    Supplier<Pipe.Op> bt = null;
	    if(bt == null) {
		Skeleton.BoneOffset bo = ed.res.res.get().layer(Skeleton.BoneOffset.class, ed.at);
		if(bo != null)
		    bt = bo.from(Composited.this);
	    }
	    if((bt == null) && (skel instanceof Skeleton.ResourceSkeleton)) {
		Skeleton.BoneOffset bo = ((Skeleton.ResourceSkeleton)skel).res.layer(Skeleton.BoneOffset.class, ed.at);
		if(bo != null)
		    bt = bo.from(Composited.this);
	    }
	    if(bt == null) {
		Skeleton.Bone bone = skel.bones.get(ed.at);
		if(bone != null)
		    bt = pose.bonetrans(bone.idx);
	    }
	    if((bt == null) && !ed.at.equals(""))
		throw(new RuntimeException("Transformation " + ed.at + " for equipment " + ed.res + " on skeleton " + skel + " could not be resolved"));
	    Supplier<Pipe.Op> dbt = (bt != null) ? bt : () -> null;
	    if((ed.off.x != 0.0f) || (ed.off.y != 0.0f) || (ed.off.z != 0.0f))
		this.et = () -> Pipe.Op.compose(dbt.get(), Location.xlate(ed.off));
	    else
		this.et = dbt;
	}

	public void tick(double dt) {
	    update();
	}
	public void gtick(Render g) {}

	protected Pipe.Op state() {return(et.get());}

	public String toString() {
	    return(String.format("#<equ %s>", r));
	}
    }

    public static class MD implements Cloneable {
	public Indir<Resource> mod;
	public List<ResData> tex;
	public int id = -1;
	private Model real;
	
	public MD(Indir<Resource> mod, List<ResData> tex) {
	    this.mod = mod;
	    this.tex = tex;
	}
	
	public boolean equals(Object o) {
	    if(!(o instanceof MD))
		return(false);
	    MD m = (MD)o;
	    return(mod.equals(m.mod) && tex.equals(m.tex));
	}
	
	public MD clone() {
	    try {
		MD ret = (MD)super.clone();
		ret.tex = new ArrayList<ResData>(tex);
		ret.real = null;
		return(ret);
	    } catch(CloneNotSupportedException e) {
		/* This is ridiculous. */
		throw(new RuntimeException(e));
	    }
	}
	
	public String toString() {
	    return(mod + "+" + tex);
	}
    }
    
    public static class ED implements Cloneable {
	public int t, id = -1;
	public String at;
	public ResData res;
	public Coord3f off;
	private Equ real;
	
	public ED(int t, String at, ResData res, Coord3f off) {
	    this.t = t;
	    this.at = at;
	    this.res = res;
	    this.off = off;
	}
	
	public boolean equals(Object o) {
	    if(!(o instanceof ED))
		return(false);
	    ED e = (ED)o;
	    return((t == e.t) && at.equals(e.at) && res.equals(e.res) && off.equals(e.off));
	}

	public boolean equals2(Object o) {
	    if(!(o instanceof ED))
		return(false);
	    ED e = (ED)o;
	    return((t == e.t) && at.equals(e.at) && res.res.equals(e.res.res) && off.equals(e.off));
	}

	public ED clone() {
	    try {
		ED ret = (ED)super.clone();
		ret.res = res.clone();
		ret.real = null;
		return(ret);
	    } catch(CloneNotSupportedException e) {
		/* This is ridiculous. */
		throw(new RuntimeException(e));
	    }
	}

	public String toString() {
	    return(String.format("<ED: %d \"%s\" %s(%s) %s>", t, at, res.res, res.sdt, off));
	}
    }
    
    public static class Desc implements Cloneable {
	public Indir<Resource> base;
	public List<MD> mod = new ArrayList<>();
	public List<ED> equ = new ArrayList<>();

	public Desc() {
	}

	public Desc(Indir<Resource> base) {
	    this.base = base;
	}

	public static Desc decode(Session sess, Object[] args) {
	    Desc ret = new Desc();
	    ret.base = sess.getresv(args[0]);
	    Object[] ma = (Object[])args[1];
	    for(int i = 0; i < ma.length; i += 2) {
		List<ResData> tex = new ArrayList<ResData>();
		Indir<Resource> mod = sess.getresv(ma[i]);
		Object[] ta = (Object[])ma[i + 1];
		for(int o = 0; o < ta.length; o++) {
		    Indir<Resource> tr = sess.getresv(ta[o]);
		    Message sdt = Message.nil;
		    if((ta.length > o + 1) && (ta[o + 1] instanceof byte[]))
			sdt = new MessageBuf((byte[])ta[++o]);
		    tex.add(new ResData(tr, sdt));
		}
		ret.mod.add(new MD(mod, tex));
	    }
	    Object[] ea = (Object[])args[2];
	    for(int i = 0; i < ea.length; i++) {
		Object[] qa = (Object[])ea[i];
		int n = 0;
		int t = Utils.iv(qa[n++]);
		String at = (String)qa[n++];
		Indir<Resource> res = sess.getresv(qa[n++]);
		Message sdt = Message.nil;
		if(qa[n] instanceof byte[])
		    sdt = new MessageBuf((byte[])qa[n++]);
		Coord3f off = new Coord3f(Utils.fv(qa[n + 0]), Utils.fv(qa[n + 1]), Utils.fv(qa[n + 2]));
		ret.equ.add(new ED(t, at, new ResData(res, sdt), off));
	    }
	    return(ret);
	}

	public Desc clone() {
	    Desc ret = new Desc(base);
	    for(MD mod : this.mod)
		ret.mod.add(mod.clone());
	    for(ED equ : this.equ)
		ret.equ.add(equ.clone());
	    return(ret);
	}

	public String toString() {
	    return(String.format("desc(%s, %s, %s)", base, mod, equ));
	}
    }
    
    private final Material.Owner matowner = new Material.Owner() {
	    public <T> T context(Class<T> cl) {
		if(eqowner == null)
		    throw(new NoContext(cl));
		return(eqowner.context(cl));
	    }
	};
    private Collection<Model> nmod(Collection<MD> nmod) {
	Collection<Model> ret = new ArrayList<>(nmod.size());
	for(MD md : nmod) {
	    Model mod = md.real;
	    if(mod == null) {
		FastMesh.MeshRes mr = md.mod.get().layer(FastMesh.MeshRes.class);
		if(mr == null)
		    throw(new Sprite.ResourceException("Model resource contains no mesh", md.mod.get()));
		mod = new Model(mr.m, md.id);
		if(mr.rdat.containsKey("cz"))
		    mod.z = Integer.parseInt(mr.rdat.get("cz"));
		/* XXX: Actually set comp-z on borka meshes and remove me. */
		if(md.mod.get().name.equals("gfx/borka/male") || md.mod.get().name.equals("gfx/borka/female"))
		    mod.z = -1;
		for(ResData lres : md.tex)
		    mod.addlay(Material.fromres(matowner, lres.res.get(), new MessageBuf(lres.sdt)));
		md.real = mod;
	    }
	    ret.add(mod);
	}
	return(ret);
    }

    private Collection<Equ> nequ(List<ED> nequ) {
	Collection<Equ> ret = new ArrayList<>(nequ.size());
	outer: for(ED ed : nequ) {
	    Equ ne = ed.real;
	    if(ne == null) {
		creat: {
		    for(Equ equ : this.equ) {
			if(equ.desc.equals(ed)) {
			    ne = equ;
			    break creat;
			} else if((equ instanceof SpriteEqu) && (((SpriteEqu)equ).r instanceof Sprite.CUpd) && equ.desc.equals2(ed)) {
			    /* XXX: This is impure and ugly, but fixing it
			     * properly would seem to be significantly more
			     * complex for what is probably little benefit. */
			    if(!ed.res.sdt.equals(equ.desc.res.sdt)) {
				((Sprite.CUpd)((SpriteEqu)equ).r).update(ed.res.sdt.clone());
				equ.desc.res.sdt = ed.res.sdt;
			    }
			    ne = equ;
			    break creat;
			}
		    }
		    switch(ed.t) {
		    case 0: ne = new SpriteEqu(ed); break;
		    case 1: ne = new LightEqu(ed); break;
		    default: throw(new RuntimeException("Invalid composite equ-type: " + ed.t));
		    }
		}
		ed.real = ne;
	    }
	    ret.add(ne);
	}
	return(ret);
    }

    public static class CompositeClick extends Clickable {
	public final Gob.GobClick gi;

	public CompositeClick(Gob.GobClick gi) {
	    this.gi = gi;
	}

	public Object[] clickargs(ClickData cd) {
	    Object[] ret = {0, (int)gi.gob.id, gi.gob.rc.floor(OCache.posres), 0, -1};
	    int id = 0;
	    for(Object node : cd.array()) {
		if(node instanceof Model) {
		    Model mod = (Model)node;
		    if(mod.id >= 0)
			id = 0x01000000 | ((mod.id & 0xff) << 8);
		} else if(node instanceof Equ) {
		    Equ equ = (Equ)node;
		    if(equ.id >= 0)
			id = 0x02000000 | ((equ.id & 0xff) << 16);
		} else if(node instanceof FastMesh.ResourceMesh) {
		    FastMesh.ResourceMesh rm = (FastMesh.ResourceMesh)node;
		    if((id & 0xff000000) == 0x02000000)
			id = (id & 0xffff0000) | (rm.id & 0xffff);
		}
	    }
	    ret[4] = id;
	    return(ret);
	}

	public static final Pipe.Op prep = p -> {
	    Clickable prev = p.get(Clickable.slot);
	    if(prev instanceof Gob.GobClick)
		new CompositeClick((Gob.GobClick)prev).apply(p);
	};

	public String toString() {
	    return(String.format("#<composite-click %s>", gi));
	}
    }

    private void parts(RenderTree.Slot slot) {
	for(Model mod : this.mod)
	    slot.add(mod);
	for(Equ equ : this.equ)
	    slot.add(equ);
	// slot.add(pose.new Debug());
    }

    public void added(RenderTree.Slot slot) {
	slot.ostate(CompositeClick.prep);
	parts(slot);
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }
    
    public Supplier<Pipe.Op> eqpoint(String nm, Message dat) {
	return(pose.eqpoint(nm, dat));
    }

    public void draw(GOut g) {
    }
    
    public void tick(double dt) {
	if(poses != null)
	    poses.tick((float)dt);
	for(Equ equ : this.equ)
	    equ.tick(dt);
    }

    public void gtick(Render g) {
	for(Equ equ : this.equ)
	    equ.gtick(g);
    }

    public void chmod(List<MD> mod) {
	if(mod.equals(cmod))
	    return;
	Collection<Model> pmod = this.mod;
	this.mod = nmod(mod);
	RUtils.readd(slots, this::parts, () -> {this.mod = pmod;});
	cmod = new ArrayList<MD>(mod);
    }
    
    public void chequ(List<ED> equ) {
	if(equ.equals(cequ))
	    return;
	Collection<Equ> pequ = this.equ;
	this.equ = nequ(equ);
	RUtils.readd(slots, this::parts, () -> {this.equ = pequ;});
	cequ = new ArrayList<ED>(equ);
    }
}
