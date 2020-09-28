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
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import com.jogamp.opengl.*;
import haven.Disposable;
import haven.Utils;
import haven.render.*;
import haven.render.sl.*;

public class GLProgram implements Disposable {
    public static boolean dumpall = false;
    public final GLEnvironment env;
    public final String vsrc, fsrc;
    public final Uniform[] uniforms;
    public final FragData[] fragdata;
    public final int[][] umap;
    public final boolean[] fmap;
    public final Attribute[] attribs;
    public final Map<Uniform, Integer> samplerids;
    public final Uniform[] samplers;
    public final AtomicInteger locked = new AtomicInteger(0);
    private final Map<Uniform, String> unifnms;
    private final Map<Attribute, AttrID> amap;
    private final String[] fragnms;
    private ProgOb glp;
    boolean disposed = false;

    public GLProgram(GLEnvironment env, ProgramContext ctx) {
	this.env = env;
	{
	    StringWriter buf = new StringWriter();
	    ctx.fctx.construct(buf);
	    fsrc = buf.toString();
	}
	{
	    StringWriter buf = new StringWriter();
	    ctx.vctx.construct(buf);
	    vsrc = buf.toString();
	}
	{
	    Uniform[] uniforms = ctx.uniforms.toArray(new Uniform[0]);
	    Map<Uniform, String> unifnms = new IdentityHashMap<>();
	    int[][] umap = new int[0][];
	    for(int i = 0; i < uniforms.length; i++) {
		unifnms.put(uniforms[i], ctx.symtab.get(uniforms[i].name));
		for(State.Slot slot : uniforms[i].deps) {
		    if(umap.length <= slot.id)
			umap = Arrays.copyOf(umap, slot.id + 1);
		    umap[slot.id] = (umap[slot.id] == null) ? new int[1] : Arrays.copyOf(umap[slot.id], umap[slot.id].length + 1);
		    umap[slot.id][umap[slot.id].length - 1] = i;
		}
	    }
	    this.uniforms = uniforms;
	    this.unifnms = unifnms;
	    this.umap = umap;
	}
	{
	    FragData[] fragdata = ctx.fragdata.toArray(new FragData[0]);
	    String[] fragnms = new String[fragdata.length];
	    boolean[] fmap = new boolean[DepthBuffer.slot.id + 1];
	    fmap[DepthBuffer.slot.id] = true;
	    for(int i = 0; i < fragdata.length; i++) {
		fragnms[i] = ctx.symtab.get(fragdata[i].name);
		for(State.Slot slot : fragdata[i].deps) {
		    if(fmap.length <= slot.id)
			fmap = Arrays.copyOf(fmap, slot.id + 1);
		    fmap[slot.id] = true;
		}
	    }
	    this.fragdata = fragdata;
	    this.fmap = fmap;
	    this.fragnms = fragnms;
	}
	{
	    int sn = 0;
	    Map<Uniform, Integer> samplerids = new IdentityHashMap<>();
	    Uniform[] samplers = new Uniform[uniforms.length];
	    for(Uniform var : uniforms) {
		if(var.type instanceof Type.Sampler) {
		    samplers[sn] = var;
		    samplerids.put(var, sn);
		    sn++;
		}
	    }
	    this.samplerids = samplerids;
	    this.samplers = Arrays.copyOf(samplers, sn);
	}
	{
	    this.attribs = ctx.attribs.toArray(new Attribute[0]);
	    Arrays.sort(this.attribs, Utils.idcmp);
	    Map<Attribute, AttrID> amap = new IdentityHashMap<>();
	    for(int i = 0, loc = 0; i < this.attribs.length; i++) {
		Attribute attr = this.attribs[i];
		amap.put(attr, new AttrID(ctx.symtab.get(attr.name), loc));
		loc += attrsize(attr);
	    }
	    this.amap = amap;
	}
    }

    public static int attrsize(Attribute attr) {
	if(attr.type == Type.MAT3)
	    return(3);
	if(attr.type == Type.MAT4)
	    return(4);
	return(1);
    }

    public static GLProgram build(GLEnvironment env, Collection<ShaderMacro> mods) {
	ProgramContext prog = new ProgramContext();
	for(ShaderMacro mod : mods)
	    mod.modify(prog);
	GLProgram ret = new GLProgram(env, prog);
	if(dumpall || prog.dump) {
	    System.err.println(mods + ":");
	    System.err.println("---> Vertex shader:");
	    System.err.print(ret.vsrc);
	    System.err.println("---> Fragment shader:");
	    System.err.print(ret.fsrc);
	    System.err.println();
	    System.err.println("-------- " + ret);
	    System.err.println();
	}
	return(ret);
    }

    public static class ShaderException extends RuntimeException {
	public final ShaderOb shader;
	public final String info;
	
