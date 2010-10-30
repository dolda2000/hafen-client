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

public class MeshBuf {
    public final Collection<Vertex> v = new LinkedList<Vertex>();
    public final Collection<Face> f = new LinkedList<Face>();
    
    public class Vertex {
	public final Coord3f pos, nrm, tex;
	private short idx;
	
	public Vertex(Coord3f pos, Coord3f nrm, Coord3f tex) {
	    this.pos = pos;
	    this.nrm = nrm;
	    this.tex = tex;
	    v.add(this);
	}
	
	public Vertex(Coord3f pos, Coord3f nrm) {
	    this(pos, nrm, null);
	}
    }
    
    public class Face {
	public final Vertex v1, v2, v3;
	
	public Face(Vertex v1, Vertex v2, Vertex v3) {
	    this.v1 = v1; this.v2 = v2; this.v3 = v3;
	    f.add(this);
	}
    }
    
    public FastMesh mkmesh() {
	if(v.isEmpty() || f.isEmpty())
	    throw(new RuntimeException("Tried to build empty mesh"));
	float[] pos, nrm, tex;
	boolean hastex = false;
	pos = new float[v.size() * 3];
	nrm = new float[v.size() * 3];
	tex = new float[v.size() * 2];
	int pi = 0, ni = 0, ti = 0;
	short i = 0;
	for(Vertex v : this.v) {
	    pos[pi + 0] = v.pos.x;
	    pos[pi + 1] = v.pos.y;
	    pos[pi + 2] = v.pos.z;
	    nrm[ni + 0] = v.nrm.x;
	    nrm[ni + 1] = v.nrm.y;
	    nrm[ni + 2] = v.nrm.z;
	    if(v.tex != null) {
		hastex = true;
		tex[ti + 0] = v.tex.x;
		tex[ti + 1] = v.tex.y;
	    }
	    pi += 3;
	    ni += 3;
	    ti += 2;
	    v.idx = i++;
	}
	if(!hastex)
	    tex = null;
	VertexBuf vbuf = new VertexBuf(pos, nrm, tex);
	short[] idx = new short[f.size() * 3];
	int ii = 0;
	for(Face f : this.f) {
	    idx[ii + 0] = f.v1.idx;
	    idx[ii + 1] = f.v2.idx;
	    idx[ii + 2] = f.v3.idx;
	    ii += 3;
	}
	return(new FastMesh(vbuf, idx));
    }
}
