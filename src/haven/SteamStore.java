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

import java.io.*;
import java.net.*;

public class SteamStore {
    public static final Config.Variable<URI> steamsvc = Config.Services.var("steamsvc", "");
    private static Authorizer cb;

    private static void sendauth(long orderid, boolean approved) {
	if(approved) {
	    URI uri = Utils.uriparam(steamsvc.get().resolve("txnfin"), "order", Long.toUnsignedString(orderid, 16));
	    try {
		/* XXX? Calling browse() immediately gets the browser
		 * window closed along with the confirmation dialog. */
		Thread.sleep(1000);
	    } catch(InterruptedException e) {
		Thread.currentThread().interrupt();
	    }
	    Steam.get().browse(uri, true);
	}
    }

    private static class Authorizer implements Steam.Listener {
	public void callback(String id, Object[] args) {
	    if(id == "onMicroTxnAuthorization") {
		long orderid = (Long)args[1];
		boolean approved = (Boolean)args[2];
		Thread th = new HackThread(() -> sendauth(orderid, approved), "Store authorizer");
		th.setDaemon(true);
		th.start();
	    }
	}
    }

    public static void launch(Session sess) {
	Steam api = Steam.get();
	long uid = (api.userid() & 0xffffffffL) | 0x01_1_00001_00000000L;
	byte[] sig = Digest.hash(Digest.HMAC.of(Digest.SHA256, sess.sesskey), "steam-store".getBytes());
	URI uri = Utils.uriparam(steamsvc.get().resolve("tostore"), "uid", Long.toUnsignedString(uid), "sig", Utils.base64enc(sig));
	api.browse(uri, false);
	synchronized(SteamStore.class) {
	    if(cb == null)
		api.add(cb = new Authorizer());
	}
    }
}
