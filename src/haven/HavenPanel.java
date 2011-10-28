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

import java.awt.GraphicsConfiguration;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;

public class HavenPanel extends GLCanvas implements Runnable {
    UI ui;
    boolean inited = false, rdr = false;
    int w, h;
    long fd = 20, fps = 0;
    double idle = 0.0;
    Queue<InputEvent> events = new LinkedList<InputEvent>();
    private String cursmode = "tex";
    private Resource lastcursor = null;
    public Coord mousepos = new Coord(0, 0);
    public Profile prof = new Profile(300);
    private Profile.Frame curf = null;
    private static final GLCapabilities stdcaps;
    static {
	stdcaps = new GLCapabilities();
	stdcaps.setDoubleBuffered(true);
	stdcaps.setAlphaBits(8);
	stdcaps.setRedBits(8);
	stdcaps.setGreenBits(8);
	stdcaps.setBlueBits(8);
	stdcaps.setSampleBuffers(true);
	stdcaps.setNumSamples(4);
    }
    public static final GLState.Slot<GLState> global = new GLState.Slot<GLState>(GLState.Slot.Type.SYS, GLState.class);
    public static final GLState.Slot<GLState> proj2d = new GLState.Slot<GLState>(GLState.Slot.Type.SYS, GLState.class, global);
    private GLState gstate, rtstate, ostate;
    private GLState.Applier state = null;
    private GLConfig glconf = null;
    
    public HavenPanel(int w, int h, GLCapabilitiesChooser cc) {
	super(stdcaps, cc, null, null);
	setSize(this.w = w, this.h = h);
	newui(null);
	initgl();
	if(Toolkit.getDefaultToolkit().getMaximumCursorColors() >= 256)
	    cursmode = "awt";
	setCursor(Toolkit.getDefaultToolkit().createCustomCursor(TexI.mkbuf(new Coord(1, 1)), new java.awt.Point(), ""));
    }
    
    public HavenPanel(int w, int h) {
	this(w, h, null);
    }
    
