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

package haven.render.gl;

import java.util.*;
import java.util.function.*;
import java.nio.ByteBuffer;
import haven.*;
import haven.render.*;
import haven.render.sl.*;
import static haven.render.DataBuffer.Usage.*;

public abstract class GLEnvironment implements Environment {
    public static final boolean debuglog = false, labels = false;
    public final Caps caps;
    public int nilfbo_id = 0, nilfbo_db = 0;
    final Object drawmon = new Object();
    final Collection<GLObject> disposed = new LinkedList<>();
    final List<GLQuery> queries = new LinkedList<>(); // Synchronized on drawmon
    final Queue<Runnable> callbacks = new LinkedList<>();
    Thread cbthread = null;
    final RenderQueue<GLRender> queue = new RenderQueue<>();
    Area wnd;
    private Applier curstate = new Applier(this);

    public static class HardwareException extends UnavailableException {
	public final Caps caps;

	public HardwareException(String msg, Caps caps) {
	    super(msg);
	    this.caps = caps;
	}
    }

    public static class Caps implements java.io.Serializable, Environment.Caps {
	private static final java.util.regex.Pattern slvp = java.util.regex.Pattern.compile("^(\\d+)\\.(\\d+)");
	public final String vendor, version, renderer;
	public final int major, minor, glslver;
	public final Collection<String> exts;
	public final int maxtargets;
	public final float anisotropy;
	public final float linemin, linemax;

	public static int glgeti(GL gl, int param) {
	    int[] buf = {0};
	    gl.glGetIntegerv(param, buf);
	    GLException.checkfor(gl, null);
	    return(buf[0]);
	}

	public static int glcondi(GL gl, int param, int def) {
	    GLException.checkfor(gl, null);
	    int[] buf = {0};
	    gl.glGetIntegerv(param, buf);
	    if(gl.glGetError() != 0)
		return(def);
	    return(buf[0]);
	}

	public static float glgetf(GL gl, int param) {
	    float[] buf = {0};
	    gl.glGetFloatv(param, buf);
	    GLException.checkfor(gl, null);
	    return(buf[0]);
	}

	public static String glconds(GL gl, int param) {
	    GLException.checkfor(gl, null);
	    String ret = gl.glGetString(param);
	    if(gl.glGetError() != 0)
		return(null);
	    return(ret);
	}

	public Caps(GL gl) {
	    {
		int major, minor;
		try {
		    major = glgeti(gl, GL.GL_MAJOR_VERSION);
		    minor = glgeti(gl, GL.GL_MINOR_VERSION);
		} catch(GLException e) {
		    major = 1;
		    minor = 0;
		}
		this.major = major; this.minor = minor;
	    }
	    this.vendor = gl.glGetString(GL.GL_VENDOR);
	    this.version = gl.glGetString(GL.GL_VERSION);
	    this.renderer = gl.glGetString(GL.GL_RENDERER);
	    if(major >= 3) {
		this.exts = new ArrayList<>();
		for(int i = 0, n = glgeti(gl, GL.GL_NUM_EXTENSIONS); i < n; i++)
		    this.exts.add(gl.glGetStringi(GL.GL_EXTENSIONS, i));
	    } else {
		this.exts = Arrays.asList(gl.glGetString(GL.GL_EXTENSIONS).split(" "));
	    }
	    this.maxtargets = glcondi(gl, GL.GL_MAX_COLOR_ATTACHMENTS, 1);
	    {
		int glslver = 0;
		String slv = glconds(gl, GL.GL_SHADING_LANGUAGE_VERSION);
		if(slv != null) {
		    java.util.regex.Matcher m = slvp.matcher(slv);
		    if(m.find()) {
			try {
			    int major = Integer.parseInt(m.group(1));
			    int minor = Integer.parseInt(m.group(2));
			    if((major > 0) && (major < 256) && (minor >= 0) && (minor < 256))
				glslver = (major << 8) | minor;
			} catch(NumberFormatException e) {
			}
		    }
		}
		this.glslver = glslver;
	    }
	    if(exts.contains("GL_EXT_texture_filter_anisotropic"))
		anisotropy = glgetf(gl, GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
	    else
		anisotropy = 0;
	    {
		float[] buf = {0, 0};
		gl.glGetFloatv(GL.GL_ALIASED_LINE_WIDTH_RANGE, buf);
		if(gl.glGetError() == 0) {
		    this.linemin = buf[0];
		    this.linemax = buf[1];
		} else {
		    this.linemin = this.linemax = 1;
		}
	    }
	}

	public void checkreq() {
	    if(major < 3)
		throw(new HardwareException("Graphics context does not support OpenGL 3.0.", this));
	}

	public String vendor() {return(vendor);}
	public String driver() {return("OpenGL (" + version + ")");}
	public String device() {return(renderer);}
    }

