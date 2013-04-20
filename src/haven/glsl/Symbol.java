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

import java.util.*;

public abstract class Symbol {
    public abstract String name(Context ctx);

    public static class Gen extends Symbol {
	public final String prefix;

	public Gen(String prefix) {
	    this.prefix = prefix;
	}

	public Gen() {
	    this("g");
	}

	public String name(Context ctx) {
	    String nm = ctx.symtab.get(this);
	    if(nm == null) {
		nm = prefix + ctx.symgen++;
		if(ctx.rsymtab.get(nm) != null)
		    throw(new RuntimeException("Name conflict for gensym"));
		ctx.symtab.put(this, nm);
		ctx.rsymtab.put(nm, this);
	    }
	    return(nm);
	}
    }

    public static class Fix extends Symbol {
	public final String name;

	public Fix(String name) {
	    this.name = name;
	}

	public String name(Context ctx) {
	    Symbol p = ctx.rsymtab.get(name);
	    if(p == null) {
		ctx.symtab.put(this, name);
		ctx.rsymtab.put(name, this);
	    } else if(p != this) {
		throw(new RuntimeException("Name conflict for fix symbol `" + name + "'"));
	    }
	    return(name);
	}

	public String toString() {
	    return(name);
	}
    }

    public static class Shared extends Symbol {
	public final String prefix;

	public Shared(String prefix) {
	    this.prefix = prefix;
	}

	public Shared() {
	    this("s_g");
	}

	public String name(Context ctx) {
	    String nm = ctx.symtab.get(this);
	    if(nm == null) {
		if(ctx instanceof FragmentContext) {
		    FragmentContext fctx = (FragmentContext)ctx;
		    VertexContext vctx = fctx.prog.vctx;
		    nm = prefix + fctx.symgen++;
		    fctx.symtab.put(this, nm);
		    fctx.rsymtab.put(nm, this);
		    if(vctx.rsymtab.get(nm) != null)
			throw(new RuntimeException("Name conflict for shared symbol"));
		    vctx.symtab.put(this, nm);
		    vctx.rsymtab.put(nm, this);
		} else {
		    throw(new RuntimeException("Shared symbol processed before named"));
		}
	    }
	    return(nm);
	}
    }
}
