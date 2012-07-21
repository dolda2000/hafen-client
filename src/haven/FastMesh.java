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

import java.nio.*;
import javax.media.opengl.*;

public class FastMesh implements FRendered {
    public final VertexBuf vert;
    public final ShortBuffer indb;
    public final int num;
    public FastMesh from;
    private DisplayList list = null;
    
    public FastMesh(VertexBuf vert, short[] ind) {
	this.vert = vert;
	num = ind.length / 3;
	if(ind.length != num * 3)
	    throw(new RuntimeException("Invalid index array length"));
	indb = Utils.bufcp(ind);
    }

    public FastMesh(FastMesh from, VertexBuf vert) {
	this.from = from;
	if(from.vert.num != vert.num)
	    throw(new RuntimeException("V-buf sizes must match"));
	this.vert = vert;
	this.indb = from.indb;
	this.num = from.num;
    }

    public void sdraw(GOut g) {
	GL gl = g.gl;
	VertexBuf.GLArray[] data = new VertexBuf.GLArray[vert.bufs.length];
	VertexBuf.VertexArray vbuf = null;
	int n = 0;
	for(int i = 0; i < vert.bufs.length; i++) {
	    if(vert.bufs[i] instanceof VertexBuf.VertexArray)
		vbuf = (VertexBuf.VertexArray)vert.bufs[i];
	    else if(vert.bufs[i] instanceof VertexBuf.GLArray)
		data[n++] = (VertexBuf.GLArray)vert.bufs[i];
	}
	gl.glBegin(GL.GL_TRIANGLES);
	for(int i = 0; i < num * 3; i++) {
	    int idx = indb.get(i);
	    for(int o = 0; o < n; o++)
		data[o].set(g, idx);
	    vbuf.set(g, idx);
	}
	gl.glEnd();
    }

    public void cdraw(GOut g) {
	indb.rewind();
	for(int i = 0; i < vert.bufs.length; i++) {
	    if(vert.bufs[i] instanceof VertexBuf.GLArray)
		((VertexBuf.GLArray)vert.bufs[i]).bind(g);
	}
	g.gl.glDrawElements(GL.GL_TRIANGLES, num * 3, GL.GL_UNSIGNED_SHORT, indb);
	for(int i = 0; i < vert.bufs.length; i++) {
	    if(vert.bufs[i] instanceof VertexBuf.GLArray)
		((VertexBuf.GLArray)vert.bufs[i]).unbind(g);
	}
    }
    
    public void draw(GOut g) {
	g.apply();
	GL gl = g.gl;
	if((list != null) && (!g.gc.usedl || (list.gl != gl))) {
	    list.dispose();
	    list = null;
	}
	if(list != null) {
	    gl.glCallList(list.id);
	} else {
	    if(compile() && g.gc.usedl) {
		list = new DisplayList(gl);
		gl.glNewList(list.id, GL.GL_COMPILE);
		sdraw(g);
		gl.glEndList();
		gl.glCallList(list.id);
	    } else {
		cdraw(g);
	    }
	}
	GOut.checkerr(gl);
    }
    
    protected boolean compile() {
	return(true);
    }
    
    public void updated() {
	synchronized(this) {
	    if(list != null) {
		list.dispose();
		list = null;
	    }
	}
    }
    
    public void dispose() {
	updated();
    }
    
    public void drawflat(GOut g) {
	g.apply();
	cdraw(g);
	GOut.checkerr(g.gl);
    }
    
    public boolean setup(RenderList r) {
	return(true);
    }
    
    public static class ResourceMesh extends FastMesh {
	public final int id;
	public final Resource res;
	
	public ResourceMesh(VertexBuf vert, short[] ind, MeshRes info) {
	    super(vert, ind);
	    this.id = info.id;
	    this.res = info.getres();
	}
	
	public String toString() {
	    return("FastMesh(" + res.name + ", " + id + ")");
	}
    }

    public static class MeshRes extends Resource.Layer implements Resource.IDLayer<Integer> {
	public transient FastMesh m;
	public transient Material.Res mat;
	private transient short[] tmp;
	public final int id, ref;
	private int matid;
	
	public MeshRes(Resource res, byte[] buf) {
	    res.super();
	    int[] off = {0};
	    int fl = Utils.ub(buf[off[0]]); off[0] += 1;
	    int num = Utils.uint16d(buf, off[0]); off[0] += 2;
	    matid = Utils.int16d(buf, off[0]); off[0] += 2;
	    if((fl & 2) != 0) {
		id = Utils.int16d(buf, off[0]); off[0] += 2;
	    } else {
		id = -1;
	    }
	    if((fl & 4) != 0) {
		ref = Utils.int16d(buf, off[0]); off[0] += 2;
	    } else {
		ref = -1;
	    }
	    if((fl & ~7) != 0)
		throw(new Resource.LoadException("Unsupported flags in fastmesh: " + fl, getres()));
	    short[] ind = new short[num * 3];
	    for(int i = 0; i < num * 3; i++)
		ind[i] = (short)Utils.int16d(buf, off[0] + (i * 2));
	    this.tmp = ind;
	}
	
	public void init() {
	    VertexBuf v = getres().layer(VertexBuf.VertexRes.class, false).b;
	    this.m = new ResourceMesh(v, this.tmp, this);
	    this.tmp = null;
	    if(matid >= 0) {
		for(Material.Res mr : getres().layers(Material.Res.class, false)) {
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