    static enum MemStats {
	INDICES, VERTICES, TEXTURES, VAOS, FBOS
    }
    final int[] stats_obj = new int[MemStats.values().length];
    final long[] stats_mem = new long[MemStats.values().length];

    protected abstract Caps mkcaps(GL initgl);

    public GLEnvironment(GL initgl, Area wnd) {
	this.wnd = wnd;
	this.caps = mkcaps(initgl);
	this.caps.checkreq();
	initialize(initgl);
    }

    private void initialize(GL gl) {
	if(debuglog) {
	    gl.glEnable(GL.GL_DEBUG_OUTPUT);
	    gl.glDebugMessageControl(GL.GL_DONT_CARE, GL.GL_DONT_CARE, GL.GL_DONT_CARE, 0, new int[0], true);
	    /* gl.glDebugMessageControl(GL3.GL_DEBUG_SOURCE_API, GL3.GL_DEBUG_TYPE_OTHER, GL3.GL_DONT_CARE, 1, new int[] {131185}, 0, false); */
	}
	gl.glEnable(GL.GL_PROGRAM_POINT_SIZE);
    }

    public GLRender render() {
	return(new GLRender(this));
    }

    public GLDrawList drawlist() {
	return(new GLDrawList(this));
    }

    public void reshape(Area wnd) {
	this.wnd = wnd;
    }

    public Area shape() {
	return(wnd);
    }

    private void ckcbt() {
	synchronized(callbacks) {
	    if(!callbacks.isEmpty() && (cbthread == null)) {
		cbthread = new HackThread(this::cbloop, "Render-query callback thread");
		cbthread.setDaemon(true);
		cbthread.start();
	    }
	}
    }

    private void cbloop() {
	try {
	    double last = Utils.rtime(), now = last;
	    while(true) {
		Runnable cb;
		synchronized(callbacks) {
		    while(callbacks.isEmpty()) {
			if(now - last >= 5) {
			    cbthread = null;
			    return;
			}
			callbacks.wait((int)((last + 6 - now) * 1000));
			now = Utils.rtime();
		    }
		    cb = callbacks.remove();
		    last = now;
		}
		cb.run();
	    }
	} catch(InterruptedException e) {
	} finally {
	    synchronized(callbacks) {
		if(cbthread == Thread.currentThread())
		    cbthread = null;
		ckcbt();
	    }
	}
    }

    void callback(Runnable cb) {
	synchronized(callbacks) {
	    callbacks.add(cb);
	    callbacks.notifyAll();
	    ckcbt();
	}
    }

    public void synccallbacks() throws InterruptedException {
	boolean[] done = {false};
	callback(() -> {
		synchronized(done) {
		    done[0] = true;
		    done.notifyAll();
		}
	    });
	synchronized(done) {
	    while(!done[0])
		done.wait();
	}
    }

    private void checkqueries(GL gl) {
	for(Iterator<GLQuery> i = queries.iterator(); i.hasNext();) {
	    GLQuery query = i.next();
	    if(!query.check(gl))
		continue;
	    query.dispose();
	    i.remove();
	}
    }

    public static class DebugMessage {
	public final int src, type, id, sev;
	public final String msg;

	public DebugMessage(int src, int type, int id, int sev, String msg) {
	    this.src = src;
	    this.type = type;
	    this.id = id;
	    this.sev = sev;
	    this.msg = msg;
	}
    }

    private List<DebugMessage> getdebuglog(GL gl) {
	List<DebugMessage> ret = new ArrayList<>();
	int n = 16;
	int[] src = new int[n], type = new int[n], id = new int[n], sev = new int[n], len = new int[n];
	while(true) {
	    int nlen = Caps.glgeti(gl, GL.GL_DEBUG_NEXT_LOGGED_MESSAGE_LENGTH);
	    byte[] buf = new byte[Math.max(nlen, 128) * n];
	    int rv = gl.glGetDebugMessageLog(n, buf.length, src, type, id, sev, len, buf);
	    if(rv == 0)
		break;
	    for(int i = 0, p = 0; i < rv; p += len[i++])
		ret.add(new DebugMessage(src[i], type[i], id[i], sev[i], new String(buf, p, len[i] - 1)));
	}
	return(ret);
    }

    private void checkdebuglog(GL gl) {
	boolean f = false;
	for(DebugMessage msg : getdebuglog(gl)) {
	    System.err.printf("%d %d %d %d -- %s\n", msg.src, msg.type, msg.id, msg.sev, msg.msg);
	    f = true;
	}
	if(f)
	    System.err.println();
    }

