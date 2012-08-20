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
import haven.Audio.CS;

public class ActAudio {
    private final Collection<CS> clips = new ArrayList<CS>();
    private final Collection<CS> current = new ArrayList<CS>();
    private final Map<Global, Global> global = new HashMap<Global, Global>();

    public interface Global {
	public void cycle();
	public void add(GLState.Buffer state);
	public CS clip();
    }

    public interface RenderedAudio extends Rendered {
	public CS clip();
    }

    public class PosAudio implements RenderedAudio {
	private final Audio.DataClip clip;
	private final Matrix4f mv = new Matrix4f();
	
	public PosAudio(Audio.DataClip clip) {
	    this.clip = clip;
	}
	
	public void draw(GOut g) {}
	public boolean setup(RenderList rl) {
	    GLState.Buffer st = rl.state();
	    mv.load(st.get(PView.cam).fin(Matrix4f.id)).mul1(st.get(PView.loc).fin(Matrix4f.id));
	    Coord3f pos = mv.mul4(Coord3f.o);
	    double pd = Math.sqrt((pos.x * pos.x) + (pos.y * pos.y));
	    this.clip.vol = Math.min(1.0, 50.0 / pd);
	    return(false);
	}
	
	public CS clip() {
	    return(this.clip);
	}
    }

    public void add(CS clip) {
	clips.add(clip);
    }

    public Global intern(Global glob) {
	Global ret = global.get(glob);
	if(ret == null)
	    global.put(ret = glob, glob);
	return(ret);
    }
    
    public void add(RenderList list) {
	for(RenderList.Slot slot : list.slots()) {
	    if(slot.r instanceof RenderedAudio)
		add(((RenderedAudio)slot.r).clip());
	    else if(slot.r instanceof Global)
		((Global)slot.r).add(slot.os);
	}
    }
    
    public void cycle() {
	for(Global glob : global.keySet())
	    glob.cycle();
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
