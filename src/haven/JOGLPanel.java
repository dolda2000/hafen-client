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
    public final GPUProfile gprof = new GPUProfile(300);
    private double framedur_fg = 0.0, framedur_bg = 0.2;
    private boolean bgmode = false;
    private boolean iswap = true, aswap;
    private int fps, framelag;
    private volatile int frameno;
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
	setAutoSwapBufferMode(false);
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

    private void initgl(GL2 gl) {
	Collection<String> exts = Arrays.asList(gl.glGetString(GL.GL_EXTENSIONS).split(" "));
	GLCapabilitiesImmutable caps = getChosenGLCapabilities();
	gl.setSwapInterval((aswap = iswap) ? 1 : 0);
	if(exts.contains("GL_ARB_multisample") && caps.getSampleBuffers()) {
	    /* Apparently, having sample buffers in the config enables
	     * multisampling by default on some systems. */
	    gl.glDisable(GL.GL_MULTISAMPLE);
	}
    }

    private void setenv(GLEnvironment env) {
	if(this.env != null)
	    this.env.dispose();
	this.env = env;
	if(this.ui != null)
	    this.ui.env = env;
    }

    private volatile CPUProfile.Frame rcurf = null;
    private long swaptime = 0, gltime = 0, waittime = 0, prevswap = System.nanoTime();
    private void redraw(GL2 gl) {
	GLContext ctx = gl.getContext();
	GLEnvironment env;
	synchronized(this) {
	    if((this.env == null) || (this.env.ctx != ctx)) {
		setenv(new GLEnvironment(gl, ctx, shape));
		initgl(gl);
	    }
	    env = this.env;
	    if(!env.shape().equals(shape))
		env.reshape(shape);
	}
	try {
	    long pst = Config.profile ? System.nanoTime() : 0;
	    if(false) {
		System.err.println("\n-----\n\n");
		gl = new TraceGL2(gl, System.err);
	    }
	    env.process(gl);
	    long end = System.nanoTime();
	    if(Config.profile)
		gltime += end - pst;
	    if(swaptime != 0) {
		CPUProfile.Frame curf = this.rcurf; this.rcurf = null;
		if(curf != null) {
		    curf.add("wait", waittime);
		    curf.add("gl", gltime - swaptime);
		    curf.add("swap", swaptime);
		    curf.tick("awt");
		    curf.fin();
		}

		double fridle = (double)(swaptime + waittime) / (double)(end - prevswap);
		ridle = (ridle * 0.95) + (fridle * 0.05);

		gltime = 0;
		prevswap = end;
		if(Config.profile)
		    rcurf = rprof.new Frame();
	    }
	} catch(BGL.BGLException e) {
	    if(dumpbgl)
		e.dump.dump();
	    throw(e);
	}
    }

    private class BufferSwap implements BGL.Request {
	final int frameno;

	BufferSwap(int frameno) {
	    this.frameno = frameno;
	}

	public void run(GL2 gl) {
	    long swst = System.nanoTime();
	    if(iswap != aswap)
		gl.setSwapInterval((aswap = iswap) ? 1 : 0);
	    JOGLPanel.this.swapBuffers();
	    long end = System.nanoTime();
	    swaptime = end - swst;
	    framelag = JOGLPanel.this.frameno - frameno;
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
		long wst = System.nanoTime();
		env.submitwait();
		waittime = System.nanoTime() - wst;
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
	Disposable free = null;
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
		if(((String)tooltip).length() > 0) {
		    free = tt = (Text.render((String)tooltip)).tex();
		}
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
	if(free != null)
	    free.dispose();
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
    private void drawstats(UI ui, GOut g, GLRender buf) {
	int y = g.sz().y - 190;
	FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "FPS: %d (%d%%, %d%% idle, latency %d)", fps, (int)(uidle * 100.0), (int)(ridle * 100.0), framelag);
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
	if(ui.sess != null)
	    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Async: L %s, D %s", ui.sess.glob.loader.stats(), Defer.gstats());
	else
	    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Async: D %s", Defer.gstats());
	int rqd = Resource.local().qdepth() + Resource.remote().qdepth();
	if(rqd > 0)
	    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "RQ depth: %d (%d)", rqd, Resource.local().numloaded() + Resource.remote().numloaded());
    }

    private void display(UI ui, GLRender buf) {
	buf.clear(wnd, FragColor.fragcol, FColor.BLACK);
	Pipe state = wnd.copy();
	state.prep(new FrameInfo());
	GOut g = new GOut(buf, state, new Coord(getSize()));;
	synchronized(ui) {
	    ui.draw(g);
	}
	if(Config.dbtext)
	    drawstats(ui, g, buf);
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
		Fence prevframe = null;
		int framep = 0;
		while(true) {
		    double fwaited = 0;
		    GLEnvironment env = this.env;
		    GLRender buf = env.render();
		    Debug.cycle();
		    CPUProfile.Frame curf = Config.profile ? uprof.new Frame() : null;
		    GPUProfile.Frame curgf = Config.profilegpu ? gprof.new Frame(buf) : null;
		    Fence curframe = new Fence();
		    buf.submit(curframe);

		    UI ui = this.ui;

		    int cfno = frameno++;
		    synchronized(ui) {
			ed.dispatch(ui);
			if(curf != null) curf.tick("dsp");

			if(ui.sess != null) {
			    ui.sess.glob.ctick();
			    ui.sess.glob.gtick(buf);
			}
			if(curf != null) curf.tick("stick");
			ui.tick();
			if((ui.root.sz.x != (shape.br.x - shape.ul.x)) || (ui.root.sz.y != (shape.br.y - shape.ul.y)))
			    ui.root.resize(new Coord(shape.br.x - shape.ul.x, shape.br.y - shape.ul.y));
			if(curf != null) curf.tick("tick");
			if(curgf != null) curgf.tick(buf, "tick");
		    }

		    if(prevframe != null) {
			double now = Utils.rtime();
			prevframe.waitfor();
			prevframe = null;
			fwaited += Utils.rtime() - now;
		    }
		    prevframe = curframe;

		    if(curf != null) curf.tick("dwait");
		    display(ui, buf);
		    if(curf != null) curf.tick("draw");
		    if(curgf != null) curgf.tick(buf, "draw");
		    buf.submit(new BufferSwap(cfno));
		    if(curgf != null) curgf.tick(buf, "swap");
		    if(curgf != null) curgf.fin(buf);
		    env.submit(buf);
		    if(curf != null) curf.tick("aux");

		    double now = Utils.rtime();
		    double fd = (bgmode && (this.framedur_bg > 0.0)) ? this.framedur_bg : this.framedur_fg;
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
	ui.env = this.env;
	ui.root.guprof = uprof;
	ui.root.grprof = rprof;
	ui.root.ggprof = gprof;
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
	cmdmap.put("hz", (cons, args) -> {
		int hz = Integer.parseInt(args[1]);
		if(hz > 0)
		    framedur_fg = 1.0 / hz;
		else
		    framedur_fg = 0.0;
	    });
	cmdmap.put("bghz", (cons, args) -> {
		int hz = Integer.parseInt(args[1]);
		if(hz > 0)
		    framedur_bg = 1.0 / hz;
		else
		    framedur_bg = 0.0;
	    });
	cmdmap.put("vsync", (cons, args) -> {
		iswap = Utils.parsebool(args[1]);
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
