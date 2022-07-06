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
import haven.render.sl.*;
import haven.render.Texture2DMS.Sampler2DMS;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

public class TexMS implements Tex {
    public static final Attribute texc = new Attribute(VEC2, "mstexc");
    public static final VertexArray.Layout fmt = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.FLOAT32), 0, 0, 16),
									new VertexArray.Layout.Input(texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 8, 16));
    public final Sampler2DMS data;

    public TexMS(Sampler2DMS data) {
	this.data = data;
    }

    public Coord sz() {return(data.tex.sz());}

    public static final AutoVarying texcoord = new AutoVarying(VEC2, "s_mstex") {
	    protected Expression root(VertexContext vctx) {
		return(pick(texc.ref(), "st"));
	    }
	};

    public static final Uniform mstex = new Uniform(SAMPLER2DMS, p -> ((Draw)p.get(RUtils.adhoc)).data, RUtils.adhoc);
    public static class Resolve implements ShaderMacro {
	public final int samples;

	private Resolve(int samples) {
	    this.samples = samples;
	}

	public void modify(ProgramContext prog) {
	    Expression[] samples = new Expression[this.samples];
	    for(int i = 0; i < samples.length; i++)
		samples[i] = texelFetch(mstex.ref(), ivec2(floor(texcoord.ref())), l(i));
	    FragColor.fragcol(prog.fctx).mod(in -> mul(add(samples), l(1.0 / samples.length)), 0);
	}

	private static final Map<Integer, Resolve> shaders = new HashMap<>();
	public static Resolve get(int samples) {
	    synchronized(shaders) {
		return(shaders.computeIfAbsent(samples, Resolve::new));
	    }
	}
    }

    public static class Draw extends RUtils.AdHoc {
	public final Sampler2DMS data;

	public Draw(Sampler2DMS data) {
	    super(Resolve.get(data.tex.s));
	    this.data = data;
	}
    }

    private Draw st = null;
    public void render(GOut g, float[] gc, float[] tc) {
	float[] vert = {
	    gc[2], gc[3], tc[2], tc[3],
	    gc[4], gc[5], tc[4], tc[5],
	    gc[0], gc[1], tc[0], tc[1],
	    gc[6], gc[7], tc[6], tc[7],
	};
	if(st == null)
	    st = new Draw(this.data);
	g.usestate(st);
	g.out.draw1(g.state(), new Model(Model.Mode.TRIANGLE_STRIP, new VertexArray(fmt, new VertexArray.Buffer(vert.length * 4, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(vert))), null, 0, 4));
	g.usestate(RUtils.adhoc);
    }
}
