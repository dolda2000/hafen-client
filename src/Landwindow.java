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

import haven.BuddyWnd.GroupSelector;
import haven.Button;
import haven.Label;
import haven.*;
import haven.Window;
import haven.MCache.Overlay;

import java.awt.*;

public class Landwindow extends Window {
    private static final String fmt = "Area: %d m²";
    Widget bn;
    Widget be;
    Widget bs;
    Widget bw;
    Widget refill;
    Widget buy;
    Widget reset;
    Widget dst;
    Widget rebond;
    GroupSelector group;
    Label area;
    Label cost;
    Widget authmeter;
    int auth;
    int acap;
    int adrain;
    boolean offline;
    Coord c1;
    Coord c2;
    Coord cc1;
    Coord cc2;
    Overlay ol;
    MCache map;
    int[] bflags = new int[8];
    Landwindow.PermBox[] perms = new Landwindow.PermBox[4];
    CheckBox homeck;
    private Tex rauth = null;

    public Landwindow(Coord var1, Coord var2) {
	super(new Coord(0, 0), "Stake", false);
	Composer composer = new Composer(this).vmrgn(UI.scale(5));
	cc1 = c1 = var1;
	cc2 = c2 = var2;
	area = new Label("");
	composer.add(area);
	final int width = UI.scale(300);
	authmeter = new Widget(new Coord(width, UI.scale(20))) {
	    public void draw(GOut var1) {
		int var2 = Landwindow.this.auth;
		int var3 = Landwindow.this.acap;
		if (var3 > 0) {
		    var1.chcolor(0, 0, 0, 255);
		    var1.frect(Coord.z, sz);
		    var1.chcolor(128, 0, 0, 255);
		    Coord var4 = sz.sub(2, 2);
		    var4.x = var2 * var4.x / var3;
		    var1.frect(new Coord(1, 1), var4);
		    var1.chcolor();
		    if (Landwindow.this.rauth == null) {
			Color var5 = Landwindow.this.offline ? Color.RED : Color.WHITE;
			Landwindow.this.rauth = new TexI(Utils.outline2(Text.render(String.format("%s/%s", var2, var3), var5).img, Utils.contrast(var5)));
		    }

		    var1.aimage(Landwindow.this.rauth, sz.div(2), 0.5D, 0.5D);
		}

	    }
	};
	composer.add(authmeter);
	final int btnw = UI.scale(140);
	refill = new Button(btnw, "Refill");
	composer.add(refill);
	final int tooltipw = UI.scale(300);
	refill.tooltip = RichText.render("Refill this claim's presence immediately from your current pool of learning points.", tooltipw);
	cost = new Label("Cost: 0");
	composer.add(cost);
	fmtarea();
	final int extw = UI.scale(120);
	bn = new Button(extw, "Extend North");
	composer.addar(width, bn);
	be = new Button(extw, "Extend East");
	bw = new Button(extw, "Extend West");
	composer.addar(width, be, bw);
	bs = new Button(extw, "Extend South");
	composer.addar(width, bs);
	buy = new Button(btnw, "Buy");
	reset = new Button(btnw, "Reset");
	composer.addar(width, buy, reset);
	dst = new Button(btnw, "Declaim");
	rebond = new Button(btnw, "Renew bond");
	composer.addar(width, dst, rebond);
	rebond.tooltip = RichText.render("Create a new bond for this claim, destroying the old one. Costs half of this claim's total presence.", tooltipw);
	composer.add(new Label("Assign permissions to memorized people:"));
	group = new GroupSelector(0) {
	    protected void changed(int var1) {
		super.changed(var1);
		Landwindow.this.updflags();
	    }
	};
	composer.add(group);
	perms[0] = new PermBox("Trespassing", 1);
	composer.add(perms[0]);
	perms[3] = new PermBox("Rummaging", 8);
	composer.add(perms[3]);
	perms[1] = new PermBox("Theft", 2);
	composer.add(perms[1]);
	perms[2] = new PermBox("Vandalism", 4);
	composer.add(perms[2]);
	composer.add(new Label("White permissions also apply to non-memorized people."));
	pack();
    }

