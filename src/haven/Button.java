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

public class Button extends SIWidget {
    public static final BufferedImage bl = Resource.loadimg("gfx/hud/buttons/tbtn/left");
    public static final BufferedImage br = Resource.loadimg("gfx/hud/buttons/tbtn/right");
    public static final BufferedImage bt = Resource.loadimg("gfx/hud/buttons/tbtn/top");
    public static final BufferedImage bb = Resource.loadimg("gfx/hud/buttons/tbtn/bottom");
    public static final BufferedImage dt = Resource.loadimg("gfx/hud/buttons/tbtn/dtex");
    public static final BufferedImage ut = Resource.loadimg("gfx/hud/buttons/tbtn/utex");
    public static final BufferedImage bm = Resource.loadimg("gfx/hud/buttons/tbtn/mid");
    public static final int hs = bl.getHeight(), hl = bm.getHeight();
    public static final Resource click = Loading.waitfor(Resource.local().load("sfx/hud/btn"));
    public static final Resource.Audio lbtdown = Loading.waitfor(Resource.local().load("sfx/hud/lbtn")).layer(Resource.audio, "down");
    public static final Resource.Audio lbtup   = Loading.waitfor(Resource.local().load("sfx/hud/lbtn")).layer(Resource.audio, "up");
    public boolean lg;
    public Text text;
    public BufferedImage cont;
    public Runnable action = null;
    static Text.Foundry tf = new Text.Foundry(Text.serif.deriveFont(Font.BOLD, 12)).aa(true);
    static Text.Furnace nf = new PUtils.BlurFurn(new PUtils.TexFurn(tf, Window.ctex), 1, 1, new Color(80, 40, 0));
    private boolean a = false;
    private UI.Grab d = null;
	
    @RName("btn")
    public static class $Btn implements Factory {
	public Widget create(UI ui, Object[] args) {
	    if(args.length > 2)
		return(new Button((Integer)args[0], (String)args[1], ((Integer)args[2]) != 0));
	    else
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
        
    private static boolean largep(int w) {
	return(w >= (bl.getWidth() + bm.getWidth() + br.getWidth()));
    }

    private Button(int w, boolean lg) {
	super(new Coord(w, lg?hl:hs));
	this.lg = lg;
    }

    public Button(int w, String text, boolean lg, Runnable action) {
	this(w, lg);
	this.text = nf.render(text);
	this.cont = this.text.img;
	this.action = action;
    }

    public Button(int w, String text, boolean lg) {
	this(w, text, lg, null);
	this.action = () -> wdgmsg("activate");
    }

    public Button(int w, String text, Runnable action) {
	this(w, text, largep(w), action);
    }

    public Button(int w, String text) {
	this(w, text, largep(w));
    }

    public Button(int w, Text text) {
	this(w, largep(w));
	this.text = text;
	this.cont = text.img;
    }
	
    public Button(int w, BufferedImage cont) {
	this(w, largep(w));
	this.cont = cont;
    }
	
    public void draw(BufferedImage img) {
	Graphics g = img.getGraphics();
	int yo = lg?((hl - hs) / 2):0;
	g.drawImage(a?dt:ut, 4, yo + 4, sz.x - 8, hs - 8, null);

	Coord tc = sz.sub(Utils.imgsz(cont)).div(2);
	if(a)
	    tc = tc.add(1, 1);
	g.drawImage(cont, tc.x, tc.y, null);

	g.drawImage(bl, 0, yo, null);
	g.drawImage(br, sz.x - br.getWidth(), yo, null);
	g.drawImage(bt, bl.getWidth(), yo, sz.x - bl.getWidth() - br.getWidth(), bt.getHeight(), null);
	g.drawImage(bb, bl.getWidth(), yo + hs - bb.getHeight(), sz.x - bl.getWidth() - br.getWidth(), bb.getHeight(), null);
	if(lg)
	    g.drawImage(bm, (sz.x - bm.getWidth()) / 2, 0, null);

	g.dispose();
    }
	
    public void change(String text, Color col) {
	this.text = tf.render(text, col);
	this.cont = this.text.img;
	redraw();
    }
    
    public void change(String text) {
	this.text = nf.render(text);
	this.cont = this.text.img;
	redraw();
    }

    public void click() {
	if(action != null)
	    action.run();
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
		redraw();
	    }
	}
    }

    protected void depress() {
	Audio.play(click);
    }

    protected void unpress() {
	Audio.play(click);
    }

    public boolean mousedown(Coord c, int button) {
	if(button != 1)
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
	    a = false;
	    redraw();
	    if(c.isect(new Coord(0, 0), sz)) {
		unpress();
		click();
	    }
	    return(true);
	}
	return(false);
    }
}
