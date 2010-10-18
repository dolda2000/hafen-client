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

public class VertexBuf {
    public final FloatBuffer posb, nrmb, texb;
    public final int num;
    
    public VertexBuf(float[] pos, float[] nrm, float[] tex) {
	if((pos == null) || (nrm == null))
	    throw(new RuntimeException("VertexBuf needs at least positions and normals"));
	num = pos.length / 3;
	if(pos.length != num * 3)
	    throw(new RuntimeException("Invalid position array length"));
	if(nrm.length != num * 3)
	    throw(new RuntimeException("Invalid position array length"));
	posb = Utils.bufcp(pos);
	nrmb = Utils.bufcp(nrm);
	if(tex != null) {
	    if(tex.length != num * 2)
		throw(new RuntimeException("Invalid position array length"));
	    texb = Utils.bufcp(tex);
	} else {
	    texb = null;
	}
    }
    
    public static class VertexRes extends Resource.Layer {
	public transient final VertexBuf b;
	
	public VertexRes(Resource res, byte[] buf) {
	    res.super();
	    float[] pos = null, nrm = null, tex = null;
	    int fl = Utils.ub(buf[0]);
	    int num = Utils.uint16d(buf, 1);
	    int off = 3;
	    while(off < buf.length) {
		int id = Utils.ub(buf[off++]);
		if(id == 0) {
		    if(pos != null)
			throw(new Resource.LoadException("Duplicate vertex position information", getres()));
		    pos = new float[num * 3];
		    for(int i = 0; i < num * 3; i++)
			pos[i] = (float)Utils.floatd(buf, off + (i * 5));
		    off += num * 5 * 3;
		} else if(id == 1) {
		    if(nrm != null)
			throw(new Resource.LoadException("Duplicate vertex normal information", getres()));
		    nrm = new float[num * 3];
		    for(int i = 0; i < num * 3; i++)
			nrm[i] = (float)Utils.floatd(buf, off + (i * 5));
		    off += num * 5 * 3;
		} else if(id == 2) {
		    if(tex != null)
			throw(new Resource.LoadException("Duplicate vertex texel information", getres()));
		    tex = new float[num * 2];
		    for(int i = 0; i < num * 2; i++)
			tex[i] = (float)Utils.floatd(buf, off + (i * 5));
		    off += num * 5 * 2;
		}
	    }
	    this.b = new VertexBuf(pos, nrm, tex);
	}
	
	public void init() {}
    }
}
