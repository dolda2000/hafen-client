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

package haven.glsl;

public abstract class Type {
    private static class Simple extends Type {
	private final String name;

	private Simple(String name) {
	    this.name = name;
	}

	public String name(Context ctx) {return(name);}
	public String toString() {return(name);}
    }

    public static final Type VOID = new Simple("void");
    public static final Type INT = new Simple("int");
    public static final Type FLOAT = new Simple("float");
    public static final Type VEC2 = new Simple("vec2");
    public static final Type VEC3 = new Simple("vec3");
    public static final Type VEC4 = new Simple("vec4");
    public static final Type IVEC2 = new Simple("ivec2");
    public static final Type IVEC3 = new Simple("ivec3");
    public static final Type IVEC4 = new Simple("ivec4");
    public static final Type MAT3 = new Simple("mat3");
    public static final Type MAT4 = new Simple("mat4");
    public static final Type SAMPLER2D = new Simple("sampler2D");
    public static final Type SAMPLER2DSHADOW = new Simple("sampler2DShadow");
    public static final Type SAMPLER2DMS = new Simple("sampler2DMS");
    public static final Type SAMPLER3D = new Simple("sampler3D");
    public static final Type SAMPLERCUBE = new Simple("samplerCube");

    public abstract String name(Context ctx);
}
