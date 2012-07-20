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
import javax.media.opengl.GL;

public class VertexBuf {
    public final AttribArray[] bufs;
    public final int num;
    
    public VertexBuf(AttribArray... bufs) {
	AttribArray[] na = new AttribArray[bufs.length];
	na[0] = bufs[0];
	int num = na[0].size();
	for(int i = 1; i < bufs.length; i++) {
	    na[i] = bufs[i];
	    if(na[i].size() != num)
		throw(new RuntimeException("Buffer sizes do not match"));
	}
	this.bufs = na;
	this.num = num;
    }
    
    public <T extends AttribArray> T buf(Class<T> type) {
	for(AttribArray a : bufs) {
	    if(type.isInstance(a))
		return(type.cast(a));
	}
	return(null);
    }
    
    public abstract static class AttribArray {
	public final int n;
	
	public AttribArray(int n) {
	    this.n = n;
	}
	
	public abstract Buffer data();
	
	public int size() {
	    Buffer b = data();
	    b.rewind();
	    return(b.capacity() / this.n);
	}
    }
    
    public static interface GLArray {
	public void set(GOut g, int idx);
	public void bind(GOut g);
	public void unbind(GOut g);
    }

    public abstract static class FloatArray extends AttribArray {
	public final FloatBuffer data;
	
	public FloatArray(int n, FloatBuffer data) {
	    super(n);
	    data.rewind();
	    if(data.capacity() % n != 0)
		throw(new RuntimeException(String.format("float-array length %d does not match element count %d", data.capacity(), n)));
	    this.data = data;
	}
	
	public FloatBuffer data() {
	    return(data);
	}
    }
    
    public abstract static class IntArray extends AttribArray {
	public final IntBuffer data;
	
	public IntArray(int n, IntBuffer data) {
	    super(n);
	    data.rewind();
	    if(data.capacity() % n != 0)
		throw(new RuntimeException(String.format("int-array length %d does not match element count %d", data.capacity(), n)));
	    this.data = data;
	}
	
	public IntBuffer data() {
	    return(data);
	}
    }
    
    public static class VertexArray extends FloatArray implements GLArray {
	public VertexArray(FloatBuffer data) {
	    super(3, data);
	}
	
	public VertexArray dup() {return(new VertexArray(Utils.bufcp(data)));}
	
	public void set(GOut g, int idx) {
	    GL gl = g.gl;
	    int i = idx * 3;
	    gl.glVertex3f(data.get(i), data.get(i + 1), data.get(i + 2));
	}
	
	public void bind(GOut g) {
	    GL gl = g.gl;
	    data.rewind();
	    gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
	    gl.glVertexPointer(3, GL.GL_FLOAT, 0, data);
	}
	
	public void unbind(GOut g) {
	    g.gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
	}
    }
    
    public static class NormalArray extends FloatArray implements GLArray {
	public NormalArray(FloatBuffer data) {
	    super(3, data);
	}
	
	public NormalArray dup() {return(new NormalArray(Utils.bufcp(data)));}

	public void set(GOut g, int idx) {
	    GL gl = g.gl;
	    int i = idx * 3;
	    gl.glNormal3f(data.get(i), data.get(i + 1), data.get(i + 2));
	}
	
	public void bind(GOut g) {
	    GL gl = g.gl;
	    data.rewind();
	    gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
	    gl.glNormalPointer(GL.GL_FLOAT, 0, data);
	}
	
	public void unbind(GOut g) {
	    g.gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
	}
    }
    
    public static class TexelArray extends FloatArray implements GLArray {
	public TexelArray(FloatBuffer data) {
	    super(2, data);
	}

	public TexelArray dup() {return(new TexelArray(Utils.bufcp(data)));}

	public void set(GOut g, int idx) {
	    GL gl = g.gl;
	    int i = idx * 2;
	    gl.glTexCoord2f(data.get(i), data.get(i + 1));
	}
	
	public void bind(GOut g) {
	    GL gl = g.gl;
	    data.rewind();
	    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
	    gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, data);
	}
	
	public void unbind(GOut g) {
	    g.gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
	}
    }
    
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
    
    public static class VertexRes extends Resource.Layer {
	public transient final VertexBuf b;
	
	public VertexRes(Resource res, byte[] buf) {
	    res.super();
	    ArrayList<AttribArray> bufs = new ArrayList<AttribArray>();
	    int fl = Utils.ub(buf[0]);
	    int num = Utils.uint16d(buf, 1);
	    int off = 3;
	    while(off < buf.length) {
		int id = Utils.ub(buf[off++]);
		if(id == 0) {
		    FloatBuffer data = Utils.mkfbuf(num * 3);
		    for(int i = 0; i < num * 3; i++)
			data.put((float)Utils.floatd(buf, off + (i * 5)));
		    off += num * 5 * 3;
		    bufs.add(new VertexArray(data));
		} else if(id == 1) {
		    FloatBuffer data = Utils.mkfbuf(num * 3);
		    for(int i = 0; i < num * 3; i++)
			data.put((float)Utils.floatd(buf, off + (i * 5)));
		    off += num * 5 * 3;
		    bufs.add(new NormalArray(data));
		} else if(id == 2) {
		    FloatBuffer data = Utils.mkfbuf(num * 2);
		    for(int i = 0; i < num * 2; i++)
			data.put((float)Utils.floatd(buf, off + (i * 5)));
		    off += num * 5 * 2;
		    bufs.add(new TexelArray(data));
		} else if(id == 3) {
		    int mba = Utils.ub(buf[off++]);
		    IntBuffer ba = Utils.mkibuf(num * mba);
		    for(int i = 0; i < num * mba; i++)
			ba.put(-1);
		    ba.rewind();
		    FloatBuffer bw = Utils.mkfbuf(num * mba);
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
				bw.put(v * mba + cna, w);
				ba.put(v * mba + cna, bidx);
			    }
			}
		    }
		    normweights(bw, ba, mba);
		    bufs.add(new BoneArray(mba, ba, bones.toArray(new String[0])));
		    bufs.add(new WeightArray(mba, bw));
		}
	    }
	    this.b = new VertexBuf(bufs.toArray(new AttribArray[0]));
	}
	
	private static void normweights(FloatBuffer bw, IntBuffer ba, int mba) {
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
	
	public void init() {}
    }
}
