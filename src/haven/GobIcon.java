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
import java.awt.Color;
import java.awt.image.*;

public class GobIcon extends GAttrib {
    private static final int size = UI.scale(20);
    public static final PUtils.Convolution filter = new PUtils.Hanning(1);
    private static final Map<Indir<Resource>, Tex> cache = new WeakHashMap<>();
    public final Indir<Resource> res;
    private Tex tex;

    public GobIcon(Gob g, Indir<Resource> res) {
	super(g);
	this.res = res;
    }

    public Tex tex() {
	if(this.tex == null) {
	    synchronized(cache) {
		if(!cache.containsKey(res)) {
		    Resource.Image img = res.get().layer(Resource.imgc);
		    Tex tex = img.tex();
		    if ((tex.sz().x <= size) && (tex.sz().y <= size)) {
			cache.put(res, tex);
		    } else {
			BufferedImage buf = img.img;
			buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
			Coord tsz;
			if(buf.getWidth() > buf.getHeight())
			    tsz = new Coord(size, (size * buf.getHeight()) / buf.getWidth());
			else
			    tsz = new Coord((size * buf.getWidth()) / buf.getHeight(), size);
			buf = PUtils.convolve(buf, tsz, filter);
			cache.put(res, new TexI(buf));
		    }
		}
		this.tex = cache.get(res);
	    }
	}
	return(this.tex);
    }

    public static Coord sz(Coord sz) {
	return sz.mul((double) size / Math.max(sz.x, sz.y));
    }

    public static class Setting implements java.io.Serializable {
	public boolean show;
    }

    public static class Settings implements java.io.Serializable {
	public Map<Resource.Spec, Setting> settings = new HashMap<>();
	public int tag = -1;

	public Setting get(Resource.Named res) {
	    return(settings.get(res));
	}

	public Setting get(Resource res) {
	    return(get(res.indir()));
	}

	public void receive(int tag, Resource.Spec[] res, Setting[] conf) {
	    Map<Resource.Spec, Setting> nset = new HashMap<>(settings);
	    for(int i = 0; i < res.length; i++) {
		if(!nset.containsKey(res[i]))
		    nset.put(res[i], conf[i]);
	    }
	    this.settings = nset;
	    this.tag = tag;
	}
    }

    public static class SettingsWindow extends Window {
	public final Settings conf;
	private final Runnable save;

	public static class Icon {
	    public final Resource.Spec res;
	    public final Setting conf;
	    public Text name = null;

	    public Icon(Resource.Spec res, Setting conf) {
		this.res = res;
		this.conf = conf;
	    }

	    private Tex img = null;
	    public Tex img() {
		if(this.img == null) {
		    BufferedImage img = res.loadsaved(Resource.remote()).layer(Resource.imgc).img;
		    Coord tsz;
		    if(img.getWidth() > img.getHeight())
			tsz = new Coord(elh, (elh * img.getHeight()) / img.getWidth());
		    else
			tsz = new Coord((elh * img.getWidth()) / img.getHeight(), elh);
		    this.img = new TexI(PUtils.convolve(img, tsz, filter));
		}
		return(this.img);
	    }
	}

	private static final Text.Foundry elf = CharWnd.attrf;
	private static final int elh = elf.height() + UI.scale(2);
	private static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32), found = new Color(255, 255, 0, 32);
	public class IconList extends Searchbox<Icon> {
	    private Coord showc;
	    private List<Icon> ordered = Collections.emptyList();
	    private Map<Resource.Spec, Setting> cur = null;
	    private boolean reorder = false;

	    private IconList(int w, int h) {
		super(w, h, elh);
		this.showc = showc();
	    }

	    private Coord showc() {
		return(new Coord(sz.x - (sb.vis() ? sb.sz.x : 0) - ((elh - CheckBox.sbox.sz().y) / 2) - CheckBox.sbox.sz().x,
				 ((elh - CheckBox.sbox.sz().y) / 2)));
	    }

	    public void tick(double dt) {
		Map<Resource.Spec, Setting> cur = this.cur;
		if(cur != conf.settings) {
		    cur = conf.settings;
		    ArrayList<Icon> ordered = new ArrayList<>();
		    for(Map.Entry<Resource.Spec, Setting> e : cur.entrySet())
			ordered.add(new Icon(e.getKey(), e.getValue()));
		    this.cur = cur;
		    this.ordered = ordered;
		    reorder = true;
		}
		if(reorder) {
		    reorder = false;
		    for(Icon icon : ordered) {
			if(icon.name == null) {
			    try {
				Resource.Tooltip name = icon.res.loadsaved(Resource.remote()).layer(Resource.tooltip);
				icon.name = elf.render((name == null) ? "???" : name.t);
			    } catch(Loading l) {
				reorder = true;
			    }
			}
		    }
		    Collections.sort(ordered, (a, b) -> {
			    if((a.name == null) && (b.name == null))
				return(0);
			    if(a.name == null)
				return(-1);
			    if(b.name == null)
				return(1);
			    return(a.name.text.compareTo(b.name.text));
			});
		}
	    }

	    public Icon listitem(int idx) {return(ordered.get(idx));}
	    public int listitems() {return(ordered.size());}
	    public boolean searchmatch(int idx, String txt) {
		Icon icon = ordered.get(idx);
		if(icon.name == null)
		   return(false);
		return(icon.name.text.toLowerCase().indexOf(txt.toLowerCase()) >= 0);
	    }

	    public void draw(GOut g) {
		this.showc = showc();
		super.draw(g);
	    }

	    protected void drawbg(GOut g) {}
	    public void drawitem(GOut g, Icon icon, int idx) {
		if(soughtitem(idx)) {
		    g.chcolor(found);
		    g.frect(Coord.z, g.sz());
		}
		g.chcolor(((idx % 2) == 0) ? every : other);
		g.frect(Coord.z, g.sz());
		g.chcolor();
		try {
		    g.aimage(icon.img(), new Coord(0, elh / 2), 0.0, 0.5);
		} catch(Loading l) {}
		if(icon.name != null)
		    g.aimage(icon.name.tex(), new Coord(elh + UI.scale(5), elh / 2), 0.0, 0.5);
		g.image(CheckBox.sbox, showc);
		if(icon.conf.show)
		    g.image(CheckBox.smark, showc);
	    }

	    public boolean mousedown(Coord c, int button) {
		int idx = idxat(c);
		if((idx >= 0) && (idx < listitems())) {
		    Icon icon = listitem(idx);
		    Coord ic = c.sub(idxc(idx));
		    if(ic.isect(showc, CheckBox.sbox.sz())) {
			icon.conf.show = !icon.conf.show;
			if(save != null)
			    save.run();
			return(true);
		    }
		}
		return(super.mousedown(c, button));
	    }

	    public boolean keydown(java.awt.event.KeyEvent ev) {
		if(ev.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
		    if(sel != null) {
			sel.conf.show = !sel.conf.show;
			if(save != null)
			    save.run();
		    }
		    return(true);
		}
		return(super.keydown(ev));
	    }
	}

	public SettingsWindow(Settings conf, Runnable save) {
	    super(Coord.z, "Icon settings");
	    this.conf = conf;
	    this.save = save;
	    add(new IconList(UI.scale(250), 25), Coord.z);
	    pack();
	}
    }
}
