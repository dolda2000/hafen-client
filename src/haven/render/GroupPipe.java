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

public interface GroupPipe extends Pipe {
    public Pipe group(int g);
    public int gstate(int id);
    public int nstates();

    public default <T extends State> T get(State.Slot<T> slot) {
	int grp = gstate(slot.id);
	if(grp < 0)
	    return(null);
	return(group(grp).get(slot));
    }

    public default Pipe copy() {
	return(new BufPipe(states()));
    }

    public default State[] states() {
	State[] ret = new State[nstates()];
	for(int i = 0; i < ret.length; i++) {
	    int grp = gstate(i);
	    if(grp < 0)
		ret[i] = null;
	    else
		ret[i] = group(grp).get(State.Slot.slots.idlist[i]);
	}
	return(ret);
    }
}
