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
import java.awt.image.*;
import java.nio.*;
import haven.*;
import haven.iosys.*;
import haven.render.*;
import haven.render.gl.*;
import haven.render.jogl.*;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.opengl.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.nativewindow.util.*;
import com.jogamp.opengl.GL;
import static com.jogamp.newt.event.KeyEvent.*;

/* XXX: Unfortunately, there's so much that NEWT doesn't implement
 * that it's hard to ever consider this toolkit complete, I
 * think. It's arguably still nice to have a fallback, though. */
@Toolkit.Available(name = "newt")
public class NEWTContext implements Providers.Factory<Toolkit> {
    public NEWTToolkit open(String... args) {
	try {
	    return(new NEWTToolkit());
	} catch(LinkageError e) {
	    throw(new Unavailable("JOGL libraries not available", e));
	}
    }

    /* Deprioritize heavily since there's so much unimplemented. */
    public int priority() {return(-100);}
    public boolean autouse() {return(false);}

    private static final NEWTContext instance = new NEWTContext();
    public static Providers.Factory<Toolkit> get() {return(instance);}

    public static class ProfileException extends Environment.UnavailableException {
	public final String availability;

	public ProfileException(Throwable cause) {
	    super("No suitable OpenGL profile is available", cause);
	    String a;
	    try {
		a = GLProfile.glAvailabilityToString();
	    } catch(Throwable t) {
		a = String.valueOf(t);
	    }
	    this.availability = a;
	}
    }

    public static class NEWTToolkit implements Toolkit {
	public final Display dpy;
	public final Screen screen;
	public final GLCapabilities caps;

	public NEWTToolkit() {
	    boolean done = false;
	    try {
		dpy = NewtFactory.createDisplay(null);
		dpy.createNative();
		screen = NewtFactory.createScreen(dpy, 0);
		screen.createNative();
		GLProfile prof;
		try {
		    prof = GLProfile.getMaxProgrammableCore(true);
		} catch(com.jogamp.opengl.GLException e) {
		    try {
			/* If not core, let GLEnvironment handle that. */
			prof = GLProfile.getDefault();
		    } catch(com.jogamp.opengl.GLException e2) {
			e2.addSuppressed(e);
			throw(new ProfileException(e2));
		    }
		}
		caps = new GLCapabilities(prof);
		caps.setDoubleBuffered(true);
		caps.setAlphaBits(8);
		caps.setRedBits(8);
		caps.setGreenBits(8);
		caps.setBlueBits(8);
		done = true;
	    } finally {
		if(!done)
		    dispose();
	    }
	}

	public static Set<Key.Mod> xlmods(InputEvent ev) {
	    Set<Key.Mod> ret = EnumSet.noneOf(Key.Mod.class);
	    int fl = ev.getModifiers();
	    if((fl & InputEvent.SHIFT_MASK)     != 0) ret.add(Key.Mod.SHIFT);
	    if((fl & InputEvent.CTRL_MASK)      != 0) ret.add(Key.Mod.CONTROL);
	    if((fl & InputEvent.META_MASK)      != 0) ret.add(Key.Mod.META);
	    if((fl & InputEvent.ALT_MASK)       != 0) ret.add(Key.Mod.ALT);
	    if((fl & InputEvent.ALT_GRAPH_MASK) != 0) ret.add(Key.Mod.ALTGR);
	    return(ret);
	}

	public static class NEWTMouseEvent {
	    public final com.jogamp.newt.event.MouseEvent newt;

	    public NEWTMouseEvent(com.jogamp.newt.event.MouseEvent newt) {
		this.newt = newt;
	    }

	    public Coord wndc() {return(Coord.of(newt.getX(), newt.getY()));}
	    public Set<MouseBtn> held() {return(Collections.emptySet());}
	    public Set<Key.Mod> mods() {return(xlmods(newt));}
	    public MouseBtn button() {
		switch(newt.getButton()) {
		case 1: return(MouseBtn.Std.LEFT);
		case 2: return(MouseBtn.Std.MIDDLE);
		case 3: return(MouseBtn.Std.RIGHT);
		}
		return(null);
	    }
	}
	public static class NEWTMouseDownEvent extends NEWTMouseEvent implements MouseDownEvent {
	    public NEWTMouseDownEvent(com.jogamp.newt.event.MouseEvent newt) {super(newt);}
	}
	public static class NEWTMouseUpEvent extends NEWTMouseEvent implements MouseUpEvent {
	    public NEWTMouseUpEvent(com.jogamp.newt.event.MouseEvent newt) {super(newt);}
	}
	public static class NEWTMouseMoveEvent extends NEWTMouseEvent implements MouseMoveEvent {
	    public NEWTMouseMoveEvent(com.jogamp.newt.event.MouseEvent newt) {super(newt);}
	}
	public static class NEWTMouseWheelEvent extends NEWTMouseEvent implements MouseWheelEvent {
	    public final Axis axis;
	    public final int amount;
	    public final double subamount;

