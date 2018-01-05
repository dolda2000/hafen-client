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
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

public class Button extends SSWidget {
    public Text text;
    public BufferedImage cont;
    static Text.Foundry tf = new Text.Foundry(Text.serif, 12, Color.YELLOW);
    boolean a = false;
    UI.Grab d = null;
	
    @RName("btn")
    public static class $Btn implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Button((Integer)args[0], (String)args[1]));
	}
    }
    @RName("ltbtn")
    public static class $LTBtn implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(wrapped((Integer)args[0], (String)args[1]));
	}
    }
	
    public static Button wrapped(int w, String text) {
	Button ret = new Button(w, tf.renderwrap(text, w - 10));
	return(ret);
    }
        
    public Button(int w, String text) {
	super(new Coord(w, 19));
	this.text = tf.render(text);
	this.cont = this.text.img;
	render();
    }
        
    public Button(int w, Text text) {
	super(new Coord(w, 19));
	this.text = text;
	this.cont = text.img;
	render();
    }
	
    public Button(int w, BufferedImage cont) {
	super(new Coord(w, 19));
	this.cont = cont;
	render();
    }
	
    public void render() {
	synchronized(this) {
	    Graphics g = graphics();
	    g.setColor(new Color(64, 48, 32));
	    g.fillRect(0, 0, sz.x, sz.y);
	    Coord tc = sz.div(2).add(Utils.imgsz(cont).div(2).inv());
	    if(a)
		tc = tc.add(1, 1);
	    g.drawImage(cont, tc.x, tc.y, null);
	    update();
	}
    }
	
    public void change(String text, Color col) {
	this.text = tf.render(text, col);
	this.cont = this.text.img;
	render();
    }
    
    public void change(String text) {
	change(text, Color.YELLOW);
    }

    public void click() {
	wdgmsg("activate");
    }
    
    public void uimsg(String msg, Object... args) {
	if(msg == "ch") {
	    if(args.length > 1)
		change((String)args[0], (Color)args[1]);
	    else
		change((String)args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }
    
    public void mousemove(Coord c) {
	if(d != null) {
	    boolean a = c.isect(Coord.z, sz);
	    if(a != this.a) {
		this.a = a;
		render();
	    }
	}
    }

    public boolean mousedown(Coord c, int button) {
	if(button != 1)
	    return(false);
	a = true;
	d = ui.grabmouse(this);
	render();
	return(true);
    }
	
    public boolean mouseup(Coord c, int button) {
	if((d != null) && button == 1) {
	    d.remove();
	    d = null;
	    a = false;
	    render();
	    if(c.isect(new Coord(0, 0), sz))
		click();
	    return(true);
	}
	return(false);
    }
}
