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
import haven.render.States; /* XXXRENDRM */
import haven.render.Rendered; /* XXXRENDRM */
import haven.render.RenderList; /* XXXRENDRM */

public abstract class PView extends Widget {
    public final RenderTree tree;
    public final RenderTree.Slot conf;
    public final RenderTree.Slot basic;
    public Texture2D fragcol = null;
    public Texture2D depth = null;
    private Texture2D.Sampler2D fragsamp;
    private final Map<Object, Pipe.Op> basicstates = new IdentityHashMap<>();
    private DrawList back = null;

    public PView(Coord sz) {
	super(sz);
	tree = new RenderTree();
	conf = tree.add((RenderTree.Node)null, conf());
	basic = conf.add((RenderTree.Node)null, null);
	basic();
    }

    private Pipe.Op conf() {
	return(new FrameConfig(this.sz));
    }

    public void resize(Coord sz) {
	super.resize(sz);
	conf.cstate(conf());
    }

    public void basic(Object id, Pipe.Op state) {
	basicstates.put(id, state);
	basic.ostate(p -> {
		for(Pipe.Op op : basicstates.values())
		    op.apply(p);
	    });
    }

    /* XXX? Remove standard clearing and assume implementations to add
     * explicit clearing slot? */
    protected FColor clearcolor() {
	return(FColor.BLACK);
    }

    /* I've no idea why this function is necessary. */
    @SuppressWarnings("unchecked")
    private static RenderList.Slot<Rendered> uglyJavaCWorkAround(RenderList.Slot<?> slot) {
	return((RenderList.Slot<Rendered>)slot);
    }

    public void draw(GOut g) {
	if((back == null) || !back.compatible(g.out.env())) {
	    if(back != null)
		back.dispose();
	    back = g.out.env().drawlist();
	    for(RenderTree.Slot slot : tree.slots()) {
		if(slot.obj() instanceof Rendered)
		    back.add(uglyJavaCWorkAround(slot));
	    }
	}
	FColor cc = clearcolor();
	if(cc != null)
	    g.out.clear(basic.state(), FragColor.fragcol, cc);
	g.out.clear(basic.state(), 1.0);
	back.draw(g.out);
	g.image(new TexRaw(fragsamp), Coord.z);
    }

    private static final Object id_fb = new Object(), id_view = new Object(), id_misc = new Object();
    protected void basic() {
	basic(id_fb, p -> {
		FrameConfig fb = p.get(FrameConfig.slot);
		if((fragcol == null) || !fragcol.sz().equals(fb.sz)) {
		    if(fragcol != null)
			fragcol.dispose();
		    fragcol = new Texture2D(fb.sz, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null);
		    fragsamp = new Texture2D.Sampler2D(fragcol);
		}
		if((depth == null) || !depth.sz().equals(fb.sz)) {
		    if(depth != null)
			depth.dispose();
		    depth = new Texture2D(fb.sz, DataBuffer.Usage.STATIC, Texture.DEPTH, new VectorFormat(1, NumberFormat.FLOAT32), null);
		}
		p.prep(new FragColor(fragcol.image(0))).prep(new DepthBuffer(depth.image(0)));
	    });
	basic(id_view, p -> {
		FrameConfig fb = p.get(FrameConfig.slot);
		Area area = Area.sized(Coord.z, fb.sz);
		p.prep(new States.Viewport(area));
		p.prep(Homo3D.state);
	    });
	basic(id_misc, Pipe.Op.compose(new States.Blending(), new States.Depthtest(States.Depthtest.Test.LE)));
    }
}
