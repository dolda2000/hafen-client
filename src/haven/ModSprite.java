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
import java.lang.annotation.*;
import haven.render.*;
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;

public class ModSprite extends Sprite implements Sprite.CUpd, EquipTarget {
    public static final Collection<RMod> rmods = new ArrayList<>();
    private static final ThreadLocal<Cons> curcons = new ThreadLocal<Cons>();
    private static RenderTree.Node[] noparts = {};
    private static final Ticker[] notickers = {};
    private static final EquipTarget[] noeqtgts = {};
    private static final Mod[] nomods = {};
    public final Gob gob;
    public int flags = 0;
    protected final ResData resdata;
    protected final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    protected ArrayList<Mod> imods = null;
    protected RenderTree.Node[] parts = noparts;
    protected Ticker[] tickers = notickers;
    protected EquipTarget[] eqtgts = noeqtgts;
    private Mod[] omods = nomods;
    private int lastupd;
    
    public static final Factory fact = new Factory() {
	    public Sprite create(Owner owner, Resource res, Message sdt) {
		if((res.layer(FastMesh.MeshRes.class) != null) ||
		   (res.layer(RenderLink.Res.class) != null))
		    return(new ModSprite(owner, res, sdt) {
			    public String toString() {
				return(String.format("#<mod-sprite %s>", res.name));
			    }
			});
		return(null);
	    }
	};

    public static interface SMod {
	public void operate(ModSprite spr);
    }

    public static interface Mod {
	public void operate(Cons cons);
	public default int order() {return(0);}
	public default void age() {}

	public static Mod of(Consumer<Cons> mod, int order) {
	    return(new Mod() {
		    public void operate(Cons cons) {mod.accept(cons);}
		    public int order() {return(order);}
		});
	}
    }

    public static interface Ticker {
	public default boolean tick(double dt) {return(false);}
	public default void gtick(Render g) {}
    }

    public static class Part {
	public RenderTree.Node obj;
	public LinkedList<NodeWrap> wraps = new LinkedList<>();
	public LinkedList<Pipe.Op> state = new LinkedList<>();
	public LinkedList<Supplier<? extends Pipe.Op>> dynstate = new LinkedList<>();

	public Part(RenderTree.Node obj, NodeWrap... wraps) {
	    this.obj = obj;
	    for(NodeWrap wrap : wraps)
		this.wraps.add(wrap);
	}

	/* XXX? Is this nice? Not sure how to handle render-links in a nicer way. */
	public void unwrap() {
	    while(obj instanceof NodeWrap.Wrapping) {
		NodeWrap.Wrapping w = (NodeWrap.Wrapping)obj;
		obj = w.wrapped();
		wraps.add(w.wrap());
	    }
	}

	public RenderTree.Node make() {
	    RenderTree.Node ret = obj;
	    if(!state.isEmpty())
		ret = Pipe.Op.compose(state.toArray(new Pipe.Op[0])).apply(ret);
	    for(NodeWrap wrap : wraps)
		ret = wrap.apply(ret);
	    if(!dynstate.isEmpty()) {
		Supplier<? extends Pipe.Op> rst;
		if(dynstate.size() == 1) {
		    rst = dynstate.get(0);
		} else {
		    @SuppressWarnings("unchecked")
		    Supplier<? extends Pipe.Op>[] buf = dynstate.toArray(new Supplier[0]);
		    rst = () -> {
			Pipe.Op[] ops = new Pipe.Op[buf.length];
			for(int i = 0; i < ops.length; i++)
			    ops[i] = buf[i].get();
			return(Pipe.Op.compose(ops));
		    };
		}
		ret = RUtils.StateTickNode.from(ret, rst);
	    }
	    return(ret);
	}
    }

    public class Cons {
	public Collection<Mod> mods = new ArrayList<>();
	public Collection<Part> parts = new ArrayList<>();
	public Collection<Ticker> tickers = new ArrayList<>();
	public Collection<EquipTarget> eqtgts = new ArrayList<>();

	public ModSprite spr() {return(ModSprite.this);}

	public void add(Mod mod) {mods.add(mod);}
	public void add(Part part) {parts.add(part);}

