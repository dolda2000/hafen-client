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

import com.jogamp.opengl.*;

public class GLException extends RuntimeException {
    public int code;
    public String str;
    private static com.jogamp.opengl.glu.GLU glu = new com.jogamp.opengl.glu.GLU();

    public GLException(int code) {
	super("GL Error: " + code + " (" + glu.gluErrorString(code) + ")");
	this.code = code;
	this.str = glu.gluErrorString(code);
    }

    public static String constname(Class<?> cl, int val) {
	String ret = null;
	for(java.lang.reflect.Field f : cl.getFields()) {
	    if(((f.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) &&
	       ((f.getModifiers() & java.lang.reflect.Modifier.PUBLIC) != 0) &&
	       (f.getType() == Integer.TYPE)) {
		int v;
		try {
		    v = f.getInt(null);
		} catch(IllegalAccessException e) {
		    continue;
		}
		if(v == val) {
		    if(ret == null)
			ret = f.getName();
		    else
			ret = ret + " or " + f.getName();
		}
	    }
	}
	if(ret == null)
	    return(Integer.toString(val));
	return(ret);
    }

    public static String constname(int val) {
	return(constname(GL3.class, val));
    }

    public static class GLInvalidEnumException extends GLException {
	public GLInvalidEnumException() {super(GL.GL_INVALID_ENUM);}
    }
    public static class GLInvalidValueException extends GLException {
	public GLInvalidValueException() {super(GL.GL_INVALID_VALUE);}
    }
    public static class GLInvalidOperationException extends GLException {
	public GLInvalidOperationException() {super(GL.GL_INVALID_OPERATION);}
    }
    public static class GLOutOfMemoryException extends GLException {
	public String memstats = null;

	public GLOutOfMemoryException() {super(GL.GL_OUT_OF_MEMORY);}

	public void initenv(GLEnvironment env) {
	    super.initenv(env);
	    memstats = env.memstats();
	}
    }

    public static GLException glexcfor(int code) {
	switch(code) {
	case GL.GL_INVALID_ENUM:      return(new GLInvalidEnumException());
	case GL.GL_INVALID_VALUE:     return(new GLInvalidValueException());
	case GL.GL_INVALID_OPERATION: return(new GLInvalidOperationException());
	case GL.GL_OUT_OF_MEMORY:     return(new GLOutOfMemoryException());
	default: return(new GLException(code));
	}
    }

    public static void checkfor(GL gl, Throwable cause, GLEnvironment env) {
	int err = gl.glGetError();
	if(err != 0) {
	    GLException exc = glexcfor(err);
	    exc.initCause(cause);
	    if(env != null)
		exc.initenv(env);
	    throw(exc);
	}
    }

    public static void checkfor(GL gl, GLEnvironment env) {
	checkfor(gl, null, env);
    }

    public void initenv(GLEnvironment env) {
    }
}
