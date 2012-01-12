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
    private static final GLShader[] shaders = {
	GLShader.VertexShader.load(TexGL.class, "glsl/tex2d.vert"),
	GLShader.FragmentShader.load(TexGL.class, "glsl/tex2d.frag"),
    };
    
    public static class TexOb extends GLObject {
	public final int id;
	
	public TexOb(GL gl) {
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
    
    public TexGL(Coord sz, Coord tdim) {
	super(sz);
	this.tdim = tdim;
    }

    public TexGL(Coord sz) {
	this(sz, new Coord(nextp2(sz.x), nextp2(sz.y)));
    }
	
    protected abstract void fill(GOut gl);

    public void apply(GOut g) {
	GL gl = g.gl;
	g.st.texunit(0);
	gl.glBindTexture(GL.GL_TEXTURE_2D, glid(g));
	if(g.st.prog != null)
	    reapply(g);
	else
	    gl.glEnable(GL.GL_TEXTURE_2D);
    }
    
    public void reapply(GOut g) {
	GL gl = g.gl;
	gl.glUniform1i(g.st.prog.uniform("tex2d"), 0);
    }

    public void unapply(GOut g) {
	GL gl = g.gl;
	g.st.texunit(0);
	if(!g.st.usedprog)
	    gl.glDisable(GL.GL_TEXTURE_2D);
    }
    
    public GLShader[] shaders() {
	return(shaders);
    }
    
    public int capply() {
	return(100);
    }
    
    public int capplyfrom(GLState from) {
	if(from instanceof TexGL)
	    return(99);
	return(-1);
    }
    
    public void applyfrom(GOut g, GLState from) {
	GL gl = g.gl;
	g.st.texunit(0);
	gl.glBindTexture(GL.GL_TEXTURE_2D, glid(g));
    }

    private void create(GOut g) {
	GL gl = g.gl;
	t = new TexOb(gl);
	gl.glBindTexture(GL.GL_TEXTURE_2D, t.id);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, minfilter);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, magfilter);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapmode);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapmode);
	fill(g);
	checkerr(gl);
    }
	
    protected Color setenv(GL gl) {
	gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
	return(Color.WHITE);
    }
	
    Color blend(GOut g, Color amb) {
	Color c = g.getcolor();
	Color n = new Color((c.getRed() * amb.getRed()) / 255,
			    (c.getGreen() * amb.getGreen()) / 255,
			    (c.getBlue() * amb.getBlue()) / 255,
			    (c.getAlpha() * amb.getAlpha()) / 255);
	return(n);
    }
	
    public float tcx(int x) {
	return(((float)x) / ((float)tdim.x));
    }

    public float tcy(int y) {
	return(((float)y) / ((float)tdim.y));
    }
    
    public void mipmap() {
	mipmap = true;
	minfilter = GL.GL_NEAREST_MIPMAP_NEAREST;
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
	GL gl = g.gl;
	g.st.prep(this);
	g.apply();
	Color amb = blend(g, setenv(gl));
	checkerr(gl);
	if(!disableall) {
	    gl.glBegin(GL.GL_QUADS);
	    float l = ((float)ul.x) / ((float)tdim.x);
	    float t = ((float)ul.y) / ((float)tdim.y);
	    float r = ((float)br.x) / ((float)tdim.x);
	    float b = ((float)br.y) / ((float)tdim.y);
	    gl.glColor4f((float)amb.getRed() / 255.0f,
			 (float)amb.getGreen() / 255.0f,
			 (float)amb.getBlue() / 255.0f,
			 (float)amb.getAlpha() / 255.0f);
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
