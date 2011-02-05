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

import java.io.*;
import java.util.*;
import javax.media.opengl.*;

public abstract class GLShader implements java.io.Serializable {
    public final String source, header;
    private transient ShaderOb gls;
    
    public GLShader(String source, String header) {
	this.source = source;
	this.header = header;
    }
    
    public static class ShaderOb extends GLObject {
	public final int id;
	
	public ShaderOb(GL gl, int type) {
	    super(gl);
	    id = gl.glCreateShaderObjectARB(type);
	    GOut.checkerr(gl);
	}
	
	protected void delete() {
	    gl.glDeleteObjectARB(id);
	}
	
	public void compile(GLShader sh) {
	    /* Does JOGL use the byte or char length or the supplied
	     * String, and in case of the former, how does one know
	     * the coding it encodes the String as so as to supply the
	     * corrent length? It won't matter since all reasonable
	     * programs will be ASCII, of course, but still... */
	    gl.glShaderSourceARB(id, 1, new String[] {sh.source}, new int[] {sh.source.length()}, 0);
	    gl.glCompileShaderARB(id);
	    int[] buf = {0};
	    gl.glGetObjectParameterivARB(id, GL.GL_OBJECT_COMPILE_STATUS_ARB, buf, 0);
	    if(buf[0] != 1) {
		String info = null;
		gl.glGetObjectParameterivARB(id, GL.GL_OBJECT_INFO_LOG_LENGTH_ARB, buf, 0);
		if(buf[0] > 0) {
		    byte[] logbuf = new byte[buf[0]];
		    gl.glGetInfoLogARB(id, logbuf.length, buf, 0, logbuf, 0);
		    /* The "platform's default charset" is probably a reasonable choice. */
		    info = new String(logbuf, 0, buf[0]);
		}
		throw(new ShaderException("Failed to compile shader", sh, info));
	    }
	}
    }
    
    public static class ShaderException extends RuntimeException {
	public final GLShader shader;
	public final String info;
	
	public ShaderException(String msg, GLShader shader, String info) {
	    super(msg);
	    this.shader = shader;
	    this.info = info;
	}
	
	public String toString() {
	    if(info == null)
		return(super.toString());
	    else
		return(super.toString() + "\nLog:\n" + info);
	}
    }
    
    public abstract static class Splitter {
	private final BufferedReader in;
	public final StringBuilder main = new StringBuilder();
	public StringBuilder buf = main;
	
	public Splitter(Reader r) {
	    in = new BufferedReader(r);
	}
	
	public Splitter(InputStream i) {
	    this(new InputStreamReader(i, Utils.ascii));
	}
	
	public void parse() throws IOException {
	    String ln;
	    while((ln = in.readLine()) != null) {
		if(ln.startsWith("#pp ")) {
		    String d = ln.substring(4).trim();
		    String a = "";
		    int p = d.indexOf(' ');
		    if(p >= 0) {
			a = d.substring(p + 1);
			d = d.substring(0, p).trim();
		    }
		    d = d.intern();
		    directive(d, a);
		} else {
		    buf.append(ln + "\n");
		}
	    }
	}
	
	public abstract void directive(String directive, String args);
    }
    
    public static class VertexShader extends GLShader {
	public final String entry;
	public final String[] args;
	public final int order;

	public VertexShader(String source, String header, String entry, int order, String... args) {
	    super(source, header);
	    this.entry = entry;
	    this.order = order;
	    this.args = args;
	}
	
	private VertexShader(String source) {
	    this(source, "", null, 0);
	}

	protected ShaderOb create(GL gl) {
	    ShaderOb r = new ShaderOb(gl, GL.GL_VERTEX_SHADER_ARB);
	    r.compile(this);
	    return(r);
	}
	
	private boolean uses(String arg) {
	    for(String a : args) {
		if(a.equals(arg))
		    return(true);
	    }
	    return(false);
	}

	private String call() {
	    String ret = entry + "(";
	    boolean f = true;
	    for(String arg : args) {
		if(!f)
		    ret += ", ";
		ret += arg;
		f = false;
	    }
	    ret += ")";
	    return(ret);
	}

	private final static Comparator<VertexShader> cmp = new Comparator<VertexShader>() {
	    public int compare(VertexShader a, VertexShader b) {
		return(a.order - b.order);
	    }
	};

