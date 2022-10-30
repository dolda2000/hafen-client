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

package haven.render.sl;

public abstract class Type {
    private static class Simple extends Type {
	private final String name;

	private Simple(String name) {
	    this.name = name;
	}

	public String name(Context ctx) {return(name);}
	public String toString() {return(name);}
    }

    public static class Sampler extends Simple {
	private Sampler(String name) {
	    super(name);
	}
    }

    public static final Type VOID = new Simple("void");
    public static final Type INT = new Simple("int");
    public static final Type UINT = new Simple("uint");
    public static final Type FLOAT = new Simple("float");
    public static final Type VEC2 = new Simple("vec2");
    public static final Type VEC3 = new Simple("vec3");
    public static final Type VEC4 = new Simple("vec4");
    public static final Type IVEC2 = new Simple("ivec2");
    public static final Type IVEC3 = new Simple("ivec3");
    public static final Type IVEC4 = new Simple("ivec4");
    public static final Type UVEC2 = new Simple("uvec2");
    public static final Type UVEC3 = new Simple("uvec3");
    public static final Type UVEC4 = new Simple("uvec4");
    public static final Type MAT3 = new Simple("mat3");
    public static final Type MAT4 = new Simple("mat4");

    public static final Type SAMPLER1D = new Sampler("sampler1D");
    public static final Type SAMPLER1DARRAY = new Sampler("sampler1DArray");
    public static final Type SAMPLER2D = new Sampler("sampler2D");
    public static final Type SAMPLER2DARRAY = new Sampler("sampler2DArray");
    public static final Type SAMPLER2DMS = new Sampler("sampler2DMS");
    public static final Type SAMPLER2DMSARRAY = new Sampler("sampler2DMSArray");
    public static final Type SAMPLER3D = new Sampler("sampler3D");
    public static final Type SAMPLERCUBE = new Sampler("samplerCube");
    public static final Type SAMPLERCUBEARRAY = new Sampler("samplerCubeArray");
    public static final Type SAMPLERBUFFER = new Sampler("samplerBuffer");

    public static final Type ISAMPLER1D = new Sampler("isampler1D");
    public static final Type ISAMPLER1DARRAY = new Sampler("isampler1DArray");
    public static final Type ISAMPLER2D = new Sampler("isampler2D");
    public static final Type ISAMPLER2DARRAY = new Sampler("isampler2DArray");
    public static final Type ISAMPLER2DMS = new Sampler("isampler2DMS");
    public static final Type ISAMPLER2DMSARRAY = new Sampler("isampler2DMSArray");
    public static final Type ISAMPLER3D = new Sampler("isampler3D");
    public static final Type ISAMPLERCUBE = new Sampler("isamplerCube");
    public static final Type ISAMPLERCUBEARRAY = new Sampler("isamplerCubeArray");
    public static final Type ISAMPLERBUFFER = new Sampler("isamplerBuffer");

    public static final Type USAMPLER1D = new Sampler("usampler1D");
    public static final Type USAMPLER1DARRAY = new Sampler("usampler1DArray");
    public static final Type USAMPLER2D = new Sampler("usampler2D");
    public static final Type USAMPLER2DARRAY = new Sampler("usampler2DArray");
    public static final Type USAMPLER2DMS = new Sampler("usampler2DMS");
    public static final Type USAMPLER2DMSARRAY = new Sampler("usampler2DMSArray");
    public static final Type USAMPLER3D = new Sampler("usampler3D");
    public static final Type USAMPLERCUBE = new Sampler("usamplerCube");
    public static final Type USAMPLERCUBEARRAY = new Sampler("usamplerCubeArray");
    public static final Type USAMPLERBUFFER = new Sampler("usamplerBuffer");

    public static final Type SAMPLER2DSHADOW = new Sampler("sampler2DShadow");
    public static final Type SAMPLERCUBESHADOW = new Sampler("samplerCubeShadow");

    public abstract String name(Context ctx);
    public void use(Context ctx) {}
}
