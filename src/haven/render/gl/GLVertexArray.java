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
import javax.media.opengl.*;
import haven.Disposable;
import haven.render.*;
import haven.render.sl.Type;
import static haven.render.DataBuffer.Usage.*;

public class GLVertexArray extends GLObject implements BGL.ID {
    private int id, state = 0;
    
    public GLVertexArray(GLEnvironment env) {
	super(env);
	env.prepare(this);
    }

    public void create(GL3 gl) {
	ckstate(state, 0);
	int[] buf = new int[1];
	gl.glGenVertexArrays(1, buf, 0);
	this.id = buf[0];
	state = 1;
	setmem(GLEnvironment.MemStats.VAOS, 0);
    }

    protected void delete(GL3 gl) {
	ckstate(state, 1);
	gl.glDeleteVertexArrays(1, new int[] {id}, 0);
	state = 2;
    }

    public int glid() {
	ckstate(state, 1);
	return(id);
    }

    public String toString() {
	return(String.format("#<gl.vao %d>", id));
    }

    static boolean ephemeralp(VertexArray va) {
	for(VertexArray.Buffer b : va.bufs) {
	    if(b.usage == EPHEMERAL)
		return(true);
	}
	return(false);
    }
    static boolean ephemeralp(Model m) {
	if((m.ind != null) && (m.ind.usage == EPHEMERAL))
	    return(true);
	if(ephemeralp(m.va))
	    return(true);
	return(false);
    }

    public static GLVertexArray create(GLProgram prog, Model mod) {
	if(ephemeralp(mod))
	    throw(new RuntimeException("got ephemeral model for VAO"));
	GLEnvironment env = prog.env;
	GLBuffer bufs[] = new GLBuffer[mod.va.bufs.length];
	for(int i = 0; i < bufs.length; i++) {
	    Disposable ro = env.prepare(mod.va.bufs[i]);
	    if(ro instanceof StreamBuffer)
		bufs[i] = ((StreamBuffer)ro).rbuf;
	    else
		bufs[i] = (GLBuffer)ro;
	}
	GLBuffer ebo;
	if(mod.ind == null) {
	    ebo = null;
	} else {
	    Disposable ro = env.prepare(mod.ind);
	    if(ro instanceof StreamBuffer)
		ebo = ((StreamBuffer)ro).rbuf;
	    else
		ebo = (GLBuffer)ro;
	}
	GLVertexArray vao = new GLVertexArray(env);
	env.prepare((GLRender g) -> {
		BGL gl = g.gl();
		VaoBindState.apply(gl, g.state, vao, ebo);
		if(ebo != null) {
		    // Rendundant with BindBuffer in VaoBindState, but
		    // only so long as DO_GL_EBO_FIXUP is true.
		    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ebo);
		}
		for(int i = 0; i < mod.va.fmt.inputs.length; i++) {
		    VertexArray.Layout.Input attr = mod.va.fmt.inputs[i];
		    GLProgram.VarID var = prog.cattrib(attr.tgt);
		    if(var != null) {
			VboState.apply(gl, g.state, bufs[attr.buf]);
			int na = 1, vo = 0;
			VectorFormat vfmt = attr.el;
			if(attr.tgt.type == Type.MAT4) {
			    if((attr.el.nc != 16) || (attr.el.cf != NumberFormat.FLOAT32))
				throw(new RuntimeException("unexpected mat4 vertex format: " + attr.el));
			    na = 4;
			    vo = 16;
			    vfmt = new VectorFormat(4, NumberFormat.FLOAT32);
			} else if(attr.tgt.type == Type.MAT3) {
			    if((attr.el.nc != 9) || (attr.el.cf != NumberFormat.FLOAT32))
				throw(new RuntimeException("unexpected mat3 vertex format: " + attr.el));
			    na = 3;
			    vo = 12;
			    vfmt = new VectorFormat(3, NumberFormat.FLOAT32);
			}
			if(na == 1) {
			    gl.glVertexAttribPointer(var, attr.el.nc, GLRender.glattribfmt(attr.el.cf), GLRender.glattribnorm(attr.el.cf), attr.stride, attr.offset);
			    gl.glEnableVertexAttribArray(var);
			    if(attr.instanced)
				gl.glVertexAttribDivisor(var, 1);
			} else {
			    for(int v = 0; v < na; v++) {
				gl.glVertexAttribPointer(var, v, vfmt.nc, GLRender.glattribfmt(vfmt.cf), GLRender.glattribnorm(vfmt.cf), attr.stride, attr.offset + (v * vo));
				gl.glEnableVertexAttribArray(var, v);
				if(attr.instanced)
				    gl.glVertexAttribDivisor(var, v, 1);
			    }
			}
		    }
		}
	    });
	return(vao);
    }

    static class ProgIndex implements Disposable {
	final Model mod;
	final GLEnvironment env;
	GLVertexArray[] vaos = new GLVertexArray[2];
	GLProgram[] progs = new GLProgram[2];
	int n = 0;

	ProgIndex(Model mod, GLEnvironment env) {
	    this.mod = mod;
	    this.env = env;
	}

	void add(GLProgram prog, GLVertexArray vao) {
	    if(vaos.length <= n) {
		vaos = Arrays.copyOf(vaos, vaos.length * 2);
		progs = Arrays.copyOf(progs, progs.length * 2);
	    }
	    vaos[n] = vao;
	    progs[n] = prog;
	    n++;
	}

	void clean() {
	    int o = 0;
	    for(int i = 0; i < n; i++) {
		if(!progs[i].disposed) {
		    progs[o] = progs[i];
		    vaos[o] = vaos[i];
		    o++;
		} else {
		    vaos[o].dispose();
		}
	    }
	    for(int i = o; i < n; i++) {
		progs[i] = null;
		vaos[i] = null;
	    }
	    n = o;
	}

	GLVertexArray get(GLProgram prog) {
	    GLVertexArray ret = null;
	    boolean clean = false;
	    for(int i = 0; i < n; i++) {
		if(progs[i].disposed)
		    clean = true;
		if(progs[i] == prog) {
		    ret = vaos[i];
		    break;
		}
	    }
	    if(ret == null) {
		ret = create(prog, mod);
		add(prog, ret);
	    }
	    if(clean) {
		/* XXX? It would be nice if VAOs could be cleaned out
		 * when programs actually go away rather than when
		 * being re-requested. */
		clean();
	    }
	    return(ret);
	}

	public void dispose() {
	    for(int i = 0; (i < vaos.length) && (vaos[i] != null); i++)
		vaos[i].dispose();
	}
    }
}
