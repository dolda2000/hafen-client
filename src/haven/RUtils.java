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

package haven;

import java.util.*;
import java.util.function.*;
import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import haven.render.*;
import haven.render.RenderTree.Node;
import haven.render.RenderTree.Slot;
import haven.render.Pipe.Op;
import haven.render.Texture.Image;
import haven.render.TextureCube.CubeImage;
import haven.render.sl.ShaderMacro;

public class RUtils {
    public static Collection<Slot> multiadd(Collection<Slot> slots, Node node) {
	Collection<Slot> added = new ArrayList<>(slots.size());
	try {
	    for(Slot slot : slots)
		added.add(slot.add(node));
	} catch(RuntimeException e) {
	    for(Slot slot : added)
		slot.remove();
	    throw(e);
	}
	return(added);
    }

    public static void multirem(Collection<Slot> slots) {
	if(slots == null)
	    return;
	for(Slot slot : slots)
	    slot.remove();
    }

    public static void readd(Collection<Slot> slots, Consumer<Slot> add, Runnable revert) {
	Collection<Slot> ch = new ArrayList<>(slots.size());
	try {
	    for(Slot slot : slots) {
		ch.add(slot);
		slot.clear();
		add.accept(slot);
	    }
	} catch(RuntimeException e) {
	    revert.run();
	    try {
		for(Slot slot : ch) {
		    slot.clear();
		    add.accept(slot);
		}
	    } catch(RuntimeException e2) {
		Error err = new Error("Unexpected non-local exit", e2);
		err.addSuppressed(e);
		throw(err);
	    }
	    throw(e);
	}
    }

    public abstract static class StateNode<R extends RenderTree.Node> implements Node {
	public final R r;
	private final Collection<Slot> slots = new ArrayList<>(1);
	private Op cstate = null;

	public StateNode(R r) {
	    this.r = r;
	}

	protected abstract Op state();

	public void update() {
	    Op nstate = state();
	    if(nstate == null)
		throw(new NullPointerException("state"));
	    if(Utils.eq(cstate, nstate))
		return;
	    for(Slot slot : slots)
		slot.ostate(nstate);
	    this.cstate = nstate;
	}

	public void added(Slot slot) {
	    if(cstate == null) {
		if((cstate = state()) == null)
		    throw(new NullPointerException("state"));
	    }
	    slot.ostate(cstate);
	    slot.add(r);
	    slots.add(slot);
	}

	public void removed(Slot slot) {
	    slots.remove(slot);
	}

	public static <R extends RenderTree.Node> StateNode from(R r, Supplier<? extends Op> st) {
	    return(new StateNode<R>(r) {
		    protected Op state() {return(st.get());}
		});
	}

	public String toString() {
	    return(String.format("#<statenode %s>", r));
	}
    }

    public static abstract class StateTickNode<R extends RenderTree.Node> extends StateNode<R> implements TickList.TickNode, TickList.Ticking {
	public StateTickNode(R r) {
	    super(r);
	}

	public TickList.Ticking ticker() {return(this);}
	public void autotick(double dt) {
	    update();
	}

	public static <R extends RenderTree.Node> StateTickNode from(R r, Supplier<? extends Op> st) {
	    return(new StateTickNode<R>(r) {
		    protected Op state() {return(st.get());}
		});
	}
    }

    public static class CubeFill implements DataBuffer.Filler<Image> {
	public final Supplier<BufferedImage> src;
	public final int[][] order;
	private BufferedImage data;

	private static final int[][] deforder = {
	    {3, 1},			// +X
	    {1, 1},			// -X
	    {2, 0},			// +Y
	    {2, 2},			// -Y
	    {2, 1},			// +Z
	    {0, 1},			// -Z
	};

	public CubeFill(Supplier<BufferedImage> src) {
	    this.src = src;
	    this.order = deforder;
	}

	private Coord osz() {
	    int mx = 0, my = 0;
	    for(int i = 0; i < order.length; i++) {
		mx = Math.max(mx, order[i][0] + 1);
		my = Math.max(my, order[i][1] + 1);
	    }
	    return(new Coord(mx, my));
	}

	private BufferedImage getsrc(TextureCube tex) {
	    Coord on = osz();
	    int ex = tex.w * on.x, ey = tex.h * on.y;
	    BufferedImage ret = src.get();
	    if((ret.getWidth() != ex) || (ret.getHeight() != ey))
		throw(new IllegalArgumentException(String.format("cube-texture source size should be (%d, %d), not (%d, %d)", ex, ey, ret.getWidth(), ret.getHeight())));
	    return(ret);
	}

	public FillBuffer fill(Image gimg, Environment env) {
	    CubeImage img = (CubeImage)gimg;
	    if(data == null)
		data = getsrc(img.tex);
	    if(img.level == 0) {
		FillBuffer buf = env.fillbuf(img);
		int[] fc = order[img.face.ordinal()];
		buf.pull(ByteBuffer.wrap(TexI.convert(data, new Coord(img.w, img.h), new Coord(fc[0] * img.tex.w, fc[1] * img.tex.h), new Coord(img.w, img.h))));
		return(buf);
	    }
	    return(null);
	}

	public void done() {
	    data = null;
	}

	public TextureCube mktex() {
	    BufferedImage img = Loading.waitfor(src::get);
	    Coord on = osz();
	    return(new TextureCube(img.getWidth() / on.x, img.getHeight() / on.y, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), this));
	}
    }

    public static final State.Slot<State> adhoc = new State.Slot<>(State.Slot.Type.DRAW, State.class);
    public static class AdHoc extends State {
	private final ShaderMacro sh;

	public AdHoc(ShaderMacro sh) {
	    this.sh = sh;
	}

	public ShaderMacro shader() {return(sh);}

	public void apply(Pipe buf) {
	    buf.put(adhoc, this);
	}
    }

    public static final State.Slot<State> adhocg = new State.Slot<State>(State.Slot.Type.GEOM, State.class);
    public static class GeomAdHoc extends State {
	private final ShaderMacro sh;

	public GeomAdHoc(ShaderMacro sh) {
	    this.sh = sh;
	}

	public ShaderMacro shader() {return(sh);}

	public void apply(Pipe buf) {
	    buf.put(adhocg, this);
	}
    }
}
