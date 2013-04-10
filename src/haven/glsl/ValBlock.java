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

public class ValBlock {
    private static final ThreadLocal<Value> processing = new ThreadLocal<Value>();
    private Collection<Value> values = new LinkedList<Value>();

    public abstract class Value {
	public final Type type;
	public final Symbol name;
	public boolean used;
	protected Variable var;
	protected Expression init;
	private final Collection<Value> deps = new LinkedList<Value>();
	private final Collection<Value> sdeps = new LinkedList<Value>();
	private final OrderList<Macro1<Expression>> mods = new OrderList<Macro1<Expression>>();
	private boolean forced;

	public Value(Type type, Symbol name) {
	    this.type = type;
	    this.name = name;
	    values.add(this);
	}

	public Value(Type type) {
	    this(type, new Symbol.Gen());
	}

	public void mod(Macro1<Expression> macro, int order) {
	    mods.add(macro, order);
	}

	public abstract Expression root();

	private void cons1() {
	    processing.set(this);
	    try {
		Expression expr = root();
		for(Macro1<Expression> mod : mods)
		    expr = mod.expand(expr);
		init = expr;
	    } finally {
		processing.remove();
	    }
	}

	protected void cons2(Block blk) {
	    var = blk.local(type, name, init);
	}

	public Expression ref() {
	    return(new Expression() {
		    public Expression process(Context ctx) {
			if(var == null)
			    throw(new IllegalStateException("Value reference processed before being constructed"));
			return(var.ref().process(ctx));
		    }
		});
	}

	public Expression depref() {
	    if(processing.get() == null)
		throw(new IllegalStateException("Dependent value reference outside construction"));
	    if(!processing.get().deps.contains(this))
		processing.get().deps.add(this);
	    return(ref());
	}

	public void force() {forced = true;}
	public void softdep(Value dep) {
	    if(!sdeps.contains(dep))
		sdeps.add(dep);
	}
    }

    private void add(List<Value> buf, Value val) {
	if(buf.contains(val))
	    return;
	for(Value dep : val.deps)
	    add(buf, dep);
	for(Value dep : val.sdeps) {
	    if(!dep.mods.isEmpty())
		add(buf, dep);
	}
	buf.add(val);
    }

    public void cons(Block blk) {
	for(Value val : values)
	    val.cons1();
	List<Value> used = new LinkedList<Value>();
	for(Value val : values) {
	    if(val.forced)
		add(used, val);
	}
	for(Value val : used) {
	    val.used = true;
	    val.cons2(blk);
	}
    }
}
