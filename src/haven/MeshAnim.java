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
import haven.MorphedMesh.Morpher;
import haven.MorphedMesh.MorphedBuf;

public class MeshAnim {
    public final Frame[] frames;
    public final float len;

    public MeshAnim(Frame[] frames, float len) {
	this.frames = frames;
	this.len = len;
    }

    public static class Frame {
	public final float time;
	public final int[] idx;
	public final float[] pos, nrm;

	public Frame(float time, int[] idx, float[] pos, float[] nrm) {
	    this.time = time;
	    this.idx = idx;
	    this.pos = pos;
	    this.nrm = nrm;
	}
    }

    public boolean animp(FastMesh mesh) {
	int min = -1, max = -1;
	for(int i = 0; i < mesh.num * 3; i++) {
	    int vi = mesh.indb.get(i);
	    if(min < 0) {
		min = max = vi;
	    } else {
		if(vi < min)
		    min = vi;
		else if(vi > max)
		    max = vi;
	    }
	}
	boolean[] used = new boolean[max + 1 - min];
	for(int i = 0; i < mesh.num * 3; i++) {
	    int vi = mesh.indb.get(i);
	    used[vi - min] = true;
	}
	for(Frame f : frames) {
	    for(int i = 0; i < f.idx.length; i++) {
		int vi = f.idx[i];
		if((vi < min) || (vi > max))
		    continue;
		if(used[f.idx[i] - min])
		    return(true);
	    }
	}
	return(false);
    }

    public class Anim implements Morpher.Factory {
	public float time = 0.0f;
	private Frame cf, nf;
	private float a;
	private int seq = 0;

	public Anim() {
	    aupdate(0.0f);
	}

	public void aupdate(float time) {
	    if(time > len)
		time = len;
	    float ct, nt;
	    int l = 0, r = frames.length;
	    while(true) {
		int c = l + ((r - l) >> 1);
		ct = frames[c].time;
		nt = (c < frames.length - 1)?(frames[c + 1].time):len;
		if(ct > time) {
		    r = c;
		} else if(nt < time) {
		    l = c + 1;
		} else {
		    cf = frames[c];
		    nf = frames[(c + 1) % frames.length];
		    if(nt == ct)
			a = 0;
		    else
			a = (time - ct) / (nt - ct);
		    break;
		}
	    }
	    seq++;
	}

	public void tick(float dt) {
	    this.time += dt;
	    while(this.time > len)
		this.time -= len;
	    aupdate(this.time);
	}

	public Morpher create(final MorphedBuf vb) {
	    return(new Morpher() {
		    int lseq = -1;
		    public boolean update() {
			if(lseq == seq)
			    return(false);
			lseq = seq;
			return(true);
		    }

		    public void morphp(FloatBuffer dst, FloatBuffer src) {
			Frame f;
			float a;

			if(dst != src) {
			    int l = dst.capacity();
			    for(int i = 0; i < l; i++)
				dst.put(i, src.get(i));
			}
			f = cf;
			a = 1.0f - Anim.this.a;
			for(int i = 0, po = 0; i < f.idx.length; i++, po += 3) {
			    int vo = f.idx[i] * 3;
			    float x = dst.get(vo), y = dst.get(vo + 1), z = dst.get(vo + 2);
			    x += f.pos[po] * a; y += f.pos[po + 1] * a; z += f.pos[po + 2] * a;
			    dst.put(vo, x).put(vo + 1, y).put(vo + 2, z);
			}
			f = nf;
			a = Anim.this.a;
			for(int i = 0, po = 0; i < f.idx.length; i++, po += 3) {
			    int vo = f.idx[i] * 3;
			    float x = dst.get(vo), y = dst.get(vo + 1), z = dst.get(vo + 2);
			    x += f.pos[po] * a; y += f.pos[po + 1] * a; z += f.pos[po + 2] * a;
			    dst.put(vo, x).put(vo + 1, y).put(vo + 2, z);
			}
		    }

		    public void morphd(FloatBuffer dst, FloatBuffer src) {
			Frame f;
			float a;

			if(dst != src) {
			    int l = dst.capacity();
			    for(int i = 0; i < l; i++)
				dst.put(i, src.get(i));
			}
			f = cf;
			a = 1.0f - Anim.this.a;
			for(int i = 0, po = 0; i < f.idx.length; i++, po += 3) {
			    int vo = f.idx[i] * 3;
			    float x = dst.get(vo), y = dst.get(vo + 1), z = dst.get(vo + 2);
			    x += f.nrm[po] * a; y += f.nrm[po + 1] * a; z += f.nrm[po + 2] * a;
			    dst.put(vo, x).put(vo + 1, y).put(vo + 2, z);
			}
			f = nf;
			a = Anim.this.a;
			for(int i = 0, po = 0; i < f.idx.length; i++, po += 3) {
			    int vo = f.idx[i] * 3;
			    float x = dst.get(vo), y = dst.get(vo + 1), z = dst.get(vo + 2);
			    x += f.nrm[po] * a; y += f.nrm[po + 1] * a; z += f.nrm[po + 2] * a;
			    dst.put(vo, x).put(vo + 1, y).put(vo + 2, z);
			}
		    }
		});
	}
    }

    @Resource.LayerName("manim")
    public static class Res extends Resource.Layer {
	public final int id;
	public final MeshAnim a;

	public Res(Resource res, byte[] data) {
	    res.super();
	    Message buf = new Message(0, data);
	    id = buf.int16();
	    float len = buf.float32();
	    List<Frame> frames = new LinkedList<Frame>();
	    while(true) {
		int t = buf.uint8();
		if(t == 0)
		    break;
		float tm = buf.float32();
		int n = buf.uint16();
		int[] idx = new int[n];
		float[] pos = new float[n * 3];
		float[] nrm = new float[n * 3];
		int i = 0;
		while(i < n) {
		    int st = buf.uint16();
		    int run = buf.uint16();
		    for(int o = 0; o < run; o++) {
			idx[i] = st + o;
			pos[(i * 3) + 0] = buf.float32();
			pos[(i * 3) + 1] = buf.float32();
			pos[(i * 3) + 2] = buf.float32();
			nrm[(i * 3) + 0] = buf.float32();
			nrm[(i * 3) + 1] = buf.float32();
			nrm[(i * 3) + 2] = buf.float32();
			i++;
		    }
		}
		frames.add(new Frame(tm, idx, pos, nrm));
	    }
	    a = new MeshAnim(frames.toArray(new Frame[0]), len);
	}

	public void init() {
	}
    }
}
