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

import java.net.*;

public class SessWidget extends AWidget {
    private final Defer.Future<Result> conn;
    private boolean rep = false;

    @RName("sess")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    String host = (String)args[0];
	    int port = Utils.iv(args[1]);
	    byte[] cookie = Utils.hex2byte((String)args[2]);
	    Object[] sargs = Utils.splice(args, 3);
	    return(new SessWidget(host, port, cookie, sargs));
	}
    }

    static class Result {
	final Session sess;
	final Connection.SessionError error;

	Result(Session sess, Connection.SessionError error) {
	    this.sess = sess;
	    this.error = error;
	}
    }

    public SessWidget(final String addr, final int port, final byte[] cookie, final Object... args) {
	conn = Defer.later(new Defer.Callable<Result>() {
		public Result call() throws InterruptedException {
		    InetAddress host;
		    try {
			host = InetAddress.getByName(addr);
		    } catch(UnknownHostException e) {
			return(new Result(null, new Connection.SessionConnError()));
		    }
		    try {
			return(new Result(new Session(new InetSocketAddress(host, port), ui.sess.username, cookie, args), null));
		    } catch(Connection.SessionError err) {
			return(new Result(null, err));
		    }
		}
	    });
    }

    public void tick(double dt) {
	super.tick(dt);
	if(!rep && conn.done()) {
	    Result r = conn.get();
	    wdgmsg("res", (r.error == null) ? 0 : r.error.code);
	    rep = true;
	}
    }

    public void uimsg(String name, Object... args) {
	if(name == "exec") {
	    ((RemoteUI)ui.rcvr).ret(conn.get().sess);
	} else {
	    super.uimsg(name, args);
	}
    }

    public void destroy() {
	super.destroy();
	/* XXX: There's a race condition here, but I admit I'm not
	 * sure what can properly be done about it, and it ought at
	 * least be uncommon. */
	if(conn.done()) {
	    Session sess = conn.get().sess;
	    if(sess != null)
		sess.close();
	} else {
	    conn.cancel();
	}
    }
}
