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
import haven.Audio.CS;

public class ActAudio extends GLState.Abstract {
    public static final GLState.Slot<ActAudio> slot = new GLState.Slot<ActAudio>(GLState.Slot.Type.SYS, ActAudio.class);
    private final Collection<CS> clips = new ArrayList<CS>();
    private final Collection<CS> current = new ArrayList<CS>();
    private final Map<Global, Global> global = new HashMap<Global, Global>();

    public void prep(Buffer st) {
	st.put(slot, this);
    }

    public interface Global {
	public boolean cycle(ActAudio list);
    }

    public static class PosClip implements Rendered {
	private final Audio.DataClip clip;
	
	public PosClip(Audio.DataClip clip) {
	    this.clip = clip;
	}
	
	public void draw(GOut g) {
	    g.apply();
	    ActAudio list = g.st.cur(slot);
	    if(list != null) {
		Coord3f pos = PView.mvxf(g).mul4(Coord3f.o);
		double pd = Math.sqrt((pos.x * pos.x) + (pos.y * pos.y));
		this.clip.vol = Math.min(1.0, 50.0 / pd);
		list.add(clip);
	    }
	}

	public boolean setup(RenderList rl) {
	    return(true);
	}
    }

    public static class Ambience implements Rendered {
	public final Resource res;
	public final double bvol;
	private Glob glob = null;

	public Ambience(Resource res, double bvol) {
	    if(res.layer(Resource.audio, "amb") == null) {
		/* This check is mostly just to make sure the resource
		 * is loaded and doesn't throw Loading exception in
		 * the setup routine. */
		throw(new RuntimeException("No ambient clip found in " + res));
	    }
	    this.res = res;
	    this.bvol = bvol;
	}

	public Ambience(Resource res) {
	    this(res, res.layer(Resource.audio, "amb").bvol);
	}

	public static class Glob implements Global {
	    public final Resource res;
	    private final Audio.DataClip clip;
	    private int n;
	    private double vacc;
	    private double lastupd = System.currentTimeMillis() / 1000.0;
	    
	    public Glob(Resource res) {
		this.res = res;
		final Resource.Audio clip = res.layer(Resource.audio, "amb");
		if(clip == null)
		    throw(new RuntimeException("No ambient clip found in " + res));
		this.clip = new Audio.DataClip(new RepeatStream(new RepeatStream.Repeater() {
			public InputStream cons() {
			    return(clip.pcmstream());
			}
		    }), 0.0, 1.0);
	    }

	    public int hashCode() {
		return(res.hashCode());
	    }

	    public boolean equals(Object other) {
		return((other instanceof Glob) && (((Glob)other).res == this.res));
	    }

	    public boolean cycle(ActAudio list) {
		double now = System.currentTimeMillis() / 1000.0;
		double td = Math.max(now - lastupd, 0.0);
		if(vacc < clip.vol)
		    clip.vol = Math.max(clip.vol - (td * 0.5), 0.0);
		else if(vacc > clip.vol)
		    clip.vol = Math.min(clip.vol + (td * 0.5), 1.0);
		if((n == 0) && (clip.vol < 0.005))
		    return(true);
		vacc = 0.0;
		n = 0;
		lastupd = now;
		list.add(clip);
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
		ActAudio list = g.st.cur(slot);
		if(list == null)
		    return;
		glob = list.intern(new Glob(res));
	    }
	    Coord3f pos = PView.mvxf(g).mul4(Coord3f.o);
	    double pd = Math.sqrt((pos.x * pos.x) + (pos.y * pos.y));
	    double svol = Math.min(1.0, 50.0 / pd);
	    glob.add(svol * bvol);
	}

	public boolean setup(RenderList rl) {
	    return(true);
	}
    }

    public void add(CS clip) {
	clips.add(clip);
    }

    @SuppressWarnings("unchecked")
    public <T extends Global> T intern(T glob) {
	T ret = (T)global.get(glob);
	if(ret == null)
	    global.put(glob, ret = glob);
	return(ret);
    }
    
    public void cycle() {
	for(Iterator<Global> i = global.keySet().iterator(); i.hasNext();) {
	    Global glob = i.next();
	    if(glob.cycle(this))
		i.remove();
	}
	for(CS clip : current) {
	    if(!clips.contains(clip))
		Audio.stop(clip);
	}
	for(CS clip : clips) {
	    if(!current.contains(clip))
		Audio.play(clip);
	}
	current.clear();
	current.addAll(clips);
	clips.clear();
    }
    
    public void clear() {
	for(CS clip : current)
	    Audio.stop(clip);
    }
}
