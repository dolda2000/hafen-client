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

    public void sdraw(GL gl) {
	gl.glBegin(GL.GL_TRIANGLES);
	for(int i = 0; i < num * 3; i++) {
	    int idx = indb.get(i);
	    int o = idx * 3;
	    gl.glNormal3f(vert.nrmb.get(o), vert.nrmb.get(o + 1), vert.nrmb.get(o + 2));
	    if(vert.texb != null) {
		int u = idx * 2;
		gl.glTexCoord2f(vert.texb.get(u), vert.texb.get(u + 1));
	    }
	    gl.glVertex3f(vert.posb.get(o), vert.posb.get(o + 1), vert.posb.get(o + 2));
	}
	gl.glEnd();
    }

    public void cdraw(GL gl) {
	vert.posb.rewind();
	vert.nrmb.rewind();
	indb.rewind();
	gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
	gl.glVertexPointer(3, GL.GL_FLOAT, 0, vert.posb);
	gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
	gl.glNormalPointer(GL.GL_FLOAT, 0, vert.nrmb);
	if(vert.texb != null) {
	    vert.texb.rewind();
	    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
	    gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, vert.texb);
	}
	gl.glDrawElements(GL.GL_TRIANGLES, num * 3, GL.GL_UNSIGNED_SHORT, indb);
	gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
	gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
	gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
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
		sdraw(gl);
		gl.glEndList();
		gl.glCallList(list.id);
	    } else {
		cdraw(gl);
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
	GL gl = g.gl;
	cdraw(gl);
	GOut.checkerr(gl);
    }
    
    public boolean setup(RenderList r) {
	return(true);
    }
    
    public boolean boned() {
	if(vert.apv == 0)
	    return(false);
	for(int i = 0; i < num * 3; i++) {
	    if(vert.assbones[indb.get(i) * vert.apv] != -1)
		return(true);
	}
	return(false);
    }

    public String boneidp() {
	int retb = -1;
	if(vert.apv == 0)
	    return(null);
	for(int i = 0; i < num * 3; i++) {
	    int vi = indb.get(i) * vert.apv;
	    int curb = vert.assbones[vi];
	    if(curb == -1)
		return(null);
	    if(retb == -1)
		retb = curb;
	    else if(retb != curb)
		return(null);
	    if((vert.apv != 1) && (vert.assbones[vi + 1] != -1))
		return(null);
	}
	return(vert.bones[retb]);
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
