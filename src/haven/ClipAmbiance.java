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

public class ClipAmbiance implements Rendered {
    public final Desc desc;
    public double bvol;
    private Glob glob = null;

    public ClipAmbiance(Desc desc) {
	this.desc = desc;
	this.bvol = desc.bvol;
    }

    public static class Glob implements ActAudio.Global {
	public final Desc desc;
	private final Audio.DataClip[] cur;
	private int curn, n;
	private double vacc, cvol;
	private double lastupd = System.currentTimeMillis() / 1000.0;

	public Glob(Desc desc) {
	    this.desc = desc;
	    cur = new Audio.DataClip[desc.cnms.length];
	    addmin();
	}

	public int hashCode() {
	    return(desc.hashCode());
	}

	public boolean equals(Object other) {
	    return((other instanceof Glob) && (((Glob)other).desc == this.desc));
	}

	private void addclip(final int idx) {
	    Resource.Audio clip = AudioSprite.randoom(desc.getres(), desc.cnms[idx]);
	    synchronized(this) {
		cur[idx] = new Audio.DataClip(clip.pcmstream(), 0.0, 1.0) {
			protected void eof() {
			    super.eof();
			    synchronized(this) {
				cur[idx] = null;
				curn--;
			    }
			}
		    };
		curn++;
	    }
	}

	private void addmin() {
	    while(curn < desc.minc) {
		double wsum = 0.0;
		for(int i = 0; i < desc.cnms.length; i++) {
		    if(cur[i] == null)
			wsum += desc.ieps[i];
		}
		double p = Math.random() * wsum;
		for(int i = 0; i < desc.cnms.length; i++) {
		    if(cur[i] != null)
			continue;
		    if((p -= desc.ieps[i]) <= 0) {
			addclip(i);
			break;
		    }
		}
	    }
	}

	private void addsome(double td) {
	    if(curn >= desc.maxc)
		return;
	    for(int i = 0; i < desc.cnms.length; i++) {
		if(cur[i] != null)
		    continue;
		if(Math.random() < ((desc.ieps[i] * td * Math.min(n, desc.maxi)) / desc.maxi)) {
		    addclip(i);
		    return;
		}
	    }
	}

	public boolean cycle(ActAudio list) {
	    double now = System.currentTimeMillis() / 1000.0;
	    double td = Math.max(now - lastupd, 0.0);
	    addmin();
	    addsome(td);
	    if(vacc < cvol)
		cvol = Math.max(cvol - (td * 0.5), 0.0);
	    else if(vacc > cvol)
		cvol = Math.min(cvol + (td * 0.5), 1.0);
	    if((n == 0) && (cvol < 0.005))
		return(true);
	    vacc = 0.0;
	    n = 0;
	    lastupd = now;
	    for(Audio.DataClip clip : cur) {
		if(clip == null) continue;
		clip.vol = cvol;
		list.add(clip);
	    }
	    return(false);
	}

	public void add(double vol) {
	    vacc += vol;
	    n++;
	}
    }

    public void draw(GOut g) {
	g.apply();
	if(glob == null) {
	    ActAudio list = g.st.cur(ActAudio.slot);
	    if(list == null)
		return;
	    glob = list.intern(new Glob(desc));
	}
	Coord3f pos = g.st.mv.mul4(Coord3f.o);
	double pd = Math.sqrt((pos.x * pos.x) + (pos.y * pos.y));
	double svol = Math.min(1.0, 50.0 / pd);
	glob.add(svol * bvol);
    }

    public boolean setup(RenderList rl) {
	return(true);
    }

    @Resource.LayerName("clamb")
    public static class Desc extends Resource.Layer {
	public final int minc, maxc, maxi;
	public final double bvol;
	public final String[] cnms;
	public final double[] ieps;

	public Desc(Resource res, byte[] bbuf) {
	    res.super();
	    Message buf = new Message(0, bbuf);
	    int ver = buf.uint8();
	    if(ver != 1)
		throw(new Resource.LoadException("Unknown clip-ambiance version: " + ver, getres()));
	    minc = buf.uint8(); maxc = buf.uint8();
	    maxi = buf.uint16(); bvol = buf.float32();
	    cnms = new String[buf.uint8()];
	    ieps = new double[cnms.length];
	    for(int i = 0; i < cnms.length; i++) {
		cnms[i] = buf.string().intern();
		ieps[i] = 1.0 / buf.float32();
	    }
	}

	public void init() {}
    }
}
