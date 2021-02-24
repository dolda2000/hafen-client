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
import java.util.function.*;
import haven.render.*;
import haven.Sprite.Owner;
import haven.render.RenderTree.Node;
import haven.render.RenderTree.Slot;

public interface RenderLink {
    public Node make(Owner owner);
    public default Node make() {return(make(null));}
    
    public static class MeshMat implements RenderLink {
	public final Resource srcres;
	public final Indir<Resource> mesh, mat;
	public final int meshid, matid;
	private Node res = null;

	public MeshMat(Resource srcres, Indir<Resource> mesh, int meshid, Indir<Resource> mat, int matid) {
	    this.srcres = srcres;
	    this.mesh = mesh;
	    this.meshid = meshid;
	    this.mat = mat;
	    this.matid = matid;
	}

	public static MeshMat parse(Resource res, Message buf) {
	    String meshnm = buf.string();
	    int meshver = buf.uint16();
	    int meshid = buf.int16();
	    String matnm = buf.string();
	    int matver = buf.uint16();
	    int matid = buf.int16();
	    Indir<Resource> mesh = meshnm.equals("") ? res.indir() : res.pool.load(meshnm, meshver);
	    Indir<Resource> mat = matnm.equals("") ? res.indir() : res.pool.load(matnm, matver);
	    return(new MeshMat(res, mesh, meshid, mat, matid));
	}

	public Node make(Owner owner) {
	    if(res == null) {
		FastMesh m = null;
		for(FastMesh.MeshRes mr : mesh.get().layers(FastMesh.MeshRes.class)) {
		    if((meshid < 0) || (mr.id == meshid)) {
			m = mr.m;
			break;
		    }
		}
		Material M = null;
		for(Material.Res mr : mat.get().layers(Material.Res.class)) {
		    if((matid < 0) || (mr.id == matid)) {
			M = mr.get();
			break;
		    }
		}
		if(m == null)
		    throw(new Sprite.ResourceException("Could not find specified mesh by ID " + meshid, srcres));
		if(M == null)
		    throw(new Sprite.ResourceException("Could not find specified material by ID " + matid, srcres));
		res = M.apply(m);
	    }
	    return(res);
	}
    }

    public static class AmbientLink implements RenderLink {
	public final Indir<Resource> res;

	public AmbientLink(Indir<Resource> res) {
	    this.res = res;
	}

	public static AmbientLink parse(Resource res, Message buf) {
	    String nm = buf.string();
	    int ver = buf.uint16();
	    return(new AmbientLink(res.pool.load(nm, ver)));
	}

	public Node make(Owner owner) {
	    return(new ActAudio.Ambience(res.get()));
	}
    }

    public static class Collect implements RenderLink {
	public final Indir<Resource> from;
	public final int meshid, meshmask;
	private Node res;

	public Collect(Indir<Resource> from, int meshid, int meshmask) {
	    this.from = from;
	    this.meshid = meshid;
	    this.meshmask = meshmask;
	}

	public static Collect parse(Resource res, Message buf) {
	    String nm = buf.string();
	    int ver = buf.uint16();
	    Indir<Resource> lres = res.pool.load(nm, ver);
	    int meshid = buf.int16();
	    int meshmask = buf.eom() ? -1 : buf.int16();
	    return(new Collect(lres, meshid, meshmask));
	}

	public Node make(Owner owner) {
	    if(res == null) {
		ArrayList<Node> cl = new ArrayList<>();
		for(FastMesh.MeshRes mr : from.get().layers(FastMesh.MeshRes.class)) {
		    if(((meshid >= 0) && (mr.id < 0)) || ((mr.id & meshmask) == meshid))
			cl.add(mr.mat.get().apply(mr.m));
		}
		final Node[] ca = cl.toArray(new Node[0]);
		res = new Node() {
			public void added(Slot slot) {
			    for(Node r : ca)
				slot.add(r);
			}
		    };
	    }
	    return(res);
	}
    }

    public static class Parameters implements RenderLink {
	public final Resource from;
	public final Indir<Resource> res;
	public final Object[] args;
	private Resource lres;
	private ArgLink link = null;

	public Parameters(Resource from, Indir<Resource> res, Object[] args) {
	    this.from = from;
	    this.res = res;
	    this.args = args;
	}

	public static Parameters parse(Resource res, Message buf) {
	    String nm = buf.string();
	    int ver = buf.uint16();
	    Object[] args = buf.list();
	    return(new Parameters(res, res.pool.load(nm, ver), args));
	}

	public Node make(Owner owner) {
	    if(link == null) {
		if(lres == null)
		    lres = res.get();
		link = lres.getcode(ArgLink.class, true);
	    }
	    return(link.create(owner, from, args));
	}
    }

    public static class ArgMaker implements Resource.PublishedCode.Instancer {
	public ArgLink make(Class<?> cl, Resource ires, Object... argv) {
	    if(ArgLink.class.isAssignableFrom(cl))
		return(Resource.PublishedCode.Instancer.stdmake(cl.asSubclass(ArgLink.class), ires, argv));
	    try {
		Function<Object[], Node> make = Utils.smthfun(cl, "mkrlink", Node.class, Owner.class, Resource.class, Object[].class);
		return((owner, res, args) -> make.apply(new Object[] {owner, res, args}));
	    } catch(NoSuchMethodException e) {}
	    if(Node.class.isAssignableFrom(cl)) {
		Class<? extends Node> scl = cl.asSubclass(Node.class);
		try {
		    Function<Object[], ? extends Node> make = Utils.consfun(scl, Owner.class, Resource.class, Object[].class);
		    return((owner, res, args) -> make.apply(new Object[] {owner, res, args}));
		} catch(NoSuchMethodException e) {}
	    }
	    throw(new RuntimeException("Could not find any suitable construct for dynamic renderlink"));
	}
    }

    @Resource.PublishedCode(name = "rlink", instancer = ArgMaker.class)
    public interface ArgLink {
	public Node create(Owner owner, Resource res, Object... args);
    }

    @Resource.LayerName("rlink")
    public class Res extends Resource.Layer implements Resource.IDLayer<Integer> {
	public transient final RenderLink l;
	public final int id;
	
	public Res(Resource res, Message buf) {
	    res.super();
	    int lver = buf.uint8();
	    int t;
	    if(lver < 3) {
		t = lver;
		id = -1;
	    } else if(lver == 3) {
		id = buf.int16();
		t = buf.uint8();
	    } else {
		throw(new Resource.LoadException("Invalid renderlink version: " + lver, res));
	    }
	    if(t == 0) {
		l = MeshMat.parse(res, buf);
	    } else if(t == 1) {
		l = AmbientLink.parse(res, buf);
	    } else if(t == 2) {
		l = Collect.parse(res, buf);
	    } else if(t == 3) {
		l = Parameters.parse(res, buf);
	    } else {
		throw(new Resource.LoadException("Invalid renderlink type: " + t, res));
	    }
	}
	
	public void init() {
	}

	public Integer layerid() {
	    return(id);
	}
    }
}
