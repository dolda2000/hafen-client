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

public class RMessage extends PMessage {
    public static final int RMSG_NEWWDG = 0;
    public static final int RMSG_WDGMSG = 1;
    public static final int RMSG_DSTWDG = 2;
    public static final int RMSG_MAPIV = 3;
    public static final int RMSG_GLOBLOB = 4;
    /* public static final int RMSG_PAGINAE = 5; -- Deprecated */
    public static final int RMSG_RESID = 6;
    public static final int RMSG_PARTY = 7;
    public static final int RMSG_SFX = 8;
    public static final int RMSG_CATTR = 9;
    public static final int RMSG_MUSIC = 10;
    public static final int RMSG_TILES = 11;
    /* public static final int RMSG_BUFF = 12; -- Deprecated */
    public static final int RMSG_SESSKEY = 13;
    public static final int RMSG_FRAGMENT = 14;
    public static final int RMSG_ADDWDG = 15;
    public static final int RMSG_WDGBAR = 16;

    public long last = 0;
    public int retx = 0;
    public int seq;

    public RMessage(int type, byte[] blob, int off, int len) {
	super(type, blob, off, len);
    }
    public RMessage(int type, byte[] blob) {
	super(type, blob);
    }
    public RMessage(int type) {
	super(type);
    }
    public RMessage(PMessage msg) {
	super(msg);
    }
}