	    public NEWTMouseWheelEvent(com.jogamp.newt.event.MouseEvent newt, Axis axis, int amount, double subamount) {
		super(newt);
		this.axis = axis;
		this.amount = amount;
		this.subamount = subamount;
	    }

	    public Axis axis() {return(axis);}
	    public int amount() {return(amount);}
	    public double subamount() {return(subamount);}
	}

	public static class NEWTSym implements Key.Sym {
	    public final int code;

	    public NEWTSym(int code) {
		this.code = code;
	    }

	    public String id() {return(("newt:" + code).intern());}
	    public String nm() {return(String.format("%X", code));}


	    public boolean equals(NEWTSym that) {return(this.code == that.code);}
	    public boolean equals(Object x) {return((x instanceof NEWTSym) && equals((NEWTSym)x));}
	    public int hashCode() {return(code);}
	}

	public static class NEWTKey implements Key {
	    public final int code, sym;
	    public final char ch;
	    public final Key.Std cstd, sstd;
	    public final NEWTSym nsym;

	    public NEWTKey(com.jogamp.newt.event.KeyEvent ev) {
		this.code = ev.getKeyCode()   & 0xffff;
		this.sym  = ev.getKeySymbol() & 0xffff;
		this.ch   = ev.getKeyChar();
		this.cstd = stdsyms.get(ev.getKeyCode());
		this.sstd = stdsyms.get(ev.getKeySymbol());
		this.nsym = new NEWTSym(code);
	    }

	    public String id() {
		return(("newt:" + code).intern());
	    }

	    public Sym primary() {
		if(sstd != null)
		    return(sstd);
		if(cstd != null)
		    return(cstd);
		return(nsym);
	    }

	    public Sym primary(Collection<? extends Sym> of) {
		if((sstd != null) && of.contains(sstd))
		    return(sstd);
		if((cstd != null) && of.contains(cstd))
		    return(cstd);
		if((nsym != null) && of.contains(nsym))
		    return(nsym);
		return(null);
	    }
	}

	public static class NEWTKeyEvent {
	    public final com.jogamp.newt.event.KeyEvent newt;
	    public final NEWTKey key;

	    public NEWTKeyEvent(com.jogamp.newt.event.KeyEvent newt) {
		this.newt = newt;
		this.key = new NEWTKey(newt);
	    }

	    public String string() {
		char c = newt.getKeyChar();
		return((c == 0) ? "" : Character.toString(c));
	    }
	    public Key key() {
		return(key);}
	    public Key.Sym sym() {
		if(key.sstd != null)
		    return(key.sstd);
		return(key.nsym);
	    }
	    public Set<Key.Mod> mods() {return(xlmods(newt));}
	}
	public static class NEWTKeyDownEvent extends NEWTKeyEvent implements KeyDownEvent {
	    public NEWTKeyDownEvent(com.jogamp.newt.event.KeyEvent newt) {super(newt);}
	}
	public static class NEWTKeyUpEvent extends NEWTKeyEvent implements KeyUpEvent {
	    public NEWTKeyUpEvent(com.jogamp.newt.event.KeyEvent newt) {super(newt);}
	}


	private static final Pipe.Op glfb = Pipe.Op.compose(new FragColor<>(FragColor.defcolor),
							    new DepthBuffer<>(DepthBuffer.defdepth));
	public class NEWTWindow implements Windeye {
	    public final GLWindow bk;
	    private final Collection<EventListener> callbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
	    private Coord size = Coord.z;
	    private JOGLEnvironment env;
	    private int cursi;
	    private boolean focused;

	    public NEWTWindow() {
		bk = GLWindow.create(screen, caps);
		bk.addGLEventListener(new GLHandler());
		EventHandler ev = new EventHandler();
		bk.addWindowListener(ev);
		bk.addMouseListener(ev);
		bk.addKeyListener(ev);
	    }

	    public NEWTToolkit toolkit() {
		return(NEWTToolkit.this);
	    }

	    public void add(EventListener l) {
		callbacks.add(l);
	    }

	    private void callback(Event ev) {
		for(EventListener l : callbacks)
		    l.event(ev);
	    }

	    class GLHandler implements GLEventListener {
		public void display(GLAutoDrawable d) {
		}

		public void init(GLAutoDrawable d) {
		    d.setAutoSwapBufferMode(false);
		}

		public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
		    size = Coord.of(w, h);
		}

