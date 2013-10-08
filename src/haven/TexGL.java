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

import haven.glsl.*;
import java.awt.Color;
import java.util.*;
import java.awt.image.*;
import java.nio.*;
import javax.media.opengl.*;
import static haven.GOut.checkerr;

public abstract class TexGL extends Tex {
    protected TexOb t = null;
    private Object idmon = new Object();
    protected boolean mipmap = false, centroid = false;
    protected int magfilter = GL.GL_NEAREST, minfilter = GL.GL_NEAREST, wrapmode = GL.GL_REPEAT;
    protected Coord tdim;
    public static boolean disableall = false;
    
    public static class TexOb extends GLObject {
	public final int id;
	
	public TexOb(GL2 gl) {
	    super(gl);
	    int[] buf = new int[1];
	    gl.glGenTextures(1, buf, 0);
	    this.id = buf[0];
	}
	
	protected void delete() {
	    int[] buf = {id};
	    gl.glDeleteTextures(1, buf, 0);
	}
    }
    
    public static final ShaderMacro mkcentroid = new ShaderMacro() {
	    public void modify(ProgramContext prog) {
		Tex2D.get(prog).ipol = Varying.Interpol.CENTROID;
	    }
	};

    public static GLState.TexUnit lbind(GOut g, TexGL tex) {
	GLState.TexUnit sampler = g.st.texalloc();
	sampler.act();
	try {
	    g.gl.glBindTexture(GL.GL_TEXTURE_2D, tex.glid(g));
	    return(sampler);
	} catch(Loading l) {
	    sampler.free();
	    throw(l);
	}
    }

    public static class TexDraw extends GLState {
	public static final Slot<TexDraw> slot = new Slot<TexDraw>(Slot.Type.DRAW, TexDraw.class, HavenPanel.global);
	private static final ShaderMacro[] nshaders = {Tex2D.mod};
	private static final ShaderMacro[] cshaders = {Tex2D.mod, mkcentroid};
	public final TexGL tex;
	private TexUnit sampler;
	
	public TexDraw(TexGL tex) {
	    this.tex = tex;
	}
	
	public void prep(Buffer buf) {
	    buf.put(slot, this);
	}

	public void apply(GOut g) {
	    GL2 gl = g.gl;
	    sampler = lbind(g, tex);
	    if(g.st.prog != null) {
		reapply(g);
	    } else {
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
		gl.glEnable(GL.GL_TEXTURE_2D);
	    }
	}
    
	public void reapply(GOut g) {
	    GL2 gl = g.gl;
	    gl.glUniform1i(g.st.prog.uniform(Tex2D.tex2d), sampler.id);
	}

	public void unapply(GOut g) {
	    GL2 gl = g.gl;
	    sampler.act();
	    if(!g.st.usedprog)
		gl.glDisable(GL.GL_TEXTURE_2D);
	    sampler.free(); sampler = null;
	}
    
	public ShaderMacro[] shaders() {
	    /* XXX: This combinatorial stuff does not seem quite right. */
	    if(tex.centroid)
		return(cshaders);
	    else
		return(nshaders);
	}
    
	public int capply() {
	    return(100);
	}
    
	public int capplyfrom(GLState from) {
	    if(from instanceof TexDraw)
		return(99);
	    return(-1);
	}
    
	public void applyfrom(GOut g, GLState sfrom) {
	    GL2 gl = g.gl;
	    TexDraw from = (TexDraw)sfrom;
	    from.sampler.act();
	    int glid = tex.glid(g);
	    sampler = from.sampler; from.sampler = null;
	    gl.glBindTexture(GL.GL_TEXTURE_2D, glid);
	    if(g.st.pdirty)
		reapply(g);
	}
	
	public String toString() {
	    return("TexDraw(" + tex + ")");
	}
    }
    private final TexDraw draw = new TexDraw(this);
    public GLState draw() {return(draw);}
    
    public static class TexClip extends GLState {
	public static final Slot<TexClip> slot = new Slot<TexClip>(Slot.Type.GEOM, TexClip.class, HavenPanel.global, TexDraw.slot);
	private static final ShaderMacro[] shaders = {Tex2D.clip};
	public final TexGL tex;
	private TexUnit sampler;
	
	public TexClip(TexGL tex) {
	    this.tex = tex;
	}
	
