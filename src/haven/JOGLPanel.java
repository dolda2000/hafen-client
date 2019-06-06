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
import java.awt.Toolkit;
import javax.media.opengl.*;
import javax.media.opengl.awt.*;
import haven.render.*;
import haven.render.States;
import haven.render.gl.*;

public class JOGLPanel extends GLCanvas implements Runnable, UIPanel, Console.Directory {
    private static final boolean dumpbgl = true;
    public final boolean vsync = true;
    public final CPUProfile uprof = new CPUProfile(300), rprof = new CPUProfile(300);
    private double framedur_fg = 0.0, framedur_bg = 0.2;
    private boolean bgmode = false;
    private int fps;
    private double uidle = 0.0, ridle = 0.0;
    private final Dispatcher ed;
    private GLEnvironment env = null;
    private UI ui;
    private Area shape;
    private Pipe base, wnd;

    private static GLCapabilities mkcaps() {
	GLProfile prof = GLProfile.getDefault();
	GLCapabilities caps = new GLCapabilities(prof);
	caps.setDoubleBuffered(true);
	caps.setAlphaBits(8);
	caps.setRedBits(8);
	caps.setGreenBits(8);
	caps.setBlueBits(8);
	return(caps);
    }

    public JOGLPanel(Coord sz) {
	super(mkcaps(), null, null);
	base = new BufPipe();
	base.prep(new FragColor<>(FragColor.defcolor)).prep(new DepthBuffer<>(DepthBuffer.defdepth));
	base.prep(new States.Blending());
	setSize(sz.x, sz.y);
	addGLEventListener(new GLEventListener() {
		public void display(GLAutoDrawable d) {
		    redraw(d.getGL().getGL2());
		}

		public void init(GLAutoDrawable d) {
		}

		public void reshape(GLAutoDrawable wdg, int x, int y, int w, int h) {
		    Area area = Area.sized(new Coord(x, y), new Coord(w, h));
		    shape = area;
		    wnd = base.copy();
		    wnd.prep(new States.Viewport(area)).prep(new Ortho2D(area));
		}

		public void dispose(GLAutoDrawable wdg) {
		}
	    });
	setFocusTraversalKeysEnabled(false);
	ed = new Dispatcher();
	ed.register(this);
	newui(null);
	if(Toolkit.getDefaultToolkit().getMaximumCursorColors() >= 256)
	    cursmode = "awt";
    }

    private static class Frame {
	GLRender buf;
	GLEnvironment env;
	BufferBGL dispose;
	boolean debug;

	Frame(GLRender buf, GLEnvironment env, BufferBGL dispose) {
	    this.buf = buf;
	    this.env = env;
	    this.dispose = dispose;
	}
    }

    private final Frame[] curdraw = {null};

    private void initgl(GL2 gl) {
	Collection<String> exts = Arrays.asList(gl.glGetString(GL.GL_EXTENSIONS).split(" "));
	GLCapabilitiesImmutable caps = getChosenGLCapabilities();
	gl.setSwapInterval(1);
	if(exts.contains("GL_ARB_multisample") && caps.getSampleBuffers()) {
	    /* Apparently, having sample buffers in the config enables
	     * multisampling by default on some systems. */
	    gl.glDisable(GL.GL_MULTISAMPLE);
	}
    }

    private void redraw(GL2 gl) {
	CPUProfile.Frame curf = Config.profile ? rprof.new Frame() : null;
	GLContext ctx = gl.getContext();
	GLEnvironment env;
	synchronized(this) {
	    if((this.env == null) || (this.env.ctx != ctx)) {
		this.env = new GLEnvironment(gl, ctx, shape);
		initgl(gl);
	    }
	    env = this.env;
	    if(!env.shape().equals(shape))
		env.reshape(shape);
	}
	Frame f;
	synchronized(curdraw) {
	    f = curdraw[0];
	    curdraw[0] = null;
	    curdraw.notifyAll();
	}
	if(curf != null) curf.tick("init");
	try {
	    if(f != null) {
		if(f.env == env) {
		    if(f.debug) {
			System.err.print("\n-----\n\n");
			gl = new TraceGL2(gl, System.err);
		    }
		    env.submit(gl, f.buf);
		    if(curf != null) curf.tick("gl");
		    f.dispose.run(gl);
		} else {
		    f.buf.dispose();
		}
	    }
	    if(curf != null) curf.tick("dispose");
	    if(curf != null) curf.fin();
	} catch(BGL.BGLException e) {
	    if(dumpbgl)
		e.dump.dump();
	    throw(e);
	}
    }

    private void uglyjoglhack() throws InterruptedException {
	try {
	    display();
	} catch(RuntimeException e) {
	    InterruptedException irq = Utils.hascause(e, InterruptedException.class);
	    if(irq != null)
		throw(irq);
	    throw(e);
	}
    }

    private void renderloop() {
	try {
	    uglyjoglhack();
	    synchronized(this) {
		if(env == null)
		    throw(new RuntimeException("Did not get GL environment even after display"));
		notifyAll();
	    }
	    while(true) {
		synchronized(curdraw) {
		    while(curdraw[0] == null)
			curdraw.wait();
		}
		uglyjoglhack();
	    }
	} catch(InterruptedException e) {
	}
    }