    public void process(GL gl) {
	RenderQueue.Snapshot<GLRender> snap = queue.drain();
	Collection<GLRender> prep = snap.prep, copy = snap.submitted;
	try {
	    synchronized(drawmon) {
		checkqueries(gl);
		for(GLRender p : prep) {
		    try {
			if(p.gl != null) {
			    BufferBGL xf = new BufferBGL(16);
			    this.curstate.apply(xf, p.init);
			    xf.run(gl);
			    p.gl.run(gl);
			    this.curstate = p.state;
			    try {
				GLException.checkfor(gl, this);
			    } catch(Exception exc) {
				throw(new BGL.BGLException(p.gl, null, exc));
			    }
			}
		    } finally {
			p.dispose();
		    }
		}
		for(GLRender cmd : copy) {
		    BufferBGL xf = new BufferBGL(16);
		    this.curstate.apply(xf, cmd.init);
		    xf.run(gl);
		    cmd.gl.run(gl);
		    this.curstate = cmd.state;
		    try {
			GLException.checkfor(gl, this);
		    } catch(Exception exc) {
			throw(new BGL.BGLException(cmd.gl, null, exc));
		    }
		    cmd.dispose();
		}
		checkqueries(gl);
		disposeall().run(gl);
		clean();
		if(debuglog)
		    checkdebuglog(gl);
	    }
	} catch(Exception e) {
	    for(Throwable c = e; c != null; c = c.getCause()) {
		if(c instanceof GLException)
		    ((GLException)c).initenv(this);
	    }
	    throw(e);
	}
    }

    public void finish(GL gl) throws InterruptedException {
	synchronized(drawmon) {
	    gl.glFinish();
	    checkqueries(gl);
	    if(!queries.isEmpty())
		throw(new AssertionError("active queries left after glFinish"));
	    synccallbacks();
	}
    }

    public void submit(Render cmd) {
	if(!(cmd instanceof GLRender))
	    throw(new IllegalArgumentException("environment mismatch"));
	GLRender gcmd = (GLRender)cmd;
	if(gcmd.env != this)
	    throw(new IllegalArgumentException("environment mismatch"));
	if(gcmd.gl == null)
	    return;
	if(!queue.enqueueSubmitted(gcmd)) {
	    gcmd.gl.abort();
	    gcmd.dispose();
	}
    }

    public void submitwait() throws InterruptedException {
	queue.awaitSubmitted();
    }

    private BufferBGL disposeall() {
	int tail;
	synchronized(seqmon) {
	    tail = seqtail;
	}
	BufferBGL buf = new BufferBGL();
	Collection<GLObject> copy;
	synchronized(disposed) {
	    if(disposed.isEmpty())
		return(buf);
	    copy = new ArrayList<>(disposed.size());
	    for(Iterator<GLObject> i = disposed.iterator(); i.hasNext();) {
		GLObject obj = i.next();
		if(obj.dispseq - tail > 0)
		    break;
		copy.add(obj);
		i.remove();
	    }
	}
	for(GLObject obj : copy)
	    buf.bglDelete(obj);
	buf.bglCheckErr();
	return(buf);
    }

    public abstract SysBuffer malloc(int sz);
    public abstract SysBuffer subsume(ByteBuffer data, int sz);

    /* fillbuf is a pure factory: it always returns a FillBuffers.Array
     * sized for the requested range, regardless of any GL-side state of
     * the target. The STREAM upload path that wants to reuse a
     * StreamBuffer's transfer pool does not go through here -- it
     * pre-allocates a StreamBuffer.Fill and runs the Filler against a
     * proxy Environment (see runStreamFill). */
    public FillBuffer fillbuf(DataBuffer tgt, int from, int to) {
	return(new FillBuffers.Array(this, to - from));
    }

    public FillBuffer fillbuf(DataBuffer target) {
	if(target instanceof Texture.Image) {
	    /* XXX: This seems to be a buf with JOGL and buffer-space
	     * checking for mip-mapped 3D textures. This should be
	     * entirely unnecessary. */
	    Texture.Image<?> img = (Texture.Image<?>)target;
	    if(img.tex instanceof Texture3D) {
		if(img.size() < 14)
		    return(fillbuf(target, 0, 14));
	    }
	}
	return(Environment.super.fillbuf(target));
    }

