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
import haven.render.Texture2D.Sampler2D;

public abstract class TexRender implements Tex, Disposable {
    public static final VertexArray.Layout vf_tex2d = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.FLOAT32), 0, 0, 16),
									     new VertexArray.Layout.Input(Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 8, 16));
    public final Sampler2D img;
    public boolean centroid = false;

    public TexRender(Sampler2D img) {
	this.img = img;
    }

    public Coord sz() {
	return(img.tex.sz());
    }

    public void dispose() {
	img.dispose();
    }

    private static final ShaderMacro mktex = prog -> {
	Tex2D.get(prog).tex2d(new Uniform.Data<Object>(p -> {
		    TexDraw draw = p.get(TexDraw.slot);
		    TexClip clip = p.get(TexClip.slot);
		    if((draw != null) && (clip != null)) {
			if(draw.tex != clip.tex)
			    throw(new RuntimeException(String.format("TexRender does not support different draw (%s) and clip (%s) textures", draw.tex, clip.tex)));
			return(draw.tex.img);
		    } else if(draw != null) {
			return(draw.tex.img);
		    } else if(clip != null) {
			return(clip.tex.img);
		    } else {
			throw(new AssertionError());
		    }
		}, TexDraw.slot, TexClip.slot));
    };
    private static final ShaderMacro mkcentroid = prog -> {
	Tex2D.get(prog).ipol = Varying.Interpol.CENTROID;
    };

    public static class TexDraw extends State {
	public static final Slot<TexDraw> slot = new Slot<TexDraw>(Slot.Type.DRAW, TexDraw.class);
	public final TexRender tex;

	public TexDraw(TexRender tex) {
	    this.tex = tex;
	}

	private static final ShaderMacro nshader = ShaderMacro.compose(mktex, Tex2D.mod);
	private static final ShaderMacro cshader = ShaderMacro.compose(mktex, Tex2D.mod, mkcentroid);
	public ShaderMacro shader() {
	    return(tex.centroid ? cshader : nshader);
	}

	public void apply(Pipe p) {
	    p.put(slot, this);
	}

	public String toString() {
	    return(String.format("#<texdraw %s>", tex));
	}
    }
    public final TexDraw draw = new TexDraw(this);

    public static class TexClip extends State {
	public static final Slot<TexClip> slot = new Slot<TexClip>(Slot.Type.GEOM, TexClip.class);
	public final TexRender tex;

	public TexClip(TexRender tex) {
	    this.tex = tex;
	}

	private static final ShaderMacro shader = ShaderMacro.compose(mktex, Tex2D.clip);
	public ShaderMacro shader() {
	    return(shader);
	}

	public void apply(Pipe p) {
	    p.put(slot, this);
	}

	public String toString() {
	    return(String.format("#<texclip %s>", tex));
	}
    }
    public final TexClip clip = new TexClip(this);
    public static final Pipe.Op noclip = p -> p.put(TexClip.slot, null);

    public void render(GOut g, float[] gc, float[] tc) {
	Coord tdim = sz();
	float ix = 1.0f / tdim.x, iy = 1.0f / tdim.y;
	float[] data = {
	    gc[2], gc[3], tc[2] * ix, tc[3] * iy,
	    gc[4], gc[5], tc[4] * ix, tc[5] * iy,
	    gc[0], gc[1], tc[0] * ix, tc[1] * iy,
	    gc[6], gc[7], tc[6] * ix, tc[7] * iy,
	};
	g.usestate(draw);
	g.out.draw1(g.state(), new Model(Model.Mode.TRIANGLE_STRIP, new VertexArray(vf_tex2d, new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(data))), null, 0, 4));
	g.usestate(ColorTex.slot);
    }

    @Material.ResName("tex")
    public static class $tex implements Material.ResCons2 {
	public static final boolean defclip = true;

	public Material.Res.Resolver cons(final Resource res, Object... args) {
	    final Indir<Resource> tres;
	    final int tid;
	    int a = 0;
	    if(args[a] instanceof String) {
		tres = res.pool.load((String)args[a], Utils.iv(args[a + 1]));
		if(args.length > a + 2)
		    tid = Utils.iv(args[a + 2]);
		else
		    tid = -1;
		a += 3;
	    } else {
		tres = res.indir();
		tid = Utils.iv(args[a]);
		a += 1;
	    }
	    boolean tclip = defclip;
	    while(a < args.length) {
		String f = (String)args[a++];
		if(f.equals("a"))
		    tclip = false;
		else if(f.equals("c"))
		    tclip = true;
	    }
	    final boolean clip = tclip; /* ¦] */
	    return(new Material.Res.Resolver() {
		    public void resolve(Collection<Pipe.Op> buf, Collection<Pipe.Op> dynbuf) {
			TexRender tex;
			TexR rt;
			if(tid >= 0)
			    rt = tres.get().layer(TexR.class, tid);
			else
			    rt = tres.get().layer(TexR.class);
			if(rt != null) {
			    tex = rt.tex();
			} else {
			    throw(new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
			}
			buf.add(tex.draw);
			buf.add(clip ? tex.clip : noclip);
		    }
		});
	}
    }
}
