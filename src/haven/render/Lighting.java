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

package haven.render;

import java.util.*;
import haven.*;
import haven.render.sl.*;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

public interface Lighting {
    public static final State.Slot<State> lights = new State.Slot<>(State.Slot.Type.SYS, State.class);
    public static final Struct s_light = Struct.make(new Symbol.Shared("light"),
						     VEC4, "amb",
						     VEC4, "dif",
						     VEC4, "spc",
						     VEC4, "pos",
						     FLOAT, "ac",
						     FLOAT, "al",
						     FLOAT, "aq");

    public static abstract class LightList implements ShaderMacro {
	public abstract void construct(Block blk, java.util.function.Function<Params, Statement> body);

	public static class Params {
	    public Expression idx, lpar;

	    public Params(Expression idx, Expression lpar) {this.idx = idx; this.lpar = lpar;}
	}

	public void modify(ProgramContext prog) {
	    prog.module(this);
	}
    }

    public static class SimpleLights extends State {
	public static final int maxlights = 4;
	public static final boolean unroll = true;
	public static final Uniform u_nlights = new Uniform(INT, "nlights", p -> ((SimpleLights)p.get(lights)).list.length, lights);
	public static final Uniform u_lights = new Uniform(new Array(s_light, maxlights), "lights", p -> ((SimpleLights)p.get(lights)).list, lights);
	private final Object[][] list;

	public SimpleLights(Object[][] lights) {
	    this.list = lights;
	}

	private static final ShaderMacro shader = prog -> {
	    prog.module(new LightList() {
		    public void construct(Block blk, java.util.function.Function<Params, Statement> body) {
			if(!unroll) {
			    Variable i = blk.local(INT, "i", null);
			    blk.add(new For(ass(i, l(0)), lt(i.ref(), u_nlights.ref()), linc(i.ref()),
					    body.apply(new Params(i.ref(), idx(u_lights.ref(), i.ref())))));
			} else {
			    for(int i = 0; i < maxlights; i++) {
				/* Some old drivers and/or hardware seem to be
				 * having trouble with the for loop. Might not be
				 * as much of a problem these days as it used to
				 * be, but keep this for now, especially if
				 * SimpleLights are to be more of a legacy
				 * concern. */
				blk.add(new If(gt(u_nlights.ref(), l(i)),
					       body.apply(new Params(l(i), idx(u_lights.ref(), l(i))))));
			    }
			}
		    }
		});
	};
	public ShaderMacro shader() {return(shader);}

	public void apply(Pipe p) {
	    p.put(lights, this);
	}
    }

    public static class LightGrid {
	public static final int maxlights = 4;
	public static final float threshold = 1f / 256f;
	public final int w, h, d;

	public LightGrid(int w, int h, int d) {
	    this.w = w;
	    this.h = h;
	    this.d = d;
	}

	private static final Hash<short[]> sahash = new Hash<short[]>() {
		public int hash(short[] ob) {return(Arrays.hashCode(ob));}
		public boolean equal(short[] x, short[] y) {return(Arrays.equals(x, y));}
	    };

	private static final Coord3f[] clipcorn = {
	    Coord3f.of(-1, -1, -1), Coord3f.of( 1, -1, -1), Coord3f.of( 1,  1, -1), Coord3f.of(-1,  1, -1),
	    Coord3f.of(-1, -1,  1), Coord3f.of( 1, -1,  1), Coord3f.of( 1,  1,  1), Coord3f.of(-1,  1,  1),
	};

	private class Compiler {
	    final Volume3f bbox;
	    final Coord3f gsz, szf;
	    final Collection<Short> global = new ArrayList<>();
	    final short[] grid = new short[w * h * d];
	    short[][] lists = new short[][] {new short[0]};
	    short[][] table = new short[32][];
	    int nlists = 1;
	    int maxlist = 0;

	    Compiler(Projection proj) {
		Matrix4f iproj = proj.fin(Matrix4f.id).invert();
		Volume3f bbox = Volume3f.point(Coord3f.of(HomoCoord4f.fromiclip(iproj, clipcorn[0])));
		for(int i = 1; i < clipcorn.length; i++)
		    bbox = bbox.include(Coord3f.of(HomoCoord4f.fromiclip(iproj, clipcorn[i])));
		this.bbox = bbox;
		gsz = bbox.sz().div(w, h, d);
		szf = Coord3f.of(1, 1, 1).div(gsz);
	    }