		public void dispose(GLAutoDrawable d) {
		}
	    };

	    class EventHandler implements WindowListener, MouseListener, KeyListener {
		private float[] wheelacc = {0, 0, 0};

		public void windowDestroyNotify(WindowEvent ev) {
		    callback(new CloseRequest() {});
		}
		public void windowResized(WindowEvent ev) {}
		public void windowMoved(WindowEvent ev) {}
		public void windowDestroyed(WindowEvent ev) {}
		public void windowGainedFocus(WindowEvent ev) {focused = true;}
		public void windowLostFocus(WindowEvent ev) {focused = false;}
		public void windowRepaint(WindowUpdateEvent ev) {}

		public void mouseClicked(com.jogamp.newt.event.MouseEvent ev) {}
		public void mouseEntered(com.jogamp.newt.event.MouseEvent ev) {}
		public void mouseExited(com.jogamp.newt.event.MouseEvent ev) {}
		public void mousePressed(com.jogamp.newt.event.MouseEvent ev) {
		    callback(new NEWTMouseDownEvent(ev));
		}
		public void mouseReleased(com.jogamp.newt.event.MouseEvent ev) {
		    callback(new NEWTMouseUpEvent(ev));
		}
		public void mouseMoved(com.jogamp.newt.event.MouseEvent ev) {
		    callback(new NEWTMouseMoveEvent(ev));
		}
		public void mouseDragged(com.jogamp.newt.event.MouseEvent ev) {
		    callback(new NEWTMouseMoveEvent(ev));
		}
		public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent ev) {
		    float[] r = ev.getRotation();
		    float s = -ev.getRotationScale();
		    int[] ia = new int[3];
		    for(int i = 0; i < 3; i++) {
			wheelacc[i] += r[i] / s;
			ia[i] = (int)wheelacc[i];
			wheelacc[i] -= ia[i];
		    }
		    if(r[0] != 0)
			callback(new NEWTMouseWheelEvent(ev, MouseWheelEvent.Axis.VERT, ia[0], r[0] / s));
		    if(r[1] != 0)
			callback(new NEWTMouseWheelEvent(ev, MouseWheelEvent.Axis.VERT, ia[1], r[1] / s));
		}

		public void keyPressed(com.jogamp.newt.event.KeyEvent ev) {
		    callback(new NEWTKeyDownEvent(ev));
		}
		public void keyReleased(com.jogamp.newt.event.KeyEvent ev) {
		    callback(new NEWTKeyUpEvent(ev));
		}
	    }

	    public Coord size() {
		return(size);
	    }

	    public State state() {
		if(bk.isFullscreen())
		    return(State.EXCLUSIVE);
		if(bk.isMaximizedVert() || bk.isMaximizedHorz())
		    return(State.MAXIMIZED);
		return(State.NORMAL);
	    }

	    public boolean focused() {
		return(focused);
	    }

	    public NEWTWindow show(boolean show) {
		bk.setVisible(show);
		return(this);
	    }

	    public NEWTWindow title(String title) {
		bk.setTitle(title);
		return(this);
	    }

	    public NEWTWindow icon(BufferedImage icon) {
		return(this);
	    }

	    public NEWTWindow state(State st) {
		switch(st) {
		case MINIMIZED: break;
		case NORMAL:
		    bk.setFullscreen(false);
		    bk.setMaximized(false, false);
		    break;
		case MAXIMIZED:
		    bk.setFullscreen(false);
		    bk.setMaximized(true, true);
		    break;
		case EXCLUSIVE:
		    bk.setFullscreen(true);
		    break;
		}
		return(this);
	    }

	    public NEWTWindow sizing(Sizing inf) {
		if(inf.fixsize != null) {
		    bk.setResizable(false);
		    bk.setSize(inf.fixsize.x, inf.fixsize.y);
		} else {
		    bk.setResizable(true);
		    if((inf.normsize != null) && !(bk.isMaximizedVert() || bk.isMaximizedHorz()))
			bk.setSize(inf.normsize.x, inf.normsize.y);
		}
		return(this);
	    }

	    public NEWTWindow cursor(Cursor c) {
		if(c instanceof Cursor.Std) {
		    bk.setPointerIcon(null);
		    bk.setPointerVisible(c != Cursor.Std.NONE);
		} else {
		    bk.setPointerVisible(true);
		    bk.setPointerIcon(((NEWTCursor)c).nc);
		}
		return(this);
	    }

