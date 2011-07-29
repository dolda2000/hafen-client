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
import java.lang.reflect.*;
import haven.Skeleton.Pose;
import haven.Skeleton.TrackMod;

public class Composite extends Drawable {
    public final static float ipollen = 0.2f;
    public final Indir<Resource> base;
    private Skeleton skel;
    private Pose pose;
    private Collection<Model> mod = new LinkedList<Model>();
    private Poses poses = null, nposes = new Poses();
    private Collection<Equ> equ = new LinkedList<Equ>();
    public int pseq;
    private List<MD> nmod = null, cmod = new LinkedList<MD>();
    private List<ED> nequ = null, cequ = new LinkedList<ED>();
    
    private class Poses {
	TrackMod[] mods = new TrackMod[0];
	final List<Indir<Resource>> loading;
	Poses seq;
	Pose old;
	float ipold = 0.0f, ipol = 0.0f;
	float limit = -1.0f;
	boolean stat, ldone;
	WrapMode mode = null;
	
	private Poses() {
	    this.loading = Collections.emptyList();
	}

	private Poses(List<Indir<Resource>> rl, float ipol) {
	    this.loading = rl;
	    this.ipol = ipol;
	}
	
	boolean load() {
	    try {
		for(Indir<Resource> res : loading)
		    res.get();
	    } catch(Loading e) {
		return(false);
	    }
	    if(ipol > 0) {
		old = skel.new Pose(pose);
		ipold = 1.0f;
	    }
	    List<TrackMod> mods = new LinkedList<TrackMod>();
	    stat = true;
	    for(Indir<Resource> res : loading) {
		for(Skeleton.ResPose p : res.get().layers(Skeleton.ResPose.class)) {
		    TrackMod mod = p.forskel(skel, (mode == null)?p.defmode:mode);
		    if(!mod.stat)
			stat = false;
		    mods.add(mod);
		}
	    }
	    this.mods = mods.toArray(new TrackMod[0]);
	    poses = this;
	    rebuild();
	    return(true);
	}
	
	void rebuild() {
	    pose.reset();
	    for(TrackMod m : mods)
		m.apply(pose);
	    if(ipold > 0.0f)
		pose.blend(old, ipold);
	    pose.gbuild();
	}
	
	void tick(float dt) {
	    boolean build = false;
	    Moving mv = gob.getattr(Moving.class);
	    double v = 0;
	    if(mv != null)
		v = mv.getv();
	    if(limit >= 0) {
		if((limit -= dt) < 0)
		    ldone = true;
	    }
	    boolean done = ldone;
	    for(TrackMod m : mods) {
		m.tick((m.speedmod)?(dt * (float)(v / m.nspeed)):dt);
		if(m.mode.ends && !m.done)
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
	    if(done && (seq != null))
		nposes = seq;
	}
    }

    private static final Rendered.Order modorder = new Rendered.Order<Model>() {
	public int mainz() {
	    return(1);
	}
	
	private final Rendered.RComparator<Model> cmp = new Rendered.RComparator<Model>() {
	    public int compare(Model a, Model b, GLState.Buffer sa, GLState.Buffer sb) {
		return(a.z - b.z);
	    }
	};
	
	public Rendered.RComparator<Model> cmp() {
	    return(cmp);
	}
    };

    private class Model implements FRendered {
	private final MorphedMesh m;
	int z = 0;
	private final List<Material> lay = new ArrayList<Material>();
	
	private Model(FastMesh m) {
	    this.m = new MorphedMesh(m, pose);
	}
	
	public void draw(GOut g) {
	    for(Material lay : this.lay) {
		g.state(lay);
		m.draw(g);
	    }
	}
	
	public void drawflat(GOut g) {
	    m.drawflat(g);
	}
	
	public Order setup(RenderList r) {
	    m.setup(r);
	    return(modorder);
	}
    }
    
    private class SpriteEqu extends Equ {
	private final Sprite spr;
	
	private SpriteEqu(ED ed) {
	    super(ed);
	    this.spr = Sprite.create(gob, ed.res.get(), new Message(0));
	}
	
	public void draw(GOut g) {
	}

	public Order setup(RenderList rl) {
	    rl.add(spr, null);
	    return(null);
	}
    }
    
    private class LightEqu extends Equ {
	private final Light l;
	
	private LightEqu(ED ed) {
	    super(ed);
	    this.l = ed.res.get().layer(Light.Res.class).make();
	}
	
	public void draw(GOut g) {
	}
	
	public Order setup(RenderList rl) {
	    rl.add(l, null);
	    return(null);
	}
    }

    private abstract class Equ implements Rendered {
	private final GLState et;
	
	private Equ(ED ed) {
	    Skeleton.BoneOffset bo = null;
	    for(Skeleton.BoneOffset co : ed.res.get().layers(Skeleton.BoneOffset.class)) {
		if(co.nm.equals(ed.at)) {
		    bo = co;
		    break;
		}
	    }
	    GLState bt;
	    if(bo != null) {
		bt = bo.forpose(pose);
	    } else {
		Skeleton.Bone bone = skel.bones.get(ed.at);
		bt = pose.bonetrans(bone.idx);
	    }
	    if((ed.off.x != 0.0f) || (ed.off.y != 0.0f) || (ed.off.z != 0.0f))
		this.et = GLState.compose(bt, Location.xlate(ed.off));
	    else
		this.et = bt;
	}
    }

