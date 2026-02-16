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
import haven.render.*;
import java.util.*;
import java.io.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import haven.Composited.ED;
import haven.Composited.MD;

public class AvaRender {
    public static class ServerRes implements Resource.Resolver {
	public final Resource.Pool pool;

	public ServerRes(Resource.Pool pool) {
	    this.pool = pool;
	}

	public Indir<Resource> getres(int id) {throw(new UnsupportedOperationException());}

	public static class ServerDescriptor extends Descriptor<ServerRes> {
	    public ServerDescriptor(ServerRes rr) {super(rr);}

	    public Maybe<Indir<Resource>> opt(Object desc) {
		if(desc instanceof String)
		    return(Maybe.of(rr.pool.load((String)desc)));
		return(super.opt(desc));
	    }
	}

	@Override public PType<Indir<Resource>> desc() {
	    return(new ServerDescriptor(this));
	}

	public Indir<Resource> dynres(UID uid) {
	    return(pool.dynres(uid));
	}
    }

    public static class Owner implements OwnerContext, RandomSource {
	private static final OwnerContext.ClassResolver<Owner> ctxr = new OwnerContext.ClassResolver<Owner>()
	    .add(Resource.Resolver.class, o -> o.map);
	public final Resource.Resolver map;

	public Owner(Resource.Resolver map) {
	    this.map = map;
	}

	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}
	public Random mkrandoom() {return(new Random());}
    }

    public static Composited compose(Composited.Desc desc, Resource.Resolver map) {
	Composited comp = new Composited(desc.base.get().flayer(Skeleton.Res.class).s, new Owner(map));
	comp.chmod(desc.mod);
	comp.chequ(desc.equ);
	return(comp);
    }

    public static BufferedImage render(Coord sz, Composited.Desc desc, Resource.Resolver map, String camnm) {
	Composited comp = Loading.waitfor(() -> compose(desc, map));;
	Camera cam = Loading.waitfor(() -> {
		Skeleton.BoneOffset camoff = desc.base.get().flayer(Skeleton.BoneOffset.class, camnm);
		Pipe buf = new BufPipe();
		buf.prep(camoff.from(comp).get());
		return(Camera.placed(buf.get(Homo3D.loc)));
	    });
	DrawBuffer buf = new DrawBuffer(Context.getdefault().env(), sz);

	float field = 0.5f;
	float aspect = ((float)buf.sz.y) / ((float)buf.sz.x);
	Projection proj = Projection.frustum(-field, field, -aspect * field, aspect * field, 1, 5000);
	RenderTree.Node light = new DirLight(Color.WHITE, Color.WHITE, Color.WHITE, new Coord3f(1, 1, 1).norm());
	BufferedImage ret = buf.draw(Pipe.Op.compose(proj, cam), RUtils.compose(comp, light));
	return(ret);
    }

    public static final Server.Command call = new Server.Command() {
	    public Object[] run(Server.Client cl, Object... args) throws InterruptedException {
		Coord sz = UI.scale((Coord)args[0]);
		Resource.Resolver rr = new ServerRes(Resource.remote());
		Composited.Desc desc = Composited.Desc.decode(rr, Utils.oav(args[1]));
		Resource.Resolver map = new Resource.Resolver.ResourceMap(rr, Utils.oav(args[2]));
		String camnm = Utils.sv(args[3]);
		BufferedImage ava = render(sz.mul(4), desc, map, camnm);
		ava = PUtils.convolvedown(ava, sz, new PUtils.Lanczos(2));
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		try {
		    javax.imageio.ImageIO.write(ava, "PNG", buf);
		} catch(IOException e) {
		    throw(new Error(e));
		}
		return(new Object[] {"ok", buf.toByteArray()});
	    }
	};

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
	Composited.Desc desc = new Composited.Desc();
	desc.base = Resource.remote().load("gfx/borka/body");
	desc.mod = Arrays.asList(new MD(Resource.remote().load("gfx/borka/male"), ResData.wrap(Arrays.asList(Resource.remote().load("gfx/borka/male")))));
	desc.equ = new LinkedList<ED>();
	BufferedImage img = render(new Coord(512, 512), desc, null, "avacam");
	img = PUtils.convolvedown(img, new Coord(128, 128), new PUtils.Lanczos(2));
	javax.imageio.ImageIO.write(img, "PNG", new java.io.File("/tmp/bard.png"));
    }
}
