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

package haven.render.sl;

import haven.*;
import haven.render.*;
import java.util.*;
import java.nio.*;
import haven.render.State.Slot;
import java.util.function.Function;

public abstract class InstancedUniform {
    public final Slot[] deps;
    public final Uniform uniform;
    public final InstancedAttribute attrib;

    public InstancedUniform(Type type, String infix, Slot... deps) {
	this.deps = deps;
	uniform = new Uniform(type, infix, this::uniformval, deps);
	attrib = new InstancedAttribute(type, infix) {
		public VectorFormat attrfmt() {return(InstancedUniform.this.attrfmt());}
		public void attrfill(ByteBuffer buf, int offset, Pipe state) {InstancedUniform.this.attrfill(buf, offset, state);}
		/*
		public void filliarr(GOut g, List<Buffer> inst, GLBuffer buf) {InstancedUniform.this.filliarr(g, inst, buf);}
		public void bindiarr(GOut g, GLBuffer buf) {InstancedUniform.this.bindiarr(g, buf);}
		public void unbindiarr(GOut g, GLBuffer buf) {InstancedUniform.this.unbindiarr(g, buf);}
		*/
	    };
    }

    private static final Object refproc = new PostProc.AutoID("refproc", 9000);
    public Expression ref() {
	return(new PostProc.AutoMacro(refproc) {
		public Expression expand(Context ctx) {
		    if(!((ShaderContext)ctx).prog.instanced)
			return(uniform.ref());
		    else
			return(attrib.ref());
		}
	    });
    }

    protected abstract Object uniformval(Pipe state);
    protected abstract VectorFormat attrfmt();
    protected abstract void attrfill(ByteBuffer buf, int offset, Pipe state);

    public static class Float1 extends InstancedUniform {
	public static final VectorFormat fmt = new VectorFormat(1, NumberFormat.FLOAT32);
	public final Function<Pipe, Float> value;

	public Float1(String infix, Function<Pipe, Float> value, Slot... deps) {
	    super(Type.FLOAT, infix, deps);
	    this.value = value;
	}

	protected Object uniformval(Pipe state) {return(value.apply(state));}

	protected VectorFormat attrfmt() {return(fmt);}

	protected void attrfill(ByteBuffer buf, int offset, Pipe state) {
	    buf.putFloat(offset, value.apply(state));
	}
    }

    public static class Vec4 extends InstancedUniform {
	public static final VectorFormat fmt = new VectorFormat(4, NumberFormat.FLOAT32);
	public final Function<Pipe, float[]> value;

	public Vec4(String infix, Function<Pipe, float[]> value, Slot... deps) {
	    super(Type.VEC4, infix, deps);
	    this.value = value;
	}

	protected Object uniformval(Pipe state) {return(value.apply(state));}

	protected VectorFormat attrfmt() {return(fmt);}

	protected void attrfill(ByteBuffer buf, int offset, Pipe state) {
	    float[] val = value.apply(state);
	    for(int i = 0, o = 0; i < 4; i++, o += 4)
		buf.putFloat(offset + o, val[i]);
	}
    }

    public static class Int extends InstancedUniform {
	public static final VectorFormat fmt = new VectorFormat(1, NumberFormat.SINT32);
	public final Function<Pipe, Integer> value;

	public Int(String infix, Function<Pipe, Integer> value, Slot... deps) {
	    super(Type.INT, infix, deps);
	    this.value = value;
	}

	protected Object uniformval(Pipe state) {return(value.apply(state));}

	protected VectorFormat attrfmt() {return(fmt);}

	protected void attrfill(ByteBuffer buf, int offset, Pipe state) {
	    buf.putInt(offset, value.apply(state));
	}
    }

    public static class IVec2 extends InstancedUniform {
	public static final VectorFormat fmt = new VectorFormat(2, NumberFormat.SINT32);
	public final Function<Pipe, int[]> value;

	public IVec2(String infix, Function<Pipe, int[]> value, Slot... deps) {
	    super(Type.IVEC2, infix, deps);
	    this.value = value;
	}

	protected Object uniformval(Pipe state) {return(value.apply(state));}

	protected VectorFormat attrfmt() {return(fmt);}

	protected void attrfill(ByteBuffer buf, int offset, Pipe state) {
	    int[] val = value.apply(state);
	    for(int i = 0, o = 0; i < 2; i++, o += 4)
		buf.putInt(offset + o, val[i]);
	}
    }

    public static class Mat4 extends InstancedUniform {
	public static final VectorFormat fmt = new VectorFormat(16, NumberFormat.FLOAT32);
	public final Function<Pipe, Matrix4f> value;

	public Mat4(String infix, Function<Pipe, Matrix4f> value, Slot... deps) {
	    super(Type.MAT4, infix, deps);
	    this.value = value;
	}

	protected Object uniformval(Pipe state) {return(value.apply(state));}

	protected VectorFormat attrfmt() {return(fmt);}

	protected void attrfill(ByteBuffer buf, int offset, Pipe state) {
	    Matrix4f val = value.apply(state);
	    for(int i = 0, o = 0; i < 16; i++, o += 4)
		buf.putFloat(offset + o, val.m[i]);
	}
    }
}
