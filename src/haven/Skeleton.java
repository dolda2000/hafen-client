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
import javax.media.opengl.*;

public class Skeleton {
    public final Map<String, Bone> bones = new HashMap<String, Bone>();
    public final Bone[] blist; /* Topologically sorted */
    public final Pose bindpose;

    public Skeleton(Collection<Bone> bones) {
	Set<Bone> bset = new HashSet<Bone>(bones);
	blist = new Bone[bones.size()];
	int idx = 0;
	for(Bone b : bones)
	    this.bones.put(b.name, b);
	while(!bset.isEmpty()) {
	    boolean f = false;
	    for(Iterator<Bone> i = bset.iterator(); i.hasNext();) {
		Bone b = i.next();
		boolean has;
		if(b.parent == null) {
		    has = true;
		} else {
		    has = false;
		    for(Bone p : blist) {
			if(p == b.parent) {
			    has = true;
			    break;
			}
		    }
		}
		if(has) {
		    blist[b.idx = idx++] = b;
		    i.remove();
		    f = true;
		}
	    }
	    if(!f)
		throw(new RuntimeException("Cyclical bone hierarchy"));
	}
	bindpose = mkbindpose();
    }
    
    public static class Bone {
	public String name;
	public Coord3f ipos, irax;
	public float irang;
	public Bone parent;
	public int idx;
	
	public Bone(String name, Coord3f ipos, Coord3f irax, float irang) {
	    this.name = name;
	    this.ipos = ipos;
	    this.irax = irax;
	    this.irang = irang;
	}
    }
    
    private static float[] rotasq(float[] q, float[] axis, float angle) {
	float m = (float)Math.sin(angle / 2.0);
	q[0] = (float)Math.cos(angle / 2.0);
	q[1] = m * axis[0]; q[2] = m * axis[1]; q[3] = m * axis[2];
	return(q);
    }
    
    private static float[] qqmul(float[] d, float[] a, float[] b) {
	float aw = a[0], ax = a[1], ay = a[2], az = a[3];
	float bw = b[0], bx = b[1], by = b[2], bz = b[3];
	d[0] = (aw * bw) - (ax * bx) - (ay * by) - (az * bz);
	d[1] = (aw * bx) + (ax * bw) + (ay * bz) - (az * by);
	d[2] = (aw * by) - (ax * bz) + (ay * bw) + (az * bx);
	d[3] = (aw * bz) + (ax * by) - (ay * bx) + (az * bw);
	return(d);
    }
    
    private static float[] vqrot(float[] d, float[] v, float[] q) {
	float vx = v[0], vy = v[1], vz = v[2];
	float qw = q[0], qx = q[1], qy = q[2], qz = q[3];
	/* I dearly wonder how the JIT's common-subexpression
	 * eliminator does on these. */
	d[0] = (qw * qw * vx) + (2 * qw * qy * vz) - (2 * qw * qz * vy) + (qx * qx * vx) +
	    (2 * qx * qy * vy) + (2 * qx * qz * vz) - (qz * qz * vx) - (qy * qy * vx);
	d[1] = (2 * qx * qy * vx) + (qy * qy * vy) + (2 * qy * qz * vz) + (2 * qw * qz * vx) -
	    (qz * qz * vy) + (qw * qw * vy) - (2 * qw * qx * vz) - (qx * qx * vy);
	d[2] = (2 * qx * qz * vx) + (2 * qy * qz * vy) + (qz * qz * vz) - (2 * qw * qy * vx) -
	    (qy * qy * vz) + (2 * qw * qx * vy) - (qx * qx * vz) + (qw * qw * vz);
	return(d);
    }
    
    private static float[] vset(float[] d, float[] s) {
	d[0] = s[0];
	d[1] = s[1];
	d[2] = s[2];
	return(d);
    }
    
    private static float[] qset(float[] d, float[] s) {
	d[0] = s[0];
	d[1] = s[1];
	d[2] = s[2];
	d[3] = s[3];
	return(d);
    }
    
    private static float[] vinv(float[] d, float[] s) {
	d[0] = -s[0];
	d[1] = -s[1];
	d[2] = -s[2];
	return(d);
    }
    
    private static float[] qinv(float[] d, float[] s) {
	/* Assumes |s| = 1.0 */
	d[0] = s[0];
	d[1] = -s[1];
	d[2] = -s[2];
	d[3] = -s[3];
	return(d);
    }
    
