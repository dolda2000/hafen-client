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

public class OCache implements Iterable<Gob> {
    /* XXX: Use weak refs */
    private Collection<Collection<Gob>> local = new LinkedList<Collection<Gob>>();
    private Map<Long, Gob> objs = new TreeMap<Long, Gob>();
    private Map<Long, Integer> deleted = new TreeMap<Long, Integer>();
    private Glob glob;
    long lastctick = 0;
	
    public OCache(Glob glob) {
	this.glob = glob;
    }
	
    public synchronized void remove(long id, int frame) {
	if(objs.containsKey(id)) {
	    if(!deleted.containsKey(id) || deleted.get(id) < frame) {
		objs.remove(id);
		deleted.put(id, frame);
	    }
	}
    }
    
    public synchronized void remove(long id) {
	objs.remove(id);
    }
	
    public synchronized void tick() {
	for(Gob g : objs.values()) {
	    g.tick();
	}
    }
	
    public void ctick() {
	long now;
	int dt;
		
	now = System.currentTimeMillis();
	if(lastctick == 0)
	    dt = 0;
	else
	    dt = (int)(System.currentTimeMillis() - lastctick);
	synchronized(this) {
	    ArrayList<Gob> copy = new ArrayList<Gob>();
	    for(Gob g : this)
		copy.add(g);
	    for(Gob g : copy)
		g.ctick(dt);
	}
	lastctick = now; 
    }
	
    @SuppressWarnings("unchecked")
    public Iterator<Gob> iterator() {
	Collection<Iterator<Gob>> is = new LinkedList<Iterator<Gob>>();
	for(Collection<Gob> gc : local)
	    is.add(gc.iterator());
	return(new I2<Gob>(objs.values().iterator(), new I2<Gob>(is)));
    }
	
    public synchronized void ladd(Collection<Gob> gob) {
	local.add(gob);
    }
	
    public synchronized void lrem(Collection<Gob> gob) {
	local.remove(gob);
    }
	
    public synchronized Gob getgob(long id) {
	return(objs.get(id));
    }
	
    public synchronized Gob getgob(long id, int frame) {
	if(!objs.containsKey(id)) {
	    boolean r = false;
	    if(deleted.containsKey(id)) {
		if(deleted.get(id) < frame)
		    deleted.remove(id);
		else
		    r = true;
	    }
	    if(r) {
		return(null);
	    } else {
		Gob g = new Gob(glob, Coord.z, id, frame);
		objs.put(id, g);
		return(g);
	    }
	} else {
	    Gob ret = objs.get(id);
	    if(ret.frame >= frame)
		return(null);
	    else
		return(ret);
	}
	/* XXX: Clean up in deleted */
    }
    
    public synchronized void move(Gob g, Coord c, double a) {
	g.move(c, a);
    }
	
    public synchronized void cres(Gob g, Indir<Resource> res, Message sdt) {
	ResDrawable d = (ResDrawable)g.getattr(Drawable.class);
	if((d == null) || (d.res != res) || (d.sdt.blob.length > 0) || (sdt.blob.length > 0)) {
	    g.setattr(new ResDrawable(g, res, sdt));
	}
    }
	
    public synchronized void linbeg(Gob g, Coord s, Coord t, int c) {
	LinMove lm = new LinMove(g, s, t, c);
	g.setattr(lm);
    }
	
    public synchronized void linstep(Gob g, int l) {
	Moving m = g.getattr(Moving.class);
	if((m == null) || !(m instanceof LinMove))
	    return;
	LinMove lm = (LinMove)m;
	if((l < 0) || (l >= lm.c))
	    g.delattr(Moving.class);
	else
	    lm.setl(l);
    }
	
    public synchronized void speak(Gob g, float zo, String text) {
	if(text.length() < 1) {
	    g.delattr(Speaking.class);
	} else {
	    Speaking m = g.getattr(Speaking.class);
	    if(m == null) {
		g.setattr(new Speaking(g, zo, text));
	    } else {
		m.zo = zo;
		m.update(text);
	    }
	}
    }
    