	    private void initgl(GL gl) {
		Collection<String> exts = Arrays.asList(gl.glGetString(GL.GL_EXTENSIONS).split(" "));
		GLCapabilitiesImmutable caps = bk.getChosenGLCapabilities();
		gl.setSwapInterval(cursi = 1);
		if(exts.contains("GL_ARB_multisample") && caps.getSampleBuffers()) {
		    /* Apparently, having sample buffers in the config enables
		     * multisampling by default on some systems. */
		    gl.glDisable(GL.GL_MULTISAMPLE);
		}
	    }

	    private boolean process(GLAutoDrawable d) {
		process(d.getGL());
		return(true);
	    }

	    private void process(GL gl) {
		GLContext ctx = gl.getContext();
		GLEnvironment env;
		Area shape = Area.sized(size);
		synchronized(this) {
		    if((this.env == null) || (this.env.ctx != ctx)) {
			if(this.env != null)
			    this.env.dispose();
			this.env = new JOGLEnvironment(gl, ctx, shape) {
				public void submit(Render cmd) {
				    super.submit(cmd);
				    bk.invoke(false, NEWTWindow.this::process);
				}
			    };
			notifyAll();
			initgl(gl);
		    }
		    env = this.env;
		    if(!env.shape().equals(shape))
			env.reshape(shape);
		}
		GL3 gl3 = gl.getGL3();
		if(false) {
		    System.err.println("\n-----\n\n");
		    gl3 = new TraceGL3(gl3, System.err);
		}
		if(false) {
		    gl3 = new DebugGL3(gl3);
		}
		env.process(new JOGLWrap(gl3));
	    }

	    public Environment env() {
		if(this.env == null) {
		    bk.invoke(false, this::process);
		    try {
			double st = Utils.rtime(), now = st;
			synchronized(this) {
			    while((this.env == null) && ((now - st) < 5)) {
				this.wait((int)Math.round(1000 * (5 - (now - st))));
				now = Utils.rtime();
			    }
			}
		    } catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		    }
		    if(this.env == null)
			throw(new RuntimeException("Did not get GL environment even after display"));
		}
		return(this.env);
	    }

	    public Pipe.Op fbstate() {
		return(glfb);
	    }

	    private void glswap(haven.render.gl.GL gl, int ival) {
		haven.render.gl.GLException.checkfor(gl, null);
		if(ival != cursi)
		    ((WrappedJOGL)gl).getGL().setSwapInterval(cursi = ival);
		bk.swapBuffers();
		haven.render.gl.GLException.checkfor(gl, null);
	    }

	    public void swapbuffers(Render buf, Object mode) {
		GLRender gbuf = (GLRender)buf;
		if(gbuf.env != this.env)
		    throw(new IllegalArgumentException());
		if(!(mode instanceof Boolean))
		    throw(new IllegalArgumentException());
		gbuf.submit(gl -> this.glswap(gl, ((Boolean)mode) ? 1 : 0));
	    }

	    public void dispose() {
		bk.destroy();
	    }
	}

	public NEWTWindow window() {
	    return(new NEWTWindow());
	}

	public Cursor.Caps cursorcaps() {
	    return(new Cursor.Caps(Integer.MAX_VALUE, 0));
	}

	public static class NEWTCursor implements Cursor {
	    public final Display.PointerIcon nc;

	    public NEWTCursor(Display.PointerIcon nc) {
		this.nc = nc;
	    }

	    public void dispose() {
		nc.destroy();
	    }
	}

	public Cursor makecursor(BufferedImage img, Coord hotspot) {
	    img = PUtils.coercergba(img, false);
	    int w = img.getWidth(), h = img.getHeight();
	    ByteBuffer pixels = ByteBuffer.allocate(w * h * 4);
	    Raster idat = img.getRaster();
	    for(int y = 0, o = 0; y < h; y++) {
		for(int x = 0; x < w; x++) {
		    for(int b = 0; b < 4; b++)
			pixels.put(o++, (byte)idat.getSample(x, y, b));
		}
	    }
	    PixelRectangle icon = new PixelRectangle.GenericPixelRect(PixelFormat.RGBA8888, new Dimension(w, h), 0, false, pixels);
	    return(new NEWTCursor(dpy.createPointerIcon(icon, hotspot.x, hotspot.y)));
	}

	public void dispose() {
	    if(dpy != null)
		dpy.destroy();
	}

	public String description() {
	    return(String.format("NEWT/JOGL, JOGL %s", JoglVersion.getInstance().getImplementationVersion()));
	}
    }

    public static final Map<Short, Key.Std> stdsyms = Utils.<Short, Key.Std>map()
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
	.put(VK_LEFT_BRACE, Key.Std.LEFTBRACE)
	.put(VK_RIGHT_BRACE, Key.Std.RIGHTBRACE)
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
	.put(VK_KANA_LOCK, Key.Std.KANA)
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
	.map();
}
