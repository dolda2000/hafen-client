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

import java.util.*;
import java.util.function.*;
import java.util.function.Function;
import haven.render.*;

public class Uniform extends Variable.Global {
    public final Function<Pipe, Object> value;
    public final Collection<State.Slot<?>> deps;

    public Uniform(Type type, Symbol name, Function<Pipe, Object> value, State.Slot<?>... deps) {
	super(type, name);
	this.value = value;
	ArrayList<State.Slot<?>> depl = new ArrayList<>();
	for(State.Slot<?> slot : deps)
	    depl.add(slot);
	depl.trimToSize();
	this.deps = depl;
    }

    public Uniform(Type type, String infix, Function<Pipe, Object> value, State.Slot<?>... deps) {
	this(type, new Symbol.Shared("s_" + infix), value, deps);
    }

    public Uniform(Type type, Function<Pipe, Object> value, State.Slot<?>... deps) {
	this(type, new Symbol.Shared(), value, deps);
    }

    private class Def extends Definition {
	public void output(Output out) {
	    if(out.ctx instanceof ShaderContext) {
		((ShaderContext)out.ctx).prog.uniforms.add(Uniform.this);
	    }
	    out.write("uniform ");
	    super.output(out);
	}
    }

    public void use(Context ctx) {
	type.use(ctx);
	if(!defined(ctx))
	    ctx.vardefs.add(new Def());
	if((type == Type.SAMPLER2DMS) || (type == Type.SAMPLER2DMSARRAY))
	    ctx.exts.add("GL_ARB_texture_multisample");
    }

    public String toString() {
	return(String.format("#<uniform %s %s %s>", type, name, deps));
    }

    public static class Data<T> {
	public final Function<Pipe, T> value;
	public final State.Slot<?>[] deps;

	public Data(Function<Pipe, T> value, State.Slot<?>... deps) {
	    this.value = value;
	    this.deps = deps;
	}
    }
}
