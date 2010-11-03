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

public class FastMesh implements Rendered {
    public final VertexBuf vert;
    public final ShortBuffer indb;
    public final int num;
    
    public FastMesh(VertexBuf vert, short[] ind) {
	this.vert = vert;
	num = ind.length / 3;
	if(ind.length != num * 3)
	    throw(new RuntimeException("Invalid index array length"));
	indb = Utils.bufcp(ind);
    }

    public void sdraw(GL gl) {
	gl.glBegin(GL.GL_TRIANGLES);
	for(int i = 0; i < num * 3; i++) {
	    int o = indb.get(i) * 3;
	    gl.glNormal3f(vert.nrmb.get(o), vert.nrmb.get(o + 1), vert.nrmb.get(o + 2));
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
	    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
	    gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, vert.texb);
	}
	gl.glDrawElements(GL.GL_TRIANGLES, num * 3, GL.GL_UNSIGNED_SHORT, indb);
	gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
	gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
	gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
    }

    public void draw(GOut g) {
	GL gl = g.gl;
	gl.glColor3f(1.0f, 1.0f, 1.0f);
	cdraw(gl);
	GOut.checkerr(gl);
    }
    
    public boolean setup(RenderList r) {
	return(true);
    }
    
    public static class MeshRes extends Resource.Layer {
	public transient FastMesh m;
	public transient Material mat;
	private transient short[] tmp;
	private int matid;
	
	public MeshRes(Resource res, byte[] buf) {
	    res.super();
	    int fl = Utils.ub(buf[0]);
	    int num = Utils.uint16d(buf, 1);
	    matid = Utils.int16d(buf, 3);
	    int off = 5;
	    short[] ind = new short[num * 3];
	    for(int i = 0; i < num * 3; i++)
		ind[i] = (short)Utils.int16d(buf, off + (i * 2));
	    this.tmp = ind;
	}
	
	public void init() {
	    VertexBuf v = getres().layer(VertexBuf.VertexRes.class).b;
	    this.m = new FastMesh(v, this.tmp);
	    this.tmp = null;
	    if(matid >= 0) {
		for(Material.Res mr : getres().layers(Material.Res.class)) {
		    if(mr.m.id == matid)
			this.mat = mr.m;
		}
		if(this.mat == null)
		    throw(new Resource.LoadException("Could not find specified material: " + matid, getres()));
	    }
	}
    }
}
