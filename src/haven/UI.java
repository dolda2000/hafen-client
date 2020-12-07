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
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.DisplayMode;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import static haven.Utils.el;
import haven.render.Environment;
import haven.render.Render;

public class UI {
    public static int MOD_SHIFT = 1, MOD_CTRL = 2, MOD_META = 4, MOD_SUPER = 8;
    public RootWidget root;
    private final LinkedList<Grab> keygrab = new LinkedList<Grab>(), mousegrab = new LinkedList<Grab>();
    private final Map<Integer, Widget> widgets = new TreeMap<Integer, Widget>();
    private final Map<Widget, Integer> rwidgets = new HashMap<Widget, Integer>();
    Environment env;
    Receiver rcvr;
    public Coord mc = Coord.z, lcc = Coord.z;
    public Session sess;
    public boolean modshift, modctrl, modmeta, modsuper;
    public Object lasttip;
    double lastevent, lasttick;
    public Widget mouseon;
    public Console cons = new WidgetConsole();
    private Collection<AfterDraw> afterdraws = new LinkedList<AfterDraw>();
    private final Context uictx;
    public GSettings gprefs = GSettings.load(true);
    private boolean gprefsdirty = false;
    public final ActAudio.Root audio = new ActAudio.Root();
    private static final double scalef;
    
    {
	lastevent = lasttick = Utils.rtime();
    }
	
    public interface Receiver {
	public void rcvmsg(int widget, String msg, Object... args);
    }

    public interface Runner {
	public Session run(UI ui) throws InterruptedException;
    }

    public interface Context {
	void setmousepos(Coord c);
    }

    public interface AfterDraw {
	public void draw(GOut g);
    }

    public void setgprefs(GSettings prefs) {
	synchronized(this) {
	    if(!Utils.eq(prefs, this.gprefs)) {
		this.gprefs = prefs;
		gprefsdirty = true;
	    }
	}
    }

    private class WidgetConsole extends Console {
	{
	    setcmd("q", new Command() {
		    public void run(Console cons, String[] args) {
			HackThread.tg().interrupt();
		    }
		});
	    setcmd("lo", new Command() {
		    public void run(Console cons, String[] args) {
			sess.close();
		    }
		});
	    setcmd("gl", new Command() {
		    <T> void merd(GSettings.Setting<T> var, String val) {
			setgprefs(gprefs.update(null, var, var.parse(val)));
		    }

		    public void run(Console cons, String[] args) throws Exception {
			if(args.length < 3)
			    throw(new Exception("usage: gl SETTING VALUE"));
			GSettings.Setting<?> var = gprefs.find(args[1]);
			if(var == null)
			    throw(new Exception("No such setting: " + var));
			merd(var, args[2]);
		    }
		});
	}
	
	private void findcmds(Map<String, Command> map, Widget wdg) {
	    if(wdg instanceof Directory) {
		Map<String, Command> cmds = ((Directory)wdg).findcmds();
		synchronized(cmds) {
		    map.putAll(cmds);
		}
	    }
	    for(Widget ch = wdg.child; ch != null; ch = ch.next)
		findcmds(map, ch);
	}

	public Map<String, Command> findcmds() {
	    Map<String, Command> ret = super.findcmds();
	    findcmds(ret, root);
	    return(ret);
	}
    }

    @SuppressWarnings("serial")
    public static class UIException extends RuntimeException {
	public String mname;
	public Object[] args;
		
	public UIException(String message, String mname, Object... args) {
	    super(message);
	    this.mname = mname;
	    this.args = args;
	}
    }
	
    public UI(Context uictx, Coord sz, Session sess) {
	this.uictx = uictx;
	root = new RootWidget(this, sz);
	widgets.put(0, root);
	rwidgets.put(root, 0);
	this.sess = sess;
    }
	
    public void setreceiver(Receiver rcvr) {
	this.rcvr = rcvr;
    }
	
    public void bind(Widget w, int id) {
	synchronized(widgets) {
	    widgets.put(id, w);
	    rwidgets.put(w, id);
	}
    }

    public Widget getwidget(int id) {
	synchronized(widgets) {
	    return(widgets.get(id));
	}
    }

    public int widgetid(Widget wdg) {
	synchronized(widgets) {
	    Integer id = rwidgets.get(wdg);
	    if(id == null)
		return(-1);
	    return(id);
	}
    }

    public void drawafter(AfterDraw ad) {
	synchronized(afterdraws) {
	    afterdraws.add(ad);
	}
    }

    public void tick() {
	double now = Utils.rtime();
	root.tick(now - lasttick);
	lasttick = now;
	if(gprefsdirty) {
	    gprefs.save();
	    gprefsdirty = false;
	}
    }

    public void gtick(Render out) {
	root.gtick(out);
    }

    public void draw(GOut g) {
	root.draw(g);
	synchronized(afterdraws) {
	    for(AfterDraw ad : afterdraws)
		ad.draw(g);
	    afterdraws.clear();
	}
    }
	
