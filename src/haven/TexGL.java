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
import javax.media.opengl.*;
import static haven.GOut.checkerr;

public abstract class TexGL extends Tex {
    protected TexOb t = null;
    private Object idmon = new Object();
    protected boolean mipmap = false;
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
    
    public static class TexDraw extends GLState {
	public static final Slot<TexDraw> slot = new Slot<TexDraw>(Slot.Type.DRAW, TexDraw.class, HavenPanel.global);
	private static final ShaderMacro[] shaders = {new Tex2D()};
	public final TexGL tex;
	
	public TexDraw(TexGL tex) {
	    this.tex = tex;
	}
	
	public void prep(Buffer buf) {
	    buf.put(slot, this);
	}

	public void apply(GOut g) {
	    GL2 gl = g.gl;
	    g.st.texunit(0);
	    TexClip clip = g.st.get(TexClip.slot);
	    if(clip != null) {
		if(clip.tex != this.tex)
		    throw(new RuntimeException("TexGL does not support different clip and draw textures."));
		gl.glEnable(GL2.GL_ALPHA_TEST);
	    }
	    gl.glBindTexture(GL.GL_TEXTURE_2D, tex.glid(g));
	    if(g.st.prog != null) {
		reapply(g);
	    } else {
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
		gl.glEnable(GL.GL_TEXTURE_2D);
	    }
	}
    
	public void reapply(GOut g) {
	    GL2 gl = g.gl;
	    gl.glUniform1i(g.st.prog.uniform(Tex2D.tex2d), 0);
	}

	public void unapply(GOut g) {
	    GL2 gl = g.gl;
	    g.st.texunit(0);
	    if(g.st.old(TexClip.slot) != null)
		gl.glDisable(GL2.GL_ALPHA_TEST);
	    if(!g.st.usedprog)
		gl.glDisable(GL.GL_TEXTURE_2D);
	}
    
	public ShaderMacro[] shaders() {
	    return(shaders);
	}
    
	public int capply() {
	    return(100);
	}
    
	public int capplyfrom(GLState from) {
	    if(from instanceof TexDraw)
		return(99);
	    return(-1);
	}
    
	public void applyfrom(GOut g, GLState from) {
	    GL2 gl = g.gl;
	    g.st.texunit(0);
	    TexClip clip = g.st.get(TexClip.slot), old = g.st.old(TexClip.slot);
	    if(clip != null) {
		if(clip.tex != this.tex)
		    throw(new RuntimeException("TexGL does not support different clip and draw textures."));
		if(old == null)
		    gl.glEnable(GL2.GL_ALPHA_TEST);
	    } else {
		if(old != null)
		    gl.glDisable(GL2.GL_ALPHA_TEST);
	    }
	    gl.glBindTexture(GL.GL_TEXTURE_2D, tex.glid(g));
	}
	
	public String toString() {
	    return("TexDraw(" + tex + ")");
	}
    }
    private final TexDraw draw = new TexDraw(this);
    public GLState draw() {return(draw);}
    
    public static class TexClip extends GLState {
	public static final Slot<TexClip> slot = new Slot<TexClip>(Slot.Type.GEOM, TexClip.class, HavenPanel.global, TexDraw.slot);
	public final TexGL tex;
	
	public TexClip(TexGL tex) {
	    this.tex = tex;
	}
	
	public void apply(GOut g) {
	    GL2 gl = g.gl;
	    TexDraw draw = g.st.get(TexDraw.slot);
	    if(draw == null) {
		g.st.texunit(0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, tex.glid(g));
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_COMBINE);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_REPLACE);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_RGB, GL2.GL_PREVIOUS);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND0_RGB, GL2.GL_SRC_COLOR);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_ALPHA, GL2.GL_MODULATE);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_ALPHA, GL2.GL_PREVIOUS);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND0_ALPHA, GL2.GL_SRC_ALPHA);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC1_ALPHA, GL2.GL_TEXTURE);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND1_ALPHA, GL2.GL_SRC_ALPHA);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glEnable(GL2.GL_ALPHA_TEST);
	    } else {
		if(draw.tex != this.tex)
		    throw(new RuntimeException("TexGL does not support different clip and draw textures."));
		gl.glEnable(GL2.GL_ALPHA_TEST);
	    }
	}
	
	public void unapply(GOut g) {
	    GL2 gl = g.gl;
	    if(g.st.old(TexDraw.slot) == null) {
		g.st.texunit(0);
		gl.glDisable(GL2.GL_ALPHA_TEST);
		gl.glDisable(GL2.GL_TEXTURE_2D);
	    } else {
		gl.glDisable(GL2.GL_ALPHA_TEST);
	    }
	}
	
	public int capply() {
	    return(100);
	}
	
	public int capplyfrom(GLState from) {
	    if(from instanceof TexClip)
		return(99);
	    return(-1);
	}
	
	public void applyfrom(GOut g, GLState from) {
	    TexDraw draw = g.st.get(TexDraw.slot), old = g.st.old(TexDraw.slot);
	    if((old != null) && (draw != null))
		return;
	    GL2 gl = g.gl;
	    if((old == null) && (draw == null)) {
		g.st.texunit(0);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, tex.glid(g));
	    } else {
		throw(new RuntimeException("TexClip is somehow being transition even though TexDraw is being replaced"));
	    }
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
	fill(g);
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
	
    static {
	Console.setscmd("texdis", new Console.Command() {
		public void run(Console cons, String[] args) {
		    disableall = (Integer.parseInt(args[1]) != 0);
		}
	    });
    }
}
