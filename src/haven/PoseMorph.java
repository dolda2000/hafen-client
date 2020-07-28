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
import haven.Skeleton.Pose;

public class PoseMorph {
    public final Pose pose;
    private float[][] offs;
    private int seq = -1;

    public PoseMorph(Pose pose) {
	this.pose = pose;
	offs = new float[pose.skel().blist.length][16];
    }

    public static boolean boned(FastMesh mesh) {
	BoneData ba = mesh.vert.buf(BoneData.class);
	if(ba == null)
	    return(false);
	for(int i = 0; i < mesh.num * 3; i++) {
	    if(ba.data.get(mesh.indb.get(i) * ba.elfmt.nc) != -1)
		return(true);
	}
	return(false);
    }

    public static String boneidp(FastMesh mesh) {
	BoneData ba = mesh.vert.buf(BoneData.class);
	if(ba == null)
	    return(null);
	int retb = -1;
	for(int i = 0; i < mesh.num * 3; i++) {
	    int vi = mesh.indb.get(i) * ba.elfmt.nc;
	    int curb = ba.data.get(vi);
	    if(curb == -1)
		return(null);
	    if(retb == -1)
		retb = curb;
	    else if(retb != curb)
		return(null);
	    if((ba.elfmt.nc != 1) && (ba.data.get(vi + 1) != -1))
		return(null);
	}
	return(ba.names[retb]);
    }
    
    private void update() {
	if(seq == pose.seq)
	    return;
	seq = pose.seq;
	for(int i = 0; i < offs.length; i++)
	    pose.boneoff(i, offs[i]);
    }

    public static class BoneData extends VertexBuf.IntData {
	public final String[] names;
	
	public BoneData(int apv, IntBuffer data, String[] names) {
	    super(PoseMorph2.vba, apv, data);
	    this.names = names;
	}
    }

    public static class WeightData extends VertexBuf.FloatData {
	public WeightData(int apv, FloatBuffer data) {
	    super(PoseMorph2.vbw, apv, data);
	}
    }

    @VertexBuf.ResName("bones")
    public static class $Res implements VertexBuf.DataCons {
	public void cons(Collection<VertexBuf.AttribData> dst, Resource res, Message buf, int nv) {
	    int mba = buf.uint8();
	    IntBuffer ba = Utils.wibuf(nv * mba);
	    for(int i = 0; i < nv * mba; i++)
		ba.put(i, -1);
	    FloatBuffer bw = Utils.wfbuf(nv * mba);
	    byte[] na = new byte[nv];
	    List<String> bones = new LinkedList<String>();
	    while(true) {
		String bone = buf.string();
		if(bone.length() == 0)
		    break;
		int bidx = bones.size();
		bones.add(bone);
		while(true) {
		    int run = buf.uint16();
		    int vn = buf.uint16();
		    if(run == 0)
			break;
		    for(int i = 0; i < run; i++, vn++) {
			float w = buf.float32();
			int cna = na[vn]++;
			if(cna >= mba)
			    continue;
			bw.put(vn * mba + cna, w);
			ba.put(vn * mba + cna, bidx);
		    }
		}
	    }
	    int tbn = 4;
	    if(mba > tbn) {
		sortweights(bw, ba, mba);
		IntBuffer tba = Utils.wibuf(nv * tbn);
		FloatBuffer tbw = Utils.wfbuf(nv * tbn);
		for(int i = 0, off = 0, toff = 0; i < nv; i++, off += mba, toff += tbn) {
		    for(int o = 0; o < tbn; o++) {
			tbw.put(toff + o, bw.get(off + o));
			tba.put(toff + o, ba.get(off + o));
		    }
		}
		ba = tba; bw = tbw; mba = tbn;
	    } else if(mba < tbn) {
		IntBuffer tba = Utils.wibuf(nv * tbn);
		FloatBuffer tbw = Utils.wfbuf(nv * tbn);
		for(int i = 0, off = 0, toff = 0; i < nv; i++, off += mba, toff += tbn) {
		    for(int o = 0; o < mba; o++) {
			tbw.put(toff + o, bw.get(off + o));
			tba.put(toff + o, ba.get(off + o));
		    }
		    for(int o = mba; o < tbn; o++)
			tba.put(toff + o, -1);
		}
		ba = tba; bw = tbw; mba = tbn;
	    }
	    normweights(bw, ba, mba);
	    dst.add(new BoneData(mba, ba, bones.toArray(new String[0])));
	    dst.add(new WeightData(mba, bw));
	}
    }

    public static void sortweights(FloatBuffer bw, IntBuffer ba, int mba) {
	Integer[] p = new Integer[mba];
	float[] cw = new float[mba];
	int[] ca = new int[mba];
	for(int i = 0; i < bw.capacity(); i += mba) {
	    int n = 0;
	    for(int o = 0; o < mba; o++) {
		if(ba.get(i + o) < 0)
		    break;
		p[n++] = Integer.valueOf(o);
	    }
	    int ci = i;
	    Arrays.sort(p, 0, n, (a, b) -> {
		    float wa = bw.get(ci + a), wb = bw.get(ci + b);
		    if(wa < wb)
			return(1);
		    else if(wa > wb)
			return(-1);
		    return(0);
		});
	    for(int o = 0; o < n; o++) {
		cw[o] = bw.get(i + o);
		ca[o] = ba.get(i + o);
	    }
	    for(int o = 0; o < n; o++) {
		bw.put(i + o, cw[p[o]]);
		ba.put(i + o, ca[p[o]]);
	    }
	}
    }

    public static void normweights(FloatBuffer bw, IntBuffer ba, int mba) {
	int i = 0;
	while(i < bw.capacity()) {
	    float tw = 0.0f;
	    int n = 0;
	    for(int o = 0; o < mba; o++) {
		if(ba.get(i + o) < 0)
		    break;
		tw += bw.get(i + o);
		n++;
	    }
	    if(tw != 1.0f) {
		for(int o = 0; o < n; o++)
		    bw.put(i + o, bw.get(i + o) / tw);
	    }
	    i += mba;
	}
    }
}
