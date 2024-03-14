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
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import static haven.Utils.el;
import haven.render.Environment;
import haven.render.Render;

public class UI {
    public static int MOD_SHIFT = KeyMatch.S, MOD_CTRL = KeyMatch.C, MOD_META = KeyMatch.M, MOD_SUPER = KeyMatch.SUPER;
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
    public double lastevent, lasttick;
    public Widget mouseon;
    public Console cons = new WidgetConsole();
    private Collection<AfterDraw> afterdraws = new LinkedList<AfterDraw>();
    private final Context uictx;
    public GSettings gprefs = GSettings.load(true);
    private boolean gprefsdirty = false;
    public final ActAudio.Root audio = new ActAudio.Root();
    public final Loader loader;
    public final CommandQueue queue = new CommandQueue();
    private static final double scalef;
    
    {
	lastevent = lasttick = Utils.rtime();
    }
	
    public interface Receiver {
	public void rcvmsg(int widget, String msg, Object... args);
    }

    public interface Runner {
	public Runner run(UI ui) throws InterruptedException;
	public default void init(UI ui) {}
	public default String title() {return(null);}

	public static class Proxy implements Runner {
	    public final Runner back;

	    public Proxy(Runner back) {
		this.back = back;
	    }

	    public Runner run(UI ui) throws InterruptedException {return(back.run(ui));}
	    public void init(UI ui) {back.init(ui);}
	    public String title() {return(back.title());}
	}
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

    public static class UIException extends RuntimeException {
	public String mname;
	public Object[] args;

	public UIException(String message, String mname, Object... args) {
	    super(message);
	    this.mname = mname;
	    this.args = args;
	}

	public void printStackTrace(java.io.PrintStream out) {
	    super.printStackTrace(out);
	    out.printf("Message: %s; Arguments: %s\n", mname, Arrays.asList(args));
	}
    }

    public static class UIWarning extends Warning {
	public String mname;
	public Object[] args;

	public UIWarning(String message, String mname, Object... args) {
	    super(message);
	    this.mname = mname;
	    this.args = args;
	}
    }

    public UI(Context uictx, Coord sz, Runner fun) {
	this.uictx = uictx;
	root = new RootWidget(this, sz);
	widgets.put(0, root);
	rwidgets.put(root, 0);
	if(fun != null)
	    fun.init(this);
	if(sess == null) {
	    loader = new Loader();
	} else {
	    if((loader = sess.glob.loader) == null)
		throw(new NullPointerException());
	}
    }

    public static class Command implements Serializable {
	private static final java.util.concurrent.atomic.AtomicInteger nextid = new java.util.concurrent.atomic.AtomicInteger(0);
	public final int id = nextid.getAndIncrement();
	public final Collection<Integer> deps = new ArrayList<>();
	public final Collection<Integer> bars  = new ArrayList<>();
	public final Collection<Command> next = new ArrayList<>();
	public final Collection<Command> wait = new ArrayList<>();
	public final Runnable action;

	public Command(Runnable action) {
	    this.action = action;
	}

	public Command dep(int id, boolean bar) {
	    deps.add(id);
	    if(bar)
		bars.add(id);
	    return(this);
	}

	private String fl(String id, Collection<?> l) {
	    if(l.isEmpty())
		return("");
	    StringBuilder buf = new StringBuilder();
	    buf.append(" (");
	    buf.append(id);
	    for(Object x : l)
		buf.append(" " + x);
	    buf.append(")");
	    return(buf.toString());
	}

	public String toString() {
	    return(String.format("#<cmd %d %s%s%s>", this.id, action, fl("deps", deps), fl("bars", bars)));
	}
    }

    public static class CommandException extends RuntimeException {
	public final Command cmd;

	public CommandException(Command cmd, Throwable cause) {
	    super(cause);
	    this.cmd = cmd;
	}

	public String getMessage() {
	    return(String.format("error during ui command-handling: " + cmd));
	}
    }

    private static final boolean cmdjitter = false;
    private static final boolean cmddump = false;
    public class CommandQueue {
	private final Map<Integer, Command> score = new HashMap<>();

	private CommandQueue() {}

