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

import java.awt.event.KeyEvent;

public class OptWnd extends Window {
    public final Panel main, video, audio, keybind;
    public Panel current;

    public void chpanel(Panel p) {
	Coord cc = this.c.add(this.sz.div(2));
	if(current != null)
	    current.hide();
	(current = p).show();
	pack();
	move(cc.sub(this.sz.div(2)));
    }

    public class PButton extends Button {
	public final Panel tgt;
	public final int key;

	public PButton(int w, String title, int key, Panel tgt) {
	    super(w, title);
	    this.tgt = tgt;
	    this.key = key;
	}

	public void click() {
	    chpanel(tgt);
	}

	public boolean keydown(java.awt.event.KeyEvent ev) {
	    if((this.key != -1) && (ev.getKeyChar() == this.key)) {
		click();
		return(true);
	    }
	    return(false);
	}
    }

    public class Panel extends Widget {
	public Panel() {
	    visible = false;
	    c = Coord.z;
	}
    }

    private void error(String msg) {
	GameUI gui = getparent(GameUI.class);
	if(gui != null)
	    gui.error(msg);
    }

    public class VideoPanel extends Panel {
	Panel prev;

	public VideoPanel(Panel prev) {
	    super();
	    this.prev = prev;
	}

	public class CPanel extends Widget {
	    public GSettings prefs;

