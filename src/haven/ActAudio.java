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
		Coord3f pos = g.st.mv.mul4(Coord3f.o);
		double pd = Math.sqrt((pos.x * pos.x) + (pos.y * pos.y));
		this.clip.vol = Math.min(1.0, 50.0 / pd);
		list.add(clip);
	    }
	}

	public boolean setup(RenderList rl) {
	    return(true);
	}
    }

    public void add(CS clip) {
	clips.add(clip);
    }

    public Global intern(Global glob) {
	Global ret = global.get(glob);
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
