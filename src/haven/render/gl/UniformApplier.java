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
import com.jogamp.opengl.*;
import haven.*;
import haven.render.*;
import haven.render.sl.*;
import haven.render.gl.GLProgram.ProgOb.UniformID;

public interface UniformApplier<T> {
    public static class NoMappingException extends RuntimeException {
	public final String varnm;
	public final Type vartype;
	public final Class<?> valtype;

	public NoMappingException(String varnm, Type vartype, Class<?> valtype) {
	    super(String.format("no uniform type mapping for %s -> %s: %s", valtype, vartype, varnm));
	    this.varnm = varnm;
	    this.vartype = vartype;
	    this.valtype = valtype;
	}
    }

    public static class TypeMapping {
	private static final Map<Type, TypeMapping> mappings = new HashMap<>();
	private final Map<Class<?>, UniformApplier<?>> reg = new HashMap<>();
	private final Map<Class<?>, UniformApplier<?>> cache = new HashMap<>();

	public static <T> void register(Type type, Class<T> cl, UniformApplier<T> fn) {
	    TypeMapping map;
	    synchronized(mappings) {
		map = mappings.computeIfAbsent(type, t -> new TypeMapping());
	    }
	    synchronized(map) {
		map.reg.put(cl, fn);
		map.cache.clear();
	    }
	}

	@SuppressWarnings("unchecked")
	public static <T> UniformApplier<T> get(Type type, Class<T> cl) {
	    /* XXX: *Should* be synchronized, but doesn't actually
	     * need to be. Test and see if there are performance
	     * consequences. */
	    TypeMapping map = mappings.get(type);
	    if(map == null)
		return(null);
	    UniformApplier<T> fn = (UniformApplier<T>)map.cache.get(cl);
	    if(fn == null) {
		for(Map.Entry<Class<?>, UniformApplier<?>> reg : map.reg.entrySet()) {
		    if(reg.getKey().isAssignableFrom(cl)) {
			fn = (UniformApplier<T>)reg.getValue();
			break;
		    }
		}
		map.cache.put(cl, fn);
	    }
	    return(fn);
	}

	@SuppressWarnings("unchecked")
	private static <T> void apply0(BGL gl, UniformApplier<T> fn, UniformID var, Type type, Object val) {
	    fn.apply(gl, var, type, (T)val);
	}
	private static void apply(BGL gl, Type type, UniformID var, Object val) {
	    if(type instanceof Array) {
		Array ary = (Array)type;
		UniformApplier<?> fn = UniformApplier.TypeMapping.get(new Array(ary.el), val.getClass());
		if(fn != null) {
		    apply0(gl, fn, var, type, val);
		    return;
		}
		Object[] sval = (Object[])val;
		/* XXX? Somewhat unclear if it should be considered
		 * okay to leave previous values unchanged, especially
		 * for samplers. */
		for(int i = 0; (i < ary.sz) && (i < sval.length); i++) {
		    if(sval[i] != null)
			apply(gl, ary.el, var.sub[i], sval[i]);
		}
	    } else if(type instanceof Struct) {
		Object[] sval = (Object[])val;
		Struct struct = (Struct)type;
		int i = 0;
		for(Struct.Field f : struct.fields) {
		    if(sval[i] != null)
			apply(gl, f.type, var.sub[i], sval[i]);
		    i++;
		}
	    } else {
		UniformApplier<?> fn = UniformApplier.TypeMapping.get(type, val.getClass());
		if(fn == null)
		    throw(new NoMappingException(var.name, type, val.getClass()));
		apply0(gl, fn, var, type, val);
	    }
	}
	public static void apply(BGL gl, GLProgram prog, Uniform var, Object val) {
	    apply(gl, var.type, prog.uniform(var), val);
	}

