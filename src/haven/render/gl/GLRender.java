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

import java.nio.*;
import javax.media.opengl.*;
import haven.Disposable;
import haven.render.*;
import static haven.render.DataBuffer.Usage.*;

public class GLRender implements Render {
    public final GLEnvironment env;
    BufferBGL gl = null;
    final Applier state;
    Applier init = null;

    GLRender(GLEnvironment env) {
	this.env = env;
	this.state = new Applier(env);
    }

    public GLEnvironment env() {return(env);}

    private BGL gl() {
	if(this.gl == null) {
	    this.gl = new BufferBGL();
	    this.init = state.clone();
	}
	return(this.gl);
    }

    static boolean ephemeralp(Model m) {
	if((m.ind != null) && (m.ind.usage == EPHEMERAL))
	    return(true);
	for(VertexArray.Buffer b : m.va.bufs) {
	    if(b.usage == EPHEMERAL)
		return(true);
	}
	return(false);
    }

    private static int glmode(Model.Mode mode) {
	switch(mode) {
	case POINTS:          return(GL.GL_POINTS);
	case LINES:           return(GL.GL_LINES);
	case LINE_STRIP:      return(GL.GL_LINE_STRIP);
	case TRIANGLES:       return(GL.GL_TRIANGLES);
	case TRIANGLE_STRIP:  return(GL.GL_TRIANGLE_STRIP);
	case TRIANGLE_FAN:    return(GL.GL_TRIANGLE_FAN);
	default:
	    throw(new RuntimeException("unimplemented draw mode " + mode));
	}
    }

    private static int glattribfmt(NumberFormat fmt) {
	switch(fmt) {
	case UNORM8:    return(GL.GL_UNSIGNED_BYTE);
	case SNORM8:    return(GL.GL_BYTE);
	case UNORM16:   return(GL.GL_UNSIGNED_SHORT);
	case SNORM16:   return(GL.GL_SHORT);
	case UNORM32:   return(GL2.GL_UNSIGNED_INT);
	case SNORM32:   return(GL2.GL_INT);
	case FLOAT16:   return(GL.GL_HALF_FLOAT);
	case FLOAT32:   return(GL.GL_FLOAT);
	case UINT8:     return(GL.GL_UNSIGNED_BYTE);
	case SINT8:     return(GL.GL_BYTE);
	case UINT16:    return(GL.GL_UNSIGNED_SHORT);
	case SINT16:    return(GL.GL_SHORT);
	case UINT32:    return(GL2.GL_UNSIGNED_INT);
	case SINT32:    return(GL2.GL_INT);
	default:
	    throw(new RuntimeException("unimplemented vertex attribute format " + fmt));
	}
    }

    private static boolean glattribnorm(NumberFormat fmt) {
	switch(fmt) {
	case UNORM8:
	case SNORM8:
	case UNORM16:
	case SNORM16:
	case UNORM32:
	case SNORM32:
	    return(true);
	default:
	    return(false);
	}
    }

    public void draw(Pipe pipe, Model data) {
	state.apply(this.gl, pipe);
	if(ephemeralp(data)) {
	    Disposable indo = null;
	    if(data.ind != null)
		indo = env.prepare(data.ind);
	    Disposable[] bufs = new Disposable[data.va.bufs.length];
	    int ne = 0;
	    for(int i = 0; i < data.va.bufs.length; i++) {
		bufs[i] = env.prepare(data.va.bufs[i]);
		if(data.va.bufs[i].usage == EPHEMERAL)
		    ne++;
	    }

	    GLProgram.VarID[] enable = new GLProgram.VarID[data.va.fmt.inputs.length];
	    for(int i = 0; i < enable.length; i++)
		enable[i] = state.prog().cattrib(data.va.fmt.inputs[i].tgt);
	    Vao0State.apply(this.gl, state, enable);

	    BGL gl = gl();
	    int[] offsets = new int[data.va.bufs.length];
	    GLBuffer cbuf = VboState.get(state);
	    GLBuffer vbuf = null;
	    if(ne > 0) {
		vbuf = env.tempvertex.get();
		if(vbuf != cbuf) {
		    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbuf);
		    cbuf = vbuf;
		}
		if(ne == 1) {
		    int sbuf = -1;
		    if(data.va.bufs.length == 1) {
			sbuf = 0;
		    } else {
			for(int i = 0; i < data.va.bufs.length; i++) {
			    if(data.va.bufs[i].usage == EPHEMERAL) {
				sbuf = i;
				break;
			    }
			}
		    }
		    gl.glBufferData(GL.GL_ARRAY_BUFFER, data.va.bufs[sbuf].size(), ByteBuffer.wrap(((HeapBuffer)bufs[sbuf]).buf), GL2.GL_STREAM_DRAW);
		    offsets[sbuf] = 0;
		} else {
		    int sz = 0;
		    for(int i = 0; i < data.va.bufs.length; i++) {
			if(data.va.bufs[i].usage == EPHEMERAL) {
			    sz += data.va.bufs[i].size();
			    offsets[i] = sz;
			}
		    }
		    int jdsz = sz; GLBuffer jdvbuf = vbuf;
		    gl.bglSubmit(new BGL.Request() {
			    public void run(GL2 gl) {
				ByteBuffer buf = ByteBuffer.wrap(new byte[jdsz]);
				for(int i = 0; i < data.va.bufs.length; i++) {
				    if(data.va.bufs[i].usage == EPHEMERAL)
					buf.put(((HeapBuffer)bufs[i]).buf);
				}
				buf.flip();
				gl.glBufferData(jdvbuf.glid(), jdsz, buf, GL2.GL_STREAM_DRAW);
			    }
			});
		}
	    }
	    for(int i = 0; i < data.va.fmt.inputs.length; i++) {
		VertexArray.Layout.Input attr = data.va.fmt.inputs[i];
		int off;
		if(data.va.bufs[attr.buf].usage == EPHEMERAL) {
		    if(vbuf != cbuf) {
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbuf);
			cbuf = vbuf;
		    }
		    off = offsets[data.va.fmt.inputs[i].buf];
		} else {
		    throw(new Error());
		}
		gl.glVertexAttribPointer(enable[i], attr.el.nc, glattribfmt(attr.el.cf), glattribnorm(attr.el.cf), attr.stride, attr.offset + off);
	    }
	    VboState.set(state, cbuf);
	    if(data.ind == null) {
		gl.glDrawArrays(glmode(data.mode), 0, data.va.n);
	    } else {
		if(data.ind.usage == EPHEMERAL) {
		    EboState.apply(gl, state, env.tempindex.get());
		    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, data.ind.size(), ByteBuffer.wrap(((HeapBuffer)indo).buf), GL2.GL_STREAM_DRAW);
		} else {
		    throw(new Error());
		}
		gl.glDrawElements(glmode(data.mode), data.ind.n, GL.GL_UNSIGNED_SHORT, 0);
	    }
	} else {
	    throw(new Error());
	}
    }

    public void execute(GL2 gl) {
	synchronized(env.drawmon) {
	}
    }
}
