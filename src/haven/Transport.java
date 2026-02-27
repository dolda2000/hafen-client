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
import java.nio.*;

public interface Transport {
    public void close();
    public void queuemsg(PMessage pmsg);
    public void send(PMessage msg);
    public Transport add(Callback cb);

    public static interface Callback {
	public default void closed() {};
	public default void handle(PMessage msg) {};
	public default void handle(OCache.ObjDelta delta) {};
	public default void mapdata(Message msg) {};

	public static class Dumper implements Callback {
	    public final Writer out;
	    private final double epoch;

	    public Dumper(Writer out) {
		this.out = out;
		this.epoch = Utils.rtime();
	    }

	    private void printf(String format, Object... args) {
		try {
		    out.write(String.format(format, args));
		} catch(IOException e) {
		    throw(new RuntimeException(e));
		}
	    }

	    public void closed() {
		printf("%4.6f close\n", Utils.rtime() - epoch);
		try {
		    out.close();
		} catch(IOException e) {
		    throw(new RuntimeException(e));
		}
	    }

	    public void handle(PMessage msg) {
		printf("%4.6f rmsg %d %s\n", Utils.rtime() - epoch, msg.type, Utils.bprint.enc(msg.bytes()));
	    }

	    public void handle(OCache.ObjDelta msg) {
		printf("%4.6f objd", Utils.rtime() - epoch);
		String fl = "";
		if(msg.initframe > 0) fl += "i";
		if((msg.fl & 2) != 0) fl += "v";
		if((msg.fl & 4) != 0) fl += "o";
		if(msg.rem) fl += "d";
		printf(" %s %d %d", (fl == "") ? "n" : fl, msg.id, msg.frame);
		if(msg.initframe > 0) printf(" %d", msg.initframe);
		for(OCache.AttrDelta attr : msg.attrs)
		    printf(" %d:%s", attr.type, Utils.bprint.enc(attr.bytes()));
		printf("\n");
	    }

	    public void mapdata(Message msg) {
		printf("%4.6f map %s\n", Utils.rtime() - epoch, Utils.b64.enc(msg.bytes()));
	    }
	}
    }
}
