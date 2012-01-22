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
import javax.media.opengl.*;

public abstract class TexRT extends TexGL {
    static Map<GL, Collection<TexRT>> current = new WeakHashMap<GL, Collection<TexRT>>();
    private GL incurrent = null;
    public Profile prof = new Profile(300);
    private Profile.Frame curf;
	
    public TexRT(Coord sz) {
	super(sz);
    }
	
    protected abstract boolean subrend(GOut g);
	
    private void rerender(GL gl) {
	if(incurrent != gl) {
	    Collection<TexRT> tc;
	    synchronized(current) {
		tc = current.get(gl);
		if(tc == null) {
		    tc = new HashSet<TexRT>();
		    current.put(gl, tc);
		}
	    }
	    synchronized(tc) {
		tc.add(this);
	    }
	    incurrent = gl;
	}
    }

    public void render(GOut g, Coord c, Coord ul, Coord br, Coord sz) {
	super.render(g, c, ul, br, sz);
	rerender(g.gl);
    }
    
    protected byte[] initdata() {
	return(new byte[tdim.x * tdim.y * 4]);
    }

    protected void fill(GOut g) {
	rerender(g.gl);
	byte[] idat = initdata();
	g.gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, tdim.x, tdim.y, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, (idat == null)?null:java.nio.ByteBuffer.wrap(idat));
	GOut.checkerr(g.gl);
    }
	
    private void subrend2(GOut g) {
	if(t == null)
	    return;
	GL gl = g.gl;
	if(Config.profile)
	    curf = prof.new Frame();
	if(!subrend(g))
	    return;
	if(curf != null)
	    curf.tick("render");
	g.st.prep(draw());
	g.apply();
	gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, dim.x, dim.y);
	GOut.checkerr(gl);
	if(curf != null) {
	    curf.tick("copy");
	    curf.fin();
	    curf = null;
	}
    }
    
    public static void renderall(GOut g) {
	GL gl = g.gl;
	Collection<TexRT> tc;
	synchronized(current) {
	    tc = current.get(gl);
	    current.put(gl, new HashSet<TexRT>());
	}
	if(tc != null) {
	    synchronized(tc) {
		for(TexRT t : tc) {
		    t.incurrent = null;
		    t.subrend2(g);
		}
	    }
	}
    }
}
