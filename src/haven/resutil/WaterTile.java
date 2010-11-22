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

package haven.resutil;

import java.util.*;
import haven.*;
import haven.Resource.Tile;
import haven.MapMesh.Surface;
import javax.media.opengl.*;
import java.awt.Color;

public class WaterTile extends Tiler {
    public final int depth;
    public final Resource.Tileset set;
    
    public static class Bottom extends Surface {
	final MapMesh m;
	boolean[] s;
	
	public Bottom(MapMesh m) {
	    m.super();
	    this.m = m;
	    Coord sz = m.sz;
	    MCache map = m.map;
	    int[] d = new int[(sz.x + 3) * (sz.y + 3)];
	    s = new boolean[(sz.x + 3) * (sz.y + 3)];
	    Coord c = new Coord();
	    int i = 0;
	    for(c.y = -1; c.y <= sz.y + 1; c.y++) {
		for(c.x = -1; c.x <= sz.x + 1; c.x++) {
		    Tiler t = map.tiler(map.gettile(c.add(m.ul)));
		    if(t instanceof WaterTile)
			d[i] = ((WaterTile)t).depth;
		    else
			d[i] = 0;
		    i++;
		}
	    }
	    i = idx(Coord.z);
	    int r = sz.x + 3;
	    for(c.y = 0; c.y <= sz.y; c.y++) {
		for(c.x = 0; c.x <= sz.x; c.x++) {
		    int td = d[i];
		    if(d[i - r - 1] < td)
			td = d[i - r - 1];
		    if(d[i - r] < td)
			td = d[i - r];
		    if(d[i - 1] < td)
			td = d[i - 1];
		    spoint(c).pos.z -= td;
		    if(td == 0)
			s[i] = true;
		    i++;
		}
		i += 2;
	    }
	}
	
	public void calcnrm() {
	    super.calcnrm();
	    Coord c = new Coord();
	    for(c.y = 0; c.y <= m.sz.y; c.y++) {
		for(c.x = 0; c.x <= m.sz.x; c.x++) {
		    if(s[idx(c)])
			spoint(c).nrm = m.gnd().spoint(c).nrm;
		}
	    }
	}
    }
    
    static final TexCube sky = new TexCube(Resource.loadimg("gfx/tiles/skycube"));
    static final Tex srf = Resource.loadtex("gfx/tiles/watertex");
    public static final GLState surfmat = new GLState() {
	    public void apply(GOut g) {
		GL gl = g.gl;
		gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_REFLECTION_MAP);
		gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_REFLECTION_MAP);
		gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_REFLECTION_MAP);
		gl.glEnable(GL.GL_TEXTURE_GEN_S);
		gl.glEnable(GL.GL_TEXTURE_GEN_T);
		gl.glEnable(GL.GL_TEXTURE_GEN_R);
		gl.glEnable(GL.GL_TEXTURE_CUBE_MAP);
		gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, sky.glid(g));
		gl.glDisable(GL.GL_ALPHA_TEST);
		gl.glDisable(GL.GL_LIGHTING);
		gl.glColor4f(1, 1, 1, 0.25f);
		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glPushMatrix();
		/* XXX */
		gl.glRotatef(90 + (float)(g.camdir.xyangle(Coord3f.o) * 180 / Math.PI), 0, 0, 1);
		float a = (float)(new Coord3f(g.camdir.z, (float)Math.sqrt(g.camdir.x * g.camdir.x + g.camdir.y * g.camdir.y), 0).xyangle(Coord3f.o) * 180 / Math.PI);
		gl.glRotatef(a, -1, 0, 0);
		gl.glMatrixMode(GL.GL_MODELVIEW);
	    }
	    
	    public void unapply(GOut g) {
		GL gl = g.gl;
		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glDisable(GL.GL_TEXTURE_CUBE_MAP);
		gl.glDisable(GL.GL_TEXTURE_GEN_S);
		gl.glDisable(GL.GL_TEXTURE_GEN_T);
		gl.glDisable(GL.GL_TEXTURE_GEN_R);
		gl.glEnable(GL.GL_ALPHA_TEST);
		gl.glEnable(GL.GL_LIGHTING);
		gl.glColor3f(1, 1, 1);
	    }
	};
    public static final Material surf2 = new Material(new Color(0, 0, 0, 0),
						      new Color(255, 255, 255, 32),
						      new Color(16, 16, 16, 64),
						      new Color(0, 0, 0, 0),
						      2);
    static {
	surf2.alphatest = false;
	surf2.tex = srf;
    }

    public static class Shallows extends WaterTile {
	public Shallows(int id, Resource.Tileset set) {
	    super(id, set, 5);
	}
    }
    
    public static class Deep extends WaterTile {
	public Deep(int id, Resource.Tileset set) {
	    super(id, set, 30);
	}
    }
    
    public WaterTile(int id, Resource.Tileset set, int depth) {
	super(id);
	this.set = set;
	this.depth = depth;
    }
    
    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	Tile g = set.ground.pick(rnd);
	m.new Plane(m.surf(Bottom.class), lc, 0, g);
	m.new Plane(m.gnd(), lc, 256, surf2);
	m.new Plane(m.gnd(), lc, 257, surfmat);
    }
    
    public void trans(MapMesh m, Random rnd, Coord lc, Coord gc, int z, int bmask, int cmask) {
	if(m.map.gettile(gc) <= id)
	    return;
	if((set.btrans != null) && (bmask > 0))
	    m.new Plane(m.surf(Bottom.class), lc, z, set.btrans[bmask - 1].pick(rnd));
	if((set.ctrans != null) && (cmask > 0))
	    m.new Plane(m.surf(Bottom.class), lc, z, set.ctrans[cmask - 1].pick(rnd));
    }
}
