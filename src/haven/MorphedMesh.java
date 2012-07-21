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
import java.lang.ref.*;
import haven.Skeleton.Pose;

public class MorphedMesh extends FastMesh {
    private static WeakHashMap<Pose, Collection<MorphedBuf>> bufs = new WeakHashMap<Pose, Collection<MorphedBuf>>();
    
    private static MorphedBuf buf(VertexBuf buf, Pose pose) {
	Collection<MorphedBuf> bl;
	synchronized(bufs) {
	    bl = bufs.get(pose);
	    if(bl == null)
		bufs.put(pose, bl = new LinkedList<MorphedBuf>());
	}
	synchronized(bl) {
	    for(MorphedBuf b : bl) {
		if(b.from == buf)
		    return(b);
	    }
	    MorphedBuf b = new MorphedBuf(buf, pose);
	    bl.add(b);
	    return(b);
	}
    }
    
    public MorphedMesh(FastMesh mesh, Pose pose) {
        super(mesh, buf(mesh.vert, pose));
    }
    
    public static boolean boned(FastMesh mesh) {
	MorphedBuf.BoneArray ba = mesh.vert.buf(MorphedBuf.BoneArray.class);
	if(ba == null)
	    return(false);
	for(int i = 0; i < mesh.num * 3; i++) {
	    if(ba.data.get(mesh.indb.get(i) * ba.n) != -1)
		return(true);
	}
	return(false);
    }

    public static String boneidp(FastMesh mesh) {
	MorphedBuf.BoneArray ba = mesh.vert.buf(MorphedBuf.BoneArray.class);
	if(ba == null)
	    return(null);
	int retb = -1;
	for(int i = 0; i < mesh.num * 3; i++) {
	    int vi = mesh.indb.get(i) * ba.n;
	    int curb = ba.data.get(vi);
	    if(curb == -1)
		return(null);
	    if(retb == -1)
		retb = curb;
	    else if(retb != curb)
		return(null);
	    if((ba.n != 1) && (ba.data.get(vi + 1) != -1))
		return(null);
	}
	return(ba.names[retb]);
    }
    
    public boolean setup(RenderList rl) {
	((MorphedBuf)vert).update();
	return(super.setup(rl));
    }
    
    protected boolean compile() {
	return(false);
    }
    
    public String toString() {
	return("morphed(" + from + ")");
    }

    public static class MorphedBuf extends VertexBuf {
	private final VertexBuf from;
	private final WeakReference<Pose> poseref;
	private int seq = 0;
	
	public static class BoneArray extends IntArray {
	    public final String[] names;
	
	    public BoneArray(int apv, IntBuffer data, String[] names) {
		super(apv, data);
		this.names = names;
	    }
	
	    public BoneArray dup() {return(new BoneArray(n, Utils.bufcp(data), Utils.splice(names, 0)));}
	}
    
	public static class WeightArray extends FloatArray {
	    public WeightArray(int apv, FloatBuffer data) {
		super(apv, data);
	    }
	}
    
	private static AttribArray[] ohBitterSweetJavaDays(VertexBuf from) {
	    AttribArray[] ret = new AttribArray[from.bufs.length];
	    for(int i = 0; i < from.bufs.length; i++) {
		if(from.bufs[i] instanceof VertexArray)
		    ret[i] = ((VertexArray)from.bufs[i]).dup();
		else if(from.bufs[i] instanceof NormalArray)
		    ret[i] = ((NormalArray)from.bufs[i]).dup();
		else if(from.bufs[i] instanceof BoneArray)
		    ret[i] = ((BoneArray)from.bufs[i]).dup();
		else
		    ret[i] = from.bufs[i];
	    }
	    return(ret);
	}

	private MorphedBuf(VertexBuf buf, Pose pose) {
	    super(ohBitterSweetJavaDays(buf));
	    this.from = buf;
	    this.poseref = new WeakReference<Pose>(pose);
	    BoneArray ob = buf.buf(BoneArray.class);
	    BoneArray nb = buf(BoneArray.class);
	    int[] xl = new int[nb.names.length];
	    for(int i = 0; i < xl.length; i++)
		xl[i] = pose.skel().bones.get(nb.names[i]).idx;
	    for(int i = 0; i < ob.data.capacity(); i++) {
		if(ob.data.get(i) == -1)
		    nb.data.put(i, -1);
		else
		    nb.data.put(i, xl[ob.data.get(i)]);
	    }
	}
	
	public void update() {
	    Pose pose = poseref.get();
	    if(seq == pose.seq)
		return;
	    seq = pose.seq;
	    float[][] offs = new float[pose.skel().blist.length][16];
	    for(int i = 0; i < offs.length; i++)
		pose.boneoff(i, offs[i]);
	    FloatBuffer opos = from.buf(VertexArray.class).data, onrm = from.buf(NormalArray.class).data;
	    FloatBuffer npos =      buf(VertexArray.class).data, nnrm =      buf(NormalArray.class).data;
	    BoneArray ba = buf(BoneArray.class);
	    int apv = ba.n;
	    IntBuffer bl = ba.data;
	    FloatBuffer wl = buf(WeightArray.class).data;
	    int vo = 0, ao = 0;
	    for(int i = 0; i < num; i++) {
		float opx = opos.get(vo), opy = opos.get(vo + 1), opz = opos.get(vo + 2);
		float onx = onrm.get(vo), ony = onrm.get(vo + 1), onz = onrm.get(vo + 2);
		float npx = 0, npy = 0, npz = 0;
		float nnx = 0, nny = 0, nnz = 0;
		float rw = 1;
		for(int o = 0; o < apv; o++) {
		    int bi = bl.get(ao + o);
		    if(bi < 0)
			break;
		    float bw = wl.get(ao + o);
		    float[] xf = offs[bi];
		    npx += ((xf[ 0] * opx) + (xf[ 4] * opy) + (xf[ 8] * opz) + xf[12]) * bw;
		    npy += ((xf[ 1] * opx) + (xf[ 5] * opy) + (xf[ 9] * opz) + xf[13]) * bw;
		    npz += ((xf[ 2] * opx) + (xf[ 6] * opy) + (xf[10] * opz) + xf[14]) * bw;
		    nnx += ((xf[ 0] * onx) + (xf[ 4] * ony) + (xf[ 8] * onz)) * bw;
		    nny += ((xf[ 1] * onx) + (xf[ 5] * ony) + (xf[ 9] * onz)) * bw;
		    nnz += ((xf[ 2] * onx) + (xf[ 6] * ony) + (xf[10] * onz)) * bw;
		    rw -= bw;
		}
		npx += opx * rw; npy += opy * rw; npz += opz * rw;
		nnx += onx * rw; nny += ony * rw; nnz += onz * rw;
		npos.put(vo, npx); npos.put(vo + 1, npy); npos.put(vo + 2, npz);
		nnrm.put(vo, nnx); nnrm.put(vo + 1, nny); nnrm.put(vo + 2, nnz);
		vo += 3;
		ao += apv;
	    }
	}
    }
}
