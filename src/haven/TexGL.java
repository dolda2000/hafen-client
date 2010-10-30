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
    protected int id = -1;
    protected GL mygl = null;
    private Object idmon = new Object();
    protected Coord tdim;
    protected static Map<GL, Collection<Integer>> disposed = new HashMap<GL, Collection<Integer>>();
    public static boolean disableall = false;
    
    public TexGL(Coord sz) {
	super(sz);
	tdim = new Coord(nextp2(sz.x), nextp2(sz.y));
    }
	
    protected abstract void fill(GOut gl);

    private void create(GOut g) {
	GL gl = g.gl;
	int[] buf = new int[1];
	gl.glGenTextures(1, buf, 0);
	id = buf[0];
	mygl = gl;
	gl.glBindTexture(GL.GL_TEXTURE_2D, id);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
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

    public int glid(GOut g) {
	GL gl = g.gl;
	synchronized(idmon) {
	    if((id != -1) && (mygl != gl)) {
		dispose(mygl, id);
		id = -1;
	    }
	    if(id < 0)
		create(g);
	    return(id);
	}
    }

    public void render(GOut g, Coord c, Coord ul, Coord br, Coord sz) {
	GL gl = g.gl;
	g.texsel(glid(g));
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
	
    private static void dispose(GL gl, int id) {
	Collection<Integer> dc;
	synchronized(disposed) {
	    dc = disposed.get(gl);
	    if(dc == null) {
		dc = new LinkedList<Integer>();
		disposed.put(gl, dc);
	    }
	}
	synchronized(dc) {
	    dc.add(id);
	}
    }
	
    public void dispose() {
	synchronized(idmon) {
	    if(id == -1)
		return;
	    dispose(mygl, id);
	    id = -1;
	}
    }
	
    protected void finalize() {
	dispose();
    }
	
    public static void disposeall(GL gl) {
	Collection<Integer> dc;
	synchronized(disposed) {
	    dc = disposed.get(gl);
	    if(dc == null)
		return;
	}
	synchronized(dc) {
	    if(dc.isEmpty())
		return;
	    int[] da = new int[dc.size()];
	    int i = 0;
	    for(int id : dc)
		da[i++] = id;
	    dc.clear();
	    gl.glDeleteTextures(da.length, da, 0);
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
