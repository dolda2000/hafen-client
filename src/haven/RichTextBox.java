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

public class RichTextBox extends Widget {
    private static final int marg = UI.scale(10);
    public final RichText.Foundry fnd;
    public Color bg = Color.BLACK;
    private Indir<? extends RichText.Document> render;
    private RichText text;
    private Scrollbar sb;

    public RichTextBox(Coord sz, RichText.Foundry fnd, Indir<? extends RichText.Document> doc) {
	super(sz);
	this.fnd = fnd;
	this.render = doc;
	this.text = null;
	this.sb = adda(new Scrollbar(sz.y, 0, 0), sz.x, 0, 1, 0);
    }

    public RichTextBox(Coord sz, Indir<? extends RichText.Document> doc) {
	this(sz, RichText.stdf, doc);
    }

    public RichTextBox(Coord sz, String text, RichText.Foundry fnd) {
	this(sz, fnd, () -> new RichText.Document(text));
    }

    public RichTextBox(Coord sz, String text, Object... attrs) {
	this(sz, text, new RichText.Foundry(attrs));
    }

    private void ckrender() {
	if(render != null) {
	    try {
		RichText.Document doc = render.get();
		this.text = (doc == null) ? null : fnd.render(doc, sz.x - (marg * 2));
	    } catch(Loading l) {
		return;
	    }
	    render = null;
	    sb.max = (this.text == null) ? 0 : this.text.sz().y + marg * 2 - sz.y;
	    sb.val = 0;
	}
    }

    public void tick(double dt) {
	super.tick(dt);
	ckrender();
    }

    public void draw(GOut g) {
	if(bg != null) {
	    g.chcolor(bg);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	}
	if(text != null)
	    g.image(text.tex(), Coord.of(marg, marg - sb.val));
	super.draw(g);
    }

    public void set(Indir<? extends RichText.Document> doc) {
	if(doc != null) {
	    render = doc;
	    ckrender();
	} else {
	    render = null;
	    text = null;
	}
    }

    public void set(RichText.Document doc) {
	set(() -> doc);
    }

    public void settext(Indir<String> text) {
	set(() -> new RichText.Document(text.get()));
    }

    public void settext(String text, Object... attrs) {
	set(new RichText.Document(text, attrs));
    }

    /* XXX: Just for binary compatibility */
    public void settext(String text) {
	set(new RichText.Document(text));
    }

    public boolean mousewheel(MouseWheelEvent ev) {
	sb.ch(ev.a * UI.scale(20));
	return(true);
    }

    public void resize(Coord sz) {
	super.resize(sz);
	sb.c = new Coord(sz.x - sb.sz.x, 0);
	sb.resize(sz.y);
    }
}
