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

package haven.render.gl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import haven.render.*;

/* Extracted from GLEnvironment.runStreamFill so the proxy/copy semantics
 * can be unit-tested without a live GLEnvironment. */
final class StreamFiller {
    private StreamFiller() {}

    /* Run a Filler against `target` using a proxy Environment that
     * intercepts whole-range fillbuf(target) calls and returns `pre`
     * instead. If the Filler bypasses env.fillbuf and returns its own
     * FillBuffer, copy the bytes into `pre` via `pull`. */
    static <T extends DataBuffer> void runWithPreallocated(
	    Environment back, T target, int size,
	    DataBuffer.Filler<? super T> init,
	    FillBuffer pre, Consumer<ByteBuffer> pull) {
	Environment proxy = new Environment.Proxy() {
		public Environment back() {return(back);}
		public FillBuffer fillbuf(DataBuffer t, int from, int to) {
		    if((t == target) && (from == 0) && (to == size))
			return(pre);
		    return(back.fillbuf(t, from, to));
		}
		public FillBuffer fillbuf(DataBuffer t) {
		    if(t == target)
			return(pre);
		    return(back.fillbuf(t));
		}
	    };
	FillBuffer result = init.fill(target, proxy);
	if(result == pre)
	    return;
	ByteBuffer src = result.push();
	((java.nio.Buffer)src).flip();
	pull.accept(src);
	result.dispose();
    }
}