    private void drawtooltip(GOut g) {
	Object tooltip;
        try {
	    synchronized(ui) {
		tooltip = ui.root.tooltip(ui.mc, ui.root);
	    }
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
	    Coord pos = ui.mc.add(sz.inv());
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
    }

    private String cursmode = "tex";
    private Resource lastcursor = null;
    private void drawcursor(GOut g) {
	Resource curs = ui.root.getcurs(ui.mc);
	if(cursmode == "awt") {
	    if(curs != lastcursor) {
		try {
		    if(curs == null)
			setCursor(null);
		    else
			setCursor(UIPanel.makeawtcurs(curs.layer(Resource.imgc).img, curs.layer(Resource.negc).cc));
		} catch(Exception e) {
		    cursmode = "tex";
		}
	    }
	} else if(cursmode == "tex") {
	    if(curs == null) {
		if(lastcursor != null)
		    setCursor(null);
	    } else {
		if(lastcursor == null)
		    setCursor(emptycurs);
		Coord dc = ui.mc.add(curs.layer(Resource.negc).cc.inv());
		g.image(curs.layer(Resource.imgc), dc);
	    }
	}
	lastcursor = curs;
    }

    @SuppressWarnings("deprecation")
    private void drawstats(GOut g, GLRender buf) {
	int y = g.sz().y - 190;
	FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "FPS: %d (%d%%, %d%% idle)", fps, (int)(uidle * 100.0), (int)(ridle * 100.0));
	Runtime rt = Runtime.getRuntime();
	long free = rt.freeMemory(), total = rt.totalMemory();
	FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Mem: %,011d/%,011d/%,011d/%,011d", free, total - free, total, rt.maxMemory());
	FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "State slots: %d", State.Slot.numslots());
	FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "GL progs: %d", buf.env.numprogs());
	FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "V-Mem: %s", buf.env.memstats());
	MapView map = ui.root.findchild(MapView.class);
	if((map != null) && (map.back != null)) {
	    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Mapview: Tree %s, Draw %s", map.tree.stats(), map.back.stats());
	}
    }

    private void display(GLRender buf) {
	buf.clear(wnd, FragColor.fragcol, FColor.BLACK);
	Pipe state = wnd.copy();
	state.prep(new FrameInfo());
	GOut g = new GOut(buf, state, new Coord(getSize()));;
	synchronized(ui) {
	    ui.draw(g);
	}
	if(Config.dbtext)
	    drawstats(g, buf);
	drawtooltip(g);
	drawcursor(g);
    }

    public void run() {
	Thread drawthread = new HackThread(this::renderloop, "Render thread");
	drawthread.start();
	try {
	    try {
		synchronized(this) {
		    while(this.env == null)
			this.wait();
		}
		double then = Utils.rtime();
		double[] frames = new double[128], waited = new double[frames.length];
		int framep = 0;
		while(true) {
		    double fwaited = 0;
		    GLEnvironment env = this.env;
		    GLRender buf = env.render();
		    Debug.cycle();
		    CPUProfile.Frame curf = Config.profile ? uprof.new Frame() : null;

		    synchronized(ui) {
			ed.dispatch(ui);
			if(curf != null) curf.tick("dsp");

			if(ui.sess != null) {
			    ui.sess.glob.ctick();
			    ui.sess.glob.gtick(buf);
			}
			if(curf != null) curf.tick("stick");
			ui.tick();
			if(curf != null) curf.tick("tick");
		    }

		    synchronized(curdraw) {
			double now = Utils.rtime();
			while(curdraw[0] != null)
			    curdraw.wait();
			fwaited += Utils.rtime() - now;
		    }
		    if(curf != null) curf.tick("dwait");
		    display(buf);
		    if(curf != null) curf.tick("draw");
		    BufferBGL dispose = env.disposeall();
		    synchronized(curdraw) {
			if(curdraw[0] != null)
			    throw(new AssertionError());
			curdraw[0] = new Frame(buf, env, dispose);
			if(false)
			    curdraw[0].debug = true;
			curdraw.notifyAll();
		    }
		    if(curf != null) curf.tick("aux");

		    double now = Utils.rtime();
		    double fd = bgmode ? this.framedur_bg : this.framedur_fg;
		    if(then + fd > now) {
			then += fd;
			synchronized(ed) {
			    long nanos = (long)((then - now) * 1e9);
			    ed.wait(nanos / 1000000, (int)(nanos % 1000000));
			}
		    } else {
			then = now;
		    }
		    fwaited += Utils.rtime() - now;
		    frames[framep] = now;
		    waited[framep] = fwaited;
		    {
			double twait = 0;
			int i = 0, ckf = framep;
			for(; i < frames.length - 1; i++) {
			    ckf = (ckf - 1 + frames.length) % frames.length;
			    twait += waited[ckf];
			    if(now - frames[ckf] > 1)
				break;
			}
			if(now > frames[ckf]) {
			    fps = (int)Math.round((i + 1) / (now - frames[ckf]));
			    uidle = twait / (now - frames[ckf]);
			}
		    }
		    framep = (framep + 1) % frames.length;
		    if(curf != null) curf.tick("wait");

		    if(curf != null) curf.fin();
		}
	    } finally {
		drawthread.interrupt();
		drawthread.join();
	    }
	} catch(InterruptedException e) {
	}
    }

    public UI newui(Session sess) {
	if(ui != null)
	    ui.destroy();
	ui = new UI(new Coord(getSize()), sess);
	ui.root.guprof = uprof;
	ui.root.grprof = rprof;
	if(getParent() instanceof Console.Directory)
	    ui.cons.add((Console.Directory)getParent());
	ui.cons.add(this);
	return(ui);
    }

    public void background(boolean bg) {
	bgmode = bg;
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("hz", new Console.Command() {
		public void run(Console cons, String[] args) {
		    int hz = Integer.parseInt(args[1]);
		    if(hz > 0)
			framedur_fg = 1.0 / hz;
		    else
			framedur_fg = 0.0;
		}
	    });
	cmdmap.put("bghz", new Console.Command() {
		public void run(Console cons, String[] args) {
		    int hz = Integer.parseInt(args[1]);
		    if(hz > 0)
			framedur_bg = 1.0 / hz;
		    else
			framedur_bg = 0.0;
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
