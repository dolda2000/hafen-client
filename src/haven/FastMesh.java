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
import java.nio.*;
import haven.render.*;
import haven.render.Model.Indices;
import haven.render.Rendered;

public class FastMesh implements Rendered, RenderTree.Node, Disposable {
    public final VertexBuf vert;
    public final ShortBuffer indb;
    public final int num;
    public final Model model;

    public FastMesh(VertexBuf vert, ShortBuffer ind) {
	this.vert = vert;
	num = ind.capacity() / 3;
	if(ind.capacity() != num * 3)
	    throw(new RuntimeException("Invalid index array length"));
	this.indb = ind;
	this.model = new Model(Model.Mode.TRIANGLES, vert.data,
			       new Indices(num * 3, NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::indfill),
			       0, num * 3);
    }

    public FastMesh(VertexBuf vert, short[] ind) {
	this(vert, ShortBuffer.wrap(ind));
    }

    private FillBuffer indfill(Indices ibuf, Environment env) {
	FillBuffer dst = env.fillbuf(ibuf);
	ShortBuffer buf = dst.push().asShortBuffer();
	indb.rewind();
	buf.put(indb);
	return(dst);
    }

    /* XXXRENDER
    private void cbounds() {
	Coord3f nb = null, pb = null;
	VertexBuf.VertexArray vbuf = null;
	for(VertexBuf.AttribArray buf : vert.bufs) {
	    if(buf instanceof VertexBuf.VertexArray) {
		vbuf = (VertexBuf.VertexArray)buf;
		break;
	    }
	}
	for(int i = 0; i < indb.capacity(); i++) {
	    int vi = indb.get(i) * 3;
	    float x = vbuf.data.get(vi), y = vbuf.data.get(vi + 1), z = vbuf.data.get(vi + 2);
	    if(nb == null) {
		nb = new Coord3f(x, y, z);
		pb = new Coord3f(x, y, z);
	    } else {
		nb.x = Math.min(nb.x, x); pb.x = Math.max(pb.x, x);
		nb.y = Math.min(nb.y, y); pb.y = Math.max(pb.y, y);
		nb.z = Math.min(nb.z, z); pb.z = Math.max(pb.z, z);
	    }
	}
	this.nb = nb;
	this.pb = pb;
    }

    public Coord3f nbounds() {
	if(nb == null) cbounds();
	return(nb);
    }
    public Coord3f pbounds() {
	if(pb == null) cbounds();
	return(pb);
    }
    */

    public void draw(Pipe context, Render out) {
	out.draw(context, model);
    }

    public void dispose() {
	model.dispose();
	vert.dispose();
    }

    public static class ResourceMesh extends FastMesh {
	public final int id;
	public final Resource res;
	public final MeshRes info;
	
	public ResourceMesh(VertexBuf vert, short[] ind, MeshRes info) {
	    super(vert, ind);
	    this.id = info.id;
	    this.res = info.getres();
	    this.info = info;
	}
	
	public String toString() {
	    return("FastMesh(" + res.name + ", " + id + ")");
	}
    }

    @Resource.LayerName("mesh")
    public static class MeshRes extends Resource.Layer implements Resource.IDLayer<Integer> {
	public transient FastMesh m;
	public transient Material.Res mat;
	public final Map<String, String> rdat;
	private transient short[] tmp;
	public final int id, ref;
	private int matid;
	
	public MeshRes(Resource res, Message buf) {
	    res.super();
	    int fl = buf.uint8();
	    int num = buf.uint16();
	    matid = buf.int16();
	    if((fl & 2) != 0) {
		id = buf.int16();
	    } else {
		id = -1;
	    }
	    if((fl & 4) != 0) {
		ref = buf.int16();
	    } else {
		ref = -1;
	    }
	    Map<String, String> rdat = new HashMap<String, String>();
	    if((fl & 8) != 0) {
		while(true) {
		    String k = buf.string();
		    if(k.equals(""))
			break;
		    rdat.put(k, buf.string());
		}
	    }
	    this.rdat = Collections.unmodifiableMap(rdat);
	    if((fl & ~15) != 0)
		throw(new Resource.LoadException("Unsupported flags in fastmesh: " + fl, getres()));
	    short[] ind = new short[num * 3];
	    for(int i = 0; i < num * 3; i++)
		ind[i] = (short)buf.uint16();
	    this.tmp = ind;
	}
	
	public void init() {
	    VertexBuf v = getres().layer(VertexBuf.VertexRes.class).b;
	    this.m = new ResourceMesh(v, this.tmp, this);
	    this.tmp = null;
	    if(matid >= 0) {
		for(Material.Res mr : getres().layers(Material.Res.class)) {
		    if(mr.id == matid)
			this.mat = mr;
		}
		if(this.mat == null)
		    throw(new Resource.LoadException("Could not find specified material: " + matid, getres()));
	    }
	}
	
	public Integer layerid() {
	    return(id);
	}
    }
}