	private void run(Command cmd) {
	    if(cmdjitter) {
		try {
		    Thread.sleep((int)(Math.random() * 200));
		} catch(InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
	    }
	    try {
		cmd.action.run();
	    } catch(Loading l) {
		throw(l);
	    } catch(RuntimeException | Error e) {
		throw(new CommandException(cmd, e));
	    }
	    finish(cmd);
	}

	private void execute(Command cmd) {
	    if(cmddump)
		System.err.printf("exec: %s\n", cmd);
	    loader.defer(() -> run(cmd), null);
	}

	public void submit(Command cmd) {
	    boolean ready = true;
	    synchronized(this) {
		for(Integer dep : cmd.deps) {
		    Command last = score.get(dep);
		    if(last != null) {
			last.next.add(cmd);
			cmd.wait.add(last);
			ready = false;
		    }
		}
		for(Integer bar : cmd.bars) {
		    score.put(bar, cmd);
		}
		if(cmddump && !ready) {
		    ArrayList<Integer> wait = new ArrayList<>();
		    for(Command p : cmd.wait)
			wait.add(p.id);
		    System.err.printf("wait: %s on %s\n", cmd, wait);
		}
	    }
	    if(ready)
		execute(cmd);
	}

	public void finish(Command cmd) {
	    if(cmddump)
		System.err.printf("done: %s\n", cmd);
	    Collection<Command> ready = new ArrayList<>();
	    synchronized(this) {
		for(Command next : cmd.next) {
		    next.wait.remove(cmd);
		    if(next.wait.isEmpty())
			ready.add(next);
		}
		for(Integer bar : cmd.bars) {
		    if(score.get(bar) == cmd)
			score.remove(bar);
		}
	    }
	    for(Command next : ready)
		execute(next);
	}
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
	double delta = now - lasttick;
	lasttick = now;
	root.tick(delta);
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

    private Collection<Integer> or_deps = null, or_bars = null;

    private void submitcmd(Command cmd) {
	synchronized(queue) {
	    if(or_deps != null) {
		cmd.deps.clear();
		cmd.deps.addAll(or_deps);
		or_deps = null;
	    }
	    if(or_bars != null) {
		cmd.bars.clear();
		cmd.bars.addAll(or_bars);
		or_bars = null;
	    }
	}
	queue.submit(cmd);
    }

    public class NewWidget implements Runnable, Serializable {
	public final int id;
	public final String typenm;
	public final Object[] cargs;
	private transient Widget.Factory type;

	private NewWidget(int id, Widget.Factory type, Object... cargs) {
	    this.id = id;
	    this.type = type;
	    this.typenm = null;
	    this.cargs = cargs;
	}

	private NewWidget(int id, String type, Object... cargs) {
	    this.id = id;
	    this.typenm = type;
	    this.cargs = cargs;
	}

	private transient Widget wdg = null;
	public void run() {
	    if((type == null) && ((type = Widget.gettype3(typenm)) == null))
		throw(new UIException("Bad widget name", typenm, cargs));
	    if(wdg == null)
		wdg = type.create(UI.this, cargs);
	    synchronized(UI.this) {
		wdg.attach(UI.this);
		bind(wdg, id);
	    }
	}

	public String toString() {
	    return(String.format("#<newwdg %d %s %s>", id, (typenm == null) ? type : typenm, Arrays.asList(cargs)));
	}
    }

    public void newwidget(int id, Widget.Factory type, Object... cargs) {
	submitcmd(new Command(new NewWidget(id, type, cargs)).dep(id, true));
    }

    public void newwidget(int id, String type, Object... cargs) throws InterruptedException {
	submitcmd(new Command(new NewWidget(id, type, cargs)).dep(id, true));
    }

    private final MultiMap<Integer, Integer> shadowchildren = new HashMultiMap<>();
    private final Map<Integer, Integer> shadowparents = new HashMap<>();

    public class AddWidget implements Runnable, Serializable {
	public final int id, parent;
	public final Object[] pargs;

	private AddWidget(int id, int parent, Object... pargs) {
	    this.id = id;
	    this.parent = parent;
	    this.pargs = pargs;
	}

	public void run() {
	    synchronized(UI.this) {
		Widget wdg = getwidget(id);
		Widget pwdg = getwidget(parent);
		if(wdg == null)
		    throw(new UIException(String.format("Null child widget %d added to %d (%s)", id, parent, pwdg), null, pargs));
		if(pwdg == null)
		    throw(new UIException(String.format("Null parent widget %d for %d (%s)", parent, id, wdg), null, pargs));
		pwdg.addchild(wdg, pargs);
	    }
	}

	public String toString() {
	    return(String.format("#<addwdg %d @ %d %s>", id, parent, Arrays.asList(pargs)));
	}
    }

    public void addwidget(int id, int parent, Object... pargs) {
	synchronized(shadowchildren) {
	    Integer prev = shadowparents.put(id, parent);
	    if(prev != null)
		throw(new RuntimeException(String.format("widget %d already has parent %d when adding it to %d", id, prev, parent)));
	    shadowchildren.put(parent, id);
	    submitcmd(new Command(new AddWidget(id, parent, pargs)).dep(id, true).dep(parent, true));
	}
    }

    public void wdgbarrier(Collection<Integer> deps, Collection<Integer> bars) {
	synchronized(queue) {
	    or_deps = deps;
	    or_bars = (bars == null) ? deps : bars;
	}
    }

    public void newwidgetp(int id, Widget.Factory type, int parent, Object[] pargs, Object... cargs) {
	newwidget(id, type, cargs);
	if(parent != -1)
	    addwidget(id, parent, pargs);
    }

    public void newwidgetp(int id, String type, int parent, Object[] pargs, Object... cargs) throws InterruptedException {
	newwidget(id, type, cargs);
	if(parent != -1)
	    addwidget(id, parent, pargs);
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

    public class DstWidget implements Runnable, Serializable {
	public final int id;

	private DstWidget(int id) {
	    this.id = id;
	}

	public void run() {
	    synchronized(UI.this) {
		Widget wdg = getwidget(id);
		if(wdg != null)
		    destroy(wdg);
	    }
	}

	public String toString() {
	    return(String.format("#<dstwdg %d>", id));
	}
    }

    public void destroy(int id) {
	synchronized(shadowchildren) {
	    Integer parent = shadowparents.remove(id);
	    if(parent != null) {
		if(shadowchildren.remove(parent, id) == null)
		    throw(new AssertionError(String.format("mismatched shadow-tree indices when removing %d from %d", id, parent)));
	    }
	    for(Integer child : new ArrayList<>(shadowchildren.getall(id))) {
		destroy(child);
	    }
	    Command cmd = new Command(new DstWidget(id)).dep(id, true);
	    if(parent != null)
		cmd.dep(parent, true);
	    submitcmd(cmd);
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
	
    public class UiMessage implements Runnable, Serializable {
	public final int id;
	public final String msg;
	public final Object[] args;

	private UiMessage(int id, String msg, Object[] args) {
	    this.id = id;
	    this.msg = msg;
	    this.args = args;
	}

	public void run() {
	    Widget wdg = getwidget(id);
	    if(wdg != null) {
		synchronized(UI.this) {
		    wdg.uimsg(msg.intern(), args);
		}
	    } else {
		throw(new UIException("Uimsg to non-existent widget " + id, msg, args));
	    }
	}

	public String toString() {
	    return(String.format("#<wdgmsg %d %s %s>", id, msg, Arrays.asList(args)));
	}
    }

    public void uimsg(int id, String msg, Object... args) {
	submitcmd(new Command(new UiMessage(id, msg, args)).dep(id, true));
    }

    public static interface MessageWidget {
	public static final Audio.Clip errsfx = Audio.resclip(Resource.local().loadwait("sfx/error"));
	public static final Audio.Clip msgsfx = Audio.resclip(Resource.local().loadwait("sfx/msg"));

	public void msg(String msg, Color color, Audio.Clip sfx);

	public static MessageWidget find(Widget w) {
	    for(Widget ch = w.child; ch != null; ch = ch.next) {
		MessageWidget ret = find(ch);
		if(ret != null)
		    return(ret);
	    }
	    if(w instanceof MessageWidget)
		return((MessageWidget)w);
	    return(null);
	}
    }

    public void msg(String msg, Color color, Audio.Clip sfx) {
	if(color == null)
	    color = Color.WHITE;
	MessageWidget h = MessageWidget.find(root);
	if(h != null)
	    h.msg(msg, color, sfx);
    }

    public void error(String msg) {
	msg(msg, new Color(192, 0, 0), MessageWidget.errsfx);
    }

    public void msg(String msg) {
	msg(msg, Color.WHITE, MessageWidget.msgsfx);
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

    public void mousehover(Coord c) {
	root.mousehover(c, true);
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

    public void sfx(Audio.CS clip) {
	audio.aui.add(clip);
    }
    public void sfx(Audio.Clip clip) {
	sfx(clip.stream());
    }
    public void sfx(Resource clip) {
	sfx(Audio.fromres(clip));
    }

    public final Map<Audio.Clip, Double> lastmsgsfx = new HashMap<>();
    public void sfxrl(Audio.Clip clip) {
	if(clip != null) {
	    double now = Utils.rtime();
	    Double last = lastmsgsfx.get(clip);
	    if((last == null) || (now - last > 0.01)) {
		sfx(clip);
		lastmsgsfx.put(clip, now);
	    }
	}
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

    public static int rscale(double v) {
	return((int)Math.round(v * scalef));
    }

    public static Coord scale(Coord v) {
	return(v.mul(scalef));
    }

    public static Coord scale(int x, int y) {
	return(scale(new Coord(x, y)));
    }

    public static Coord rscale(double x, double y) {
	return(new Coord(rscale(x), rscale(y)));
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

    public static final Config.Variable<Double> uiscale = Config.Variable.propf("haven.uiscale", null);
    private static double loadscale() {
	if(uiscale.get() != null)
	    return(uiscale.get());
	double scale = Utils.getprefd("uiscale", 1.0);
	scale = Math.max(Math.min(scale, maxscale()), 1.0);
	return(scale);
    }

    static {
	scalef = loadscale();
    }
}
