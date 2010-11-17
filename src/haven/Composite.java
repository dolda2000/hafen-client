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
    private final Indir<Resource> base;
    private Skeleton skel;
    private Pose pose;
    private Collection<Model> mod = new LinkedList<Model>();
    private TrackMod[] mods = new TrackMod[0];
    private Collection<Equ> equ = new LinkedList<Equ>();
    private boolean stat = true;
    private List<Indir<Resource>> nposes = null, cposes = new LinkedList<Indir<Resource>>();
    private List<MD> nmod = null, cmod = new LinkedList<MD>();
    private List<ED> nequ = null, cequ = new LinkedList<ED>();
    
    private class Model implements Rendered {
	private final MorphedMesh m;
	private final List<Material> lay = new ArrayList<Material>();
	
	private Model(FastMesh m) {
	    this.m = new MorphedMesh(m, pose);
	}
	
	public void draw(GOut g) {
	    for(Material lay : this.lay) {
		g.matsel(lay);
		m.draw(g);
	    }
	}
	
	public boolean setup(RenderList r) {
	    return(m.setup(r));
	}
    }
    
    private class SpriteEqu extends Equ {
	private final Sprite spr;
	private final Transform st;
	
	private SpriteEqu(ED ed) {
	    super(ed);
	    this.spr = Sprite.create(gob, ed.res.get(), new Message(0));
	    Skeleton.BoneOffset off = ed.res.get().layer(Skeleton.BoneOffset.class);
	    if(off == null)
		this.st = null;
	    else
		this.st = off.xf();
	}
	
	public void draw(GOut g) {
	}

	public boolean setup(RenderList rl) {
	    rl.add(spr, st);
	    return(false);
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
	    rl.add(l);
	    return(false);
	}
    }

    private abstract class Equ implements Rendered {
	private final Transform et;
	
	private Equ(ED ed) {
	    Skeleton.Bone bone = skel.bones.get(ed.at);
	    Transform bt = pose.bonetrans(bone.idx);
	    if((ed.off.x != 0.0f) || (ed.off.y != 0.0f) || (ed.off.z != 0.0f))
		this.et = Transform.seq(bt, Transform.xlate(ed.off));
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
	if(skel != null) {
	    return;
	}
	if(base.get() == null)
	    return;
	skel = base.get().layer(Skeleton.Res.class).s;
	pose = skel.new Pose(skel.bindpose);
    }
    
    private void nposes() {
	for(Indir<Resource> res : this.nposes) {
	    if(res.get() == null)
		return;
	}
	List<TrackMod> nposes = new LinkedList<TrackMod>();
	stat = true;
	pose.reset();
	for(Indir<Resource> res : this.nposes) {
	    for(Skeleton.ResPose p : res.get().layers(Skeleton.ResPose.class)) {
		TrackMod mod = p.forskel(skel);
		if(!mod.stat)
		    stat = false;
		nposes.add(mod);
		mod.apply(pose);
	    }
	}
	pose.gbuild();
	this.mods = nposes.toArray(this.mods);
	this.nposes = null;
    }
    
    private void nmod() {
	for(Iterator<MD> i = nmod.iterator(); i.hasNext();) {
	    MD md = i.next();
	    if(md.real == null) {
		if(md.mod.get() == null)
		    continue;
		FastMesh.MeshRes mr = md.mod.get().layer(FastMesh.MeshRes.class);
		md.real = new Model(mr.m);
		this.mod.add(md.real);
	    }
	    for(Iterator<Indir<Resource>> o = md.tex.iterator(); o.hasNext();) {
		Indir<Resource> res = o.next();
		if(res.get() != null) {
		    for(Material.Res mr : res.get().layers(Material.Res.class))
			md.real.lay.add(mr.m);
		    o.remove();
		}
	    }
	    if(md.tex.isEmpty())
		i.remove();
	}
	if(nmod.isEmpty())
	    nmod = null;
    }

    private void nequ() {
	for(Iterator<ED> i = nequ.iterator(); i.hasNext();) {
	    ED ed = i.next();
	    if((ed.res != null) && (ed.res.get() == null))
		continue;
	    if(ed.t == 0)
		this.equ.add(new SpriteEqu(ed));
	    else if(ed.t == 1)
		this.equ.add(new LightEqu(ed));
	    i.remove();
	}
	if(nequ.isEmpty())
	    nequ = null;
    }

    private void changes() {
	if(nposes != null)
	    nposes();
	if(nmod != null)
	    nmod();
	if(nequ != null)
	    nequ();
    }

    public void setup(RenderList rl) {
	init();
	if(skel == null)
	    return;
	changes();
	for(Model mod : this.mod)
	    rl.add(mod, null);
	for(Equ equ : this.equ)
	    rl.add(equ, equ.et);
    }
	
    public void ctick(int dt) {
	if(!stat) {
	    pose.reset();
	    for(TrackMod m : mods) {
		m.update(dt / 1000.0f);
		m.apply(pose);
	    }
	    pose.gbuild();
	}
    }
    
    public Resource.Neg getneg() {
	Resource r = base.get();
	if(r == null)
	    return(null);
	return(r.layer(Resource.negc));
    }
    
    public void chposes(List<Indir<Resource>> poses) {
	if(poses.equals(cposes))
	    return;
	nposes = cposes = poses;
    }
    
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
