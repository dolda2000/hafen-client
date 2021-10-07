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
import haven.render.*;
import haven.Audio.CS;
import haven.Audio.VolAdjust;
import haven.Audio.Mixer;

public class ClipAmbiance implements RenderTree.Node {
    public final Desc desc;
    public double bvol;
    private Desc parent;
    private Glob glob = null;

    public ClipAmbiance(Desc desc) {
	this.desc = desc;
	this.bvol = desc.bvol;
    }

    public static class Glob implements ActAudio.Global {
	public final Desc desc;
	public final ActAudio list;
	private final Collection<RenderList.Slot<ClipAmbiance>> active = new ArrayList<>();
	private final Mixer mixer = new Mixer(true);
	private VolAdjust volc = null;
	private boolean dead = false;
	private Desc[] chans = {null};
	private CS[][] cur = {null};
	private int curn;
	private int[] n = {0};
	private double cvol;
	private double lastupd = Utils.rtime();

	public Glob(Desc desc, ActAudio list) {
	    this.desc = desc;
	    this.list = list;
	}

	public int hashCode() {
	    return(desc.hashCode());
	}

	public boolean equals(Object other) {
	    return((other instanceof Glob) && (((Glob)other).desc == this.desc));
	}

	private void addclip(final int chan, final int idx) {
	    Audio.Clip clip = AudioSprite.randoom(chans[chan].getres(), chans[chan].cnms[idx]);
	    final CS[] clist = cur[chan];
	    CS nc;
	    synchronized(this) {
		nc = new Audio.Monitor(clip.stream()) {
			protected void eof() {
			    synchronized(Glob.this) {
				clist[idx] = null;
				curn--;
			    }
			}
		    };
		clist[idx] = nc;
		curn++;
	    }
	    mixer.add(nc);
	}

	private void addmin() {
	    while((curn < desc.minc) && (chans[0] != null)) {
		double wsum = 0.0;
		for(int i = 0; (i < chans.length) && (chans[i] != null); i++) {
		    for(int o = 0; o < chans[i].cnms.length; o++) {
			if(cur[i][o] == null)
			    wsum += chans[i].ieps[o];
		    }
		}
		double p = Math.random() * wsum;
		for(int i = 0; (i < chans.length) && (chans[i] != null); i++) {
		    for(int o = 0; o < chans[i].cnms.length; o++) {
			if(cur[i][o] != null)
			    continue;
			if((p -= chans[i].ieps[o]) <= 0) {
			    addclip(i, o);
			    break;
			}
		    }
		}
	    }
	}

	private void addsome(double td) {
	    if(curn >= desc.maxc)
		return;
	    int ns;
	    synchronized(active) {
		/* XXX? Shouldn't n[i] be used instead? */
		ns = active.size();
	    }
	    for(int i = 0; (i < chans.length) && (chans[i] != null); i++) {
		for(int o = 0; o < chans[i].cnms.length; o++) {
		    if((cur[i][o] != null) || (n[i] < 1))
			continue;
		    if(Math.random() < ((chans[i].ieps[o] * td * Math.min(ns, desc.maxi)) / desc.maxi)) {
			addclip(i, o);
			return;
		    }
		}
	    }
	}

	private boolean playing(int ch) {
	    for(int i = 0; i < cur[ch].length; i++) {
		if(cur[ch][i] != null)
		    return(true);
	    }
	    return(false);
	}

	private void trim() {
	    int i = 0, o = 0;
	    for(; (i < chans.length) && (chans[i] != null); i++) {
		if((n[i] > 0) || playing(i)) {
		    chans[o] = chans[i];
		    cur[o] = cur[i];
		    n[o] = n[i];
		    o++;
		}
	    }
	    for(; o < chans.length; o++) {
		chans[o] = null;
		cur[o] = null;
		n[o] = 0;
	    }
	}

	private double curvol() {
	    double acc = 0;
	    for(RenderList.Slot<ClipAmbiance> slot : active) {
		Coord3f pos = ActAudio.spos(slot.state());
		double bvol = slot.obj().bvol;
		double svol = Math.min(1.0, 50.0 / Math.hypot(pos.x, pos.y));
		acc += svol * bvol;
	    }
	    return(acc);
	}

