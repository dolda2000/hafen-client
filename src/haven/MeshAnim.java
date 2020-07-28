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
import java.nio.*;
import haven.render.*;
import haven.render.sl.*;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

public class MeshAnim extends State {
    public static final State.Slot<MeshAnim> anim = new State.Slot<>(State.Slot.Type.GEOM, MeshAnim.class);
    public static final State.Slot<Animated> frame = new State.Slot<>(State.Slot.Type.GEOM, Animated.class)
	.instanced(new Instancable<Animated>() {
		final Instancer<Animated> nil = Instancer.dummy();
		public Instancer<Animated> instid(Animated st) {
		    return((st == null) ? nil : st.anim.instancer);
		}
	    });
    public final Frame[] frames;
    public final float len;
    public final int minv, maxv;

    public MeshAnim(Frame[] frames, float len) {
	this.frames = frames;
	this.len = len;
	int min = -1, max = -1;
	for(int i = 0; i < frames.length; i++) {
	    if(frames[i].minv < 0)
		continue;
	    if((min < 0) || (frames[i].minv < min))
		min = frames[i].minv;
	    if((max < 0) || (frames[i].maxv > max))
		max = frames[i].maxv;
	}
	if(min < 0)
	    throw(new RuntimeException("No animated vertex in meshanim"));
	this.minv = min;
	this.maxv = max;
    }

    public static class Frame {
	public final float time;
	public final int[] idx;
	public final float[] pos, nrm;
	public final int minv, maxv;

	public Frame(float time, int[] idx, float[] pos, float[] nrm) {
	    this.time = time;
	    this.idx = idx;
	    this.pos = pos;
	    this.nrm = nrm;
	    if(idx.length > 0) {
		int min = idx[0], max = idx[0];
		for(int i = 1; i < idx.length; i++) {
		    min = Math.min(min, idx[i]);
		    max = Math.max(max, idx[i]);
		}
		this.minv = min;
		this.maxv = max;
	    } else {
		this.minv = -1;
		this.maxv = -1;
	    }
	}
    }

    public boolean hasnrm() {
	return(frames[0].nrm != null);
    }

    private Texture2D dtex(boolean pos) {
	int nv = maxv + 1 - minv;
	int nt = nv * frames.length;
	int w = Tex.nextp2((int)Math.ceil(Math.sqrt(nt)));
	int h = (nt + w - 1) / w;
	DataBuffer.Filler<Texture2D.Image> init = (img, env) -> {
	    if(img.level != 0)
		return(null);
	    FillBuffer ret = env.fillbuf(img);
	    ShortBuffer buf = ret.push().asShortBuffer();
	    for(int i = 0; i < buf.limit(); i++)
		buf.put(i, (short)0);
	    for(int fn = 0; fn < frames.length; fn++) {
		Frame f = frames[fn];
		float[] data = pos ? f.pos : f.nrm;
		for(int i = 0; i < f.idx.length; i++) {
		    int tn = (f.idx[i] - minv) + (nv * fn);
		    buf.put((tn * 3) + 0, Utils.hfenc(data[(i * 3) + 0]));
		    buf.put((tn * 3) + 1, Utils.hfenc(data[(i * 3) + 1]));
		    buf.put((tn * 3) + 2, Utils.hfenc(data[(i * 3) + 2]));
		}
	    }
	    return(ret);
	};
	return(new Texture2D(w, h, DataBuffer.Usage.STATIC, new VectorFormat(3, NumberFormat.FLOAT16), init));
    }

    private Texture2D.Sampler2D ptex = null, ntex = null;
    public Texture2D.Sampler2D ptex() {
	if(ptex == null) {
	    synchronized(this) {
		if(ptex == null)
		    ptex = new Texture2D.Sampler2D(dtex(true));
	    }
	}
	return(ptex);
    }
    public Texture2D.Sampler2D ntex() {
	if(ntex == null) {
	    synchronized(this) {
		if(ntex == null)
		    ntex = new Texture2D.Sampler2D(dtex(false));
	    }
	}
	return(ntex);
    }

    private static class Shader implements ShaderMacro {
	static final Uniform pdata = new Uniform(SAMPLER2D, "panim", p -> p.get(anim).ptex(), anim);
	static final Uniform ndata = new Uniform(SAMPLER2D, "nanim", p -> {
		MeshAnim an = p.get(anim);
		return(an.hasnrm() ? an.ntex() : null);
	}, anim);
	static final Uniform voff = new Uniform(IVEC2, "voff", p -> {
		MeshAnim an = p.get(anim);
		return(new int[] {an.minv, an.maxv + 1 - an.minv});
	}, anim);
	static final InstancedUniform frames = new InstancedUniform.IVec2("frames", p -> {
		Animated fs = p.get(frame);
		return(new int[] {fs.foff(), fs.toff()});
	    }, frame);
	static final InstancedUniform ipol = new InstancedUniform.Float1("ipol", p -> p.get(frame).a, frame);
	final boolean nrm;
	final Object id;