    private static float[] vvadd(float[] d, float[] a, float[] b) {
	float ax = a[0], ay = a[1], az = a[2];
	float bx = b[0], by = b[1], bz = b[2];
	d[0] = ax + bx;
	d[1] = ay + by;
	d[2] = az + bz;
	return(d);
    }
    
    private static float[] qqslerp(float[] d, float[] a, float[] b, float t) {
	float aw = a[0], ax = a[1], ay = a[2], az = a[3];
	float bw = b[0], bx = b[1], by = b[2], bz = b[3];
	if((aw == bw) && (ax == bx) && (ay == by) && (az == bz))
	    return(qset(d, a));
	float cos = (aw * bw) + (ax * bx) + (ay * by) + (az * bz);
	if(cos < 0) {
	    bw = -bw; bx = -bx; by = -by; bz = -bz;
	    cos = -cos;
	}
	float d0, d1;
	if(cos > 0.9999f) {
	    /* Reasonable threshold? Is this function even critical
	     * for performance? */
	    d0 = 1.0f - t; d1 = t;
	} else {
	    float da = (float)Math.acos(Utils.clip(cos, 0.0, 1.0));
	    float nf = 1.0f / (float)Math.sin(da);
	    d0 = (float)Math.sin((1.0f - t) * da) * nf;
	    d1 = (float)Math.sin(t * da) * nf;
	}
	d[0] = (d0 * aw) + (d1 * bw);
	d[1] = (d0 * ax) + (d1 * bx);
	d[2] = (d0 * ay) + (d1 * by);
	d[3] = (d0 * az) + (d1 * bz);
	return(d);
    }

    public Pose mkbindpose() {
	Pose p = new Pose();
	for(int i = 0; i < blist.length; i++) {
	    Bone b = blist[i];
	    p.lpos[i][0] = b.ipos.x; p.lpos[i][1] = b.ipos.y; p.lpos[i][2] = b.ipos.z;
	    rotasq(p.lrot[i], b.irax.to3a(), b.irang);
	}
	p.gbuild();
	return(p);
    }
	
    public class Pose {
	public float[][] lpos, gpos;
	public float[][] lrot, grot;
	private Pose from = null;
	public int seq = 0;
	
	private Pose() {
	    int nb = blist.length;
	    lpos = new float[nb][3];
	    gpos = new float[nb][3];
	    lrot = new float[nb][4];
	    grot = new float[nb][4];
	}
	
	public Pose(Pose from) {
	    this();
	    this.from = from;
	    reset();
	    gbuild();
	}
	
	public Skeleton skel() {
	    return(Skeleton.this);
	}
	
	public void reset() {
	    for(int i = 0; i < blist.length; i++) {
		vset(lpos[i], from.lpos[i]);
		qset(lrot[i], from.lrot[i]);
	    }
	}

	public void gbuild() {
	    int nb = blist.length;
	    for(int i = 0; i < nb; i++) {
		Bone b = blist[i];
		if(b.parent == null) {
		    gpos[i][0] = lpos[i][0];
		    gpos[i][1] = lpos[i][1];
		    gpos[i][2] = lpos[i][2];
		    grot[i][0] = lrot[i][0];
		    grot[i][1] = lrot[i][1];
		    grot[i][2] = lrot[i][2];
		    grot[i][3] = lrot[i][3];
		} else {
		    int pi = b.parent.idx;
		    qqmul(grot[i], grot[pi], lrot[i]);
		    vqrot(gpos[i], lpos[i], grot[pi]);
		    vvadd(gpos[i], gpos[i], gpos[pi]);
		}
	    }
	    seq++;
	}
	
	public void blend(Pose o, float d) {
	    for(int i = 0; i < blist.length; i++) {
		qqslerp(lrot[i], lrot[i], o.lrot[i], d);
		lpos[i][0] = lpos[i][0] + ((o.lpos[i][0] - lpos[i][0]) * d);
		lpos[i][1] = lpos[i][1] + ((o.lpos[i][1] - lpos[i][1]) * d);
		lpos[i][2] = lpos[i][2] + ((o.lpos[i][2] - lpos[i][2]) * d);
	    }
	}
	
