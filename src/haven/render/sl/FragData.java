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

public class FragData extends Variable.Global {
    public final Function<Pipe, Object> value;
    public final Collection<State.Slot<?>> deps;
    public boolean primary = false;

    public FragData(Type type, Symbol name, Function<Pipe, Object> value, State.Slot<?>... deps) {
	super(type, name);
	this.value = value;
	ArrayList<State.Slot<?>> depl = new ArrayList<>(Arrays.asList(deps));
	depl.trimToSize();
	this.deps = depl;
    }

    public FragData(Type type, String infix, Function<Pipe, Object> value, State.Slot<?>... deps) {
	this(type, new Symbol.Shared("s_" + infix), value, deps);
    }

    public FragData(Type type, Function<Pipe, Object> value, State.Slot<?>... deps) {
	this(type, new Symbol.Shared(), value, deps);
    }

    public FragData primary() {
	primary = true;
	return(this);
    }

    public String toString() {
	return(String.format("#<fragdata %s %s %s>", type, name, deps));
    }

    private static final Object defid = new PostProc.AutoID("fragdata", 15000) {
	    public void proc(Context ctx) {
		FragmentContext fctx = (FragmentContext)ctx;
		Collection<FragData> used = new HashSet<>();
		for(Toplevel tl : fctx.vardefs) {
		    if(tl instanceof Def)
			used.add(((Def)tl).var());
		}
		FragData primary = null;
		FragData[] slots = new FragData[used.size()];
		int s = 0;
		for(FragData f : used) {
		    if(f.primary) {
			if(primary == null) {
			    slots[0] = primary = f;
			    s = 1;
			} else {
			    throw(new RuntimeException("Several fragment data require primary slot: " + primary + " and " + f));
			}
		    }
		}
		for(FragData f : used) {
		    if(f != primary)
			slots[s++] = f;
		}
		if(s != slots.length)
		    throw(new AssertionError());
		fctx.prog.fragdata.addAll(Arrays.asList(slots));
	    }
	};
    private class Def extends Definition implements PostProc.Processed {
	public void process(PostProc proc) {}
	public Object ppid() {return(defid);}
	private FragData var() {return(FragData.this);}

	public void output(Output out) {
	    if(out.ctx instanceof FragmentContext)
		out.write("out ");
	    else
		throw(new RuntimeException("use of fragdata variable outside fragment context: " + FragData.this));
	    super.output(out);
	}
    }

    public void use(Context ctx) {
	type.use(ctx);
	if(!defined(ctx))
	    ctx.vardefs.add(new Def());
    }
}
