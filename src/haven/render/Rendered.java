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

import java.util.*;
import haven.render.sl.ShaderMacro;

public interface Rendered {
    public void draw(Pipe context, Render out);

    public static final State.Slot<Order> order = new State.Slot<>(State.Slot.Type.GEOM, Order.class);
    public abstract static class Order<C extends Order> extends State {
	public abstract int mainorder();
	public abstract Comparator<? super C> comparator();

	public ShaderMacro shader() {return(null);}
	public void apply(Pipe p) {p.put(order, this);}

	public static class Default extends Order<Order> {
	    private final int z;

	    public Default(int z) {
		this.z = z;
	    }

	    public int mainorder() {
		return(z);
	    }

	    private static final Comparator<Order> cmp = new Comparator<Order>() {
		    public int compare(Order a, Order b) {
			return(0);
		    }
		};
	    public Comparator<Order> comparator() {return(cmp);}
	}

	public static final Comparator<Order> cmp = new Comparator<Order>() {
		@SuppressWarnings("unchecked")
		public int compare(Order a, Order b) {
		    int c;
		    if((c = (a.mainorder() - b.mainorder())) != 0)
			return(c);
		    return(a.comparator().compare(a, b));
		}
	    };
    }

    public final static Order deflt = new Order.Default(0);
    public final static Order first = new Order.Default(Integer.MIN_VALUE);
    public final static Order last = new Order.Default(Integer.MAX_VALUE);
    public final static Order postfx = new Order.Default(5000);
    public final static Order postpfx = new Order.Default(5500);
}