	public Location bonetrans(final int bone) {
	    return(new Location(Matrix4f.identity()) {
		    private int cseq = -1;
		    
		    public Matrix4f fin(Matrix4f p) {
			if(cseq != seq) {
			    Matrix4f xf = Transform.makexlate(new Matrix4f(), new Coord3f(gpos[bone][0], gpos[bone][1], gpos[bone][2]));
			    if(grot[bone][0] < 0.9999) {
				float ang = (float)(Math.acos(grot[bone][0]) * 2.0);
				xf = xf.mul1(Transform.makerot(new Matrix4f(), new Coord3f(grot[bone][1], grot[bone][2], grot[bone][3]).norm(), ang));
			    }
			    update(xf);
			    cseq = seq;
			}
			return(super.fin(p));
		    }
		});
	}
	
	public Location bonetrans2(final int bone) {
	    return(new Location(Matrix4f.identity()) {
		    private int cseq = -1;
		    private float[] pos = new float[3], rot = new float[4];
		    
		    public Matrix4f fin(Matrix4f p) {
			if(cseq != seq) {
			    rot = qqmul(rot, grot[bone], qinv(rot, bindpose.grot[bone]));
			    pos = vvadd(pos, gpos[bone], vqrot(pos, vinv(pos, bindpose.gpos[bone]), rot));
			    Matrix4f xf = Transform.makexlate(new Matrix4f(), new Coord3f(pos[0], pos[1], pos[2]));
			    if(rot[0] < 0.9999) {
				float ang = (float)(Math.acos(rot[0]) * 2.0);
				xf = xf.mul1(Transform.makerot(new Matrix4f(), new Coord3f(rot[1], rot[2], rot[3]).norm(), ang));
			    }
			    update(xf);
			    cseq = seq;
			}
			return(super.fin(p));
		    }
		});
	}

	public class BoneAlign extends Location {
	    private final Coord3f ref;
	    private final int orig, tgt;
	    private int cseq = -1;
	    
	    public BoneAlign(Coord3f ref, Bone orig, Bone tgt) {
		super(Matrix4f.identity());
		this.ref = ref;
		this.orig = orig.idx;
		this.tgt = tgt.idx;
	    }
		
	    public Matrix4f fin(Matrix4f p) {
		if(cseq != seq) {
		    Coord3f cur = new Coord3f(gpos[tgt][0] - gpos[orig][0], gpos[tgt][1] - gpos[orig][1], gpos[tgt][2] - gpos[orig][2]).norm();
		    Coord3f axis = cur.cmul(ref).norm();
		    float ang = (float)Math.acos(cur.dmul(ref));
		    /*
		    System.err.println(cur + ", " + ref + ", " + axis + ", " + ang);
		    */
		    update(Transform.makexlate(new Matrix4f(), new Coord3f(gpos[orig][0], gpos[orig][1], gpos[orig][2]))
			   .mul1(Transform.makerot(new Matrix4f(), axis, -ang)));
		    cseq = seq;
		}
		return(super.fin(p));
	    }
	}

	public void boneoff(int bone, float[] offtrans) {
	    /* It would be nice if these "new float"s get
	     * stack-allocated. */
	    float[] rot = new float[4], xlate = new float[3];
	    rot = qqmul(rot, grot[bone], qinv(rot, bindpose.grot[bone]));
	    xlate = vvadd(xlate, gpos[bone], vqrot(xlate, vinv(xlate, bindpose.gpos[bone]), rot));
	    offtrans[3] = 0; offtrans[7] = 0; offtrans[11] = 0; offtrans[15] = 1;
	    offtrans[12] = xlate[0]; offtrans[13] = xlate[1]; offtrans[14] = xlate[2];
	    /* I must admit I don't /quite/ understand why the
	     * rotation needs to be inverted... */
	    float w = -rot[0], x = rot[1], y = rot[2], z = rot[3];
	    float xw = x * w * 2, xx = x * x * 2, xy = x * y * 2, xz = x * z * 2;
	    float yw = y * w * 2, yy = y * y * 2, yz = y * z * 2;
	    float zw = z * w * 2, zz = z * z * 2;
	    offtrans[ 0] = 1 - (yy + zz);
	    offtrans[ 5] = 1 - (xx + zz);
	    offtrans[10] = 1 - (xx + yy);
	    offtrans[ 1] = xy - zw;
	    offtrans[ 2] = xz + yw;
	    offtrans[ 4] = xy + zw;
	    offtrans[ 6] = yz - xw;
	    offtrans[ 8] = xz - yw;
	    offtrans[ 9] = yz + xw;
	}
	
