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
import java.awt.image.BufferedImage;

public interface MapSource {
    public int gettile(Coord tc);
    public double getfz(Coord tc);
    public Tileset tileset(int t);
    public Tiler tiler(int t);

    static BufferedImage tileimg(MapSource m, BufferedImage[] texes, int t) {
	BufferedImage img = texes[t];
	if(img == null) {
	    Tileset set = m.tileset(t);
	    if(set == null)
		return(null);
	    Resource r = set.getres();
	    Resource.Image ir = r.layer(Resource.imgc);
	    if(ir == null)
		return(null);
	    img = ir.img;
	    texes[t] = img;
	}
	return(img);
    }

    public static BufferedImage drawmap(MapSource m, Area a) {
	Coord sz = a.sz();
	BufferedImage[] texes = new BufferedImage[256];
	BufferedImage buf = TexI.mkbuf(sz);
	Coord c = new Coord();
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		int t = m.gettile(a.ul.add(c));
		if(t < 0) {
		    buf.setRGB(c.x, c.y, 0);
		    continue;
		}
		BufferedImage tex = tileimg(m, texes, t);
		int rgb = 0;
		if(tex != null)
		    rgb = tex.getRGB(Utils.floormod(c.x + a.ul.x, tex.getWidth()),
				     Utils.floormod(c.y + a.ul.y, tex.getHeight()));
		buf.setRGB(c.x, c.y, rgb);
	    }
	}
	for(c.y = 1; c.y < sz.y - 1; c.y++) {
	    for(c.x = 1; c.x < sz.x - 1; c.x++) {
		int t = m.gettile(a.ul.add(c));
		if(t < 0)
		    continue;
		Tiler tl = m.tiler(t);
		if(tl instanceof haven.resutil.Ridges.RidgeTile) {
		    if(haven.resutil.Ridges.brokenp(m, a.ul.add(c))) {
			for(int y = c.y - 1; y <= c.y + 1; y++) {
			    for(int x = c.x - 1; x <= c.x + 1; x++) {
				Color cc = new Color(buf.getRGB(x, y));
				buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, ((x == c.x) && (y == c.y))?1:0.1).getRGB());
			    }
			}
		    }
		}
	    }
	}
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		int t = m.gettile(a.ul.add(c));
		if((m.gettile(a.ul.add(c).add(-1, 0)) > t) ||
		   (m.gettile(a.ul.add(c).add( 1, 0)) > t) ||
		   (m.gettile(a.ul.add(c).add(0, -1)) > t) ||
		   (m.gettile(a.ul.add(c).add(0,  1)) > t))
		    buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
	    }
	}
	return(buf);
    }
}
