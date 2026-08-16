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

package haven.iosys.tk;

import java.util.*;
import java.util.function.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import haven.*;
import haven.iosys.*;
import haven.render.*;
import haven.render.gl.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.filechooser.*;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import javax.swing.JFileChooser;
import static java.awt.event.KeyEvent.*;

public abstract class AWTToolkit implements Toolkit {
    public static void initawt1() {
	try {
	    System.setProperty("sun.java2d.uiScale.enabled", "false");
	    System.setProperty("apple.awt.application.name", "Haven & Hearth");
	} catch(Throwable e) {
	    new Warning(e, "AWT initialization failed").issue();
	}
    }

    public static void initawt2() {
	try {
	    javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	    // java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().push(new DebugEventQueue());
	} catch(Throwable e) {
	    new Warning(e, "AWT initialization failed").issue();
	}
    }

    static {
	initawt1();
    }

    protected AWTToolkit() {
	initawt2();
	try {
	    emptycurs = java.awt.Toolkit.getDefaultToolkit().createCustomCursor(TexI.mkbuf(new Coord(1, 1)), new java.awt.Point(), "");
	} catch(HeadlessException e) {
	    throw(new Unavailable("AWT is running headlessly", e));
	} catch(Throwable e) {
	    throw(new Unavailable("AWT failed to initialize", e));
	}
    }

    protected void awtrun(Runnable task) {
	if(EventQueue.isDispatchThread()) {
	    task.run();
	} else {
	    try {
		EventQueue.invokeAndWait(task);
	    } catch(InterruptedException e) {
		Thread.currentThread().interrupt();
	    } catch(java.lang.reflect.InvocationTargetException e) {
		if(e.getCause() instanceof RuntimeException)
		    throw((RuntimeException)e.getCause());
		throw(new RuntimeException(e));
	    }
	}
    }

    public static <T> Promise<T> awtpromise(Supplier<T> task) {
	return(Promise.deferred(task, EventQueue::invokeLater));
    }

