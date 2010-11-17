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
    private Map<Integer, Gob> objs = new TreeMap<Integer, Gob>();
    private Map<Integer, Integer> deleted = new TreeMap<Integer, Integer>();
    private Glob glob;
    long lastctick = 0;
	
    public OCache(Glob glob) {
	this.glob = glob;
    }
	
    public synchronized void remove(int id, int frame) {
	if(objs.containsKey(id)) {
	    objs.remove(id);
	    deleted.put(id, frame);
	}
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
	    for(Gob g : objs.values())
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
	
    public synchronized Gob getgob(int id) {
	return(objs.get(id));
    }
	
    public synchronized Gob getgob(int id, int frame) {
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
	    return(objs.get(id));
	}
	/* XXX: Clean up in deleted */
    }
    
    public synchronized void move(int id, int frame, Coord c, double a) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	g.move(c, a);
    }
	
    public synchronized void cres(int id, int frame, Indir<Resource> res, Message sdt) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	ResDrawable d = (ResDrawable)g.getattr(Drawable.class);
	if((d == null) || (d.res != res) || (d.sdt.blob.length > 0) || (sdt.blob.length > 0)) {
	    g.setattr(new ResDrawable(g, res, sdt));
	}
    }
	
    public synchronized void linbeg(int id, int frame, Coord s, Coord t, int c) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	LinMove lm = new LinMove(g, s, t, c);
	g.setattr(lm);
    }
	
    public synchronized void linstep(int id, int frame, int l) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	Moving m = g.getattr(Moving.class);
	if((m == null) || !(m instanceof LinMove))
	    return;
	LinMove lm = (LinMove)m;
	if((l < 0) || (l >= lm.c))
	    g.delattr(Moving.class);
	else
	    lm.setl(l);
    }
	
    public synchronized void speak(int id, int frame, Coord off, String text) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	if(text.length() < 1) {
	    g.delattr(Speaking.class);
	} else {
	    Speaking m = g.getattr(Speaking.class);
	    if(m == null) {
		g.setattr(new Speaking(g, off, text));
	    } else {
		m.off = off;
		m.update(text);
	    }
	}
    }
    
    public synchronized void composite(int id, int frame, Indir<Resource> base, List<Indir<Resource>> poses, List<Composite.MD> mod, List<Composite.ED> equ) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	Composite cmp = (Composite)g.getattr(Drawable.class);
	if(cmp == null) {
	    cmp = new Composite(g, base);
	    g.setattr(cmp);
	}
	if(poses != null)
	    cmp.chposes(poses);
	if(mod != null)
	    cmp.chmod(mod);
	if(equ != null)
	    cmp.chequ(equ);
    }
    
    public synchronized void avatar(int id, int frame, List<Indir<Resource>> layers) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	Avatar ava = g.getattr(Avatar.class);
	if(ava == null) {
	    ava = new Avatar(g);
	    g.setattr(ava);
	}
	ava.setlayers(layers);
    }
	
    public synchronized void drawoff(int id, int frame, Coord off) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
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
	
    public synchronized void lumin(int id, int frame, Coord off, int sz, int str) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	g.setattr(new Lumin(g, off, sz, str));
    }
	
    public synchronized void follow(int id, int frame, int oid, Coord off, int szo) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	if(oid == -1) {
	    g.delattr(Following.class);
	} else {
	    Following flw = g.getattr(Following.class);
	    if(flw == null) {
		flw = new Following(g, oid, off, szo);
		g.setattr(flw);
	    } else {
		flw.tgt = oid;
		flw.doff = off;
		flw.szo = szo;
	    }
	}
    }

    public synchronized void homostop(int id, int frame) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	g.delattr(Homing.class);
    }

    public synchronized void homing(int id, int frame, int oid, Coord tc, int v) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	g.setattr(new Homing(g, oid, tc, v));
    }
	
    public synchronized void homocoord(int id, int frame, Coord tc, int v) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	Homing homo = g.getattr(Homing.class);
	if(homo != null) {
	    homo.tc = tc;
	    homo.v = v;
	}
    }
	
    public synchronized void overlay(int id, int frame, int olid, boolean prs, Indir<Resource> resid, Message sdt) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
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

    public synchronized void health(int id, int frame, int hp) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	g.setattr(new GobHealth(g, hp));
    }
	
    public synchronized void buddy(int id, int frame, String name, int group, int type) {
	Gob g = getgob(id, frame);
	if(g == null)
	    return;
	if((name.length() == 0) && (group == 0) && (type == 0)) {
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