    public void newwidget(int id, String type, int parent, Object[] pargs, Object... cargs) throws InterruptedException {
	Widget.Factory f = Widget.gettype2(type);
	if(f == null)
	    throw(new UIException("Bad widget name", type, cargs));
	synchronized(this) {
	    Widget wdg = f.create(this, cargs);
	    wdg.attach(this);
	    if(parent != 65535) {
		Widget pwdg = getwidget(parent);
		if(pwdg == null)
		    throw(new UIException("Null parent widget " + parent + " for " + id, type, cargs));
		pwdg.addchild(wdg, pargs);
	    }
	    bind(wdg, id);
	}
    }

    public void addwidget(int id, int parent, Object[] pargs) {
	synchronized(this) {
	    Widget wdg = getwidget(id);
	    if(wdg == null)
		throw(new UIException("Null child widget " + id + " added to " + parent, null, pargs));
	    Widget pwdg = getwidget(parent);
	    if(pwdg == null)
		throw(new UIException("Null parent widget " + parent + " for " + id, null, pargs));
	    pwdg.addchild(wdg, pargs);
	}
    }

    public abstract class Grab {
	public final Widget wdg;
	public Grab(Widget wdg) {this.wdg = wdg;}
	public abstract void remove();
    }

    public Grab grabmouse(Widget wdg) {
	if(wdg == null) throw(new NullPointerException());
	Grab g = new Grab(wdg) {
		public void remove() {
		    mousegrab.remove(this);
		}
	    };
	mousegrab.addFirst(g);
	return(g);
    }

    public Grab grabkeys(Widget wdg) {
	if(wdg == null) throw(new NullPointerException());
	Grab g = new Grab(wdg) {
		public void remove() {
		    keygrab.remove(this);
		}
	    };
	keygrab.addFirst(g);
	return(g);
    }

    private void removeid(Widget wdg) {
	synchronized(widgets) {
	    Integer id = rwidgets.get(wdg);
	    if(id != null) {
		widgets.remove(id);
		rwidgets.remove(wdg);
	    }
	}
	for(Widget child = wdg.child; child != null; child = child.next)
	    removeid(child);
    }
	
    public void removed(Widget wdg) {
	for(Iterator<Grab> i = mousegrab.iterator(); i.hasNext();) {
	    Grab g = i.next();
	    if(g.wdg.hasparent(wdg))
		i.remove();
	}
	for(Iterator<Grab> i = keygrab.iterator(); i.hasNext();) {
	    Grab g = i.next();
	    if(g.wdg.hasparent(wdg))
		i.remove();
	}
    }

    public void destroy(Widget wdg) {
	removeid(wdg);
	wdg.reqdestroy();
    }
    
    public void destroy(int id) {
	synchronized(this) {
	    Widget wdg = getwidget(id);
	    if(wdg != null)
		destroy(wdg);
	}
    }
	
    public void wdgmsg(Widget sender, String msg, Object... args) {
	int id = widgetid(sender);
	if(id < 0) {
	    new Warning("wdgmsg sender (%s) is not in rwidgets, message is %s", sender.getClass().getName(), msg).issue();
	    return;
	}
	if(rcvr != null)
	    rcvr.rcvmsg(id, msg, args);
    }
	
    public void uimsg(int id, String msg, Object... args) {
	Widget wdg = getwidget(id);
	if(wdg != null) {
	    synchronized(this) {
		wdg.uimsg(msg.intern(), args);
	    }
	} else {
	    throw(new UIException("Uimsg to non-existent widget " + id, msg, args));
	}
    }
	
    private void setmods(InputEvent ev) {
	int mod = ev.getModifiersEx();
	modshift = (mod & InputEvent.SHIFT_DOWN_MASK) != 0;
	modctrl = (mod & InputEvent.CTRL_DOWN_MASK) != 0;
	modmeta = (mod & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0;
	/*
	modsuper = (mod & InputEvent.SUPER_DOWN_MASK) != 0;
	*/
    }

    private Grab[] c(Collection<Grab> g) {return(g.toArray(new Grab[0]));}

    public void keydown(KeyEvent ev) {
	setmods(ev);
	for(Grab g : c(keygrab)) {
	    if(g.wdg.keydown(ev))
		return;
	}
	if(!root.keydown(ev)) {
	    char key = ev.getKeyChar();
	    if(key == ev.CHAR_UNDEFINED)
		key = 0;
	    root.globtype(key, ev);
	}
    }
	
    public void keyup(KeyEvent ev) {
	setmods(ev);
	for(Grab g : c(keygrab)) {
	    if(g.wdg.keyup(ev))
		return;
	}
	root.keyup(ev);
    }
	
    private Coord wdgxlate(Coord c, Widget wdg) {
	return(c.sub(wdg.rootpos()));
    }
	
    public boolean dropthing(Widget w, Coord c, Object thing) {
	if(w instanceof DropTarget) {
	    if(((DropTarget)w).dropthing(c, thing))
		return(true);
	}
	for(Widget wdg = w.lchild; wdg != null; wdg = wdg.prev) {
	    Coord cc = w.xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(dropthing(wdg, c.add(cc.inv()), thing))
		    return(true);
	    }
	}
	return(false);
    }