    private static Set<Key.Mod> awtmods(int awt) {
	Set<Key.Mod> ret = EnumSet.noneOf(Key.Mod.class);
	if((awt & InputEvent.SHIFT_DOWN_MASK) != 0) ret.add(Key.Mod.SHIFT);
	if((awt & InputEvent.CTRL_DOWN_MASK) != 0) ret.add(Key.Mod.CONTROL);
	if((awt & InputEvent.META_DOWN_MASK) != 0) ret.add(Key.Mod.META);
	if((awt & InputEvent.ALT_DOWN_MASK) != 0) ret.add(Key.Mod.ALT);
	if((awt & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) ret.add(Key.Mod.ALTGR);
	return(ret);
    }

    public static class AWTSym implements Key.Sym {
	public final int code;
	public final String id, nm;

	public AWTSym(int code, String nm) {
	    this.code = code;
	    this.id = String.format("awt:%x", code).intern();
	    this.nm = nm;
	}

	public String id() {return(id);}
	public String nm() {return(nm);}

	public boolean equals(AWTSym that) {return(this.code == that.code);}
	public boolean equals(Object x) {return((x instanceof AWTSym) && equals((AWTSym)x));}
	public int hashCode() {return(code);}

	public String toString() {
	    return(String.format("#<awt-sym %x(%s)>", code, nm));
	}
    }

    public static class AWTKey implements Key {
	public final int pc, ec;
	public final char ch;
	public final Key.Std stdsym;
	public final AWTSym extsym;

	public AWTKey(int pc, int ec, char ch) {
	    this.pc = pc;
	    this.ec = ec;
	    this.ch = ch;
	    this.stdsym = stdsyms.get(pc);
	    this.extsym = extended();
	}

	public String id() {
	    return(String.format("awt:%x", ec).intern());
	}

	private AWTSym extended() {
	    if(ec == java.awt.event.KeyEvent.VK_UNDEFINED)
		return(null);
	    String name;
	    if(pc != java.awt.event.KeyEvent.VK_UNDEFINED)
		name = java.awt.event.KeyEvent.getKeyText(pc);
	    else if(!Character.isISOControl(ch))
		name = Character.toString(Character.toUpperCase(ch));
	    else
		name = String.format("%X", ec);
	    return(new AWTSym(ec, name));
	}

	public Sym primary() {
	    return((stdsym != null) ? stdsym : extsym);
	}

	public Sym primary(Collection<? extends Sym> of) {
	    if((stdsym != null) && of.contains(stdsym))
		return(stdsym);
	    if((extsym != null) && of.contains(extsym))
		return(stdsym);
	    return(null);
	}

	public String toString() {
	    return(String.format("#<awt-key pc=%x ec=%x std=%s ext=%s>", pc, ec, stdsym, extsym));
	}
    }

    public static class AWTKeyEvent {
	public final java.awt.event.KeyEvent awt;
	public final Set<Key.Mod> mods;
	public final AWTKey key;

	public AWTKeyEvent(java.awt.event.KeyEvent awt) {
	    this.awt = awt;
	    this.key = new AWTKey(awt.getKeyCode(), awt.getExtendedKeyCode(), awt.getKeyChar());
	    mods = awtmods(awt.getModifiersEx());
	}

	public Key key() {
	    return(key);
	}
	public Key.Sym sym() {
	    return(key.primary());
	}
	public String string() {
	    char c = awt.getKeyChar();
	    return((c == java.awt.event.KeyEvent.CHAR_UNDEFINED) ? "" : Character.toString(c));
	}
	public Set<Key.Mod> mods() {return(mods);}

	public String toString() {
	    return(String.format("#<%s %s str=\"%s\" %s>", getClass().getSimpleName(), key, (string() == null) ? "" : Utils.bprint.enc(string().getBytes(Utils.utf8)), awt));
	}
    }
    public static class AWTKeyDownEvent extends AWTKeyEvent implements KeyDownEvent {
	public AWTKeyDownEvent(java.awt.event.KeyEvent awt) {super(awt);}
    }
    public static class AWTKeyUpEvent extends AWTKeyEvent implements KeyUpEvent {
	public AWTKeyUpEvent(java.awt.event.KeyEvent awt) {super(awt);}
    }

    public static class AWTMouseEvent {
	public final java.awt.event.MouseEvent awt;
	private Set<Key.Mod> mods = null;
	private Set<MouseBtn> held = null;

	public AWTMouseEvent(java.awt.event.MouseEvent awt) {
	    this.awt = awt;
	}

	public Coord wndc() {return(Coord.of(awt.getX(), awt.getY()));}
	public Set<MouseBtn> held() {
	    if(held == null) {
		int mod = awt.getModifiersEx();
		held = new HashSet<>();
		if((mod & InputEvent.BUTTON1_DOWN_MASK) != 0) held.add(MouseBtn.Std.LEFT);
		if((mod & InputEvent.BUTTON2_DOWN_MASK) != 0) held.add(MouseBtn.Std.MIDDLE);
		if((mod & InputEvent.BUTTON3_DOWN_MASK) != 0) held.add(MouseBtn.Std.RIGHT);
	    }
	    return(held);
	}
	public Set<Key.Mod> mods() {
	    if(mods == null)
		mods = awtmods(awt.getModifiersEx());
	    return(mods);
	}
	public MouseBtn button() {
	    int id = awt.getButton();
	    /* XXX: Back/forward buttons? I'm not sure they're even
	     * standardized in AWT... */
	    if(id == java.awt.event.MouseEvent.BUTTON1)
		return(MouseBtn.Std.LEFT);
	    else if(id == java.awt.event.MouseEvent.BUTTON2)
		return(MouseBtn.Std.MIDDLE);
	    else if(id == java.awt.event.MouseEvent.BUTTON3)
		return(MouseBtn.Std.RIGHT);
	    return(new MouseBtn() {
		public String id() {return("awt:" + id);}
		public String nm() {return("Button " + id);}
	    });
	}
    }
    public static class AWTMouseDownEvent extends AWTMouseEvent implements MouseDownEvent {
	public AWTMouseDownEvent(java.awt.event.MouseEvent awt) {super(awt);}
    }
    public static class AWTMouseUpEvent extends AWTMouseEvent implements MouseUpEvent {
	public AWTMouseUpEvent(java.awt.event.MouseEvent awt) {super(awt);}
    }
    public static class AWTMouseMoveEvent extends AWTMouseEvent implements MouseMoveEvent {
	public AWTMouseMoveEvent(java.awt.event.MouseEvent awt) {super(awt);}
    }
    public static class AWTMouseWheelEvent extends AWTMouseEvent implements MouseWheelEvent {
	public final java.awt.event.MouseWheelEvent wev;
	
	public AWTMouseWheelEvent(java.awt.event.MouseWheelEvent awt) {
	    super(awt);
	    this.wev = awt;
	}

	public Axis axis() {return(Axis.VERT);}
	public int amount() {return(wev.getWheelRotation());}
	public double subamount() {return(wev.getPreciseWheelRotation());}
    }

    public final java.awt.Cursor emptycurs;
    public java.awt.Cursor getsyscurs(Cursor.Std id) {
	switch(id) {
	case DEFAULT:   return(null);
	case NONE:      return(emptycurs);
	case POINTER:   return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
	case HAND:      return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
	case MOVE:      return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
	case WAIT:      return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
	case CARET:     return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.TEXT_CURSOR));
	case CROSSHAIR: return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.CROSSHAIR_CURSOR));
	case SIZE_N:    return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.N_RESIZE_CURSOR));
	case SIZE_NE:   return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.NE_RESIZE_CURSOR));
	case SIZE_E:    return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR));
	case SIZE_SE:   return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.SE_RESIZE_CURSOR));
	case SIZE_S:    return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.S_RESIZE_CURSOR));
	case SIZE_SW:   return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.SW_RESIZE_CURSOR));
	case SIZE_W:    return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.W_RESIZE_CURSOR));
	case SIZE_NW:   return(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.NW_RESIZE_CURSOR));
	default: return(null);
	}
    }

    public static class AWTClipboard implements Clipboard {
	public final java.awt.datatransfer.Clipboard bk;

	public AWTClipboard(java.awt.datatransfer.Clipboard bk) {
	    this.bk = bk;
	}

	private static Collection<Path> fl2pc(List files) {
	    Collection<Path> paths = new ArrayList<>();
	    ((List<?>)files).forEach(file -> paths.add(((File)file).toPath()));
	    return(paths);
	}

	public void put(Contents c, Runnable expire) {
	    try {
		bk.setContents(new Transferable() {
		    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if(flavor.equals(DataFlavor.stringFlavor)) {
			    Item<CharSequence> item = c.find(Clipboard.Format.TEXT);
			    if(item != null)
				return(item.fetch().toString());
			}
			if(flavor.equals(DataFlavor.imageFlavor)) {
			    Item<BufferedImage> item = c.find(Clipboard.Format.IMAGE);
			    if(item != null)
				return(item.fetch());
			}
			if(flavor.equals(DataFlavor.javaFileListFlavor)) {
			    Item<Collection<Path>> item = c.find(Clipboard.Format.PATHS);
			    if(item != null) {
				List<File> ret = new ArrayList<>();
				item.fetch().forEach(path -> ret.add(path.toFile()));
				return(ret);
			    }
			}
			throw(new UnsupportedFlavorException(flavor));
		    }

		    public boolean isDataFlavorSupported(DataFlavor flavor) {
			if(flavor.equals(DataFlavor.stringFlavor))
			    return(c.find(Clipboard.Format.TEXT) != null);
			if(flavor.equals(DataFlavor.imageFlavor))
			    return(c.find(Clipboard.Format.IMAGE) != null);
			if(flavor.equals(DataFlavor.javaFileListFlavor))
			    return(c.find(Clipboard.Format.PATHS) != null);
			return(false);
		    }

		    public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] buf = new DataFlavor[3];
			int n = 0;
			if(c.find(Clipboard.Format.TEXT) != null)
			    buf[n++] = DataFlavor.stringFlavor;
			if(c.find(Clipboard.Format.IMAGE) != null)
			    buf[n++] = DataFlavor.imageFlavor;
			if(c.find(Clipboard.Format.PATHS) != null)
			    buf[n++] = DataFlavor.javaFileListFlavor;
			return(Utils.splice(buf, 0, n));
		    }
		}, new ClipboardOwner() {
		    public void lostOwnership(java.awt.datatransfer.Clipboard cb, Transferable cnt) {
			if(expire != null)
			    expire.run();
		    }
		});
	    } catch(IllegalStateException e) {}
	}

	public static <T> Promise<T> xfpromise(Class<T> typ, Transferable xf, DataFlavor flavor) {
	    return(awtpromise(() -> {
		try {
		    return(typ.cast(xf.getTransferData(flavor)));
		} catch(UnsupportedFlavorException | IOException e) {
		    throw(new RuntimeException(e));
		}
	    }));
	}

	public Promise<Contents> get() {
	    return(awtpromise(() -> bk.getContents(null)).map(xf -> {
		Collection<Item<?>> ret = new ArrayList<>();
		if(xf.isDataFlavorSupported(DataFlavor.stringFlavor))
		    ret.add(new Item<CharSequence>(Format.TEXT, () -> xfpromise(CharSequence.class, xf, DataFlavor.stringFlavor)));
		if(xf.isDataFlavorSupported(DataFlavor.imageFlavor))
		    ret.add(new Item<BufferedImage>(Format.IMAGE, () -> xfpromise(BufferedImage.class, xf, DataFlavor.imageFlavor)));
		if(xf.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
		    ret.add(new Item<Collection<Path>>(Format.PATHS, () -> xfpromise(List.class, xf, DataFlavor.javaFileListFlavor).map(AWTClipboard::fl2pc)));
		}
		return(new Contents(ret));
	    }));
	}

	public static  abstract class AWTDropEvent {
	    public static final BMap<DropHandler.Action, Integer> actmap =
		new HashBMap<>(Utils.<DropHandler.Action, Integer>map()
			       .put(DropHandler.Action.COPY, DnDConstants.ACTION_COPY)
			       .put(DropHandler.Action.MOVE, DnDConstants.ACTION_MOVE)
			       .put(DropHandler.Action.LINK, DnDConstants.ACTION_LINK)
			       .map());
	    public abstract Coord wndc();
	    protected abstract boolean supported(DataFlavor flavor);
	    protected abstract int awtactions();
	    protected abstract <T> Promise<T> xfpromise(Class<T> typ, DataFlavor flavor);

	    public Set<DropHandler.Action> actions() {
		Set<DropHandler.Action> ret = EnumSet.noneOf(DropHandler.Action.class);
		int a = awtactions();
		for(Map.Entry<DropHandler.Action, Integer> ent : actmap.entrySet())
		    if((a & ent.getValue()) != 0) ret.add(ent.getKey());
		return(ret);
	    }

	    protected Clipboard.Contents mkcontents() {
		Collection<Clipboard.Item<?>> ret = new ArrayList<>();
		if(supported(DataFlavor.stringFlavor))
		    ret.add(new Clipboard.Item<CharSequence>(Clipboard.Format.TEXT, () -> xfpromise(CharSequence.class, DataFlavor.stringFlavor)));
		if(supported(DataFlavor.imageFlavor))
		    ret.add(new Clipboard.Item<BufferedImage>(Clipboard.Format.IMAGE, () -> xfpromise(BufferedImage.class, DataFlavor.imageFlavor)));
		if(supported(DataFlavor.javaFileListFlavor))
		    ret.add(new Clipboard.Item<Collection<Path>>(Clipboard.Format.PATHS, () -> xfpromise(List.class, DataFlavor.javaFileListFlavor).map(AWTClipboard::fl2pc)));
		return(new Clipboard.Contents(ret));
	    }
	}
    }

    public abstract class AWTWindow implements Windeye {
	public final java.awt.Frame frame;
	private final Collection<EventListener> callbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
	private boolean excl = false;
	private boolean focused;
	private DropHandler drop = null;
	private Sizing sizing = null;

	public AWTWindow() {
	    frame = new java.awt.Frame();
	}

	protected abstract Component panel();

	public AWTToolkit toolkit() {
	    return(AWTToolkit.this);
	}

	public void add(EventListener l) {
	    callbacks.add(l);
	}

	private void callback(Event ev) {
	    for(EventListener l : callbacks)
		l.event(ev);
	}

	public AWTWindow show(boolean show) {
	    awtrun(() -> frame.setVisible(show));
	    return(this);
	}

	public AWTWindow title(String title) {
	    frame.setTitle(title);
	    return(this);
	}

	public AWTWindow icon(BufferedImage icon) {
	    frame.setIconImage(icon);
	    try {
		Class<?> ctb = Class.forName("java.awt.Taskbar");
		Object tb = ctb.getMethod("getTaskbar").invoke(null);
		ctb.getMethod("setIconImage", java.awt.Image.class).invoke(tb, icon);
	    } catch(Exception e) {
	    }
	    return(this);
	}

	class EventQueue implements WindowListener, KeyListener, MouseListener, MouseWheelListener, MouseMotionListener, DropTargetListener {
	    private final List<InputEvent> events = new ArrayList<>();
	    private java.awt.event.MouseEvent mousemv;

	    public void windowClosing(WindowEvent e) {
		callback(new CloseRequest() {});
	    }
	    public void windowActivated(WindowEvent e) {
		focused = true;
	    }
	    public void windowDeactivated(WindowEvent e) {
		focused = false;
	    }
	    public void windowOpened(WindowEvent e) {}
	    public void windowClosed(WindowEvent e) {}
	    public void windowIconified(WindowEvent e) {}
	    public void windowDeiconified(WindowEvent e) {}

	    public synchronized void keyTyped(java.awt.event.KeyEvent e) {
		events.add(e);
	    }
	    public synchronized void keyPressed(java.awt.event.KeyEvent e) {
		events.add(e);
	    }
	    public synchronized void keyReleased(java.awt.event.KeyEvent e) {
		events.add(e);
	    }

	    public void mouseEntered(java.awt.event.MouseEvent e) {}
	    public void mouseExited(java.awt.event.MouseEvent e) {}
	    public void mouseClicked(java.awt.event.MouseEvent e) {}
	    public synchronized void mousePressed(java.awt.event.MouseEvent e) {
		events.add(e);
	    }
	    public synchronized void mouseReleased(java.awt.event.MouseEvent e) {
		events.add(e);
	    }
	    public synchronized void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
		events.add(e);
	    }
	    public synchronized void mouseDragged(java.awt.event.MouseEvent e) {
		mousemv = e;
	    }
	    public synchronized void mouseMoved(java.awt.event.MouseEvent e) {
		mousemv = e;
	    }

	    /* Xrad-and-drop events must be handled synchronously. */
	    private void drophover(DropTargetDragEvent ev) {
		if(drop == null) {
		    ev.rejectDrag();
		    return;
		}
		Coord c = Coord.of(ev.getLocation().x, ev.getLocation().y);
		class Event extends AWTClipboard.AWTDropEvent implements DropHandler.DropHoverEvent {
		    public Coord wndc() {return(c);}
		    protected int awtactions() {return(ev.getSourceActions());}
		    protected boolean supported(DataFlavor flavor) {return(ev.isDataFlavorSupported(flavor));}
		    public Clipboard.Contents contents() {return(mkcontents());}

		    protected <T> Promise<T> xfpromise(Class<T> typ, DataFlavor flavor) {
			throw(new IllegalStateException("Cannot transfer data before accepting drag."));
		    }
		}
		DropHandler.Action act = drop.drophover(new Event());
		if(act == null) {
		    ev.rejectDrag();
		} else {
		    Integer awt = AWTClipboard.AWTDropEvent.actmap.get(act);
		    if(awt == null)
			ev.rejectDrag();
		    else
			ev.acceptDrag(awt);
		}
	    }

	    private void dropped(DropTargetDropEvent ev) {
		if(drop == null) {
		    ev.rejectDrop();
		    return;
		}
		Coord c = Coord.of(ev.getLocation().x, ev.getLocation().y);
		class Event extends AWTClipboard.AWTDropEvent implements DropHandler.DroppedEvent {
		    DropHandler.Action act = null;

		    public Coord wndc() {return(c);}
		    protected int awtactions() {return(ev.getSourceActions());}
		    protected boolean supported(DataFlavor flavor) {return(ev.isDataFlavorSupported(flavor));}
		    public Clipboard.Contents contents() {return(mkcontents());}

		    protected <T> Promise<T> xfpromise(Class<T> typ, DataFlavor flavor) {
			return(Promise.of(ev::getTransferable)
			       .map(Utils.uncheck(xf -> {return(xf.getTransferData(flavor));}))
			       .map(typ::cast));
		    }

		    public void accept(DropHandler.Action act) {
			ev.acceptDrop(AWTClipboard.AWTDropEvent.actmap.get(act));
			this.act = act;
		    }
		}
		Event de = new Event();
		boolean acc = drop.dropped(de);
		if(acc) {
		    ev.dropComplete(true);
		} else {
		    if(de.act != null)
			ev.dropComplete(false);
		    else
			ev.rejectDrop();
		}
	    }

	    public void dragEnter(DropTargetDragEvent ev) {drophover(ev);}
	    public void dragOver(DropTargetDragEvent ev) {drophover(ev);}
	    public void dropActionChanged(DropTargetDragEvent ev) {drophover(ev);}
	    public void dragExit(DropTargetEvent ev) {}
	    public void drop(DropTargetDropEvent ev) {dropped(ev);}

	    void register() {
		frame.addWindowListener(this);
		panel().addKeyListener(this);
		panel().addMouseListener(this);
		panel().addMouseWheelListener(this);
		panel().addMouseMotionListener(this);
		new java.awt.dnd.DropTarget(panel(), this);
	    }

	    private java.awt.event.KeyEvent lastpress = null;
	    void process() {
		List<InputEvent> batch;
		java.awt.event.MouseEvent mousemv;
		synchronized(this) {
		    mousemv = this.mousemv;
		    this.mousemv = null;
		    batch = new ArrayList<>(events);
		    events.clear();
		}
		if(mousemv != null) {
		    callback(new AWTMouseMoveEvent(mousemv));
		}
		for(int i = 0; i < batch.size(); i++) {
		    InputEvent ev = batch.get(i);
		    InputEvent ne = (i < batch.size() - 1) ? batch.get(i + 1) : null;
		    if(ev instanceof java.awt.event.MouseEvent) {
			java.awt.event.MouseEvent me = (java.awt.event.MouseEvent)ev;
			if(ev.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED) {
			    callback(new AWTMouseDownEvent(me));
			} else if(ev.getID() == java.awt.event.MouseEvent.MOUSE_RELEASED) {
			    callback(new AWTMouseUpEvent(me));
			} else if(ev instanceof java.awt.event.MouseWheelEvent) {
			    callback(new AWTMouseWheelEvent((java.awt.event.MouseWheelEvent)me));
			}
		    } else if(ev instanceof java.awt.event.KeyEvent) {
			java.awt.event.KeyEvent ke = (java.awt.event.KeyEvent)ev;
			if(ke.getID() == java.awt.event.KeyEvent.KEY_PRESSED) {
			    lastpress = ke;
			    if(ne instanceof java.awt.event.KeyEvent) {
				/* This is an extension of the below hack
				 * to handle dead keys (on Windows). It's
				 * extremely ugly and error-prone and
				 * should be dealt with, but I've no idea
				 * what the alternative would be.*/
				java.awt.event.KeyEvent nke = (java.awt.event.KeyEvent)ne;
				if((nke.getID() == java.awt.event.KeyEvent.KEY_TYPED) && (nke.getWhen() == ke.getWhen())) {
				    ke.setKeyChar(nke.getKeyChar());
				    i++;
				}
			    }
			    callback(new AWTKeyDownEvent(ke));
			} else if(ke.getID() == java.awt.event.KeyEvent.KEY_RELEASED) {
			    callback(new AWTKeyUpEvent(ke));
			} else if(ke.getID() == java.awt.event.KeyEvent.KEY_TYPED) {
			    if((lastpress != null) && (lastpress.getKeyChar() == ke.getKeyChar())) {
				/* Squelch this event. It certainly is an
				 * ugly hack, but I just haven't found any
				 * other way to disambiguate these
				 * duplicate events. Also, apparently
				 * getWhen() cannot be completely trusted
				 * to have the same value for a
				 * KEY_PRESSED and corresponding KEY_TYPED
				 * event.*/
			    } else {
				callback(new AWTKeyDownEvent(ke));
			    }
			}
		    }
		}
	    }
	}

	public AWTWindow cursor(Cursor cursor) {
	    if(cursor == null) {
		panel().setCursor(null);
		return(this);
	    }
	    if(cursor instanceof Cursor.Std) {
		panel().setCursor(getsyscurs((Cursor.Std)cursor));
	    } else {
		panel().setCursor(((AWTCursor)cursor).bk);
	    }
	    return(this);
	}

	public AWTWindow sizing(Sizing info) {
	    awtrun(() -> {
		if(info.fixsize == null) {
		    frame.setResizable(true);
		    if(info.normsize != null)
			frame.setSize(info.normsize.x, info.normsize.y);
		    frame.setMinimumSize((info.minsize == null) ? null : new java.awt.Dimension(info.minsize.x, info.minsize.y));
		    frame.setMaximumSize((info.maxsize == null) ? null : new java.awt.Dimension(info.maxsize.x, info.maxsize.y));
		} else {
		    frame.setResizable(false);
		    frame.setSize(info.fixsize.x, info.fixsize.y);
		    frame.setMinimumSize(null);
		    frame.setMaximumSize(null);
		}
	    });
	    sizing = info;
	    return(this);
	}

	private void setfs() {
	    if(excl)
		return;
	    awtrun(() -> {
		GraphicsDevice dev = frame.getGraphicsConfiguration().getDevice();
		frame.setVisible(false);
		frame.dispose();
		frame.setUndecorated(true);
		frame.setVisible(true);
		dev.setFullScreenWindow(frame);
		frame.pack();
	    });
	    excl = true;
	}

	private void setwnd() {
	    if(!excl)
		return;
	    awtrun(() -> {
		GraphicsDevice dev = frame.getGraphicsConfiguration().getDevice();
		dev.setFullScreenWindow(null);
		frame.setVisible(false);
		frame.dispose();
		frame.setUndecorated(false);
		frame.setVisible(true);
		if(sizing != null)
		    sizing(sizing);
		frame.pack();
	    });
	    excl = false;
	}

	public AWTWindow state(State st) {
	    switch(st) {
	    case EXCLUSIVE:
		setfs();
		break;
	    case MINIMIZED:
		setwnd();
		frame.setExtendedState(java.awt.Frame.ICONIFIED);
		break;
	    case NORMAL:
		setwnd();
		frame.setExtendedState(java.awt.Frame.NORMAL);
		break;
	    case MAXIMIZED:
		setwnd();
		frame.setExtendedState(frame.getExtendedState() | java.awt.Frame.MAXIMIZED_BOTH);
		break;
	    }
	    return(this);
	}

	public Coord size() {
	    java.awt.Dimension d = panel().getSize();
	    return(Coord.of(d.width, d.height));
	}

	public State state() {
	    if(excl)
		return(State.EXCLUSIVE);
	    if((frame.getExtendedState() & java.awt.Frame.ICONIFIED) != 0)
		return(State.MINIMIZED);
	    if((frame.getExtendedState() & java.awt.Frame.MAXIMIZED_BOTH) != 0)
		return(State.MAXIMIZED);
	    return(State.NORMAL);
	}

	public boolean focused() {
	    return(focused);
	}

	public Clipboard clipboard(Object id) {
	    java.awt.datatransfer.Clipboard c = null;
	    if(id == Clipboard.Std.PRIMARY)
		c = java.awt.Toolkit.getDefaultToolkit().getSystemSelection();
	    else if(id == Clipboard.Std.CLIPBOARD)
		c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	    return((c != null) ? new AWTClipboard(c) : Clipboard.nil);
	}

	public AWTWindow drophandler(DropHandler drop) {
	    this.drop = drop;
	    return(this);
	}

	public void dispose() {
	    frame.dispose();
	}
    }

    public static class AWTPicker implements FilePicker {
	public final Mode mode;
	public final Component parent;
	private javax.swing.filechooser.FileFilter filter = null;
	private boolean shown = false;
	private Path result;

	public AWTPicker(Mode mode, Component parent) {
	    this.mode = mode;
	    this.parent = parent;
	}

	public void filter(String desc, String... exts) {
	    if(shown)
		throw(new IllegalStateException());
	    filter = new FileNameExtensionFilter(desc, exts);
	}

	public Promise<Path> show() {
	    if(shown)
		throw(new IllegalStateException());
	    return(awtpromise(() -> {
		JFileChooser dialog = new JFileChooser();
		if(filter != null)
		    dialog.setFileFilter(filter);
		int res;
		if(mode == Mode.OPEN)
		    res = dialog.showOpenDialog(parent);
		else
		    res = dialog.showSaveDialog(parent);
		if(res == JFileChooser.APPROVE_OPTION)
		    return(dialog.getSelectedFile().toPath());
		return(null);
	    }));
	}
    }

    public FilePicker.Factory picker() {
	return((mode, wnd) -> {
	    Component parent = null;
	    if(wnd != null)
		parent = ((AWTWindow)wnd).frame;
	    return(new AWTPicker(mode, parent));
	});
    }

    public void browse(java.net.URI location) throws IOException {
	if(!Desktop.isDesktopSupported())
	    throw(new IOException("Java Desktop interface is unavailable."));
	Desktop dt = Desktop.getDesktop();
	if(!dt.isSupported(Desktop.Action.BROWSE))
	    throw(new IOException("Web browser is unsupported."));
	dt.browse(location);
    }

    public void dispose() {
    }

    public static class AWTMonitor implements Monitor {
	public final GraphicsDevice dev;

	public AWTMonitor(GraphicsDevice dev) {
	    this.dev = dev;
	}

	public Coord resolution() {
	    DisplayMode mode = dev.getDisplayMode();
	    return(Coord.of(mode.getWidth(), mode.getHeight()));
	}

	public int refresh() {
	    return(dev.getDisplayMode().getRefreshRate());
	}

	public double userdpi() {
	    return(java.awt.Toolkit.getDefaultToolkit().getScreenResolution());
	}

	public String toString() {
	    return(String.format("#<awt-monitor %s %s %.2fdpi>", dev.toString(), resolution(), userdpi()));
	}
    }

    public Collection<Monitor> monitors() {
	Collection<Monitor> ret = new ArrayList<>();
	for(GraphicsDevice dev : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
	    ret.add(new AWTMonitor(dev));
	return(ret);
    }

    public static class AWTCursor implements Cursor {
	public final java.awt.Cursor bk;

	public AWTCursor(java.awt.Cursor bk) {
	    this.bk = bk;
	}

	public void dispose() {}
    }

    public Cursor.Caps cursorcaps() {
	java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
	if(tk.getMaximumCursorColors() < 256)
	    return(null);
	java.awt.Dimension cd = tk.getBestCursorSize(512, 512);
	return(new Cursor.Caps((int)Math.min(cd.getWidth(), cd.getHeight()), 0));
    }

    public AWTCursor makecursor(BufferedImage img, Coord hs) {
	java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
	java.awt.Dimension cd = tk.getBestCursorSize(img.getWidth(), img.getHeight());
	BufferedImage buf = TexI.mkbuf(new Coord((int)cd.getWidth(), (int)cd.getHeight()));
	java.awt.Graphics g = buf.getGraphics();
	g.drawImage(img, 0, 0, null);
	g.dispose();
	return(new AWTCursor(tk.createCustomCursor(buf, new java.awt.Point(hs.x, hs.y), "")));
    }

    public static final Map<Integer, Key.Std> stdsyms = Utils.<Integer, Key.Std>map()
	.put(VK_ENTER, Key.Std.ENTER)
	.put(VK_BACK_SPACE, Key.Std.BACKSPACE)
	.put(VK_TAB, Key.Std.TAB)
	.put(VK_CANCEL, Key.Std.CANCEL)
	.put(VK_CLEAR, Key.Std.CLEAR)
	.put(VK_SHIFT, Key.Std.SHIFT)
	.put(VK_CONTROL, Key.Std.CONTROL)
	.put(VK_ALT, Key.Std.ALT)
	.put(VK_PAUSE, Key.Std.PAUSE)
	.put(VK_CAPS_LOCK, Key.Std.CAPSLOCK)
	.put(VK_ESCAPE, Key.Std.ESCAPE)
	.put(VK_SPACE, Key.Std.SPACE)
	.put(VK_PAGE_UP, Key.Std.PAGEUP)
	.put(VK_PAGE_DOWN, Key.Std.PAGEDOWN)
	.put(VK_END, Key.Std.END)
	.put(VK_HOME, Key.Std.HOME)
	.put(VK_LEFT, Key.Std.LEFT)
	.put(VK_UP, Key.Std.UP)
	.put(VK_RIGHT, Key.Std.RIGHT)
	.put(VK_DOWN, Key.Std.DOWN)
	.put(VK_COMMA, Key.Std.COMMA)
	.put(VK_MINUS, Key.Std.MINUS)
	.put(VK_PERIOD, Key.Std.PERIOD)
	.put(VK_SLASH, Key.Std.SLASH)
	.put(VK_SEMICOLON, Key.Std.SEMICOLON)
	.put(VK_EQUALS, Key.Std.EQUALS)
	.put(VK_OPEN_BRACKET, Key.Std.LEFTBRACKET)
	.put(VK_CLOSE_BRACKET, Key.Std.RIGHTBRACKET)
	.put(VK_BACK_SLASH, Key.Std.BACKSLASH)
	.put(VK_DELETE, Key.Std.DELETE)
	.put(VK_NUM_LOCK, Key.Std.NUMLOCK)
	.put(VK_SCROLL_LOCK, Key.Std.SCROLLLOCK)
	.put(VK_PRINTSCREEN, Key.Std.PRINTSCREEN)
	.put(VK_INSERT, Key.Std.INSERT)
	.put(VK_HELP, Key.Std.HELP)
	.put(VK_META, Key.Std.META)
	.put(VK_BACK_QUOTE, Key.Std.BACKQUOTE)
	.put(VK_QUOTE, Key.Std.QUOTE)
 
	.put(VK_AMPERSAND, Key.Std.AMPERSAND)
	.put(VK_ASTERISK, Key.Std.ASTERISK)
	.put(VK_QUOTEDBL, Key.Std.DBLQUOTE)
	.put(VK_LESS, Key.Std.LT)
	.put(VK_GREATER, Key.Std.GT)
	.put(VK_BRACELEFT, Key.Std.LEFTBRACE)
	.put(VK_BRACERIGHT, Key.Std.RIGHTBRACE)
	.put(VK_AT, Key.Std.AT)
	.put(VK_COLON, Key.Std.COLON)
	.put(VK_CIRCUMFLEX, Key.Std.CIRCUMFLEX)
	.put(VK_DOLLAR, Key.Std.DOLLAR)
	.put(VK_EURO_SIGN, Key.Std.EUROSIGN)
	.put(VK_EXCLAMATION_MARK, Key.Std.EXCL)
	.put(VK_INVERTED_EXCLAMATION_MARK, Key.Std.INVEXCL)
	.put(VK_LEFT_PARENTHESIS, Key.Std.LEFTPAREN)
	.put(VK_RIGHT_PARENTHESIS, Key.Std.RIGHTPAREN)
	.put(VK_NUMBER_SIGN, Key.Std.NUMBERSIGN)
	.put(VK_PLUS, Key.Std.PLUS)
	.put(VK_UNDERSCORE, Key.Std.UNDERSCORE)
	.put(VK_WINDOWS, Key.Std.WINDOWS)
	.put(VK_CONTEXT_MENU, Key.Std.MENU)
	.put(VK_FINAL, Key.Std.FINAL)
	.put(VK_CONVERT, Key.Std.CONVERT)
	.put(VK_NONCONVERT, Key.Std.NONCONVERT)
	.put(VK_ACCEPT, Key.Std.ACCEPT)
	.put(VK_MODECHANGE, Key.Std.MODECHANGE)
	.put(VK_KANA, Key.Std.KANA)
	.put(VK_KANJI, Key.Std.KANJI)
	.put(VK_ALPHANUMERIC, Key.Std.ALNUM)
	.put(VK_KATAKANA, Key.Std.KATAKANA)
	.put(VK_HIRAGANA, Key.Std.HIRAGANA)
	.put(VK_FULL_WIDTH, Key.Std.FULLWIDTH)
	.put(VK_HALF_WIDTH, Key.Std.HALFWIDTH)
	.put(VK_ROMAN_CHARACTERS, Key.Std.ROMAN)
	.put(VK_ALL_CANDIDATES, Key.Std.ALLCAND)
	.put(VK_PREVIOUS_CANDIDATE, Key.Std.PREVCAND)
	.put(VK_KANA_LOCK, Key.Std.KANALOCK)
	.put(VK_INPUT_METHOD_ON_OFF, Key.Std.INPUTMETH)
	.put(VK_CUT, Key.Std.CUT)
	.put(VK_COPY, Key.Std.COPY)
	.put(VK_PASTE, Key.Std.PASTE)
	.put(VK_UNDO, Key.Std.UNDO)
	.put(VK_AGAIN, Key.Std.AGAIN)
	.put(VK_FIND, Key.Std.FIND)
	.put(VK_PROPS, Key.Std.PROPS)
	.put(VK_STOP, Key.Std.STOP)
	.put(VK_COMPOSE, Key.Std.COMPOSE)
	.put(VK_ALT_GRAPH, Key.Std.ALTGR)
	.put(VK_BEGIN, Key.Std.BEGIN)

	.put(VK_DEAD_GRAVE, Key.Std.DEADGRAVE)
	.put(VK_DEAD_ACUTE, Key.Std.DEADACUTE)
	.put(VK_DEAD_CIRCUMFLEX, Key.Std.DEADCIRCUMFLEX)
	.put(VK_DEAD_TILDE, Key.Std.DEADTILDE)
	.put(VK_DEAD_MACRON, Key.Std.DEADMACRON)
	.put(VK_DEAD_BREVE, Key.Std.DEADBREVE)
	.put(VK_DEAD_ABOVEDOT, Key.Std.DEADABOVEDOT)
	.put(VK_DEAD_DIAERESIS, Key.Std.DEADDIAERESIS)
	.put(VK_DEAD_ABOVERING, Key.Std.DEADABOVERING)
	.put(VK_DEAD_DOUBLEACUTE, Key.Std.DEADDOUBLEACUTE)
	.put(VK_DEAD_CARON, Key.Std.DEADCARON)
	.put(VK_DEAD_CEDILLA, Key.Std.DEADCEDILLA)
	.put(VK_DEAD_OGONEK, Key.Std.DEADOGONEK)
	.put(VK_DEAD_IOTA, Key.Std.DEADIOTA)
	.put(VK_DEAD_VOICED_SOUND, Key.Std.DEADVOICED)
	.put(VK_DEAD_SEMIVOICED_SOUND, Key.Std.DEADSEMIVOICED)
 
	.put(VK_0, Key.Std.N0)
	.put(VK_1, Key.Std.N1)
	.put(VK_2, Key.Std.N2)
	.put(VK_3, Key.Std.N3)
	.put(VK_4, Key.Std.N4)
	.put(VK_5, Key.Std.N5)
	.put(VK_6, Key.Std.N6)
	.put(VK_7, Key.Std.N7)
	.put(VK_8, Key.Std.N8)
	.put(VK_9, Key.Std.N9)
 
	.put(VK_A, Key.Std.A)
	.put(VK_B, Key.Std.B)
	.put(VK_C, Key.Std.C)
	.put(VK_D, Key.Std.D)
	.put(VK_E, Key.Std.E)
	.put(VK_F, Key.Std.F)
	.put(VK_G, Key.Std.G)
	.put(VK_H, Key.Std.H)
	.put(VK_I, Key.Std.I)
	.put(VK_J, Key.Std.J)
	.put(VK_K, Key.Std.K)
	.put(VK_L, Key.Std.L)
	.put(VK_M, Key.Std.M)
	.put(VK_N, Key.Std.N)
	.put(VK_O, Key.Std.O)
	.put(VK_P, Key.Std.P)
	.put(VK_Q, Key.Std.Q)
	.put(VK_R, Key.Std.R)
	.put(VK_S, Key.Std.S)
	.put(VK_T, Key.Std.T)
	.put(VK_U, Key.Std.U)
	.put(VK_V, Key.Std.V)
	.put(VK_W, Key.Std.W)
	.put(VK_X, Key.Std.X)
	.put(VK_Y, Key.Std.Y)
	.put(VK_Z, Key.Std.Z)
 
	.put(VK_F1, Key.Std.F1)
	.put(VK_F2, Key.Std.F2)
	.put(VK_F3, Key.Std.F3)
	.put(VK_F4, Key.Std.F4)
	.put(VK_F5, Key.Std.F5)
	.put(VK_F6, Key.Std.F6)
	.put(VK_F7, Key.Std.F7)
	.put(VK_F8, Key.Std.F8)
	.put(VK_F9, Key.Std.F9)
	.put(VK_F10, Key.Std.F10)
	.put(VK_F11, Key.Std.F11)
	.put(VK_F12, Key.Std.F12)
	.put(VK_F13, Key.Std.F13)
	.put(VK_F14, Key.Std.F14)
	.put(VK_F15, Key.Std.F15)
	.put(VK_F16, Key.Std.F16)
	.put(VK_F17, Key.Std.F17)
	.put(VK_F18, Key.Std.F18)
	.put(VK_F19, Key.Std.F19)
	.put(VK_F20, Key.Std.F20)
	.put(VK_F21, Key.Std.F21)
	.put(VK_F22, Key.Std.F22)
	.put(VK_F23, Key.Std.F23)
	.put(VK_F24, Key.Std.F24)
 
	.put(VK_NUMPAD0, Key.Std.NP0)
	.put(VK_NUMPAD1, Key.Std.NP1)
	.put(VK_NUMPAD2, Key.Std.NP2)
	.put(VK_NUMPAD3, Key.Std.NP3)
	.put(VK_NUMPAD4, Key.Std.NP4)
	.put(VK_NUMPAD5, Key.Std.NP5)
	.put(VK_NUMPAD6, Key.Std.NP6)
	.put(VK_NUMPAD7, Key.Std.NP7)
	.put(VK_NUMPAD8, Key.Std.NP8)
	.put(VK_NUMPAD9, Key.Std.NP9)
	.put(VK_DIVIDE, Key.Std.NP_DIV)
	.put(VK_MULTIPLY, Key.Std.NP_MUL)
	.put(VK_SUBTRACT, Key.Std.NP_SUB)
	.put(VK_ADD, Key.Std.NP_ADD)
	.put(VK_SEPARATOR, Key.Std.NP_SEP)
	.put(VK_DECIMAL, Key.Std.NP_DEC)
	.put(VK_KP_UP, Key.Std.NP_UP)
	.put(VK_KP_DOWN, Key.Std.NP_DOWN)
	.put(VK_KP_LEFT, Key.Std.NP_LEFT)
	.put(VK_KP_RIGHT, Key.Std.NP_RIGHT)
	.map();
}

