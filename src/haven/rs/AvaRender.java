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

package haven.rs;

import haven.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import haven.Composited.ED;
import haven.Composited.MD;

public class AvaRender {
    public static Composited compose(Resource base, List<MD> mod, List<ED> equ) {
	Composited comp = new Composited(base.layer(Skeleton.Res.class).s);
	comp.chmod(mod);
	comp.chequ(equ);
	comp.changes();
	return(comp);
    }

    public static BufferedImage render(Coord sz, Resource base, List<MD> mod, List<ED> equ) throws InterruptedException {
	Composited tcomp;
	Camera tcam;
	while(true) {
	    try {
		Skeleton.BoneOffset camoff = base.layer(Skeleton.BoneOffset.class, "avacam");
		tcomp = compose(base, mod, equ);
		GLState.Buffer buf = new GLState.Buffer(null);
		camoff.forpose(tcomp.pose).prep(buf);
		tcam = new LocationCam(buf.get(PView.loc));
		break;
	    } catch(Loading ev) {
		ev.waitfor();
	    }
	}
	final Composited comp = tcomp; /* ¦] */
	final Camera cam = tcam;
	final GBuffer buf = new GBuffer(sz);
	final BufferedImage[] ret = {null};
	buf.render(new Drawn() {
		public void draw(GOut g) {
		    float field = 0.5f;
		    float aspect = ((float)buf.sz.y) / ((float)buf.sz.x);
		    Projection proj = Projection.frustum(-field, field, -aspect * field, aspect * field, 1, 5000);

		    Light.Model lmod = new Light.Model();
		    lmod.cc = javax.media.opengl.GL2.GL_SEPARATE_SPECULAR_COLOR;

		    BufView view = new BufView(buf, GLState.compose(proj, cam, lmod, new Light.LightList()));
		    view.render(new Rendered() {
			    public void draw(GOut g) {}

			    public boolean setup(RenderList rl) {
				rl.add(comp, null);
				rl.add(new DirLight(Color.WHITE, Color.WHITE, Color.WHITE, new Coord3f(1, 1, 1).norm()), null);
				return(false);
			    }
			}, g);
		    ret[0] = g.getimage();
		}
	    });
	return(ret[0]);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
	Resource base = Resource.load("gfx/borka/body");
	List<MD> mod = Arrays.asList(new MD(Resource.load("gfx/borka/male").indir(), Arrays.asList(Resource.load("gfx/borka/male").indir())));
	List<ED> equ = new LinkedList<ED>();
	BufferedImage img = render(new Coord(512, 512), base, mod, equ);
	img = PUtils.convolvedown(img, new Coord(128, 128), new PUtils.Lanczos(2));
	javax.imageio.ImageIO.write(img, "PNG", new java.io.File("/tmp/bard.png"));
    }
}
