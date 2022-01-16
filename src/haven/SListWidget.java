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
import java.util.function.*;
import java.awt.image.BufferedImage;

public abstract class SListWidget<I, W extends Widget> extends Widget {
    public I sel;

    public SListWidget(Coord sz) {
	super(sz);
    }

    protected abstract List<? extends I> items();
    protected abstract W makeitem(I item, int idx, Coord sz);

    public void change(I item) {
	this.sel = item;
    }

    public static class ItemWidget<I> extends Widget {
	public final SListWidget<I, ?> list;
	public final I item;

	public ItemWidget(SListWidget<I, ?> list, Coord sz, I item) {
	    super(sz);
	    this.list = list;
	    this.item = item;
	}

	public boolean mousedown(Coord c, int button) {
	    if(super.mousedown(c, button))
		return(true);
	    list.change(item);
	    return(true);
	}
    }

    public static abstract class IconText extends Widget {
	public IconText(Coord sz) {super(sz);}

	protected abstract BufferedImage img();
	protected abstract String text();
	protected int margin() {return(0);}
	protected Text.Foundry foundry() {return(CharWnd.attrf);}

	private Tex img = null;
	private Text text = null;
	public void draw(GOut g) {
	    int m = margin(), h = sz.y - (m * 2);
	    try {
		if(this.img == null) {
		    BufferedImage img = img();
		    if(img.getWidth() > img.getHeight()) {
			if(img.getWidth() != h)
			    img = PUtils.convolve(img, Coord.of(h, (h * img.getHeight()) / img.getWidth()), GobIcon.filter);
		    } else {
			if(img.getHeight() != h)
			    img = PUtils.convolve(img, Coord.of((h * img.getWidth()) / img.getHeight(), h), GobIcon.filter);
		    }
		    this.img = new TexI(img);
		}
		g.image(this.img, Coord.of(m));
	    } catch(Loading l) {
		g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.of(m), Coord.of(sz.y - (m * 2)));
	    }
	    try {
		if(this.text == null)
		    this.text = foundry().render(text());
		g.image(this.text.tex(), Coord.of(sz.y + UI.scale(5), (sz.y - this.text.sz().y) / 2));
	    } catch(Loading l) {
		Tex missing = foundry().render("...").tex();
		g.image(missing, Coord.of(sz.y + UI.scale(5), (sz.y - this.text.sz().y) / 2));
		missing.dispose();
	    }
	}

	public void invalidate() {
	    if(img != null)
		img = null;
	    if(text != null)
		text = null;
	}

	public static IconText of(Coord sz, Indir<Resource> res) {
	    return(new IconText(sz) {
		    public BufferedImage img() {return(res.get().layer(Resource.imgc).img);}
		    public String text() {return(res.get().layer(Resource.tooltip).t);}
		});
	}

	public static IconText of(Coord sz, Supplier<BufferedImage> img, Supplier<String> text) {
	    return(new IconText(sz) {
		    public BufferedImage img() {return(img.get());}
		    public String text() {return(text.get());}
		});
	}
    }
}