class DebugEventQueue extends EventQueue {
    private final Map<AWTEvent, Record> posted = new IdentityHashMap<>();
    private StringBuilder buf = new StringBuilder();

    public static final class Record {
	public static int nextseq = 0;
	public final int seq;
	public final double st;

	public Record() {
	    synchronized(Record.class) {
		seq = nextseq++;
		st = Utils.rtime();
	    }
	}
    }

    public DebugEventQueue() {
	Thread th = new Thread(this::ioloop, "DebugEventQueue flusher");
	th.setDaemon(true);
	th.start();
    }

    private void ioloop() {
	try {
	    try(Writer out = Files.newBufferedWriter(Utils.path("edt-dump"))) {
		while(true) {
		    StringBuilder buf;
		    synchronized(this) {
			while(this.buf.length() == 0)
			    wait();
			buf = this.buf;
			this.buf = new StringBuilder();
		    }
		    out.write(buf.toString());
		    out.flush();
		}
	    }
	} catch(InterruptedException e) {
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }

    private void printf(String fmt, Object... args) {
	synchronized(this) {
	    buf.append(String.format(fmt, args));
	    notifyAll();
	}
    }

    protected void dispatchEvent(AWTEvent ev) {
	Record rec = posted.remove(ev);
	double st = Utils.rtime();
	super.dispatchEvent(ev);
	double et = Utils.rtime();
	long el = (ev instanceof InputEvent) ? (System.currentTimeMillis() - ((InputEvent)ev).getWhen()) : -1;
	if(rec != null)
	    printf("%8.2f: dispatch %8d (%5d, %8.2f + %.2f ms): %s\n", st, rec.seq, el, (et - st) * 1000, (st - rec.st) * 1000, ev);
	else
	    printf("%8.2f: dispatch      N/A (%5d, %8.2f ms): %s\n", st, el, (et - st) * 1000, ev);
    }

    public void postEvent(AWTEvent ev) {
	Record rec = new Record();
	synchronized(posted) {
	    posted.put(ev, rec);
	}
	super.postEvent(ev);
	long el = (ev instanceof InputEvent) ? (System.currentTimeMillis() - ((InputEvent)ev).getWhen()) : -1;
	printf("%8.2f: posted   %8d (%5d ms): %s\n", Utils.rtime(), rec.seq, el, ev);
    }
}