    public void mousedown(MouseEvent ev, Coord c, int button) {
	setmods(ev);
	lcc = mc = c;
	for(Grab g : c(mousegrab)) {
	    if(g.wdg.mousedown(wdgxlate(c, g.wdg), button))
		return;
	}
	root.mousedown(c, button);
    }
	
    public void mouseup(MouseEvent ev, Coord c, int button) {
	setmods(ev);
	mc = c;
	for(Grab g : c(mousegrab)) {
	    if(g.wdg.mouseup(wdgxlate(c, g.wdg), button))
		return;
	}
	root.mouseup(c, button);
    }
	
    public void mousemove(MouseEvent ev, Coord c) {
	setmods(ev);
	mc = c;
	root.mousemove(c);
    }

    public void setmousepos(Coord c) {
	uictx.setmousepos(c);
    }
	
    public void mousewheel(MouseEvent ev, Coord c, int amount) {
	setmods(ev);
	lcc = mc = c;
	for(Grab g : c(mousegrab)) {
	    if(g.wdg.mousewheel(wdgxlate(c, g.wdg), amount))
		return;
	}
	root.mousewheel(c, amount);
    }

    public Resource getcurs(Coord c) {
	for(Grab g : mousegrab) {
	    Resource ret = g.wdg.getcurs(wdgxlate(c, g.wdg));
	    if(ret != null)
		return(ret);
	}
	return(root.getcurs(c));
    }

    public static int modflags(InputEvent ev) {
	int mod = ev.getModifiersEx();
	return((((mod & InputEvent.SHIFT_DOWN_MASK) != 0) ? MOD_SHIFT : 0) |
	       (((mod & InputEvent.CTRL_DOWN_MASK) != 0)  ? MOD_CTRL : 0) |
	       (((mod & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0) ? MOD_META : 0)
	       /* (((mod & InputEvent.SUPER_DOWN_MASK) != 0) ? MOD_SUPER : 0) */);
    }

    public int modflags() {
	return((modshift ? MOD_SHIFT : 0) |
	       (modctrl  ? MOD_CTRL  : 0) |
	       (modmeta  ? MOD_META  : 0) |
	       (modsuper ? MOD_SUPER : 0));
    }

    public Environment getenv() {
	return(env);
    }

    public void destroy() {
	root.destroy();
	audio.clear();
    }

    public static double scale(double v) {
	return(v * scalef);
    }

    public static float scale(float v) {
	return(v * (float)scalef);
    }

    public static int scale(int v) {
	return(Math.round(scale((float)v)));
    }

    public static Coord scale(Coord v) {
	return(v.mul(scalef));
    }

    public static Coord scale(int x, int y) {
	return(scale(new Coord(x, y)));
    }

    public static Coord2d scale(Coord2d v) {
	return(v.mul(scalef));
    }

    static public Font scale(Font f, float size) {
	return(f.deriveFont(scale(size)));
    }

    public static <T extends Tex> ScaledTex<T> scale(T tex) {
	return(new ScaledTex<T>(tex, UI.scale(tex.sz())));
    }

    public static <T extends Tex> ScaledTex<T> scale(ScaledTex<T> tex) {
	return(tex);
    }

    public static double unscale(double v) {
	return(v / scalef);
    }

    public static float unscale(float v) {
	return(v / (float)scalef);
    }

    public static int unscale(int v) {
	return(Math.round(unscale((float)v)));
    }

    public static Coord unscale(Coord v) {
	return(v.div(scalef));
    }

    private static double maxscale = -1;
    public static double maxscale() {
	synchronized(UI.class) {
	    if(maxscale < 0) {
		double fscale = 1.25;
		try {
		    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		    for(GraphicsDevice dev : env.getScreenDevices()) {
			DisplayMode mode = dev.getDisplayMode();
			double scale = Math.min(mode.getWidth() / 800.0, mode.getHeight() / 600.0);
			fscale = Math.max(fscale, scale);
		    }
		} catch(Exception exc) {
		    new Warning(exc, "could not determine maximum scaling factor").issue();
		}
		maxscale = fscale;
	    }
	    return(maxscale);
	}
    }

    private static double loadscale() {
	if(Config.uiscale != null)
	    return(Config.uiscale);
	double scale = Utils.getprefd("uiscale", 1.0);
	scale = Math.max(Math.min(scale, maxscale()), 1.0);
	return(scale);
    }

    static {
	scalef = loadscale();
    }
}
