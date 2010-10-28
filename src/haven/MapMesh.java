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

import static haven.MCache.tilesz;
import java.util.*;
import javax.media.opengl.*;

public class MapMesh implements Rendered {
    public final Coord sz;
    private SPoint[] surf;
    private FastMesh test;

    public static class SPoint {
	final Coord3f pos, nrm;
	public SPoint(Coord3f pos, Coord3f nrm) {
	    this.pos = pos; this.nrm = nrm;
	}
    }
    
    private MapMesh(Coord sz) {
	this.sz = sz;
    }
    
    public static MapMesh build(MCache mc, final Coord ul, Coord sz) {
	MapMesh m = new MapMesh(sz);
	m.surf = new SPoint[(sz.x + 1) * (sz.y + 1)];
	Coord c = new Coord();
	int i, o;
	i = 0;
	for(c.y = 0; c.y <= sz.y; c.y++) {
	    for(c.x = 0; c.x <= sz.x; c.x++) {
		Coord3f s = new Coord3f(c.x * tilesz.x, (c.y + 1) * tilesz.y, mc.getz(ul.add(c.x, c.y + 1)));
		Coord3f w = new Coord3f((c.x - 1) * tilesz.x, c.y * tilesz.y, mc.getz(ul.add(c.x - 1, c.y)));
		Coord3f n = new Coord3f(c.x * tilesz.x, (c.y - 1) * tilesz.y, mc.getz(ul.add(c.x, c.y - 1)));
		Coord3f e = new Coord3f((c.x + 1) * tilesz.x, c.y * tilesz.y, mc.getz(ul.add(c.x + 1, c.y)));
		Coord3f nrm = (s.cmul(w)).add(e.cmul(s)).add(n.cmul(e)).add(w.cmul(n)).norm();
		m.surf[i++] = new SPoint(new Coord3f(c.x * tilesz.x, c.y * tilesz.y, mc.getz(ul.add(c))), nrm);
	    }
	}
	float[] pos = new float[m.surf.length * 3];
	float[] nrm = new float[m.surf.length * 3];
	for(i = 0, o = 0; i < m.surf.length; i++, o += 3) {
	    pos[o] = m.surf[i].pos.x;
	    pos[o + 1] = m.surf[i].pos.y;
	    pos[o + 2] = m.surf[i].pos.z;
	    nrm[o] = m.surf[i].nrm.x;
	    nrm[o + 1] = m.surf[i].nrm.y;
	    nrm[o + 2] = m.surf[i].nrm.z;
	}
	VertexBuf buf = new VertexBuf(pos, nrm, null);
	short[] ind = new short[sz.x * sz.y * 6];
	i = 0;
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		ind[i] = (short)(c.x + (c.y * (sz.x + 1)));
		ind[i + 1] = (short)((c.x + 1) + (c.y * (sz.x + 1)));
		ind[i + 2] = (short)(c.x + ((c.y + 1) * (sz.x + 1)));
		ind[i + 3] = (short)((c.x + 1) + ((c.y + 1) * (sz.x + 1)));
		ind[i + 4] = (short)(c.x + ((c.y + 1) * (sz.x + 1)));
		ind[i + 5] = (short)((c.x + 1) + (c.y * (sz.x + 1)));
		i += 6;
	    }
	}
	m.test = new FastMesh(buf, ind);
	m.surf = null;
	return(m);
    }
    
    public void draw(GOut g) {
	GL gl = g.gl;
	gl.glBegin(GL.GL_POINTS);
	gl.glColor3f(1.0f, 0.0f, 0.0f);
	gl.glVertex3f(0.0f, 0.0f, 0.0f);
	gl.glEnd();
	GOut.checkerr(gl);
    }
    
    public boolean setup(RenderList rl) {
	rl.add(test, null);
	return(true);
    }
}
