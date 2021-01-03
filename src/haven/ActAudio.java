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
import java.io.*;
import java.lang.ref.WeakReference;
import haven.render.*;
import haven.Audio.CS;
import haven.Audio.VolAdjust;

public class ActAudio extends State {
    public static final Slot<ActAudio> audio = new State.Slot<>(Slot.Type.SYS, ActAudio.class);
    public final Channel pos;
    public final Channel amb;
    private final Map<Global, Global> global = new HashMap<Global, Global>();

    public haven.render.sl.ShaderMacro shader() {return(null);}

    public ActAudio(Root root) {
	this.pos = new Adapter(root.pos);
	this.amb = new Adapter(root.amb);
    }

    public void apply(Pipe st) {
	st.put(audio, this);
    }

    public static interface Channel {
	public void add(CS clip);
	public void remove(CS clip);
	public void clear();
    }

    public static class Adapter implements Channel {
	public final Channel parent;
	private Collection<CS> clips = new HashSet<CS>();

	public Adapter(Channel parent) {
	    this.parent = parent;
	}

	public void add(CS clip) {
	    synchronized(this) {
		if(clips != null) {
		    clips.add(clip);
		    parent.add(clip);
		}
	    }
	}

	public void remove(CS clip) {
	    synchronized(this) {
		if(clips != null) {
		    clips.remove(clip);
		    parent.remove(clip);
		}
	    }
	}

	public void clear() {
	    synchronized(this) {
		Collection<CS> clips = this.clips;
		this.clips = null;
		for(Iterator<CS> i = clips.iterator(); i.hasNext();) {
		    parent.remove(i.next());
		    i.remove();
		}
	    }
	}
    }

    public static class RootChannel implements Channel {
	public final String name;
	public double volume;
	private Audio.VolAdjust volc = null;
	private Audio.Mixer mixer = null;

	private RootChannel(String name) {
	    this.name = name;
	    this.volume = Double.parseDouble(Utils.getpref("sfxvol-" + name, "1.0"));
	}

	public Audio.Mixer mixer() {
	    Audio.Mixer ret = this.mixer;
	    if(ret == null) {
		synchronized(this) {
		    if((ret = this.mixer) == null) {
			this.volc = new Audio.VolAdjust(ret = this.mixer = new Audio.Mixer(true));
			this.volc.vol = volume;
			Audio.play(this.volc);
		    }
		}
	    }
	    return(ret);
	}

	public void setvolume(double volume) {
	    if(volc != null)
		volc.vol = volume;
	    this.volume = volume;
	    Utils.setpref("sfxvol-" + name, Double.toString(volume));
	}

	public void clear() {
	    synchronized(this) {
		if(mixer != null) {
		    Audio.stop(volc);
		    /* XXX? clear() should only be called once, so
		     * ensure mixer isn't later mistakenly re-added
		     * due to racy code still using this channel.
		    mixer = null;
		    volc = null;
		    */
		}
	    }
	}

	public void add(CS clip) {
	    synchronized(this) {
		mixer().add(clip);
	    }
	}

	public void remove(CS clip) {
	    synchronized(this) {
		if(mixer != null)
		    mixer.stop(clip);
	    }
	}
    }

    public static class Root {
	public final RootChannel pos = new RootChannel("pos");
	public final RootChannel amb = new RootChannel("amb");

	public void clear() {
	    pos.clear();
	    amb.clear();
	}
    }

    public interface Global {
	public boolean cycle(ActAudio list);
    }

    public static Coord3f spos(Pipe st) {
	Coord3f pos = Coord3f.o;
	Location.Chain loc = st.get(Homo3D.loc);
	if(loc != null)
	    pos = loc.fin(Matrix4f.id).mul4(pos);
	Camera cam = st.get(Homo3D.cam);
	if(cam != null)
	    pos = cam.fin(Matrix4f.id).mul4(pos);
	return(pos);
    }

    public static class PosClip implements RenderTree.Node, TickList.TickNode, TickList.Ticking {
	private final VolAdjust clip;
	private final Collection<RenderTree.Slot> slots = new ArrayList<>(1);

	public PosClip(VolAdjust clip) {
	    this.clip = clip;
	}

	public PosClip(CS clip) {
	    this(new VolAdjust(clip));
	}

	public void added(RenderTree.Slot slot) {
	    ActAudio list = slot.state().get(audio);
	    if(list == null)
		return;
	    slots.add(slot);
	    autotick(0);
	    list.pos.add(clip);
	}

