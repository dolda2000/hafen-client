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
import java.util.function.Consumer;
import haven.render.Render;

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
    private HashMultiMap<Long, Gob> objs = new HashMultiMap<Long, Gob>();
    private Glob glob;
    private final Collection<ChangeCallback> cbs = new WeakList<ChangeCallback>();

    public interface ChangeCallback {
	public void added(Gob ob);
	public void removed(Gob ob);
    }

    public OCache(Glob glob) {
	this.glob = glob;
    }

    public synchronized void callback(ChangeCallback cb) {
	cbs.add(cb);
    }

    public synchronized void uncallback(ChangeCallback cb) {
	cbs.remove(cb);
    }

    public void add(Gob ob) {
	synchronized(ob) {
	    Collection<ChangeCallback> cbs;
	    synchronized(this) {
		cbs = new ArrayList<>(this.cbs);
		objs.put(ob.id, ob);
	    }
	    for(ChangeCallback cb : cbs)
		cb.added(ob);
	}
    }

    public void remove(Gob ob) {
	Gob old;
	Collection<ChangeCallback> cbs;
	synchronized(this) {
	    old = objs.remove(ob.id, ob);
	    if((old != null) && (old != ob))
		throw(new RuntimeException(String.format("object %d removed wrong object", ob.id)));
	    cbs = new ArrayList<>(this.cbs);
	}
	if(old != null) {
	    synchronized(old) {
		for(ChangeCallback cb : cbs)
		    cb.removed(old);
	    }
	}
    }

    public void ctick(double dt) {
	ArrayList<Gob> copy = new ArrayList<Gob>();
	synchronized(this) {
	    for(Gob g : this)
		copy.add(g);
	}
	Consumer<Gob> task = g -> {
	    synchronized(g) {
		g.ctick(dt);
	    }
	};
	if(!Config.par)
	    copy.forEach(task);
	else
	    copy.parallelStream().forEach(task);
    }

    public void gtick(Render g) {
	ArrayList<Gob> copy = new ArrayList<Gob>();
	synchronized(this) {
	    for(Gob ob : this)
		copy.add(ob);
	}
	if(!Config.par) {
	    copy.forEach(ob -> {
		    synchronized(ob) {
			ob.gtick(g);
		    }
		});
	} else {
	    Collection<Render> subs = new ArrayList<>();
	    ThreadLocal<Render> subv = new ThreadLocal<>();
	    copy.parallelStream().forEach(ob -> {
		    Render sub = subv.get();
		    if(sub == null) {
			sub = g.env().render();
			synchronized(subs) {
			    subs.add(sub);
			}
			subv.set(sub);
		    }
		    synchronized(ob) {
			ob.gtick(sub);
		    }
		});
	    for(Render sub : subs)
		g.submit(sub);
	}
    }

    @SuppressWarnings("unchecked")
    public Iterator<Gob> iterator() {
	Collection<Iterator<Gob>> is = new LinkedList<Iterator<Gob>>();
	for(Collection<Gob> gc : local)
	    is.add(gc.iterator());
	return(new I2<Gob>(objs.values().iterator(), new I2<Gob>(is)));
    }

    public void ladd(Collection<Gob> gob) {
	Collection<ChangeCallback> cbs;
	synchronized(this) {
	    cbs = new ArrayList<>(this.cbs);
	    local.add(gob);
	}
	for(Gob g : gob) {
	    synchronized(g) {
		for(ChangeCallback cb : cbs)
		    cb.added(g);
	    }
	}
    }

    public void lrem(Collection<Gob> gob) {
	Collection<ChangeCallback> cbs;
	synchronized(this) {
	    cbs = new ArrayList<>(this.cbs);
	    local.remove(gob);
	}
	for(Gob g : gob) {
	    synchronized(g) {
		for(ChangeCallback cb : cbs)
		    cb.removed(g);
	    }
	}
    }

    public synchronized Gob getgob(long id) {
	return(objs.get(id));
    }

    private java.util.concurrent.atomic.AtomicLong nextvirt = new java.util.concurrent.atomic.AtomicLong(-1);
    public class Virtual extends Gob {
	public Virtual(Coord2d c, double a) {
	    super(OCache.this.glob, c, nextvirt.getAndDecrement());
	    this.a = a;
	    virtual = true;
	}
    }

    private Indir<Resource> getres(int id) {
	return(glob.sess.getres(id));
    }

    public interface Delta {
	public void apply(Gob gob);
    }

    public static void move(Gob g, Coord2d c, double a) {
	g.move(c, a);
    }
    public Delta move(Message msg) {
	Coord2d c = msg.coord().mul(posres);
	int ia = msg.uint16();
	return(gob -> move(gob, c, (ia / 65536.0) * Math.PI * 2));
    }

    public static void cres(Gob g, Indir<Resource> res, Message dat) {
	MessageBuf sdt = new MessageBuf(dat);
	Drawable dr = g.getattr(Drawable.class);
	ResDrawable d = (dr instanceof ResDrawable)?(ResDrawable)dr:null;
	if((d != null) && (d.res == res) && !d.sdt.equals(sdt) && (d.spr != null) && (d.spr instanceof Sprite.CUpd)) {
	    ((Sprite.CUpd)d.spr).update(sdt);
	    d.sdt = sdt;
	} else if((d == null) || (d.res != res) || !d.sdt.equals(sdt)) {
	    g.setattr(new ResDrawable(g, res, sdt));
	}
    }
    public Delta cres(Message msg) {
	int resid = msg.uint16();
	Message sdt = Message.nil;
	if((resid & 0x8000) != 0) {
	    resid &= ~0x8000;
	    sdt = new MessageBuf(msg.bytes(msg.uint8()));
	}
	Indir<Resource> cres = getres(resid);
	Message csdt = sdt;
	return(gob -> cres(gob, cres, csdt));
    }

    public static void linbeg(Gob g, Coord2d s, Coord2d v) {
	LinMove lm = g.getattr(LinMove.class);
	if((lm == null) || !lm.s.equals(s) || !lm.v.equals(v)) {
	    g.setattr(new LinMove(g, s, v));
	}
    }
    public Delta linbeg(Message msg) {
	Coord2d s = msg.coord().mul(posres);
	Coord2d v = msg.coord().mul(posres);
	return(gob -> linbeg(gob, s, v));
    }

    public static void linstep(Gob g, double t, double e) {
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
    public Delta linstep(Message msg) {
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
	return(gob -> linstep(gob, t, e));
    }

    public static void speak(Gob g, float zo, String text) {
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
    public Delta speak(Message msg) {
	float zo = msg.int16() / 100.0f;
	String text = msg.string();
	return(gob -> speak(gob, zo, text));
    }

    public static void composite(Gob g, Indir<Resource> base) {
	Drawable dr = g.getattr(Drawable.class);
	Composite cmp = (dr instanceof Composite)?(Composite)dr:null;
	if((cmp == null) || !cmp.base.equals(base)) {
	    cmp = new Composite(g, base);
	    g.setattr(cmp);
	}
    }
    public Delta composite(Message msg) {
	Indir<Resource> base = getres(msg.uint16());
	return(gob -> composite(gob, base));
    }

    public static void cmppose(Gob g, int pseq, List<ResData> poses, List<ResData> tposes, boolean interp, float ttime) {
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
    public Delta cmppose(Message msg) {
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
	List<ResData> cposes = poses, ctposes = tposes;
	float cttime = ttime;
	return(gob -> cmppose(gob, seq, cposes, ctposes, interp, cttime));
    }

    public static void cmpmod(Gob g, List<Composited.MD> mod) {
	Composite cmp = (Composite)g.getattr(Drawable.class);
	if(cmp == null)
	    throw(new RuntimeException(String.format("cmpmod on non-composed object: %s", mod)));
	cmp.chmod(mod);
    }
    public Delta cmpmod(Message msg) {
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
	return(gob -> cmpmod(gob, mod));
    }

    public static void cmpequ(Gob g, List<Composited.ED> equ) {
	Composite cmp = (Composite)g.getattr(Drawable.class);
	if(cmp == null)
	    throw(new RuntimeException(String.format("cmpequ on non-composed object: %s", equ)));
	cmp.chequ(equ);
    }
    public Delta cmpequ(Message msg) {
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
	return(gob -> cmpequ(gob, equ));
    }

    public static void avatar(Gob g, List<Indir<Resource>> layers) {
	Avatar ava = g.getattr(Avatar.class);
	if(ava == null) {
	    ava = new Avatar(g);
	    g.setattr(ava);
	}
	ava.setlayers(layers);
    }
    public Delta avatar(Message msg) {
	List<Indir<Resource>> layers = new LinkedList<Indir<Resource>>();
	while(true) {
	    int layer = msg.uint16();
	    if(layer == 65535)
		break;
	    layers.add(getres(layer));
	}
	return(gob -> avatar(gob, layers));
    }

    public static void zoff(Gob g, float off) {
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
    }
    public Delta zoff(Message msg) {
	float off = msg.int16() / 100.0f;
	return(gob -> zoff(gob, off));
    }

    public static void lumin(Gob g, Coord off, int sz, int str) {
	g.setattr(new Lumin(g, off, sz, str));
    }
    public Delta lumin(Message msg) {
	Coord off = msg.coord();
	int sz = msg.uint16();
	int str = msg.uint8();
	return(gob -> lumin(gob, off, sz, str));
    }

    public static void follow(Gob g, long oid, Indir<Resource> xfres, String xfname) {
	if(oid == -1) {
	    g.delattr(Following.class);
	} else {
	    g.setattr(new Following(g, oid, xfres, xfname));
	}
    }
    public Delta follow(Message msg) {
	long oid = msg.uint32();
	if(oid != 0xffffffffl) {
	    Indir<Resource> xfres = getres(msg.uint16());
	    String xfname = msg.string();
	    return(gob -> follow(gob, oid, xfres, xfname));
	} else {
	    return(gob -> follow(gob, -1, null, null));
	}
    }

    public static void homostop(Gob g) {
	g.delattr(Homing.class);
    }
    public static void homing(Gob g, long oid, Coord2d tc, double v) {
	Homing homo = g.getattr(Homing.class);
	if((homo == null) || (homo.tgt != oid)) {
	    g.setattr(new Homing(g, oid, tc, v));
	} else {
	    homo.tc = tc;
	    homo.v = v;
	}
    }
    public Delta homing(Message msg) {
	long oid = msg.uint32();
	if(oid == 0xffffffffl) {
	    return(gob -> homostop(gob));
	} else {
	    Coord2d tgtc = msg.coord().mul(posres);
	    double v = msg.int32() * 0x1p-10 * 11;
	    return(gob -> homing(gob, oid, tgtc, v));
	}
    }

    public static void overlay(Gob g, int olid, boolean prs, Indir<Resource> resid, Message sdt) {
	Gob.Overlay ol = g.findol(olid);
	if(resid != null) {
	    sdt = new MessageBuf(sdt);
	    Gob.Overlay nol = null;
	    if(ol == null) {
		g.addol(nol = new Gob.Overlay(g, olid, resid, sdt), false);
	    } else if(!ol.sdt.equals(sdt)) {
		if(ol.spr instanceof Sprite.CUpd) {
		    MessageBuf copy = new MessageBuf(sdt);
		    ((Sprite.CUpd)ol.spr).update(copy);
		    ol.sdt = copy;
		} else {
		    g.addol(nol = new Gob.Overlay(g, olid, resid, sdt), false);
		    ol.remove();
		}
	    }
	    if(nol != null)
		nol.delign = prs;
	} else {
	    if(ol != null) {
		if(ol.spr instanceof Sprite.CDel)
		    ((Sprite.CDel)ol.spr).delete();
		else
		    ol.remove();
	    }
	}
    }
    public Delta overlay(Message msg) {
	int olidf = msg.int32();
	boolean prs = (olidf & 1) != 0;
	int olid = olidf >>> 1;
	int resid = msg.uint16();
	Indir<Resource> res;
	Message sdt;
	if(resid == 65535) {
	    res = null;
	    sdt = Message.nil;
	} else {
	    if((resid & 0x8000) != 0) {
		resid &= ~0x8000;
		sdt = new MessageBuf(msg.bytes(msg.uint8()));
	    } else {
		sdt = Message.nil;
	    }
	    res = getres(resid);
	}
	return(gob -> overlay(gob, olid, prs, res, sdt));
    }

    public static void health(Gob g, int hp) {
	g.setattr(new GobHealth(g, hp));
    }
    public Delta health(Message msg) {
	int hp = msg.uint8();
	return(gob -> health(gob, hp));
    }

    public static void buddy(Gob g, String name, int group, int type) {
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
    public Delta buddy(Message msg) {
	String name = msg.string();
	if(name.length() > 0) {
	    int group = msg.uint8();
	    int btype = msg.uint8();
	    return(gob -> buddy(gob, name, group, btype));
	} else {
	    return(gob -> buddy(gob, null, 0, 0));
	}
    }

    public static void icon(Gob g, Indir<Resource> res) {
	if(res == null)
	    g.delattr(GobIcon.class);
	else
	    g.setattr(new GobIcon(g, res));
    }
    public Delta icon(Message msg) {
	int resid = msg.uint16();
	Indir<Resource> res;
	if(resid == 65535) {
	    return(gob -> icon(gob, (Indir<Resource>)null));
	} else {
	    int ifl = msg.uint8();
	    return(gob -> icon(gob, getres(resid)));
	}
    }

    public static void resattr(Gob g, Indir<Resource> resid, Message dat) {
	Resource res = resid.get();
	GAttrib.Parser parser = res.getcode(GAttrib.Parser.class, false);
	if(parser != null) {
	    parser.apply(g, dat);
	    return;
	}
	if(dat != null)
	    g.setrattr(resid, dat);
	else
	    g.delrattr(resid);
    }
    public Delta resattr(Message msg) {
	Indir<Resource> resid = getres(msg.uint16());
	int len = msg.uint8();
	Message dat = (len > 0)?new MessageBuf(msg.bytes(len)):null;
	return(gob -> resattr(gob, resid, dat));
    }

    public Delta parse(int type, Message msg) {
	switch(type) {
	case OD_MOVE:
	    return(move(msg));
	case OD_RES:
	    return(cres(msg));
	case OD_LINBEG:
	    return(linbeg(msg));
	case OD_LINSTEP:
	    return(linstep(msg));
	case OD_HOMING:
	    return(homing(msg));
	case OD_SPEECH:
	    return(speak(msg));
	case OD_COMPOSE:
	    return(composite(msg));
	case OD_CMPPOSE:
	    return(cmppose(msg));
	case OD_CMPMOD:
	    return(cmpmod(msg));
	case OD_CMPEQU:
	    return(cmpequ(msg));
	case OD_ZOFF:
	    return(zoff(msg));
	case OD_LUMIN:
	    return(lumin(msg));
	case OD_AVATAR:
	    return(avatar(msg));
	case OD_FOLLOW:
	    return(follow(msg));
	case OD_OVERLAY:
	    return(overlay(msg));
	case OD_HEALTH:
	    return(health(msg));
	case OD_BUDDY:
	    return(buddy(msg));
	case OD_ICON:
	    return(icon(msg));
	case OD_RESATTR:
	    return(resattr(msg));
	default:
	    throw(new Session.MessageException("Unknown objdelta type: " + type, msg));
	}
    }

    public class GobInfo {
	public final long id;
	public final LinkedList<Delta> pending = new LinkedList<>();
	public int frame;
	public boolean nremoved, added, gremoved, virtual;
	public Gob gob;
	public Loader.Future<?> applier;

	public GobInfo(long id, int frame) {
	    this.id = id;
	    this.frame = frame;
	}

	private void apply() {
	    main: {
		synchronized(this) {
		    if(nremoved && (!added || gremoved))
			break main;
		    if(nremoved && added && !gremoved) {
			remove(gob);
			gob.updated();
			gremoved = true;
			gob = null;
			break main;
		    }
		    if(gob == null) {
			gob = new Gob(glob, Coord2d.z, id);
			gob.virtual = virtual;
		    }
		}
		while(true) {
		    Delta d;
		    synchronized(this) {
			if((d = pending.peek()) == null)
			    break;
		    }
		    synchronized(gob) {
			d.apply(gob);
		    }
		    synchronized(this) {
			if((pending.poll()) != d)
			    throw(new RuntimeException());
		    }
		}
		if(!added) {
		    add(gob);
		    added = true;
		}
		gob.updated();
	    }
	    synchronized(this) {
		applier = null;
		checkdirty(false);
	    }
	}

	public void checkdirty(boolean interrupt) {
	    synchronized(this) {
		if(applier == null) {
		    if(nremoved ? (added && !gremoved) : (!added || !pending.isEmpty())) {
			applier = glob.loader.defer(this::apply, null);
		    }
		} else if(interrupt) {
		    applier.restart();
		}
	    }
	}
    }

    private final Map<Long, GobInfo> netinfo = new HashMap<>();

    private GobInfo netremove(long id, int frame) {
	synchronized(netinfo) {
	    GobInfo ng = netinfo.get(id);
	    if((ng == null) || (ng.frame > frame))
		return(null);
	    synchronized(ng) {
		/* XXX: Clean up removed objects */
		ng.nremoved = true;
		ng.checkdirty(true);
	    }
	    return(ng);
	}
    }

    private GobInfo netget(long id, int frame) {
	synchronized(netinfo) {
	    GobInfo ng = netinfo.get(id);
	    if((ng != null) && ng.nremoved) {
		if(ng.frame >= frame)
		    return(null);
		netinfo.remove(id);
		ng = null;
	    }
	    if(ng == null) {
		ng = new GobInfo(id, frame);
		netinfo.put(id, ng);
	    } else {
		if(ng.frame >= frame)
		    return(null);
	    }
	    return(ng);
	}
    }

    public GobInfo receive(int fl, long id, int frame, Message msg) {
	List<Delta> attrs = new ArrayList<>();
	boolean hasrem = false;
	GobInfo removed = null;
	while(true) {
	    int type = msg.uint8();
	    if(type == OD_END) {
		break;
	    } else if(type == OD_REM) {
		removed = netremove(id, frame - 1);
		hasrem = true;
	    } else {
		attrs.add(parse(type, msg));
	    }
	}
	if(hasrem)
	    return(removed);
	synchronized(netinfo) {
	    if((fl & 1) != 0)
		netremove(id, frame - 1);
	    GobInfo ng = netget(id, frame);
	    if(ng != null) {
		synchronized(ng) {
		    ng.frame = frame;
		    ng.virtual = ((fl & 2) != 0);
		    ng.pending.addAll(attrs);
		    ng.checkdirty(false);
		}
	    }
	    return(ng);
	}
    }
}
