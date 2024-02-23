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
import haven.render.*;

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
	
    public class Pose implements EquipTarget {
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
	
	/* XXX: It seems the return type of these should be something more generic. */
	public Supplier<Pipe.Op> bonetrans(int bone) {
	    return(new Supplier<Pipe.Op>() {
		    int cseq = -1;
		    Location cur;

		    public Pipe.Op get() {
			if(cseq != seq) {
			    Matrix4f xf = Transform.makexlate(new Matrix4f(), new Coord3f(gpos[bone][0], gpos[bone][1], gpos[bone][2]));
			    if(grot[bone][0] < 0.999999) {
				float ang = (float)(Math.acos(grot[bone][0]) * 2.0);
				xf = xf.mul1(Transform.makerot(new Matrix4f(), new Coord3f(grot[bone][1], grot[bone][2], grot[bone][3]).norm(), ang));
			    }
			    cur = new Location(xf);
			    cseq = seq;
			}
			return(cur);
		    }
		});
	}

	public Supplier<Pipe.Op> eqpoint(String name, Message dat) {
	    Bone bone = bones.get(name);
	    if(bone == null)
		return(null);
	    return(bonetrans(bone.idx));
	}

	public Supplier<Pipe.Op> bonetrans2(int bone) {
	    return(new Supplier<Pipe.Op>() {
		    int cseq = -1;
		    Location cur;
		    float[] pos = new float[3], rot = new float[4];

		    public Pipe.Op get() {
			if(cseq != seq) {
			    rot = qqmul(rot, grot[bone], qinv(rot, bindpose.grot[bone]));
			    pos = vvadd(pos, gpos[bone], vqrot(pos, vinv(pos, bindpose.gpos[bone]), rot));
			    Matrix4f xf = Transform.makexlate(new Matrix4f(), new Coord3f(pos[0], pos[1], pos[2]));
			    if(rot[0] < 0.999999) {
				float ang = (float)(Math.acos(rot[0]) * 2.0);
				xf = xf.mul1(Transform.makerot(new Matrix4f(), new Coord3f(rot[1], rot[2], rot[3]).norm(), ang));
			    }
			    cur = new Location(xf);
			    cseq = seq;
			}
			return(cur);
		    }
		});
	}

	public class BoneAlign implements Supplier<Pipe.Op> {
	    private final Coord3f ref;
	    private final int orig, tgt;
	    private Location cur;
	    private int cseq = -1;
	    
	    public BoneAlign(Coord3f ref, Bone orig, Bone tgt) {
		this.ref = ref;
		this.orig = orig.idx;
		this.tgt = tgt.idx;
	    }
		
	    public Location get() {
		if(cseq != seq) {
		    Coord3f cur = new Coord3f(gpos[tgt][0] - gpos[orig][0], gpos[tgt][1] - gpos[orig][1], gpos[tgt][2] - gpos[orig][2]).norm();
		    Coord3f axis = cur.cmul(ref).norm();
		    float ang = (float)Math.acos(cur.dmul(ref));
		    // Debug.dump(cur, ref, axis, ang);
		    this.cur = new Location(Transform.makexlate(new Matrix4f(), new Coord3f(gpos[orig][0], gpos[orig][1], gpos[orig][2]))
				       .mul1(Transform.makerot(new Matrix4f(), axis, -ang)));
		    cseq = seq;
		}
		return(cur);
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
	
	public class Debug implements RenderTree.Node, Rendered, TickList.Ticking, TickList.TickNode {
	    private final VertexArray.Layout fmt = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex,     new VectorFormat(3, NumberFormat.FLOAT32), 0,  0, 16),
									  new VertexArray.Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.UNORM8),  0, 12, 16));
	    private final VertexArray.Buffer data;
	    private final Model model;
	    private final int[] bperm;

	    public Debug() {
		int[] bperm = new int[blist.length];
		int n = 0;
		for(int i = 0; i < blist.length; i++) {
		    if(blist[i].parent != null)
			bperm[n++] = i;
		}
		this.bperm = Arrays.copyOf(bperm, n);
		if(n > 0) {
		    data = new VertexArray.Buffer(fmt.inputs[0].stride * n * 2, DataBuffer.Usage.STREAM, null);
		    model = new Model(Model.Mode.LINES, new VertexArray(fmt, data), null);
		} else {
		    data = null;
		    model = null;
		}
	    }

	    public void draw(Pipe state, Render g) {
		if(model != null)
		    g.draw(state, model);
	    }

	    public void autogtick(Render g) {
		if(data == null)
		    return;
		g.update(data, (tgt, env) -> {
			FillBuffer ret = env.fillbuf(tgt);
			java.nio.ByteBuffer buf = ret.push();
			for(int i = 0; i < bperm.length; i++) {
			    int bi = bperm[i], pi = blist[bi].parent.idx;
			    buf.putFloat(gpos[pi][0]).putFloat(gpos[pi][1]).putFloat(gpos[pi][2]);
			    buf.put((byte)255).put((byte)0).put((byte)0).put((byte)255);
			    buf.putFloat(gpos[bi][0]).putFloat(gpos[bi][1]).putFloat(gpos[bi][2]);
			    buf.put((byte)0).put((byte)255).put((byte)0).put((byte)255);
			}
			return(ret);
		    });
	    }

	    public TickList.Ticking ticker() {return(this);}

	    public void added(RenderTree.Slot slot) {
		slot.ostate(Pipe.Op.compose(VertexColor.instance, States.Depthtest.none, Rendered.last));
	    }
	}
    }

    public interface ModOwner extends OwnerContext {
	public double getv();
	public Collection<Location.Chain> getloc();

	public static final ModOwner nil = new ModOwner() {
		public double getv() {return(0);}
		public Collection<Location.Chain> getloc() {return(Collections.emptyList());}
		public <T> T context(Class<T> cl) {throw(new NoContext(cl));}
	    };
    }

    public abstract class PoseMod {
	public final ModOwner owner;
	public float[][] lpos, lrot;
	protected final Collection<FxTrack.EventListener> cbl = new ArrayList<FxTrack.EventListener>(0);

	public PoseMod(ModOwner owner) {
	    this.owner = owner;
	    int nb = blist.length;
	    lpos = new float[nb][3];
	    lrot = new float[nb][4];
	    for(int i = 0; i < nb; i++)
		lrot[i][0] = 1;
	}

	public Skeleton skel() {return(Skeleton.this);}
	
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

	public void age() {
	}
	
	public void listen(FxTrack.EventListener l) {
	    cbl.add(l);
	}

	public void remove(FxTrack.EventListener l) {
	    cbl.remove(l);
	}

	public void callback(FxTrack.Event ev) {
	    for(FxTrack.EventListener l : cbl)
		l.event(ev);
	}

	public abstract boolean stat();
	public abstract boolean done();
    }

    public PoseMod nilmod() {
	return(new PoseMod(ModOwner.nil) {
		public boolean stat() {return(true);}
		public boolean done() {return(false);}
	    });
    }

    public static PoseMod combine(final PoseMod... mods) {
	PoseMod first = mods[0];
	return(first.skel().new PoseMod(first.owner) {
		final boolean stat; {
		    boolean s = true;
		    for(PoseMod m : mods)
			s = s && m.stat();
		    stat = s;
		}

		public void apply(Pose p) {
		    for(PoseMod m : mods)
			m.apply(p);
		}

		public boolean tick(float dt) {
		    boolean ret = false;
		    for(PoseMod m : mods) {
			if(m.tick(dt))
			    ret = true;
		    }
		    return(ret);
		}

		public void age() {
		    for(PoseMod m : mods)
			m.age();
		}

		public boolean stat() {
		    return(stat);
		}

		public boolean done() {
		    for(PoseMod m : mods) {
			if(m.done())
			    return(true);
		    }
		    return(false);
		}
	    });
    }

    @Resource.PublishedCode(name = "pose")
    public interface ModFactory {
	public PoseMod create(Skeleton skel, ModOwner owner, Resource res, Message sdt);

	public static final ModFactory def = new ModFactory() {
		public PoseMod create(Skeleton skel, ModOwner owner, Resource res, Message sdt) {
		    int mask = Sprite.decnum(sdt);
		    Collection<PoseMod> poses = new ArrayList<PoseMod>(16);
		    for(ResPose p : res.layers(ResPose.class)) {
			if((p.id < 0) || ((mask & (1 << p.id)) != 0))
			    poses.add(p.forskel(owner, skel, p.defmode));
		    }
		    if(poses.size() == 0)
			return(skel.nilmod());
		    else if(poses.size() == 1)
			return(Utils.el(poses));
		    else
			return(combine(poses.toArray(new PoseMod[0])));
		}
	    };
    }

    public PoseMod mkposemod(ModOwner owner, Resource res, Message sdt) {
	ModFactory f = res.getcode(ModFactory.class, false);
	if(f == null)
	    f = ModFactory.def;
	return(f.create(this, owner, res, sdt));
    }

    public static class ResourceSkeleton extends Skeleton {
	public final Resource res;

	public ResourceSkeleton(Collection<Bone> bones, Res info) {
	    super(bones);
	    this.res = info.getres();
	}

	public String toString() {
	    return("Skeleton(" + res.name + ")");
	}
    }

    @Resource.LayerName("skel")
    public static class Res extends Resource.Layer {
	public final transient Skeleton s;
	
	private void read(Map<String, Bone> bones, Map<Bone, String> pm, Message buf, int ver) {
	    if(ver == 0) {
		while(!buf.eom()) {
		    String bnm = buf.string();
		    if((bnm.length() == 1) && (((int)bnm.charAt(0)) < 32)) {
			read(bones, pm, buf, (int)bnm.charAt(0));
			return;
		    }
		    Coord3f pos = new Coord3f((float)buf.cpfloat(), (float)buf.cpfloat(), (float)buf.cpfloat());
		    Coord3f rax = new Coord3f((float)buf.cpfloat(), (float)buf.cpfloat(), (float)buf.cpfloat()).norm();
		    float rang = (float)buf.cpfloat();
		    String bp = buf.string();
		    Bone b = new Bone(bnm, pos, rax, rang);
		    if(bones.put(bnm, b) != null)
			throw(new RuntimeException("Duplicate bone name: " + b.name));
		    pm.put(b, bp);
		}
	    } else if(ver == 1) {
		while(!buf.eom()) {
		    String bnm = buf.string();
		    String bp = buf.string();
		    Coord3f pos = new Coord3f(buf.float32(), buf.float32(), buf.float32());
		    float rang = buf.mnorm16() * 2 * (float)Math.PI;
		    float[] rax = new float[3];
		    Utils.oct2uvec(rax, buf.snorm16(), buf.snorm16());
		    Bone b = new Bone(bnm, pos, new Coord3f(rax[0], rax[1], rax[2]), rang);
		    if(bones.put(bnm, b) != null)
			throw(new RuntimeException("Duplicate bone name: " + b.name));
		    pm.put(b, bp);
		}
	    } else {
		throw(new AssertionError());
	    }
	}

	public Res(Resource res, Message buf) {
	    res.super();
	    Map<String, Bone> bones = new HashMap<String, Bone>();
	    Map<Bone, String> pm = new HashMap<Bone, String>();
	    read(bones, pm, buf, 0);
	    for(Bone b : bones.values()) {
		String bp = pm.get(b);
		if(bp.length() == 0) {
		    b.parent = null;
		} else {
		    if((b.parent = bones.get(bp)) == null)
			throw(new Resource.LoadException("Parent bone " + bp + " not found for " + b.name, getres()));
		}
	    }
	    s = new ResourceSkeleton(bones.values(), this);
	}
	
	public void init() {}
    }
    
    public class TrackMod extends PoseMod {
	public final Track[] tracks;
	public final FxTrack[] effects;
	public final float len;
	public final WrapMode mode;
	private final boolean stat;
	private boolean done;
	public float time = 0.0f;
	protected boolean speedmod = false;
	protected double nspeed = 0.0;
	private boolean back = false;
	
	public TrackMod(ModOwner owner, Track[] tracks, FxTrack[] effects, float len, WrapMode mode) {
	    super(owner);
	    this.tracks = tracks;
	    this.effects = effects;
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
		    int n = 0;
		    while(true) {
			if(++n > 100)
			    throw(new RuntimeException("Cannot find track frame in " + this + " for time " + time));
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

	private void playfx(float ot, float nt) {
	    if(ot > nt) {
		playfx(Math.min(ot, len), len);
		playfx(0, Math.max(0, nt));
	    } else {
		for(FxTrack t : effects) {
		    for(FxTrack.Event ev : t.events) {
			if((ev.time >= ot) && (ev.time < nt)) {
			    callback(ev);
			    ev.trigger(owner, this);
			}
		    }
		}
		if(!cbl.isEmpty())
		    callback(new FxTrack.Tick(nt));
	    }
	}

	public boolean tick(float dt) {
	    if(speedmod)
		dt *= owner.getv() / nspeed;
	    float nt = time + (back ? -dt : dt);
	    switch(mode) {
	    case LOOP:
		if(len == 0)
		    nt = 0;
		else
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
	    float ot = this.time;
	    this.time = nt;
	    if(!stat) {
		aupdate(this.time);
		if(!back)
		    playfx(ot, nt);
		else
		    playfx(nt, ot);
		return(true);
	    } else {
		return(false);
	    }
	}

	public void age() {
	    switch(mode) {
	    case PONGLOOP:
		back = Math.random() >= 0.5;
	    case LOOP:
		time = (float)Math.random() * len;
		break;
	    case PONG:
		back = true;
		time = 0;
		break;
	    case ONCE:
		time = len;
		break;
	    }
	    aupdate(time);
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

    public static class FxTrack {
	public final Event[] events;

	public static interface EventListener {
	    public void event(Event ev);
	}

	public static abstract class Event {
	    public final float time;

	    public Event(float time) {
		this.time = time;
	    }

	    public abstract void trigger(ModOwner owner, PoseMod mod);
	}

	public FxTrack(Event[] events) {
	    this.events = events;
	}

	public static class SpawnSprite extends Event {
	    public final Indir<Resource> res;
	    public final byte[] sdt;
	    public final Function<ModOwner, Pipe.Op> loc;

	    public SpawnSprite(float time, Indir<Resource> res, byte[] sdt, Function<ModOwner, Pipe.Op> loc) {
		super(time);
		this.res = res;
		this.sdt = (sdt == null)?new byte[0]:sdt;
		this.loc = loc;
	    }

	    public void trigger(ModOwner owner, PoseMod mod) {
		Glob glob = owner.context(Glob.class);
		Collection<Location.Chain> locs = owner.getloc();
		Loader l = glob.loader;
		l.defer(() -> {
			Pipe.Op ploc = (this.loc != null) ? this.loc.apply(owner) : null;
			for(Location.Chain loc : locs) {
			    Coord3f o = loc.fin(Matrix4f.id).mul4(Coord3f.o);
			    Location lxf = new Location(loc.fin(Location.makexlate(new Matrix4f(), o.inv())));
			    l.defer(() -> {
				    Gob n = glob.oc.new FixedPlace(o.invy(), 0) {
					    protected void obstate(Pipe buf) {
						buf.prep(lxf);
						if(ploc != null)
						    buf.prep(ploc);
					    }
					};
				    n.addol(new Gob.Overlay(n, -1, res, new MessageBuf(sdt)), false);
				    glob.oc.add(n);
				}, null);
			}
		    }, null);
	    }
	}

	public static class FxOverlay extends Gob.Overlay implements FxTrack.EventListener {
	    public final String fxid;
	    private final PoseMod mod;
	    private boolean ticked = true;

	    public FxOverlay(Gob gob, PoseMod mod, String id, Indir<Resource> res, Message sdt) {
		super(gob, -1, res, sdt);
		this.fxid = id;
		this.mod = mod;
		mod.listen(this);
	    }

	    public boolean tick(double dt) {
		if(super.tick(dt))
		    return(true);
		boolean rv = !ticked;
		ticked = false;
		return(rv);
	    }

	    protected void removed() {
		super.removed();
		mod.remove(this);
	    }

	    public void event(FxTrack.Event ev) {
		if(ev instanceof FxTrack.Tick)
		    ticked = true;
	    }
	}

	public static class MkOverlay extends Event {
	    public final String id;
	    public final Indir<Resource> res;
	    public final byte[] sdt;

	    public MkOverlay(float time, String id, Indir<Resource> res, byte[] sdt) {
		super(time);
		this.id = id.intern();
		this.res = res;
		this.sdt = sdt;
	    }

	    public void trigger(ModOwner owner, PoseMod mod) {
		Gob gob = owner.fcontext(Gob.class, false);
		if(gob != null) {
		    FxOverlay ol = new FxOverlay(gob, mod, this.id, this.res, new MessageBuf(this.sdt));
		    gob.addol(ol, true);
		}
	    }
	}

	public static class RmOverlay extends Event {
	    public final String id;

	    public RmOverlay(float time, String id) {
		super(time);
		this.id = id.intern();
	    }

	    public void trigger(ModOwner owner, PoseMod mod) {
		Gob gob = owner.fcontext(Gob.class, false);
		if(gob != null) {
		    for(Gob.Overlay ol : gob.ols) {
			if((ol instanceof FxOverlay) && (((FxOverlay)ol).fxid == this.id)) {
			    if(ol.spr instanceof Sprite.CDel)
				((Sprite.CDel)ol.spr).delete();
			    else
				ol.remove(true);
			}
		    }
		}
	    }
	}

	public static class Trigger extends Event {
	    public final String id;

	    public Trigger(float time, String id) {
		super(time);
		this.id = id.intern();
	    }

	    public void trigger(ModOwner owner, PoseMod mod) {}
	}

	public static class Tick extends Event {
	    public Tick(float time) {
		super(time);
	    }

	    public void trigger(ModOwner owner, PoseMod mod) {}
	}
    }

    @Resource.LayerName("skan")
    public static class ResPose extends Resource.Layer implements Resource.IDLayer<Integer> {
	public final int id;
	public final float len;
	public final transient Track[] tracks;
	public final transient FxTrack[] effects;
	public final double nspeed;
	public final WrapMode defmode;
	
	private Track.Frame[] parseframes(int fmt, Message buf) {
	    Track.Frame[] frames = new Track.Frame[buf.uint16()];
	    if(fmt == 0) {
		for(int i = 0; i < frames.length; i++) {
		    float tm = (float)buf.cpfloat();
		    float[] trans = new float[3];
		    for(int o = 0; o < 3; o++)
			trans[o] = (float)buf.cpfloat();
		    float rang = (float)buf.cpfloat();
		    float[] rax = new float[3];
		    for(int o = 0; o < 3; o++)
			rax[o] = (float)buf.cpfloat();
		    frames[i] = new Track.Frame(tm, trans, rotasq(new float[4], rax, rang));
		}
	    } else if(fmt == 1) {
		for(int i = 0; i < frames.length; i++) {
		    float tm = buf.unorm16() * len;
		    float[] trans = new float[3];
		    for(int o = 0; o < 3; o++)
			trans[o] = Utils.hfdec((short)buf.int16());
		    float rang = buf.mnorm16() * 2 * (float)Math.PI;
		    float[] rax = new float[3];
		    Utils.oct2uvec(rax, buf.snorm16(), buf.snorm16());
		    frames[i] = new Track.Frame(tm, trans, rotasq(new float[4], rax, rang));
		}
	    }
	    return(frames);
	}

	private FxTrack parsefx(int fmt, Message buf) {
	    FxTrack.Event[] events = new FxTrack.Event[buf.uint16()];
	    for(int i = 0; i < events.length; i++) {
		float tm = (fmt == 0) ? (float)buf.cpfloat() : (buf.unorm16() * len);
		int t = buf.uint8();
		Message sub = buf;
		boolean exhaust = false;
		if((t & 0x80) != 0) {
		    sub = new MessageBuf(buf.bytes(buf.uint16()));
		    t &= 0x7f;
		    exhaust = true;
		}
		switch(t) {
		case 0: case 2: {
		    String resnm = sub.string();
		    int resver = sub.uint16();
		    byte[] sdt = sub.bytes(sub.uint8());
		    int fl = (t == 2) ? sub.uint8() : 0;
		    Indir<Resource> res = getres().pool.load(resnm, resver);
		    Function<ModOwner, Pipe.Op> ploc = null;
		    if((fl & 1) != 0) {
			String eqnm = sub.string();
			Indir<Resource> src = ((fl & 2) == 0) ? getres().indir() : res;
			ploc = new Function<ModOwner, Pipe.Op>() {
				BoneOffset eqp = null;

				public Pipe.Op apply(ModOwner owner) {
				    if(eqp == null)
					eqp = src.get().flayer(BoneOffset.class, eqnm);
				    return(eqp.from(owner.context(EquipTarget.class)).get());
				}
			    };
		    }
		    events[i] = new FxTrack.SpawnSprite(tm, res, sdt, ploc);
		    break;
		}
		case 1: {
		    String id = sub.string();
		    events[i] = new FxTrack.Trigger(tm, id);
		    break;
		}
		case 3: {
		    int fl = sub.uint8();
		    String id = sub.string();
		    String resnm = sub.string();
		    int resver = sub.uint16();
		    byte[] sdt = sub.bytes(sub.uint8());
		    Indir<Resource> res = getres().pool.load(resnm, resver);
		    events[i] = new FxTrack.MkOverlay(tm, id, res, sdt);
		    break;
		}
		case 4: {
		    String id = sub.string();
		    events[i] = new FxTrack.RmOverlay(tm, id);
		    break;
		}
		default:
		    if(exhaust)
			Warning.warn("unknown animation control event: %d", t);
		    else
			throw(new Resource.LoadException("Illegal control event: " + t, getres()));
		}
		if(exhaust)
		    sub.skip();
	    }
	    return(new FxTrack(events));
	}

	public ResPose(Resource res, Message buf) {
	    res.super();
	    this.id = buf.int16();
	    int fl = buf.uint8();
	    int fmt = (fl & 6) >> 1;
	    int mode = buf.uint8();
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
	    if(fmt == 0)
		this.len = (float)buf.cpfloat();
	    else
		this.len = buf.float32();
	    if((fl & 1) != 0) {
		if(fmt == 0)
		    nspeed = buf.cpfloat();
		else
		    nspeed = buf.float32();
	    } else {
		nspeed = -1;
	    }
	    Collection<Track> tracks = new LinkedList<Track>();
	    Collection<FxTrack> fx = new LinkedList<FxTrack>();
	    while(!buf.eom()) {
		String bnm = buf.string();
		if(bnm.equals("{ctl}")) {
		    fx.add(parsefx(fmt, buf));
		} else {
		    tracks.add(new Track(bnm, parseframes(fmt, buf)));
		}
	    }
	    this.tracks = tracks.toArray(new Track[0]);
	    this.effects = fx.toArray(new FxTrack[0]);
	}

	private Track[] iaIaCthulhuFhtagn(Skeleton skel) {
	    Track[] remap = new Track[skel.blist.length];
	    for(Track t : tracks) {
		Skeleton.Bone b = skel.bones.get(t.bone);
		if(b == null)
		    throw(new RuntimeException("Bone \"" + t.bone + "\" in animation reference does not exist in skeleton " + skel));
		remap[b.idx] = t;
	    }
	    return(remap);
	}

	public class ResMod extends TrackMod {
	    public ResMod(ModOwner owner, Skeleton skel, WrapMode mode) {
		skel.super(owner, iaIaCthulhuFhtagn(skel), ResPose.this.effects, ResPose.this.len, mode);
		if(ResPose.this.nspeed > 0) {
		    this.speedmod = true;
		    this.nspeed = ResPose.this.nspeed;
		}
	    }

	    public ResMod(ModOwner owner, Skeleton skel) {
		this(owner, skel, defmode);
	    }

	    public String toString() {
		return(String.format("#<pose %d in %s>", id, getres().name));
	    }
	}

	public TrackMod forskel(ModOwner owner, Skeleton skel, WrapMode mode) {
	    return(new ResMod(owner, skel, mode));
	}

	public Integer layerid() {
	    return(id);
	}

	public void init() {}
    }

    @Resource.LayerName("boneoff")
    public static class BoneOffset extends Resource.Layer implements Resource.IDLayer<String> {
	public final String nm;
	public final transient Function<EquipTarget, Supplier<Pipe.Op>>[] prog;

	@SuppressWarnings("unchecked")
	private static final BiFunction<Message, BoneOffset, Function<EquipTarget, Supplier<? extends Pipe.Op>>>[] opcodes = new BiFunction[256];
	static {
	    opcodes[0] = (buf, bo) -> {
		float x = (float)buf.cpfloat();
		float y = (float)buf.cpfloat();
		float z = (float)buf.cpfloat();
		Location loc = Location.xlate(new Coord3f(x, y, z));
		return(equ -> () -> loc);
	    };
	    opcodes[16] = (buf, bo) -> {
		float x = buf.float32();
		float y = buf.float32();
		float z = buf.float32();
		Location loc = Location.xlate(new Coord3f(x, y, z));
		return(equ -> () -> loc);
	    };
	    opcodes[1] = (buf, bo) -> {
		float ang = (float)buf.cpfloat();
		float ax = (float)buf.cpfloat();
		float ay = (float)buf.cpfloat();
		float az = (float)buf.cpfloat();
		Location loc = Location.rot(new Coord3f(ax, ay, az), ang);
		return(equ -> () -> loc);
	    };
	    opcodes[17] = (buf, bo) -> {
		float ang = buf.mnorm16() * 2 * (float)Math.PI;
		float[] ax = new float[3];
		Utils.oct2uvec(ax, buf.snorm16(), buf.snorm16());
		Location loc = Location.rot(new Coord3f(ax[0], ax[1], ax[2]), ang);
		return(equ -> () -> loc);
	    };
	    opcodes[2] = (buf, bo) -> {
		String bonenm = buf.string();
		return(equ -> EquipTarget.eqpoint(equ, bonenm, Message.nil, bo));
	    };
	    opcodes[3] = (buf, bo) -> {
		Coord3f ref = Coord3f.of((float)buf.cpfloat(), (float)buf.cpfloat(), (float)buf.cpfloat()).norm();
		String orignm = buf.string();
		String tgtnm = buf.string();
		return(equ -> {
			Pose pose = (Pose)equ;
			Bone orig = pose.skel().bones.get(orignm);
			Bone tgt = pose.skel().bones.get(tgtnm);
			return(pose.new BoneAlign(ref, orig, tgt));
		    });
	    };
	    opcodes[19] = (buf, bo) -> {
		Coord3f ref = Utils.oct2uvec(buf.snorm16(), buf.snorm16());
		String orignm = buf.string();
		String tgtnm = buf.string();
		return(equ -> {
			Pose pose = (Pose)equ;
			Bone orig = pose.skel().bones.get(orignm);
			Bone tgt = pose.skel().bones.get(tgtnm);
			return(pose.new BoneAlign(ref, orig, tgt));
		    });
	    };
	    opcodes[4] = (buf, bo) -> {
		return(equ -> () -> Location.nullrot);
	    };
	    opcodes[5] = (buf, bo) -> {
		float scale = buf.float32();
		Location loc = Location.scale(scale);
		return(post -> () -> loc);
	    };
	}

	@SuppressWarnings("unchecked")
	public BoneOffset(Resource res, Message buf) {
	    res.super();
	    this.nm = buf.string();
	    List<Function<EquipTarget, Supplier<? extends Pipe.Op>>> cbuf = new LinkedList<>();
	    while(!buf.eom())
		cbuf.add(opcodes[buf.uint8()].apply(buf, this));
	    this.prog = cbuf.toArray(new Function[0]);
	}

	public String layerid() {
	    return(nm);
	}

	public void init() {
	}

	@SuppressWarnings("unchecked")
	public Supplier<Pipe.Op> from(EquipTarget equ) {
	    if(prog.length == 1)
		return(prog[0].apply(equ));
	    Supplier<Pipe.Op>[] ls = new Supplier[prog.length];
	    for(int i = 0; i < prog.length; i++)
		ls[i] = prog[i].apply(equ);
	    return(() -> {
		    Pipe.Op[] buf = new Pipe.Op[ls.length];
		    for(int i = 0; i < ls.length; i++)
			buf[i] = ls[i].get();
		    return(Pipe.Op.compose(buf));
		});
	}

	@Deprecated
	public Supplier<Pipe.Op> forpose(Pose pose) {
	    return(from(pose));
	}
    }
}
