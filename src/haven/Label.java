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

public class Label extends Widget {
    public final Text.Foundry f;
    public Text text;
    public String texts;
    public Color col = Color.WHITE;

    @RName("lbl")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    if(args.length > 1)
		return(new Label(Utils.sv(args[0]), UI.scale(Utils.iv(args[1]))));
	    else
		return(new Label(Utils.sv(args[0])));
	}
    }

    public Label(String text, int w, Text.Foundry f) {
	super(Coord.z);
	this.f = f;
	this.text = f.renderwrap(texts = text, this.col, w);
	resize(this.text.sz());
    }

    public Label(String text, Text.Foundry f) {
	super(Coord.z);
	this.f = f;
	this.text = f.render(texts = text, this.col);
	resize(this.text.sz());
    }

    public Label(String text, int w) {
	this(text, w, Text.std);
    }

    public Label(String text) {
	this(text, Text.std);
    }

    public void draw(GOut g) {
	g.image(text.tex(), Coord.z);
    }

    public void settext(String text) {
	if(text.equals(this.text.text))
	    return;
	this.text.dispose();
	this.text = f.render(texts = text, col);
	resize(this.text.sz());
    }

    public void setcolor(Color color) {
	if(color.equals(col))
	    return;
	this.text.dispose();
	this.text = f.render(texts, col = color);
	resize(this.text.sz());
    }

    public void dispose() {
	super.dispose();
	this.text.dispose();
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "set") {
	    settext(Utils.sv(args[0]));
	} else if(msg == "col") {
	    setcolor((Color)args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }
}