    /* Build a one-shot prep render, run func against it, and enqueue it
     * for the next process() pass. Each prep call gets its own private
     * GLRender so callers don't share a multi-writer buffer. */
    private void enqprep(GLRender p) {
	if(p.gl == null) {
	    p.dispose();
	    return;
	}
	if(!queue.enqueuePrep(p)) {
	    p.gl.abort();
	    p.dispose();
	}
    }
    void prepare(GLObject obj) {
	GLRender p = new GLRender(this);
	p.gl().bglCreate(obj);
	enqprep(p);
    }
    void prepare(BGL.Request req) {
	GLRender p = new GLRender(this);
	p.gl().bglSubmit(req);
	enqprep(p);
    }
    void prepare(Consumer<GLRender> func) {
	GLRender p = new GLRender(this);
	func.accept(p);
	enqprep(p);
    }

    /* Run a Filler against a STREAM-backed buffer, writing directly into a
     * pre-allocated StreamBuffer.Fill so we avoid the FillBuffers.Array
     * allocation that env.fillbuf would otherwise hand out. We pass a
     * proxy Environment whose fillbuf returns the pre-allocated Fill for
     * the target buffer; standard Fillers call env.fillbuf(tgt) and then
     * write into the returned FillBuffer, so they end up writing straight
     * into the Fill. If a non-standard Filler bypasses env.fillbuf and
     * returns a different FillBuffer, we fall back to a copy.
     *
     * This decouples the STREAM upload path from buf.ro publication
     * order: callers may assign buf.ro after enqueueing the prep without
     * affecting which FillBuffer subtype the Filler receives. */
    <T extends DataBuffer> StreamBuffer.Fill runStreamFill(StreamBuffer ret, T buf, DataBuffer.Filler<? super T> init) {
	StreamBuffer.Fill fill = ret.new Fill();
	StreamFiller.runWithPreallocated(this, buf, buf.size(), init, fill, fill::pull);
	return(fill);
    }

