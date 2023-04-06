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
import java.lang.reflect.*;
import haven.render.*;
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;
import static haven.Composited.ED;
import static haven.Composited.MD;

public class Composite extends Drawable implements EquipTarget {
    public final static float ipollen = 0.2f;
    public final Indir<Resource> base;
    public final Composited comp;
    public int pseq;
    public List<MD> nmod;
    public List<ED> nequ;
    private Collection<ResData> nposes = null, tposes = null;
    private boolean nposesold, retainequ = false;
    private float tptime;
    private WrapMode tpmode;
    
    public Composite(Gob gob, Indir<Resource> base) {
	super(gob);
	this.base = base;
	comp = new Composited(base.get().layer(Skeleton.Res.class).s);
	comp.eqowner = gob;
    }
    
    public void added(RenderTree.Slot slot) {
	slot.add(comp);
	super.added(slot);
    }

    public static List<PoseMod> loadposes(Collection<ResData> rl, Skeleton.ModOwner owner, Skeleton skel, boolean old) {
	List<PoseMod> mods = new ArrayList<PoseMod>(rl.size());
	for(ResData dat : rl) {
	    PoseMod mod = skel.mkposemod(owner, dat.res.get(), dat.sdt.clone());
	    if(old)
		mod.age();
	    mods.add(mod);
	}
	return(mods);
    }

    private List<PoseMod> loadposes(Collection<ResData> rl, Skeleton skel, boolean old) {
	return(loadposes(rl, gob, skel, old));
    }

    private List<PoseMod> loadposes(Collection<ResData> rl, Skeleton skel, WrapMode mode) {
	List<PoseMod> mods = new ArrayList<PoseMod>(rl.size());
	for(ResData dat : rl) {
	    for(Skeleton.ResPose p : dat.res.get().layers(Skeleton.ResPose.class))
		mods.add(p.forskel(gob, skel, (mode == null)?p.defmode:mode));
	}
	return(mods);
    }

    private void updequ() {
	retainequ = false;
	if(nmod != null) {
	    try {
		comp.chmod(nmod);
		nmod = null;
	    } catch(Loading l) {
	    }
	}
	if(nequ != null) {
	    try {
		comp.chequ(nequ);
		nequ = null;
	    } catch(Loading l) {
	    }
	}
    }

    public void ctick(double dt) {
	if(nposes != null) {
	    try {
		Composited.Poses np = comp.new Poses(loadposes(nposes, comp.skel, nposesold));
		np.set(nposesold?0:ipollen);
		nposes = null;
		updequ();
	    } catch(Loading e) {}
	} else if(tposes != null) {
	    try {
		final Composited.Poses cp = comp.poses;
		Composited.Poses np = comp.new Poses(loadposes(tposes, comp.skel, tpmode)) {
			protected void done() {
			    cp.set(ipollen);
			    updequ();
			}
		    };
		np.limit = tptime;
		np.set(ipollen);
		tposes = null;
		retainequ = true;
	    } catch(Loading e) {}
	} else if(!retainequ) {
	    updequ();
	}
	comp.tick(dt);
    }

    public void gtick(Render g) {
	comp.gtick(g);
    }

    public Resource getres() {
	return(base.get());
    }
    
    public Pose getpose() {
	return(comp.pose);
    }

    public Supplier<Pipe.Op> eqpoint(String nm, Message dat) {
	return(comp.eqpoint(nm, dat));
    }
    
    public void chposes(Collection<ResData> poses, boolean interp) {
	if(tposes != null)
	    tposes = null;
	nposes = poses;
	nposesold = !interp;
    }
    
