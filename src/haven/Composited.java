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
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;
import haven.Skeleton.TrackMod;

public class Composited implements Rendered {
    public final Skeleton skel;
    public final Pose pose;
    private Collection<Model> mod = new LinkedList<Model>();
    private Collection<Equ> equ = new LinkedList<Equ>();
    public Poses poses = new Poses();
    public List<MD> nmod = null, cmod = new LinkedList<MD>();
    public List<ED> nequ = null, cequ = new LinkedList<ED>();
    
    public class Poses {
	private final PoseMod[] mods;
	Pose old;
	float ipold = 0.0f, ipol = 0.0f;
	public float limit = -1.0f;
	public boolean stat, ldone;
	
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

	public void tick(float dt, double v) {
	    boolean build = false;
	    if(limit >= 0) {
		if((limit -= dt) < 0)
		    ldone = true;
	    }
	    boolean done = ldone;
	    for(PoseMod m : mods) {
		float mdt = dt;
		if(m instanceof TrackMod) {
		    TrackMod t = (TrackMod)m;
		    if(t.speedmod)
			mdt *= (float)(v / t.nspeed);
		}
		m.tick(mdt);
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
    
    private static final Rendered.Order modorder = new Rendered.Order<Model.Layer>() {
	public int mainz() {
	    return(1);
	}
	
	private final Rendered.RComparator<Model.Layer> cmp = new Rendered.RComparator<Model.Layer>() {
	    public int compare(Model.Layer a, Model.Layer b, GLState.Buffer sa, GLState.Buffer sb) {
		if(a.z1 != b.z1)
		    return(a.z1 - b.z1);
		return(a.z2 - b.z2);
	    }
	};
	
	public Rendered.RComparator<Model.Layer> cmp() {
	    return(cmp);
	}
    };

    private class Model implements Rendered {
	private final MorphedMesh m;
	int z = 0, lz = 0;
	private class Layer implements FRendered {
	    private final Material mat;
	    private final int z1, z2;
	    
	    private Layer(Material mat, int z1, int z2) {
		this.mat = mat;
		this.z1 = z1;
		this.z2 = z2;
	    }
	    
	    public void draw(GOut g) {
		m.draw(g);
	    }
	    
	    public void drawflat(GOut g) {
		if(z2 == 0)
		    m.drawflat(g);
	    }
	    
	    public boolean setup(RenderList r) {
		r.prepo(modorder);
		r.prepo(mat);
		return(true);
	    }
	}
	private final List<Layer> lay = new ArrayList<Layer>();
	
	private Model(FastMesh m) {
	    this.m = new MorphedMesh(m, pose);
	}
	
	private void addlay(Material mat) {
	    lay.add(new Layer(mat, z, lz++));
	}

	public void draw(GOut g) {
	}
	
	public boolean setup(RenderList r) {
	    m.setup(r);
	    for(Layer lay : this.lay)
		r.add(lay, null);
	    return(false);
	}
    }
    
    private class SpriteEqu extends Equ {
	private final Sprite spr;
	
	private SpriteEqu(ED ed) {
	    super(ed);
	    this.spr = Sprite.create(null, ed.res.get(), new Message(0));
	}
	
	public void draw(GOut g) {
	}

	public boolean setup(RenderList rl) {
	    rl.add(spr, null);
	    return(false);
	}
	
	public void tick(int dt) {
	    spr.tick(dt);
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
	
	public boolean setup(RenderList rl) {
	    rl.add(l, null);
	    return(false);
	}
    }

    private abstract class Equ implements Rendered {
	private final GLState et;
	
	private Equ(ED ed) {
	    Skeleton.BoneOffset bo = ed.res.get().layer(Skeleton.BoneOffset.class, ed.at);
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
	
	public void tick(int dt) {}
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
			md.real.addlay(mr.get());
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

    public boolean setup(RenderList rl) {
	changes();
	for(Model mod : this.mod)
	    rl.add(mod, null);
	for(Equ equ : this.equ)
	    rl.add(equ, equ.et);
	return(false);
    }
    
    public void draw(GOut g) {
    }
    
    public void tick(int dt, double v) {
	if(poses != null)
	    poses.tick(dt / 1000.0f, v);
	for(Equ equ : this.equ)
	    equ.tick(dt);
    }

    public void chmod(List<MD> mod) {
	if(mod.equals(cmod))
	    return;
	this.mod = new LinkedList<Model>();
	nmod = new LinkedList<MD>(mod);
	cmod = new ArrayList<MD>(mod.size());
	for(MD md : mod)
	    cmod.add(md.clone());
    }
    
    public void chequ(List<ED> equ) {
	if(equ.equals(cequ))
	    return;
	this.equ = new LinkedList<Equ>();
	nequ = new LinkedList<ED>(equ);
	cequ = new ArrayList<ED>(equ.size());
	for(ED ed : equ)
	    cequ.add(ed.clone());
    }
}
