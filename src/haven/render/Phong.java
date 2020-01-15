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

package haven.render;

import haven.render.sl.*;
import haven.render.sl.ValBlock.Value;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

public class Phong extends ValBlock.Group {
    public static final int maxlights = 4;
    public static final Struct s_light = Struct.make(new Symbol.Shared("light"),
						     VEC4, "amb",
						     VEC4, "dif",
						     VEC4, "spc",
						     VEC4, "pos",
						     FLOAT, "ac",
						     FLOAT, "al",
						     FLOAT, "aq");
    public static final Struct s_material = Struct.make(new Symbol.Shared("material"),
							VEC4, "emi",
							VEC4, "amb",
							VEC4, "dif",
							VEC4, "spc",
							FLOAT, "shine");
    private final Uniform nlights;
    private final Uniform lights;
    private final Uniform material;
    public final GValue bcol = new GValue(VEC3), scol = new GValue(VEC3);
    public final boolean pfrag;
    private final ProgramContext prog;
    private final Expression vert, edir, norm;

    public static class CelShade implements ShaderMacro {
	public final boolean dif, spc;

	public CelShade(boolean dif, boolean spc) {
	    this.dif = dif; this.spc = spc;
	}

	public static final Function celramp = new Function.Def(VEC3) {{
	    Expression c = param(IN, VEC3).ref();
	    Block.Local m = code.local(FLOAT, max(pick(c, "r"), pick(c, "g"), pick(c, "b")));
	    code.add(new If(lt(m.ref(), l(0.01)),
			    new Return(Vec3Cons.z)));
	    Block.Local v = code.local(FLOAT, null);
	    code.add(new If(gt(m.ref(), l(0.5)),
			    stmt(ass(v, l(1.0))),
			    new If(gt(m.ref(), l(0.1)),
				   stmt(ass(v, l(0.5))),
				   stmt(ass(v, l(0.0))))));
	    code.add(new Return(mul(c, div(v.ref(), m.ref()))));
	}};

	public void modify(ProgramContext prog) {
	    Phong ph = prog.getmod(Phong.class);
	    if(ph == null)
		return;
	    if(dif)
		ph.bcol.mod(in -> celramp.call(in), 0);
	    if(spc)
		ph.scol.mod(in -> celramp.call(in), 0);
	}
    }

    public class DoLight extends Function.Def {
	public final Expression i = param(IN, INT).ref();
	public final Expression vert = param(IN, VEC3).ref();
	public final Expression edir = param(IN, VEC3).ref();
	public final Expression norm = param(IN, VEC3).ref();
	public final LValue diff = param(INOUT, VEC3).ref();
	public final LValue spec = param(INOUT, VEC3).ref();
	public final Expression ls = idx(lights.ref(), i);
	public final Expression mat = material.ref();
	public final Expression shine = fref(mat, "shine");
	public final Value lvl, dir, dl, sl;
	public final ValBlock dvals = new ValBlock();
	public final ValBlock svals = new ValBlock();
	private final OrderList<Runnable> mods = new OrderList<Runnable>();
	public Block dcalc, scalc;
	public Statement dcurs, scurs;

	private DoLight() {
	    super(VOID, "dolight");

	    ValBlock.Group tdep = dvals.new Group() {
		    public void cons1() {
		    }

		    public void cons2(Block blk) {
			lvl.tgt = blk.local(FLOAT, null).ref();
			dir.tgt = blk.local(VEC3, null).ref();
			Block.Local rel = new Block.Local(VEC3);
			Block.Local dst = new Block.Local(FLOAT);
			code.add(new If(eq(pick(fref(ls, "pos"), "w"), l(0.0)),
					new Block(stmt(ass(lvl.tgt, l(1.0))),
						  stmt(ass(dir.tgt, pick(fref(ls, "pos"), "xyz")))),
					new Block(rel.new Def(sub(pick(fref(ls, "pos"), "xyz"), vert)),
						  stmt(ass(dir.tgt, normalize(rel.ref()))),
						  dst.new Def(length(rel.ref())),
						  stmt(ass(lvl.tgt, inv(add(fref(ls, "ac"),
									    mul(fref(ls, "al"), dst.ref()),
									    mul(fref(ls, "aq"), dst.ref(), dst.ref()))))))));
		    }
		};
	    lvl = tdep.new GValue(FLOAT);
	    dir = tdep.new GValue(VEC3);
	    dl = dvals.new Value(FLOAT) {
		    public Expression root() {
			return(dot(norm, dir.depref()));
		    }
		};
	    sl = svals.new Value(FLOAT) {
		    public Expression root() {
			Expression reflvl = pow(max(dot(edir, reflect(neg(dir.ref()), norm)), l(0.0)), shine);
			Expression hvlvl = pow(max(dot(norm, normalize(add(edir, dir.ref())))), shine);
			return(reflvl);
		    }
		};
	    lvl.force();
	    dl.force();
	    sl.force();
	}

