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
import haven.Skeleton.TrackMod;

public class Composite extends Drawable {
    private final Indir<Resource> base;
    private Skeleton skel;
    private Pose pose;
    private Collection<Model> mod = new LinkedList<Model>();
    private TrackMod[] mods = new TrackMod[0];
    private boolean stat = true;
    private List<Indir<Resource>> nposes = null;
    private List<MD> nmod = null;
    private List<ED> nequ = null;
    
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
    
    private class Equ {
    }

    public static class MD {
	public Indir<Resource> mod;
	public List<Indir<Resource>> tex;
	private Model real;
	
	public MD(Indir<Resource> mod, List<Indir<Resource>> tex) {
	    this.mod = mod;
	    this.tex = tex;
	}
    }
    
    public static class ED {
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

    private void changes() {
	if(nposes != null) {
	    nposes();
	}
	if(nmod != null)
	    nmod();
    }

    public void setup(RenderList rl) {
	init();
	if(skel == null)
	    return;
	changes();
	for(Model mod : this.mod)
	    rl.add(mod, null);
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
	nposes = poses;
    }
    
    public void chmod(List<MD> mod) {
	this.mod = new LinkedList<Model>();
	nmod = mod;
    }
    
    public void chequ(List<ED> equ) {
	nequ = equ;
    }
}
