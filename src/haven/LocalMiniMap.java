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

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;

public class LocalMiniMap extends Widget {
    public final MapView mv;
    Tex mapimg = null;
    Coord ultile = null, cgrid = null;
    private final BufferedImage[] texes = new BufferedImage[256];
    
    private BufferedImage tileimg(int t) {
	BufferedImage img = texes[t];
	if(img == null) {
	    Resource r = ui.sess.glob.map.sets[t];
	    if(r == null)
		return(null);
	    Resource.Image ir = r.layer(Resource.imgc);
	    if(ir == null)
		return(null);
	    img = ir.img;
	    texes[t] = img;
	}
	return(img);
    }
    
    public BufferedImage drawmap(Coord ul, Coord sz) {
	MCache m = ui.sess.glob.map;
	BufferedImage buf = TexI.mkbuf(sz);
	Coord c = new Coord();
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		int t = m.gettile(ul.add(c));
		BufferedImage tex = tileimg(t);
		if(tex != null)
		    buf.setRGB(c.x, c.y, tex.getRGB(Utils.floormod(c.x + ul.x, tex.getWidth()),
						    Utils.floormod(c.y + ul.y, tex.getHeight())));
	    }
	}
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		int t = m.gettile(ul.add(c));
		if((m.gettile(ul.add(c).add(-1, 0)) > t) ||
		   (m.gettile(ul.add(c).add( 1, 0)) > t) ||
		   (m.gettile(ul.add(c).add(0, -1)) > t) ||
		   (m.gettile(ul.add(c).add(0,  1)) > t))
		    buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
	    }
	}
	return(buf);
    }

    public LocalMiniMap(Coord c, Coord sz, Widget parent, MapView mv) {
	super(c, sz, parent);
	this.mv = mv;
    }
    
    public void draw(GOut g) {
	Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
	if(pl == null)
	    return;
	Coord plt = pl.rc.div(tilesz);
	Coord plg = plt.div(cmaps);
	if((cgrid == null) || !plg.equals(cgrid)) {
	    try {
		Coord ul = plg.mul(cmaps).sub(cmaps).add(1, 1);
		Tex prev = this.mapimg;
		this.mapimg = new TexI(drawmap(ul, cmaps.mul(3).sub(2, 2)));
		this.ultile = ul;
		this.cgrid = plg;
		if(prev != null)
		    prev.dispose();
	    } catch(Loading l) {}
	}
	if(mapimg != null) {
	    GOut g2 = g.reclip(Window.swbox.tloff(), sz.sub(Window.swbox.bisz()));
	    g2.image(mapimg, ultile.sub(plt).add(sz.div(2)));
	    Window.swbox.draw(g, Coord.z, sz);
	    try {
		synchronized(ui.sess.glob.party.memb) {
		    for(Party.Member m : ui.sess.glob.party.memb.values()) {
			Coord ptc;
			try {
			    ptc = new Coord(m.getc());
			} catch(MCache.LoadingMap e) {
			    ptc = null;
			}
			if(ptc == null)
			    continue;
			ptc = ptc.div(tilesz).sub(plt).add(sz.div(2));
			g2.chcolor(m.col.getRed(), m.col.getGreen(), m.col.getBlue(), 128);
			g2.image(MiniMap.plx.layer(Resource.imgc).tex(), ptc.add(MiniMap.plx.layer(Resource.negc).cc.inv()));
			g2.chcolor();
		    }
		}
	    } catch(Loading l) {}
	}
    }
}
