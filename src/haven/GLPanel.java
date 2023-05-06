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
import haven.render.*;
import haven.render.gl.*;
import java.awt.Toolkit;
import haven.JOGLPanel.SyncMode;

public interface GLPanel extends UIPanel, UI.Context {
    public GLEnvironment env();
    public Area shape();
    public Pipe basestate();
    public void glswap(GL gl);

    public static class Loop implements Console.Directory {
	public static boolean gldebug = false;
	public final GLPanel p;
	public final CPUProfile uprof = new CPUProfile(300), rprof = new CPUProfile(300);
	public final GPUProfile gprof = new GPUProfile(300);
	protected boolean bgmode = false;
	protected int fps, framelag;
	protected volatile int frameno;
	protected double uidle = 0.0, ridle = 0.0;
	protected long lastrcycle = 0, ridletime = 0;
	protected UI lockedui, ui;
	private final Dispatcher ed;
	private final Object uilock = new Object();

	public Loop(GLPanel p) {
	    this.p = p;
	    ed = new Dispatcher();
	    ed.register((java.awt.Component)p);
	}

	private double framedur() {
	    GSettings gp = this.ui.gprefs;
	    double hz = gp.hz.val, bghz = gp.bghz.val;
	    if(bgmode) {
		if(bghz != Double.POSITIVE_INFINITY)
		    return(1.0 / bghz);
	    }
	    if(hz == Double.POSITIVE_INFINITY)
		return(0.0);
	    return(1.0 / hz);
	}

	private class BufferSwap implements BGL.Request {
	    final int frameno;

	    BufferSwap(int frameno) {
		this.frameno = frameno;
	    }

	    public void run(GL gl) {
		long start = System.nanoTime();
		p.glswap(gl);
		ridletime += System.nanoTime() - start;
		framelag = Loop.this.frameno - frameno;
	    }
	}

	private class GLFinish implements BGL.Request {
	    public void run(GL gl) {
		long start = System.nanoTime();
		gl.glFinish();
		/* Should this count towards idle time? Who knows. */
		ridletime += System.nanoTime() - start;
	    }
	}

	private class FrameCycle implements BGL.Request {
	    public void run(GL gl) {
		long now = System.nanoTime();
		if(lastrcycle != 0) {
		    double fridle = (double)ridletime / (double)(now - lastrcycle);
		    ridle = (ridle * 0.95) + (fridle * 0.05);
		}
		lastrcycle = now;
		ridletime = 0;
	    }
	}

	public static class ProfileCycle implements BGL.Request {
	    final CPUProfile prof;
	    final String label;
	    ProfileCycle prev;
	    CPUProfile.Frame frame;

	    ProfileCycle(CPUProfile prof, ProfileCycle prev, String label) {
		this.prof = prof;
		this.prev = prev;
		this.label = label;
	    }

	    public void run(GL gl) {
		if(prev != null) {
		    if(prev.frame != null) {
			/* The reason frame would be null is if the
			 * environment has become invalid and the previous
			 * cycle never ran. */
			if(label != null)
			    prev.frame.tick(label);
			prev.frame.fin();
		    }
		    prev = null;
		}
		frame = prof.new Frame();
	    }
	}

	public static class ProfileTick implements BGL.Request {
	    final ProfileCycle prof;
	    final String label;

	    ProfileTick(ProfileCycle prof, String label) {
		this.prof = prof;
		this.label = label;
	    }

	    public void run(GL gl) {
		if((prof != null) && (prof.frame != null))
		    prof.frame.tick(label);
	    }
	}