	Shader(boolean nrm) {
	    this.nrm = nrm;
	    this.id = nrm;
	}

	Function off(VertexContext vctx, boolean pos) {
	    Function.Def fun = new Function.Def(VEC3, pos ? "poff" : "doff");
	    Block code = fun.code;
	    Expression snum = code.local(INT, sub(vctx.vertid(), pick(voff.ref(), "x"))).ref();
	    code.add(new If(or(lt(snum, l(0)), ge(snum, pick(voff.ref(), "y"))),
			    new Return(vec3(0, 0, 0))));
	    Expression data = (pos ? pdata : ndata).ref();
	    Expression ts = code.local(IVEC2, textureSize(data, l(0))).ref();
	    Expression snf = code.local(INT, add(pick(frames.ref(), "x"), snum)).ref();
	    Expression snt = code.local(INT, add(pick(frames.ref(), "y"), snum)).ref();
	    LValue scf = code.local(IVEC2, null).ref();
	    code.add(ass(pick(scf, "y"), div(snf, pick(ts, "x"))));
	    code.add(ass(pick(scf, "x"), sub(snf, mul(pick(scf, "y"), pick(ts, "x")))));
	    LValue sct = code.local(IVEC2, null).ref();
	    code.add(ass(pick(sct, "y"), div(snt, pick(ts, "x"))));
	    code.add(ass(pick(sct, "x"), sub(snt, mul(pick(sct, "y"), pick(ts, "x")))));
	    code.add(new Return(mix(pick(texelFetch(data, scf, l(0)), "rgb"),
				    pick(texelFetch(data, sct, l(0)), "rgb"),
				    ipol.ref())));
	    return(fun);
	}

	public void modify(ProgramContext prog) {
	    MeshMorph.get(prog.vctx).add(new MeshMorph.Morpher() {
		    Function poff = off(prog.vctx, true);
		    Function noff = off(prog.vctx, false);

		    public void morph(ValBlock.Value val, MeshMorph.MorphType type, VertexContext vctx) {
			switch(type) {
			case POS:
			    val.mod(in -> add(in, vec4(poff.call(), l(0.0))), -260);
			    break;
			case DIR:
			    if(nrm)
				val.mod(in -> add(in, noff.call()), -260);
			    break;
			}
		    }
		});
	}

	public int hashCode() {
	    return(id.hashCode());
	}

	public boolean equals(Object that) {
	    return((that instanceof Shader) && Utils.eq(((Shader)that).id, this.id));
	}

	private static final WeakHashedSet<Shader> interned = new WeakHashedSet<>(Hash.eq);
	public static Shader get(boolean nrm) {
	    return(interned.intern(new Shader(nrm)));
	}
    }

    public static class Animated extends State implements InstanceBatch.AttribState {
	public final MeshAnim anim;
	public final int ff, tf;
	public final float a;

	public Animated(MeshAnim anim, int ff, int tf, float a) {
	    this.anim = anim;
	    this.ff = ff;
	    this.tf = tf;
	    this.a = a;
	}

	int foff() {return(ff * (anim.maxv + 1 - anim.minv));}
	int toff() {return(tf * (anim.maxv + 1 - anim.minv));}

	public ShaderMacro shader() {return(null);}

	public void apply(Pipe p) {
	    p.put(frame, this);
	}

	public InstancedAttribute[] attribs() {
	    return(new InstancedAttribute[] {Shader.frames.attrib, Shader.ipol.attrib});
	}
    }

    static class Instanced extends Animated {
	Instanced(MeshAnim anim) {
	    super(anim, 0, 0, 0);
	}

	public ShaderMacro shader() {return(Instancer.mkinstanced);}
    }
    private Instanced ianim = null;
    private final Instancer<Animated> instancer = (ast, bat) -> {
	synchronized(this) {
	    if(this.ianim == null)
		this.ianim = new Instanced(this);
	    return(this.ianim);
	}
    };

    private ShaderMacro shader = null;
    public ShaderMacro shader() {
	if(shader == null)
	    shader = Shader.get(hasnrm());
	return(shader);
    }
    public void apply(Pipe p) {
	p.put(anim, this);
    }

    public abstract class Animation {
	public abstract Animated state();
	public abstract boolean tick(float dt);
	public MeshAnim desc() {return(MeshAnim.this);}
    }

    public class SeqAnimation extends Animation {
	private int cf;
	private float flen, ftm;

	public SeqAnimation() {
	    cf = -1;
	    flen = ftm = 0;
	    tick(0);
	}

	public boolean tick(float dt) {
	    boolean rv = false;
	    ftm += dt;
	    while(true) {
		if(ftm < flen)
		    break;
		ftm -= flen;
		cf = (cf + 1) % frames.length;
		if(cf == (frames.length - 1))
		    flen = len - frames[cf].time;
		else
		    flen = frames[cf + 1].time - frames[cf].time;
		if(cf == 0)
		    rv = true;
	    }
	    return(rv);
	}

