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
import haven.render.*;
import haven.render.sl.*;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

public class CloudShadow extends State {
    public static final Slot<CloudShadow> slot = new Slot<CloudShadow>(Slot.Type.DRAW, CloudShadow.class);
    public final TexRender tex;
    public Coord3f dir, vel;
    public float scale;
    public float cmin = 0.5f, cmax = 1.0f, rmin = 0.4f, rmax = 1.0f;

    public CloudShadow(TexRender tex, DirLight light, Coord3f vel, float scale) {
	this.tex = tex;
	this.dir = new Coord3f(light.dir[0], light.dir[1], light.dir[2]);
	this.vel = vel;
	this.scale = scale;
    }

    public static final Uniform tsky = new Uniform(SAMPLER2D, p -> p.get(slot).tex.img, slot);
    public static final Uniform cdir = new Uniform(VEC2, p -> {
	    Coord3f dir = p.get(slot).dir;
	    float zf = 1.0f / (dir.z + 1.1f);
	    float xd = -dir.x * zf, yd = -dir.y * zf;
	    return(new float[] {xd, yd});
	}, slot);
    public static final Uniform cvel = new Uniform(VEC2, p -> p.get(slot).vel, slot);
    public static final Uniform cscl = new Uniform(FLOAT, p -> p.get(slot).scale, slot);
    public static final Uniform cthr = new Uniform(VEC4, p -> {
	    CloudShadow sdw = p.get(slot);
	    return(new float[] {sdw.cmin, sdw.cmax, sdw.rmin, sdw.rmax - sdw.rmin});
	}, slot);
    private static final ShaderMacro shader = prog -> {
	final Phong ph = prog.getmod(Phong.class);
	if((ph == null) || !ph.pfrag)
	    return;
	final ValBlock.Value shval = prog.fctx.uniform.new Value(FLOAT) {
		public Expression root() {
		    Expression tc = add(mul(add(pick(Homo3D.fragmapv.ref(), "xy"),
						mul(pick(Homo3D.fragmapv.ref(), "z"), cdir.ref())),
					    cscl.ref()), mul(cvel.ref(), Glob.FrameInfo.globtime()));
		    Expression cl = pick(texture2D(tsky.ref(), tc), "r");
		    Expression th = cthr.ref();
		    return(add(mul(smoothstep(pick(th, "x"), pick(th, "y"), cl), pick(th, "w")), pick(th, "z")));
		}

		protected void cons2(Block blk) {
		    tgt = new Variable.Global(FLOAT).ref();
		    blk.add(ass(tgt, init));
		}
	    };
	shval.force();
	ph.dolight.mod(new Runnable() {
		public void run() {
		    ph.dolight.dcalc.add(new If(eq(MapView.amblight_idx.ref(), ph.dolight.i),
						stmt(amul(ph.dolight.dl.tgt, shval.ref()))),
					 ph.dolight.dcurs);
		}
	    }, 0);
    };

    public ShaderMacro shader() {return(shader);}

    public void apply(Pipe buf) {
	buf.put(slot, this);
    }

    public boolean equals(CloudShadow that) {
	return((this.tex == that.tex) && Utils.eq(this.dir, that.dir) &&
	       Utils.eq(this.vel, that.vel) && (this.scale == that.scale) &&
	       (this.cmin == that.cmin) && (this.cmax == that.cmax) && (this.rmin == that.rmin) && (this.rmax == that.rmax));
    }

    public boolean equals(Object o) {
	return((o instanceof CloudShadow) && equals((CloudShadow)o));
    }
}
