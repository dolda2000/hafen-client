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
import haven.render.*;
import haven.render.Rendered;

public class TestView extends PView {
    public TestView(Coord sz) {
	super(sz);
	basic.add(new Quad(), null);
    }

    public static class Quad implements Rendered, RenderTree.Node {
	public static final Model data;

	static {
	    float[] vert = {
		75, 25, 1, 0, 0, 1,
		75, 75, 0, 1, 0, 1,
		25, 25, 0, 0, 1, 1,
		25, 75, 1, 0, 0.5f, 1,
	    };
	    VertexArray.Layout fmt = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.FLOAT32), 0, 0, 24),
							    new VertexArray.Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.FLOAT32), 0, 8, 24));
	    VertexArray vao = new VertexArray(fmt, new VertexArray.Buffer(vert.length * 4, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(vert)));
	    data = new Model(Model.Mode.TRIANGLE_STRIP, vao, null, 0, 4);
	}

	public void draw(Pipe state, Render out) {
	    out.draw(state, data);
	}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(new VertexColor());
	}
    }
}
