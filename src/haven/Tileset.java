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
import java.io.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

@Resource.LayerName("tileset2")
public class Tileset extends Resource.Layer {
    private String tn = "gnd";
    public String[] tags = {};
    public Object[] ta = new Object[0];
    private transient Tiler.Factory tfac;
    public WeightList<Indir<Resource>> flavobjs = new WeightList<Indir<Resource>>();
    public GLState flavobjmat = null;
    public WeightList<Tile> ground;
    public WeightList<Tile>[] ctrans, btrans;
    public int flavprob;

    @Resource.LayerName("tile")
    public static class Tile extends Resource.Layer {
	transient BufferedImage img;
	transient private Tex tex;
	public final int id;
	public final int w;
	public final char t;
		
	public Tile(Resource res, Message buf) {
	    res.super();
	    t = (char)buf.uint8();
	    id = buf.uint8();
	    w = buf.uint16();
	    try {
		img = ImageIO.read(new MessageInputStream(buf));
	    } catch(IOException e) {
		throw(new Resource.LoadException(e, res));
	    }
	    if(img == null)
		throw(new Resource.LoadException("Invalid image data in " + res.name, res));
	}

	public synchronized Tex tex() {
	    if(tex == null)
		tex = new TexI(img);
	    return(tex);
	}
		
	public void init() {}
    }

    private Tileset(Resource res) {
	res.super();
    }

    public Tileset(Resource res, Message buf) {
	res.super();
	while(!buf.eom()) {
	    int p = buf.uint8();
	    switch(p) {
	    case 0:
		tn = buf.string();
		ta = buf.list();
		break;
	    case 1:
		int flnum = buf.uint16();
		flavprob = buf.uint16();
		for(int i = 0; i < flnum; i++) {
		    String fln = buf.string();
		    int flv = buf.uint16();
		    int flw = buf.uint8();
		    try {
			flavobjs.add(res.pool.load(fln, flv), flw);
		    } catch(RuntimeException e) {
			throw(new Resource.LoadException("Illegal resource dependency", e, res));
		    }
		}
		break;
	    case 2:
		tags = new String[buf.int8()];
		for(int i = 0; i < tags.length; i++)
		    tags[i] = buf.string();
		Arrays.sort(tags);
		break;
	    default:
		throw(new Resource.LoadException("Invalid tileset part " + p + "  in " + res.name, res));
	    }
	}
    }

    public Tiler.Factory tfac() {
	synchronized(this) {
	    if(tfac == null) {
		Resource.CodeEntry ent = getres().layer(Resource.CodeEntry.class);
		if(ent != null) {
		    tfac = ent.get(Tiler.Factory.class);
		} else {
		    if((tfac = Tiler.byname(tn)) == null)
			throw(new RuntimeException("Invalid tiler name in " + getres().name + ": " + tn));
		}
	    }
	    return(tfac);
	}
    }

    private void packtiles(Collection<Tile> tiles, Coord tsz) {
	if(tiles.size() < 1)
	    return;
	int min = -1, minw = -1, minh = -1, mine = -1;
	final int nt = tiles.size();
	for(int i = 1; i <= nt; i++) {
	    int w = Tex.nextp2(tsz.x * i);
	    int h;
	    if((nt % i) == 0)
		h = nt / i;
	    else
		h = (nt / i) + 1;
	    h = Tex.nextp2(tsz.y * h);
	    int a = w * h;
	    int e = (w < h)?h:w;
	    if((min == -1) || (a < min) || ((a == min) && (e < mine))) {
		min = a;
		minw = w;
		minh = h;
		mine = e;
	    }
	}
	final Tile[] order = new Tile[nt];
	final Coord[] place = new Coord[nt];
	Tex packbuf = new TexL(new Coord(minw, minh)) {
		{
		    mipmap(Mipmapper.avg);
		    minfilter(javax.media.opengl.GL2.GL_NEAREST_MIPMAP_LINEAR);
		    centroid = true;
		}

		public BufferedImage fill() {
		    BufferedImage buf = TexI.mkbuf(dim);
		    Graphics g = buf.createGraphics();
		    for(int i = 0; i < nt; i++)
			g.drawImage(order[i].img, place[i].x, place[i].y, null);
		    g.dispose();
		    return(buf);
		}

		public String toString() {
		    return("TileTex(" + getres().name + ")");
		}

		public String loadname() {
		    return("tileset in " + getres().name);
		}
	    };
	int x = 0, y = 0, n = 0;
	for(Tile t :  tiles) {
	    if(y >= minh)
		throw(new Resource.LoadException("Could not pack tiles into calculated minimum texture", getres()));
	    order[n] = t;
	    place[n] = new Coord(x, y);
	    t.tex = new TexSI(packbuf, place[n], tsz);
	    n++;
	    if((x += tsz.x) > (minw - tsz.x)) {
		x = 0;
		y += tsz.y;
	    }
	}
    }

    @SuppressWarnings("unchecked")
    public void init() {
	WeightList<Tile> ground = new WeightList<Tile>();
	WeightList<Tile>[] ctrans = new WeightList[15];
	WeightList<Tile>[] btrans = new WeightList[15];
	for(int i = 0; i < 15; i++) {
	    ctrans[i] = new WeightList<Tile>();
	    btrans[i] = new WeightList<Tile>();
	}
	int cn = 0, bn = 0;
	Collection<Tile> tiles = new LinkedList<Tile>();
	Coord tsz = null;
	for(Tile t : getres().layers(Tile.class)) {
	    if(t.t == 'g') {
		ground.add(t, t.w);
	    } else if(t.t == 'b') {
		btrans[t.id - 1].add(t, t.w);
		bn++;
	    } else if(t.t == 'c') {
		ctrans[t.id - 1].add(t, t.w);
		cn++;
	    }
	    tiles.add(t);
	    if(tsz == null) {
		tsz = Utils.imgsz(t.img);
	    } else {
		if(!Utils.imgsz(t.img).equals(tsz)) {
		    throw(new Resource.LoadException("Different tile sizes within set", getres()));
		}
	    }
	}
	if(ground.size() > 0)
	    this.ground = ground;
	if(cn > 0)
	    this.ctrans = ctrans;
	if(bn > 0)
	    this.btrans = btrans;
	packtiles(tiles, tsz);
    }

    /* Only for backwards compatibility */
    @Resource.LayerName("tileset")
    public static class OrigTileset implements Resource.LayerFactory<Tileset> {
	public Tileset cons(Resource res, Message buf) {
	    Tileset ret = new Tileset(res);
	    int fl = buf.uint8();
	    int flnum = buf.uint16();
	    ret.flavprob = buf.uint16();
	    for(int i = 0; i < flnum; i++) {
		String fln = buf.string();
		int flv = buf.uint16();
		int flw = buf.uint8();
		try {
		    ret.flavobjs.add(res.pool.load(fln, flv), flw);
		} catch(RuntimeException e) {
		    throw(new Resource.LoadException("Illegal resource dependency", e, res));
		}
	    }
	    return(ret);
	}
    }
}