    Disposable prepare(Model.Indices buf) {
	synchronized(buf) {
	    switch(buf.usage) {
	    case EPHEMERAL: {
		if(!(buf.ro instanceof HeapBuffer)) {
		    if(buf.ro != null)
			buf.ro.dispose();
		    buf.ro = new HeapBuffer(this, buf, buf.init);
		}
		return(buf.ro);
	    }
	    case STREAM: {
		StreamBuffer ret;
		if(((ret = GLReference.get(buf.ro, StreamBuffer.class)) == null) || (ret.rbuf.env != this)) {
		    Disposable old = buf.ro;
		    ret = new StreamBuffer(this, buf.size());
		    StreamBuffer.Fill data = (buf.init == null) ? null : runStreamFill(ret, buf, buf.init);
		    StreamBuffer jdret = ret;
		    GLBuffer rbuf = ret.rbuf;
		    /* Enqueue the data-store write before publishing buf.ro so
		     * any concurrent reader that observes the new ro also sees
		     * the upload sitting ahead of any render they later submit. */
		    prepare((GLRender g) -> {
			    BGL gl = g.gl();
			    Vao0State.apply(this, gl, g.state, rbuf);
			    if(data == null) {
				gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, buf.size(), null, GL.GL_DYNAMIC_DRAW);
			    } else {
				ByteBuffer xfbuf = data.get();
				gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, buf.size(), xfbuf, GL.GL_DYNAMIC_DRAW);
				jdret.put(gl, xfbuf);
			    }
			    if(labels && (buf.desc != null))
				gl.glObjectLabel(GL.GL_BUFFER, rbuf, String.valueOf(buf.desc));
			    rbuf.setmem(MemStats.INDICES, buf.size());
			});
		    if(old != null)
			old.dispose();
		    buf.ro = new GLReference<>(ret);
		}
		return(ret);
	    }
	    case STATIC: {
		GLBuffer ret;
		if(((ret = GLReference.get(buf.ro, GLBuffer.class)) == null) || (ret.env != this)) {
		    Disposable old = buf.ro;
		    ret = new GLBuffer(this);
		    FillBuffers.Array data = (buf.init == null) ? null : (FillBuffers.Array)buf.init.fill(buf, this);
		    GLBuffer jdret = ret;
		    prepare((GLRender g) -> {
			    BGL gl = g.gl();
			    Vao0State.apply(this, gl, g.state, jdret);
			    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, buf.size(), (data == null) ? null : data.data(), GL.GL_STATIC_DRAW);
			    if(labels && (buf.desc != null))
				gl.glObjectLabel(GL.GL_BUFFER, jdret, String.valueOf(buf.desc));
			    jdret.setmem(MemStats.INDICES, buf.size());
			    if(data != null) data.dispose();
			});
		    if(old != null)
			old.dispose();
		    buf.ro = new GLReference<>(ret);
		}
		return(ret);
	    }
	    default:
		throw(new Error());
	    }
	}
    }
    Disposable prepare(VertexArray.Buffer buf) {
	synchronized(buf) {
	    switch(buf.usage) {
	    case EPHEMERAL: {
		if(!(buf.ro instanceof HeapBuffer)) {
		    if(buf.ro != null)
			buf.ro.dispose();
		    buf.ro = new HeapBuffer(this, buf, buf.init);
		}
		return(buf.ro);
	    }
	    case STREAM: {
		StreamBuffer ret;
		if(((ret = GLReference.get(buf.ro, StreamBuffer.class)) == null) || (ret.rbuf.env != this)) {
		    Disposable old = buf.ro;
		    ret = new StreamBuffer(this, buf.size());
		    StreamBuffer.Fill data = (buf.init == null) ? null : runStreamFill(ret, buf, buf.init);
		    StreamBuffer jdret = ret;
		    GLBuffer rbuf = ret.rbuf;
		    /* Enqueue the data-store write before publishing buf.ro so
		     * any concurrent reader that observes the new ro also sees
		     * the upload sitting ahead of any render they later submit. */
		    prepare((GLRender g) -> {
			    BGL gl = g.gl();
			    VboState.apply(gl, g.state, rbuf);
			    if(data == null) {
				gl.glBufferData(GL.GL_ARRAY_BUFFER, buf.size(), null, GL.GL_DYNAMIC_DRAW);
			    } else {
				ByteBuffer xfbuf = data.get();
				gl.glBufferData(GL.GL_ARRAY_BUFFER, buf.size(), xfbuf, GL.GL_DYNAMIC_DRAW);
				jdret.put(gl, xfbuf);
			    }
			    if(labels && (buf.desc != null))
				gl.glObjectLabel(GL.GL_BUFFER, rbuf, String.valueOf(buf.desc));
			    rbuf.setmem(MemStats.VERTICES, buf.size());
			});
		    if(old != null)
			old.dispose();
		    buf.ro = new GLReference<>(ret);
		}
		return(ret);
	    }
	    case STATIC: {
		GLBuffer ret;
		if(((ret = GLReference.get(buf.ro, GLBuffer.class)) == null) || (ret.env != this)) {
		    Disposable old = buf.ro;
		    ret = new GLBuffer(this);
		    FillBuffers.Array data = (buf.init == null) ? null : (FillBuffers.Array)buf.init.fill(buf, this);
		    GLBuffer jdret = ret;
		    prepare((GLRender g) -> {
			    BGL gl = g.gl();
			    VboState.apply(gl, g.state, jdret);
			    gl.glBufferData(GL.GL_ARRAY_BUFFER, buf.size(), (data == null) ? null : data.data(), GL.GL_STATIC_DRAW);
			    if(labels && (buf.desc != null))
				gl.glObjectLabel(GL.GL_BUFFER, jdret, String.valueOf(buf.desc));
			    jdret.setmem(MemStats.VERTICES, buf.size());
			    if(data != null) data.dispose();
			});
		    if(old != null)
			old.dispose();
		    buf.ro = new GLReference<>(ret);
		}
		return(ret);
	    }
	    default:
		throw(new Error());
	    }
	}
    }
    GLVertexArray prepare(Model mod, GLProgram prog) {
	synchronized(mod) {
	    GLVertexArray.ProgIndex idx;
	    if(((idx = GLReference.get(mod.ro, GLVertexArray.ProgIndex.class)) == null) || (idx.env != this)) {
		if(mod.ro != null)
		    mod.ro.dispose();
		mod.ro = new GLReference<>(idx = new GLVertexArray.ProgIndex(this, mod));
	    }
	    return(idx.get(prog, mod));
	}
    }
    GLTexture.Tex2D prepare(Texture2D tex) {
	synchronized(tex) {
	    GLTexture.Tex2D ret;
	    if(((ret = GLReference.get(tex.ro, GLTexture.Tex2D.class)) == null) || (ret.env != this)) {
		if(tex.ro != null)
		    tex.ro.dispose();
		tex.ro = new GLReference<>(ret = GLTexture.Tex2D.create(this, tex));
	    }
	    return(ret);
	}
    }
    GLTexture.Tex2D prepare(Texture2D.Sampler2D smp) {
	Texture2D tex = smp.tex;
	synchronized(tex) {
	    GLTexture.Tex2D ret = prepare(tex);
	    ret.setsampler(smp);
	    return(ret);
	}
    }
    GLTexture.Tex3D prepare(Texture3D tex) {
	synchronized(tex) {
	    GLTexture.Tex3D ret;
	    if(((ret = GLReference.get(tex.ro, GLTexture.Tex3D.class)) == null) || (ret.env != this)) {
		if(tex.ro != null)
		    tex.ro.dispose();
		tex.ro = new GLReference<>(ret = GLTexture.Tex3D.create(this, tex));
	    }
	    return(ret);
	}
    }
    GLTexture.Tex3D prepare(Texture3D.Sampler3D smp) {
	Texture3D tex = smp.tex;
	synchronized(tex) {
	    GLTexture.Tex3D ret = prepare(tex);
	    ret.setsampler(smp);
	    return(ret);
	}
    }
    GLTexture.Tex2DArray prepare(Texture2DArray tex) {
	synchronized(tex) {
	    GLTexture.Tex2DArray ret;
	    if(((ret = GLReference.get(tex.ro, GLTexture.Tex2DArray.class)) == null) || (ret.env != this)) {
		if(tex.ro != null)
		    tex.ro.dispose();
		tex.ro = new GLReference<>(ret = GLTexture.Tex2DArray.create(this, tex));
	    }
	    return(ret);
	}
    }
    GLTexture.Tex2DArray prepare(Texture2DArray.Sampler2DArray smp) {
	Texture2DArray tex = smp.tex;
	synchronized(tex) {
	    GLTexture.Tex2DArray ret = prepare(tex);
	    ret.setsampler(smp);
	    return(ret);
	}
    }
    GLTexture.Tex2DMS prepare(Texture2DMS tex) {
	synchronized(tex) {
	    GLTexture.Tex2DMS ret;
	    if(((ret = GLReference.get(tex.ro, GLTexture.Tex2DMS.class)) == null) || (ret.env != this)) {
		if(tex.ro != null)
		    tex.ro.dispose();
		tex.ro = new GLReference<>(ret = GLTexture.Tex2DMS.create(this, tex));
	    }
	    return(ret);
	}
    }
    GLTexture.Tex2DMS prepare(Texture2DMS.Sampler2DMS smp) {
	Texture2DMS tex = smp.tex;
	synchronized(tex) {
	    GLTexture.Tex2DMS ret = prepare(tex);
	    ret.setsampler(smp);
	    return(ret);
	}
    }
    GLTexture.TexCube prepare(TextureCube tex) {
	synchronized(tex) {
	    GLTexture.TexCube ret;
	    if(((ret = GLReference.get(tex.ro, GLTexture.TexCube.class)) == null) || (ret.env != this)) {
		if(tex.ro != null)
		    tex.ro.dispose();
		tex.ro = new GLReference<>(ret = GLTexture.TexCube.create(this, tex));
	    }
	    return(ret);
	}
    }
    GLTexture.TexCube prepare(TextureCube.SamplerCube smp) {
	TextureCube tex = smp.tex;
	synchronized(tex) {
	    GLTexture.TexCube ret = prepare(tex);
	    ret.setsampler(smp);
	    return(ret);
	}
    }

    Object prepuval(Object val) {
	if(val instanceof Texture.Sampler) {
	    if(val instanceof Texture2D.Sampler2D)
		return(prepare((Texture2D.Sampler2D)val));
	    if(val instanceof Texture3D.Sampler3D)
		return(prepare((Texture3D.Sampler3D)val));
	    if(val instanceof Texture2DArray.Sampler2DArray)
		return(prepare((Texture2DArray.Sampler2DArray)val));
	    if(val instanceof Texture2DMS.Sampler2DMS)
		return(prepare((Texture2DMS.Sampler2DMS)val));
	    if(val instanceof TextureCube.SamplerCube)
		return(prepare((TextureCube.SamplerCube)val));
	}
	return(val);
    }

    Object prepfval(Object val) {
	if(val instanceof Texture.Image)
	    return(GLFrameBuffer.prepimg(this, (Texture.Image)val));
	return(val);
    }

    public class TempData<T> implements Supplier<T> {
	private final Supplier<T> bk;
	private T d = null;

	public TempData(Supplier<T> bk) {this.bk = bk;}

	public T get() {
	    if(d == null) {
		synchronized(this) {
		    if(d == null)
			d = bk.get();
		}
	    }
	    return(d);
	}
    }

    public final Supplier<GLVertexArray> tempvao = new TempData<>(() -> new GLVertexArray(this));
    public final Supplier<GLBuffer> tempvertex = new TempData<>(() -> new GLBuffer(this));
    public final Supplier<GLBuffer> tempindex = new TempData<>(() -> new GLBuffer(this));

    static class SavedProg {
	final int hash;
	final ShaderMacro[] shaders;
	final GLProgram prog;
	SavedProg next;
	boolean used = true;

	SavedProg(int hash, ShaderMacro[] shaders, GLProgram prog) {
	    this.hash = hash;
	    this.shaders = Arrays.copyOf(shaders, shaders.length);
	    this.prog = prog;
	}
    }

    private final Object pmon = new Object();
    private SavedProg[] ptab = new SavedProg[32];
    private int nprog = 0;
    private SavedProg findprog(int hash, ShaderMacro[] shaders) {
	int idx = hash & (ptab.length - 1);
	outer: for(SavedProg s = ptab[idx]; s != null; s = s.next) {
	    if(s.hash != hash)
		continue;
	    ShaderMacro[] a, b;
	    if(shaders.length < s.shaders.length) {
		a = shaders; b = s.shaders;
	    } else {
		a = s.shaders; b = shaders;
	    }
	    int i = 0;
	    for(; i < a.length; i++) {
		if(a[i] != b[i])
		    continue outer;
	    }
	    for(; i < b.length; i++) {
		if(b[i] != null)
		    continue outer;
	    }
	    return(s);
	}
	return(null);
    }

    private void rehash(int nlen) {
	SavedProg[] ntab = new SavedProg[nlen];
	for(int i = 0; i < ptab.length; i++) {
	    while(ptab[i] != null) {
		SavedProg s = ptab[i];
		ptab[i] = s.next;
		int ni = s.hash & (nlen - 1);
		s.next = ntab[ni];
		ntab[ni] = s;
	    }
	}
	ptab = ntab;
    }

    private void putprog(int hash, ShaderMacro[] shaders, GLProgram prog) {
	int idx = hash & (ptab.length - 1);
	SavedProg save = new SavedProg(hash, shaders, prog);
	save.next = ptab[idx];
	ptab[idx] = save;
	nprog++;
	if(nprog > ptab.length)
	    rehash(ptab.length * 2);
    }

    public GLProgram getprog(int hash, ShaderMacro[] shaders) {
	synchronized(pmon) {
	    SavedProg s = findprog(hash, shaders);
	    if(s != null) {
		s.used = true;
		return(s.prog);
	    }
	}
	Collection<ShaderMacro> mods = new LinkedList<>();
	for(int i = 0; i < shaders.length; i++) {
	    if(shaders[i] != null)
		mods.add(shaders[i]);
	}
	GLProgram prog = GLProgram.build(this, mods);
	synchronized(pmon) {
	    SavedProg s = findprog(hash, shaders);
	    if(s != null) {
		prog.dispose();
		s.used = true;
		return(s.prog);
	    }
	    putprog(hash, shaders, prog);
	    return(prog);
	}
    }

    private void cleanprogs() {
	synchronized(pmon) {
	    for(int i = 0; i < ptab.length; i++) {
		SavedProg c, p;
		for(c = ptab[i], p = null; c != null; c = c.next) {
		    int rc = c.prog.locked.get();
		    if(c.used || (rc > 0)) {
			if(rc < 1)
			    c.used = false;
			p = c;
		    } else {
			if(p == null)
			    ptab[i] = c.next;
			else
			    p.next = c.next;
			c.prog.dispose();
			nprog--;
		    }
		}
	    }
	    /* XXX: Rehash into smaller table? It's probably not a
	     * problem, but it might be nice just for
	     * completeness. */
	}
    }

    public Object progdump() {
	HashMap<String, Object> ret = new HashMap<>();
	synchronized(pmon) {
	    int seq = 0;
	    for(int i = 0; i < ptab.length; i++) {
		for(SavedProg p = ptab[i]; p != null; p = p.next) {
		    ret.put(String.format("p%d-idx", seq), i);
		    ret.put(String.format("p%d-hash", seq), p.hash);
		    ret.put(String.format("p%d-rc", seq), p.prog.locked.get());
		    ret.put(String.format("p%d-id", seq), System.identityHashCode(p.prog));
		    List<String> macros = new ArrayList<>();
		    List<Integer> macroi = new ArrayList<>();
		    for(int o = 0; o < p.shaders.length; o++) {
			macros.add(String.valueOf(p.shaders[o]));
			macroi.add(System.identityHashCode(p.shaders[o]));
		    }
		    ret.put(String.format("p%d-mac", seq), macros);
		    ret.put(String.format("p%d-macid", seq), macroi);
		    seq++;
		}
	    }
	}
	return(ret);
    }

    public boolean compatible(DrawList ob) {
	return((ob instanceof GLDrawList) && (((GLDrawList)ob).env == this));
    }

    public boolean compatible(Texture ob) {
	GLObject ro = GLReference.get(ob.ro, GLObject.class);
	return((ro != null) && (ro.env == this));
    }

    public boolean compatible(DataBuffer ob) {
	if(ob instanceof Model.Indices) {
	    Disposable ro = GLReference.get(((Model.Indices)ob).ro, Disposable.class);
	    if(ro instanceof StreamBuffer) ro = ((StreamBuffer)ro).rbuf;
	    return((ro != null) && (ro instanceof GLObject) && (((GLObject)ro).env == this));
	} else if(ob instanceof VertexArray.Buffer) {
	    Disposable ro = GLReference.get(((VertexArray.Buffer)ob).ro, Disposable.class);
	    if(ro instanceof StreamBuffer) ro = ((StreamBuffer)ro).rbuf;
	    return((ro != null) && (ro instanceof GLObject) && (((GLObject)ro).env == this));
	} else {
	    throw(new NotImplemented());
	}
    }

    private double lastpclean = Utils.rtime();
    public void clean() {
	double now = Utils.rtime();
	if(now - lastpclean > 60) {
	    cleanprogs();
	    lastpclean = now;
	}
    }

    private final Object seqmon = new Object();
    /* Initial size sized to observed steady-state (~32k in-flight
     * Sequences during normal rendering) so seqresize() doesn't fire
     * during warm-up. */
    private boolean[] sequse = new boolean[0x8000];
    private int seqhead = 1, seqtail = seqhead;

    private void seqresize(int nsz) {
	boolean[] cseq = sequse, nseq = new boolean[nsz];
	int csz = cseq.length;
	for(int i = 0; i < csz; i++)
	    nseq[(seqtail + i) & (nsz - 1)] = cseq[(seqtail + i) & (csz - 1)];
	sequse = nseq;
	/* Warn at 4x observed steady-state -- a tight enough margin to
	 * surface real leaks while leaving headroom for transient spikes. */
	if(nsz >= 0x20000)
	    Warning.warn("warning: dispose queue size increased to " + nsz);
    }

    int seqreg() {
	synchronized(seqmon) {
	    int seq = seqhead;
	    if(++seqhead == 0)
		seqhead = 1;
	    if(seqhead - seqtail == sequse.length - 1)
		seqresize(sequse.length << 1);
	    sequse[seq & (sequse.length - 1)] = true;
	    return(seq);
	}
    }

    void sequnreg(int seq) {
	if(seq == 0)
	    return;
	synchronized(seqmon) {
	    int m = sequse.length - 1;
	    int si = seq & m;
	    if(!sequse[si])
		throw(new AssertionError());
	    sequse[si] = false;
	    if(seq == seqtail) {
		while((seqhead - seqtail > 0) && !sequse[seqtail & m])
		    seqtail++;
	    }
	}
    }

    int dispseq() {
	synchronized(seqmon) {
	    return(seqhead);
	}
    }

    class Sequence implements Disposable {
	public final int no;
	private final Runnable clean;
	private final String desc;
	private volatile boolean cleaned = false;

	Sequence(Object owner) {
	    this.desc = owner.toString();
	    this.no = seqreg();
	    clean = Finalizer.finalize(owner, this::disposed);
	}

	private void disposed() {
	    sequnreg(no);
	    if(!cleaned) {
		Warning.warn("warning: disposal sequence leaked: " + desc);
	    }
	}

	public void dispose() {
	    cleaned = true;
	    clean.run();
	}
    }

    public int numprogs() {return(nprog);}
    public Caps caps() {return(caps);}

    public String memstats() {
	StringBuilder buf = new StringBuilder();
	MemStats[] sta = MemStats.values();
	for(int i = 0; i < sta.length; i++) {
	    if(i > 0)
		buf.append(" / ");
	    buf.append(String.format("%c %,d (%,d)", sta[i].name().charAt(0), stats_mem[i], stats_obj[i]));
	}
	return(buf.toString());
    }

    public void dispose() {
	queue.invalidate();
	RenderQueue.Snapshot<GLRender> snap = queue.drain();
	for(GLRender cmd : snap.submitted) {
	    cmd.gl.abort();
	    cmd.dispose();
	}
	for(GLRender p : snap.prep) {
	    if(p.gl != null)
		p.gl.abort();
	    p.dispose();
	}
	{
	    Collection<GLQuery> copy;
	    synchronized(drawmon) {
		copy = new ArrayList<>(queries);
		queries.clear();
	    }
	    for(GLQuery query : copy)
		query.abort();
	}
    }
}
