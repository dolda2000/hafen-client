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

public class MapMesh implements FRendered {
    public final Coord ul, sz;
    public final MCache map;
    private SPoint[] surf;
    private List<Layer> layers;
    public int clickmode;

    public static class SPoint {
	public final Coord3f pos, nrm;
	public SPoint(Coord3f pos, Coord3f nrm) {
	    this.pos = pos; this.nrm = nrm;
	}
    }

    public class Plane {
	public SPoint[] vrt;
	public int z;
	public Tex tex;
	
	public Plane(Coord sc, int z, Tex tex) {
	    vrt = new SPoint[]{spoint(sc),
			       spoint(sc.add(0, 1)),
			       spoint(sc.add(1, 1)),
			       spoint(sc.add(1, 0))};
	    this.z = z;
	    this.tex = tex;
	    reg();
	}
	
	private void build(MeshBuf buf) {
	    int r = tex.sz().x, b = tex.sz().y;
	    MeshBuf.Vertex v1 = buf.new Vertex(vrt[0].pos, vrt[0].nrm, new Coord3f(tex.tcx(0), tex.tcy(0), 0.0f));
	    MeshBuf.Vertex v2 = buf.new Vertex(vrt[1].pos, vrt[1].nrm, new Coord3f(tex.tcx(0), tex.tcy(b), 0.0f));
	    MeshBuf.Vertex v3 = buf.new Vertex(vrt[2].pos, vrt[2].nrm, new Coord3f(tex.tcx(r), tex.tcy(b), 0.0f));
	    MeshBuf.Vertex v4 = buf.new Vertex(vrt[3].pos, vrt[3].nrm, new Coord3f(tex.tcx(r), tex.tcy(0), 0.0f));
	    buf.new Face(v1, v2, v3);
	    buf.new Face(v1, v3, v4);
	}
	
	private void reg() {
	    TexGL tex;
	    if(this.tex instanceof TexGL)
		tex = (TexGL)this.tex;
	    else if((this.tex instanceof TexSI) && (((TexSI)this.tex).parent instanceof TexGL))
		tex = (TexGL)((TexSI)this.tex).parent;
	    else
		throw(new RuntimeException("Cannot use texture for map rendering: " + this.tex));
	    for(Layer l : layers) {
		if((l.tex == tex) && (l.z == z)) {
		    l.pl.add(this);
		    return;
		}
	    }
	    Layer l = new Layer();
	    l.tex = tex;
	    l.z = z;
	    l.pl.add(this);
	    layers.add(l);
	}
    }
    
    private static class Layer {
	TexGL tex;
	int z;
	FastMesh mesh;
	Collection<Plane> pl = new LinkedList<Plane>();
    }
    
    private MapMesh(MCache map, Coord ul, Coord sz) {
	this.map = map;
	this.ul = ul;
	this.sz = sz;
    }
	
    public SPoint spoint(Coord lc) {
	return(surf[lc.x + (lc.y * (sz.y + 1))]);
    }
    
    private static void dotrans(MapMesh m, Random rnd, Coord lc, Coord gc) {
	int tr[][] = new int[3][3];
	int max = -1;
	for(int y = -1; y <= 1; y++) {
	    for(int x = -1; x <= 1; x++) {
		if((x == 0) && (y == 0))
		    continue;
		int tn = m.map.gettile(gc.add(x, y));
		tr[x + 1][y + 1] = tn;
		if(tn > max)
		    max = tn;
	    }
	}
	int bx[] = {0, 1, 2, 1};
	int by[] = {1, 0, 1, 2};
	int cx[] = {0, 2, 2, 0};
	int cy[] = {0, 0, 2, 2};
	for(int i = max; i >= 0; i--) {
	    Tiler t = m.map.tiler(i);
	    if(t == null)
		continue;
	    int bm = 0, cm = 0;
	    for(int o = 0; o < 4; o++) {
		if(tr[bx[o]][by[o]] == i)
		    bm |= 1 << o;
	    }
	    for(int o = 0; o < 4; o++) {
		if((bm & ((1 << o) | (1 << ((o + 1) % 4)))) != 0)
		    continue;
		if(tr[cx[o]][cy[o]] == i)
		    cm |= 1 << o;
	    }
	    if((bm != 0) || (cm != 0))
		t.trans(m, rnd, lc, gc, 255 - i, bm, cm);
	}
    }