	public static VertexShader makemain(List<VertexShader> shaders) {
	    StringBuilder buf = new StringBuilder();
	    Collections.sort(shaders, cmp);
	    for(VertexShader sh : shaders)
		buf.append(sh.header + "\n");
	    buf.append("\n");
	    buf.append("void main()\n{\n");
	    buf.append("    vec4 fcol = gl_Color;\n");
	    buf.append("    vec4 bcol = gl_Color;\n");
	    buf.append("    vec4 objv = gl_Vertex;\n");
	    buf.append("    vec3 objn = gl_Normal;\n");
	    int i = 0;
	    for(; i < shaders.size(); i++) {
		VertexShader sh = shaders.get(i);
		if(sh.uses("eyev") || sh.uses("eyen"))
		    break;
		buf.append("    " + sh.call() + ";\n");
	    }
	    buf.append("    vec4 eyev = gl_ModelViewMatrix * objv;\n");
	    buf.append("    vec3 eyen = gl_NormalMatrix * objn;\n");
	    for(; i < shaders.size(); i++) {
		VertexShader sh = shaders.get(i);
		buf.append("    " + sh.call() + ";\n");
	    }
	    buf.append("    gl_FrontColor = fcol;\n");
	    buf.append("    gl_Position = gl_ProjectionMatrix * eyev;\n");
	    buf.append("}\n");
	    return(new VertexShader(buf.toString()));
	}
	
	public static VertexShader parse(Reader in) throws IOException {
	    class VSplitter extends Splitter {
		StringBuilder header = new StringBuilder();
		String entry;
		String[] args;
		int order = 0;
		
		VSplitter(Reader in) {super(in);}
		
		public void directive(String d, String a) {
		    if(d == "header") {
			buf = header;
		    } else if(d == "main") {
			buf = main;
		    } else if(d == "order") {
			order = Integer.parseInt(a);
		    } else if(d == "entry") {
			String[] args = a.split(" +");
			entry = args[0];
			this.args = new String[args.length - 1];
			for(int i = 1, o = 0; i < args.length; i++, o++)
			    this.args[o] = args[i];
		    }
		}
	    }
	    VSplitter p = new VSplitter(in);
	    p.parse();
	    if(p.entry == null)
		throw(new RuntimeException("No entry specified in shader source."));
	    return(new VertexShader(p.main.toString(), p.header.toString(), p.entry, p.order, p.args));
	}
	
	public static VertexShader load(Class<?> base, String name) {
	    InputStream in = base.getResourceAsStream(name);
	    try {
		try {
		    return(parse(new InputStreamReader(in, Utils.ascii)));
		} finally {
		    in.close();
		}
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}
    }

    public static class FragmentShader extends GLShader {
	public final String entry;
	public final int order;

	public FragmentShader(String source, String header, String entry, int order) {
	    super(source, header);
	    this.entry = entry;
	    this.order = order;
	}
	
	private FragmentShader(String source) {
	    this(source, "", null, 0);
	}
	
	protected ShaderOb create(GL gl) {
	    ShaderOb r = new ShaderOb(gl, GL.GL_FRAGMENT_SHADER_ARB);
	    r.compile(this);
	    return(r);
	}

	private final static Comparator<FragmentShader> cmp = new Comparator<FragmentShader>() {
	    public int compare(FragmentShader a, FragmentShader b) {
		return(a.order - b.order);
	    }
	};

	public static FragmentShader makemain(List<FragmentShader> shaders) {
	    StringBuilder buf = new StringBuilder();
	    Collections.sort(shaders, cmp);
	    for(FragmentShader sh : shaders)
		buf.append(sh.header + "\n");
	    buf.append("\n");
	    buf.append("void main()\n{\n");
	    buf.append("    vec4 res = gl_Color;\n");
	    for(FragmentShader sh : shaders) {
		buf.append("    " + sh.entry + "(res);\n");
	    }
	    buf.append("    gl_FragColor = res;\n");
	    buf.append("}\n");
	    return(new FragmentShader(buf.toString()));
	}
	
	public static FragmentShader parse(Reader in) throws IOException {
	    class FSplitter extends Splitter {
		StringBuilder header = new StringBuilder();
		String entry;
		int order = 0;
		
		FSplitter(Reader in) {super(in);}
		
		public void directive(String d, String a) {
		    if(d == "header") {
			buf = header;
		    } else if(d == "main") {
			buf = main;
		    } else if(d == "order") {
			order = Integer.parseInt(a);
		    } else if(d == "entry") {
			String[] args = a.split(" +");
			entry = args[0];
		    }
		}
	    }
	    FSplitter p = new FSplitter(in);
	    p.parse();
	    if(p.entry == null)
		throw(new RuntimeException("No entry specified in shader source."));
	    return(new FragmentShader(p.main.toString(), p.header.toString(), p.entry, p.order));
	}
	
	public static FragmentShader load(Class<?> base, String name) {
	    InputStream in = base.getResourceAsStream(name);
	    try {
		try {
		    return(parse(new InputStreamReader(in, Utils.ascii)));
		} finally {
		    in.close();
		}
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}
    }
    
    public int glid(GL gl) {
	if((gls != null) && (gls.gl != gl)) {
	    gls.dispose();
	    gls = null;
	}
	if(gls == null)
	    gls = create(gl);
	return(gls.id);
    }
    
    protected abstract ShaderOb create(GL gl);
}