	public void process() {
	    Cons prev = curcons.get();
	    curcons.set(this);
	    try {
		while(!mods.isEmpty()) {
		    Mod min = null;
		    int mino = 0;
		    for(Mod mod : mods) {
			int o = mod.order();
			if((min == null) || (o < mino)) {
			    min = mod;
			    mino = o;
			}
		    }
		    mods.remove(min);
		    min.operate(this);
		}
	    } finally {
		curcons.set(prev);
	    }
	}

	public RenderTree.Node[] parts() {
	    RenderTree.Node[] ret = new RenderTree.Node[parts.size()];
	    int i = 0;
	    for(Part part : parts)
		ret[i++] = part.make();
	    return(ret);
	}
    }

    public static class ResData {
	public final Resource res;
	public final Collection<Mod> mods = new ArrayList<>();
	public final Collection<SMod> smods = new ArrayList<>();

	public ResData(Resource res) {
	    this.res = res;
	}
    }

    public static class ModMaker extends Resource.PublishedCode.Instancer.Chain<RMod> {
	public ModMaker() {super(RMod.class);}
	{
	    add(new Direct<>(RMod.class));
	    add(new StaticCall<>(RMod.class, "operate", Void.TYPE, new Class<?>[] {ResData.class},
				 (make) -> (dat) -> make.apply(new Object[] {dat})));
	}}

    @Resource.PublishedCode(name = "sprmod", instancer = ModMaker.class)
    public static interface RMod {
	public void operate(ResData dat);

	@dolda.jglob.Discoverable
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Global {}
    }

    static {
	for(Class<?> cl : dolda.jglob.Loader.get(RMod.Global.class).classes()) {
	    if(RMod.class.isAssignableFrom(cl)) {
		rmods.add(Utils.construct(cl.asSubclass(RMod.class)));
	    } else {
		throw(new Error("Illegal res-mod class: " + cl));
	    }
	}
    }

    private static final Map<Resource, ResData> rdcache = new WeakHashMap<>();
    protected ResData resdata(Resource res) {
	synchronized(rdcache) {
	    ResData dat = rdcache.get(res);
	    if(dat == null) {
		dat = new ResData(res);
		for(RMod mod : rmods)
		    mod.operate(dat);
		RMod lmod = res.getcode(RMod.class, false);
		if(lmod != null)
		    lmod.operate(dat);
		rdcache.put(res, dat);
	    }
	    return(dat);
	}
    }

    public static int decflags(Message sdt) {
	return(sdt.eom() ? 0xffff0000 : decnum(sdt));
    }

    protected void decdata(Message sdt) {
	flags = decflags(sdt);
    }

    protected ModSprite(boolean dummy, Owner owner, Resource res) {
	super(owner, res);
	if((gob = owner.fcontext(Gob.class, false)) != null) {
	    omods = getomods();
	    lastupd = gob.updateseq;
	}
	resdata = resdata(res);
	for(SMod smod : resdata.smods)
	    smod.operate(this);
	if(imods != null)
	    imods.trimToSize();
    }

    public ModSprite(Owner owner, Resource res) {
	this(false, owner, res);
	init();
    }

    public ModSprite(Owner owner, Resource res, int flags) {
	this(false, owner, res);
	this.flags = flags;
	init();
    }

    public ModSprite(Owner owner, Resource res, Message sdt) {
	this(false, owner, res);
	decdata(sdt);
	init();
    }

    protected void init() {
	update();
    }

    public void imod(Mod mod) {
	if(imods == null)
	    imods = new ArrayList<>();
	imods.add(mod);
    }

    protected Cons cons() {
	return(new Cons());
    }

    public static Cons curcons() {
	return(curcons.get());
    }

    protected void modifiers(Cons cons) {
	for(Mod mod : resdata.mods)
	    cons.add(mod);
	if(imods != null) {
	    for(Mod mod : imods)
		cons.add(mod);
	}
	for(Mod mod : omods)
	    cons.add(mod);
    }

    protected void update() {
	Cons cons = cons();
	modifiers(cons);
	cons.process();
	RenderTree.Node[] pparts = this.parts;
	this.parts = cons.parts();
	RUtils.readd(slots, this::parts, () -> {this.parts = pparts;});
	this.tickers = cons.tickers.toArray(notickers);
	this.eqtgts = cons.eqtgts.toArray(noeqtgts);
    }

    public void update(Message sdt) {
	decdata(sdt);
	update();
    }

