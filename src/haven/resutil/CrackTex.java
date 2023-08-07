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

package haven.resutil;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.function.*;
import java.awt.Color;
import java.util.zip.GZIPInputStream;
import haven.render.Texture3D.Sampler3D;
import static haven.render.sl.Type.*;
import static haven.render.sl.Cons.*;

public class CrackTex extends State implements InstanceBatch.AttribState {
    public static final Slot<CrackTex> slot = new Slot<>(Slot.Type.DRAW, CrackTex.class)
	.instanced(new Instancable<CrackTex>() {
		final Instancer<CrackTex> nil = Instancer.dummy();
		public Instancer<CrackTex> instid(CrackTex st) {
		    return((st == null) ? nil : st.instancer());
		}
	    });
    public static final int texsz = 256;
    public static final Sampler3D[] imgs;
    public final Sampler3D img;
    public final Color color;
    public final float[] rot;

    public static class Decoder implements DataBuffer.Filler<Texture.Image> {
	public final Supplier<InputStream> src;
	private Defer.Future<FillBuffer[]> decode;
	private FillBuffer[] data;

	public Decoder(Supplier<InputStream> src) {
	    this.src = src;
	}

	private FillBuffer[] decode(Texture3D tex, Environment env) {
	    try(InputStream raw = src.get()) {
		InputStream fp = new GZIPInputStream(raw);
		FillBuffer[] data = new FillBuffer[tex.images().size()];
		ByteBuffer[] bufs = new ByteBuffer[data.length];
		for(int i = 0; i < data.length; i++) {
		    data[i] = env.fillbuf(tex.image(i));
		    bufs[i] = data[i].push();
		}
		for(int i = 0, n = 8, b = 0; i < texsz * texsz * texsz; i++, n++) {
		    if(n >= 8) {
			b = fp.read();
			n = 0;
		    }
		    if((b & 1) != 0)
			bufs[0].put(i, (byte)255);
		    else
			bufs[0].put(i, (byte)0);
		    b >>= 1;
		}
		for(int i = 1; i < data.length; i++) {
		    int lsz = texsz >> i, usz = lsz << 1, ssz = usz * usz;
		    int[] offs = new int[] {
			0, 1, usz, usz + 1,
			ssz, ssz + 1, ssz + usz, ssz + usz + 1
		    };
		    int lo = 0, uo = 0;
		    for(int z = 0; z < lsz; z++, uo += ssz) {
			for(int y = 0; y < lsz; y++, uo += usz) {
			    for(int x = 0; x < lsz; x++, lo++, uo += 2) {
				int v = 0;
				for(int o = 0; o < 8; o++)
				    v += bufs[i - 1].get(uo + offs[o]) & 0xff;
				v >>= 3;
				bufs[i].put(lo, (v >= 64) ? (byte)255 : (byte)0);
				/*
				if(v >= 128)
				    bufs[i].put(lo, (byte)255);
				else
				    bufs[i].put(lo, (byte)(v << 1));
				*/
			    }
			}
		    }
		}
		return(data);
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}

	public FillBuffer fill(Texture.Image img, Environment env) {
	    while(data == null) {
		if(decode == null)
		    decode = Defer.later(() -> this.decode((Texture3D)img.tex, env));
		data = decode.get();
		decode = null;
		if(!data[0].compatible(env))
		    data = null;
	    }
	    return(data[img.level]);
	}

	public void done() {
	    decode = null;
	    data = null;
	}
    }

    public static Sampler3D loadtex(Supplier<InputStream> fp) {
	Texture3D tex = new Texture3D(texsz, texsz, texsz, DataBuffer.Usage.STATIC, new VectorFormat(1, NumberFormat.UNORM8), new Decoder(fp));
	Sampler3D ret = tex.sampler();
	ret.minfilter(Texture.Filter.LINEAR).mipfilter(Texture.Filter.LINEAR);
	return(ret);
    }

    static {
	imgs = new Sampler3D[3];
	for(int i : Utils.range(0, 3, 1)) {
	    imgs[i] = loadtex(() -> CrackTex.class.getResourceAsStream("crack-tex-" + i + ".gz"));
	    imgs[i].tex.desc("crack-tex " + i);
	}
    }

    public CrackTex(Sampler3D img, Color color, Coord3f rax, float rang) {
	this.img = img;
	this.color = color;
	this.rot = MiscLib.rotasq(rax, rang);
    }

    public CrackTex(Sampler3D img, Color color) {
	this(img, color, Coord3f.zu, 0);
    }

    private static final Uniform u_tex = new Uniform(SAMPLER3D, "cracktex", p -> p.get(slot).img, slot);
    private static final Uniform u_col = new Uniform(VEC3, "crackcol", p -> p.get(slot).color, slot);
    private static final InstancedUniform u_rot = new InstancedUniform.Vec4("crackrot", p -> p.get(slot).rot, slot);
    private static final ShaderMacro shader = prog -> {
	final AutoVarying crackc = new AutoVarying(VEC3, "s_crackc") {
		protected Expression root(VertexContext vctx) {
		    return(MiscLib.vqrot.call(pick(Homo3D.vertex.ref(), "xyz"), u_rot.ref()));
		}
	    };
	FragColor.fragcol(prog.fctx).mod(in -> MiscLib.colblend.call(in, vec4(u_col.ref(),
									      texture3D(u_tex.ref(), mul(crackc.ref(), l(0.025))))),
					 100);
    };

    public ShaderMacro shader() {
	return(shader);
    }

    public void apply(Pipe buf) {
	buf.put(slot, this);
    }

    private static final Map<Sampler3D, Instancer<CrackTex>> instids = new WeakHashMap<>();
    private Instancer<CrackTex> instancer() {
	synchronized(instids) {
	    Instancer<CrackTex> ret = instids.get(img);
	    if(ret == null) {
		ret = new Instancer<CrackTex>() {
			public CrackTex inststate(CrackTex uinst, InstanceBatch bat) {
			    return(CrackTex.this);
			}
		    };
		instids.put(img, ret);
	    }
	    return(ret);
	}
    }

    public InstancedAttribute[] attribs() {
	return(new InstancedAttribute[] {u_rot.attrib});
    }
}