	public final Rendered debug = new Rendered() {
		public void draw(GOut g) {
		    GL gl = g.gl;
		    g.st.put(Light.lighting, null);
		    g.state(States.xray);
		    g.apply();
		    gl.glBegin(GL.GL_LINES);
		    for(int i = 0; i < blist.length; i++) {
			if(blist[i].parent != null) {
			    int pi = blist[i].parent.idx;
			    gl.glColor3f(1.0f, 0.0f, 0.0f);
			    gl.glVertex3f(gpos[pi][0], gpos[pi][1], gpos[pi][2]);
			    gl.glColor3f(0.0f, 1.0f, 0.0f);
			    gl.glVertex3f(gpos[i][0], gpos[i][1], gpos[i][2]);
			}
		    }
		    gl.glEnd();
		}
	    
		public boolean setup(RenderList rl) {
		    rl.prepo(States.xray);
		    return(true);
		}
	    };
    }
    
    public abstract class PoseMod {
	public float[][] lpos, lrot;
	
	public PoseMod() {
	    int nb = blist.length;
	    lpos = new float[nb][3];
	    lrot = new float[nb][4];
	    for(int i = 0; i < nb; i++)
		lrot[i][0] = 1;
	}
	
	public void reset() {
	    for(int i = 0; i < blist.length; i++) {
		lpos[i][0] = 0; lpos[i][1] = 0; lpos[i][2] = 0;
		lrot[i][0] = 1; lrot[i][1] = 0; lrot[i][2] = 0; lrot[i][3] = 0;
	    }
	}
	
	public void rot(int bone, float ang, float ax, float ay, float az) {
	    float[] x = {ax, ay, az};
	    qqmul(lrot[bone], lrot[bone], rotasq(new float[4], x, ang));
	}

	public void apply(Pose p) {
	    for(int i = 0; i < blist.length; i++) {
		vvadd(p.lpos[i], p.lpos[i], lpos[i]);
		qqmul(p.lrot[i], p.lrot[i], lrot[i]);
	    }
	}
	
	public boolean tick(float dt) {
	    return(false);
	}
	
	public abstract boolean stat();
	public abstract boolean done();
    }
    
    public static class Res extends Resource.Layer {
	public final Skeleton s;
	
	public Res(Resource res, byte[] buf) {
	    res.super();
	    Map<String, Bone> bones = new HashMap<String, Bone>();
	    Map<Bone, String> pm = new HashMap<Bone, String>();
	    int[] off = {0};
	    while(off[0] < buf.length) {
		String bnm = Utils.strd(buf, off);
		Coord3f pos = new Coord3f((float)Utils.floatd(buf, off[0]), (float)Utils.floatd(buf, off[0] + 5), (float)Utils.floatd(buf, off[0] + 10));
		off[0] += 15;
		Coord3f rax = new Coord3f((float)Utils.floatd(buf, off[0]), (float)Utils.floatd(buf, off[0] + 5), (float)Utils.floatd(buf, off[0] + 10)).norm();
		off[0] += 15;
		float rang = (float)Utils.floatd(buf, off[0]);
		off[0] += 5;
		String bp = Utils.strd(buf, off);
		Bone b = new Bone(bnm, pos, rax, rang);
		if(bones.put(bnm, b) != null)
		    throw(new RuntimeException("Duplicate bone name: " + b.name));
		pm.put(b, bp);
	    }
	    for(Bone b : bones.values()) {
		String bp = pm.get(b);
		if(bp.length() == 0) {
		    b.parent = null;
		} else {
		    if((b.parent = bones.get(bp)) == null)
			throw(new Resource.LoadException("Parent bone " + bp + " not found for " + b.name, getres()));
		}
	    }
	    s = new Skeleton(bones.values()) {
		    public String toString() {
			return("Skeleton(" + getres().name + ")");
		    }
		};
	}
	
	public void init() {}
    }
    
    public class TrackMod extends PoseMod {
	public final Track[] tracks;
	public final float len;
	private final boolean stat;
	public final WrapMode mode;
	private boolean done;
	public float time = 0.0f;
	public boolean speedmod = false;
	public double nspeed = 0.0;
	private boolean back = false;
	