    protected void omods(Collection<Mod> buf, Gob gob) {
	for(GAttrib attr : gob.attr.values()) {
	    if(attr instanceof Mod)
		buf.add((Mod)attr);
	}
    }

    private Mod[] getomods() {
	Collection<Mod> buf = new ArrayList<>();
	omods(buf, gob);
	return(buf.toArray(nomods));
    }

    private void attrupdate() {
	synchronized(gob) {
	    Mod[] omods = getomods();
	    if(!Arrays.equals(omods, this.omods)) {
		Mod[] pmods = this.omods;
		this.omods = omods;
		try {
		    update();
		} catch(Loading l) {
		    this.omods = pmods;
		    throw(l);
		}
	    }
	}
    }

    public boolean tick(double dt) {
	if(gob != null) {
	    int seq = gob.updateseq;
	    if(seq != lastupd) {
		gob.glob.loader.defer(this::attrupdate, null);
		lastupd = seq;
	    }
	}
	boolean ret = false;
	for(Ticker ticker : tickers)
	    ret |= ticker.tick(dt);
	return(ret);
    }

    public void gtick(Render g) {
	for(Ticker ticker : tickers)
	    ticker.gtick(g);
    }

    public void age() {
	if(imods != null) {
	    for(Mod mod : imods)
		mod.age();
	}
    }

    private void parts(RenderTree.Slot slot) {
	for(RenderTree.Node part : parts)
	    slot.add(part);
    }

    public void added(RenderTree.Slot slot) {
	parts(slot);
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }

    public Supplier<? extends Pipe.Op> eqpoint(String nm, Message dat) {
	for(EquipTarget tgt : eqtgts) {
	    Supplier<? extends Pipe.Op> ret = tgt.eqpoint(nm, dat);
	    if(ret != null)
		return(ret);
	}
	Cons cons = curcons();
	if(cons != null) {
	    /* XXX? Not sure how nice this is, but equip-targets are
	     * likely mostly needed during construction. */
	    for(EquipTarget tgt : cons.eqtgts) {
		Supplier<? extends Pipe.Op> ret = tgt.eqpoint(nm, dat);
		if(ret != null)
		    return(ret);
	    }
	}
	return(null);
    }

    public String toString() {
	return(String.format("#<mod-sprite %s>", (res == null) ? "nil" : res.name));
    }

    public static class Meshes implements Mod {
	public final FastMesh.MeshRes[] meshes;

	public Meshes(FastMesh.MeshRes[] meshes) {
	    this.meshes = meshes;
	}

	public void operate(Cons cons) {
	    int flags = cons.spr().flags;
	    for(FastMesh.MeshRes mr : meshes) {
		if((mr.id < 0) || (((1 << mr.id) & flags) != 0))
		    cons.add(new Part(mr.m, mr.mat.get()));
	    }
	}

	@RMod.Global
	public static class $res implements RMod {
	    public void operate(ResData dat) {
		Collection<FastMesh.MeshRes> buf = new ArrayList<>();
		for(FastMesh.MeshRes mr : dat.res.layers(FastMesh.MeshRes.class)) {
		    if(mr.mat != null)
			buf.add(mr);
		}
		if(!buf.isEmpty())
		    dat.mods.add(new Meshes(buf.toArray(new FastMesh.MeshRes[0])));
	    }
	}
    }

    public static class RenderLinks implements Mod, Sprite.Owner {
	public final ModSprite main;
	public final RenderLink.Res[] rlinks;

	public RenderLinks(ModSprite spr, RenderLink.Res[] rlinks) {
	    this.main = spr;
	    this.rlinks = rlinks;
	}

	public void operate(Cons cons) {
	    int flags = cons.spr().flags;
	    for(RenderLink.Res lr : rlinks) {
		if((lr.id < 0) || (((1 << lr.id) & flags) != 0)) {
		    Part part = new Part(lr.l.make(this));
		    part.unwrap();
		    cons.add(part);
		    if(part.obj instanceof Sprite) {
			Sprite spr = (Sprite)part.obj;
			cons.tickers.add(new Ticker() {
				public boolean tick(double dt) {return(spr.tick(dt));}
				public void gtick(Render out) {spr.gtick(out);}
			    });
		    }
		}
	    }
	}

