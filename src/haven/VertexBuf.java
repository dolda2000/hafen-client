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
import java.util.*;

public class VertexBuf {
    public final FloatBuffer posb, nrmb, texb;
    public final int num;
    public VertexBuf from = null;
    public int apv = 0;
    public int[] assbones;
    public float[] assweights;
    public String[] bones;
    
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
    
    private static FloatBuffer bufdup(FloatBuffer in) {
	if(in == null)
	    return(null);
	in.rewind();
	FloatBuffer ret = Utils.mkfbuf(in.remaining());
	ret.put(in);
	ret.rewind();
	return(ret);
    }

    public VertexBuf(VertexBuf from, boolean posk, boolean nrmk, boolean texk) {
	this.from = from;
	if(posk)
	    this.posb = from.posb;
	else
	    this.posb = bufdup(from.posb);
	if(nrmk)
	    this.nrmb = from.nrmb;
	else
	    this.nrmb = bufdup(from.nrmb);
	if(!texk)
	    this.texb = from.texb;
	else
	    this.texb = bufdup(from.texb);
	this.num = from.num;
	this.apv = from.apv;
	this.assbones = from.assbones;
	this.assweights = from.assweights;
	this.bones = from.bones;
    }

    public static class VertexRes extends Resource.Layer {
	public transient final VertexBuf b;
	
	public VertexRes(Resource res, byte[] buf) {
	    res.super();
	    float[] pos = null, nrm = null, tex = null, bw = null;
	    int mba = 0;
	    int[] ba = null;
	    String[] bn = null;
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
		} else if(id == 3) {
		    if(ba != null)
			throw(new Resource.LoadException("Duplicate vertex bone information", getres()));
		    mba = Utils.ub(buf[off++]);
		    ba = new int[num * mba];
		    for(int i = 0; i < ba.length; i++)
			ba[i] = -1;
		    bw = new float[num * mba];
		    int[] na = new int[num];
		    List<String> bones = new ArrayList<String>();
		    while(true) {
			int[] ob = {off};
			String bone = Utils.strd(buf, ob);
			off = ob[0];
			if(bone.length() == 0)
			    break;
			int bidx = bones.size();
			bones.add(bone);
			while(true) {
			    int run = Utils.uint16d(buf, off); off += 2;
			    int st = Utils.uint16d(buf, off); off += 2;
			    if(run == 0)
				break;
			    for(int i = 0; i < run; i++) {
				float w = (float)Utils.floatd(buf, off);
				off += 5;
				int v = i + st;
				int cna = na[v]++;
				if(cna >= mba)
				    continue;
				bw[v * mba + cna] = w;
				ba[v * mba + cna] = bidx;
			    }
			}
		    }
		    normweights(bw, ba, mba);
		    bn = bones.toArray(new String[0]);
		}
	    }
	    this.b = new VertexBuf(pos, nrm, tex);
	    if(ba != null) {
		this.b.apv = mba;
		this.b.assbones = ba;
		this.b.assweights = bw;
		this.b.bones = bn;
	    }
	}
	
	private static void normweights(float[] bw, int[] ba, int mba) {
	    int i = 0;
	    while(i < bw.length) {
		float tw = 0.0f;
		int n = 0;
		for(int o = 0; o < mba; o++) {
		    if(ba[i + o] < 0)
			break;
		    tw += bw[i + o];
		    n++;
		}
		if(tw != 1.0f) {
		    for(int o = 0; o < n; o++)
			bw[i + o] = bw[i + o] / tw;
		}
		i += mba;
	    }
	}
	
	public void init() {}
    }
}