    private void initgl() {
	final Thread caller = Thread.currentThread();
	final haven.error.ErrorHandler h = haven.error.ErrorHandler.find();
	addGLEventListener(new GLEventListener() {
		public void display(GLAutoDrawable d) {
		    GL gl = d.getGL();
		    if(inited && rdr)
			redraw(gl);
		    GLObject.disposeall(gl);
		}
			
		public void init(GLAutoDrawable d) {
		    GL gl = d.getGL();
		    glconf = GLConfig.fromgl(gl, d.getContext(), getChosenGLCapabilities());
		    ui.cons.add(glconf);
		    if(h != null) {
			h.lsetprop("gl.vendor", gl.glGetString(gl.GL_VENDOR));
			h.lsetprop("gl.version", gl.glGetString(gl.GL_VERSION));
			h.lsetprop("gl.renderer", gl.glGetString(gl.GL_RENDERER));
			h.lsetprop("gl.exts", Arrays.asList(gl.glGetString(gl.GL_EXTENSIONS).split(" ")));
			h.lsetprop("gl.caps", d.getChosenGLCapabilities().toString());
			h.lsetprop("gl.conf", glconf);
		    }
		    gstate = new GLState() {
			    public void apply(GOut g) {
				GL gl = g.gl;
				gl.glColor3f(1, 1, 1);
				gl.glPointSize(4);
				gl.setSwapInterval(1);
				gl.glEnable(GL.GL_BLEND);
				//gl.glEnable(GL.GL_LINE_SMOOTH);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				if(g.gc.havefsaa()) {
				    /* Apparently, having sample
				     * buffers in the config enables
				     * multisampling by default on
				     * some systems. */
				    g.gl.glDisable(GL.GL_MULTISAMPLE);
				}
				GOut.checkerr(gl);
			    }
			    public void unapply(GOut g) {
			    }
			    public void prep(Buffer buf) {
				buf.put(global, this);
			    }
			};
		}

		public void reshape(GLAutoDrawable d, final int x, final int y, final int w, final int h) {
		    ostate = new GLState() {
			    public void apply(GOut g) {
				GL gl = g.gl;
				g.st.matmode(GL.GL_PROJECTION);
				gl.glLoadIdentity();
				gl.glOrtho(0, w, h, 0, -1, 1);
			    }
			    public void unapply(GOut g) {
			    }
			    public void prep(Buffer buf) {
				buf.put(proj2d, this);
			    }
			};
		    rtstate = new GLState() {
			    public void apply(GOut g) {
				GL gl = g.gl;
				g.st.matmode(GL.GL_PROJECTION);
				gl.glLoadIdentity();
				gl.glOrtho(0, w, 0, h, -1, 1);
			    }
			    public void unapply(GOut g) {
			    }
			    public void prep(Buffer buf) {
				buf.put(proj2d, this);
			    }
			};
		    HavenPanel.this.w = w;
		    HavenPanel.this.h = h;
		}
		
		public void displayChanged(GLAutoDrawable d, boolean cp1, boolean cp2) {}
	    });
    }
	
    public void init() {
	setFocusTraversalKeysEnabled(false);
	newui(null);
	addKeyListener(new KeyAdapter() {
		public void keyTyped(KeyEvent e) {
		    synchronized(events) {
			events.add(e);
			events.notifyAll();
		    }
		}

		public void keyPressed(KeyEvent e) {
		    synchronized(events) {
			events.add(e);
			events.notifyAll();
		    }
		}
		public void keyReleased(KeyEvent e) {
		    synchronized(events) {
			events.add(e);
			events.notifyAll();
		    }
		}
	    });
	addMouseListener(new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
		    synchronized(events) {
			events.add(e);
			events.notifyAll();
		    }
		}

		public void mouseReleased(MouseEvent e) {
		    synchronized(events) {
			events.add(e);
			events.notifyAll();
		    }
		}
	    });
	addMouseMotionListener(new MouseMotionListener() {
		public void mouseDragged(MouseEvent e) {
		    synchronized(events) {
			events.add(e);
		    }
		}

		public void mouseMoved(MouseEvent e) {
		    synchronized(events) {
			events.add(e);
		    }
		}
	    });
	addMouseWheelListener(new MouseWheelListener() {
		public void mouseWheelMoved(MouseWheelEvent e) {
		    synchronized(events) {
			events.add(e);
			events.notifyAll();
		    }
		}
	    });
	inited = true;
    }
	
    UI newui(Session sess) {
	if(ui != null)
	    ui.destroy();
	ui = new UI(new Coord(w, h), sess);
	ui.root.gprof = prof;
	if(getParent() instanceof Console.Directory)
	    ui.cons.add((Console.Directory)getParent());
	if(glconf != null)
	    ui.cons.add(glconf);
	return(ui);
    }
    
    private static Cursor makeawtcurs(BufferedImage img, Coord hs) {
	java.awt.Dimension cd = Toolkit.getDefaultToolkit().getBestCursorSize(img.getWidth(), img.getHeight());
	BufferedImage buf = TexI.mkbuf(new Coord((int)cd.getWidth(), (int)cd.getHeight()));
	java.awt.Graphics g = buf.getGraphics();
	g.drawImage(img, 0, 0, null);
	g.dispose();
	return(Toolkit.getDefaultToolkit().createCustomCursor(buf, new java.awt.Point(hs.x, hs.y), ""));
    }
    
    void redraw(GL gl) {
	if((state == null) || (state.gl != gl))
	    state = new GLState.Applier(gl, glconf);
	GLState.Buffer ibuf = new GLState.Buffer(glconf);
	gstate.prep(ibuf);
	ostate.prep(ibuf);
	GOut g = new GOut(gl, getContext(), glconf, state, ibuf, new Coord(w, h));
	state.set(ibuf);

	g.state(rtstate);
	TexRT.renderall(g);
	if(curf != null)
	    curf.tick("texrt");

	g.state(ostate);
	g.apply();
	gl.glClearColor(0, 0, 0, 1);
	gl.glClear(GL.GL_COLOR_BUFFER_BIT);
	if(curf != null)
	    curf.tick("cls");
	synchronized(ui) {
	    ui.draw(g);
	}
	if(curf != null)
	    curf.tick("draw");

	if(Config.dbtext) {
	    int y = h - 20;
	    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "FPS: %d (%d%% idle)", fps, (int)(idle * 100.0));
	    Runtime rt = Runtime.getRuntime();
	    long free = rt.freeMemory(), total = rt.totalMemory();
	    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Mem: %,011d/%,011d/%,011d/%,011d", free, total - free, total, rt.maxMemory());
	    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "RT-current: %d", TexRT.current.get(gl).size());
	    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "GL progs: %d", g.st.numprogs());
	    GameUI gi = ui.root.findchild(GameUI.class);
	    if((gi != null) && (gi.map != null)) {
		try {
		    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "MV pos: %s (%s)", gi.map.getcc(), gi.map.camera);
		} catch(Loading e) {}
	    }
	    if(Resource.qdepth() > 0)
		FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "RQ depth: %d (%d)", Resource.qdepth(), Resource.numloaded());
	}
	Object tooltip;
        try {
	    tooltip = ui.root.tooltip(mousepos, ui.root);
	} catch(Loading e) {
	    tooltip = "...";
	}
	Tex tt = null;
	if(tooltip != null) {
	    if(tooltip instanceof Text) {
		tt = ((Text)tooltip).tex();
	    } else if(tooltip instanceof Tex) {
		tt = (Tex)tooltip;
	    } else if(tooltip instanceof Indir<?>) {
		Indir<?> t = (Indir<?>)tooltip;
		Object o = t.get();
		if(o instanceof Tex)
		    tt = (Tex)o;
	    } else if(tooltip instanceof String) {
		if(((String)tooltip).length() > 0)
		    tt = (Text.render((String)tooltip)).tex();
	    }
	}
	if(tt != null) {
	    Coord sz = tt.sz();
	    Coord pos = mousepos.add(sz.inv());
	    if(pos.x < 0)
		pos.x = 0;
	    if(pos.y < 0)
		pos.y = 0;
	    g.chcolor(244, 247, 21, 192);
	    g.rect(pos.add(-3, -3), sz.add(6, 6));
	    g.chcolor(35, 35, 35, 192);
	    g.frect(pos.add(-2, -2), sz.add(4, 4));
	    g.chcolor();
	    g.image(tt, pos);
	}
	ui.lasttip = tooltip;
	Resource curs = ui.root.getcurs(mousepos);
	if(!curs.loading) {
	    if(cursmode == "awt") {
		if(curs != lastcursor) {
		    try {
			setCursor(makeawtcurs(curs.layer(Resource.imgc).img, curs.layer(Resource.negc).cc));
			lastcursor = curs;
		    } catch(Exception e) {
			cursmode = "tex";
		    }
		}
	    } else if(cursmode == "tex") {
		Coord dc = mousepos.add(curs.layer(Resource.negc).cc.inv());
		g.image(curs.layer(Resource.imgc), dc);
	    }
	}
	state.clean();
    }
	
    void dispatch() {
	synchronized(events) {
	    InputEvent e = null;
	    while((e = events.poll()) != null) {
		if(e instanceof MouseEvent) {
		    MouseEvent me = (MouseEvent)e;
		    if(me.getID() == MouseEvent.MOUSE_PRESSED) {
			ui.mousedown(me, new Coord(me.getX(), me.getY()), me.getButton());
		    } else if(me.getID() == MouseEvent.MOUSE_RELEASED) {
			ui.mouseup(me, new Coord(me.getX(), me.getY()), me.getButton());
		    } else if(me.getID() == MouseEvent.MOUSE_MOVED || me.getID() == MouseEvent.MOUSE_DRAGGED) {
			mousepos = new Coord(me.getX(), me.getY());
			ui.mousemove(me, mousepos);
		    } else if(me instanceof MouseWheelEvent) {
			ui.mousewheel(me, new Coord(me.getX(), me.getY()), ((MouseWheelEvent)me).getWheelRotation());
		    }
		} else if(e instanceof KeyEvent) {
		    KeyEvent ke = (KeyEvent)e;
		    if(ke.getID() == KeyEvent.KEY_PRESSED) {
			ui.keydown(ke);
		    } else if(ke.getID() == KeyEvent.KEY_RELEASED) {
			ui.keyup(ke);
		    } else if(ke.getID() == KeyEvent.KEY_TYPED) {
			ui.type(ke);
		    }
		}
		ui.lastevent = System.currentTimeMillis();
	    }
	}
    }
	
    public void uglyjoglhack() throws InterruptedException {
	try {
	    rdr = true;
	    display();
	} catch(GLException e) {
	    if(e.getCause() instanceof InterruptedException) {
		throw((InterruptedException)e.getCause());
	    } else {
		e.printStackTrace();
		throw(e);
	    }
	} finally {
	    rdr = false;
	}
    }
	
    public void run() {
	try {
	    long now, fthen, then;
	    int frames = 0, waited = 0;
	    fthen = System.currentTimeMillis();
	    while(true) {
		UI ui = this.ui;
		then = System.currentTimeMillis();
		if(Config.profile)
		    curf = prof.new Frame();
		synchronized(ui) {
		    if(ui.sess != null) {
			ui.sess.glob.oc.ctick();
			ui.sess.glob.map.ctick();
		    }
		    dispatch();
		    ui.tick();
		    if((ui.root.sz.x != w) || (ui.root.sz.y != h))
			ui.root.resize(new Coord(w, h));
		}
		if(curf != null)
		    curf.tick("dsp");
		uglyjoglhack();
		ui.audio.cycle();
		if(curf != null)
		    curf.tick("aux");
		frames++;
		now = System.currentTimeMillis();
		if(now - then < fd) {
		    synchronized(events) {
			events.wait(fd - (now - then));
		    }
		    waited += System.currentTimeMillis() - now;
		}
		if(curf != null)
		    curf.tick("wait");
		if(now - fthen > 1000) {
		    fps = frames;
		    idle = ((double)waited) / ((double)(now - fthen));
		    frames = 0;
		    waited = 0;
		    fthen = now;
		}
		if(curf != null)
		    curf.fin();
		if(Thread.interrupted())
		    throw(new InterruptedException());
	    }
	} catch(InterruptedException e) {
	} finally {
	    ui.destroy();
	}
    }
	
    public GraphicsConfiguration getconf() {
	return(getGraphicsConfiguration());
    }
}
