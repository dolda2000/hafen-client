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
import haven.render.VertexArray.Layout;
import haven.render.Model.Indices;
import haven.render.Rendered;

public class FastMesh implements Rendered.Instancable, RenderTree.Node, Disposable {
    public final VertexBuf vert;
    public final ShortBuffer indb;
    public final int num;
    public final Model model;
    private Coord3f nb, pb;

    public FastMesh(VertexBuf vert, ShortBuffer ind) {
	this.vert = vert;
	num = ind.capacity() / 3;
	if(ind.capacity() != num * 3)
	    throw(new RuntimeException("Invalid index array length"));
	this.indb = ind;
	this.model = new Model(Model.Mode.TRIANGLES, vert.data(),
			       new Indices(num * 3, NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::indfill).shared(),
			       0, num * 3);
    }

    public FastMesh(VertexBuf vert, short[] ind) {
	this(vert, ShortBuffer.wrap(ind));
    }

    private FillBuffer indfill(Indices ibuf, Environment env) {
	FillBuffer dst = env.fillbuf(ibuf);
	ShortBuffer buf = dst.push().asShortBuffer();
	ShortBuffer tx = indb.duplicate();
	tx.rewind();
	buf.put(tx);
	return(dst);
    }

    private void cbounds() {
	Coord3f nb = null, pb = null;
	VertexBuf.VertexData vbuf = null;
	for(VertexBuf.AttribData buf : vert.bufs) {
	    if(buf instanceof VertexBuf.VertexData) {
		vbuf = (VertexBuf.VertexData)buf;
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

    public void draw(Pipe context, Render out) {
	out.draw(context, model);
    }

    public void dispose() {
	model.ind.dispose();
	model.dispose();
	vert.dispose();
    }

    public class Instanced implements Rendered.Instanced {
	public final InstanceBatch bat;
	private final InstanceBatch.AttributeData attr;
	private final Layout fmt;
	private VertexArray data;
	private Model model;
	private int ninst;

	private Layout mkfmt(Layout.Input[] ifmt) {
	    VertexArray sdat = vert.data();
	    Layout.Input[] inputs = new Layout.Input[sdat.fmt.inputs.length + ifmt.length];
	    for(int i = 0; i < sdat.fmt.inputs.length; i++)
		inputs[i] = sdat.fmt.inputs[i];
	    for(int i = 0; i < ifmt.length; i++) {
		Layout.Input si = ifmt[i];
		inputs[i + sdat.fmt.inputs.length] = new Layout.Input(si.tgt, si.el, sdat.fmt.nbufs, si.offset, si.stride, true);
	    }
	    return(new Layout(inputs));
	}

	private VertexArray mkdata(Layout.Input[] ifmt, VertexArray.Buffer ibuf) {
	    if(ibuf == null)
		return(null);
	    VertexArray sdat = vert.data();
	    VertexArray.Buffer[] bufs = new VertexArray.Buffer[sdat.bufs.length + 1];
	    for(int i = 0; i < sdat.bufs.length; i++)
		bufs[i] = sdat.bufs[i];
	    bufs[sdat.bufs.length] = ibuf;
	    return(new VertexArray(this.fmt, bufs).shared());
	}

	private Instanced(InstanceBatch bat) {
	    this.bat = bat;
	    this.attr = new InstanceBatch.AttributeData(bat);
	    this.fmt = mkfmt(attr.fmt);
	    vertupdate();
	    modupdate(false);
	}

	public void draw(Pipe context, Render out) {
	    out.draw(context, model);
	}

	private void modupdate(boolean batupd) {
	    if(model != null)
		model.dispose();
	    Model smod = FastMesh.this.model;
	    model = new Model(smod.mode, (data != null) ? data : vert.data(),
			      smod.ind, smod.f, smod.n,
			      ninst);
	    if(batupd)
		bat.instupdate();
	}

	private void vertupdate() {
	    if(data != null)
		data.dispose();
	    data = mkdata(this.attr.fmt, attr.buf());
	}

	public void iupdate(int idx) {
	    boolean vu = attr.iupdate(idx);
	    ninst = Math.max(ninst, idx + 1);
	    if(vu)
		vertupdate();
	    if(vu || ((model != null) && (model.ninst != ninst)))
		modupdate(true);
	}

	public void itrim(int idx) {
	    boolean vu = attr.itrim(idx);
	    ninst = Math.min(ninst, idx);
	    if(vu)
		vertupdate();
	    if(vu || ((model != null) && (model.ninst != ninst)))
		modupdate(true);
	}

	public void commit(Render g) {
	    attr.commit(g);
	}

	public void dispose() {
	    if(model != null)
		model.dispose();
	}
    }

    public Rendered.Instanced instancify(InstanceBatch bat) {
	return(new Instanced(bat));
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
	private int vbufid, matid;
	
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
	    if((fl & 16) != 0)
		vbufid = buf.int16();
	    else
		vbufid = 0;
	    if((fl & ~31) != 0)
		throw(new Resource.LoadException("Unsupported flags in fastmesh: " + fl, getres()));
	    short[] ind = new short[num * 3];
	    for(int i = 0; i < num * 3; i++)
		ind[i] = (short)buf.uint16();
	    this.tmp = ind;
	}
	
	public void init() {
	    VertexBuf v = getres().layer(VertexBuf.VertexRes.class, vbufid).b;
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