    public static class MD implements Cloneable {
	public Indir<Resource> mod;
	public List<Indir<Resource>> tex;
	private Model real;
	
	public MD(Indir<Resource> mod, List<Indir<Resource>> tex) {
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
		ret.tex = new LinkedList<Indir<Resource>>(tex);
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
	public int t;
	public String at;
	public Indir<Resource> res;
	public Coord3f off;
	
	public ED(int t, String at, Indir<Resource> res, Coord3f off) {
	    this.t = t;
	    this.at = at;
	    this.res = res;
	    this.off = off;
	}
	
	public boolean equals(Object o) {
	    if(!(o instanceof ED))
		return(false);
	    ED e = (ED)o;
	    return((t == e.t) && at.equals(e.at) && res.equals(e.res));
	}

	public ED clone() {
	    try {
		ED ret = (ED)super.clone();
		return(ret);
	    } catch(CloneNotSupportedException e) {
		/* This is ridiculous. */
		throw(new RuntimeException(e));
	    }
	}
    }
    
    public Composite(Gob gob, Indir<Resource> base) {
	super(gob);
	this.base = base;
    }
    
    private void init() {
	if(skel != null)
	    return;
	skel = base.get().layer(Skeleton.Res.class).s;
	pose = skel.new Pose(skel.bindpose);
    }
    
    private void nmod() {
	for(Iterator<MD> i = nmod.iterator(); i.hasNext();) {
	    MD md = i.next();
	    try {
		if(md.real == null) {
		    FastMesh.MeshRes mr = md.mod.get().layer(FastMesh.MeshRes.class);
		    md.real = new Model(mr.m);
		    /* This is really ugly, but I can't really think of
		     * anything less ugly right now. */
		    if(md.mod.get().name.equals("gfx/borka/male") || md.mod.get().name.equals("gfx/borka/female"))
			md.real.z = -1;
		    this.mod.add(md.real);
		}
		for(Iterator<Indir<Resource>> o = md.tex.iterator(); o.hasNext();) {
		    Indir<Resource> res = o.next();
		    for(Material.Res mr : res.get().layers(Material.Res.class))
			md.real.lay.add(mr.get());
		    o.remove();
		}
		i.remove();
	    } catch(Loading e) {}
	}
	if(nmod.isEmpty())
	    nmod = null;
    }

    private void nequ() {
	for(Iterator<ED> i = nequ.iterator(); i.hasNext();) {
	    ED ed = i.next();
	    try {
		if(ed.t == 0)
		    this.equ.add(new SpriteEqu(ed));
		else if(ed.t == 1)
		    this.equ.add(new LightEqu(ed));
		i.remove();
	    } catch(Loading e) {}
	}
	if(nequ.isEmpty())
	    nequ = null;
    }

    private void changes() {
	if(nmod != null)
	    nmod();
	if(nequ != null)
	    nequ();
    }

    public void setup(RenderList rl) {
	try {
	    init();
	} catch(Loading e) {
	    return;
	}
	changes();
	for(Model mod : this.mod)
	    rl.add(mod, null);
	for(Equ equ : this.equ)
	    rl.add(equ, equ.et);
    }
	
    public void ctick(int dt) {
	if(skel == null)
	    return;
	if(nposes != null) {
	    if(nposes.load())
		nposes = null;
	}
	if(poses != null)
	    poses.tick(dt / 1000.0f);
    }

    public Resource.Neg getneg() {
	return(base.get().layer(Resource.negc));
    }
    
    public Pose getpose() {
	init();
	return(pose);
    }
    
    public void chposes(List<Indir<Resource>> poses, boolean interp) {
	nposes = new Poses(poses, interp?ipollen:0.0f);
    }
    
    public void tposes(List<Indir<Resource>> tposes, WrapMode mode, float time) {
	Poses p = new Poses(tposes, ipollen);
	p.mode = mode;
	if(time >= 0)
	    p.limit = time;
	Poses seq;
	if(nposes != null)
	    seq = nposes;
	else
	    seq = poses;
	if(seq.seq != null)
	    seq = seq.seq;
	else
	    seq = new Poses(seq.loading, ipollen);
	p.seq = seq;
	nposes = p;
    }
    
    /*
    public void tpose(List<Indir<Resource>> poses, WrapMode mode, float time) {
	this.tposes = poses;
	this.tpmode = mode;
	this.tptime = time;
    }
    */
    
    public void chmod(List<MD> mod) {
	if(mod.equals(cmod))
	    return;
	this.mod = new LinkedList<Model>();
	nmod = mod;
	cmod = new ArrayList<MD>(mod.size());
	for(MD md : mod)
	    cmod.add(md.clone());
    }
    
    public void chequ(List<ED> equ) {
	if(equ.equals(cequ))
	    return;
	this.equ = new LinkedList<Equ>();
	nequ = equ;
	cequ = new ArrayList<ED>(equ.size());
	for(ED ed : equ)
	    cequ.add(ed.clone());
    }
}
