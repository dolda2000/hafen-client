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
import javax.media.opengl.*;
import haven.*;
import haven.render.*;
import haven.render.sl.*;
import static haven.render.DataBuffer.Usage.*;

public class GLEnvironment implements Environment {
    public final GLContext ctx;
    final Object drawmon = new Object();
    final Object prepmon = new Object();
    final Collection<GLObject> disposed = new LinkedList<>();
    final List<GLQuery> queries = new LinkedList<>(); // Synchronized on drawmon
    Area wnd;
    private GLRender prep = null;
    private Applier curstate = new Applier(this);

    public GLEnvironment(GLContext ctx, Area wnd) {
	this.ctx = ctx;
	this.wnd = wnd;
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

    private void checkqueries(GL2 gl) {
	for(Iterator<GLQuery> i = queries.iterator(); i.hasNext();) {
	    GLQuery query = i.next();
	    if(!query.check(gl))
		continue;
	    query.dispose();
	    i.remove();
	}
    }

    public void submit(GL2 gl, GLRender cmd) {
	if(cmd.gl != null) {
	    GLRender prep;
	    synchronized(prepmon) {
		prep = this.prep;
		this.prep = null;
	    }
	    synchronized(drawmon) {
		checkqueries(gl);
		if((prep != null) && (prep.gl != null)) {
		    BufferBGL xf = new BufferBGL(16);
		    this.curstate.apply(xf, prep.init);
		    prep.gl.run(gl);
		    this.curstate = prep.state;
		    GLException.checkfor(gl);
		}
		BufferBGL xf = new BufferBGL(16);
		this.curstate.apply(xf, cmd.init);
		xf.run(gl);
		cmd.gl.run(gl);
		this.curstate = cmd.state;
		GLException.checkfor(gl);
		checkqueries(gl);
	    }
	}
    }

    public BufferBGL disposeall() {
	BufferBGL buf = new BufferBGL();
	Collection<GLObject> copy;
	synchronized(disposed) {
	    if(disposed.isEmpty())
		return(buf);
	    copy = new ArrayList<>(disposed);
	    disposed.clear();
	}
	for(GLObject obj : copy)
	    obj.delete(buf);
	buf.bglCheckErr();
	return(buf);
    }

    public FillBuffer fillbuf(DataBuffer tgt) {
	return(new FillBuffers.Array(tgt.size()));
    }

    GLRender prepare() {
	if(prep == null)
	    prep = new GLRender(this);
	return(prep);
    }
    void prepare(GLObject obj) {
	synchronized(prepmon) {
	    prepare().gl().bglCreate(obj);
	}
    }
    void prepare(BGL.Request req) {
	synchronized(prepmon) {
	    prepare().gl().bglSubmit(req);
	}
    }
    void prepare(Consumer<GLRender> func) {
	synchronized(prepmon) {
	    func.accept(prepare());
	}
    }

    Disposable prepare(Model.Indices buf) {
	synchronized(buf) {
	    switch(buf.usage) {
	    case EPHEMERAL:
		if(!(buf.ro instanceof HeapBuffer)) {
		    if(buf.ro != null)
			buf.ro.dispose();
		    buf.ro = new HeapBuffer(this, buf, buf.init);
		}
		return(buf.ro);
	    case STREAM:
	    case STATIC:
		GLBuffer ret;
		if(!(buf.ro instanceof GLBuffer) || ((ret = ((GLBuffer)buf.ro)).env != this)) {
		    if(buf.ro != null)
			buf.ro.dispose();
		    buf.ro = ret = new GLBuffer(this);
		    if(buf.init != null) {
			FillBuffers.Array data = (FillBuffers.Array)buf.init.fill(buf, this);
			GLBuffer jdret = ret;
			prepare((GLRender g) -> {
				BGL gl = g.gl();
				Vao0State.apply(gl, g.state, jdret);
				int usage = (buf.usage == STREAM) ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW;
				gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, buf.size(), ByteBuffer.wrap(data.data), usage);
			    });
		    }
		}
		return(ret);
	    default:
		throw(new Error());
	    }
	}
    }
    Disposable prepare(VertexArray.Buffer buf) {
	synchronized(buf) {
	    switch(buf.usage) {
	    case EPHEMERAL:
		if(!(buf.ro instanceof HeapBuffer)) {
		    if(buf.ro != null)
			buf.ro.dispose();
		    buf.ro = new HeapBuffer(this, buf, buf.init);
		}
		return(buf.ro);
	    case STREAM:
	    case STATIC:
		GLBuffer ret;
		if(!(buf.ro instanceof GLBuffer) || ((ret = ((GLBuffer)buf.ro)).env != this)) {
		    if(buf.ro != null)
			buf.ro.dispose();
		    buf.ro = ret = new GLBuffer(this);
		    if(buf.init != null) {
			FillBuffers.Array data = (FillBuffers.Array)buf.init.fill(buf, this);
			GLBuffer jdret = ret;
			prepare((GLRender g) -> {
				BGL gl = g.gl();
				VboState.apply(gl, g.state, jdret);
				int usage = (buf.usage == STREAM) ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW;
				gl.glBufferData(GL.GL_ARRAY_BUFFER, buf.size(), ByteBuffer.wrap(data.data), usage);
			    });
		    }
		}
		return(ret);
	    default:
		throw(new Error());
	    }
	}
    }
    GLVertexArray prepare(Model mod, GLProgram prog) {
	synchronized(mod) {
	    GLVertexArray.ProgIndex idx;
	    if(!(mod.ro instanceof GLVertexArray.ProgIndex) || ((idx = ((GLVertexArray.ProgIndex)mod.ro)).env != this)) {
		if(mod.ro != null)
		    mod.ro.dispose();
		mod.ro = idx = new GLVertexArray.ProgIndex(mod, this);
	    }
	    return(idx.get(prog));
	}
    }
    GLTexture.Tex2D prepare(Texture2D tex) {
	synchronized(tex) {
	    GLTexture.Tex2D ret;
	    if(!(tex.ro instanceof GLTexture.Tex2D) || ((ret = (GLTexture.Tex2D)tex.ro).env != this)) {
		if(tex.ro != null)
		    tex.ro.dispose();
		tex.ro = ret = GLTexture.Tex2D.create(this, tex);
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

    Object prepuval(Object val) {
	if(val instanceof Texture.Sampler) {
	    if(val instanceof Texture2D.Sampler2D)
		return(prepare((Texture2D.Sampler2D)val));
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
		    if(c.used || (c.prog.locked.get() > 0)) {
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

    private double lastpclean = Utils.rtime();
    public void clean() {
	double now = Utils.rtime();
	if(now - lastpclean > 60) {
	    cleanprogs();
	    lastpclean = now;
	}
    }

    public int numprogs() {return(nprog);}
}