	    int us(short v) {
		return(v & 0xffff);
	    }

	    void rehash(int nlen) {
		short[][] ntab = new short[nlen][];
		short[] ntnum = new short[nlen];
		for(int b = 0; b < table.length; b++) {
		    if(table[b] == null)
			continue;
		    for(int bi = 0; (bi < table[b].length) && (table[b][bi] != -1); bi++) {
			int ln = us(table[b][bi]);
			short[] list = lists[ln];
			int hash = 0;
			for(int i = 0; i < list.length; i++)
			    hash = (hash * 31) + list[i];
			int nb = hash & (nlen - 1);
			if(ntab[nb] == null) {
			    ntab[nb] = new short[] {(short)ln, -1, -1, -1};
			    ntnum[nb] = 1;
			} else {
			    int tlen = us(ntnum[nb]);
			    if(tlen == ntab[nb].length) {
				ntab[nb] = Arrays.copyOf(ntab[nb], tlen * 2);
				Arrays.fill(ntab[nb], tlen, tlen * 2, (short)-1);
			    }
			    ntab[nb][tlen] = (short)ln;
			    ntnum[nb] = (short)(tlen + 1);
			}
		    }
		}
		table = ntab;
	    }

	    void ckrehash() {
		if(nlists >= table.length / 2)
		    rehash(table.length * 2);
	    }

	    short addlist(short[] plist, short add) {
		if(nlists == lists.length)
		    lists = Arrays.copyOf(lists, lists.length * 2);
		lists[nlists] = Arrays.copyOf(plist, plist.length + 1);
		lists[nlists][plist.length] = add;
		if(Debug.ff)
		    Debug.dump(lists[nlists]);
		maxlist = Math.max(maxlist, lists[nlists].length);
		return((short)nlists++);
	    }

	    short getlist(short[] plist, short add) {
		int hash = 0;
		for(int i = 0; i < plist.length; i++)
		    hash = (hash * 31) + plist[i];
		hash = (hash * 31) + add;
		int b = hash & (table.length - 1);
		if(table[b] == null) {
		    short ret = addlist(plist, add);
		    table[b] = new short[] {ret, -1, -1, -1};
		    ckrehash();
		    return(ret);
		} else {
		    list: for(int bi = 0; bi < table[b].length; bi++) {
			if(table[b][bi] == -1) {
			    short ret = table[b][bi] = addlist(plist, add);
			    ckrehash();
			    return(ret);
			}
			int ln = us(table[b][bi]);
			if((lists[ln].length == (plist.length + 1))) {
			    for(int li = 0; li < plist.length; li++) {
				if(plist[li] != lists[ln][li])
				    continue list;
			    }
			    if(lists[ln][plist.length] == add)
				return((short)ln);
			}
		    }
		    int n = table[b].length;
		    table[b] = Arrays.copyOf(table[b], n * 2);
		    Arrays.fill(table[b], n, table[b].length, (short)-1);
		    short ret = table[b][n] = addlist(plist, add);
		    ckrehash();
		    return(ret);
		}
	    }

