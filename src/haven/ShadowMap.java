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
import haven.render.*;
import haven.render.sl.*;
import java.awt.image.*;
import haven.render.DataBuffer;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

public class ShadowMap extends State {
    public final static Slot<ShadowMap> smap = new Slot<ShadowMap>(Slot.Type.DRAW, ShadowMap.class);
    public final Texture2D lbuf;
    public final Texture2D.Sampler2D lsamp;
    private final Projection lproj;
    private final Pipe.Op basic;
    private DirLight light;
    private Camera lcam;
    private Pipe.Op curbasic;
    private final static Matrix4f texbias = new Matrix4f(0.5f, 0.0f, 0.0f, 0.5f,
							 0.0f, 0.5f, 0.0f, 0.5f,
							 0.0f, 0.0f, 0.5f, 0.5f,
							 0.0f, 0.0f, 0.0f, 1.0f);

    public ShadowMap(Coord res, float size, float depth, float dthr) {
	lbuf = new Texture2D(res, DataBuffer.Usage.STATIC, Texture.DEPTH, new VectorFormat(1, NumberFormat.FLOAT32), null);
	(lsamp = new Texture2D.Sampler2D(lbuf)).magfilter(Texture.Filter.LINEAR).wrapmode(Texture.Wrapping.CLAMP);
	/* XXX: It would arguably be nice to intern the shader. */
	shader = Shader.get(1.0 / res.x, 1.0 / res.y, 4, dthr / depth);
	lproj = Projection.ortho(-size, size, -size, size, 1, depth);
	basic = Pipe.Op.compose(new DepthBuffer<>(lbuf.image(0)),
				new States.Viewport(Area.sized(Coord.z, res)),
				lproj);
    }

    private ShadowMap(ShadowMap that) {
	this.lbuf     = that.lbuf;
	this.lsamp    = that.lsamp;
	this.shader   = that.shader;
	this.lproj    = that.lproj;
	this.basic    = that.basic;
	this.light    = that.light;
	this.lcam     = that.lcam;
	this.curbasic = that.curbasic;
    }

    public void dispose() {
	lbuf.dispose();
    }

    public static class ShadowList implements RenderList<Rendered>, RenderList.Adapter, Disposable {
	public static final Pipe.Op shadowbasic = Pipe.Op.compose(new States.Depthtest(States.Depthtest.Test.LE),
								  new States.Facecull(),
								  Homo3D.state);
	private final RenderList.Adapter master;
	private final ProxyPipe basic = new ProxyPipe();
	private final Map<Slot<? extends Rendered>, Shadowslot> slots = new HashMap<>();
	private DrawList back = null;
	private DefPipe curbasic = null;

	public ShadowList(RenderList.Adapter master) {
	    asyncadd(this.master = master, Rendered.class);
	}

	public class Shadowslot implements Slot<Rendered>, GroupPipe {
	    static final int idx_bas = 0, idx_back = 1;
	    public final Slot<? extends Rendered> bk;

	    public Shadowslot(Slot<? extends Rendered> bk) {
		this.bk = bk;
	    }

	    public Rendered obj() {
		return(bk.obj());
	    }

	    public GroupPipe state() {
		return(this);
	    }

	    public Pipe group(int idx) {
		switch(idx) {
		case idx_bas: return(basic);
		default: return(bk.state().group(idx - idx_back));
		}
	    }

	    public int gstate(int id) {
		if(State.Slot.byid(id).type == State.Slot.Type.GEOM) {
		    int ret = bk.state().gstate(id);
		    if(ret >= 0)
			return(ret + idx_back);
		}
		if((id < curbasic.mask.length) && curbasic.mask[id])
		    return(idx_bas);
		return(-1);
	    }

	    public int nstates() {
		return(Math.max(bk.state().nstates(), curbasic.mask.length));
	    }
	}

	public void add(Slot<? extends Rendered> slot) {
	    if(slot.state().get(Light.lighting) == null)
		return;
	    Shadowslot ns = new Shadowslot(slot);
	    if(back != null)
		back.add(ns);
	    if((slots.put(slot, ns)) != null)
		throw(new AssertionError());
	}

	public void remove(Slot<? extends Rendered> slot) {
	    Shadowslot cs = slots.remove(slot);
	    if(cs != null) {
		if(back != null)
		    back.remove(cs);
	    }
	}

	public void update(Slot<? extends Rendered> slot) {
	    if(back != null) {
		Shadowslot cs = slots.get(slot);
		if(cs != null) {
		    back.update(cs);
		}
	    }
	}

	public void update(Pipe group, int[] statemask) {
	    if(back != null)
		back.update(group, statemask);
	}

	public Locked lock() {
	    return(master.lock());
	}

	public Iterable<? extends Slot<?>> slots() {
	    return(slots.values());
	}

	/* Shouldn't have to care. */
	public <R> void add(RenderList<R> list, Class<? extends R> type) {}
	public void remove(RenderList<?> list) {}

	public void basic(Pipe.Op st) {
	    try(Locked lk = lock()) {
		DefPipe buf = new DefPipe();
		buf.prep(st);
		if(curbasic != null) {
		    int[] mask = curbasic.maskdiff(buf);
		    if(mask.length != 0) {
			for(int id : mask)
			    System.err.println(State.Slot.byid(id));
			throw(new RuntimeException("changing shadowlist basic definition mask is not supported"));
		    }
		}
		int[] mask = basic.dupdate(buf);
		curbasic = buf;
		if(back != null)
		    back.update(basic, mask);
	    }
	}

