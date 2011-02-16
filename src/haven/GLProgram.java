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
import javax.media.opengl.*;
import haven.GLShader.VertexShader;
import haven.GLShader.FragmentShader;

public class GLProgram implements java.io.Serializable {
    public final Collection<GLShader> shaders;
    private transient ProgOb glp;
    
    public GLProgram(Collection<GLShader> shaders) {
	this.shaders = new ArrayList<GLShader>(shaders);
	makemains(this.shaders);
    }
    
    /* Meaningful function is meaningful. :-P */
    private static Collection<GLShader> collapse(GLShader[][] shaders) {
	Collection<GLShader> sc = new ArrayList<GLShader>();
	for(int i = 0; i < shaders.length; i++) {
	    if(shaders[i] == null)
		continue;
	    for(int o = 0; o < shaders[i].length; o++)
		sc.add(shaders[i][o]);
	}
	return(sc);
    }
    
    public GLProgram(GLShader[][] shaders) {
	this(collapse(shaders));
    }

    private static void makemains(Collection<GLShader> shaders) {
	List<VertexShader> vs = new ArrayList<VertexShader>();
	List<FragmentShader> fs = new ArrayList<FragmentShader>();
	for(GLShader s : shaders) {
	    if(s instanceof VertexShader)
		vs.add((VertexShader)s);
	    else if(s instanceof FragmentShader)
		fs.add((FragmentShader)s);
	}
	shaders.add(VertexShader.makemain(vs));
	shaders.add(FragmentShader.makemain(fs));
    }
    
    public static class ProgOb extends GLObject {
	public final int id;
	
	public ProgOb(GL gl) {
	    super(gl);
	    id = gl.glCreateProgramObjectARB();
	}
	
	public void delete() {
	    gl.glDeleteObjectARB(id);
	}
	
	public void link(GLProgram prog) {
	    for(GLShader sh : prog.shaders)
		gl.glAttachShader(id, sh.glid(gl));
	    gl.glLinkProgram(id);
	    int[] buf = {0};
	    gl.glGetObjectParameterivARB(id, GL.GL_OBJECT_LINK_STATUS_ARB, buf, 0);
	    if(buf[0] != 1) {
		String info = null;
		gl.glGetObjectParameterivARB(id, GL.GL_OBJECT_INFO_LOG_LENGTH_ARB, buf, 0);
		if(buf[0] > 0) {
		    byte[] logbuf = new byte[buf[0]];
		    gl.glGetInfoLogARB(id, logbuf.length, buf, 0, logbuf, 0);
		    /* The "platform's default charset" is probably a reasonable choice. */
		    info = new String(logbuf, 0, buf[0]);
		}
		throw(new ProgramException("Failed to link GL program", prog, info));
	    }
	}
	
	public int uniform(String name) {
	    int r = gl.glGetUniformLocationARB(id, name);
	    if(r < 0)
		throw(new RuntimeException("Unknown uniform name: " + name));
	    return(r);
	}
	
	public int attrib(String name) {
	    int r = gl.glGetAttribLocationARB(id, name);
	    if(r < 0)
		throw(new RuntimeException("Unknown uniform name: " + name));
	    return(r);
	}
    }
    
    public static class ProgramException extends RuntimeException {
	public final GLProgram program;
	public final String info;
	
	public ProgramException(String msg, GLProgram program, String info) {
	    super(msg);
	    this.program = program;
	    this.info = info;
	}
	
	public String toString() {
	    if(info == null)
		return(super.toString());
	    else
		return(super.toString() + "\nLog:\n" + info);
	}
    }
    
    public void apply(GOut g) {
	if((glp != null) && (glp.gl != g.gl)) {
	    glp.dispose();
	    glp = null;
	}
	if(glp == null) {
	    glp = new ProgOb(g.gl);
	    glp.link(this);
	}
	g.gl.glUseProgramObjectARB(glp.id);
    }
    
    public void dispose() {
	synchronized(this) {
	    if(glp != null) {
		ProgOb cur = glp;
		glp = null;
		cur.dispose();
	    }
	}
    }

    private final Map<String, Integer> umap = new IdentityHashMap<String, Integer>();
    public int uniform(String name) {
	Integer r = umap.get(name);
	if(r == null)
	    umap.put(name, r = new Integer(glp.uniform(name)));
	return(r.intValue());
    }

    private final Map<String, Integer> amap = new IdentityHashMap<String, Integer>();
    public int attrib(String name) {
	Integer r = amap.get(name);
	if(r == null)
	    amap.put(name, r = new Integer(glp.attrib(name)));
	return(r.intValue());
    }
}
