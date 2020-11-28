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
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class Widget {
    public UI ui;
    public Coord c, sz;
    public Widget next, prev, child, lchild, parent;
    public boolean focustab = false, focusctl = false, hasfocus = false, visible = true;
    private boolean attached = false;
    private boolean canfocus = false, autofocus = false;
    public boolean canactivate = false, cancancel = false;
    public Widget focused;
    public Indir<Resource> cursor = null;
    public Object tooltip = null;
    public KeyMatch gkey;
    public KeyBinding kb_gkey;
    private Widget prevtt;
    static Map<String, Factory> types = new TreeMap<String, Factory>();

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RName {
	public String value();
    }

    @RName("cnt")
    public static class $Cont implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Widget(UI.scale((Coord)args[0])));
	}
    }
    @RName("ccnt")
    public static class $CCont implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Widget ret = new Widget(UI.scale((Coord)args[0])) {
		    public void presize() {
			c = parent.sz.div(2).sub(sz.div(2));
		    }

		    protected void added() {
			presize();
		    }
		};
	    return(ret);
	}
    }
    @RName("fcnt")
    public static class $FCont implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Widget ret = new Widget(Coord.z) {
		    Collection<Widget> fill = new ArrayList<Widget>();
		    public void presize() {
			resize(parent.sz);
			for(Widget ch : fill)
			    ch.resize(sz);
		    }
		    public void added() {presize();}
		    public void addchild(Widget child, Object... args) {
			if((args[0] instanceof String) && args[0].equals("fill")) {
			    child.resize(sz);
			    fill.add(child);
			    add(child, Coord.z);
			} else {
			    super.addchild(child, args);
			}
		    }
		};
	    return(ret);
	}
    }

    public static abstract class AlignPanel extends Widget {
	protected abstract Coord getc();

	public <T extends Widget> T add(T child) {
	    super.add(child);
	    pack();
	    if(parent != null)
		presize();
	    return(child);
	}

	public void cresize(Widget ch) {
	    pack();
	    if(parent != null)
		presize();
	}

	public void presize() {
	    c = getc();
	}

	protected void added() {presize();}
    }
    @RName("acnt")
    public static class $ACont implements Factory {
	public Widget create(UI ui, final Object[] args) {
	    final String expr = (String)args[0];
	    return(new AlignPanel() {
		    protected Coord getc() {
			return(relpos(expr, this, args, 1));
		    }
		});
	}
    }

    @Resource.PublishedCode(name = "wdg", instancer = FactMaker.class)
    public interface Factory {
	public Widget create(UI ui, Object[] par);
    }

    public static class FactMaker implements Resource.PublishedCode.Instancer {
	public Factory make(Class<?> cl, Resource ires, Object... argv) {
	    if(Factory.class.isAssignableFrom(cl))
		return(Resource.PublishedCode.Instancer.stdmake(cl.asSubclass(Factory.class), ires, argv));
	    try {
		final Method mkm = cl.getDeclaredMethod("mkwidget", UI.class, Object[].class);
		int mod = mkm.getModifiers();
		if(Widget.class.isAssignableFrom(mkm.getReturnType()) && ((mod & Modifier.STATIC) != 0) && ((mod & Modifier.PUBLIC) != 0)) {
		    return(new Factory() {
			    public Widget create(UI ui, Object[] args) {
				try {
				    return((Widget)mkm.invoke(null, ui, args));
				} catch(Exception e) {
				    if(e instanceof RuntimeException) throw((RuntimeException)e);
				    throw(new RuntimeException(e));
				}
			    }
			});
		}
	    } catch(NoSuchMethodException e) {
	    }
	    return(null);
	}
    }

    private static boolean inited = false;
    public static void initnames() {
	if(!inited) {
	    for(Factory f : dolda.jglob.Loader.get(RName.class).instances(Factory.class)) {
		synchronized(types) {
		    types.put(f.getClass().getAnnotation(RName.class).value(), f);
		}
	    }
	    inited = true;
	}
    }

    public static Factory gettype2(String name) throws InterruptedException {
	if(name.indexOf('/') < 0) {
	    synchronized(types) {
		return(types.get(name));
	    }
	} else {
	    int ver = -1, p;
	    if((p = name.indexOf(':')) > 0) {
		ver = Integer.parseInt(name.substring(p + 1));
		name = name.substring(0, p);
	    }
	    Indir<Resource> res = Resource.remote().load(name, ver, 10);
	    while(true) {
		try {
		    return(res.get().getcode(Factory.class, true));
		} catch(Loading l) {
		    l.waitfor();
		}
	    }
	}
    }

    public static Factory gettype(String name) {
	long start = System.currentTimeMillis();
	Factory f;
	try {
	    f = gettype2(name);
	} catch(InterruptedException e) {
	    /* XXX: This is not proper behavior. On the other hand,
	     * InterruptedException should not be checked. :-/ */
	    throw(new RuntimeException("Interrupted while loading resource widget (took " + (System.currentTimeMillis() - start) + " ms)", e));
	}
	if(f == null)
	    throw(new RuntimeException("No such widget type: " + name));
	return(f);
    }

    public Widget(Coord sz) {
	this.c = Coord.z;
	this.sz = sz;
    }

    public Widget() {
	this(Coord.z);
    }

    public Widget(UI ui, Coord c, Coord sz) {
	this.ui = ui;
	this.c = c;
	this.sz = sz;
	this.attached = true;
    }

    protected void attach(UI ui) {
	this.ui = ui;
	for(Widget ch = child; ch != null; ch = ch.next)
	    ch.attach(ui);
    }

    protected void attached() {
	attached = true;
	for(Widget ch = child; ch != null; ch = ch.next)
	    ch.attached();
    }

    private <T extends Widget> T add0(T child) {
	if((child.ui == null) && (this.ui != null))
	    ((Widget)child).attach(this.ui);
	child.parent = this;
	child.link();
	child.added();
	if(attached)
	    child.attached();
	if(((Widget)child).canfocus && child.visible)
	    newfocusable(child);
	return(child);
    }

    public <T extends Widget> T add(T child) {
	if(ui != null) {
	    synchronized(ui) {
		return(add0(child));
	    }
	} else {
	    return(add0(child));
	}
    }

    public <T extends Widget> T add(T child, Coord c) {
	child.c = c;
	return(add(child));
    }

    public <T extends Widget> T add(T child, int x, int y) {
	return(add(child, new Coord(x, y)));
    }

    public <T extends Widget> T adda(T child, int x, int y, double ax, double ay) {
	return(add(child, x - (int)(child.sz.x * ax), y - (int)(child.sz.y * ay)));
    }

    public <T extends Widget> T adda(T child, Coord c, double ax, double ay) {
	return(adda(child, c.x, c.y, ax, ay));
    }

    public <T extends Widget> T adda(T child, double ax, double ay) {
	return(adda(child, (int)(sz.x * ax), (int)(sz.y * ay), ax, ay));
    }

    protected void added() {}

    public static class RelposError extends RuntimeException {
	public final String spec;
	public final int pos;
	public final Stack<Object> stack;

	public RelposError(Throwable cause, String spec, int pos, Stack<Object> stack) {
	    super(cause);
	    this.spec = spec;
	    this.pos = pos;
	    this.stack = stack;
	}

	public String getMessage() {
	    return(String.format("Unhandled exception at %s+%d, stack is %s", spec, pos, stack));
	}
    }

    public Coord relpos(String spec, Object self, Object[] args, int off) {
	int i = 0;
	Stack<Object> st = new Stack<Object>();
	try {
	    while(i < spec.length()) {
		char op = spec.charAt(i++);
		if(Character.isDigit(op)) {
		    int e;
		    for(e = i; (e < spec.length()) && Character.isDigit(spec.charAt(e)); e++);
		    int v = Integer.parseInt(spec.substring(i - 1, e));
		    st.push(v);
		    i = e;
		} else if(op == '!') {
		    st.push(args[off++]);
		} else if(op == '$') {
		    st.push(self);
		} else if(op == '@') {
		    st.push(this);
		} else if(op == '_') {
		    st.push(st.peek());
		} else if(op == '.') {
		    st.pop();
		} else if(op == '^') {
		    Object a = st.pop();
		    Object b = st.pop();
		    st.push(a);
		    st.push(b);
		} else if(op == 'c') {
		    int y = (Integer)st.pop();
		    int x = (Integer)st.pop();
		    st.push(new Coord(x, y));
		} else if(op == 'o') {
		    Widget w = (Widget)st.pop();
		    st.push(w.c.add(w.sz));
		} else if(op == 'p') {
		    st.push(((Widget)st.pop()).c);
		} else if(op == 'P') {
		    Widget parent = (Widget)st.pop();
		    st.push(((Widget)st.pop()).parentpos(parent));
		} else if(op == 's') {
		    st.push(((Widget)st.pop()).sz);
		} else if(op == 'w') {
		    int id = (Integer)st.pop();
		    Widget w = ui.getwidget(id);
		    if(w == null)
			throw(new RuntimeException("Invalid widget ID: " + id));
		    st.push(w);
		} else if(op == 'x') {
		    st.push(((Coord)st.pop()).x);
		} else if(op == 'y') {
		    st.push(((Coord)st.pop()).y);
		} else if(op == '+') {
		    Object b = st.pop();
		    Object a = st.pop();
		    if((a instanceof Integer) && (b instanceof Integer)) {
			st.push((Integer)a + (Integer)b);
		    } else if((a instanceof Coord) && (b instanceof Coord)) {
			st.push(((Coord)a).add((Coord)b));
		    } else {
			throw(new RuntimeException("Invalid addition operands: " + a + " + " + b));
		    }
		} else if(op == '-') {
		    Object b = st.pop();
		    Object a = st.pop();
		    if((a instanceof Integer) && (b instanceof Integer)) {
			st.push((Integer)a - (Integer)b);
		    } else if((a instanceof Coord) && (b instanceof Coord)) {
			st.push(((Coord)a).sub((Coord)b));
		    } else {
			throw(new RuntimeException("Invalid subtraction operands: " + a + " - " + b));
		    }
		} else if(op == '*') {
		    Object b = st.pop();
		    Object a = st.pop();
		    if((a instanceof Integer) && (b instanceof Integer)) {
			st.push((Integer)a * (Integer)b);
		    } else if((a instanceof Coord) && (b instanceof Integer)) {
			st.push(((Coord)a).mul((Integer)b));
		    } else if((a instanceof Coord) && (b instanceof Coord)) {
			st.push(((Coord)a).mul((Coord)b));
		    } else {
			throw(new RuntimeException("Invalid multiplication operands: " + a + " - " + b));
		    }
		} else if(op == '/') {
		    Object b = st.pop();
		    Object a = st.pop();
		    if((a instanceof Integer) && (b instanceof Integer)) {
			st.push((Integer)a / (Integer)b);
		    } else if((a instanceof Coord) && (b instanceof Integer)) {
			st.push(((Coord)a).div((Integer)b));
		    } else if((a instanceof Coord) && (b instanceof Coord)) {
			st.push(((Coord)a).div((Coord)b));
		    } else {
			throw(new RuntimeException("Invalid division operands: " + a + " - " + b));
		    }
		} else if(op == 'S') {
		    Object a = st.pop();
		    if(a instanceof Integer) {
			st.push(UI.scale((Integer)a));
		    } else if(a instanceof Coord) {
			st.push(UI.scale((Coord)a));
		    } else {
			throw(new RuntimeException("Invalid scaling operand: " + a));
		    }
		} else if(Character.isWhitespace(op)) {
		} else {
		    throw(new RuntimeException("Unknown position operation: " + op));
		}
	    }
	} catch(RuntimeException e) {
	    throw(new RelposError(e, spec, i, st));
	}
	return((Coord)st.pop());
    }

    public void addchild(Widget child, Object... args) {
	if(args[0] instanceof Coord) {
	    Coord c = (Coord)args[0];
	    String opt = (args.length > 1) ? (String)args[1] : "";
	    if(opt.indexOf('u') < 0)
		c = UI.scale(c);
	    add(child, c);
	} else if(args[0] instanceof Coord2d) {
	    add(child, ((Coord2d)args[0]).mul(new Coord2d(this.sz.sub(child.sz))).round());
	} else if(args[0] instanceof String) {
	    add(child, relpos((String)args[0], child, args, 1));
	} else {
	    throw(new UI.UIException("Unknown child widget creation specification.", null, args));
	}
    }

    public void link() {
	if(parent.lchild != null)
	    parent.lchild.next = this;
	if(parent.child == null)
	    parent.child = this;
	this.prev = parent.lchild;
	parent.lchild = this;
    }
    
    public void linkfirst() {
	if(parent.child != null)
	    parent.child.prev = this;
	if(parent.lchild == null)
	    parent.lchild = this;
	this.next = parent.child;
	parent.child = this;
    }
	
    public void unlink() {
	if(next != null)
	    next.prev = prev;
	if(prev != null)
	    prev.next = next;
	if(parent.child == this)
	    parent.child = next;
	if(parent.lchild == this)
	    parent.lchild = prev;
	next = null;
	prev = null;
    }
	
    public Coord xlate(Coord c, boolean in) {
	return(c);
    }
	
    public Coord parentpos(Widget in) {
	if(in == this)
	    return(new Coord(0, 0));
	return(parent.xlate(parent.parentpos(in).add(c), true));
    }

    public Coord parentpos(Widget in, Coord c) {
	return(parentpos(in).add(c));
    }

    public Coord rootpos() {
	return(parentpos(ui.root));
    }
    
    public Coord rootpos(Coord c) {
	return(rootpos().add(c));
    }

    public Coord rootxlate(Coord c) {
	return(c.sub(rootpos()));
    }
	
    public boolean hasparent(Widget w2) {
	for(Widget w = this; w != null; w = w.parent) {
	    if(w == w2)
		return(true);
	}
	return(false);
    }

    public void gotfocus() {
	if(focusctl && (focused != null)) {
	    focused.hasfocus = true;
	    focused.gotfocus();
	}
    }

    public void dispose() {
    }

    public void rdispose() {
	for(Widget ch = child; ch != null; ch = ch.next)
	    ch.rdispose();
	dispose();
    }

    public void remove() {
	if(canfocus)
	    setcanfocus(false);
	if(parent != null) {
	    unlink();
	    parent.cdestroy(this);
	}
	if(ui != null)
	    ui.removed(this);
    }

    public void reqdestroy() {
	destroy();
    }

    public void destroy() {
	remove();
	rdispose();
    }

    /* XXX: Should be renamed to cremove at this point. */
    public void cdestroy(Widget w) {
    }

    public int wdgid() {
	return(ui.widgetid(this));
    }

    public void lostfocus() {
	if(focusctl && (focused != null)) {
	    focused.hasfocus = false;
	    focused.lostfocus();
	}
    }

    public void setfocus(Widget w) {
	if(focusctl) {
	    if(w != focused) {
		Widget last = focused;
		focused = w;
		if(hasfocus) {
		    if(last != null)
			last.hasfocus = false;
		    w.hasfocus = true;
		    if(last != null)
			last.lostfocus();
		    w.gotfocus();
		} else if((last != null) && last.hasfocus) {
		    /* Bug, but ah well. */
		    last.hasfocus = false;
		    last.lostfocus();
		}
		if((ui != null) && (w != null) && (wdgid() >= 0)) {
		    int id = w.wdgid();
		    if(id >= 0)
			wdgmsg("focus", id);
		}
	    }
	    if((parent != null) && visible && canfocus)
		parent.setfocus(this);
	} else {
	    parent.setfocus(w);
	}
    }

    public void setcanfocus(boolean canfocus) {
	this.autofocus = this.canfocus = canfocus;
	if(parent != null) {
	    if(canfocus) {
		parent.newfocusable(this);
	    } else {
		parent.delfocusable(this);
	    }
	}
    }

    public void newfocusable(Widget w) {
	if(focusctl) {
	    if(focused == null)
		setfocus(w);
	} else {
	    if(parent != null)
		parent.newfocusable(w);
	}
    }

    public void delfocusable(Widget w) {
	if(focusctl) {
	    if((focused != null) && focused.hasparent(w)) {
		findfocus();
	    }
	} else {
	    if(parent != null)
		parent.delfocusable(w);
	}
    }

    private void findfocus() {
	/* XXX: Might need to check subwidgets recursively */
	focused = null;
	for(Widget w = lchild; w != null; w = w.prev) {
	    if(w.visible && w.autofocus) {
		focused = w;
		if(hasfocus) {
		    focused.hasfocus = true;
		    w.gotfocus();
		}
		break;
	    }
	}
    }
	
    public void setfocusctl(boolean focusctl) {
	if(this.focusctl = focusctl) {
	    findfocus();
	    setcanfocus(true);
	}
    }
	
    public void setfocustab(boolean focustab) {
	if(focustab && !focusctl)
	    setfocusctl(true);
	this.focustab = focustab;
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "tabfocus") {
	    setfocustab(((Integer)args[0] != 0));
	} else if(msg == "act") {
	    canactivate = (Integer)args[0] != 0;
	} else if(msg == "cancel") {
	    cancancel = (Integer)args[0] != 0;
	} else if(msg == "autofocus") {
	    autofocus = (Integer)args[0] != 0;
	} else if(msg == "focus") {
	    int tid = (Integer)args[0];
	    if(tid < 0) {
		setfocus(null);
	    } else {
		Widget w = ui.getwidget(tid);
		if(w != null) {
		    if(w.canfocus)
			setfocus(w);
		}
	    }
	} else if(msg == "pack") {
	    pack();
	} else if(msg == "curs") {
	    if(args.length == 0)
		cursor = null;
	    else
		cursor = Resource.remote().load((String)args[0], (Integer)args[1]);
	} else if(msg == "tip") {
	    int a = 0;
	    Object tt = args[a++];
	    if(tt instanceof String) {
		settip((String)tt);
	    } else if(tt instanceof Integer) {
		tooltip = new PaginaTip(ui.sess.getres((Integer)tt));
	    }
	} else if(msg == "gk") {
	    if(args[0] instanceof Integer) {
		KeyMatch key = gkeymatch((Integer)args[0]);
		if(args.length > 1) {
		    int modign = 0;
		    if(args.length > 2)
			modign = (Integer)args[2];
		    setgkey(KeyBinding.get("wgk/" + (String)args[1], key, modign));
		} else {
		    gkey = key;
		}
	    }
	} else {
	    new Warning("unhandled widget message: " + msg).issue();
	}
    }
	
    public void wdgmsg(String msg, Object... args) {
	wdgmsg(this, msg, args);
    }
	
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(parent == null)
	    ui.wdgmsg(sender, msg, args);
	else
	    parent.wdgmsg(sender, msg, args);
    }
	
    public void tick(double dt) {
	Widget next;
	
	for(Widget wdg = child; wdg != null; wdg = next) {
	    next = wdg.next;
	    wdg.tick(dt);
	}
	/* It would be very nice to do these things in harmless mix-in
	 * classes, but alas, this is Java. */
	anims.addAll(nanims);
	nanims.clear();
	for(Iterator<Anim> i = anims.iterator(); i.hasNext();) {
	    Anim anim = i.next();
	    if(anim.tick(dt))
		i.remove();
	}
    }

    public void gtick(haven.render.Render out) {
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    wdg.gtick(out);
	}
    }

    public void draw(GOut g, boolean strict) {
	Widget next;
		
	for(Widget wdg = child; wdg != null; wdg = next) {
	    next = wdg.next;
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    GOut g2;
	    if(strict)
		g2 = g.reclip(cc, wdg.sz);
	    else
		g2 = g.reclipl(cc, wdg.sz);
	    wdg.draw(g2);
	}
    }
    
    public void draw(GOut g) {
	draw(g, true);
    }
	
    public boolean checkhit(Coord c) {
	return(true);
    }

    public boolean mousedown(Coord c, int button) {
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(wdg.mousedown(c.add(cc.inv()), button)) {
		    return(true);
		}
	    }
	}
	return(false);
    }
	
    public boolean mouseup(Coord c, int button) {
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(wdg.mouseup(c.add(cc.inv()), button)) {
		    return(true);
		}
	    }
	}
	return(false);
    }
	
    public boolean mousewheel(Coord c, int amount) {
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(wdg.mousewheel(c.add(cc.inv()), amount)) {
		    return(true);
		}
	    }
	}
	return(false);
    }
	
    public void mousemove(Coord c) {
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    wdg.mousemove(c.add(cc.inv()));
	}
    }

    private static final Map<Integer, Integer> gkeys = Utils.<Integer, Integer>map().
	put((int)'0', KeyEvent.VK_0).put((int)'1', KeyEvent.VK_1).put((int)'2', KeyEvent.VK_2).put((int)'3', KeyEvent.VK_3).put((int)'4', KeyEvent.VK_4).
	put((int)'5', KeyEvent.VK_5).put((int)'6', KeyEvent.VK_6).put((int)'7', KeyEvent.VK_7).put((int)'8', KeyEvent.VK_8).put((int)'9', KeyEvent.VK_9).
	put((int)'`', KeyEvent.VK_BACK_QUOTE).put((int)'-', KeyEvent.VK_MINUS).put((int)'=', KeyEvent.VK_EQUALS).
	put(8, KeyEvent.VK_BACK_SPACE).put(9, KeyEvent.VK_TAB).put(13, KeyEvent.VK_ENTER).put(27, KeyEvent.VK_ESCAPE).
	put(128, KeyEvent.VK_UP).put(129, KeyEvent.VK_RIGHT).put(130, KeyEvent.VK_DOWN).put(131, KeyEvent.VK_LEFT).
	put(132, KeyEvent.VK_INSERT).put(133, KeyEvent.VK_HOME).put(134, KeyEvent.VK_PAGE_UP).put(135, KeyEvent.VK_DELETE).put(136, KeyEvent.VK_END).put(137, KeyEvent.VK_PAGE_DOWN).map();

    public static KeyMatch gkeymatch(int gkey) {
	if(gkey == 0)
	    return(KeyMatch.nil);
	int key = gkey & 0xff, modmask = (gkey & 0xf000) >> 12, modmatch = (gkey & 0x0f00) >> 8;
	if(modmask == 0)
	    modmask = KeyMatch.MODS;
	Integer code = gkeys.get(key);
	if(code != null)
	    return(KeyMatch.forcode(code, modmask, modmatch));
	if(gkey < 32)
	    return(KeyMatch.forchar((char)((int)'A' + gkey - 1), KeyMatch.C));
	return(KeyMatch.forchar((char)key, modmask, modmatch));
    }

    public boolean gkeytype(KeyEvent ev) {
	wdgmsg("activate", UI.modflags(ev));
	return(true);
    }

    public boolean globtype(char key, KeyEvent ev) {
	KeyMatch gkey = this.gkey;
	if(kb_gkey != null)
	    gkey = kb_gkey.key();
	if((gkey != null) && gkey.match(ev))
	    return(gkeytype(ev));
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(wdg.globtype(key, ev))
		return(true);
	}
	return(false);
    }

    public Widget setgkey(KeyBinding gkey) {
	kb_gkey = gkey;
	if((tooltip == null) && (kb_gkey != null))
	    tooltip = new KeyboundTip();
	return(this);
    }
	
    public static final KeyMatch key_act = KeyMatch.forcode(KeyEvent.VK_ENTER, 0);
    public static final KeyMatch key_esc = KeyMatch.forcode(KeyEvent.VK_ESCAPE, 0);
    public static final KeyMatch key_tab = KeyMatch.forcode(KeyEvent.VK_TAB, 0);
    public boolean keydown(KeyEvent ev) {
	if(canactivate) {
	    if(key_act.match(ev)) {
		wdgmsg("activate");
		return(true);
	    }
	}
	if(cancancel) {
	    if(key_esc.match(ev)) {
		wdgmsg("cancel");
		return(true);
	    }
	}
	if(focusctl) {
	    if(focused != null) {
		if(focused.keydown(ev))
		    return(true);
		if(focustab) {
		    if(key_tab.match(ev)) {
			Widget f = focused;
			while(true) {
			    if((ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
				Widget n = f.rnext();
				f = ((n == null) || !n.hasparent(this))?child:n;
			    } else {
				Widget p = f.rprev();
				f = ((p == null) || (p == this) || !p.hasparent(this))?lchild:p;
			    }
			    if(f.canfocus)
				break;
			}
			setfocus(f);
			return(true);
		    } else {
			return(false);
		    }
		} else {
		    return(false);
		}
	    } else {
		return(false);
	    }
	} else {
	    for(Widget wdg = child; wdg != null; wdg = wdg.next) {
		if(wdg.visible) {
		    if(wdg.keydown(ev))
			return(true);
		}
	    }
	}
	return(false);
    }
	
    public boolean keyup(KeyEvent ev) {
	if(focusctl) {
	    if(focused != null) {
		if(focused.keyup(ev))
		    return(true);
		return(false);
	    } else {
		return(false);
	    }
	} else {
	    for(Widget wdg = child; wdg != null; wdg = wdg.next) {
		if(wdg.visible) {
		    if(wdg.keyup(ev))	
			return(true);
		}
	    }
	}
	return(false);
    }
    
    public Area area() {
	return(Area.sized(c, sz));
    }

    public Area parentarea(Widget in) {
	return(Area.sized(parentpos(in), sz));
    }

    public Area rootarea() {
	return(parentarea(ui.root));
    }

    public Coord contentsz() {
	Coord max = new Coord(0, 0);
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if(!wdg.visible)
		continue;
	    Coord br = wdg.c.add(wdg.sz);
	    if(br.x > max.x)
		max.x = br.x;
	    if(br.y > max.y)
		max.y = br.y;
	}
	return(max);
    }

    public void pack() {
	resize(contentsz());
    }
    
    public void move(Coord c) {
	this.c = c;
    }

    public void resize(Coord sz) {
	this.sz = sz;
	for(Widget ch = child; ch != null; ch = ch.next)
	    ch.presize();
	if(parent != null)
	    parent.cresize(this);
    }

    public void move(Area a) {
	move(a.ul);
	resize(a.sz());
    }

    public void resize(int x, int y) {
	resize(new Coord(x, y));
    }

    public void cresize(Widget ch) {
    }
    
    public void presize() {
    }

    public static class Position extends Coord {
	public Position(int x, int y) {
	    super(x, y);
	}

	public Position(Coord c) {
	    this(c.x, c.y);
	}

	public Position add(int X, int Y) {return(new Position(x + X, y + Y));}
	public Position add(Coord c) {return(add(c.x, c.y));}
	public Position adds(int x, int y) {return(add(UI.scale(x), UI.scale(y)));}
	public Position adds(Coord c) {return(add(UI.scale(c)));}

	public Position sub(int X, int Y) {return(new Position(x - X, y - Y));}
	public Position sub(Coord c) {return(sub(c.x, c.y));}
	public Position subs(int x, int y) {return(sub(UI.scale(x), UI.scale(y)));}
	public Position subs(Coord c) {return(sub(UI.scale(c)));}

	public Position x(int X) {return(new Position(X, y));}
	public Position y(int Y) {return(new Position(x, Y));}
	public Position xs(int x) {return(x(UI.scale(x)));}
	public Position ys(int y) {return(y(UI.scale(y)));}
    }

    public Position getpos(String nm) {
	switch(nm) {
	case "ul": return(new Position(this.c));
	case "ur": return(new Position(this.c.add(this.sz.x, 0)));
	case "br": return(new Position(this.c.add(this.sz)));
	case "bl": return(new Position(this.c.add(0, this.sz.y)));
	case "cbr": return(new Position(this.sz));
	case "cur": return(new Position(this.sz.x, 0));
	case "cbl": return(new Position(0, this.sz.y));
	case "mid": return(new Position(this.c.add(this.sz.div(2))));
	case "cmid": return(new Position(this.sz.div(2)));
	default: return(null);
	}
    }

    public Position pos(String nm) {
	Position ret = getpos(nm);
	if(ret == null)
	    throw(new IllegalArgumentException(String.format("Illegal position anchor \"%s\" from widget %s", nm, this)));
	return(ret);
    }

    public void raise() {
	synchronized((ui != null)?ui:new Object()) {
	    unlink();
	    link();
	}
    }

    public void lower() {
	synchronized((ui != null)?ui:new Object()) {
	    unlink();
	    linkfirst();
	}
    }

    public <T> T getchild(Class<T> cl) {
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if(cl.isInstance(wdg))
		return(cl.cast(wdg));
	}
	return(null);
    }

    @Deprecated
    public <T extends Widget> T findchild(Class<T> cl) {
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if(cl.isInstance(wdg))
		return(cl.cast(wdg));
	    T ret = wdg.findchild(cl);
	    if(ret != null)
		return(ret);
	}
	return(null);
    }

    public Widget rprev() {
	if(prev != null) {
	    Widget lc = prev.lchild;
	    if(lc != null) {
		for(; lc.lchild != null; lc = lc.lchild);
		return(lc);
	    }
	    return(prev);
	}
	return(parent);
    }

    public Widget rnext() {
	if(child != null)
	    return(child);
	if(next != null)
	    return(next);
	for(Widget p = parent; p != null; p = p.parent) {
	    if(p.next != null)
		return(p.next);
	}
	return(null);
    }
    
    public class Children extends AbstractSequentialList<Widget> {
	protected Children() {
	}

	public int size() {
	    int n = 0;
	    for(Widget ch : this)
		n++;
	    return(n);
	}

	public ListIterator<Widget> listIterator(int idx) {
	    ListIterator<Widget> ret = new ListIterator<Widget>() {
		    Widget next = child, prev = null;
		    Widget last = null;
		    int idx = -1;

		    public boolean hasNext() {
			return(next != null);
		    }

		    public boolean hasPrevious() {
			return(prev != null);
		    }

		    public Widget next() {
			if(next == null)
			    throw(new NoSuchElementException());
			last = next;
			next = last.next;
			prev = last;
			idx++;
			return(last);
		    }

		    public Widget previous() {
			if(prev == null)
			    throw(new NoSuchElementException());
			last = prev;
			next = last;
			prev = last.prev;
			idx--;
			return(last);
		    }

		    public void add(Widget wdg) {throw(new UnsupportedOperationException());}
		    public void set(Widget wdg) {throw(new UnsupportedOperationException());}
		    public void remove() {
			if(last == null)
			    throw(new IllegalStateException());
			if(next == last)
			    next = next.next;
			if(prev == last)
			    prev = prev.prev;
			last.destroy();
			last = null;
		    }

		    public int nextIndex() {return(idx + 1);}
		    public int previousIndex() {return(idx);}
		};
	    for(int i = 0; i < idx; i++)
		ret.next();
	    return(ret);
	}
    }

    public List<Widget> children() {
	return(new Children());
    }

    /* XXX: Should be renamed to rchildren at this point. */
    public <T extends Widget> Set<T> children(final Class<T> cl) {
	return(new AbstractSet<T>() {
		public int size() {
		    int i = 0;
		    for(T w : this)
			i++;
		    return(i);
		}
		
		public Iterator<T> iterator() {
		    return(new Iterator<T>() {
			    T cur = n(Widget.this);
			    
			    private T n(Widget w) {
				for(Widget n; true; w = n) {
				    if(w == null) {
					return(null);
				    } else if(w.child != null) {
					n = w.child;
				    } else if(w == Widget.this) {
					return(null);
				    } else if(w.next != null) {
					n = w.next;
				    } else {
					for(n = w.parent; (n != null) && (n.next == null) && (n != Widget.this); n = n.parent);
					if((n == null) || (n == Widget.this))
					    return(null);
					n = n.next;
				    }
				    if((n == null) || cl.isInstance(n))
					return(cl.cast(n));
				}
			    }
			    
			    public T next() {
				if(cur == null)
				    throw(new NoSuchElementException());
				T ret = cur;
				cur = n(ret);
				return(ret);
			    }
			    
			    public boolean hasNext() {
				return(cur != null);
			    }
			});
		}
	    });
    }

    public Resource getcurs(Coord c) {
	Resource ret;
		
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if((ret = wdg.getcurs(c.add(cc.inv()))) != null)
		    return(ret);
	    }
	}
	try {
	    return((cursor == null)?null:cursor.get());
	} catch(Loading l) {
	    return(null);
	}
    }

    public static class PaginaTip implements Indir<Tex> {
	public final String title;
	public final Indir<Resource> res;
	private Tex rend;
	private boolean hasrend = false;

	public PaginaTip(Indir<Resource> res, String title) {
	    this.res = res;
	    this.title = title;
	}

	public PaginaTip(Indir<Resource> res) {
	    this(res, null);
	}

	public Tex get() {
	    if(!hasrend) {
		render: {
		    try {
			Resource.Pagina pag = res.get().layer(Resource.pagina);
			if(pag == null)
			    break render;
			String text;
			if(title == null) {
			    if(pag.text.length() == 0)
				break render;
			    text = pag.text;
			} else {
			    if(pag.text.length() == 0)
				text = title;
			    else
				text = title + "\n\n" + pag.text;
			}
			rend = RichText.render(text, 300).tex();
		    } catch(Loading l) {
			return(null);
		    }
		}
		hasrend = true;
	    }
	    return(rend);
	}
    }

    public class KeyboundTip implements Indir<Tex> {
	public final String base;
	private Tex rend = null;
	private boolean hrend = false;
	private KeyMatch rkey = null;

	public KeyboundTip(String base) {
	    this.base = base;
	}

	public KeyboundTip() {
	    this(null);
	}

	public Tex get() {
	    KeyMatch key = (kb_gkey == null) ? null : kb_gkey.key();
	    if(!hrend || (rkey != key)) {
		String tip;
		if(base != null) {
		    tip = RichText.Parser.quote(base);
		    if((key != null) && (key != KeyMatch.nil))
			tip = String.format("%s ($col[255,255,0]{%s})", tip, RichText.Parser.quote(kb_gkey.key().name()));
		} else {
		    if((key == null) || (key == KeyMatch.nil))
			tip = null;
		    else
			tip = String.format("Keyboard shortcut: $col[255,255,0]{%s}", RichText.Parser.quote(kb_gkey.key().name()));
		}
		rend = (tip == null) ? null : RichText.render(tip, 0).tex();
		hrend = true;
		rkey = key;
	    }
	    return(rend);
	}
    }

    @Deprecated
    public Object tooltip(Coord c, boolean again) {
	return(null);
    }

    public Object tooltip(Coord c, Widget prev) {
	if(prev != this)
	    prevtt = null;
	if(tooltip != null) {
	    prevtt = null;
	    return(tooltip);
	}
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		Object ret = wdg.tooltip(c.add(cc.inv()), prevtt);
		if(ret != null) {
		    prevtt = wdg;
		    return(ret);
		}
	    }
	}
	prevtt = null;
	return(tooltip(c, prev == this));
    }

    public Widget settip(String text) {
	tooltip = new KeyboundTip(text);
	return(this);
    }
    
    public <T extends Widget> T getparent(Class<T> cl) {
	for(Widget w = this; w != null; w = w.parent) {
	    if(cl.isInstance(w))
		return(cl.cast(w));
	}
	return(null);
    }

    public void hide() {
	visible = false;
	if(canfocus && (parent != null))
	    parent.delfocusable(this);
    }

    public void show() {
	visible = true;
	if(canfocus && (parent != null))
	    parent.newfocusable(this);
    }

    public boolean show(boolean show) {
	if(show)
	    show();
	else
	    hide();
	return(show);
    }

    public boolean tvisible() {
	for(Widget w = this; w != null; w = w.parent) {
	    if(!w.visible)
		return(false);
	}
	return(true);
    }

    public final Collection<Anim> anims = new LinkedList<Anim>();
    public final Collection<Anim> nanims = new LinkedList<Anim>();
    public <T extends Anim> void clearanims(Class<T> type) {
	for(Iterator<Anim> i = nanims.iterator(); i.hasNext();) {
	    Anim a = i.next();
	    if(type.isInstance(a))
		i.remove();
	}
	for(Iterator<Anim> i = anims.iterator(); i.hasNext();) {
	    Anim a = i.next();
	    if(type.isInstance(a))
		i.remove();
	}
    }

    public abstract class Anim {
	public Anim() {
	    synchronized(ui) {
		nanims.add(this);
	    }
	}

	public void clear() {
	    synchronized(ui) {
		nanims.remove(this);
		anims.remove(this);
	    }
	}

	public abstract boolean tick(double dt);
    }

    public abstract class NormAnim extends Anim {
	private double a = 0.0;
	private final double s;
	
	public NormAnim(double s) {
	    this.s = 1.0 / s;
	}

	public boolean tick(double dt) {
	    a += dt;
	    double na = a * s;
	    if(na >= 1.0) {
		ntick(1.0);
		return(true);
	    } else {
		ntick(na);
		return(false);
	    }
	}

	public abstract void ntick(double a);
    }

    public static class Temporary extends Widget {
	public void lower() {
	    Widget last = parent.lchild;
	    last.next = child;
	    child.prev = last;
	    for(Widget w = child; w != null; w = w.next) {
		w.parent = parent;
		w.c = w.c.add(c);
	    }
	    parent.lchild = lchild;
	    child = null;
	    lchild = null;
	    destroy();
	}

	public static void optimize(Widget wdg) {
	    for(Widget w = wdg.child; w != null; w = w.next) {
		if (w instanceof Temporary)
		    ((Temporary)w).lower();
		else
		    optimize(w);
	    }
	}
    }
}