	private Object prevtooltip = null;
	private Indir<Tex> prevtooltex = null;
	private Disposable freetooltex = null;
	private void drawtooltip(UI ui, GOut g) {
	    Object tooltip;
	    try {
		synchronized(ui) {
		    tooltip = ui.root.tooltip(ui.mc, ui.root);
		}
	    } catch(Loading e) {
		tooltip = "...";
	    }
	    Indir<Tex> tt = null;
	    if(Utils.eq(tooltip, prevtooltip)) {
		tt = prevtooltex;
	    } else {
		if(freetooltex != null) {
		    freetooltex.dispose();
		    freetooltex = null;
		}
		prevtooltip = null;
		prevtooltex = null;
		Disposable free = null;
		if(tooltip != null) {
		    if(tooltip instanceof Text) {
			Tex t = ((Text)tooltip).tex();
			tt = () -> t;
		    } else if(tooltip instanceof Tex) {
			Tex t = (Tex)tooltip;
			tt = () -> t;
		    } else if(tooltip instanceof Indir<?>) {
			@SuppressWarnings("unchecked")
			    Indir<Tex> c = (Indir<Tex>)tooltip;
			tt = c;
		    } else if(tooltip instanceof String) {
			if(((String)tooltip).length() > 0) {
			    Tex r = new TexI(Text.render((String)tooltip).img, false);
			    tt = () -> r;
			    free = r;
			}
		    }
		}
		prevtooltip = tooltip;
		prevtooltex = tt;
		freetooltex = free;
	    }
	    Tex tex = (tt == null) ? null : tt.get();
	    if(tex != null) {
		Coord sz = tex.sz();
		Coord pos = ui.mc.sub(sz).sub(curshotspot);
		if(pos.x < 0)
		    pos.x = 0;
		if(pos.y < 0)
		    pos.y = 0;
		g.chcolor(244, 247, 21, 192);
		g.rect(pos.add(-3, -3), sz.add(6, 6));
		g.chcolor(35, 35, 35, 192);
		g.frect(pos.add(-2, -2), sz.add(4, 4));
		g.chcolor();
		g.image(tex, pos);
	    }
	    ui.lasttip = tooltip;
	}

	private static String defaultcurs() {
	    if(Toolkit.getDefaultToolkit().getMaximumCursorColors() >= 256)
		return("awt");
	    return("tex");
	}

	private String cursmode = defaultcurs();
	private Resource lastcursor = null;
	private Coord curshotspot = Coord.z;
	private void drawcursor(UI ui, GOut g) {
	    Resource curs;
	    synchronized(ui) {
		curs = ui.getcurs(ui.mc);
	    }
	    if(cursmode == "awt") {
		if(curs != lastcursor) {
		    try {
			if(curs == null) {
			    curshotspot = Coord.z;
			    p.setCursor(null);
			} else {
			    curshotspot = curs.flayer(Resource.negc).cc;
			    p.setCursor(UIPanel.makeawtcurs(curs.flayer(Resource.imgc).img, curshotspot));
			}
		    } catch(Exception e) {
			cursmode = "tex";
		    }
		}
	    } else if(cursmode == "tex") {
		if(curs == null) {
		    curshotspot = Coord.z;
		    if(lastcursor != null)
			p.setCursor(null);
		} else {
		    if(lastcursor == null)
			p.setCursor(emptycurs);
		    curshotspot = UI.scale(curs.flayer(Resource.negc).cc);
		    Coord dc = ui.mc.sub(curshotspot);
		    g.image(curs.flayer(Resource.imgc), dc);
		}
	    }
	    lastcursor = curs;
	}