	public void removed(RenderTree.Slot slot) {
	    ActAudio list = slot.state().get(audio);
	    if(list == null)
		return;
	    slots.remove(slot);
	    list.pos.remove(clip);
	}

	public TickList.Ticking ticker() {return(this);}
	public void autotick(double dt) {
	    for(RenderTree.Slot slot : slots) {
		Coord3f pos = spos(slot.state());
		this.clip.vol = Math.min(1.0, 50.0 / Math.hypot(pos.x, pos.y));
		this.clip.bal = Utils.clip(Math.atan2(pos.x, -pos.z) / (Math.PI / 8.0), -1, 1);
		break;
	    }
	}
    }

    public static class Ambience implements RenderTree.Node {
	public final Resource res;
	public final double bvol;

	public Ambience(Resource res, double bvol) {
	    if(res.layer(Resource.audio, "amb") == null)
		throw(new RuntimeException("No ambient clip found in " + res));
	    this.res = res;
	    this.bvol = bvol;
	}

	public Ambience(Resource res) {
	    this(res, res.layer(Resource.audio, "amb").bvol);
	}

	public static class Glob implements Global {
	    public final Resource res;
	    private final VolAdjust clip;
	    private final Collection<RenderList.Slot<Ambience>> active = new ArrayList<>();
	    private double lastupd = Utils.rtime();
	    private boolean added = false, hasvol = false;
	    
	    public Glob(Resource res) {
		this.res = res;
		final Resource.Audio clip = res.layer(Resource.audio, "amb");
		if(clip == null)
		    throw(new RuntimeException("No ambient clip found in " + res));
		this.clip = new VolAdjust(new Audio.Repeater() {
			public CS cons() {
			    return(clip.stream());
			}
		    });
	    }

	    public int hashCode() {
		return(res.hashCode());
	    }

	    public boolean equals(Object other) {
		return((other instanceof Glob) && (((Glob)other).res == this.res));
	    }

	    private double curvol() {
		double acc = 0;
		for(RenderList.Slot<Ambience> slot : active) {
		    Coord3f pos = spos(slot.state());
		    double bvol = slot.obj().bvol;
		    double svol = Math.min(1.0, 50.0 / Math.hypot(pos.x, pos.y));
		    acc += svol * bvol;
		}
		return(acc);
	    }

	    public boolean cycle(ActAudio list) {
		double now = Utils.rtime();
		double td = Math.max(now - lastupd, 0.0);
		synchronized(active) {
		    double vacc = curvol();
		    if(hasvol) {
			if(vacc < clip.vol)
			    clip.vol = Math.max(clip.vol - (td * 0.5), 0.0);
			else if(vacc > clip.vol)
			    clip.vol = Math.min(clip.vol + (td * 0.5), 1.0);
		    } else {
			clip.vol = vacc;
			hasvol = true;
		    }
		    if(active.isEmpty() && (clip.vol < 0.005)) {
			list.amb.remove(clip);
			return(true);
		    }
		}
		lastupd = now;
		if(!added) {
		    list.amb.add(clip);
		    added = true;
		}
		return(false);
	    }

	    public void add(RenderList.Slot<Ambience> slot) {
		synchronized(active) {
		    active.add(slot);
		}
	    }

	    public void remove(RenderList.Slot<Ambience> slot) {
		synchronized(active) {
		    active.remove(slot);
		}
	    }
	}

	public void added(RenderTree.Slot slot) {
	    ActAudio list = slot.state().get(audio);
	    if(list == null)
		return;
	    list.intern(new Glob(res)).add(slot.cast(Ambience.class));
	}

	public void removed(RenderTree.Slot slot) {
	    ActAudio list = slot.state().get(audio);
	    if(list == null)
		return;
	    list.intern(new Glob(res)).remove(slot.cast(Ambience.class));
	}
    }

    @SuppressWarnings("unchecked")
    public <T extends Global> T intern(T glob) {
	synchronized(global) {
	    T ret = (T)global.get(glob);
	    if(ret == null)
		global.put(glob, ret = glob);
	    return(ret);
	}
    }

    public void cycle() {
	synchronized(global) {
	    for(Iterator<Global> i = global.keySet().iterator(); i.hasNext();) {
		Global glob = i.next();
		if(glob.cycle(this))
		    i.remove();
	    }
	}
    }
    
    public void clear() {
	pos.clear();
	amb.clear();
    }
}
