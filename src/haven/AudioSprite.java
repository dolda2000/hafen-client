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

public class AudioSprite {
    public static final Sprite.Factory fact = new Sprite.Factory() {
	    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
		List<Resource.Audio> cl = new ArrayList<Resource.Audio>();
		for(Resource.Audio clip : res.layers(Resource.audio)) {
		    if(clip.id == "cl")
			cl.add(clip);
		}
		if(!cl.isEmpty()) {
		    Resource.Audio clip = cl.get((int)(Math.random() * cl.size()));
		    return(new ClipSprite(owner, res, clip));
		}
		Resource.Audio clip = res.layer(Resource.audio, "amb");
		if(clip != null)
		    return(new Ambience(owner, res));
		return(null);
	    }
	};

    public static class ClipSprite extends Sprite {
	public final ActAudio.PosClip clip;
	private boolean done = false;

	public ClipSprite(Owner owner, Resource res, Resource.Audio clip) {
	    super(owner, res);
	    this.clip = new ActAudio.PosClip(new Audio.DataClip(clip.pcmstream()) {
		    protected void eof() {
			super.eof();
			done = true;
		    }
		});
	}

	public boolean setup(RenderList r) {
	    r.add(clip, null);
	    return(false);
	}

	public boolean tick(int dt) {
	    return(done);
	}
    }

    public static class Ambience extends Sprite {
	public final ActAudio.Ambience amb;

	public Ambience(Owner owner, Resource res) {
	    super(owner, res);
	    this.amb = new ActAudio.Ambience(res);
	}

	public boolean setup(RenderList r) {
	    r.add(amb, null);
	    return(false);
	}
    }
}
