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

import java.awt.image.BufferedImage;

public class ICheckBox extends ACheckBox {
    public final Tex up, down, hoverup, hoverdown;
    private final BufferedImage img;
    public boolean h;

    @RName("ichk")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Tex up = Loading.waitfor(ui.sess.getres((Integer)args[0])).layer(Resource.imgc).tex();
	    Tex down = Loading.waitfor(ui.sess.getres((Integer)args[1])).layer(Resource.imgc).tex();
	    Tex hoverup = (args.length > 2) ? Loading.waitfor(ui.sess.getres((Integer)args[1])).layer(Resource.imgc).tex() : up;
	    Tex hoverdown = (args.length > 3) ? Loading.waitfor(ui.sess.getres((Integer)args[1])).layer(Resource.imgc).tex() : down;
	    ICheckBox ret = new ICheckBox(up, down, hoverup, hoverdown);
	    ret.canactivate = true;
	    return(ret);
	}
    }

    public ICheckBox(Tex up, Tex down, Tex hoverup, Tex hoverdown) {
	super(up.sz());
	this.up = up;
	this.down = down;
	this.hoverup = hoverup;
	this.hoverdown = hoverdown;
	if(up instanceof TexI)
	    this.img = ((TexI)up).back;
	else
	    this.img = null;
    }

    public ICheckBox(Tex up, Tex down, Tex hover) {
	this(up, down, hover, down);
    }

    public ICheckBox(Tex up, Tex down) {
	this(up, down, up);
    }

    public ICheckBox(String base, String up, String down, String hoverup, String hoverdown) {
	this(Resource.loadtex(base + up), Resource.loadtex(base + down), Resource.loadtex(base + hoverup), Resource.loadtex(base + hoverdown));
    }
    public ICheckBox(String base, String up, String down, String hover) {
	this(Resource.loadtex(base + up), Resource.loadtex(base + down), Resource.loadtex(base + hover));
    }
    public ICheckBox(String base, String up, String down) {
	this(Resource.loadtex(base + up), Resource.loadtex(base + down));
    }

    public void draw(GOut g) {
	if(!state())
	    g.image(h ? hoverup : up, Coord.z);
	else
	    g.image(h ? hoverdown : down, Coord.z);
        super.draw(g);
    }

    public boolean checkhit(Coord c) {
	if(!c.isect(Coord.z, sz))
	    return(false);
	if((img == null) || img.getRaster().getNumBands() < 4)
	    return(true);
	return(img.getRaster().getSample(c.x, c.y, 3) >= 128);
    }

    public boolean mousedown(Coord c, int button) {
	if((button == 1) && checkhit(c)) {
	    click();
	    return(true);
	}
	return(super.mousedown(c, button));
    }

    public void mousemove(Coord c) {
	this.h = checkhit(c);
    }

    public Object tooltip(Coord c, Widget prev) {
	if(!checkhit(c))
	    return(null);
	return(super.tooltip(c, prev));
    }
}
