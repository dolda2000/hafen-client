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

import java.awt.*;
import haven.render.*;

public class Speaking extends GAttrib implements RenderTree.Node, PView.Render2D {
    public static final IBox sb = new IBox("gfx/hud/emote", "tl", "tr", "bl", "br", "el", "er", "et", "eb");
    public static final Tex svans = Resource.loadtex("gfx/hud/emote/svans");
    public static final int sx = UI.scale(5);
    public float zo;
    public Text text;
	
    public Speaking(Gob gob, float zo, String text) {
	super(gob);
	this.zo = zo;
	this.text = Text.render(text, Color.BLACK);
    }
	
    public void update(String text) {
	this.text = Text.render(text, Color.BLACK);
    }
	
    public void draw(GOut g, Coord c) {
	Coord sz = text.sz();
	sz.x = Math.max(sz.x, UI.scale(15));
	Coord tl = c.sub(sx, sb.cisz().y + sz.y + svans.sz().y - sb.bb.sz().y);
	Coord ftl = tl.add(sb.btloff());
	g.chcolor(Color.WHITE);
	g.frect(ftl, sz);
	sb.draw(g, tl, sz.add(sb.cisz()));
	g.chcolor(Color.BLACK);
	g.image(text.tex(), ftl);
	g.chcolor(Color.WHITE);
	g.image(svans, c.add(0, -svans.sz().y));
    }

    public void draw(GOut g, Pipe state) {
	Coord sc = Homo3D.obj2view(new Coord3f(0, 0, zo), state, Area.sized(g.sz())).round2();
	draw(g, sc.add(sx, 0));
    }
}
