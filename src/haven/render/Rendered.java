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
import haven.*;
import haven.render.sl.ShaderMacro;

public interface Rendered {
    public void draw(Pipe context, Render out);

    public static interface Instancable extends Rendered {
	public default Object instanceid() {return(this);}
	public Instanced instancify(InstanceBatch batch);
    }

    public static interface Instanced extends Rendered, InstanceBatch.Client, Disposable {
    }

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

	    public String toString() {return(String.format("#<order flat %s>", z));}
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

    public final static Pipe.Op eyesort = new Order.Default(10000); // XXXRENDER
    public final static Pipe.Op eeyesort = new Order.Default(4500); // XXXRENDER

    public static class ScreenQuad implements Rendered, RenderTree.Node {
	public static final Model data =
	    new Model(Model.Mode.TRIANGLE_STRIP,
		      new VertexArray(new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.SNORM8), 0, 0, 4),
							     new VertexArray.Layout.Input(Tex2D.texc, new VectorFormat(2, NumberFormat.SNORM8), 0, 2, 4)),
				      new VertexArray.Buffer(16, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(new byte[] {
						  -127, -127,   0,   0,
						   127, -127, 127,   0,
						  -127,  127,   0, 127,
						   127,  127, 127, 127,
					      }))),
		      null, 0, 4);
	public final Pipe.Op state;

	public ScreenQuad(boolean invert) {
	    Pipe.Op vxf;
	    if(invert)
		vxf = new Ortho2D(-1, -1,  1,  1);
	    else
		vxf = new Ortho2D(-1,  1,  1, -1);
	    state = Pipe.Op.compose(States.maskdepth, States.Depthtest.none, new States.Facecull(States.Facecull.Mode.NONE),
				    vxf);
	}

	public void draw(Pipe state, Render out) {
	    out.draw(state, data);
	}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(state);
	}
    }
}