    public static MapMesh build(MCache mc, Random rnd, Coord ul, Coord sz) {
	MapMesh m = new MapMesh(mc, ul, sz);
	m.surf = new SPoint[(sz.x + 1) * (sz.y + 1)];
	Coord c = new Coord();
	int i, o;
	i = 0;
	for(c.y = 0; c.y <= sz.y; c.y++) {
	    for(c.x = 0; c.x <= sz.x; c.x++) {
		Coord3f s = new Coord3f(0, -tilesz.y, mc.getz(ul.add(c.x, c.y + 1)));
		Coord3f w = new Coord3f(-tilesz.x, 0, mc.getz(ul.add(c.x - 1, c.y)));
		Coord3f n = new Coord3f(0, tilesz.y, mc.getz(ul.add(c.x, c.y - 1)));
		Coord3f e = new Coord3f(tilesz.x, 0, mc.getz(ul.add(c.x + 1, c.y)));
		Coord3f nrm = (n.cmul(w)).add(e.cmul(n)).add(s.cmul(e)).add(w.cmul(s)).norm();
		m.surf[i++] = new SPoint(new Coord3f(c.x * tilesz.x, c.y * -tilesz.y, mc.getz(ul.add(c))), nrm);
	    }
	}
	m.layers = new ArrayList<Layer>();
	int tz = 1;
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		Coord gc = c.add(ul);
		mc.tiler(mc.gettile(gc)).lay(m, rnd, c, gc);
		dotrans(m, rnd, c, gc);
	    }
	}
	for(Layer l : m.layers) {
	    MeshBuf buf = new MeshBuf();
	    if(l.pl.isEmpty())
		throw(new RuntimeException("Map layer without planes?!"));
	    for(Plane p : l.pl)
		p.build(buf);
	    l.pl = null;
	    l.mesh = buf.mkmesh();
	}
	Collections.sort(m.layers, new Comparator<Layer>() {
		public int compare(Layer a, Layer b) {
		    return(a.z - b.z);
		}
	    });
	m.surf = null;
	return(m);
    }
    
    public void draw(GOut g) {
	for(Layer l : layers) {
	    g.texsel(l.tex.glid(g));
	    l.mesh.draw(g);
	}
	g.texsel(-1);
    }
    
    public void drawflat(GOut g) {
	GL gl = g.gl;
	gl.glBegin(GL.GL_TRIANGLES);
	Coord c = new Coord();
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		if(clickmode == 1)
		    gl.glColor3f((c.x + 1) / 256.0f, (c.y + 1) / 256.0f, 0.0f);
		if(clickmode == 2)
		    gl.glColor3f(0.0f, 0.0f, 0.0f);
		gl.glVertex3f(c.x * tilesz.x, c.y * -tilesz.y, map.getz(ul.add(c)));
		if(clickmode == 2)
		    gl.glColor3f(0.0f, 1.0f, 0.0f);
		gl.glVertex3f(c.x * tilesz.x, (c.y + 1) * -tilesz.y, map.getz(ul.add(c.x, c.y + 1)));
		if(clickmode == 2)
		    gl.glColor3f(1.0f, 1.0f, 0.0f);
		gl.glVertex3f((c.x + 1) * tilesz.x, (c.y + 1) * -tilesz.y, map.getz(ul.add(c.x + 1, c.y + 1)));
		if(clickmode == 2)
		    gl.glColor3f(0.0f, 0.0f, 0.0f);
		gl.glVertex3f(c.x * tilesz.x, c.y * -tilesz.y, map.getz(ul.add(c)));
		if(clickmode == 2)
		    gl.glColor3f(1.0f, 1.0f, 0.0f);
		gl.glVertex3f((c.x + 1) * tilesz.x, (c.y + 1) * -tilesz.y, map.getz(ul.add(c.x + 1, c.y + 1)));
		if(clickmode == 2)
		    gl.glColor3f(1.0f, 0.0f, 0.0f);
		gl.glVertex3f((c.x + 1) * tilesz.x, c.y * -tilesz.y, map.getz(ul.add(c.x + 1, c.y)));
	    }
	}	
	gl.glEnd();
	GOut.checkerr(gl);
    }
    
    public boolean setup(RenderList rl) {
	clickmode = 0;
	return(true);
    }
}
