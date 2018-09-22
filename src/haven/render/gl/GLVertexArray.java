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
import static haven.render.DataBuffer.Usage.*;

public class GLVertexArray extends GLObject implements BGL.ID {
    private int id;
    
    public GLVertexArray(GLEnvironment env) {
	super(env);
	env.prepare(this);
    }

    public void create(GL2 gl) {
	int[] buf = new int[1];
	gl.glGenVertexArrays(1, buf, 0);
	this.id = buf[0];
    }

    protected void delete(BGL gl) {
	BGL.ID[] buf = {this};
	gl.glDeleteVertexArrays(1, buf, 0);
    }

    public int glid() {
	return(id);
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
	for(int i = 0; i < bufs.length; i++)
	    bufs[i] = (GLBuffer)env.prepare(mod.va.bufs[i]);
	GLBuffer ebo = (mod.ind == null) ? null : (GLBuffer)env.prepare(mod.ind);
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
			gl.glVertexAttribPointer(var, attr.el.nc, GLRender.glattribfmt(attr.el.cf), GLRender.glattribnorm(attr.el.cf), attr.stride, attr.offset);
			gl.glEnableVertexAttribArray(var);
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
	}

	GLVertexArray get(GLProgram prog) {
	    for(int i = 0; (i < progs.length) && (progs[i] != null); i++) {
		if(progs[i] == prog)
		    return(vaos[i]);
	    }
	    GLVertexArray ret = create(prog, mod);
	    add(prog, ret);
	    return(ret);
	}

	public void dispose() {
	    for(int i = 0; (i < vaos.length) && (vaos[i] != null); i++)
		vaos[i].dispose();
	}
    }
}