    public void tposes(Collection<ResData> poses, WrapMode mode, float time) {
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

    public Object staticp() {
	return(null);
    }

    @OCache.DeltaType(OCache.OD_COMPOSE)
    public static class $composite implements OCache.Delta {
	public void apply(Gob g, Message msg) {
	    Indir<Resource> base = OCache.Delta.getres(g, msg.uint16());
	    Drawable dr = g.getattr(Drawable.class);
	    Composite cmp = (dr instanceof Composite)?(Composite)dr:null;
	    if((cmp == null) || !cmp.base.equals(base)) {
		cmp = new Composite(g, base);
		g.setattr(cmp);
	    }
	}
    }

    @OCache.DeltaType(OCache.OD_CMPPOSE)
    public static class $cmppose implements OCache.Delta {
	public void apply(Gob g, Message msg) {
	    List<ResData> poses = null, tposes = null;
	    int pfl = msg.uint8();
	    int pseq = msg.uint8();
	    boolean interp = (pfl & 1) != 0;
	    if((pfl & 2) != 0) {
		poses = new LinkedList<ResData>();
		while(true) {
		    int resid = msg.uint16();
		    if(resid == 65535)
			break;
		    Message sdt = Message.nil;
		    if((resid & 0x8000) != 0) {
			resid &= ~0x8000;
			sdt = new MessageBuf(msg.bytes(msg.uint8()));
		    }
		    poses.add(new ResData(OCache.Delta.getres(g, resid), sdt));
		}
	    }
	    float ttime = 0;
	    if((pfl & 4) != 0) {
		tposes = new LinkedList<ResData>();
		while(true) {
		    int resid = msg.uint16();
		    if(resid == 65535)
			break;
		    Message sdt = Message.nil;
		    if((resid & 0x8000) != 0) {
			resid &= ~0x8000;
			sdt = new MessageBuf(msg.bytes(msg.uint8()));
		    }
		    tposes.add(new ResData(OCache.Delta.getres(g, resid), sdt));
		}
		ttime = (msg.uint8() / 10.0f);
	    }
	    List<ResData> cposes = poses, ctposes = tposes;
	    float cttime = ttime;
	    Composite cmp = (Composite)g.getattr(Drawable.class);
	    if(cmp == null)
		throw(new RuntimeException(String.format("cmppose on non-composed object: %s %s %s %s", poses, tposes, interp, ttime)));
	    if(cmp.pseq != pseq) {
		cmp.pseq = pseq;
		if(poses != null)
		    cmp.chposes(poses, interp);
		if(tposes != null)
		    cmp.tposes(tposes, WrapMode.ONCE, ttime);
	    }
	}
    }

    @OCache.DeltaType(OCache.OD_CMPMOD)
    public static class $cmpmod implements OCache.Delta {
	public void apply(Gob g, Message msg) {
	    List<Composited.MD> mod = new LinkedList<Composited.MD>();
	    int mseq = 0;
	    while(true) {
		int modid = msg.uint16();
		if(modid == 65535)
		    break;
		Indir<Resource> modr = OCache.Delta.getres(g, modid);
		List<ResData> tex = new LinkedList<ResData>();
		while(true) {
		    int resid = msg.uint16();
		    if(resid == 65535)
			break;
		    Message sdt = Message.nil;
		    if((resid & 0x8000) != 0) {
			resid &= ~0x8000;
			sdt = new MessageBuf(msg.bytes(msg.uint8()));
		    }
		    tex.add(new ResData(OCache.Delta.getres(g, resid), sdt));
		}
		Composited.MD md = new Composited.MD(modr, tex);
		md.id = mseq++;
		mod.add(md);
	    }
	    Composite cmp = (Composite)g.getattr(Drawable.class);
	    if(cmp == null)
		throw(new RuntimeException(String.format("cmpmod on non-composed object: %s", mod)));
	    cmp.chmod(mod);
	}
    }

    @OCache.DeltaType(OCache.OD_CMPEQU)
    public static class $cmpequ implements OCache.Delta {
	public void apply(Gob g, Message msg) {
	    List<Composited.ED> equ = new LinkedList<Composited.ED>();
	    int eseq = 0;
	    while(true) {
		int h = msg.uint8();
		if(h == 255)
		    break;
		int ef = h & 0x80;
		int et = h & 0x7f;
		String at = msg.string();
		Indir<Resource> res;
		int resid = msg.uint16();
		Message sdt = Message.nil;
		if((resid & 0x8000) != 0) {
		    resid &= ~0x8000;
		    sdt = new MessageBuf(msg.bytes(msg.uint8()));
		}
		res = OCache.Delta.getres(g, resid);
		Coord3f off;
		if((ef & 128) != 0) {
		    int x = msg.int16(), y = msg.int16(), z = msg.int16();
		    off = Coord3f.of(x / 1000.0f, y / 1000.0f, z / 1000.0f);
		} else {
		    off = Coord3f.o;
		}
		Composited.ED ed = new Composited.ED(et, at, new ResData(res, sdt), off);
		ed.id = eseq++;
		equ.add(ed);
	    }
	    Composite cmp = (Composite)g.getattr(Drawable.class);
	    if(cmp == null)
		throw(new RuntimeException(String.format("cmpequ on non-composed object: %s", equ)));
	    cmp.chequ(equ);
	}
    }
}
