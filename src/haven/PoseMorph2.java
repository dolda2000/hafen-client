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
import haven.Skeleton.Pose;
import haven.Skeleton.Bone;
import haven.PoseMorph.BoneData;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

public class PoseMorph2 {
    public static final State.Slot<Morphed> slot = new State.Slot<>(State.Slot.Type.GEOM, Morphed.class);
    public final Pose pose;
    private final int[] bperm;

    private static int[] mkperm(Skeleton skel, BoneData bd) {
	int[] ret = new int[bd.names.length];
	for(int i = 0; i < ret.length; i++) {
	    Bone bone = skel.bones.get(bd.names[i]);
	    if(bone == null)
		throw(new RuntimeException("Bone " + bd.names[i] + " not found in sksleton " + skel));
	    ret[i] = bone.idx;
	}
	return(ret);
    }

    public PoseMorph2(Pose pose, FastMesh mesh) {
	BoneData bd = mesh.vert.buf(BoneData.class);
	if(bd == null)
	    throw(new RuntimeException("No bonedata in " + mesh));
	this.pose = pose;
	this.bperm = mkperm(pose.skel(), bd);
    }

    private Morphed last;
    private int lastseq;
    public Morphed state() {
	if((last == null) || (lastseq != pose.seq)) {
	    float[][] offs = new float[bperm.length][16];
	    for(int i = 0; i < bperm.length; i++)
		pose.boneoff(bperm[i], offs[i]);
	    last = new Morphed(offs);
	    lastseq = pose.seq;
	}
	return(last);
    }

    public static final Attribute vba = new Attribute(IVEC4, "vba");
    public static final Attribute vbw = new Attribute(VEC4, "vbw");
    private static class Shader implements ShaderMacro, MeshMorph.Morpher {
	final int nb;
	final Uniform bo;
	final Function skanp, skand;

	Shader(int nb) {
	    this.nb = nb;
	    this.bo = new Uniform(new Array(MAT4, nb), "bo", p -> p.get(slot).offs, slot);
	    this.skanp = skan(true);
	    this.skand = skan(false);
	}

	Function skan(boolean pos) {
	    Function.Def fun;
	    if(pos)
		fun = new Function.Def(VEC4, "skanp");
	    else
		fun = new Function.Def(VEC3, "skand");
	    Block code = fun.code;
	    Expression c; LValue r;
	    if(pos) {
		c = fun.param(Function.PDir.IN, VEC4).ref();
		r = code.local(VEC4, vec4(0, 0, 0, 0)).ref();
	    } else {
		c = fun.param(Function.PDir.IN, VEC3).ref();
		r = code.local(VEC3, vec3(0, 0, 0)).ref();
	    }
	    String[] els = {"x", "y", "z", "w"};
	    for(int i = 0; i < els.length; i++) {
		Expression ba = pick(vba.ref(), els[i]);
		Expression mat = idx(bo.ref(), ba);
		if(!pos)
		    mat = mat3(mat);
		code.add(new If(ge(ba, l(0)),
				stmt(aadd(r, mul(mul(mat, c),
						 pick(vbw.ref(), els[i]))))));
	    }
	    code.add(new Return(r));
	    return(fun);
	}

	public void morph(ValBlock.Value val, MeshMorph.MorphType type, VertexContext vctx) {
	    switch(type) {
	    case POS:
		val.mod(in -> skanp.call(in), -250);
		break;
	    case DIR:
		val.mod(in -> skand.call(in), -250);
		break;
	    }
	}

	public void modify(ProgramContext prog) {
	    MeshMorph.get(prog.vctx).add(this);
	}

	public int hashCode() {
	    return(nb);
	}

	public boolean equals(Object that) {
	    return((that instanceof Shader) && (this.nb == ((Shader)that).nb));
	}

	private static final WeakHashedSet<Shader> interned = new WeakHashedSet<>(Hash.eq);
	public static Shader get(int nb) {
	    return(interned.intern(new Shader(nb)));
	}
    }

    public static class Morphed extends State {
	public final float[][] offs;
	private final ShaderMacro shader;

	public Morphed(float[][] offs) {
	    this.offs = offs;
	    int nb = Integer.highestOneBit(offs.length);
	    nb = Math.max(4, (nb == offs.length) ? nb : nb << 1);
	    this.shader = Shader.get(nb);
	}

	public ShaderMacro shader() {return(shader);}

	public void apply(Pipe p) {
	    p.put(slot, this);
	}
    }
}