	public void draw(Render out) {
	    if((back == null) || !back.compatible(out.env())) {
		if(back != null)
		    back.dispose();
		back = out.env().drawlist();
		back.asyncadd(this, Rendered.class);
	    }
	    back.draw(out);
	}

	public void dispose() {
	    if(back != null)
		back.dispose();
	}
    }

    public ShadowMap light(DirLight light) {
	if(light == this.light)
	    return(this);
	ShadowMap ret = new ShadowMap(this);
	ret.light = light;
	return(ret);
    }

    public boolean haspos() {
	return(lcam != null);
    }

    public ShadowMap setpos(Coord3f base, Coord3f dir) {
	DirCam lcam = new DirCam();
	lcam.update(base, dir);
	if(Utils.eq(this.lcam, lcam))
	    return(this);
	ShadowMap ret = new ShadowMap(this);
	ret.lcam = lcam;
	ret.curbasic = Pipe.Op.compose(ShadowList.shadowbasic, ret.basic, lcam);
	return(ret);
    }

    public void update(Render out, ShadowList data) {
	/* XXX: FrameInfo, and potentially others, should quite
	 * arguably be inherited from some parent context instead. */
	Pipe.Op basic = Pipe.Op.compose(curbasic, new FrameInfo());
	Pipe bstate = new BufPipe().prep(basic);
	out.clear(bstate, 1.0);
	data.basic(basic);
	data.draw(out);
	if(false)
	    GOut.getimage(out, lbuf.image(0), Debug::dumpimage);
    }

    public void apply(Pipe buf) {
	buf.put(smap, this);
    }

    public static class Shader implements ShaderMacro {
	public static final Uniform txf = new Uniform(MAT4, p -> {
		ShadowMap sm = p.get(smap);
		Matrix4f cm = Transform.rxinvert(p.get(Homo3D.cam).fin(Matrix4f.id));
		Matrix4f proj = sm.lproj.fin(Matrix4f.id);
		Matrix4f lcam = sm.lcam.fin(Matrix4f.id);
		Matrix4f txf = texbias.mul(proj).mul(lcam).mul(cm);
		return(txf);
	    }, smap, Homo3D.cam);
	public static final Uniform sl = new Uniform(INT, p -> {
		DirLight light = p.get(smap).light;
		Light.LightList lights = p.get(Light.lights);
		int idx = -1;
		if(light != null)
		    idx = lights.index(light);
		return(idx);
	    }, smap, Light.lights);
	public static final Uniform map = new Uniform(SAMPLER2D, p -> p.get(smap).lsamp, smap);
	public static final AutoVarying stc = new AutoVarying(VEC4) {
		public Expression root(VertexContext vctx) {
		    return(mul(txf.ref(), Homo3D.get(vctx.prog).eyev.depref()));
		}
	    };

	public final Function.Def shcalc;
	private final Object id;

	private Shader(double xd, double yd, int res, double thr) {
	    this.id = Arrays.asList(xd, yd, res, thr);
	    this.shcalc = new Function.Def(FLOAT) {
		    {
			LValue sdw = code.local(FLOAT, l(0.0)).ref();
			Expression mapc = code.local(VEC3, div(pick(stc.ref(), "xyz"), pick(stc.ref(), "w"))).ref();
			double xr = xd * (res - 1), yr = yd * (res - 1);
			boolean unroll = false;
			if(!unroll) {
			    LValue xo = code.local(FLOAT, null).ref();
			    LValue yo = code.local(FLOAT, null).ref();
			    code.add(new For(ass(yo, l(-yr / 2)), lt(yo, l((yr / 2) + (yd / 2))), aadd(yo, l(yd)),
					     new For(ass(xo, l(-xr / 2)), lt(xo, l((xr / 2) + (xd / 2))), aadd(xo, l(xd)),
						     new If(gt(add(pick(texture2D(map.ref(), add(pick(mapc, "xy"), vec2(xo, yo))), "r"), l(thr)), pick(mapc, "z")),
							    stmt(aadd(sdw, l(1.0 / (res * res))))))));
			} else {
			    for(double yo = -yr / 2; yo < (yr / 2) + (yd / 2); yo += yd) {
				for(double xo = -xr / 2; xo < (xr / 2) + (xd / 2); xo += xd) {
				    code.add(new If(gt(add(pick(texture2D(map.ref(), add(pick(mapc, "xy"), vec2(l(xo), l(yo)))), "r"), l(thr)), pick(mapc, "z")),
						    stmt(aadd(sdw, l(1.0 / (res * res))))));
				}
			    }
			}
			code.add(new Return(sdw));
		    }
		};
	}

	public void modify(ProgramContext prog) {
	    final Phong ph = prog.getmod(Phong.class);
	    if((ph == null) || !ph.pfrag)
		return;
	    
	    ph.dolight.mod(new Runnable() {
		    public void run() {
			ph.dolight.dcalc.add(new If(eq(sl.ref(), ph.dolight.i),
						    stmt(amul(ph.dolight.dl.tgt, shcalc.call()))),
					     ph.dolight.dcurs);
		    }
		}, 0);
	}

	public int hashCode() {
	    return(id.hashCode());
	}

	public boolean equals(Object that) {
	    return((that instanceof Shader) && Utils.eq(this.id, ((Shader)that).id));
	}

	private static final WeakHashedSet<Shader> interned = new WeakHashedSet<>(Hash.eq);
	public static Shader get(double xd, double yd, int res, double thr) {
	    return(interned.intern(new Shader(xd, yd, res, thr)));
	}
    }

    public final Shader shader;

    public ShaderMacro shader() {return(shader);}
}
