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
import static haven.ItemInfo.find;
import static haven.Inventory.sqsz;

public class WItem extends Widget implements DTarget {
    private static final int SHOW_QUALITY_ALL = 0;
    private static final int SHOW_QUALITY_AVG = 1;
    private static final int SHOW_QUALITY_MAX = 2;

    public static final Resource missing = Resource.local().loadwait("gfx/invobjs/missing");
    public final GItem item;
    private Resource cspr = null;
    private Message csdt = Message.nil;
    private Tex meter;
    private int meterValue;
    
    public WItem(GItem item) {
	super(sqsz);
	this.item = item;
    }
    
    public void drawmain(GOut g, GSprite spr) {
	spr.draw(g);
    }

    public static BufferedImage shorttip(List<ItemInfo> info) {
	return(ItemInfo.shorttip(info));
    }
    
    public static BufferedImage longtip(GItem item, List<ItemInfo> info) {
	BufferedImage img = ItemInfo.longtip(info);
	Resource.Pagina pg = item.res.get().layer(Resource.pagina);
	if(pg != null)
	    img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, 200).img);
	return(img);
    }
    
    public BufferedImage longtip(List<ItemInfo> info) {
	return(longtip(item, info));
    }
    
    public class ItemTip implements Indir<Tex> {
	private final TexI tex;
	
	public ItemTip(BufferedImage img) {
	    if(img == null)
		throw(new Loading());
	    tex = new TexI(img);
	}
	
	public GItem item() {
	    return(item);
	}
	
	public Tex get() {
	    return(tex);
	}
    }
    
    public class ShortTip extends ItemTip {
	public ShortTip(List<ItemInfo> info) {super(shorttip(info));}
    }
    
    public class LongTip extends ItemTip {
	public LongTip(List<ItemInfo> info) {super(longtip(info));}
    }

    private long hoverstart;
    private ItemTip shorttip = null, longtip = null;
    private List<ItemInfo> ttinfo = null;
    public Object tooltip(Coord c, Widget prev) {
	long now = System.currentTimeMillis();
	if(prev == this) {
	} else if(prev instanceof WItem) {
	    long ps = ((WItem)prev).hoverstart;
	    if(now - ps < 1000)
		hoverstart = now;
	    else
		hoverstart = ps;
	} else {
	    hoverstart = now;
	}
	try {
	    List<ItemInfo> info = item.info();
	    if(info.size() < 1)
		return(null);
	    if(info != ttinfo) {
		shorttip = longtip = null;
		ttinfo = info;
	    }
	    if(now - hoverstart < 1000 && !Config.alwaysShowExtendedTooltips.get()) {
		if(shorttip == null)
		    shorttip = new ShortTip(info);
		return(shorttip);
	    } else {
		if(longtip == null)
		    longtip = new LongTip(info);
		return(longtip);
	    }
	} catch(Loading e) {
	    return("...");
	}
    }

    public abstract class AttrCache<T> {
	private List<ItemInfo> forinfo = null;
	private T save = null;
	
	public T get() {
	    try {
		List<ItemInfo> info = item.info();
		if(info != forinfo) {
		    save = find(info);
		    forinfo = info;
		}
	    } catch(Loading e) {
		return(null);
	    }
	    return(save);
	}
	
	protected abstract T find(List<ItemInfo> info);
    }
    
    public final AttrCache<Color> olcol = new AttrCache<Color>() {
	protected Color find(List<ItemInfo> info) {
	    GItem.ColorInfo cinf = ItemInfo.find(GItem.ColorInfo.class, info);
	    return((cinf == null)?null:cinf.olcol());
	}
    };
    
    public final AttrCache<Tex> itemnum = new AttrCache<Tex>() {
	protected Tex find(List<ItemInfo> info) {
	    GItem.NumberInfo ninf = ItemInfo.find(GItem.NumberInfo.class, info);
	    if(ninf == null) return(null);
	    return(new TexI(Utils.outline2(Text.render(Integer.toString(ninf.itemnum()), Color.WHITE).img, Utils.contrast(Color.WHITE))));
	}
    };

    public final AttrCache<ItemQuality> quality = new AttrCache<ItemQuality>() {
        protected ItemQuality find(List<ItemInfo> info) {
            return ItemQuality.fromItemInfo(info);
        }
    };

    private GSprite lspr = null;
    public void tick(double dt) {
	/* XXX: This is ugly and there should be a better way to
	 * ensure the resizing happens as it should, but I can't think
	 * of one yet. */
	GSprite spr = item.spr();
	if((spr != null) && (spr != lspr)) {
	    Coord sz = new Coord(spr.sz());
	    if((sz.x % sqsz.x) != 0)
		sz.x = sqsz.x * ((sz.x / sqsz.x) + 1);
	    if((sz.y % sqsz.y) != 0)
		sz.y = sqsz.y * ((sz.y / sqsz.y) + 1);
	    resize(sz);
	    lspr = spr;
	}
    }

    public void draw(GOut g) {
	GSprite spr = item.spr();
	if(spr != null) {
	    Coord sz = spr.sz();
	    g.defstate();
	    if(olcol.get() != null)
		g.usestate(new ColorMask(olcol.get()));
	    drawmain(g, spr);
	    g.defstate();
	    if(item.num >= 0) {
		g.atext(Integer.toString(item.num), sz, 1, 1);
	    } else if(itemnum.get() != null) {
		g.aimage(itemnum.get(), sz, 1, 1);
	    }
        if (item.meter > 0) {
            double a = ((double) item.meter) / 100.0;
            int r = (int) ((1 - a) * 255);
            int gr = (int) (a * 255);
            int b = 0;
            g.chcolor(r, gr, b, 255);
            g.frect(new Coord(sz.x - 5, (int) ((1 - a) * sz.y)), new Coord(5, (int) (a * sz.y)));
            g.chcolor();
            // draw percentage when quality is not shown
            if (!Config.showQuality.get() || quality.get() == null) {
                if (meter == null || meterValue != item.meter) {
                    meterValue = item.meter;
                    meter = Text.std.renderstroked(String.format("%d%%", meterValue), Color.WHITE, Color.BLACK).tex();
                }
                g.image(meter, new Coord(0, -5));
            }
        }
        drawquality(g);
	} else {
	    g.image(missing.layer(Resource.imgc).tex(), Coord.z, sz);
	}
    }
    
    public boolean mousedown(Coord c, int btn) {
	if(checkXfer(btn)) {
	    return true;
	} else if(btn == 1) {
	    item.wdgmsg("take", c);
	    return true;
	} else if(btn == 3) {
	    item.wdgmsg("iact", c, ui.modflags());
	    return(true);
	}
	return(false);
    }

    private boolean checkXfer(int button) {
	boolean inv = parent instanceof Inventory;
	if(ui.modshift) {
	    if(ui.modmeta) {
		if(inv) {
		    wdgmsg("transfer-same", this, button == 3);
		    return true;
		}
	    } else if(button == 1) {
		item.wdgmsg("transfer", c);
		return true;
	    }
	} else if(ui.modctrl) {
	    if(ui.modmeta) {
		if(inv) {
		    wdgmsg("drop-same", this, button == 3);
		    return true;
		}
	    } else if(button == 1) {
		item.wdgmsg("drop", c);
		return true;
	    }
	}
	return false;
    }

    public boolean drop(Coord cc, Coord ul) {
	return(false);
    }
	
    public boolean iteminteract(Coord cc, Coord ul) {
	item.wdgmsg("itemact", ui.modflags());
	return(true);
    }

    private void drawquality(GOut g) {
        ItemQuality q = quality.get();
        if (q == null || !Config.showQuality.get())
            return;

        List<ItemQuality.Element> elements = new ArrayList<ItemQuality.Element>();
        switch (Config.showQualityMode.get()) {
            case SHOW_QUALITY_ALL:
                elements.add(q.essence);
                elements.add(q.substance);
                elements.add(q.vitality);
                break;
            case SHOW_QUALITY_AVG:
                elements.add(q.average);
                break;
            case SHOW_QUALITY_MAX:
                elements.add(q.getMaxElement());
                break;
        }

        Coord c = new Coord(0, -4);
        if (Config.showQualityBackground.get()) {
            int w = 0;
            int h = elements.size() > 0 ? elements.get(0).tex().sz().y : 0;
            for (ItemQuality.Element el : elements) {
                w = Math.max(w, el.tex().sz().x);
            }
            g.chcolor(0, 0, 0, 128);
            g.frect(c, new Coord(w + 1, (h - 5) * elements.size() + 5));
            g.chcolor();
        }
        for (ItemQuality.Element el : elements) {
            g.image(el.tex(), c);
            c.y += 10;
        }
    }

    public boolean isSameKind(WItem other) {
        if (other == null)
            return false;
        GSprite thisSpr = this.item.spr();
        GSprite otherSpr = other.item.spr();
        return item.resname().equals(other.item.resname()) && (thisSpr == otherSpr || (thisSpr != null && thisSpr.isSame(otherSpr)));
    }

    public boolean isSameQuality(WItem other) {
        if (other == null)
            return false;
        ItemQuality aq = this.quality.get();
        ItemQuality bq = other.quality.get();
        if (aq != null)
            return aq.equals(bq);
        return (bq == null);
    }
}