	    public CPanel(GSettings gprefs) {
		this.prefs = gprefs;
		Composer composer = new Composer(this)
		    .vmrgn(UI.scale(5))
		    .hmrgn(UI.scale(5));
		composer.add(new CheckBox("Render shadows") {
			{a = prefs.lshadow.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.lshadow, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    });
		composer.add(new Label("Render scale"));
		{
		    Label dpy = new Label("");
		    final int steps = 4;
		    composer.addr(
			new HSlider(UI.scale(160), -2 * steps, 2 * steps, (int)Math.round(steps * Math.log(prefs.rscale.val) / Math.log(2.0f))) {
			    protected void added() {
				dpy();
			    }
			    void dpy() {
				dpy.settext(String.format("%.2f\u00d7", Math.pow(2, this.val / (double)steps)));
			    }
			    public void changed() {
				try {
				    float val = (float)Math.pow(2, this.val / (double)steps);
				    ui.setgprefs(prefs = prefs.update(null, prefs.rscale, val));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				dpy();
			    }
			},
			dpy
		    );
		}
		composer.add(new CheckBox("Vertical sync") {
			{a = prefs.vsync.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.vsync, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    });
		composer.add(new Label("Framerate limit (active window)"));
		{
		    Label dpy = new Label("");
		    final int max = 250;
		    composer.addr(
			    new HSlider(UI.scale(160), 1, max, (prefs.hz.val == Float.POSITIVE_INFINITY) ? max : prefs.hz.val.intValue()) {
			    protected void added() {
				dpy();
			    }
			    void dpy() {
				if(this.val == max)
				    dpy.settext("None");
				else
				    dpy.settext(Integer.toString(this.val));
			    }
			    public void changed() {
				try {
				    if(this.val > 10)
					this.val = (this.val / 2) * 2;
				    float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				    ui.setgprefs(prefs = prefs.update(null, prefs.hz, val));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				dpy();
			    }
			},
			dpy
		    );
		}
		composer.add(new Label("Framerate limit (background window)"));
		{
		    Label dpy = new Label("");
		    final int max = 250;
		    composer.addr(
			    new HSlider(UI.scale(160), 1, max, (prefs.bghz.val == Float.POSITIVE_INFINITY) ? max : prefs.bghz.val.intValue()) {
			    protected void added() {
				dpy();
			    }
			    void dpy() {
				if(this.val == max)
				    dpy.settext("None");
				else
				    dpy.settext(Integer.toString(this.val));
			    }
			    public void changed() {
				try {
				    if(this.val > 10)
					this.val = (this.val / 2) * 2;
				    float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				    ui.setgprefs(prefs = prefs.update(null, prefs.bghz, val));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				dpy();
			    }
			},
			dpy
		    );
		}
		composer.add(new Label("Frame sync mode"));
		{
		    boolean[] done = {false};
		    RadioGroup grp = new RadioGroup(this, composer) {
			    public void changed(int btn, String lbl) {
				if(!done[0])
				    return;
				try {
				    ui.setgprefs(prefs = prefs.update(null, prefs.syncmode, JOGLPanel.SyncMode.values()[btn]));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
			    }
			};
		    composer.hmrgn(UI.scale(5));
		    composer.add(new Label("\u2191 Better performance, worse latency"));
		    grp.add("One-frame overlap");
		    grp.add("Tick overlap");
		    grp.add("CPU-sequential");
		    grp.add("GPU-sequential");
		    composer.add(new Label("\u2193 Worse performance, better latency"));
		    grp.check(prefs.syncmode.val.ordinal());
		    done[0] = true;
		}
		/* XXXRENDER
		composer.add(new CheckBox("Antialiasing") {
			{a = cf.fsaa.val;}

			public void set(boolean val) {
			    try {
				cf.fsaa.set(val);
			    } catch(GLSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			    cf.dirty = true;
			}
		    });
		composer.add(new Label("Anisotropic filtering"));
		if(cf.anisotex.max() <= 1) {
		    composer.add(new Label("(Not supported)"));
		} else {
		    final Label dpy = new Label("");
		    composer.addRow(
			    new HSlider(UI.scale(160), (int)(cf.anisotex.min() * 2), (int)(cf.anisotex.max() * 2), (int)(cf.anisotex.val * 2)) {
			    protected void added() {
				dpy();
			    }
			    void dpy() {
				if(val < 2)
				    dpy.settext("Off");
				else
				    dpy.settext(String.format("%.1f\u00d7", (val / 2.0)));
			    }
			    public void changed() {
				try {
				    cf.anisotex.set(val / 2.0f);
				} catch(GLSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				dpy();
				cf.dirty = true;
			    }
			},
			dpy
		    );
		}
		*/
		composer.add(new Label("UI scale (requires restart)"));
		{
		    Label dpy = new Label("");
		    float multiplier = 4;
		    composer.addr(
			new HSlider(UI.scale(160), Math.round(multiplier), Math.round(3 * multiplier), Math.round(prefs.uiscale.val * multiplier)) {
			    protected void added() {
				dpy();
			    }
			    void dpy() {
				dpy.settext(String.format("%.2f\u00d7", this.val / multiplier));
			    }
			    public void changed() {
				try {
				    float val = this.val / multiplier;
				    ui.setgprefs(prefs = prefs.update(null, prefs.uiscale, val));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				dpy();
			    }
			},
			dpy
		    );
		}
		composer.add(new Button(UI.scale(200), "Reset to defaults") {
			public void click() {
			    ui.setgprefs(GSettings.defaults());
			    curcf.destroy();
			    curcf = null;
			    back.destroy();
			    back = null;
			}
		    });
		pack();
	    }
	}

	private CPanel curcf = null;
	private PButton back = null;

	public void attach(UI ui) {
	    super.attach(ui);
	    if (curcf != null && back != null) {
		return;
	    }
	    if (curcf != null) {
		curcf.destroy();
	    }
	    curcf = new CPanel(ui.gprefs);
	    if (back != null) {
		back.destroy();
	    }
	    back = new PButton(UI.scale(200), "Back", 27, prev);
	    Composer composer = new Composer(this);
	    composer.add(curcf);
	    composer.add(back);
	    pack();
	}

	public void draw(GOut g) {
	    if((curcf == null) || (ui.gprefs != curcf.prefs)) {
		if(curcf != null)
		    curcf.destroy();
		if(back != null)
		    back.destroy();
		curcf = new CPanel(ui.gprefs);
		back = new PButton(UI.scale(200), "Back", 27, prev);
		Composer composer = new Composer(this).vmrgn(UI.scale(5));
		composer.add(curcf);
		composer.add(back);
		pack();
	    }
	    super.draw(g);
	}
    }

    private static final Text kbtt = RichText.render("$col[255,255,0]{Escape}: Cancel input\n" +
						     "$col[255,255,0]{Backspace}: Revert to default\n" +
						     "$col[255,255,0]{Delete}: Disable keybinding", 0);
    public class BindingPanel extends Panel {
	private void addbtn(Composer cont, int width, String nm, KeyBinding cmd) {
	    cont.addrf(width / 2, new Label(nm), new SetButton(width / 2, cmd));
	}

	public BindingPanel(Panel back) {
	    super();
	    Scrollport scrollport = new Scrollport(UI.scale(new Coord(300, 300)));
	    Composer scroll = new Composer(scrollport.cont)
		.vmrgn(UI.scale(5))
		.hpad(UI.scale(5));
	    int width = scrollport.cont.sz.x - UI.scale(5);
	    scroll.adda(new Label("Main menu"), width / 2, 0.5);
	    addbtn(scroll, width, "Inventory", GameUI.kb_inv);
	    addbtn(scroll, width, "Equipment", GameUI.kb_equ);
	    addbtn(scroll, width, "Character sheet", GameUI.kb_chr);
	    addbtn(scroll, width, "Map window", GameUI.kb_map);
	    addbtn(scroll, width, "Kith & Kin", GameUI.kb_bud);
	    addbtn(scroll, width, "Options", GameUI.kb_opt);
	    addbtn(scroll, width, "Search actions", GameUI.kb_srch);
	    addbtn(scroll, width, "Toggle chat", GameUI.kb_chat);
	    addbtn(scroll, width, "Quick chat", ChatUI.kb_quick);
	    addbtn(scroll, width, "Display claims", GameUI.kb_claim);
	    addbtn(scroll, width, "Display villages", GameUI.kb_vil);
	    addbtn(scroll, width, "Display realms", GameUI.kb_rlm);
	    addbtn(scroll, width, "Take screenshot", GameUI.kb_shoot);
	    addbtn(scroll, width, "Toggle UI", GameUI.kb_hide);
	    scroll.adda(new Label("Camera control"), width / 2, 0.5);
	    addbtn(scroll, width, "Rotate left", MapView.kb_camleft);
	    addbtn(scroll, width, "Rotate right", MapView.kb_camright);
	    addbtn(scroll, width, "Zoom in", MapView.kb_camin);
	    addbtn(scroll, width, "Zoom out", MapView.kb_camout);
	    addbtn(scroll, width, "Reset", MapView.kb_camreset);
	    scroll.adda(new Label("Walking speed"), width / 2, 0.5);
	    addbtn(scroll, width, "Increase speed", Speedget.kb_speedup);
	    addbtn(scroll, width, "Decrease speed", Speedget.kb_speeddn);
	    for(int i = 0; i < 4; i++)
		addbtn(scroll, width, String.format("Set speed %d", i + 1), Speedget.kb_speeds[i]);
	    scroll.adda(new Label("Combat actions"), width / 2, 0.5);
	    for(int i = 0; i < Fightsess.kb_acts.length; i++)
		addbtn(scroll, width, String.format("Combat action %d", i + 1), Fightsess.kb_acts[i]);
	    addbtn(scroll, width, "Switch targets", Fightsess.kb_relcycle);
	    Composer composer = new Composer(this).vmrgn(UI.scale(5));
	    composer.adda(scrollport, scrollport.cont.sz.x / 2, 0.5);
	    composer.vmrgn(0);
	    composer.adda(new PointBind(UI.scale(200)), scrollport.cont.sz.x / 2, 0.5);
	    composer.adda(new PButton(UI.scale(200), "Back", 27, back), scrollport.cont.sz.x / 2, 0.5);
	    pack();
	}

	public class SetButton extends KeyMatch.Capture {
	    public final KeyBinding cmd;

	    public SetButton(int w, KeyBinding cmd) {
		super(w, cmd.key());
		this.cmd = cmd;
	    }

	    public void set(KeyMatch key) {
		super.set(key);
		cmd.set(key);
	    }

	    protected KeyMatch mkmatch(KeyEvent ev) {
		return(KeyMatch.forevent(ev, ~cmd.modign));
	    }

	    protected boolean handle(KeyEvent ev) {
		if(ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
		    cmd.set(null);
		    super.set(cmd.key());
		    return(true);
		}
		return(super.handle(ev));
	    }

	    public Object tooltip(Coord c, Widget prev) {
		return(kbtt.tex());
	    }
	}
    }


    public static class PointBind extends Button {
	public static final String msg = "Bind other elements...";
	public static final Resource curs = Resource.local().loadwait("gfx/hud/curs/wrench");
	private UI.Grab mg, kg;
	private KeyBinding cmd;

	public PointBind(int w) {
	    super(w, msg, false);
	    tooltip = RichText.render("Bind a key to an element not listed above, such as an action-menu " +
				      "button. Click the element to bind, and then press the key to bind to it. " +
				      "Right-click to stop rebinding.",
				      300);
	}

	public void click() {
	    if(mg == null) {
		change("Click element...");
		mg = ui.grabmouse(this);
	    } else if(kg != null) {
		kg.remove();
		kg = null;
		change(msg);
	    }
	}

	private boolean handle(KeyEvent ev) {
	    switch(ev.getKeyCode()) {
	    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
	    case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
		return(false);
	    }
	    int code = ev.getKeyCode();
	    if(code == KeyEvent.VK_ESCAPE) {
		return(true);
	    }
	    if(code == KeyEvent.VK_BACK_SPACE) {
		cmd.set(null);
		return(true);
	    }
	    if(code == KeyEvent.VK_DELETE) {
		cmd.set(KeyMatch.nil);
		return(true);
	    }
	    KeyMatch key = KeyMatch.forevent(ev, ~cmd.modign);
	    if(key != null)
		cmd.set(key);
	    return(true);
	}

	public boolean mousedown(Coord c, int btn) {
	    if(mg == null)
		return(super.mousedown(c, btn));
	    Coord gc = ui.mc;
	    if(btn == 1) {
		this.cmd = KeyBinding.Bindable.getbinding(ui.root, gc);
		return(true);
	    }
	    if(btn == 3) {
		mg.remove();
		mg = null;
		change(msg);
		return(true);
	    }
	    return(false);
	}

	public boolean mouseup(Coord c, int btn) {
	    if(mg == null)
		return(super.mouseup(c, btn));
	    Coord gc = ui.mc;
	    if(btn == 1) {
		if((this.cmd != null) && (KeyBinding.Bindable.getbinding(ui.root, gc) == this.cmd)) {
		    mg.remove();
		    mg = null;
		    kg = ui.grabkeys(this);
		    change("Press key...");
		} else {
		    this.cmd = null;
		}
		return(true);
	    }
	    if(btn == 3)
		return(true);
	    return(false);
	}

	public Resource getcurs(Coord c) {
	    if(mg == null)
		return(null);
	    return(curs);
	}

	public boolean keydown(KeyEvent ev) {
	    if(kg == null)
		return(super.keydown(ev));
	    if(handle(ev)) {
		kg.remove();
		kg = null;
		cmd = null;
		change("Click another element...");
		mg = ui.grabmouse(this);
	    }
	    return(true);
	}
    }

    public OptWnd(boolean gopts) {
	super(Coord.z, "Options", true);
	main = add(new Panel());
	video = add(new VideoPanel(main));
	audio = add(new Panel());
	keybind = add(new BindingPanel(main));

	Composer main = new Composer(this.main).vmrgn(UI.scale(-6));
	main.add(new PButton(UI.scale(200), "Video settings", 'v', video));
	main.add(new PButton(UI.scale(200), "Audio settings", 'a', audio));
	main.add(new PButton(UI.scale(200), "Keybindings", 'k', keybind));
	if(gopts) {
	    main.add(new Button(UI.scale(200), "Switch character") {
		    public void click() {
			getparent(GameUI.class).act("lo", "cs");
		    }
		});
	    main.add(new Button(UI.scale(200), "Log out") {
		    public void click() {
			getparent(GameUI.class).act("lo");
		    }
		});
	}
	main.add(new Button(UI.scale(200), "Close") {
		public void click() {
		    OptWnd.this.hide();
		}
	    });
	this.main.pack();

	Composer audio = new Composer(this.audio).vmrgn(UI.scale(5));
	audio.add(new Label("Master audio volume"));
	audio.add(new HSlider(UI.scale(200), 0, 1000, (int)(Audio.volume * 1000)) {
		public void changed() {
		    Audio.setvolume(val / 1000.0);
		}
	    });
	audio.add(new Label("In-game event volume"));
	audio.add(new HSlider(UI.scale(200), 0, 1000, 0) {
		protected void attach(UI ui) {
		    super.attach(ui);
		    val = (int)(ui.audio.pos.volume * 1000);
		}
		public void changed() {
		    ui.audio.pos.setvolume(val / 1000.0);
		}
	    });
	audio.add(new Label("Ambient volume"));
	audio.add(new HSlider(UI.scale(200), 0, 1000, 0) {
		protected void attach(UI ui) {
		    super.attach(ui);
		    val = (int)(ui.audio.amb.volume * 1000);
		}
		public void changed() {
		    ui.audio.amb.setvolume(val / 1000.0);
		}
	    });
	audio.add(new PButton(UI.scale(200), "Back", 27, this.main));
	this.audio.pack();

	chpanel(this.main);
    }

    public OptWnd() {
	this(true);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && (msg == "close")) {
	    hide();
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    public void show() {
	chpanel(main);
	super.show();
    }
}
