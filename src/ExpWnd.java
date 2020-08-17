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

import haven.*;
import haven.Resource.Image;
import haven.Resource.Pagina;
import haven.Resource.Tooltip;
import haven.RichText.Foundry;

public class ExpWnd extends Window {
    public static final Foundry fnd;
    public static Resource sfx;

    static {
	sfx = Loading.waitfor(Resource.local().load("sfx/exp", 1));
	fnd = new Foundry();
    }

    public final Indir<Resource> exp;
    public final int ep;
    private Button close;
    private Img img;
    private Img text;

    public ExpWnd(Indir<Resource> var1, int var2) {
	super(UI.scale(new Coord(300, 50)), "Hey, listen!", true);
	this.exp = var1;
	this.ep = var2;
    }

    public static Widget mkwidget(UI var0, Object... var1) {
	Indir var2 = var0.sess.getres((Integer) var1[0]);
	int var3 = var1.length > 1 ? (Integer) var1[1] : 0;
	return new ExpWnd(var2, var3);
    }

    protected void added() {
	if (this.c.equals(0, 0)) {
	    this.c = new Coord((this.parent.sz.x - this.sz.x) / 2, (this.parent.sz.y / 2 - this.sz.y) / 2);
	}

	Audio.play(sfx);
	super.added();
    }

    public void tick(double var1) {
	if (this.img == null) {
	    Tex var3;
	    String var4;
	    String var5;
	    try {
		var3 = this.exp.get().layer(Resource.imgc).tex();
		Tooltip var6 = this.exp.get().layer(Resource.tooltip);
		var4 = var6 == null ? null : var6.t;
		var5 = this.exp.get().layer(Resource.pagina).text;
	    } catch (Loading var7) {
		return;
	    }

	    if (var4 != null) {
		this.chcap(var4);
	    }

	    this.img = this.add(new Img(var3), 0, UI.scale(10));
	    this.text = this.add(new Img(fnd.render(var5, UI.scale(300)).tex()), var3.sz().x + UI.scale(5), UI.scale(10));
	    if (this.ep > 0) {
		this.add(new Label("Experience points gained: " + this.ep), this.text.c.x, this.text.c.y + this.text.sz.y + UI.scale(10));
	    }

	    Coord var8 = this.contentsz();
	    this.close = this.adda(new Button(UI.scale(100), "Okay!"), var8.x / 2, var8.y + UI.scale(25), 0.5D, 0.0D);
	    this.resize(this.contentsz());
	    this.c = new Coord((this.parent.sz.x - this.sz.x) / 2, (this.parent.sz.y / 2 - this.sz.y) / 2);
	}

    }

    public void wdgmsg(Widget var1, String var2, Object... var3) {
	if (var1 == this.close) {
	    this.wdgmsg("close");
	} else {
	    super.wdgmsg(var1, var2, var3);
	}

    }
}
