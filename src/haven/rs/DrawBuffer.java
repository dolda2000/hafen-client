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

package haven.rs;

import haven.*;
import haven.render.*;
import java.awt.image.BufferedImage;

public class DrawBuffer implements Disposable {
    public final Environment env;
    public final Coord sz;
    public final Texture2D color, depth;
    private final Pipe.Op basic;

    public DrawBuffer(Environment env, Coord sz) {
	this.env = env;
	this.sz = sz;
	this.color = new Texture2D(sz.x, sz.y, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null);
	this.depth = new Texture2D(sz.x, sz.y, DataBuffer.Usage.STATIC, Texture.DEPTH, new VectorFormat(1, NumberFormat.FLOAT32), null);
	Area vp = Area.sized(Coord.z, sz);
	this.basic = Pipe.Op.compose(new States.Viewport(vp), new FrameConfig(sz),
				     new FragColor<>(color.image(0)), new DepthBuffer<>(depth.image(0)));
    }

    public Pipe.Op basic() {
	return(basic);
    }

    public GOut graphics() {
	Pipe state = new BufPipe();
	state.prep(basic());
	state.prep(FragColor.blend(new BlendMode(BlendMode.Function.ADD, BlendMode.Factor.SRC_ALPHA, BlendMode.Factor.INV_SRC_ALPHA,
						 BlendMode.Function.MAX, BlendMode.Factor.SRC_ALPHA, BlendMode.Factor.ONE)));
	state.prep(new Ortho2D(Area.sized(Coord.z, this.sz)));
	return(new GOut(env.render(), state, this.sz));
    }

    public void draw(Drawn thing) {
	GOut g = graphics();
	thing.draw(g);
	env.submit(g.out);
    }

    static DrawList merdel = null;
    public BufferedImage draw(Pipe.Op state, RenderTree.Node n) {
	RenderTree tree = new RenderTree();
	TickList tick = new TickList();
	DrawList rnd = env.drawlist();
	Light.LightList lights = new Light.LightList();
	tree.add(tick, TickList.TickNode.class);
	tree.add(rnd, Rendered.class);
	RenderTree.Slot basic = tree.add((RenderTree.Node)null);
	Pipe.Op bstate = Pipe.Op.compose(basic(),
					 Homo3D.state, new States.Depthtest(States.Depthtest.Test.LE), new States.Facecull(), lights,
					 state);
	basic.ostate(bstate);
	Loading.waitfor(() -> basic.add(n));
	basic.ostate(Pipe.Op.compose(bstate, lights.compile()));
	Render cmd = env.render();
	tick.tick(0);
	tick.gtick(cmd);
	cmd.clear(basic.state(), FragColor.fragcol, new FColor(0, 0, 0 ,0));
	cmd.clear(basic.state(), 1.0);
	rnd.draw(cmd);
	BufferedImage[] retbuf = {null};
	GOut.getimage(cmd, basic.state(), FragColor.fragcol, Area.sized(Coord.z, this.sz), img -> {
		retbuf[0] = img;
	    });
	env.submit(cmd);
	if(retbuf[0] == null)
	    throw(new AssertionError());
	merdel = rnd;
	return(retbuf[0]);
    }

    public void dispose() {
	color.dispose();
	depth.dispose();
    }
}
