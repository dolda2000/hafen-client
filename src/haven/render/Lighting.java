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
import static haven.render.sl.Cons.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

public interface Lighting {
    public static final State.Slot<State> lights = new State.Slot<>(State.Slot.Type.SYS, State.class);
    public static final Struct s_light = Struct.make(new Symbol.Shared("light"),
						     VEC4, "amb",
						     VEC4, "dif",
						     VEC4, "spc",
						     VEC4, "pos",
						     FLOAT, "ac",
						     FLOAT, "al",
						     FLOAT, "aq");

    public static abstract class LightList implements ShaderMacro {
	public abstract void construct(Block blk, java.util.function.Function<Params, Statement> body);

	public static class Params {
	    public Expression idx, lpar;

	    public Params(Expression idx, Expression lpar) {this.idx = idx; this.lpar = lpar;}
	}

	public void modify(ProgramContext prog) {
	    prog.module(this);
	}
    }

    public static class SimpleLights extends State {
	public static final int maxlights = 4;
	public static final boolean unroll = true;
	public static final Uniform u_nlights = new Uniform(INT, "nlights", p -> ((SimpleLights)p.get(lights)).list.length, lights);
	public static final Uniform u_lights = new Uniform(new Array(s_light, maxlights), "lights", p -> ((SimpleLights)p.get(lights)).list, lights);
	private final Object[][] list;

	public SimpleLights(Object[][] lights) {
	    this.list = lights;
	}

	private static final ShaderMacro shader = prog -> {
	    prog.module(new LightList() {
		    public void construct(Block blk, java.util.function.Function<Params, Statement> body) {
			if(!unroll) {
			    Variable i = blk.local(INT, "i", null);
			    blk.add(new For(ass(i, l(0)), lt(i.ref(), u_nlights.ref()), linc(i.ref()),
					    body.apply(new Params(i.ref(), idx(u_lights.ref(), i.ref())))));
			} else {
			    for(int i = 0; i < maxlights; i++) {
				/* Some old drivers and/or hardware seem to be
				 * having trouble with the for loop. Might not be
				 * as much of a problem these days as it used to
				 * be, but keep this for now, especially if
				 * SimpleLights are to be more of a legacy
				 * concern. */
				blk.add(new If(gt(u_nlights.ref(), l(i)),
					       body.apply(new Params(l(i), idx(u_lights.ref(), l(i))))));
			    }
			}
		    }
		});
	};
	public ShaderMacro shader() {return(shader);}

	public void apply(Pipe p) {
	    p.put(lights, this);
	}
    }
}
