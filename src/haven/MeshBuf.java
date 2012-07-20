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

public class MeshBuf {
    public final Collection<Vertex> v = new LinkedList<Vertex>();
    public final Collection<Face> f = new LinkedList<Face>();
    private VertexBuf vbuf = null;
    
    public class Vertex {
	public Coord3f pos, nrm, tex;
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
	
	public String toString() {
	    return(String.format("MeshBuf.Vertex(%s, %s, %s)", pos, nrm, tex));
	}
    }
    
    public class Face {
	public final Vertex v1, v2, v3;
	
	public Face(Vertex v1, Vertex v2, Vertex v3) {
	    this.v1 = v1; this.v2 = v2; this.v3 = v3;
	    f.add(this);
	}
    }
    
    public Vertex[] copy(FastMesh src) {
	int min = -1, max = -1;
	for(int i = 0; i < src.num * 3; i++) {
	    int idx = src.indb.get(i);
	    if((min < 0) || (idx < min))
		min = idx;
	    if(idx > max)
		max = idx;
	}
	int nv = 0;
	VertexBuf.VertexArray posb = src.vert.buf(VertexBuf.VertexArray.class);
	VertexBuf.NormalArray nrmb = src.vert.buf(VertexBuf.NormalArray.class);
	VertexBuf.TexelArray texb = src.vert.buf(VertexBuf.TexelArray.class);
	Vertex[] vmap = new Vertex[max + 1 - min];
	for(int i = 0; i < src.num * 3; i++) {
	    int idx = src.indb.get(i);
	    if(vmap[idx - min] == null) {
		int o = idx * posb.n;
		Coord3f pos = new Coord3f(posb.data.get(o), posb.data.get(o + 1), posb.data.get(o + 2));
		o = idx * nrmb.n;
		Coord3f nrm = new Coord3f(nrmb.data.get(o), nrmb.data.get(o + 1), nrmb.data.get(o + 2));
		Coord3f tex = null;
		if(texb != null) {
		    o = idx * texb.n;
		    tex = new Coord3f(texb.data.get(o), texb.data.get(o + 1), 0);
		}
		vmap[idx - min] = new Vertex(pos, nrm, tex);
		nv++;
	    }
	}
	for(int i = 0; i < src.num; i++) {
	    int o = i * 3;
	    new Face(vmap[src.indb.get(o) - min],
		     vmap[src.indb.get(o + 1) - min],
		     vmap[src.indb.get(o + 2) - min]);
	}
	Vertex[] vl = new Vertex[nv];
	int n = 0;
	for(int i = 0; i < vmap.length; i++) {
	    if(vmap[i] != null)
		vl[n++] = vmap[i];
	}
	return(vl);
    }
    
    private void mkvbuf() {
	if(v.isEmpty())
	    throw(new RuntimeException("Tried to build empty vertex buffer"));
	FloatBuffer pos, nrm, tex;
	boolean hastex = false;
	pos = Utils.mkfbuf(v.size() * 3);
	nrm = Utils.mkfbuf(v.size() * 3);
	tex = Utils.mkfbuf(v.size() * 2);
	int pi = 0, ni = 0, ti = 0;
	short i = 0;
	for(Vertex v : this.v) {
	    pos.put(pi + 0, v.pos.x);
	    pos.put(pi + 1, v.pos.y);
	    pos.put(pi + 2, v.pos.z);
	    nrm.put(pi + 0, v.nrm.x);
	    nrm.put(pi + 1, v.nrm.y);
	    nrm.put(pi + 2, v.nrm.z);
	    if(v.tex != null) {
		hastex = true;
		tex.put(ti + 0, v.tex.x);
		tex.put(ti + 1, v.tex.y);
	    }
	    pi += 3;
	    ni += 3;
	    ti += 2;
	    v.idx = i++;
	    if(i == 0)
		throw(new RuntimeException("Too many vertices in meshbuf"));
	}
	if(!hastex)
	    this.vbuf = new VertexBuf(new VertexBuf.VertexArray(pos), new VertexBuf.NormalArray(nrm));
	else
	    this.vbuf = new VertexBuf(new VertexBuf.VertexArray(pos), new VertexBuf.NormalArray(nrm), new VertexBuf.TexelArray(tex));
    }

    public void clearfaces() {
	this.f.clear();
    }

    public FastMesh mkmesh() {
	if(f.isEmpty())
	    throw(new RuntimeException("Tried to build empty mesh"));
	if(this.vbuf == null)
	    mkvbuf();
	short[] idx = new short[f.size() * 3];
	int ii = 0;
	for(Face f : this.f) {
	    idx[ii + 0] = f.v1.idx;
	    idx[ii + 1] = f.v2.idx;
	    idx[ii + 2] = f.v3.idx;
	    ii += 3;
	}
	return(new FastMesh(this.vbuf, idx));
    }
    
    public boolean emptyp() {
	return(f.isEmpty());
    }
}