	public TrackMod(Track[] tracks, float len, WrapMode mode) {
	    this.tracks = tracks;
	    this.len = len;
	    this.mode = mode;
	    for(Track t : tracks) {
		if((t != null) && (t.frames.length > 1)) {
		    stat = false;
		    aupdate(0.0f);
		    return;
		}
	    }
	    stat = done = true;
	    aupdate(0.0f);
	}
	
	public void aupdate(float time) {
	    if(time > len)
		time = len;
	    reset();
	    for(int i = 0; i < tracks.length; i++) {
		Track t = tracks[i];
		if((t == null) || (t.frames.length == 0))
		    continue;
		if(t.frames.length == 1) {
		    qset(lrot[i], t.frames[0].rot);
		    vset(lpos[i], t.frames[0].trans);
		} else {
		    Track.Frame cf, nf;
		    float ct, nt;
		    int l = 0, r = t.frames.length;
		    while(true) {
			/* c should never be able to be >= frames.length */
			int c = l + ((r - l) >> 1);
			ct = t.frames[c].time;
			nt = (c < t.frames.length - 1)?(t.frames[c + 1].time):len;
			if(ct > time) {
			    r = c;
			} else if(nt < time) {
			    l = c + 1;
			} else {
			    cf = t.frames[c];
			    nf = t.frames[(c + 1) % t.frames.length];
			    break;
			}
		    }
		    float d;
		    if(nt == ct)
			d = 0;
		    else
			d = (time - ct) / (nt - ct);
		    qqslerp(lrot[i], cf.rot, nf.rot, d);
		    lpos[i][0] = cf.trans[0] + ((nf.trans[0] - cf.trans[0]) * d);
		    lpos[i][1] = cf.trans[1] + ((nf.trans[1] - cf.trans[1]) * d);
		    lpos[i][2] = cf.trans[2] + ((nf.trans[2] - cf.trans[2]) * d);
		}
	    }
	}
	
	public boolean tick(float dt) {
	    float nt = time + (back?-dt:dt);
	    switch(mode) {
	    case LOOP:
		nt %= len;
		break;
	    case ONCE:
		if(nt > len) {
		    nt = len;
		    done = true;
		}
		break;
	    case PONG:
		if(!back && (nt > len)) {
		    nt = len;
		    back = true;
		} else if(back && (nt < 0)) {
		    nt = 0;
		    done = true;
		}
		break;
	    case PONGLOOP:
		if(!back && (nt > len)) {
		    nt = len;
		    back = true;
		} else if(back && (nt < 0)) {
		    nt = 0;
		    back = false;
		}
		break;
	    }
	    this.time = nt;
	    if(!stat) {
		aupdate(this.time);
		return(true);
	    } else {
		return(false);
	    }
	}
	
	public boolean stat() {
	    return(stat);
	}
	
	public boolean done() {
	    return(done);
	}
    }

    public static class Track {
	public final String bone;
	public final Frame[] frames;
	    
	public static class Frame {
	    public final float time;
	    public final float[] trans, rot;
		
	    public Frame(float time, float[] trans, float[] rot) {
		this.time = time;
		this.trans = trans;
		this.rot = rot;
	    }
	}
	    
	public Track(String bone, Frame[] frames) {
	    this.bone = bone;
	    this.frames = frames;
	}
    }

    public static class ResPose extends Resource.Layer implements Resource.IDLayer<Integer> {
	public final int id;
	public final float len;
	public final Track[] tracks;
	public final double nspeed;
	public final WrapMode defmode;
	
