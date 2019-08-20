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
    public static final int OD_REM = 0;
    public static final int OD_MOVE = 1;
    public static final int OD_RES = 2;
    public static final int OD_LINBEG = 3;
    public static final int OD_LINSTEP = 4;
    public static final int OD_SPEECH = 5;
    public static final int OD_COMPOSE = 6;
    public static final int OD_ZOFF = 7;
    public static final int OD_LUMIN = 8;
    public static final int OD_AVATAR = 9;
    public static final int OD_FOLLOW = 10;
    public static final int OD_HOMING = 11;
    public static final int OD_OVERLAY = 12;
    /* public static final int OD_AUTH = 13; -- Removed */
    public static final int OD_HEALTH = 14;
    public static final int OD_BUDDY = 15;
    public static final int OD_CMPPOSE = 16;
    public static final int OD_CMPMOD = 17;
    public static final int OD_CMPEQU = 18;
    public static final int OD_ICON = 19;
    public static final int OD_RESATTR = 20;
    public static final int OD_END = 255;
    public static final Coord2d posres = new Coord2d(0x1.0p-10, 0x1.0p-10).mul(11, 11);
    /* XXX: Use weak refs */
    private Collection<Collection<Gob>> local = new LinkedList<Collection<Gob>>();
    private Map<Long, Gob> objs = new TreeMap<Long, Gob>();
    private Map<Long, Integer> deleted = new TreeMap<Long, Integer>();
    private Glob glob;
    private final Collection<ChangeCallback> cbs = new WeakList<ChangeCallback>();

    public interface ChangeCallback {
	public void changed(Gob ob);
	public void removed(Gob ob);
    }

    public OCache(Glob glob) {
	this.glob = glob;
    }

    public synchronized void callback(ChangeCallback cb) {
	cbs.add(cb);
    }

    public void changed(Gob ob) {
	ob.changed();
	for(ChangeCallback cb : cbs)
	    cb.changed(ob);
    }

    public synchronized void remove(long id, int frame) {
	if(objs.containsKey(id)) {
	    if(!deleted.containsKey(id) || deleted.get(id) < frame) {
		Gob old = objs.remove(id);
		deleted.put(id, frame);
		old.dispose();
		for(ChangeCallback cb : cbs)
		    cb.removed(old);
	    }
	}
    }
    
    public synchronized void remove(long id) {
	Gob old = objs.remove(id);
	if(old != null) {
	    for(ChangeCallback cb : cbs)
		cb.removed(old);
	}
    }
	
    public synchronized void tick() {
	for(Gob g : objs.values()) {
	    g.tick();
	}
    }
	
    public void ctick(int dt) {
	synchronized(this) {
	    ArrayList<Gob> copy = new ArrayList<Gob>();
	    for(Gob g : this)
		copy.add(g);
	    for(Gob g : copy)
		g.ctick(dt);
	}
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
	for(Gob g : gob) {
	    for(ChangeCallback cb : cbs)
		cb.changed(g);
	}
    }
	
    public synchronized void lrem(Collection<Gob> gob) {
	local.remove(gob);
	for(Gob g : gob) {
	    for(ChangeCallback cb : cbs)
		cb.removed(g);
	}
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
		Gob g = new Gob(glob, Coord2d.z, id, frame);
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

    private long nextvirt = -1;
    public class Virtual extends Gob {
	public Virtual(Coord2d c, double a) {
	    super(OCache.this.glob, c, nextvirt--, 0);
	    this.a = a;
	    virtual = true;
	    synchronized(OCache.this) {
		objs.put(id, this);
		OCache.this.changed(this);
	    }
	}
    }
    
    private Indir<Resource> getres(int id) {
	return(glob.sess.getres(id));
    }

    public synchronized void move(Gob g, Coord2d c, double a) {
	g.move(c, a);
	changed(g);
    }
    public void move(Gob gob, Message msg) {
	Coord2d c = msg.coord().mul(posres);
	int ia = msg.uint16();
	if(gob != null)
	    move(gob, c, (ia / 65536.0) * Math.PI * 2);
    }
	
    public synchronized void cres(Gob g, Indir<Resource> res, Message dat) {
	MessageBuf sdt = new MessageBuf(dat);
	Drawable dr = g.getattr(Drawable.class);
	ResDrawable d = (dr instanceof ResDrawable)?(ResDrawable)dr:null;
	if((d != null) && (d.res == res) && !d.sdt.equals(sdt) && (d.spr != null) && (d.spr instanceof Gob.Overlay.CUpd)) {
	    ((Gob.Overlay.CUpd)d.spr).update(sdt);
	    d.sdt = sdt;
	} else if((d == null) || (d.res != res) || !d.sdt.equals(sdt)) {
	    g.setattr(new ResDrawable(g, res, sdt));
	}
	changed(g);
    }
    public void cres(Gob gob, Message msg) {
	int resid = msg.uint16();
	Message sdt = Message.nil;
	if((resid & 0x8000) != 0) {
	    resid &= ~0x8000;
	    sdt = new MessageBuf(msg.bytes(msg.uint8()));
	}
	if(gob != null)
	    cres(gob, getres(resid), sdt);
    }
	
    public synchronized void linbeg(Gob g, Coord2d s, Coord2d v) {
	LinMove lm = g.getattr(LinMove.class);
	if((lm == null) || !lm.s.equals(s) || !lm.v.equals(v)) {
	    g.setattr(new LinMove(g, s, v));
	    changed(g);
	}
    }
    public void linbeg(Gob gob, Message msg) {
	Coord2d s = msg.coord().mul(posres);
	Coord2d v = msg.coord().mul(posres);
	if(gob != null)
	    linbeg(gob, s, v);
    }
	
    public synchronized void linstep(Gob g, double t, double e) {
	Moving m = g.getattr(Moving.class);
	if((m == null) || !(m instanceof LinMove))
	    return;
	LinMove lm = (LinMove)m;
	if(t < 0)
	    g.delattr(Moving.class);
	else
	    lm.sett(t);
	if(e >= 0)
	    lm.e = e;
	else
	    lm.e = Double.NaN;
    }
    public void linstep(Gob gob, Message msg) {
	double t, e;
	int w = msg.int32();
	if(w == -1) {
	    t = e = -1;
	} else if((w & 0x80000000) == 0) {
	    t = w * 0x1p-10;
	    e = -1;
	} else {
	    t = (w & ~0x80000000) * 0x1p-10;
	    w = msg.int32();
	    e = (w < 0)?-1:(w * 0x1p-10);
	}
	if(gob != null)
	    linstep(gob, t, e);
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
	changed(g);
    }
    public void speak(Gob gob, Message msg) {
	float zo = msg.int16() / 100.0f;
	String text = msg.string();
	if(gob != null)
	    speak(gob, zo, text);
    }
    
    public synchronized void composite(Gob g, Indir<Resource> base) {
	Drawable dr = g.getattr(Drawable.class);
	Composite cmp = (dr instanceof Composite)?(Composite)dr:null;
	if((cmp == null) || !cmp.base.equals(base)) {
	    cmp = new Composite(g, base);
	    g.setattr(cmp);
	}
	changed(g);
    }
    public void composite(Gob gob, Message msg) {
	Indir<Resource> base = getres(msg.uint16());
	if(gob != null)
	    composite(gob, base);
    }
    
    public synchronized void cmppose(Gob g, int pseq, List<ResData> poses, List<ResData> tposes, boolean interp, float ttime) {
	Composite cmp = (Composite)g.getattr(Drawable.class);
	if(cmp.pseq != pseq) {
	    cmp.pseq = pseq;
	    if(poses != null)
		cmp.chposes(poses, interp);
	    if(tposes != null)
		cmp.tposes(tposes, WrapMode.ONCE, ttime);
	}
	changed(g);
    }
    public void cmppose(Gob gob, Message msg) {
	List<ResData> poses = null, tposes = null;
	int pfl = msg.uint8();
	int seq = msg.uint8();
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
		poses.add(new ResData(getres(resid), sdt));
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
		tposes.add(new ResData(getres(resid), sdt));
	    }
	    ttime = (msg.uint8() / 10.0f);
	}
	if(gob != null)
	    cmppose(gob, seq, poses, tposes, interp, ttime);
    }
    
    public synchronized void cmpmod(Gob g, List<Composited.MD> mod) {
	Composite cmp = (Composite)g.getattr(Drawable.class);
	cmp.chmod(mod);
	changed(g);
    }
    public void cmpmod(Gob gob, Message msg) {
	List<Composited.MD> mod = new LinkedList<Composited.MD>();
	int mseq = 0;
	while(true) {
	    int modid = msg.uint16();
	    if(modid == 65535)
		break;
	    Indir<Resource> modr = getres(modid);
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
		tex.add(new ResData(getres(resid), sdt));
	    }
	    Composited.MD md = new Composited.MD(modr, tex);
	    md.id = mseq++;
	    mod.add(md);
	}
	if(gob != null)
	    cmpmod(gob, mod);
    }
    
    public synchronized void cmpequ(Gob g, List<Composited.ED> equ) {
	Composite cmp = (Composite)g.getattr(Drawable.class);
	cmp.chequ(equ);
	changed(g);
    }
    public void cmpequ(Gob gob, Message msg) {
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
	    res = getres(resid);
	    Coord3f off;
	    if((ef & 128) != 0) {
		int x = msg.int16(), y = msg.int16(), z = msg.int16();
		off = new Coord3f(x / 1000.0f, y / 1000.0f, z / 1000.0f);
	    } else {
		off = Coord3f.o;
	    }
	    Composited.ED ed = new Composited.ED(et, at, new ResData(res, sdt), off);
	    ed.id = eseq++;
	    equ.add(ed);
	}
	if(gob != null)
	    cmpequ(gob, equ);
    }
    
    public synchronized void avatar(Gob g, List<Indir<Resource>> layers) {
	Avatar ava = g.getattr(Avatar.class);
	if(ava == null) {
	    ava = new Avatar(g);
	    g.setattr(ava);
	}
	ava.setlayers(layers);
	changed(g);
    }
    public void avatar(Gob gob, Message msg) {
	List<Indir<Resource>> layers = new LinkedList<Indir<Resource>>();
	while(true) {
	    int layer = msg.uint16();
	    if(layer == 65535)
		break;
	    layers.add(getres(layer));
	}
	if(gob != null)
	    avatar(gob, layers);
    }
	
    public synchronized void zoff(Gob g, float off) {
	if(off == 0) {
	    g.delattr(DrawOffset.class);
	} else {
	    DrawOffset dro = g.getattr(DrawOffset.class);
	    if(dro == null) {
		dro = new DrawOffset(g, new Coord3f(0, 0, off));
		g.setattr(dro);
	    } else {
		dro.off = new Coord3f(0, 0, off);
	    }
	}
	changed(g);
    }
    public void zoff(Gob gob, Message msg) {
	float off = msg.int16() / 100.0f;
	if(gob != null)
	    zoff(gob, off);
    }
	
    public synchronized void lumin(Gob g, Coord off, int sz, int str) {
	g.setattr(new Lumin(g, off, sz, str));
	changed(g);
    }
    public void lumin(Gob gob, Message msg) {
	Coord off = msg.coord();
	int sz = msg.uint16();
	int str = msg.uint8();
	if(gob != null)
	    lumin(gob, off, sz, str);
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
	changed(g);
    }
    public void follow(Gob gob, Message msg) {
	long oid = msg.uint32();
	Indir<Resource> xfres = null;
	String xfname = null;
	if(oid != 0xffffffffl) {
	    xfres = getres(msg.uint16());
	    xfname = msg.string();
	}
	if(gob != null)
	    follow(gob, oid, xfres, xfname);
    }

    public synchronized void homostop(Gob g) {
	g.delattr(Homing.class);
	changed(g);
    }
    public synchronized void homing(Gob g, long oid, Coord2d tc, double v) {
	Homing homo = g.getattr(Homing.class);
	if((homo == null) || (homo.tgt != oid)) {
	    g.setattr(new Homing(g, oid, tc, v));
	} else {
	    homo.tc = tc;
	    homo.v = v;
	}
	changed(g);
    }
    public void homing(Gob gob, Message msg) {
	long oid = msg.uint32();
	if(oid == 0xffffffffl) {
	    if(gob != null)
		homostop(gob);
	} else {
	    Coord2d tgtc = msg.coord().mul(posres);
	    double v = msg.int32() * 0x1p-10 * 11;
	    if(gob != null)
		homing(gob, oid, tgtc, v);
	}
    }
	
    public synchronized void overlay(Gob g, int olid, boolean prs, Indir<Resource> resid, Message sdt) {
	Gob.Overlay ol = g.findol(olid);
	if(resid != null) {
	    sdt = new MessageBuf(sdt);
	    if(ol == null) {
		g.ols.add(ol = new Gob.Overlay(olid, resid, sdt));
	    } else if(!ol.sdt.equals(sdt)) {
		if(ol.spr instanceof Gob.Overlay.CUpd) {
		    ol.sdt = new MessageBuf(sdt);
		    ((Gob.Overlay.CUpd)ol.spr).update(ol.sdt);
		} else {
		    g.ols.remove(ol);
		    g.ols.add(ol = new Gob.Overlay(olid, resid, sdt));
		}
	    }
	    ol.delign = prs;
	} else {
	    if((ol != null) && (ol.spr instanceof Gob.Overlay.CDel))
		((Gob.Overlay.CDel)ol.spr).delete();
	    else
		g.ols.remove(ol);
	}
	changed(g);
    }
    public void overlay(Gob gob, Message msg) {
	int olid = msg.int32();
	boolean prs = (olid & 1) != 0;
	olid >>>= 1;
	int resid = msg.uint16();
	Indir<Resource> res;
	Message sdt = Message.nil;
	if(resid == 65535) {
	    res = null;
	} else {
	    if((resid & 0x8000) != 0) {
		resid &= ~0x8000;
		sdt = new MessageBuf(msg.bytes(msg.uint8()));
	    }
	    res = getres(resid);
	}
	if(gob != null)
	    overlay(gob, olid, prs, res, sdt);
    }

    public synchronized void health(Gob g, int hp) {
	g.setattr(new GobHealth(g, hp));
	changed(g);
    }
    public void health(Gob gob, Message msg) {
	int hp = msg.uint8();
	if(gob != null)
	    health(gob, hp);
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
	changed(g);
    }
    public void buddy(Gob gob, Message msg) {
	String name = msg.string();
	if(name.length() > 0) {
	    int group = msg.uint8();
	    int btype = msg.uint8();
	    if(gob != null)
		buddy(gob, name, group, btype);
	} else {
	    if(gob != null)
		buddy(gob, null, 0, 0);
	}
    }

    public synchronized void icon(Gob g, Indir<Resource> res) {
	if(res == null)
	    g.delattr(GobIcon.class);
	else
	    g.setattr(new GobIcon(g, res));
	changed(g);
    }
    public void icon(Gob gob, Message msg) {
	int resid = msg.uint16();
	Indir<Resource> res;
	if(resid == 65535) {
	    if(gob != null)
		icon(gob, (Indir<Resource>)null);
	} else {
	    int ifl = msg.uint8();
	    if(gob != null)
		icon(gob, getres(resid));
	}
    }

    public synchronized void resattr(Gob g, Indir<Resource> resid, Message dat) {
	if(dat != null)
	    g.setrattr(resid, dat);
	else
	    g.delrattr(resid);
	changed(g);
    }
    public void resattr(Gob gob, Message msg) {
	Indir<Resource> resid = getres(msg.uint16());
	int len = msg.uint8();
	Message dat = (len > 0)?new MessageBuf(msg.bytes(len)):null;
	if(gob != null)
	    resattr(gob, resid, dat);
    }

    public void receive(Gob gob, int type, Message msg) {
	switch(type) {
	case OD_MOVE:
	    move(gob, msg);
	    break;
	case OD_RES:
	    cres(gob, msg);
	    break;
	case OD_LINBEG:
	    linbeg(gob, msg);
	    break;
	case OD_LINSTEP:
	    linstep(gob, msg);
	    break;
	case OD_HOMING:
	    homing(gob, msg);
	    break;
	case OD_SPEECH:
	    speak(gob, msg);
	    break;
	case OD_COMPOSE:
	    composite(gob, msg);
	    break;
	case OD_CMPPOSE:
	    cmppose(gob, msg);
	    break;
	case OD_CMPMOD:
	    cmpmod(gob, msg);
	    break;
	case OD_CMPEQU:
	    cmpequ(gob, msg);
	    break;
	case OD_ZOFF:
	    zoff(gob, msg);
	    break;
	case OD_LUMIN:
	    lumin(gob, msg);
	    break;
	case OD_AVATAR:
	    avatar(gob, msg);
	    break;
	case OD_FOLLOW:
	    follow(gob, msg);
	    break;
	case OD_OVERLAY:
	    overlay(gob, msg);
	    break;
	case OD_HEALTH:
	    health(gob, msg);
	    break;
	case OD_BUDDY:
	    buddy(gob, msg);
	    break;
	case OD_ICON:
	    icon(gob, msg);
	    break;
	case OD_RESATTR:
	    resattr(gob, msg);
	    break;
	default:
	    throw(new Session.MessageException("Unknown objdelta type: " + type, msg));
	}
    }
}
