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

import java.util.*;
import java.awt.event.KeyEvent;

public class LoginScreen extends Widget {
    public static final Text.Foundry
	textf = new Text.Foundry(Text.sans, 16).aa(true),
	textfs = new Text.Foundry(Text.sans, 14).aa(true);
    public static final Tex bg = Resource.loadtex("gfx/loginscr");
    public static final Position bgc = new Position(UI.scale(420, 300));
    public final Credbox login;
    public final String hostname;
    private Text error, progress;
    private Button optbtn;
    private OptWnd opts;

    private String getpref(String name, String def) {
	return(Utils.getpref(name + "@" + hostname, def));
    }

    private void setpref(String name, String val) {
	Utils.setpref(name + "@" + hostname, val);
    }

    private byte[] gettoken(String name) {
	String sv = getpref("savedtoken-" + name, null);
	if(sv == null)
	    return(null);
	byte[] ret = Utils.hex2byte(sv);
	if(ret.length == 0)
	    return(null);
	return(ret);
    }

    public LoginScreen(String hostname) {
	super(bg.sz());
	this.hostname = hostname;
	setfocustab(true);
	add(new Img(bg), Coord.z);
	optbtn = adda(new Button(UI.scale(100), "Options"), pos("cbl").add(10, -10), 0, 1);
	optbtn.setgkey(GameUI.kb_opt);
	adda(login = new Credbox(), bgc.adds(0, 10), 0.5, 0.0).hide();
    }

    public static final KeyBinding kb_savtoken = KeyBinding.get("login/savtoken", KeyMatch.forchar('R', KeyMatch.M));
    public static final KeyBinding kb_deltoken = KeyBinding.get("login/deltoken", KeyMatch.forchar('F', KeyMatch.M));
    public class Credbox extends Widget {
	public final UserEntry user;
	private final TextEntry pass;
	private final CheckBox savetoken;
	private final Button fbtn;
	private final IButton exec;
	private final Widget pwbox, tkbox;
	private byte[] token = null;
	private boolean inited = false;

	public class UserEntry extends TextEntry {
	    private UserEntry(int w) {
		super(w, "");
	    }

	    protected void changed() {
		checktoken();
	    }
	}

	private Credbox() {
	    super(UI.scale(200, 150));
	    setfocustab(true);
	    Widget prev = add(new Label("User name", textf), 0, 0);
	    add(user = new UserEntry(UI.scale(this.sz.x)), prev.pos("bl").adds(0, 1));
	    setfocus(user);

	    add(pwbox = new Widget(Coord.z), user.pos("bl").adds(0, 10));
	    pwbox.add(prev = new Label("Password", textf), Coord.z);
	    pwbox.add(pass = new TextEntry(UI.scale(this.sz.x), ""), prev.pos("bl").adds(0, 1)).pw = true;
	    pwbox.add(savetoken = new CheckBox("Remember me", true), pass.pos("bl").adds(0, 10));
	    savetoken.setgkey(kb_savtoken);
	    savetoken.settip("Saving your login does not save your password, but rather " +
			    "a randomly generated token that will be used to log in. " +
			    "You can manage your saved tokens in your Account Settings.",
			    true);
	    pwbox.pack();
	    pwbox.hide();

	    add(tkbox = new Widget(new Coord(this.sz.x, 0)), user.pos("bl").adds(0, 10));
	    tkbox.add(prev = new Label("Login saved", textfs), UI.scale(0, 25));
	    tkbox.adda(fbtn = new Button(UI.scale(100), "Forget me"), prev.pos("mid").x(this.sz.x), 1.0, 0.5).action(this::forget);
	    fbtn.setgkey(kb_deltoken);
	    tkbox.pack();
	    tkbox.hide();

	    adda(exec = new IButton("gfx/hud/buttons/login", "u", "d", "o") {
		    protected void depress() {ui.sfx(Button.clbtdown.stream());}
		    protected void unpress() {ui.sfx(Button.clbtup.stream());}
		    public void click() {enter();}
		},
		pos("cmid").y(Math.max(pwbox.pos("bl").y, tkbox.pos("bl").y)).adds(0, 35), 0.5, 0.0);
	    pack();
	}

	private void init() {
	    if(inited)
		return;
	    inited = true;
	    user.rsettext(getpref("loginname", ""));
	    checktoken();
	    if(pwbox.visible && !user.text().equals(""))
		setfocus(pass);
	}

	private void checktoken() {
	    if(this.token != null) {
		Arrays.fill(this.token, (byte)0);
		this.token = null;
	    }
	    byte[] token = gettoken(user.text());
	    if(token == null) {
		tkbox.hide();
		pwbox.show();
	    } else {
		tkbox.show();
		pwbox.hide();
		this.token = token;
	    }
	}

	private void forget() {
	    setpref("savedtoken-" + user.text(), "");
	    checktoken();
	}

	private void enter() {
	    if(user.text().equals("")) {
		setfocus(user);
	    } else if(pwbox.visible && pass.text().equals("")) {
		setfocus(pass);
	    } else {
		LoginScreen.this.wdgmsg("login", creds(), pwbox.visible && savetoken.state());
	    }
	}

	private AuthClient.Credentials creds() {
	    byte[] token = this.token;
	    if(token != null)
		return(new AuthClient.TokenCred(user.text(), Arrays.copyOf(token, token.length)));
	    else
		return(new AuthClient.NativeCred(user.text(), pass.text()));
	}

	public boolean keydown(KeyEvent ev) {
	    if(key_act.match(ev)) {
		enter();
		return(true);
	    }
	    return(super.keydown(ev));
	}

	public void show() {
	    if(!inited)
		init();
	    super.show();
	}
    }

    private void mklogin() {
	login.show();
	progress(null);
    }

    private void error(String error) {
	if(this.error != null)
	    this.error = null;
	if(error != null)
	    this.error = textf.render(error, java.awt.Color.RED);
    }

    private void progress(String p) {
	if(progress != null)
	    progress = null;
	if(p != null)
	    progress = textf.render(p, java.awt.Color.WHITE);
    }

    private void clear() {
	login.hide();
	progress(null);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == optbtn) {
	    if(opts == null) {
		opts = adda(new OptWnd(false) {
			public void hide() {
			    /* XXX */
			    reqdestroy();
			}
		    }, sz.div(2), 0.5, 0.5);
	    } else {
		opts.reqdestroy();
		opts = null;
	    }
	    return;
	} else if(sender == opts) {
	    opts.reqdestroy();
	    opts = null;
	}
	super.wdgmsg(sender, msg, args);
    }

    public void cdestroy(Widget ch) {
	if(ch == opts) {
	    opts = null;
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "login") {
	    mklogin();
	} else if(msg == "error") {
	    error((String)args[0]);
	} else if(msg == "prg") {
	    error(null);
	    clear();
	    progress((String)args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void presize() {
	c = parent.sz.div(2).sub(sz.div(2));
    }

    protected void added() {
	presize();
	parent.setfocus(this);
    }

    public void draw(GOut g) {
	super.draw(g);
	if(error != null)
	    g.aimage(error.tex(), bgc.adds(0, 150), 0.5, 0.0);
	if(progress != null)
	    g.aimage(progress.tex(), bgc.adds(0, 50), 0.5, 0.0);
    }
}