	private long prevfree = 0, framealloc = 0;
	@SuppressWarnings("deprecation")
	private void drawstats(UI ui, GOut g, GLRender buf) {
	    int y = g.sz().y - UI.scale(190), dy = FastText.h;
	    FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "FPS: %d (%d%%, %d%% idle, latency %d)", fps, (int)(uidle * 100.0), (int)(ridle * 100.0), framelag);
	    Runtime rt = Runtime.getRuntime();
	    long free = rt.freeMemory(), total = rt.totalMemory();
	    if(free < prevfree)
		framealloc = ((prevfree - free) + (framealloc * 19)) / 20;
	    prevfree = free;
	    FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "Mem: %,011d/%,011d/%,011d/%,011d (%,d)", free, total - free, total, rt.maxMemory(), framealloc);
	    FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "State slots: %d", State.Slot.numslots());
	    FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "GL progs: %d", buf.env.numprogs());
	    FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "V-Mem: %s", buf.env.memstats());
	    MapView map = ui.root.findchild(MapView.class);
	    if((map != null) && (map.back != null)) {
		FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "Camera: %s", map.camstats());
		FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "Mapview: %s", map.stats());
		// FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "Click: Map: %s, Obj: %s", map.clmaplist.stats(), map.clobjlist.stats());
	    }
	    if(ui.sess != null)
		FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "Async: L %s, D %s", ui.sess.glob.loader.stats(), Defer.gstats());
	    else
		FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "Async: D %s", Defer.gstats());
	    int rqd = Resource.local().qdepth() + Resource.remote().qdepth();
	    if(rqd > 0)
		FastText.aprintf(g, new Coord(10, y -= dy), 0, 1, "RQ depth: %d (%d)", rqd, Resource.local().numloaded() + Resource.remote().numloaded());
	    synchronized(Debug.framestats) {
		for(Object line : Debug.framestats)
		    FastText.aprint(g, new Coord(10, y -= dy), 0, 1, String.valueOf(line));
	    }
	}

	private StreamOut streamout = null;

	private void display(UI ui, GLRender buf) {
	    Pipe wnd = p.basestate();
	    buf.clear(wnd, FragColor.fragcol, FColor.BLACK);
	    Pipe state = wnd.copy();
	    state.prep(new FrameInfo());
	    GOut g = new GOut(buf, state, new Coord(p.getSize()));;
	    synchronized(ui) {
		ui.draw(g);
	    }
	    if(dbtext.get())
		drawstats(ui, g, buf);
	    drawtooltip(ui, g);
	    drawcursor(ui, g);
	    if(StreamOut.path.get() != null) {
		if(streamout == null) {
		    try {
			streamout = new StreamOut(p.shape().sz(), StreamOut.path.get());
		    } catch(java.io.IOException e) {
			throw(new RuntimeException(e));
		    }
		}
		streamout.accept(buf, state);
	    }
	}

	public void run() throws InterruptedException {
	    GLRender buf = null;
	    try {
		double then = Utils.rtime();
		double[] frames = new double[128], waited = new double[frames.length];
		Fence prevframe = null;
		ProfileCycle rprofc = null;
		int framep = 0;
		while(true) {
		    double fwaited = 0;
		    GLEnvironment env = p.env();
		    buf = env.render();
		    UI ui;
		    synchronized(uilock) {
			this.lockedui = ui = this.ui;
			uilock.notifyAll();
		    }
		    Debug.cycle(ui.modflags());
		    GSettings prefs = ui.gprefs;
		    SyncMode syncmode = prefs.syncmode.val;
		    CPUProfile.Frame curf = profile.get() ? uprof.new Frame() : null;
		    GPUProfile.Frame curgf = profilegpu.get() ? gprof.new Frame(buf) : null;
		    BufferBGL.Profile frameprof = false ? new BufferBGL.Profile() : null;
		    if(frameprof != null) buf.submit(frameprof.start);
		    buf.submit(new ProfileTick(rprofc, "wait"));
		    Fence curframe = new Fence();
		    if(syncmode == SyncMode.FRAME)
			buf.submit(curframe);

		    boolean tickwait = (syncmode == SyncMode.FRAME) || (syncmode == SyncMode.TICK);
		    if(!tickwait) {
			if(prevframe != null) {
			    double now = Utils.rtime();
			    prevframe.waitfor();
			    prevframe = null;
			    fwaited += Utils.rtime() - now;
			}
			if(curf != null) curf.tick("dwait");
		    }

		    int cfno = frameno++;
		    synchronized(ui) {
			ed.dispatch(ui);
			ui.mousehover(ui.mc);
			if(curf != null) curf.tick("dsp");

			if(ui.sess != null) {
			    ui.sess.glob.ctick();
			    ui.sess.glob.gtick(buf);
			}
			if(curf != null) curf.tick("stick");
			ui.tick();
			ui.gtick(buf);
			Area shape = p.shape();
			if((ui.root.sz.x != (shape.br.x - shape.ul.x)) || (ui.root.sz.y != (shape.br.y - shape.ul.y)))
			    ui.root.resize(new Coord(shape.br.x - shape.ul.x, shape.br.y - shape.ul.y));
			if(curf != null) curf.tick("tick");
			buf.submit(new ProfileTick(rprofc, "tick"));
			if(curgf != null) curgf.tick(buf, "tick");
		    }

		    if(tickwait) {
			if(prevframe != null) {
			    double now = Utils.rtime();
			    prevframe.waitfor();
			    prevframe = null;
			    fwaited += Utils.rtime() - now;
			}
			if(curf != null) curf.tick("dwait");
		    }

		    display(ui, buf);
		    if(curf != null) curf.tick("draw");
		    if(curgf != null) curgf.tick(buf, "draw");
		    buf.submit(new ProfileTick(rprofc, "gl"));
		    buf.submit(new BufferSwap(cfno));
		    if(curgf != null) curgf.tick(buf, "swap");
		    buf.submit(new ProfileTick(rprofc, "swap"));
		    if(curgf != null) curgf.fin(buf);
		    if(syncmode == SyncMode.FINISH) {
			buf.submit(new GLFinish());
			buf.submit(new ProfileTick(rprofc, "finish"));
		    }
		    if(syncmode != SyncMode.FRAME)
			buf.submit(curframe);
		    if(profile.get())
			buf.submit(rprofc = new ProfileCycle(rprof, rprofc, "aux"));
		    else
			rprofc = null;
		    buf.submit(new FrameCycle());
		    if(frameprof != null) {
			buf.submit(frameprof.stop);
			buf.submit(frameprof.dump(Utils.path("frameprof")));
		    }
		    env.submit(buf);
		    buf = null;
		    if(curf != null) curf.tick("aux");

		    double now = Utils.rtime();
		    double fd = framedur();
		    if(then + fd > now) {
			then += fd;
			long nanos = (long)((then - now) * 1e9);
			Thread.sleep(nanos / 1000000, (int)(nanos % 1000000));
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
		    prevframe = curframe;
		}
	    } finally {
		synchronized(uilock) {
		    lockedui = null;
		    uilock.notifyAll();
		}
		if(buf != null)
		    buf.dispose();
	    }
	}

	public UI newui(UI.Runner fun) {
	    UI prevui, newui = new UI(p, new Coord(p.getSize()), fun);
	    newui.env = p.env();
	    if(p.getParent() instanceof Console.Directory)
		newui.cons.add((Console.Directory)p.getParent());
	    if(p instanceof Console.Directory)
		newui.cons.add((Console.Directory)p);
	    newui.cons.add(this);
	    synchronized(uilock) {
		prevui = this.ui;
		ui = newui;
		ui.root.guprof = uprof;
		ui.root.grprof = rprof;
		ui.root.ggprof = gprof;
		while((this.lockedui != null) && (this.lockedui == prevui)) {
		    try {
			uilock.wait();
		    } catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			break;
		    }
		}
	    }
	    if(prevui != null) {
		synchronized(prevui) {
		    prevui.destroy();
		}
	    }
	    return(newui);
	}

	private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
	{
	    cmdmap.put("gldebug", (cons, args) -> {
		    gldebug = Utils.parsebool(args[1]);
		});
	}
	public Map<String, Console.Command> findcmds() {
	    return(cmdmap);
	}

	{
	    if(Utils.getprefb("glcrash", false)) {
		Warning.warn("enabling GL debug-mode due to GL crash flag being set");
		Utils.setprefb("glcrash", false);
		haven.error.ErrorHandler errh = haven.error.ErrorHandler.find();
		if(errh != null)
		    errh.lsetprop("gl.debug", Boolean.TRUE);
		gldebug = true;
	    }
	}

	/* XXX: This should be in UIPanel, but Java is dumb and needlessly forbids it. */
	static {
	    Console.setscmd("stats", new Console.Command() {
		    public void run(Console cons, String[] args) {
			dbtext.set(Utils.parsebool(args[1]));
		    }
		});
	    Console.setscmd("profile", new Console.Command() {
		    public void run(Console cons, String[] args) {
			if(args[1].equals("none") || args[1].equals("off")) {
			    profile.set(false);
			    profilegpu.set(false);
			} else if(args[1].equals("cpu")) {
			    profile.set(true);
			} else if(args[1].equals("gpu")) {
			    profilegpu.set(true);
			} else if(args[1].equals("all")) {
			    profile.set(true);
			    profilegpu.set(true);
			}
		    }
		});
	}
    }
}