    public synchronized void composite(Gob g, Indir<Resource> base) {
	Composite cmp = (Composite)g.getattr(Drawable.class);
	if((cmp == null) || !cmp.base.equals(base)) {
	    cmp = new Composite(g, base);
	    g.setattr(cmp);
	}
    }
    
    public synchronized void cmppose(Gob g, int pseq, List<Indir<Resource>> poses, List<Indir<Resource>> tposes, boolean interp, float ttime) {
	Composite cmp = (Composite)g.getattr(Drawable.class);
	if(cmp.pseq != pseq) {
	    cmp.pseq = pseq;
	    if(poses != null)
		cmp.chposes(poses, interp);
	    if(tposes != null)
		cmp.tposes(tposes, WrapMode.ONCE, ttime);
	}
    }
    
    public synchronized void cmpmod(Gob g, List<Composite.MD> mod) {
	Composite cmp = (Composite)g.getattr(Drawable.class);
	cmp.chmod(mod);
    }
    
    public synchronized void cmpequ(Gob g, List<Composite.ED> equ) {
	Composite cmp = (Composite)g.getattr(Drawable.class);
	cmp.chequ(equ);
    }
    
    public synchronized void avatar(Gob g, List<Indir<Resource>> layers) {
	Avatar ava = g.getattr(Avatar.class);
	if(ava == null) {
	    ava = new Avatar(g);
	    g.setattr(ava);
	}
	ava.setlayers(layers);
    }
	
    public synchronized void drawoff(Gob g, Coord off) {
	if((off.x == 0) && (off.y == 0)) {
	    g.delattr(DrawOffset.class);
	} else {
	    DrawOffset dro = g.getattr(DrawOffset.class);
	    if(dro == null) {
		dro = new DrawOffset(g, off);
		g.setattr(dro);
	    } else {
		dro.off = off;
	    }
	}
    }
	
    public synchronized void lumin(Gob g, Coord off, int sz, int str) {
	g.setattr(new Lumin(g, off, sz, str));
    }
	
    public synchronized void follow(Gob g, long oid, Indir<Resource> xfres, String xfname) {
	if(oid == 0xffffffffl) {
	    g.delattr(Following.class);
	} else {
	    Following flw = g.getattr(Following.class);
	    if(flw == null) {
		flw = new Following(g, oid, xfres, xfname);
		g.setattr(flw);
	    } else {
		synchronized(flw) {
		    flw.tgt = oid;
		    flw.xfres = xfres;
		    flw.xfname = xfname;
		    flw.lxfb = null;
		    flw.xf = null;
		}
	    }
	}
    }

    public synchronized void homostop(Gob g) {
	g.delattr(Homing.class);
    }

    public synchronized void homing(Gob g, long oid, Coord tc, int v) {
	g.setattr(new Homing(g, oid, tc, v));
    }
	
    public synchronized void homocoord(Gob g, Coord tc, int v) {
	Homing homo = g.getattr(Homing.class);
	if(homo != null) {
	    homo.tc = tc;
	    homo.v = v;
	}
    }
	
    public synchronized void overlay(Gob g, int olid, boolean prs, Indir<Resource> resid, Message sdt) {
	Gob.Overlay ol = g.findol(olid);
	if(resid != null) {
	    if(ol == null) {
		g.ols.add(ol = new Gob.Overlay(olid, resid, sdt));
	    } else if(!ol.sdt.equals(sdt)) {
		g.ols.remove(ol);
		g.ols.add(ol = new Gob.Overlay(olid, resid, sdt));
	    }
	    ol.delign = prs;
	} else {
	    if((ol != null) && (ol.spr instanceof Gob.Overlay.CDel))
		((Gob.Overlay.CDel)ol.spr).delete();
	    else
		g.ols.remove(ol);
	}
    }

    public synchronized void health(Gob g, int hp) {
	g.setattr(new GobHealth(g, hp));
    }
	
    public synchronized void buddy(Gob g, String name, int group, int type) {
	if(name == null) {
	    g.delattr(KinInfo.class);
	} else {
	    KinInfo b = g.getattr(KinInfo.class);
	    if(b == null) {
		g.setattr(new KinInfo(g, name, group, type));
	    } else {
		b.update(name, group, type);
	    }
	}
    }
}
