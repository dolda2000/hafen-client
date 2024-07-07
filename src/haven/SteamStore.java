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
    public static final Config.Variable<URI> steamsvc = Config.Variable.propu("haven.steamsvc", "");

    public static void launch(Session sess) {
	Steam api = Steam.get();
	long uid = (api.userid() & 0xffffffffL) | 0x01_1_00001_00000000L;
	byte[] sig = Digest.hash(Digest.HMAC.of(Digest.SHA256, sess.sesskey), "steam-store".getBytes());
	URI uri = Utils.uriparam(steamsvc.get().resolve("tostore"), "uid", Long.toUnsignedString(uid), "sig", Utils.base64enc(sig));
	api.browse(uri, true);
    }
}
