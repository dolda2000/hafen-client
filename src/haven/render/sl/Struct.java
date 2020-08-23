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

public class Struct extends Type {
    public final Symbol name;
    public final List<Field> fields;

    public static class Field {
	public final Type type;
	public final String name;

	public Field(Type type, String name) {
	    this.type = type;
	    this.name = name;
	}
    }

    private Struct(Symbol name, List<Field> fields) {
	this.name = name;
	this.fields = fields;
    }

    public Struct(Symbol name, Field... fields) {
	this(name, Arrays.asList(fields));
    }

    public Struct(Symbol name) {
	this(name, new LinkedList<Field>());
    }

    public static Struct make(Symbol name, Object... args) {
	Field[] fs = new Field[args.length / 2];
	for(int f = 0, a = 0; a < args.length;) {
	    Type ft = (Type)args[a++];
	    String fn = (String)args[a++];
	    fs[f++] = new Field(ft, fn);
	}
	return(new Struct(name, fs));
    }

    public String name(Context ctx) {
	return(name.name(ctx));
    }

    public int hashCode() {
	return(fields.hashCode());
    }

    public boolean equals(Object o) {
	return((o instanceof Struct) && Objects.equals(fields, ((Struct)o).fields));
    }

    public class Definition extends Toplevel {
	public void walk(Walker w) {}

	public void output(Output out) {
	    out.write("struct ");
	    out.write(name);
	    out.write(" {\n");
	    for(Field f : fields) {
		out.write("    ");
		out.write(f.type.name(out.ctx));
		out.write(" ");
		out.write(f.name);
		out.write(";\n");
	    }
	    out.write("};\n");
	}

	public Struct type() {
	    return(Struct.this);
	}
    }

    public boolean defined(Context ctx) {
	for(Toplevel tl : ctx.typedefs) {
	    if((tl instanceof Definition) && (((Definition)tl).type() == this))
		return(true);
	}
	return(false);
    }

    public void use(Context ctx) {
	if(!defined(ctx))
	    ctx.typedefs.add(new Definition());
    }
}
