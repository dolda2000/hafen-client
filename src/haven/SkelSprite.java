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

public class SkelSprite extends Sprite implements Sprite.CUpd, EquipTarget, Sprite.Owner, Skeleton.ModOwner {
    public static final Pipe.Op
	rigid = new BaseColor(FColor.GREEN),
	morphed = new BaseColor(FColor.RED),
	unboned = new BaseColor(FColor.YELLOW);
    public static boolean bonedb = false;
    public static final float ipollen = 0.3f;
    public final Skeleton skel;
    public final Pose pose;
    public PoseMod[] mods = new PoseMod[0];
    public MeshAnim.Animation[] manims = new MeshAnim.Animation[0];
    public int curfl;
    protected final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    private Pose oldpose;
    private float ipold;
    private boolean stat = true;
    private RenderTree.Node[] parts;
    private Collection<Runnable> tickparts = Collections.emptyList();
    private Collection<Consumer<Render>> gtickparts = Collections.emptyList();
    
    public static final Factory fact = new Factory() {
	    public Sprite create(Owner owner, Resource res, Message sdt) {
		if(res.layer(Skeleton.Res.class) == null)
		    return(null);
		return(new SkelSprite(owner, res, sdt) {
			public String toString() {
			    return(String.format("#<skel-sprite %s>", res.name));
			}
		    });
	    }
	};
    
    public SkelSprite(Owner owner, Resource res, int fl) {
	super(owner, res);
	Skeleton.Res sr = res.layer(Skeleton.Res.class);
	if(sr != null) {
	    skel = sr.s;
	    pose = skel.new Pose(skel.bindpose);
	} else {
	    skel = null;
	    pose = null;
	}
	update(fl);
    }

    public SkelSprite(Owner owner, Resource res) {
	this(owner, res, 0xffff0000);
    }

    public SkelSprite(Owner owner, Resource res, Message sdt) {
	this(owner, res, sdt.eom() ? 0xffff0000 : decnum(sdt));
    }

    private void parts(RenderTree.Slot slot) {
	for(RenderTree.Node p : parts)
	    slot.add(p);
	// if(pose != null)
	//     slot.add(pose.new Debug());
    }

    private static final OwnerContext.ClassResolver<SkelSprite> ctxr = new OwnerContext.ClassResolver<SkelSprite>()
	.add(SkelSprite.class, spr -> spr);
    public <T> T context(Class<T> cl) {
	return(OwnerContext.orparent(cl, ctxr.context(cl, this, false), owner));
    }
    public Random mkrandoom() {
	return(owner.mkrandoom());
    }
    @Deprecated public Resource getres() {
	return(res);
    }
    public Collection<Location.Chain> getloc() {
	Collection<Location.Chain> ret = new ArrayList<>(slots.size());
	for(RenderTree.Slot slot : slots)
	    ret.add(slot.state().get(Homo3D.loc));
	return(ret);
    }
    public double getv() {
	return((owner instanceof Skeleton.ModOwner) ? ((Skeleton.ModOwner)owner).getv() : 0);
    }

    /* XXX: It's ugly to snoop inside a wrapping, but I can't think of
     * a better way to apply morphing to renderlinks right now. */
    protected RenderTree.Node animwrap(Pipe.Op.Wrapping wrap, Collection<Runnable> tbuf, Collection<Consumer<Render>> gbuf) {
	if(!(wrap.r instanceof FastMesh))
	    return(wrap);
	FastMesh m = (FastMesh)wrap.r;
	ArrayList<Supplier<Pipe.Op>> states = new ArrayList<>();
	for(MeshAnim.Animation anim : manims) {
	    if(anim.desc().animp(m)) {
		wrap = anim.desc().apply(wrap);
		states.add(anim::state);
		break;
	    }
	}
	if(PoseMorph.boned(m)) {
	    String bnm = PoseMorph.boneidp(m);
	    if(bnm == null) {
		PoseMorph st = new PoseMorph(pose, m);
		states.add(st::state);
		if(bonedb)
		    states.add(() -> morphed);
	    } else {
		states.add(pose.bonetrans2(skel.bones.get(bnm).idx));
		if(bonedb)
		    states.add(() -> rigid);
	    }
	} else {
	    if(bonedb)
		states.add(() -> unboned);
	}
	Supplier<Pipe.Op> rst;
	if(states.size() == 0) {
	    return(wrap);
	} else if(states.size() == 1) {
	    rst = states.get(0);
	} else {
	    states.trimToSize();
	    rst = () -> {
		Pipe.Op[] ops = new Pipe.Op[states.size()];
		for(int i = 0; i < ops.length; i++)
		    ops[i] = states.get(i).get();
		return(Pipe.Op.compose(ops));
	    };
	}
	return(RUtils.StateTickNode.from(wrap, rst));
    }