	private static final OwnerContext.ClassResolver<ModSprite> ctxr = new OwnerContext.ClassResolver<ModSprite>()
	    .add(ModSprite.class, spr -> spr);
	public <T> T context(Class<T> cl) {
	    return(OwnerContext.orparent(cl, ctxr.context(cl, main, false), main.owner));
	}
	public Random mkrandoom() {
	    return(main.owner.mkrandoom());
	}
	@Deprecated public Resource getres() {
	    return(main.res);
	}

	public int order() {return(2000);}

	@RMod.Global
	public static class $res implements RMod {
	    public void operate(ResData dat) {
		Collection<RenderLink.Res> buf = new ArrayList<>(dat.res.layers(RenderLink.Res.class));
		if(!buf.isEmpty())
		    dat.smods.add(spr -> spr.imod(new RenderLinks(spr, buf.toArray(new RenderLink.Res[0]))));
	    }
	}
    }

    public static class Animation implements Mod, Ticker {
	public final MeshAnim.Res[] descs;
	public MeshAnim.Animation[] anims = {};
	private Map<MeshAnim.Res, MeshAnim.Animation> ids = Collections.emptyMap();

	public Animation(MeshAnim.Res[] descs) {
	    this.descs = descs;
	}

	public void operate(Cons cons) {
	    int flags = cons.spr().flags;
	    Collection<MeshAnim.Animation> anims = new ArrayList<>(descs.length);
	    Map<MeshAnim.Res, MeshAnim.Animation> newids = new HashMap<>();
	    for(MeshAnim.Res ar : descs) {
		if((ar.id < 0) || (((1 << ar.id) & flags) != 0)) {
		    MeshAnim.Animation anim = ids.get(ar);
		    if(anim == null)
			anim = ar.make();
		    newids.put(ar, anim);
		    anims.add(anim);
		}
	    }
	    this.anims = anims.toArray(new MeshAnim.Animation[0]);
	    this.ids = newids.isEmpty() ? Collections.emptyMap() : newids;

	    for(Part part : cons.parts) {
		if(part.obj instanceof FastMesh) {
		    FastMesh m = (FastMesh)part.obj;
		    for(MeshAnim.Animation anim : this.anims) {
			if(anim.desc().animp(m)) {
			    part.wraps.add(anim.desc());
			    part.dynstate.add(anim::state);
			    break;
			}
		    }
		}
	    }

	    cons.tickers.add(this);
	}

	public boolean tick(double ddt) {
	    float dt = (float)ddt;
	    boolean done = false;
	    for(MeshAnim.Animation anim : anims)
		done |= anim.tick(dt);
	    return(done);
	}

	public void age() {
	    for(MeshAnim.Animation anim : anims)
		anim.age();
	}

	public int order() {return(1000);}

	@RMod.Global
	public static class $res implements RMod {
	    public void operate(ResData dat) {
		Collection<MeshAnim.Res> buf = new ArrayList<>(dat.res.layers(MeshAnim.Res.class));
		if(!buf.isEmpty())
		    dat.smods.add(spr -> spr.imod(new Animation(buf.toArray(new MeshAnim.Res[0]))));
	    }
	}
    }

    public static class Poser implements Mod, Ticker, Skeleton.ModOwner, EquipTarget {
	public static final Pipe.Op
	    rigid = new BaseColor(FColor.GREEN),
	    morphed = new BaseColor(FColor.RED),
	    unboned = new BaseColor(FColor.YELLOW);
	public static boolean bonedb = false;
	public static final float ipollen = 0.3f;
	private static final Map<Skeleton.ResPose, PoseMod> initids = new HashMap<>();
	public final ModSprite spr;
	public final Skeleton skel;
	public final Pose pose;
	public final Skeleton.ResPose[] descs;
	public PoseMod[] mods = {};
	private Map<Skeleton.ResPose, PoseMod> ids = Collections.emptyMap();
	private boolean stat = false;
	private Pose oldpose;
	private float ipold;

	public Poser(ModSprite spr, Skeleton skel, Skeleton.ResPose[] descs) {
	    this.spr = spr;
	    this.skel = skel;
	    this.descs = descs;
	    this.pose = skel.new Pose(skel.bindpose);
	}

