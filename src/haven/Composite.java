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
import static haven.Composited.ED;
import static haven.Composited.MD;

public class Composite extends Drawable {
    public final static float ipollen = 0.2f;
    public final Indir<Resource> base;
    public Composited comp;
    private List<Indir<Resource>> nposes = null, tposes = null;
    private float tptime;
    private WrapMode tpmode;
    public int pseq;
    private List<MD> nmod;
    private List<ED> nequ;
    
    public Composite(Gob gob, Indir<Resource> base) {
	super(gob);
	this.base = base;
    }
    
    private void init() {
	if(comp != null)
	    return;
	comp = new Composited(base.get().layer(Skeleton.Res.class).s);
    }
    
    public void setup(RenderList rl) {
	try {
	    init();
	} catch(Loading e) {
	    return;
	}
	rl.add(comp, null);
    }
	
    private static List<TrackMod> loadposes(List<Indir<Resource>> rl, Skeleton skel, WrapMode mode) {
	List<TrackMod> mods = new ArrayList<TrackMod>(rl.size());
	for(Indir<Resource> res : rl) {
	    for(Skeleton.ResPose p : res.get().layers(Skeleton.ResPose.class))
		mods.add(p.forskel(skel, (mode == null)?p.defmode:mode));
	}
	return(mods);
    }

    public void ctick(int dt) {
	if(comp == null)
	    return;
	if(nposes != null) {
	    try {
		Composited.Poses np = comp.new Poses(loadposes(nposes, comp.skel, null));
		np.set(ipollen);
		nposes = null;
	    } catch(Loading e) {}
	} else if(tposes != null) {
	    try {
		final Composited.Poses cp = comp.poses;
		Composited.Poses np = comp.new Poses(loadposes(tposes, comp.skel, tpmode)) {
			protected void done() {
			    cp.set(ipollen);
			}
		    };
		np.limit = tptime;
		np.set(ipollen);
		tposes = null;
	    } catch(Loading e) {}
	}
	if(nmod != null) {
	    comp.chmod(nmod);
	    nmod = null;
	}
	if(nequ != null) {
	    comp.chequ(nequ);
	    nequ = null;
	}
	Moving mv = gob.getattr(Moving.class);
	double v = 0;
	if(mv != null)
	    v = mv.getv();
	comp.tick(dt, v);
    }

    public Resource.Neg getneg() {
	return(base.get().layer(Resource.negc));
    }
    
    public Pose getpose() {
	init();
	return(comp.pose);
    }
    
    public void chposes(List<Indir<Resource>> poses, boolean interp) {
	if(tposes != null)
	    tposes = null;
	nposes = poses;
    }
    
    public void tposes(List<Indir<Resource>> poses, WrapMode mode, float time) {
	this.tposes = poses;
	this.tpmode = mode;
	this.tptime = time;
    }
    
    public void chmod(List<MD> mod) {
	nmod = mod;
    }

    public void chequ(List<ED> equ) {
	nequ = equ;
    }
}
