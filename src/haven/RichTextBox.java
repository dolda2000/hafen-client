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

public class RichTextBox extends Widget {
    public Color bg = Color.BLACK;
    private final RichText.Foundry fnd;
    private RichText text;
    private Scrollbar sb;
    
    public RichTextBox(Coord sz, String text, RichText.Foundry fnd) {
	super(sz);
	this.fnd = fnd;
	this.text = fnd.render(text, sz.x - 20);
	this.sb = adda(new Scrollbar(sz.y, 0, this.text.sz().y + 20 - sz.y), sz.x, 0, 1, 0);
    }
    
    public RichTextBox(Coord sz, String text, Object... attrs) {
	this(sz, text, new RichText.Foundry(attrs));
    }
    
    public void draw(GOut g) {
	if(bg != null) {
	    g.chcolor(bg);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	}
	g.image(text.tex(), new Coord(10, 10 - sb.val));
	super.draw(g);
    }
    
    public void settext(String text) {
	this.text = fnd.render(text, sz.x - 20);
	sb.max = this.text.sz().y + 20 - sz.y;
	sb.val = 0;
    }
    
    public boolean mousewheel(Coord c, int amount) {
	sb.ch(amount * 20);
	return(true);
    }

    public void resize(Coord sz) {
	super.resize(sz);
	sb.c = new Coord(sz.x - sb.sz.x, 0);
	sb.resize(sz.y);
    }
}