    public void iparts(int mask, Collection<RenderTree.Node> rbuf, Collection<Runnable> tbuf, Collection<Consumer<Render>> gbuf) {
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    if((mr.mat != null) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0)))
		rbuf.add(animwrap(mr.mat.get().apply(mr.m), tbuf, gbuf));
	}
	for(RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
	    if((lr.id < 0) || (((1 << lr.id) & mask) != 0)) {
		RenderTree.Node r = lr.l.make(this);
		if(r instanceof Pipe.Op.Wrapping)
		    r = animwrap((Pipe.Op.Wrapping)r, tbuf, gbuf);
		rbuf.add(r);
	    }
	}
    }

    private void chparts(int mask) {
	Collection<RenderTree.Node> rl = new ArrayList<>();
	Collection<Runnable> tbuf = new ArrayList<>();
	Collection<Consumer<Render>> gbuf = new ArrayList<>();
	iparts(mask, rl, tbuf, gbuf);
	/* XXX: Arguably, updating should be forgone if the parts
	 * haven't actually changed. Somewhat ill-defined, however. */
	RenderTree.Node[] pparts = this.parts;
	this.parts = rl.toArray(new RenderTree.Node[0]);
	RUtils.readd(slots, this::parts, () -> {this.parts = pparts;});
	this.tickparts = tbuf;
	this.gtickparts = gbuf;
    }
    
    private void rebuild() {
	pose.reset();
	for(PoseMod m : mods)
	    m.apply(pose);
	if(ipold > 0) {
	    float f = ipold * ipold * (3 - (2 * ipold));
	    pose.blend(oldpose, f);
	}
	pose.gbuild();
    }

    private static final Map<MeshAnim.Res, MeshAnim.Animation> nomaids = new HashMap<>();
    private Map<MeshAnim.Res, MeshAnim.Animation> maids = nomaids;
    private void chmanims(int mask) {
	Collection<MeshAnim.Animation> anims = new LinkedList<>();
	Map<MeshAnim.Res, MeshAnim.Animation> newids = new HashMap<>();
	for(MeshAnim.Res ar : res.layers(MeshAnim.Res.class)) {
	    if((ar.id < 0) || (((1 << ar.id) & mask) != 0)) {
		MeshAnim.Animation anim = maids.get(ar);
		if(anim == null)
		    anim = ar.make();
		newids.put(ar, anim);
		anims.add(anim);
	    }
	}
	this.manims = anims.toArray(new MeshAnim.Animation[0]);
	if(newids.isEmpty())
	    newids = nomaids;
	this.maids = newids;
    }

    private static final Map<Skeleton.ResPose, PoseMod> initmodids = new HashMap<>();
    private Map<Skeleton.ResPose, PoseMod> modids = initmodids;
    private void chposes(int mask) {
	Collection<PoseMod> poses = new LinkedList<PoseMod>();
	stat = true;
	Map<Skeleton.ResPose, PoseMod> newids = new HashMap<>();
	for(Skeleton.ResPose p : res.layers(Skeleton.ResPose.class)) {
	    if((p.id < 0) || ((mask & (1 << p.id)) != 0)) {
		Skeleton.PoseMod mod = modids.get(p);
		if(mod == null)
		    mod = p.forskel(this, skel, p.defmode);
		newids.put(p, mod);
		if(!mod.stat())
		    stat = false;
		poses.add(mod);
	    }
	}
	this.mods = poses.toArray(new PoseMod[0]);
	if((modids != initmodids) && !modids.equals(newids)) {
	    this.oldpose = skel.new Pose(pose);
	    this.ipold = 1.0f;
	}
	this.modids = newids;
	rebuild();
    }

    public void update(int fl) {
	chmanims(fl);
	if(skel != null)
	    chposes(fl);
	chparts(fl);
	this.curfl = fl;
    }

    public void update() {
	update(curfl);
    }

    public void update(Message sdt) {
	int fl = sdt.eom() ? 0xffff0000 : decnum(sdt);
	update(fl);
    }
    
    public void added(RenderTree.Slot slot) {
	parts(slot);
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }
    
    public void age() {
	for(PoseMod mod : mods)
	    mod.age();
	for(MeshAnim.Animation anim : manims)
	    anim.age();
	this.ipold = 0.0f;
	this.oldpose = null;
    }

    public boolean tick(double ddt) {
	float dt = (float)ddt;
	if(!stat || (ipold > 0)) {
	    boolean done = true;
	    for(PoseMod m : mods) {
		m.tick(dt);
		done = done && m.done();
	    }
	    if(done)
		stat = true;
	    if(ipold > 0) {
		if((ipold -= (dt / ipollen)) < 0) {
		    ipold = 0;
		    oldpose = null;
		}
	    }
	    rebuild();
	}
	for(MeshAnim.Animation anim : manims)
	    anim.tick(dt);
	for(Runnable tpart : tickparts)
	    tpart.run();
	return(false);
    }

    public void gtick(Render g) {
	for(Consumer<Render> gpart : gtickparts)
	    gpart.accept(g);
    }

    public Supplier<Pipe.Op> eqpoint(String nm, Message dat) {
	Skeleton.BoneOffset bo = res.layer(Skeleton.BoneOffset.class, nm);
	if(bo != null)
	    return(bo.from(pose));
	if(pose != null)
	    return(pose.eqpoint(nm, dat));
	return(null);
    }

    static {
	Console.setscmd("bonedb", new Console.Command() {
		public void run(Console cons, String[] args) {
		    bonedb = Utils.parsebool(args[1], false);
		}
	    });
    }
}