	public boolean cycle(ActAudio list) {
	    double now = Utils.rtime();
	    double td = Math.max(now - lastupd, 0.0);
	    addmin();
	    addsome(td);
	    synchronized(active) {
		double vacc = curvol();
		if(vacc < cvol)
		    cvol = Math.max(cvol - (td * 0.5), 0.0);
		else if(vacc > cvol)
		    cvol = Math.min(cvol + (td * 0.5), 1.0);
		if(active.isEmpty() && (cvol < 0.005)) {
		    dead = true;
		    list.amb.remove(volc);
		    return(true);
		}
	    }
	    lastupd = now;
	    if(volc == null) {
		volc = new VolAdjust(mixer, cvol);
		list.amb.add(volc);
	    } else {
		volc.vol = cvol;
	    }
	    return(false);
	}

	public void add(Desc ch, RenderList.Slot<ClipAmbiance> slot) {
	    if(ch == null)
		throw(new NullPointerException());
	    synchronized(active) {
		int i;
		for(i = 0; i < chans.length; i++) {
		    if((chans[i] == null) || (chans[i] == ch))
			break;
		}
		if(i == chans.length) {
		    int nn = chans.length * 2;
		    chans = Utils.extend(chans, nn);
		    cur = Utils.extend(cur, nn);
		    n = Utils.extend(n, nn);
		}
		if(chans[i] == null) {
		    chans[i] = ch;
		    cur[i] = new CS[ch.cnms.length];
		    n[i] = 0;
		}
		n[i]++;
		active.add(slot);
	    }
	}

	public void remove(Desc ch, RenderList.Slot<ClipAmbiance> slot) {
	    if(ch == null)
		throw(new NullPointerException());
	    synchronized(active) {
		if(!active.remove(slot))
		    return;
		int i;
		for(i = 0; i < chans.length; i++) {
		    if(chans[i] == ch) {
			n[i]--;
			break;
		    }
		}
		trim();
	    }
	}
    }

    public void added(RenderTree.Slot slot) {
	ActAudio list = slot.state().get(ActAudio.audio);
	if(list == null)
	    return;
	if(parent == null)
	    parent = desc.parent.get().layer(Desc.class);
	list.intern(new Glob(parent, list)).add(desc, slot.cast(ClipAmbiance.class));
    }

    public void removed(RenderTree.Slot slot) {
	ActAudio list = slot.state().get(ActAudio.audio);
	if((list == null) || (parent == null))
	    return;
	list.intern(new Glob(parent, list)).remove(desc, slot.cast(ClipAmbiance.class));
    }

    @Resource.LayerName("clamb")
    public static class Desc extends Resource.Layer {
	public final Indir<Resource> parent;
	public final int minc, maxc, maxi;
	public final double bvol;
	public final String[] cnms;
	public final double[] ieps;
	/* XXX: Due to Glob handling, this identity is probably not a
	 * good idea, but if removed, then instancing needs to be
	 * handled some other way. */
	public final ClipAmbiance spr;

	public Desc(Resource res, Message buf) {
	    res.super();
	    int ver = buf.uint8();
	    if((ver < 1) || (ver > 2))
		throw(new Resource.LoadException("Unknown clip-ambiance version: " + ver, getres()));
	    if(ver >= 2) {
		String pnm = buf.string();
		if(pnm.length() == 0)
		    parent = res.indir();
		else
		    parent = res.pool.load(pnm, buf.uint16());
	    } else {
		parent = res.indir();
	    }
	    minc = buf.uint8(); maxc = buf.uint8();
	    maxi = buf.uint16(); bvol = buf.float32();
	    cnms = new String[buf.uint8()];
	    ieps = new double[cnms.length];
	    for(int i = 0; i < cnms.length; i++) {
		cnms[i] = buf.string().intern();
		ieps[i] = 1.0 / buf.float32();
	    }
	    spr = new ClipAmbiance(this);
	}

	public void init() {}
    }
}
