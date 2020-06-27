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

public interface Environment extends haven.Disposable {
    public Render render();
    public FillBuffer fillbuf(DataBuffer target, int from, int to);
    public default FillBuffer fillbuf(DataBuffer target) {return(fillbuf(target, 0, target.size()));}
    public DrawList drawlist();
    public void submit(Render cmd);

    public static class UnavailableException extends RuntimeException {
	public UnavailableException(String msg) {
	    super(msg);
	}

	public UnavailableException(String msg, Throwable cause) {
	    super(msg, cause);
	}
    }

    public static interface Caps {
	public String vendor();
	public String driver();
	public String device();
    }

    public Caps caps();

    public abstract static class Proxy implements Environment {
	public abstract Environment back();

	public Render render() {return(back().render());}
	public FillBuffer fillbuf(DataBuffer target, int from, int to) {return(back().fillbuf(target, from, to));}
	public DrawList drawlist() {return(back().drawlist());}
	public void submit(Render cmd) {back().submit(cmd);}
	public void dispose() {back().dispose();}
	public Caps caps() {return(back().caps());}
    }
}