	public Animated state() {
	    return(new Animated(MeshAnim.this, cf, (cf + 1) % frames.length, ftm / flen));
	}
    }

    public class RandAnimation extends Animation {
	private float fl, fp;
	private int cfi, nfi;
	private final Random rnd = new Random();

	public RandAnimation() {
	    fp = 0;
	    setfr(rnd.nextInt(frames.length));
	}

	private void setfr(int fi) {
	    cfi = fi;
	    nfi = rnd.nextInt(frames.length - 1);
	    if(nfi >= fi) nfi++;
	    fl = ((fi < frames.length - 1) ? (frames[fi + 1].time) : len) - frames[fi].time;
	}

	public boolean tick(float dt) {
	    fp += dt;
	    if(fp >= fl) {
		fp -= fl;
		setfr(nfi);
		if(fp >= fl) {
		    fp = 0;
		    setfr(rnd.nextInt(frames.length));
		}
	    }
	    return(false);
	}

	public Animated state() {
	    return(new Animated(MeshAnim.this, cfi, nfi, fp / fl));
	}
    }

    public boolean animp(FastMesh mesh) {
	int min = -1, max = -1;
	for(int i = 0; i < mesh.num * 3; i++) {
	    int vi = mesh.indb.get(i);
	    if(min < 0) {
		min = max = vi;
	    } else {
		if(vi < min)
		    min = vi;
		else if(vi > max)
		    max = vi;
	    }
	}
	boolean[] used = new boolean[max + 1 - min];
	for(int i = 0; i < mesh.num * 3; i++) {
	    int vi = mesh.indb.get(i);
	    used[vi - min] = true;
	}
	for(Frame f : frames) {
	    for(int i = 0; i < f.idx.length; i++) {
		int vi = f.idx[i];
		if((vi < min) || (vi > max))
		    continue;
		if(used[f.idx[i] - min])
		    return(true);
	    }
	}
	return(false);
    }

    @Resource.LayerName("manim")
    public static class Res extends Resource.Layer {
	public final int id;
	public final MeshAnim a;
	public final boolean rnd;

	public Res(Resource res, Message buf) {
	    res.super();
	    final float[] xfb = new float[3];
	    int ver = buf.uint8();
	    if(ver == 1) {
		id = buf.int16();
		rnd = buf.uint8() != 0;
		float len = buf.float32();
		List<Frame> frames = new LinkedList<Frame>();
		while(true) {
		    int t = buf.uint8();
		    if(t == 0)
			break;
		    else if((t < 0) || (t > 3))
			throw(new Resource.LoadException("Unknown meshanim frame format: " + t, res));
		    float tm = buf.float32();
		    int n = buf.uint16();
		    int[] idx = new int[n];
		    float[] pos = new float[n * 3];
		    float[] nrm = new float[n * 3];
		    int i = 0;
		    while(i < n) {
			int st = buf.uint16();
			int run = buf.uint16();
			for(int o = 0; o < run; o++) {
			    idx[i] = st + o;
			    if(t == 1) {
				pos[(i * 3) + 0] = buf.float32();
				pos[(i * 3) + 1] = buf.float32();
				pos[(i * 3) + 2] = buf.float32();
				nrm[(i * 3) + 0] = buf.float32();
				nrm[(i * 3) + 1] = buf.float32();
				nrm[(i * 3) + 2] = buf.float32();
			    } else if(t == 2) {
				Utils.float9995d(buf.int32(), xfb);
				pos[(i * 3) + 0] = xfb[0];
				pos[(i * 3) + 1] = xfb[1];
				pos[(i * 3) + 2] = xfb[2];
				nrm[(i * 3) + 0] = 0;
				nrm[(i * 3) + 1] = 0;
				nrm[(i * 3) + 2] = 0;
			    } else if(t == 3) {
				pos[(i * 3) + 0] = Utils.hfdec((short)buf.int16());
				pos[(i * 3) + 1] = Utils.hfdec((short)buf.int16());
				pos[(i * 3) + 2] = Utils.hfdec((short)buf.int16());
				nrm[(i * 3) + 0] = 0;
				nrm[(i * 3) + 1] = 0;
				nrm[(i * 3) + 2] = 0;
			    }
			    i++;
			}
		    }
		    for(i = 0; i < nrm.length; i++) {
			if(nrm[i] != 0)
			    break;
		    }
		    if(i == nrm.length)
			nrm = null;
		    frames.add(new Frame(tm, idx, pos, nrm));
		}
		a = new MeshAnim(frames.toArray(new Frame[0]), len);
	    } else {
		throw(new Resource.LoadException("Invalid meshanim format version: " + ver, res));
	    }
	}

	public Animation make() {
	    return(rnd ? a.new RandAnimation() : a.new SeqAnimation());
	}

	public void init() {
	}
    }
}
