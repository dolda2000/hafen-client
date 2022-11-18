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

import haven.render.*;
import haven.render.sl.*;
import haven.RenderContext.FrameFormat;

public class Tonemapper extends RenderContext.PostProcessor {
    public final State draw;

    public Tonemapper(State draw) {
	this.draw = draw;
    }

    public void run(GOut g, Texture2D.Sampler2D in) {
	g.usestate(draw);
	g.image(new TexRaw(in, true), Coord.z);
    }

    public int order() {return(ORDER_TONEMAP);}
    public FrameFormat outformat(FrameFormat in) {
	FrameFormat ret = new FrameFormat(in);
	ret.cfmt = new VectorFormat(in.cfmt.nc, NumberFormat.UNORM8);
	return(ret);
    }

    public static final State justgamma = new RUtils.AdHoc(prog -> {
	    FragColor.fragcol(prog.fctx).mod(in -> MiscLib.lin2srgb.call(in), 0);
    });
}
