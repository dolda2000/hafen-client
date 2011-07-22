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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;
import haven.GItem.Info;
import static haven.GItem.find;

public class WItem extends Widget implements DTarget {
    public static final Resource missing = Resource.load("gfx/invobjs/missing");
    public final GItem item;
    private Tex mask = null;
    private Resource cmask = null;
    
    public WItem(Coord c, Widget parent, GItem item) {
	super(c, new Coord(30, 30), parent);
	this.item = item;
    }
    
    public void drawmain(GOut g, Tex tex) {
	g.image(tex, Coord.z);
    }

    public static String rendershort(List<Info> info) {
	StringBuilder buf = new StringBuilder();
	GItem.Name nm = find(GItem.Name.class, info);
	if(nm != null) {
	    buf.append(nm.str.text);
	}
	return(buf.toString());
    }

    public Tex shorttip(List<Info> info) {
	String buf = rendershort(info);
	GItem.Contents cont = find(GItem.Contents.class, info);
	if(cont != null)
	    buf += "\n" + rendershort(cont.sub);
	return(RichText.render(buf, 0).tex());
    }
    
    public Tex longtip(List<Info> info) {
	BufferedImage img = GItem.longtip(info);
	Resource.Pagina pg = item.res.get().layer(Resource.pagina);
	if(pg != null)
	    img = GItem.catimgs(Arrays.asList(new BufferedImage[]{img, RichText.render("\n" + pg.text, 200).img}));
	return(new TexI(img));
    }

    private long hoverstart;
    private Tex shorttip = null, longtip = null;
    private List<Info> ttinfo = null;
    public Object tooltip(Coord c, boolean again) {
	long now = System.currentTimeMillis();
	if(!again)
	    hoverstart = now;
	try {
	    List<Info> info = item.info();
	    if(info != ttinfo) {
		shorttip = longtip = null;
		ttinfo = info;
	    }
	    if(now - hoverstart < 1000) {
		if(shorttip == null)
		    shorttip = shorttip(info);
		return(shorttip);
	    } else {
		if(longtip == null)
		    longtip = longtip(info);
		return(longtip);
	    }
	} catch(Loading e) {
	    return("...");
	}
    }

    private List<Info> olinfo = null;
    private Color olcol = null;
    private Color olcol() {
	try {
	    List<Info> info = item.info();
	    if(info != olinfo) {
		olcol = null;
		GItem.ColorInfo cinf = find(GItem.ColorInfo.class, info);
		if(cinf != null)
		    olcol = cinf.olcol();
		olinfo = info;
	    }
	} catch(Loading e) {
	    return(null);
	}
	return(olcol);
    }

    public void draw(GOut g) {
	try {
	    Resource res = item.res.get();
	    Tex tex = res.layer(Resource.imgc).tex();
	    drawmain(g, tex);
	    if(item.num >= 0) {
		g.chcolor(Color.WHITE);
		g.atext(Integer.toString(item.num), tex.sz(), 1, 1);
	    }
	    if(item.meter > 0) {
		double a = ((double)item.meter) / 100.0;
		g.chcolor(255, 255, 255, 64);
		g.fellipse(sz.div(2), new Coord(15, 15), 90, (int)(90 + (360 * a)));
		g.chcolor();
	    }
	    if(olcol() != null) {
		if(cmask != res) {
		    mask = null;
		    if(tex instanceof TexI)
			mask = ((TexI)tex).mkmask();
		    cmask = res;
		}
		if(mask != null) {
		    g.chcolor(olcol());
		    g.image(mask, Coord.z);
		    g.chcolor();
		}
	    }
	} catch(Loading e) {
	    missing.loadwait();
	    g.image(missing.layer(Resource.imgc).tex(), Coord.z, sz);
	}
    }
    
    public boolean mousedown(Coord c, int btn) {
	if(btn == 1) {
	    if(ui.modshift)
		item.wdgmsg("transfer", c);
	    else if(ui.modctrl)
		item.wdgmsg("drop", c);
	    else
		item.wdgmsg("take", c);
	    return(true);
	} else if(btn == 3) {
	    item.wdgmsg("iact", c);
	    return(true);
	}
	return(false);
    }

    public boolean drop(Coord cc, Coord ul) {
	return(false);
    }
	
    public boolean iteminteract(Coord cc, Coord ul) {
	item.wdgmsg("itemact", ui.modflags());
	return(true);
    }
}