	protected void cons() {
	    dvals.cons(code);
	    code.add(stmt(aadd(diff, mul(pick(fref(mat, "amb"), "rgb"),
					 pick(fref(ls,  "amb"), "rgb"),
					 lvl.ref()))));

	    code.add(new If(gt(dl.ref(), l(0.0)), dcalc = new Block()));
	    dcalc.add(dcurs = new Placeholder());
	    dcalc.add(aadd(diff, mul(pick(fref(mat, "dif"), "rgb"),
				     pick(fref(ls,  "dif"), "rgb"),
				     dl.ref(), lvl.ref())));

	    dcalc.add(new If(gt(shine, l(0.5)), scalc = new Block()));
	    svals.cons(scalc);
	    scalc.add(scurs = new Placeholder());
	    scalc.add(aadd(spec, mul(pick(fref(mat, "spc"), "rgb"),
				     pick(fref(ls,  "spc"), "rgb"),
				     sl.ref())));

	    for(Runnable mod : mods)
		mod.run();
	}

	public void mod(Runnable mod, int order) {
	    mods.add(mod, order);
	}
    }
    public final DoLight dolight;

    public void cons1() {
    }

    public void cons2(Block blk) {
	bcol.tgt = blk.local(VEC3, pick(fref(material.ref(), "emi"), "rgb")).ref();
	scol.tgt = blk.local(VEC3, Vec3Cons.z).ref();
	boolean unroll = true;
	if(!unroll) {
	    Variable i = blk.local(INT, "i", null);
	    blk.add(new For(ass(i, l(0)), lt(i.ref(), nlights.ref()), linc(i.ref()),
			    stmt(dolight.call(i.ref(), vert, edir, norm, bcol.tgt, scol.tgt))));
	} else {
	    for(int i = 0; i < 4; i++) {
		/* No few drivers seem to be having trouble with the for
		 * loop. It would be nice to be able to select this code
		 * path only on those drivers. */
		blk.add(new If(gt(nlights.ref(), l(i)),
			       stmt(dolight.call(l(i), vert, edir, norm, bcol.tgt, scol.tgt))));
	    }
	}
	bcol.addmods(blk); scol.addmods(blk);
    }

    private void fmod(final FragmentContext fctx, final Expression bcol, final Expression scol) {
	FragColor.fragcol(fctx).mod(in -> add(mul(in, vec4(bcol, pick(fref(material.ref(), "dif"), "a"))), vec4(scol, l(0.0))), 500);
    }

    public Phong(VertexContext vctx, Uniform.Data<Object[]> lights, Uniform.Data<?> material) {
	vctx.mainvals.super();
	this.nlights = new Uniform(INT, "nlights", p -> lights.value.apply(p).length, lights.deps);
	this.lights = new Uniform(new Array(s_light, maxlights), "lights", p -> lights.value.apply(p), lights.deps);
	this.material = new Uniform(s_material, "material", p -> material.value.apply(p), material.deps);
	pfrag = false;
	prog = vctx.prog;
	Homo3D homo = Homo3D.get(vctx.prog);
	Value edir = Homo3D.vertedir(vctx);
	depend(homo.eyev); depend(edir); depend(homo.eyen);
	this.vert = pick(homo.eyev.ref(), "xyz");
	this.edir = edir.ref();
	this.norm = homo.eyen.ref();

	Expression bcol = new AutoVarying(VEC3) {
		public Expression root(VertexContext vctx) {return(Phong.this.bcol.depref());}
	    }.ref();
	Expression scol = new AutoVarying(VEC3) {
		public Expression root(VertexContext vctx) {return(Phong.this.scol.depref());}
	    }.ref();
	fmod(vctx.prog.fctx, bcol, scol);
	dolight = new DoLight();
	prog.module(this);
    }

    public Phong(FragmentContext fctx, Uniform.Data<Object[]> lights, Uniform.Data<?> material) {
	fctx.mainvals.super();
	this.nlights = new Uniform(INT, "nlights", p -> lights.value.apply(p).length, lights.deps);
	this.lights = new Uniform(new Array(s_light, maxlights), "lights", p -> lights.value.apply(p), lights.deps);
	this.material = new Uniform(s_material, "material", p -> material.value.apply(p), material.deps);
	pfrag = true;
	prog = fctx.prog;
	Homo3D homo = Homo3D.get(prog);
	Value edir = homo.fragedir(fctx);
	Value norm = homo.frageyen(fctx);
	depend(edir); depend(norm);
	this.vert = homo.frageyev.ref();
	this.edir = edir.ref();
	this.norm = norm.ref();
	fmod(fctx, bcol.ref(), scol.ref());
	FragColor.fragcol(fctx).depend(bcol);
	dolight = new DoLight();
	prog.module(this);
    }
}