	private void fapply(GOut g) {
	    GL2 gl = g.gl;
	    TexDraw draw = g.st.get(TexDraw.slot);
	    if(draw == null) {
		sampler = lbind(g, tex);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_COMBINE);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_REPLACE);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_RGB, GL2.GL_PREVIOUS);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND0_RGB, GL2.GL_SRC_COLOR);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_ALPHA, GL2.GL_MODULATE);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_ALPHA, GL2.GL_PREVIOUS);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND0_ALPHA, GL2.GL_SRC_ALPHA);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2GL3.GL_SRC1_ALPHA, GL2.GL_TEXTURE);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND1_ALPHA, GL2.GL_SRC_ALPHA);
		gl.glEnable(GL2.GL_TEXTURE_2D);
	    } else {
		if(draw.tex != this.tex)
		    throw(new RuntimeException("TexGL does not support different clip and draw textures."));
	    }
	    gl.glEnable(GL2.GL_ALPHA_TEST);
	}

	private void papply(GOut g) {
	    TexDraw draw = g.st.get(TexDraw.slot);
	    if(draw == null) {
		sampler = lbind(g, tex);
	    } else {
		if(draw.tex != this.tex)
		    throw(new RuntimeException("TexGL does not support different clip and draw textures."));
	    }
	}

	public void apply(GOut g) {
	    if(g.st.prog == null)
		fapply(g);
	    else
		papply(g);
	    if(g.gc.pref.alphacov.val) {
		g.gl.glEnable(GL2.GL_SAMPLE_ALPHA_TO_COVERAGE);
		g.gl.glEnable(GL2.GL_SAMPLE_ALPHA_TO_ONE);
	    }
	}
	
	private void funapply(GOut g) {
	    GL2 gl = g.gl;
	    if(g.st.old(TexDraw.slot) == null) {
		sampler.act();
		gl.glDisable(GL2.GL_TEXTURE_2D);
		sampler.free(); sampler = null;
	    }
	    gl.glDisable(GL2.GL_ALPHA_TEST);
	}

	private void punapply(GOut g) {
	    GL2 gl = g.gl;
	    if(g.st.old(TexDraw.slot) == null) {
		sampler.act();
		sampler.free(); sampler = null;
	    }
	}

	public void unapply(GOut g) {
	    if(!g.st.usedprog)
		funapply(g);
	    else
		punapply(g);
	    if(g.gc.pref.alphacov.val) {
		g.gl.glDisable(GL2.GL_SAMPLE_ALPHA_TO_COVERAGE);
		g.gl.glDisable(GL2.GL_SAMPLE_ALPHA_TO_ONE);
	    }
	}
	
	public ShaderMacro[] shaders() {
	    return(shaders);
	}

	public int capply() {
	    return(100);
	}
	
	public int capplyfrom(GLState from) {
	    if(from instanceof TexClip)
		return(99);
	    return(-1);
	}
	
	public void applyfrom(GOut g, GLState sfrom) {
	    TexDraw draw = g.st.get(TexDraw.slot), old = g.st.old(TexDraw.slot);
	    if((old != null) || (draw != null))
		throw(new RuntimeException("TexClip is somehow being transition even though there is a TexDraw"));
	    GL2 gl = g.gl;
	    TexClip from = (TexClip)sfrom;
	    from.sampler.act();
	    int glid = tex.glid(g);
	    sampler = from.sampler; from.sampler = null;
	    gl.glBindTexture(GL2.GL_TEXTURE_2D, glid);
	}
	
	public void prep(Buffer buf) {
	    buf.put(slot, this);
	}
	
	public String toString() {
	    return("TexClip(" + tex + ")");
	}
    }
    private final TexClip clip = new TexClip(this);
    public GLState clip() {return(clip);}
    
    public TexGL(Coord sz, Coord tdim) {
	super(sz);
	this.tdim = tdim;
    }

    public TexGL(Coord sz) {
	this(sz, new Coord(nextp2(sz.x), nextp2(sz.y)));
    }
	
    protected abstract void fill(GOut gl);

    private void create(GOut g) {
	GL2 gl = g.gl;
	t = new TexOb(gl);
	gl.glBindTexture(GL.GL_TEXTURE_2D, t.id);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, minfilter);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, magfilter);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapmode);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapmode);
	try {
	    fill(g);
	} catch(Loading l) {
	    t.dispose();
	    t = null;
	    throw(l);
	}
	checkerr(gl);
    }
	
    public float tcx(int x) {
	return(((float)x) / ((float)tdim.x));
    }

    public float tcy(int y) {
	return(((float)y) / ((float)tdim.y));
    }
    
    public void mipmap() {
	mipmap = true;
	minfilter = GL.GL_LINEAR_MIPMAP_LINEAR;
	dispose();
    }

    public void magfilter(int filter) {
	magfilter = filter;
	dispose();
    }
    
    public void minfilter(int filter) {
	minfilter = filter;
	dispose();
    }

    public int glid(GOut g) {
	GL gl = g.gl;
	synchronized(idmon) {
	    if((t != null) && (t.gl != gl))
		dispose();
	    if(t == null)
		create(g);
	    return(t.id);
	}
    }

    public void render(GOut g, Coord c, Coord ul, Coord br, Coord sz) {
	GL2 gl = g.gl;
	g.st.prep(draw);
	g.apply();
	checkerr(gl);
	if(!disableall) {
	    gl.glBegin(GL2.GL_QUADS);
	    float l = ((float)ul.x) / ((float)tdim.x);
	    float t = ((float)ul.y) / ((float)tdim.y);
	    float r = ((float)br.x) / ((float)tdim.x);
	    float b = ((float)br.y) / ((float)tdim.y);
	    gl.glTexCoord2f(l, t); gl.glVertex3i(c.x, c.y, 0);
	    gl.glTexCoord2f(r, t); gl.glVertex3i(c.x + sz.x, c.y, 0);
	    gl.glTexCoord2f(r, b); gl.glVertex3i(c.x + sz.x, c.y + sz.y, 0);
	    gl.glTexCoord2f(l, b); gl.glVertex3i(c.x, c.y + sz.y, 0);
	    gl.glEnd();
	    checkerr(gl);
	}
    }
	
    public void dispose() {
	synchronized(idmon) {
	    if(t != null) {
		t.dispose();
		t = null;
	    }
	}
    }

    public BufferedImage get(GOut g) {
	GL2 gl = g.gl;
	g.state2d();
	g.apply();
	GLState.TexUnit s = g.st.texalloc();
	s.act();
	gl.glBindTexture(GL.GL_TEXTURE_2D, glid(g));
	byte[] buf = new byte[tdim.x * tdim.y * 4];
	gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(buf));
	s.free();
	GOut.checkerr(gl);
	WritableRaster raster = Raster.createInterleavedRaster(new DataBufferByte(buf, buf.length), tdim.x, tdim.y, 4 * tdim.x, 4, new int[] {0, 1, 2, 3}, null);
	return(new BufferedImage(TexI.glcm, raster, false, null));
    }

    @Material.ResName("tex")
    public static class $tex implements Material.ResCons2 {
	public void cons(final Resource res, List<GLState> states, List<Material.Res.Resolver> left, Object... args) {
	    final Resource tres;
	    final int tid;
	    int a = 0;
	    if(args[a] instanceof String) {
		tres = Resource.load((String)args[a], (Integer)args[a + 1]);
		tid = (Integer)args[a + 2];
		a += 3;
	    } else {
		tres = res;
		tid = (Integer)args[a];
		a += 1;
	    }
	    boolean tclip = true;
	    while(a < args.length) {
		String f = (String)args[a++];
		if(f.equals("a"))
		    tclip = false;
	    }
	    final boolean clip = tclip; /* ¦] */
	    left.add(new Material.Res.Resolver() {
		    public void resolve(Collection<GLState> buf) {
			Tex tex;
			TexR rt = tres.layer(TexR.class, tid);
			if(rt != null) {
			    tex = rt.tex();
			} else {
			    Resource.Image img = tres.layer(Resource.imgc, tid);
			    if(img != null) {
				tex = img.tex();
			    } else {
				throw(new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
			    }
			}
			buf.add(tex.draw());
			if(clip)
			    buf.add(tex.clip());
		    }
		});
	}
    }
	
    static {
	Console.setscmd("texdis", new Console.Command() {
		public void run(Console cons, String[] args) {
		    disableall = (Integer.parseInt(args[1]) != 0);
		}
	    });
    }
}
