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

package haven.resutil;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.awt.Color;
import haven.render.TextureCube.SamplerCube;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

@Material.ResName("envref")
public class EnvMap extends State {
    public static final Slot<EnvMap> slot = new Slot<EnvMap>(Slot.Type.DRAW, EnvMap.class);
    private static final Uniform csky = new Uniform(SAMPLERCUBE, p -> p.get(slot).sky, slot);
    private static final Uniform ccol = new Uniform(VEC3, p -> p.get(slot).col, slot);
    private static final Uniform icam = new Uniform(MAT3, p -> Homo3D.camxf(p).transpose().trim3(), Homo3D.cam);
    private static final SamplerCube sky = WaterTile.sky;
    public final float[] col;
    
    public EnvMap(Color col) {
	this.col = new float[] {
	    col.getRed() / 255.0f,
	    col.getGreen() / 255.0f,
	    col.getBlue() / 255.0f,
	};
    }

    public EnvMap(Resource res, Object... args) {
	this((Color)args[0]);
    }

    private static final ShaderMacro shader = prog -> {
	prog.dump = true;
	FragColor.fragcol(prog.fctx).mod(in -> {
		return(add(in, mul(textureCube(csky.ref(), neg(mul(icam.ref(),
								   reflect(Homo3D.fragedir(prog.fctx).depref(),
									   Homo3D.frageyen(prog.fctx).depref())))),
				   vec4(ccol.ref(), l(0.0)))));
	    }, 90);
    };

    public ShaderMacro shader() {return(shader);}

    public void apply(Pipe buf) {
	buf.put(slot, this);
    }
}