	static {
	    TypeMapping.register(Type.FLOAT, Float.class, (gl, var, type, n) -> {
		    gl.glUniform1f(var, n);
		});

	    TypeMapping.register(Type.INT, Integer.class, (gl, var, type, n) -> {
		    gl.glUniform1i(var, n);
		});

	    TypeMapping.register(Type.IVEC2, int[].class, (gl, var, type, n) -> {
		    gl.glUniform2i(var, n[0], n[1]);
		});
	    TypeMapping.register(Type.IVEC2, Coord.class, (gl, var, type, c) -> {
		    gl.glUniform2i(var, c.x, c.y);
		});

	    TypeMapping.register(Type.IVEC3, int[].class, (gl, var, type, n) -> {
		    gl.glUniform3i(var, n[0], n[1], n[2]);
		});

	    TypeMapping.register(Type.IVEC4, int[].class, (gl, var, type, n) -> {
		    gl.glUniform4i(var, n[0], n[1], n[2], n[3]);
		});

	    TypeMapping.register(Type.VEC2, float[].class, (gl, var, type, a) -> {
		    gl.glUniform2f(var, a[0], a[1]);
		});
	    TypeMapping.register(Type.VEC2, Coord.class, (gl, var, type, c) -> {
		    gl.glUniform2f(var, c.x, c.y);
		});
	    TypeMapping.register(Type.VEC2, Coord3f.class, (gl, var, type, c) -> {
		    gl.glUniform2f(var, c.x, c.y);
		});

	    TypeMapping.register(Type.VEC3, float[].class, (gl, var, type, a) -> {
		    gl.glUniform3f(var, a[0], a[1], a[2]);
		});
	    TypeMapping.register(Type.VEC3, Coord3f.class, (gl, var, type, c) -> {
		    gl.glUniform3f(var, c.x, c.y, c.z);
		});
	    TypeMapping.register(Type.VEC3, java.awt.Color.class, (gl, var, type, col) -> {
		    gl.glUniform3f(var, col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);
		});
	    TypeMapping.register(Type.VEC3, FColor.class, (gl, var, type, col) -> {
		    gl.glUniform3f(var, col.r, col.g, col.b);
		});

	    TypeMapping.register(Type.VEC4, float[].class, (gl, var, type, a) -> {
		    gl.glUniform4f(var, a[0], a[1], a[2], a[3]);
		});
	    TypeMapping.register(Type.VEC4, Coord3f.class, (gl, var, type, c) -> {
		    gl.glUniform4f(var, c.x, c.y, c.z, 1);
		});
	    TypeMapping.register(Type.VEC4, java.awt.Color.class, (gl, var, type, col) -> {
		    gl.glUniform4f(var, col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f, col.getAlpha() / 255f);
		});
	    TypeMapping.register(Type.VEC4, FColor.class, (gl, var, type, col) -> {
		    gl.glUniform4f(var, col.r, col.g, col.b, col.a);
		});

	    TypeMapping.register(Type.MAT3, float[].class, (gl, var, type, mat) -> {
		    gl.glUniformMatrix3fv(var, 1, false, mat, 0);
		});
	    TypeMapping.register(Type.MAT3, Matrix4f.class, (gl, var, type, mat) -> {
		    gl.glUniformMatrix3fv(var, 1, false, mat.trim3(), 0);
		});

	    TypeMapping.register(Type.MAT4, float[].class, (gl, var, type, mat) -> {
		    gl.glUniformMatrix4fv(var, 1, false, mat, 0);
		});
	    TypeMapping.register(Type.MAT4, Matrix4f.class, (gl, var, type, mat) -> {
		    gl.glUniformMatrix4fv(var, 1, false, mat.m, 0);
		});
	    TypeMapping.register(new Array(Type.MAT4), float[][].class, (gl, var, type, mats) -> {
		    Array ary = (Array)type;
		    int n = Math.min(mats.length, ary.sz);
		    float[] buf = new float[n * 16];
		    for(int i = 0, m = 0; i < n; i++) {
			for(int o = 0; o < 16; o++)
			    buf[m++] = mats[i][o];
		    }
		    gl.glUniformMatrix4fv(var, n, false, buf, 0);
		});

	    TypeMapping.register(Type.SAMPLER2D, GLTexture.Tex2D.class, (gl, var, type, smp) -> {
		    if(var.sampler < 0) throw(new RuntimeException());
		    gl.glActiveTexture(GL.GL_TEXTURE0 + var.sampler);
		    smp.bind(gl);
		});
	    TypeMapping.register(Type.SAMPLER3D, GLTexture.Tex3D.class, (gl, var, type, smp) -> {
		    if(var.sampler < 0) throw(new RuntimeException());
		    gl.glActiveTexture(GL.GL_TEXTURE0 + var.sampler);
		    smp.bind(gl);
		});
	    TypeMapping.register(Type.SAMPLER2DARRAY, GLTexture.Tex2DArray.class, (gl, var, type, smp) -> {
		    if(var.sampler < 0) throw(new RuntimeException());
		    gl.glActiveTexture(GL.GL_TEXTURE0 + var.sampler);
		    smp.bind(gl);
		});
	    TypeMapping.register(Type.SAMPLERCUBE, GLTexture.TexCube.class, (gl, var, type, smp) -> {
		    if(var.sampler < 0) throw(new RuntimeException());
		    gl.glActiveTexture(GL.GL_TEXTURE0 + var.sampler);
		    smp.bind(gl);
		});
	}
    }

    public void apply(BGL gl, UniformID var, Type type, T value);
}