	    void addpoint(int idx, Object[] light, float[] pos) {
		float lx = pos[0], ly = pos[1], lz = pos[2];
		Coord3f lc = Coord3f.of(lx, ly, lz);
		float ac = (Float)light[4];
		float al = (Float)light[5];
		float aq = (Float)light[6];
		float aqi = 1f / aq;
		float r = -(al * aqi * 0.5f) + (float)Math.sqrt((aqi / threshold) - (ac * aqi) + (al * al * aqi * aqi * 0.25f));
		if(Debug.ff)
		    Debug.dump(r);
		if(bbox.closest(lc).dist(lc) > r)
		    return;
		int nz = (int)Math.floor((lz - r - bbox.n.z) * szf.z), pz = (int)Math.ceil((lz + r - bbox.n.z) * szf.z);
		int ng = 0;
		for(int gz = Math.max(nz, 0); gz < Math.min(pz, d); gz++) {
		    float gnz = (gz * gsz.z) + bbox.n.z, gpz = gnz + gsz.z;
		    float cz = Utils.clip(lz, gnz, gpz), dz = lz - cz;
		    float ey = (float)Math.sqrt((r * r) - (dz * dz));
		    int ny = (int)Math.floor((ly - ey - bbox.n.y) * szf.y), py = (int)Math.ceil((ly + ey - bbox.n.y) * szf.y);
		    for(int gy = Math.max(ny, 0); gy < Math.min(py, h); gy++) {
			float gny = (gy * gsz.y) + bbox.n.y, gpy = gny + gsz.y;
			float cy = Utils.clip(ly, gny, gpy), dy = ly - cy;
			float ex = (float)Math.sqrt(((r * r) - (dy * dy) - (dz * dz)));
			int nx = (int)Math.floor((lx - ex - bbox.n.x) * szf.x), px = (int)Math.ceil((lx + ex - bbox.n.x) * szf.x);
			for(int gx = Math.max(nx, 0); gx < Math.min(px, w); gx++) {
			    int gri = gx + (gy * w) + (gz * w * h);
			    if((lists[us(grid[gri])].length + global.size()) >= maxlights)
				return;
			}
		    }
		}
		short cpval = -1, cnval = -1;
		for(int gz = Math.max(nz, 0); gz < Math.min(pz, d); gz++) {
		    float gnz = (gz * gsz.z) + bbox.n.z, gpz = gnz + gsz.z;
		    float cz = Utils.clip(lz, gnz, gpz), dz = lz - cz;
		    float ey = (float)Math.sqrt((r * r) - (dz * dz));
		    int ny = (int)Math.floor((ly - ey - bbox.n.y) * szf.y), py = (int)Math.ceil((ly + ey - bbox.n.y) * szf.y);
		    for(int gy = Math.max(ny, 0); gy < Math.min(py, h); gy++) {
			float gny = (gy * gsz.y) + bbox.n.y, gpy = gny + gsz.y;
			float cy = Utils.clip(ly, gny, gpy), dy = ly - cy;
			float ex = (float)Math.sqrt(((r * r) - (dy * dy) - (dz * dz)));
			int nx = (int)Math.floor((lx - ex - bbox.n.x) * szf.x), px = (int)Math.ceil((lx + ex - bbox.n.x) * szf.x);
			for(int gx = Math.max(nx, 0); gx < Math.min(px, w); gx++) {
			    int gri = gx + (gy * w) + (gz * w * h);
			    short val = grid[gri];
			    if(val == cpval) {
				grid[gri] = cnval;
			    } else {
				cpval = val;
				cnval = grid[gri] = getlist(lists[grid[gri]], (short)idx);
			    }
			    ng++;
			}
		    }
		}
		if(Debug.ff)
		    Debug.dump(ng);
	    }

	    void addglobal(int idx, Object[] light) {
		if(global.size() + maxlist >= maxlights)
		    return;
		global.add((short)idx);
	    }

	    void addlight(int idx, Object[] light) {
		float[] pos = (float[])light[3];
		if(pos[3] == 0) {
		    addglobal(idx, light);
		} else {
		    addpoint(idx, light, pos);
		}
	    }

	    void dump() {
		try(java.io.BufferedWriter fp = java.nio.file.Files.newBufferedWriter(Debug.somedir("clight-dump"))) {
		    for(int z = 0; z < d; z++) {
			fp.write(Integer.toString(z));
			fp.write('\n');
			for(int y = 0; y < h; y++) {
			    for(int x = 0; x < w; x++)
				fp.write('0' + grid[x + (y * w) + (z * w * h)]);
			    fp.write('\n');
			}
			fp.write('\n');
		    }
		    for(int i = 0; i < nlists; i++) {
			fp.write(String.format("%d:", i));
			for(short v : lists[i]) {
			    fp.write(' ');
			    fp.write(Integer.toString(v));
			}
			fp.write('\n');
		    }
		} catch(java.io.IOException e) {
		    e.printStackTrace();
		}
	    }
	}

	public State compile(Object[][] lights, Projection proj) {
	    Compiler c = new Compiler(proj);
	    if(Debug.ff)
		Debug.dump(c.bbox, c.gsz);
	    int n = Math.min(lights.length, 65535);
	    for(int i = 0; i < n; i++)
		c.addlight(i, lights[i]);
	    if(Debug.ff)
		Debug.dump(c.maxlist, c.nlists);
	    if(Debug.ff)
		c.dump();
	    return(null);
	}

	public static class GridLights extends State {
	    public GridLights(Object[][] lights) {
	    }

	    private static final ShaderMacro shader = prog -> {
		prog.module(new LightList() {
			public void construct(Block blk, java.util.function.Function<Params, Statement> body) {
			}
		    });
	    };
	    public ShaderMacro shader() {return(shader);}

	    public void apply(Pipe p) {
		p.put(lights, this);
	    }
	}
    }
}
