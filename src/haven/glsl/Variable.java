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

package haven.glsl;

public abstract class Variable {
    public final Type type;
    public final Symbol name;

    public Variable(Type type, Symbol name) {
	this.type = type;
	this.name = name;
    }

    public class Ref extends LValue {
	public void walk(Walker w) {}

	public void output(Output out) {
	    out.write(name);
	}
    }

    public Ref ref() {
	return(new Ref());
    }

    public static class Implicit extends Variable {
	public Implicit(Type type, Symbol name) {
	    super(type, name);
	}
    }

    public static class Global extends Variable {
	public Global(Type type, Symbol name) {
	    super(type, name);
	}

	public Global(Type type) {
	    super(type, new Symbol.Gen());
	}

	private static final Object ppid = new PostProc.AutoID("vardef", 10000);
	public class Ref extends Variable.Ref implements PostProc.Processed {
	    public void process(PostProc proc) {
		use(proc.ctx);
	    }

	    public Object ppid() {
		return(ppid);
	    }

	    public void walk(Walker w) {}
	}

	public Ref ref() {
	    return(new Ref());
	}

	public boolean defined(Context ctx) {
	    for(Toplevel tl : ctx.vardefs) {
		if((tl instanceof Definition) && (((Definition)tl).var() == this))
		    return(true);
	    }
	    return(false);
	}

	public void use(Context ctx) {
	    if(!defined(ctx))
		ctx.vardefs.add(new Definition());
	}

	public class Definition extends Toplevel {
	    public void walk(Walker w) {}

	    public void output(Output out) {
		out.write(type.name(out.ctx));
		out.write(" ");
		out.write(name);
		out.write(";\n");
	    }

	    private Global var() {
		return(Global.this);
	    }
	}
    }
}