    public static Widget mkwidget(UI var0, Object... var1) {
	Coord var2 = (Coord) var1[0];
	Coord var3 = (Coord) var1[1];
	return new Landwindow(var2, var3);
    }

    private void fmtarea() {
	area.settext(String.format("Area: %d m²", (c2.x - c1.x + 1) * (c2.y - c1.y + 1)));
    }

    private void updatecost() {
	cost.settext(String.format("Cost: %d", 10 * ((cc2.x - cc1.x + 1) * (cc2.y - cc1.y + 1) - (c2.x - c1.x + 1) * (c2.y - c1.y + 1))));
    }

    private void updflags() {
	int var1 = bflags[group.group];
	Landwindow.PermBox[] var2 = perms;
	int var3 = var2.length;

	for (int var4 = 0; var4 < var3; ++var4) {
	    Landwindow.PermBox var5 = var2[var4];
	    var5.a = (var1 & var5.fl) != 0;
	}

    }

    protected void added() {
	super.added();
	map = ui.sess.glob.map;
	MapView var1 = getparent(GameUI.class).map;
	var1.enol(0);
	var1.enol(1);
	var1.enol(16);
	ol = map.new Overlay(cc1, cc2, 65536);
    }

    public void destroy() {
	MapView var1 = getparent(GameUI.class).map;
	var1.disol(0);
	var1.disol(1);
	var1.disol(16);
	ol.destroy();
	super.destroy();
    }

    public void uimsg(String var1, Object... var2) {
	if (var1 == "upd") {
	    Coord var3 = (Coord) var2[0];
	    Coord var4 = (Coord) var2[1];
	    c1 = var3;
	    c2 = var4;
	    fmtarea();
	    updatecost();
	} else {
	    int var5;
	    if (var1 == "shared") {
		var5 = (Integer) var2[0];
		int var6 = (Integer) var2[1];
		bflags[var5] = var6;
		if (var5 == group.group) {
		    updflags();
		}
	    } else if (var1 == "auth") {
		auth = (Integer) var2[0];
		acap = (Integer) var2[1];
		adrain = (Integer) var2[2];
		offline = (Integer) var2[3] != 0;
		rauth = null;
	    } else if (var1 == "entime") {
		var5 = (Integer) var2[0];
		authmeter.tooltip = Text.render(String.format("%d:%02d until enabled", var5 / 3600, var5 % 3600 / 60));
	    }
	}

    }

    public void wdgmsg(Widget var1, String var2, Object... var3) {
	if (var1 == bn) {
	    cc1 = cc1.add(0, -1);
	    ol.update(cc1, cc2);
	    updatecost();
	} else if (var1 == be) {
	    cc2 = cc2.add(1, 0);
	    ol.update(cc1, cc2);
	    updatecost();
	} else if (var1 == bs) {
	    cc2 = cc2.add(0, 1);
	    ol.update(cc1, cc2);
	    updatecost();
	} else if (var1 == bw) {
	    cc1 = cc1.add(-1, 0);
	    ol.update(cc1, cc2);
	    updatecost();
	} else if (var1 == buy) {
	    wdgmsg("take", cc1, cc2);
	} else if (var1 == reset) {
	    ol.update(cc1 = c1, cc2 = c2);
	    updatecost();
	} else if (var1 == dst) {
	    wdgmsg("declaim");
	} else if (var1 == rebond) {
	    wdgmsg("bond");
	} else if (var1 == refill) {
	    wdgmsg("refill");
	} else {
	    super.wdgmsg(var1, var2, var3);
	}
    }

    private class PermBox extends CheckBox {
	int fl;

	PermBox(String var2, int var3) {
	    super(var2);
	    fl = var3;
	}

	public void changed(boolean var1) {
	    int var2 = 0;
	    Landwindow.PermBox[] var3 = Landwindow.this.perms;
	    int var4 = var3.length;

	    for (int var5 = 0; var5 < var4; ++var5) {
		Landwindow.PermBox var6 = var3[var5];
		if (var6.a) {
		    var2 |= var6.fl;
		}
	    }

	    Landwindow.this.wdgmsg("shared", Landwindow.this.group.group, var2);
	    Landwindow.this.bflags[Landwindow.this.group.group] = var2;
	}
    }
}
