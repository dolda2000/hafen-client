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

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class IButton extends SIWidget {
    BufferedImage up, down, hover;
    boolean h = false;
    boolean a = false;
    UI.Grab d = null;

    @RName("ibtn")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new IButton(Resource.loadimg((String)args[0]), Resource.loadimg((String)args[1])));
	}
    }

    public IButton(BufferedImage up, BufferedImage down, BufferedImage hover) {
	super(Utils.imgsz(up));
	this.up = up;
	this.down = down;
	this.hover = hover;
    }

    public IButton(BufferedImage up, BufferedImage down) {
	this(up, down, up);
    }

    public IButton(String base, String up, String down, String hover) {
	this(Resource.loadimg(base + up), Resource.loadimg(base + down), Resource.loadimg(base + (hover == null?up:hover)));
    }

    public void draw(BufferedImage buf) {
	Graphics g = buf.getGraphics();
	if(a)
	    g.drawImage(down, 0, 0, null);
	else if(h)
	    g.drawImage(hover, 0, 0, null);
	else
	    g.drawImage(up, 0, 0, null);
	g.dispose();
    }

    public boolean checkhit(Coord c) {
	if(!c.isect(Coord.z, sz))
	    return(false);
	if(up.getRaster().getNumBands() < 4)
	    return(true);
	return(up.getRaster().getSample(c.x, c.y, 3) >= 128);
    }

    public void click() {
	wdgmsg("activate");
    }

    protected void depress() {
    }

    protected void unpress() {
    }

    public boolean mousedown(Coord c, int button) {
	if(button != 1)
	    return(false);
	if(!checkhit(c))
	    return(false);
	a = true;
	d = ui.grabmouse(this);
	depress();
	redraw();
	return(true);
    }

    public boolean mouseup(Coord c, int button) {
	if((d != null) && button == 1) {
	    d.remove();
	    d = null;
	    mousemove(c);
	    if(checkhit(c)) {
		unpress();
		click();
	    }
	    return(true);
	}
	return(false);
    }

    public void mousemove(Coord c) {
	boolean h = checkhit(c);
	boolean a = false;
	if(d != null) {
	    a = h;
	    h = true;
	}
	if((h != this.h) || (a != this.a)) {
	    this.h = h;
	    this.a = a;
	    redraw();
	}
    }

    public Object tooltip(Coord c, Widget prev) {
	if(!checkhit(c))
	    return(null);
	return(super.tooltip(c, prev));
    }
}