	public ShaderException(String msg, ShaderOb shader, String info) {
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

    public static class ShaderOb extends GLObject implements BGL.ID {
	public final int type;
	public final String text;
	private int id;

	public ShaderOb(GLEnvironment env, int type, String text) {
	    super(env);
	    this.type = type;
	    this.text = text;
	    env.prepare(this);
	}

	public void create(GL3 gl) {
	    /* Does JOGL use the byte or char length or the supplied
	     * String, and in case of the former, how does one know
	     * the coding it encodes the String as so as to supply the
	     * corrent length? It won't matter since all reasonable
	     * programs will be ASCII, of course, but it would be
	     * interesting to know, so to speak. */
	    this.id = gl.glCreateShader(type);
	    GLException.checkfor(gl, env);
	    gl.glShaderSource(this.id, 1, new String[] {text}, new int[] {text.length()}, 0);
	    gl.glCompileShader(this.id);
	    int[] buf = {0};
	    gl.glGetShaderiv(this.id, GL3.GL_COMPILE_STATUS, buf, 0);
	    if(buf[0] != 1) {
		String info = null;
		gl.glGetShaderiv(this.id, GL3.GL_INFO_LOG_LENGTH, buf, 0);
		if(buf[0] > 0) {
		    byte[] logbuf = new byte[buf[0]];
		    gl.glGetShaderInfoLog(this.id, logbuf.length, buf, 0, logbuf, 0);
		    info = new String(logbuf, 0, buf[0]);
		}
		throw(new ShaderException("Failed to compile shader", this, info));
	    }
	}

	protected void delete(GL3 gl) {
	    gl.glDeleteShader(id);
	}

	public int glid() {
	    return(id);
	}
    }

    public static class ProgramException extends RuntimeException {
	public final GLProgram program;

	public ProgramException(String msg, GLProgram program) {
	    super(msg);
	    this.program = program;
	}
    }

    public static class UnknownExternException extends ProgramException {
	public final String type, symbol;

	public UnknownExternException(String msg, GLProgram program, String type, String symbol) {
	    super(msg, program);
	    this.type = type;
	    this.symbol = symbol;
	}
    }

    public static class LinkException extends ProgramException {
	public final String info;

	public LinkException(String msg, GLProgram program, String info) {
	    super(msg, program);
	    this.info = info;
	}

	public String toString() {
	    if(info == null)
		return(super.toString());
	    else
		return(super.toString() + "\nLog:\n" + info);
	}
    }

    public static class AttrID implements BGL.ID {
	public final String name;
	public final int id;

	private AttrID(String name, int id) {
	    this.name = name;
	    this.id = id;
	}

	public int glid() {
	    return(id);
	}

	public String toString() {
	    return(String.format("#<gl.attrib %d %s>", id, name));
	}
    }

    public abstract static class VarID implements BGL.ID, BGL.Request {
	public final String name;
	protected int id = -1;

	private VarID(String name) {
	    this.name = name;
	}

	public abstract int glid();
    }

    public class ProgOb extends GLObject implements BGL.ID {
	public final ShaderOb[] shaders;
	private int id;

	public ProgOb(GLEnvironment env, ShaderOb... shaders) {
	    super(env);
	    this.shaders = shaders;
	    env.prepare(this);
	    for(Map.Entry<Uniform, String> uni : GLProgram.this.unifnms.entrySet()) {
		UniformID id = uniresolve(uni.getKey().type, uni.getValue());
		/* XXX: This should work with samplers in compound
		 * uniforms, but that requires support in preparing
		 * them as well, so wait until actually used for
		 * anything. */
		if(samplerids.containsKey(uni.getKey()))
		    id.sampler = samplerids.get(uni.getKey());
		umap.put(uni.getKey(), id);
	    }
	}

	private UniformID uniresolve(Type type, String name) {
	    UniformID ret = new UniformID(name);
	    if(type instanceof Array) {
		Array ary = (Array)type;
		if(ary.sz > 0) {
		    UniformID[] sub = new UniformID[ary.sz];
		    for(int i = 0; i < ary.sz; i++)
			sub[i] = uniresolve(ary.el, name + "[" + i + "]");
		    ret.sub = sub;
		}
	    } else if(type instanceof Struct) {
		Struct struct = (Struct)type;
		UniformID[] sub = new UniformID[struct.fields.size()];
		int n = 0;
		for(Struct.Field f : struct.fields)
		    sub[n++] = uniresolve(f.type, name + "." + f.name);
		ret.sub = sub;
	    }
	    env.prepare(ret);
	    return(ret);
	}

	public void create(GL3 gl) {
	    this.id = gl.glCreateProgram();
	    for(ShaderOb sh : shaders)
		gl.glAttachShader(this.id, sh.glid());
	    for(AttrID attr : amap.values())
		gl.glBindAttribLocation(this.id, attr.id, attr.name);
	    for(int i = 0; i < fragdata.length; i++)
		gl.glBindFragDataLocation(this.id, i, fragnms[i]);
	    gl.glLinkProgram(this.id);
	    int[] buf = {0};
	    gl.glGetProgramiv(this.id, GL3.GL_LINK_STATUS, buf, 0);
	    if(buf[0] != 1) {
		String info = null;
		gl.glGetProgramiv(this.id, GL3.GL_INFO_LOG_LENGTH, buf, 0);
		if(buf[0] > 0) {
		    byte[] logbuf = new byte[buf[0]];
		    gl.glGetProgramInfoLog(this.id, logbuf.length, buf, 0, logbuf, 0);
		    info = new String(logbuf, 0, buf[0]);
		}
		throw(new LinkException("Failed to link GL program", GLProgram.this, info));
	    }
	}

	protected void delete(GL3 gl) {
	    gl.glDeleteProgram(id);
	}

	public int glid() {
	    return(this.id);
	}

	public class UniformID extends VarID {
	    public UniformID[] sub = null;
	    public int sampler = -1;

	    private UniformID(String name) {super(name);}

	    public void run(GL3 gl) {
		this.id = gl.glGetUniformLocation(ProgOb.this.id, name);
	    }

	    public int glid() {
		if(id < 0)
		    throw(new UnknownExternException("Uniform not resolvable in program: " + name, GLProgram.this, "uniform", name));
		return(id);
	    }

	    public String toString() {
		return(String.format("#<gl.uniform %d %s>", id, name));
	    }
	}

	private final transient Map<Uniform, UniformID> umap = new IdentityHashMap<>();
	public UniformID cuniform(Uniform var) {
	    return(umap.get(var));
	}
    }

    public ProgOb glid() {
	ProgOb glp;
	if((glp = this.glp) == null) {
	    synchronized(this) {
		if(disposed)
		    throw(new RuntimeException("reusing disposed program"));
		if((glp = this.glp) == null) {
		    glp = new ProgOb(env,
				     new ShaderOb(env, GL3.GL_VERTEX_SHADER, vsrc),
				     new ShaderOb(env, GL3.GL_FRAGMENT_SHADER, fsrc));
		    this.glp = glp;
		}
	    }
	}
	return(glp);
    }

    public static void apply(BGL gl, GLProgram from, GLProgram to) {
	if(to != null)
	    gl.glUseProgram(to.glid());
	else
	    gl.glUseProgram(null);
	if((from != null) && (to == null)) {
	    for(int i = 0; i < from.samplers.length; i++) {
		gl.glActiveTexture(GL.GL_TEXTURE0 + i);
		gl.glBindTexture(GLRender.glsamplertarget(from.samplers[i].type), null);
	    }
	} else if((from != null) && (to != null)) {
	    for(int i = 0; i < Math.min(to.samplers.length, from.samplers.length); i++) {
		if(from.samplers[i].type != to.samplers[i].type) {
		    gl.glActiveTexture(GL.GL_TEXTURE0 + i);
		    gl.glBindTexture(GLRender.glsamplertarget(from.samplers[i].type), null);
		}
	    }
	    if(from.samplers.length > to.samplers.length) {
		for(int i = to.samplers.length; i < from.samplers.length; i++) {
		    gl.glActiveTexture(GL.GL_TEXTURE0 + i);
		    gl.glBindTexture(GLRender.glsamplertarget(from.samplers[i].type), null);
		}
	    }
	}
	if(to != null) {
	    for(int i = 0; i < to.samplers.length; i++)
		gl.glUniform1i(to.uniform(to.samplers[i]), i);
	}
    }

    public AttrID cattrib(Attribute var) {
	return(amap.get(var));
    }
    public AttrID attrib(Attribute var) {
	AttrID r = cattrib(var);
	if(r == null)
	    throw(new UnknownExternException("Attribute not found in symtab: " + var, this, "attrib", var.toString()));
	return(r);
    }

    public ProgOb.UniformID cuniform(Uniform var) {
	return(glid().cuniform(var));
    }
    public ProgOb.UniformID uniform(Uniform var) {
	ProgOb.UniformID r = cuniform(var);
	if(r == null)
	    throw(new UnknownExternException("Uniform not found in symtab: " + var, this, "uniform", var.toString()));
	return(r);
    }

    public void dispose() {
	synchronized(this) {
	    if(glp != null) {
		ProgOb cur = glp;
		glp = null;
		cur.dispose();
		disposed = true;
	    }
	}
    }

    public void lock() {
	locked.incrementAndGet();
    }

    public void unlock() {
	locked.decrementAndGet();
    }

    public static class Dump implements Serializable {
	public final String vsrc, fsrc;
	public final int id;

	public Dump(GLProgram prog) {
	    this.vsrc = prog.vsrc;
	    this.fsrc = prog.fsrc;
	    this.id = System.identityHashCode(prog);
	}
    }

    public Dump dump() {
	return(new Dump(this));
    }
}
