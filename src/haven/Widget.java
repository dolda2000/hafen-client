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
import java.util.concurrent.CopyOnWriteArrayList;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class Widget {
    public UI ui;
    public Coord c, sz;
    public int z;
    public Widget next, prev, child, lchild, parent;
    public int childseq;
    public boolean focustab = false, focusctl = false, hasfocus = false, visible = true;
    private boolean attached = false;
    public boolean canfocus = false, autofocus = false;
    public boolean canactivate = false, cancancel = false;
    public Widget focused;
    public Indir<Resource> cursor = null;
    public Object tooltip = null;
    public KeyMatch gkey;
    public KeyBinding kb_gkey;
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

    public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
	public FactMaker() {
	    super(Factory.class);
	    add(new Direct<>(Factory.class));
	    add(new StaticCall<>(Factory.class, "mkwidget", Widget.class, new Class<?>[] {UI.class, Object[]. class},
				 (make) -> (ui, args) -> make.apply(new Object [] {ui, args})));
	}
    }

    private static boolean inited = false;
    public static void initnames() {
	if(!inited) {
	    for(Factory f : dolda.jglob.Loader.get(RName.class).instances(Factory.class)) {
		synchronized(types) {
		    String nm = f.getClass().getAnnotation(RName.class).value();
		    if(types.put(nm, f) != null)
			Warning.warn("duplicated widget name: " + nm);
		}
	    }
	    inited = true;
	}
    }

    public static Factory gettype3(String name) {
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
	    Indir<Resource> res = Resource.remote().load(name, ver);
	    return(res.get().getcode(Factory.class, true));
	}
    }

    public static Factory gettype2(String name) throws InterruptedException {
	return(Loading.waitforint(() -> gettype3(name)));
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
	childseq++;
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
	return(add(child, Coord.of(x, y)));
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
		    st.push(Coord.of(x, y));
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
	if((args.length > 0) && (args[0] == null)) {
	    add(child);
	} if(args[0] instanceof Coord) {
	    Coord c = (Coord)args[0];
	    String opt = (args.length > 1) ? (String)args[1] : "";
	    if(opt.indexOf('u') < 0)
		c = UI.scale(c);
	    add(child, c);
	} else if(args[0] instanceof Coord2d) {
	    add(child, ((Coord2d)args[0]).mul(Coord2d.of(this.sz.sub(child.sz))).round());
	} else if(args[0] instanceof String) {
	    add(child, relpos((String)args[0], child, args, 1));
	} else {
	    throw(new UI.UIException("Unknown child widget creation specification.", null, args));
	}
    }

    public void link() {
	Widget prev;
	for(prev = parent.lchild; (prev != null) && (prev.z > this.z); prev = prev.prev);
	if(prev != null) {
	    if((this.next = prev.next) != null)
		this.next.prev = this;
	    else
		parent.lchild = this;
	    (this.prev = prev).next = this;
	} else {
	    if((this.next = parent.child) != null)
		this.next.prev = this;
	    else
		parent.lchild = this;
	    parent.child = this;
	}
    }
    
    public void linkfirst() {
	Widget next;
	for(next = parent.child; (next != null) && (next.z < this.z); next = next.next);
	if(next != null) {
	    if((this.prev = next.prev) != null)
		this.prev.next = this;
	    else
		parent.child = this;
	    (this.next = next).prev = this;
	} else {
	    if((this.prev = parent.lchild) != null)
		this.prev.next = this;
	    else
		parent.child = this;
	    parent.lchild = this;
	}
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
	    return(Coord.z);
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
	    parent = null;
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
	childseq++;
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
	
    public static class HandlerMaker extends Resource.PublishedCode.Instancer.Chain<MessageHandler> {
	public HandlerMaker() {super(MessageHandler.class);}
	{
	    add(new Direct<>(MessageHandler.class));
	    add(new StaticCall<>(MessageHandler.class, "uimsg", Void.TYPE, new Class<?>[] {Widget.class, Object[].class},
				 (handle) -> (tgt, args) -> handle.apply(new Object[] {tgt, args})));
	}
    }

    @Resource.PublishedCode(name = "uimsg", instancer = HandlerMaker.class)
    public static interface MessageHandler {
	public void handle(Widget tgt, Object... args);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "tabfocus") {
	    setfocustab(Utils.bv(args[0]));
	} else if(msg == "act") {
	    canactivate = Utils.bv(args[0]);
	} else if(msg == "cancel") {
	    cancancel = Utils.bv(args[0]);
	} else if(msg == "autofocus") {
	    autofocus = Utils.bv(args[0]);
	} else if(msg == "focus") {
	    int tid = Utils.iv(args[0]);
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
	} else if(msg == "z") {
	    z(Utils.iv(args[0]));
	} else if(msg == "show") {
	    show(Utils.bv(args[0]));
	} else if(msg == "curs") {
	    if(args.length == 0)
		cursor = null;
	    else if(args[0] instanceof String)
		cursor = Resource.remote().load((String)args[0], Utils.iv(args[1]));
	    else
		cursor = ui.sess.getresv(args[0]);
	} else if(msg == "tip") {
	    int a = 0;
	    Object tt = args[a++];
	    if(tt instanceof String) {
		settip((String)tt);
	    } else if(tt instanceof Integer) {
		tooltip = new PaginaTip(ui.sess.getresv(tt));
	    }
	} else if(msg == "gk") {
	    if(args[0] instanceof Integer) {
		KeyMatch key = gkeymatch(Utils.iv(args[0]));
		if(args.length > 1) {
		    int modign = 0;
		    if(args.length > 2)
			modign = Utils.iv(args[2]);
		    setgkey(KeyBinding.get("wgk/" + (String)args[1], key, modign));
		} else {
		    gkey = key;
		}
	    }
	} else if(msg == "ext") {
	    ui.sess.getresv(args[0]).get().getcode(MessageHandler.class, true).handle(this, Utils.splice(args, 1));
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

    public void tick(TickEvent ev) {
	tick(ev.dt);
    }

    public void gtick(haven.render.Render out) {
    }

    public void gtick(GTickEvent ev) {
	gtick(ev.out);
    }

    public void draw(GOut g, boolean strict) {
	Widget next;
		
	for(Widget wdg = child; wdg != null; wdg = next) {
	    next = wdg.next;
	    if(!wdg.visible)
		continue;
	    try(CPUProfile.Current prof = CPUProfile.begin(wdg)) {
		Coord cc = xlate(wdg.c, true);
		GOut g2;
		if(strict)
		    g2 = g.reclip(cc, wdg.sz);
		else
		    g2 = g.reclipl(cc, wdg.sz);
		wdg.draw(g2);
	    }
	}
    }
    
    public void draw(GOut g) {
	draw(g, true);
    }
	
    public boolean checkhit(Coord c) {
	return(c.isect(Coord.z, sz));
    }

    public static abstract class Event {
	public boolean propagate, grabbed;
	public Widget handling, target;
	private boolean phandled;

	public Event() {
	}

	public Event(Event from) {
	    this.grabbed = from.grabbed;
	}

	public Event grabbed(boolean g) {grabbed = g; return(this);}

	protected abstract boolean propagation(Widget from);

	public void stop() {
	    if(propagate) {
		propagate = false;
		phandled = false;
	    }
	}

	protected boolean shandle(Widget w) {
	    return(false);
	}

	public boolean propagate(Widget from) {
	    if(!propagate)
		return(phandled);
	    phandled = propagation(from);
	    propagate = false;
	    return(phandled);
	}

	public boolean fpropagate(Widget from) {
	    propagate = true;
	    return(propagate(from));
	}

	public boolean dispatch(Widget w) {
	    try(CPUProfile.Current prof = CPUProfile.begin(w)) {
		Widget phandling = handling;
		handling = w;
		try {
		    propagate = true;
		    if(w.handle(this))
			return(true);
		    return(propagate(w));
		} finally {
		    handling = phandling;
		}
	    }
	}
    }

    private List<EventHandler.Listener<?>> listening = null;

    public <E> void listen(Class<E> t, EventHandler<? super E> h) {
	if(listening == null)
	    listening = new CopyOnWriteArrayList<>();
	listening.add(new EventHandler.Listener<>(t, h));
    }

    public boolean deafen(EventHandler<?> h) {
	if(listening != null) {
	    for(int i = 0; i < listening.size(); i++) {
		EventHandler.Listener<?> l = listening.get(i);
		if(l.h == h) {
		    listening.remove(i);
		    return(true);
		}
	    }
	}
	return(false);
    }

    public <H extends EventHandler<?>> H listening(Class<H> cl) {
	if(listening != null) {
	    for(EventHandler.Listener<?> l : listening) {
		if(cl.isInstance(l.h))
		    return(cl.cast(l.h));
	    }
	}
	return(null);
    }

    public boolean handle(Event ev) {
	if(listening != null) {
	    for(EventHandler.Listener<?> l : listening) {
		if(l.check(ev))
		    return(true);
	    }
	}
	return(ev.shandle(this));
    }

    public static class TickEvent extends Event {
	public final double dt;

	public TickEvent(double dt) {
	    this.dt = dt;
	}

	protected boolean propagation(Widget from) {
	    for(Widget next, wdg = from.child; wdg != null; wdg = next) {
		next = wdg.next;
		dispatch(wdg);
	    }
	    return(true);
	}

	protected boolean shandle(Widget w) {
	    boolean pp = (w.parent != null);
	    w.tick(this);
	    /* XXX? This feels a bit hacky. */
	    return(pp && (w.parent == null));
	}
    }

    public static class GTickEvent extends Event {
	public final haven.render.Render out;

	public GTickEvent(haven.render.Render out) {
	    this.out = out;
	}

	protected boolean propagation(Widget from) {
	    for(Widget wdg = from.child; wdg != null; wdg = wdg.next)
		dispatch(wdg);
	    return(true);
	}

	protected boolean shandle(Widget w) {
	    w.gtick(this);
	    return(false);
	}
    }

    public static class MessageEvent extends Event {
	public final String msg;
	public final Object[] args;

	public MessageEvent(String msg, Object[] args) {
	    this.msg = msg.intern();
	    this.args = args;
	}

	protected boolean propagation(Widget from) {
	    return(false);
	}

	protected boolean shandle(Widget w) {
	    w.uimsg(msg, args);
	    return(true);
	}
    }

    public static abstract class PointerEvent extends Event {
	public final Coord c;

	public PointerEvent(Coord c) {
	    this.c = c;
	}
	public PointerEvent(PointerEvent from, Coord c) {
	    super(from);
	    this.c = c;
	}

	public abstract PointerEvent derive(Coord c);

	protected boolean propagation(Widget from) {
	    for(Widget wdg = from.lchild; wdg != null; wdg = wdg.prev) {
		if(!wdg.visible())
		    continue;
		Coord cc = from.xlate(wdg.c, true);
		if(c.isect(cc, wdg.sz)) {
		    if(derive(c.sub(cc)).dispatch(wdg))
			return(true);
		}
	    }
	    return(false);
	}
    }

    public static abstract class MouseEvent extends PointerEvent {
	public MouseEvent(Coord c) {super(c);}
	public MouseEvent(MouseEvent from, Coord c) {super(from, c);}
    }

    public static abstract class MouseActionEvent extends MouseEvent {
	public MouseActionEvent(Coord c) {super(c);}
	public MouseActionEvent(MouseEvent from, Coord c) {super(from, c);}
    }

    public static abstract class MouseButtonEvent extends MouseActionEvent {
	public final int b;

	public MouseButtonEvent(Coord c, int b) {
	    super(c);
	    this.b = b;
	}
	public MouseButtonEvent(MouseButtonEvent from, Coord c) {
	    super(from, c);
	    this.b = from.b;
	}

	public abstract MouseButtonEvent derive(Coord c);
    }

    public static class MouseDownEvent extends MouseButtonEvent {
	public MouseDownEvent(Coord c, int b) {
	    super(c, b);
	}
	public MouseDownEvent(MouseDownEvent from, Coord c) {
	    super(from, c);
	}

	public MouseDownEvent derive(Coord c) {return(new MouseDownEvent(this, c));}

	protected boolean shandle(Widget w) {
	    if(hackhandle(this, w, "mousedown", new Class<?>[] {Coord.class, Integer.TYPE}, c, b))
		return(true);
	    if(w.mousedown(this))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static class MouseUpEvent extends MouseButtonEvent {
	public MouseUpEvent(Coord c, int b) {
	    super(c, b);
	}
	public MouseUpEvent(MouseUpEvent from, Coord c) {
	    super(from, c);
	}

	public MouseUpEvent derive(Coord c) {return(new MouseUpEvent(this, c));}

	protected boolean shandle(Widget w) {
	    if(hackhandle(this, w, "mouseup", new Class<?>[] {Coord.class, Integer.TYPE}, c, b))
		return(true);
	    if(w.mouseup(this))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static class MouseMoveEvent extends MouseEvent {
	public MouseMoveEvent(Coord c) {super(c);}
	public MouseMoveEvent(MouseMoveEvent from, Coord c) {super(from, c);;}

	public MouseMoveEvent derive(Coord c) {return(new MouseMoveEvent(this, c));}

	protected boolean propagation(Widget from) {
	    for(Widget wdg = from.lchild; wdg != null; wdg = wdg.prev) {
		if(!wdg.visible())
		    continue;
		Coord cc = from.xlate(wdg.c, true);
		derive(c.sub(cc)).dispatch(wdg);
	    }
	    return(true);
	}

	public boolean shandle(Widget w) {
	    if(hackhandle(this, w, "mousemove", new Class<?>[] {Coord.class}, c))
		return(true);
	    w.mousemove(this);
	    return(false);
	}
    }

    public static class MouseWheelEvent extends MouseActionEvent {
	public final int a;

	public MouseWheelEvent(Coord c, int a) {
	    super(c);
	    this.a = a;
	}
	public MouseWheelEvent(MouseWheelEvent from, Coord c) {
	    super(from, c);
	    this.a = from.a;
	}

	public MouseWheelEvent derive(Coord c) {return(new MouseWheelEvent(this, c));}

	public boolean shandle(Widget w) {
	    if(hackhandle(this, w, "mousewheel", new Class<?>[] {Coord.class, Integer.TYPE}, c, a))
		return(true);
	    if(w.mousewheel(this))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static class MouseHoverEvent extends MouseEvent {
	public boolean hovering;

	public MouseHoverEvent(Coord c) {
	    super(c);
	    hovering = true;
	}
	public MouseHoverEvent(MouseHoverEvent from, Coord c) {
	    super(from, c);
	}

	public MouseHoverEvent derive(Coord c) {return(new MouseHoverEvent(this, c));}

	public MouseHoverEvent hovering(boolean h) {hovering = h; return(this);}

	protected boolean propagation(Widget from) {
	    boolean ret = false;
	    boolean hovering = this.hovering;
	    for(Widget wdg = from.lchild; wdg != null; wdg = wdg.prev) {
		Coord cc = from.xlate(wdg.c, true);
		boolean inside = c.isect(cc, wdg.sz);
		boolean ch = hovering && inside && wdg.visible();
		if(derive(c.sub(cc)).hovering(ch).dispatch(wdg) && ch) {
		    hovering = false;
		    ret = true;
		}
	    }
	    return(ret);
	}

	protected boolean shandle(Widget w) {
	    if(hackhandle(this, w, "mousehover", new Class<?>[] {Coord.class, Boolean.TYPE}, c, hovering))
		return(true);
	    if(w.mousehover(this, hovering))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static final KeyMatch key_act = KeyMatch.forcode(KeyEvent.VK_ENTER, 0);
    public static final KeyMatch key_esc = KeyMatch.forcode(KeyEvent.VK_ESCAPE, 0);
    public static final KeyMatch key_tab = KeyMatch.forcode(KeyEvent.VK_TAB, 0);
    public static abstract class KbdEvent extends Event {
	public final KeyEvent awt;
	public final int code, mods;
	public final char c;

	public KbdEvent(KeyEvent awt) {
	    this.awt = awt;
	    this.code = awt.getKeyCode();
	    mods = UI.modflags(awt);
	    char c = awt.getKeyChar();
	    if(((mods & KeyMatch.C) != 0) && (c < 32)) {
		/* Undo Java's TTY Control-code mangling */
		if(code == KeyEvent.VK_BACK_SPACE) {
		} else if(code == KeyEvent.VK_ENTER) {
		} else if(code == KeyEvent.VK_TAB) {
		} else if(code == KeyEvent.VK_ESCAPE) {
		} else {
		    if((mods & KeyMatch.S) != 0)
			c = (char)(c + 'A' - 1);
		    else
			c = (char)(c + 'a' - 1);
		}
	    }
	    if(c == awt.CHAR_UNDEFINED)
		c = 0;
	    this.c = c;
	}
    }

    public static abstract class FocusedKeyEvent extends KbdEvent {
	public FocusedKeyEvent(KeyEvent awt) {
	    super(awt);
	}

	protected boolean propagation(Widget from) {
	    if(from.focusctl) {
		if(from.focused == null)
		    return(false);
		return(dispatch(from.focused));
	    } else {
		for(Widget wdg = from.child; wdg != null; wdg = wdg.next) {
		    if(wdg.visible()) {
			if(dispatch(wdg))
			    return(true);
		    }
		}
		return(false);
	    }
	}
    }

    public static class KeyDownEvent extends FocusedKeyEvent {
	public KeyDownEvent(KeyEvent awt) {super(awt);}

	protected boolean shandle(Widget w) {
	    if(hackhandle(this, w, "keydown", new Class<?>[] {KeyEvent.class}, awt))
		return(true);
	    if(w.keydown(this))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static class KeyUpEvent extends FocusedKeyEvent {
	public KeyUpEvent(KeyEvent awt) {super(awt);}

	public boolean shandle(Widget w) {
	    if(hackhandle(this, w, "keyup", new Class<?>[] {KeyEvent.class}, awt))
		return(true);
	    if(w.keyup(this))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static class GlobKeyEvent extends KbdEvent {
	public GlobKeyEvent(KeyEvent awt) {super(awt);}

	protected boolean propagation(Widget from) {
	    for(Widget wdg = from.lchild; wdg != null; wdg = wdg.prev) {
		if(dispatch(wdg))
		    return(true);
	    }
	    return(false);
	}

	protected boolean shandle(Widget w) {
	    if(hackhandle(this, w, "keydown", new Class<?>[] {Character.TYPE, KeyEvent.class}, awt.getKeyChar(), awt))
		return(true);
	    if(w.globtype(this))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static abstract class QueryEvent<R> extends PointerEvent {
	public final QueryEvent<R> root;
	public R ret;

	public QueryEvent(Coord c) {
	    super(c);
	    root = this;
	    ret = defvalue();
	}
	public QueryEvent(QueryEvent<R> from, Coord c) {
	    super(from, c);
	    root = from.root;
	}

	/* Return value doesn't indicate anything, it's just to be
	 * able to do return(ev.nset(res)). */
	public boolean set(R ret) {
	    root.ret = ret;
	    return(true);
	}

	protected R defvalue() {return(null);}
    }

    public static class TooltipQuery extends PointerEvent {
	public final TooltipQuery root;
	public final Widget last;
	public Object ret;
	public Widget from;

	public TooltipQuery(Coord c, Widget last) {
	    super(c);
	    this.root = this;
	    this.last = last;
	}
	public TooltipQuery(TooltipQuery from, Coord c) {
	    super(from, c);
	    this.root = from.root;
	    this.last = from.last;
	}
	public TooltipQuery derive(Coord c) {return(new TooltipQuery(this, c));}

	/* Return value doesn't indicate anything, it's just to be
	 * able to do return(ev.nset(res)). */
	public boolean set(Object ret, Widget from) {
	    root.ret = ret;
	    root.from = from;
	    return(true);
	}

	protected boolean shandle(Widget w) {
	    if(w.tooltip(this))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static class CursorQuery extends QueryEvent<Object> {
	public static final Resource defcurs = Resource.local().loadwait("gfx/hud/curs/arw");

	public CursorQuery(Coord c) {super(c);}
	public CursorQuery(CursorQuery from, Coord c) {super(from, c);}
	public CursorQuery derive(Coord c) {return(new CursorQuery(this, c));}

	public static interface Handler {
	    public boolean getcurs(CursorQuery ev);
	}

	protected boolean shandle(Widget w) {
	    if((w instanceof Handler) && ((Handler)w).getcurs(this))
		return(true);
	    if(propagate(w))
		return(true);
	    if(w.cursor != null) {
		try {
		    set(w.cursor.get());
		    return(true);
		} catch(Loading l) {}
	    }
	    return(super.shandle(w));
	}

	protected Object defvalue() {return(defcurs);}
    }

    /* XXX: Remove me! */
    private static final ThreadLocal<Event> hackhandling = new ThreadLocal<>();
    private static final Set<Pair<Class, String>> hackwarned = new HashSet<>();
    private static boolean hackhandle(Event ev, Widget w, String nm, Class<?>[] argt, Object... args) {
	Event prev = hackhandling.get();
	hackhandling.set(ev);
	try {
	    Class<?> cls = w.getClass();
	    Method m = cls.getMethod(nm, argt),
		  wm = Widget.class.getMethod(nm, argt);
	    if(Utils.eq(m, wm))
		return(false);
	    if(!cls.getName().startsWith("haven.res.") && hackwarned.add(new Pair<>(cls, nm)))
		Warning.warn("hack-hacndling event %s for %s", nm, cls);
	    Boolean ret = (Boolean)wm.invoke(w, args);
	    return((ret == null) ? false : ret);
	} catch(InvocationTargetException e) {
	    throw((RuntimeException)e.getCause());
	} catch(NoSuchMethodException | IllegalAccessException e) {
	    return(false);
	} finally {
	    hackhandling.set(prev);
	}
    }

    public boolean mousedown(MouseDownEvent ev) {return(false);}
    public boolean mouseup(MouseUpEvent ev) {return(false);}
    public boolean mousewheel(MouseWheelEvent ev) {return(false);}
    public void mousemove(MouseMoveEvent ev) {}
    public boolean mousehover(MouseHoverEvent ev, boolean hovering) {return(false);}

    @Deprecated
    public boolean mousedown(Coord c, int button) {
	return(hackhandling.get().propagate(this));
    }
	
    @Deprecated
    public boolean mouseup(Coord c, int button) {
	return(hackhandling.get().propagate(this));
    }
	
    @Deprecated
    public boolean mousewheel(Coord c, int amount) {
	return(hackhandling.get().propagate(this));
    }
	
    @Deprecated
    public void mousemove(Coord c) {
	hackhandling.get().propagate(this);
    }

    @Deprecated
    public boolean mousehover(Coord c, boolean hovering) {
	return(hackhandling.get().propagate(this));
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

    public boolean gkeytype(GlobKeyEvent ev) {
	wdgmsg("activate", UI.modflags(ev.awt));
	return(true);
    }

    public boolean keydown(KeyDownEvent ev) {
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
	if(ev.propagate(this))
	    return(true);
	if(focusctl && focustab) {
	    Widget f = focused;
	    if(key_tab.match(ev.awt) && (f != null)) {
		while(true) {
		    if((ev.mods & KeyMatch.S) == 0) {
			Widget n = f.rnext();
			f = ((n == null) || !n.hasparent(this)) ? this.child : n;
		    } else {
			Widget p = f.rprev();
			f = ((p == null) || (p == this) || !p.hasparent(this)) ? this.lchild : p;
		    }
		    if((f.canfocus && f.tvisible()) || (f == focused))
			break;
		}
		setfocus(f);
		return(true);
	    }
	}
	return(false);
    }

    public boolean keyup(KeyUpEvent ev) {
	return(false);
    }

    public boolean globtype(GlobKeyEvent ev) {
	KeyMatch gkey = this.gkey;
	if(kb_gkey != null)
	    gkey = kb_gkey.key();
	if((gkey != null) && gkey.match(ev.awt)) {
	    if(gkeytype(ev))
		return(true);
	}
	return(false);
    }

    @Deprecated
    public boolean gkeytype(KeyEvent ev) {
	wdgmsg("activate", UI.modflags(ev));
	return(true);
    }

    @Deprecated
    public boolean globtype(char key, KeyEvent ev) {
	return(hackhandling.get().propagate(this));
    }

    public Widget setgkey(KeyMatch gkey) {
	this.gkey = gkey;
	return(this);
    }

    public Widget setgkey(KeyBinding gkey) {
	kb_gkey = gkey;
	if((tooltip == null) && (kb_gkey != null))
	    tooltip = new KeyboundTip();
	return(this);
    }

    @Deprecated
    public boolean keydown(KeyEvent ev) {
	return(hackhandling.get().propagate(this));
    }
	
    @Deprecated
    public boolean keyup(KeyEvent ev) {
	return(hackhandling.get().propagate(this));
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
	if(Utils.eq(this.sz, sz))
	    return;
	this.sz = sz;
	for(Widget ch = child; ch != null; ch = ch.next)
	    ch.presize();
	if(parent != null)
	    parent.cresize(this);
    }

    public void z(int z) {
	if(z != this.z) {
	    this.z = z;
	    if(parent != null) {
		unlink();
		link();
	    }
	}
    }

    public void move(Area a) {
	move(a.ul);
	resize(a.sz());
    }

    public void resize(int x, int y) {resize(Coord.of(x, y));}
    public void resizew(int w) {resize(w, sz.y);}
    public void resizeh(int h) {resize(sz.x, h);}

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

    public Coord addhlp(Coord c, int pad, Widget... children) {
	int x = c.x, y = c.y;
	int maxh = 0;
	for(Widget child : children)
	    maxh = Math.max(maxh, child.sz.y);
	for(Widget child : children) {
	    add(child, x, y + ((maxh - child.sz.y) / 2));
	    x += child.sz.x + pad;
	}
	return(Coord.of(x - pad, y + maxh));
    }

    public int addhlp(Coord c, int pad, int w, Widget... children) {
	int cw = (w - ((children.length - 1) * pad)) / children.length;
	for(Widget ch : children)
	    ch.resizew(cw);
	return(addhl(c, w, children));
    }

    public int addhl(Coord c, int w, Widget... children) {
	int x = c.x, y = c.y;
	if(children.length == 1) {
	    adda(children[0], x + (w / 2), y, 0.5, 0.0);
	    return(y + children[0].sz.y);
	}
	int maxh = 0, cw = 0;
	for(Widget child : children) {
	    cw += child.sz.x;
	    maxh = Math.max(maxh, child.sz.y);
	}
	int tpad = w - cw, npad = children.length - 1, perror = 0;
	for(Widget child : children) {
	    add(child, x, y + ((maxh - child.sz.y) / 2));
	    x += child.sz.x;
	    perror += tpad;
	    x += perror / npad;
	    perror %= npad;
	}
	return(y + maxh);
    }

    public Coord addvlp(Coord c, int pad, Widget... children) {
	int x = c.x, y = c.y;
	int maxw = 0;
	for(Widget child : children)
	    maxw = Math.max(maxw, child.sz.x);
	for(Widget child : children) {
	    add(child, x + ((maxw - child.sz.x) / 2), y);
	    y += child.sz.y + pad;
	}
	return(Coord.of(x + maxw, y - pad));
    }

    public int addvlp(Coord c, int pad, int h, Widget... children) {
	int ch = (h - ((children.length - 1) * pad)) / children.length;
	for(Widget wdg : children)
	    wdg.resizeh(ch);
	return(addvl(c, h, children));
    }

    public int addvl(Coord c, int h, Widget... children) {
	int x = c.x, y = c.y;
	if(children.length == 1) {
	    adda(children[0], x, y + (h / 2), 0.0, 0.5);
	    return(x + children[0].sz.x);
	}
	int maxw = 0, ch = 0;
	for(Widget child : children) {
	    ch += child.sz.y;
	    maxw = Math.max(maxw, child.sz.x);
	}
	int tpad = h - ch, npad = children.length - 1, perror = 0;
	for(Widget child : children) {
	    add(child, x + ((maxw - child.sz.x) / 2), y);
	    y += child.sz.y;
	    perror += tpad;
	    y += perror / npad;
	    perror %= npad;
	}
	return(x + maxw);
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
			rend = RichText.render(text, UI.scale(300)).tex();
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
	public final boolean rich;
	private Tex rend = null;
	private boolean hrend = false;
	private KeyMatch rkey = null;

	public KeyboundTip(String base, boolean rich) {
	    this.base = base;
	    this.rich = rich;
	}

	public KeyboundTip(String base) {
	    this(base, false);
	}

	public KeyboundTip() {
	    this(null);
	}

	public Tex get() {
	    KeyMatch key = (kb_gkey == null) ? null : kb_gkey.key();
	    if(!hrend || (rkey != key)) {
		String tip;
		int w = 0;
		if(base != null) {
		    if(rich) {
			tip = base;
			if((key != null) && (key != KeyMatch.nil))
			    tip = String.format("%s\n\nKeyboard shortcut: $col[255,255,0]{%s}", tip, RichText.Parser.quote(key.name()));
			w = UI.scale(300);
		    } else {
			tip = RichText.Parser.quote(base);
			if((key != null) && (key != KeyMatch.nil))
			    tip = String.format("%s ($col[255,255,0]{%s})", tip, RichText.Parser.quote(key.name()));
		    }
		} else {
		    if((key == null) || (key == KeyMatch.nil))
			tip = null;
		    else
			tip = String.format("Keyboard shortcut: $col[255,255,0]{%s}", RichText.Parser.quote(key.name()));
		}
		rend = (tip == null) ? null : RichText.render(tip, w).tex();
		hrend = true;
		rkey = key;
	    }
	    return(rend);
	}
    }

    public Object tooltip(Coord c, Widget prev) {
	return(tooltip);
    }

    public boolean tooltip(TooltipQuery ev) {
	if(ev.propagate(this))
	    return(true);
	if(checkhit(ev.c)) {
	    Object tip;
	    try {
		tip = tooltip(ev.c, ev.last);
	    } catch(Loading l) {
		tip = "...";
	    }
	    if(tip != null) {
		ev.set(tip, this);
		return(true);
	    }
	}
	return(false);
    }

    public Widget settip(String text, boolean rich) {
	tooltip = new KeyboundTip(text, rich);
	return(this);
    }

    public Widget settip(String text) {
	return(settip(text, false));
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
	if(parent != null)
	    parent.delfocusable(this);
    }

    public void show() {
	visible = true;
	if(parent != null)
	    parent.newfocusable(this);
    }

    public boolean show(boolean show) {
	if(show)
	    show();
	else
	    hide();
	return(show);
    }

    public boolean visible() {
	return(visible);
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
	    synchronized((ui == null) ? this : ui) {
		nanims.add(this);
	    }
	}

	public void clear() {
	    synchronized((ui == null) ? this : ui) {
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

    public static final OwnerContext.ClassResolver<Widget> wdgctx = new OwnerContext.ClassResolver<Widget>()
	.add(Widget.class, wdg -> wdg)
	.add(UI.class, wdg -> wdg.ui)
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
}