	private void rebuild() {
	    pose.reset();
	    for(PoseMod m : mods)
		m.apply(pose);
	    if(ipold > 0)
		pose.blend(oldpose, Utils.smoothstep(ipold));
	    pose.gbuild();
	}

	public void operate(Cons cons) {
	    int flags = cons.spr().flags;
	    stat = true;
	    Collection<PoseMod> poses = new ArrayList<>();
	    Map<Skeleton.ResPose, PoseMod> newids = new HashMap<>();
	    for(Skeleton.ResPose p : descs) {
		if((p.id < 0) || (((1 << p.id) & flags) != 0)) {
		    Skeleton.PoseMod mod = ids.get(p);
		    if(mod == null)
			mod = p.forskel(this, skel, p.defmode);
		    newids.put(p, mod);
		    stat &= mod.stat();
		    poses.add(mod);
		}
	    }
	    this.mods = poses.toArray(new PoseMod[0]);
	    if((ids != initids) && !ids.equals(newids)) {
		this.oldpose = skel.new Pose(pose);
		this.ipold = 1.0f;
	    }
	    this.ids = newids.isEmpty() ? Collections.emptyMap() : newids;
	    rebuild();

	    for(Part part : cons.parts) {
		if(part.obj instanceof FastMesh) {
		    FastMesh m = (FastMesh)part.obj;
		    if(PoseMorph.boned(m)) {
			String bnm = PoseMorph.boneidp(m);
			if(bnm == null) {
			    PoseMorph st = new PoseMorph(pose, m);
			    part.dynstate.add(st::state);
			    if(bonedb)
				part.state.add(morphed);
			} else {
			    part.dynstate.add(pose.bonetrans2(skel.bones.get(bnm).idx));
			    if(bonedb)
				part.state.add(rigid);
			}
		    } else {
			if(bonedb)
			    part.state.add(unboned);
		    }
		}
	    }

	    cons.tickers.add(this);
	    cons.eqtgts.add(this);
	    // cons.add(new Part(pose.new Debug()));
	}

	public boolean tick(double ddt) {
	    float dt = (float)ddt;
	    if(!stat || (ipold > 0)) {
		boolean done = true;
		for(PoseMod m : mods) {
		    m.tick(dt);
		    done &= m.done();
		}
		if(done)
		    stat = true;
		if(ipold > 0) {
		    if((ipold -= (dt / ipollen)) < 0) {
			ipold = 0.0f;
			oldpose = null;
		    }
		}
		rebuild();
	    }
	    return(false);
	}

	public Supplier<Pipe.Op> eqpoint(String nm, Message dat) {
	    Skeleton.BoneOffset bo = spr.res.layer(Skeleton.BoneOffset.class, nm);
	    if(bo != null)
		return(bo.from(pose));
	    if(pose != null)
		return(pose.eqpoint(nm, dat));
	    return(null);
	}

	private static final OwnerContext.ClassResolver<Poser> ctxr = new OwnerContext.ClassResolver<Poser>()
	    .add(Poser.class, p -> p)
	    .add(ModSprite.class, p -> p.spr);
	public <T> T context(Class<T> cl) {
	    return(OwnerContext.orparent(cl, ctxr.context(cl, this, false), spr.owner));
	}
	public Collection<Location.Chain> getloc() {
	    Collection<Location.Chain> ret = new ArrayList<>(spr.slots.size());
	    for(RenderTree.Slot slot : spr.slots)
		ret.add(slot.state().get(Homo3D.loc));
	    return(ret);
	}
	public double getv() {
	    return((spr.owner instanceof Skeleton.ModOwner) ? ((Skeleton.ModOwner)spr.owner).getv() : 0);
	}

	public void age() {
	    for(PoseMod mod : mods)
		mod.age();
	    this.ipold = 0.0f;
	    this.oldpose = null;
	}

	public int order() {return(1010);}

	@RMod.Global
	public static class $res implements RMod {
	    public void operate(ResData dat) {
		Skeleton.Res sr = dat.res.layer(Skeleton.Res.class);
		Collection<Skeleton.ResPose> buf = new ArrayList<>(dat.res.layers(Skeleton.ResPose.class));
		if((sr != null) && !buf.isEmpty())
		    dat.smods.add(spr -> spr.imod(new Poser(spr, sr.s, buf.toArray(new Skeleton.ResPose[0]))));
	    }
	}
    }
}
