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

public class LoginScreen extends Widget {
    Login cur;
    Text error;
    IButton btn;
    static Text.Foundry textf, textfs;
    static Tex bg = Resource.loadtex("gfx/loginscr");
    static Tex logo = Resource.loadtex("gfx/logo");
    Text progress = null;
	
    static {
	textf = new Text.Foundry(new java.awt.Font("Sans", java.awt.Font.PLAIN, 16));
	textfs = new Text.Foundry(new java.awt.Font("Sans", java.awt.Font.PLAIN, 14));
    }
	
    public LoginScreen(Widget parent) {
	super(parent.sz.div(2).sub(bg.sz().div(2)), bg.sz(), parent);
	setfocustab(true);
	parent.setfocus(this);
	new Img(Coord.z, bg, this);
	new Img(new Coord(420, 215).add(logo.sz().div(2).inv()), logo, this);
    }

    private static abstract class Login extends Widget {
	private Login(Coord c, Coord sz, Widget parent) {
	    super(c, sz, parent);
	}
		
	abstract Object[] data();
	abstract boolean enter();
    }

    private class Pwbox extends Login {
	TextEntry user, pass;
	CheckBox savepass;
		
	private Pwbox(String username, boolean save) {
	    super(new Coord(345, 310), new Coord(150, 150), LoginScreen.this);
	    setfocustab(true);
	    new Label(new Coord(0, 0), this, "User name", textf);
	    user = new TextEntry(new Coord(0, 20), new Coord(150, 20), this, username);
	    new Label(new Coord(0, 60), this, "Password", textf);
	    pass = new TextEntry(new Coord(0, 80), new Coord(150, 20), this, "");
	    pass.pw = true;
	    savepass = new CheckBox(new Coord(0, 110), this, "Remember me");
	    savepass.a = save;
	    if(user.text.equals(""))
		setfocus(user);
	    else
		setfocus(pass);
	}
		
	public void wdgmsg(Widget sender, String name, Object... args) {
	}
		
	Object[] data() {
	    return(new Object[] {user.text, pass.text, savepass.a});
	}
		
	boolean enter() {
	    if(user.text.equals("")) {
		setfocus(user);
		return(false);
	    } else if(pass.text.equals("")) {
		setfocus(pass);
		return(false);
	    } else {
		return(true);
	    }
	}
    }
	
    private class Tokenbox extends Login {
	Text label;
	Button btn;
		
	private Tokenbox(String username) {
	    super(new Coord(295, 310), new Coord(250, 100), LoginScreen.this);
	    label = textfs.render("Identity is saved for " + username, java.awt.Color.WHITE);
	    btn = new Button(new Coord(75, 30), 100, this, "Forget me");
	}
		
	Object[] data() {
	    return(new Object[0]);
	}
		
	boolean enter() {
	    return(true);
	}
		
	public void wdgmsg(Widget sender, String name, Object... args) {
	    if(sender == btn) {
		LoginScreen.this.wdgmsg("forget");
		return;
	    }
	    super.wdgmsg(sender, name, args);
	}
		
	public void draw(GOut g) {
	    g.image(label.tex(), new Coord((sz.x / 2) - (label.sz().x / 2), 0));
	    super.draw(g);
	}
    }

    private void mklogin() {
	synchronized(ui) {
	    btn = new IButton(new Coord(373, 460), this, Resource.loadimg("gfx/hud/buttons/loginu"), Resource.loadimg("gfx/hud/buttons/logind"));
	    progress(null);
	}
    }
	
    private void error(String error) {
	synchronized(ui) {
	    if(this.error != null)
		this.error = null;
	    if(error != null)
		this.error = textf.render(error, java.awt.Color.RED);
	}
    }
    
    private void progress(String p) {
	synchronized(ui) {
	    if(progress != null)
		progress = null;
	    if(p != null)
		progress = textf.render(p, java.awt.Color.WHITE);
	}
    }
    
    private void clear() {
	if(cur != null) {
	    ui.destroy(cur);
	    cur = null;
	    ui.destroy(btn);
	    btn = null;
	}
	progress(null);
    }
    
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == btn) {
	    if(cur.enter())
		super.wdgmsg("login", cur.data());
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }
	
    public void uimsg(String msg, Object... args) {
	synchronized(ui) {
	    if(msg == "passwd") {
		clear();
		cur = new Pwbox((String)args[0], (Boolean)args[1]);
		mklogin();
	    } else if(msg == "token") {
		clear();
		cur = new Tokenbox((String)args[0]);
		mklogin();
	    } else if(msg == "error") {
		error((String)args[0]);
	    } else if(msg == "prg") {
		error(null);
		clear();
		progress((String)args[0]);
	    }
	}
    }
    
    public void presize() {
	c = parent.sz.div(2).sub(sz.div(2));
    }
	
    public void draw(GOut g) {
	super.draw(g);
	if(error != null)
	    g.image(error.tex(), new Coord(420 - (error.sz().x / 2), 500));
	if(progress != null)
	    g.image(progress.tex(), new Coord(420 - (progress.sz().x / 2), 350));
    }
	
    public boolean type(char k, java.awt.event.KeyEvent ev) {
	if(k == 10) {
	    if((cur != null) && cur.enter())
		wdgmsg("login", cur.data());
	    return(true);
	}
	return(super.type(k, ev));
    }
}
