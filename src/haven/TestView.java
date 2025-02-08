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
    static final FastMesh borkamesh = Resource.remote().loadwait("gfx/test/borka").layer(FastMesh.MeshRes.class).m;
    static final Material borkamat = Resource.remote().loadwait("gfx/test/borka").layer(Material.Res.class).get();
    float dist = 15, e = (float)Math.PI * 3 / 2, a = (float)Math.PI / 2, rot = 0;
    final RenderTree.Slot[] borka = {null, null};
    final Light.PhongLight light = new Light.PhongLight(true, FColor.BLACK, FColor.WHITE, FColor.BLACK, FColor.BLACK, 0);

    public TestView(Coord sz) {
	super(sz);
	setcam();
	borka[0] = basic.add(null);
	borka[0].add(borkamat.apply(borkamesh));
	borka[1] = basic.add(null);
	borka[1].add(borkamat.apply(borkamesh));
	basic.add(new DirLight(FColor.BLACK, FColor.WHITE, FColor.BLACK, Coord3f.xu));
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

    private void setcam() {
	Coord3f base = Coord3f.o;
	Matrix4f cam = Transform.makexlate(new Matrix4f(), new Coord3f(0.0f, 0.0f, -dist))
	    .mul1(Transform.makerot(new Matrix4f(), new Coord3f(-1.0f, 0.0f, 0.0f), ((float)Math.PI / 2.0f) - e))
	    .mul1(Transform.makerot(new Matrix4f(), new Coord3f(0.0f, 0.0f, -1.0f), ((float)Math.PI / 2.0f) + a))
	    .mul1(Transform.makexlate(new Matrix4f(), base.inv()));
	float field = 0.5f;
	float aspect = ((float)sz.y) / ((float)sz.x);
	basic(Camera.class, Pipe.Op.compose(new Camera(cam),
					    Projection.frustum(-field, field, -aspect * field, aspect * field, 1, 5000)));
    }

    public void mousemove(MouseMoveEvent ev) {
	Coord c = ev.c;
	if(c.x < 0 || c.x >= sz.x || c.y < 0 || c.y >= sz.y)
	    return;
	this.e = (float)Math.PI * 2 * ((float)c.y / (float)sz.y);
	this.a = (float)Math.PI * 2 * ((float)c.x / (float)sz.x);
	setcam();
    }

    public boolean mousewheel(MouseWheelEvent ev) {
	float d = this.dist + (ev.a * 5);
	if(d < 5)
	    d = 5;
	this.dist = d;
	setcam();
	return(true);
    }

    public void tick(double dt) {
	super.tick(dt);
	rot += (float)dt;
	borka[0].ostate(new Location(Transform.makexlate(new Matrix4f(), new Coord3f(0, 10, 0))
				     .mul1(Transform.makerot(new Matrix4f(), Coord3f.zu, rot))));
	borka[1].ostate(new Location(Transform.makexlate(new Matrix4f(), new Coord3f(0, -10, 0))
				     .mul1(Transform.makerot(new Matrix4f(), Coord3f.zu, -rot))));
    }

    protected FColor clearcolor() {return(FColor.RED);}
}