	public ResPose(Resource res, byte[] buf) {
	    res.super();
	    this.id = Utils.int16d(buf, 0);
	    int fl = buf[2];
	    int mode = buf[3];
	    if(mode == 0)
		defmode = WrapMode.ONCE;
	    else if(mode == 1)
		defmode = WrapMode.LOOP;
	    else if(mode == 2)
		defmode = WrapMode.PONG;
	    else if(mode == 3)
		defmode = WrapMode.PONGLOOP;
	    else
		throw(new Resource.LoadException("Illegal animation mode: " + mode, getres()));
	    this.len = (float)Utils.floatd(buf, 4);
	    int[] off = {9};
	    if((fl & 1) != 0) {
		nspeed = Utils.floatd(buf, off[0]); off[0] += 5;
	    } else {
		nspeed = -1;
	    }
	    Collection<Track> tracks = new LinkedList<Track>();
	    while(off[0] < buf.length) {
		String bnm = Utils.strd(buf, off);
		Track.Frame[] frames = new Track.Frame[Utils.uint16d(buf, off[0])]; off[0] += 2;
		for(int i = 0; i < frames.length; i++) {
		    float tm = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		    float[] trans = new float[3];
		    for(int o = 0; o < 3; o++) {
			trans[o] = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		    }
		    float rang = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		    float[] rax = new float[3];
		    for(int o = 0; o < 3; o++) {
			rax[o] = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		    }
		    frames[i] = new Track.Frame(tm, trans, rotasq(new float[4], rax, rang));
		}
		tracks.add(new Track(bnm, frames));
	    }
	    this.tracks = tracks.toArray(new Track[0]);
	}
	
	public TrackMod forskel(Skeleton skel, WrapMode mode) {
	    Track[] remap = new Track[skel.blist.length];
	    for(Track t : tracks)
		remap[skel.bones.get(t.bone).idx] = t;
	    TrackMod ret = skel.new TrackMod(remap, len, mode);
	    if(nspeed > 0) {
		ret.speedmod = true;
		ret.nspeed = nspeed;
	    }
	    return(ret);
	}
	
	public Integer layerid() {
	    return(id);
	}
	
	public void init() {}
    }
    
    public static class BoneOffset extends Resource.Layer implements Resource.IDLayer<String> {
	public final String nm;
	public final Command[] prog;
	private static final HatingJava[] opcodes = new HatingJava[256];
	static {
	    opcodes[0] = new HatingJava() {
		    public Command make(byte[] buf, int[] off) {
			final float x = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			final float y = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			final float z = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			return(new Command() {
				public GLState make(Pose pose) {
				    return(Location.xlate(new Coord3f(x, y, z)));
				}
			    });
		    }
		};
	    opcodes[1] = new HatingJava() {
		    public Command make(byte[] buf, int[] off) {
			final float ang = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			final float ax = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			final float ay = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			final float az = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			return(new Command() {
				public GLState make(Pose pose) {
				    return(Location.rot(new Coord3f(ax, ay, az), ang));
				}
			    });
		    }
		};
	    opcodes[2] = new HatingJava() {
		    public Command make(byte[] buf, int[] off) {
			final String bonenm = Utils.strd(buf, off);
			return(new Command() {
				public GLState make(Pose pose) {
				    Bone bone = pose.skel().bones.get(bonenm);
				    return(pose.bonetrans(bone.idx));
				}
			    });
		    }
		};
	    opcodes[3] = new HatingJava() {
		    public Command make(byte[] buf, int[] off) {
			float rx1 = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			float ry1 = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			float rz1 = (float)Utils.floatd(buf, off[0]); off[0] += 5;
			float l = (float)Math.sqrt((rx1 * rx1) + (ry1 * ry1) + (rz1 * rz1));
			final Coord3f ref = new Coord3f(rx1 / l, ry1 / l, rz1 / l);
			final String orignm = Utils.strd(buf, off);
			final String tgtnm = Utils.strd(buf, off);
			return(new Command() {
				public GLState make(Pose pose) {
				    Bone orig = pose.skel().bones.get(orignm);
				    Bone tgt = pose.skel().bones.get(tgtnm);
				    return(pose.new BoneAlign(ref, orig, tgt));
				}
			    });
		    }
		};
	}
	
	public interface Command {
	    public GLState make(Pose pose);
	}

	public interface HatingJava {
	    public Command make(byte[] buf, int[] off);
	}
	
	public BoneOffset(Resource res, byte[] buf) {
	    res.super();
	    int[] off = {0};
	    this.nm = Utils.strd(buf, off);
	    List<Command> cbuf = new LinkedList<Command>();
	    while(off[0] < buf.length)
		cbuf.add(opcodes[buf[off[0]++]].make(buf, off));
	    this.prog = cbuf.toArray(new Command[0]);
	}
	
	public String layerid() {
	    return(nm);
	}
	
	public void init() {
	}
	
	public GLState forpose(Pose pose) {
	    GLState[] ls = new GLState[prog.length];
	    for(int i = 0; i < prog.length; i++)
		ls[i] = prog[i].make(pose);
	    return(GLState.compose(ls));
	}
    }
}
