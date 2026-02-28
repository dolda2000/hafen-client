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

	public static class Recorder implements Callback {
	    public final Writer out;
	    private final double epoch;

	    public Recorder(Writer out) {
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

    public static class Playback implements Transport {
	public final BufferedReader in;
	private final Collection<Callback> cbs = new ArrayList<>();
	private boolean closed = false;

	public Playback(Reader in) {
	    this.in = (in instanceof BufferedReader) ? (BufferedReader)in : new BufferedReader(in);
	}

	public Playback add(Callback cb) {
	    this.cbs.add(cb);
	    return(this);
	}

	public void close() {}
	public void queuemsg(PMessage  pmsg) {}
	public void send(PMessage msg) {}

	private String[] split(String text) {
	    ArrayList<String> words = new ArrayList<>();
	    StringBuilder buf = new StringBuilder();
	    String st = "ws";
	    int i = 0;
	    while(i < text.length()) {
		char c = text.charAt(i);
		if(st == "ws") {
		    if(!Character.isWhitespace(c))
			st = "word";
		    else
			i++;
		} else if(st == "word") {
		    if(Character.isWhitespace(c)) {
			words.add(buf.toString());
			buf = new StringBuilder();
			st = "ws";
		    } else {
			buf.append(c);
			i++;
		    }
		}
	    }
	    if(st == "word")
		words.add(buf.toString());
	    return(words.toArray(new String[0]));
	}

	private void play() {
	    int lno = 0;
	    double epoch = Utils.rtime();
	    try {
		while(true) {
		    String ln;
		    try {
			lno++;
			if((ln = in.readLine()) == null)
			    break;
		    } catch(IOException e) {
			throw(new RuntimeException(e));
		    }
		    String[] w = split(ln);
		    if((w.length < 1) || (w[0].charAt(0) == '#'))
			continue;
		    double tm = Double.parseDouble(w[0]);
		    double now = Utils.rtime() - epoch;
		    if(tm > now) {
			try {
			    Thread.sleep((long)Math.ceil((tm - now) * 1000));
			} catch(InterruptedException e) {
			    throw(new RuntimeException(e));
			}
		    }
		    switch(w[1]) {
		    case "close":
			for(Callback cb : cbs)
			    cb.closed();
			closed = true;
			break;
		    case "rmsg":
			for(Callback cb : cbs)
			    cb.handle(new PMessage(Integer.parseInt(w[2]), Utils.bprint.dec(w[3])));
			break;
		    case "objd":
			for(Callback cb : cbs) {
			    int fl = 0;
			    if(w[2].indexOf('v') >= 0) fl |= 2;
			    if(w[2].indexOf('o') >= 0) fl |= 4;
			    OCache.ObjDelta delta = new OCache.ObjDelta(fl, Long.parseLong(w[3]), Integer.parseInt(w[4]));
			    int a = 5;
			    if(w[2].indexOf('i') >= 0) delta.initframe = Integer.parseInt(w[a++]);
			    for(; a < w.length; a++) {
				int p = w[a].indexOf(':');
				delta.attrs.add(new OCache.AttrDelta(delta, Integer.parseInt(w[a].substring(0, p)), Utils.bprint.dec(w[a].substring(p + 1))));
			    }
			    cb.handle(delta);
			}
			break;
		    case "map":
			for(Callback cb : cbs)
			    cb.mapdata(new MessageBuf(Utils.b64.dec(w[2])));
			break;
		    default:
			Warning.warn("unexpected recording statement: " + w[1]);
			break;
		    }
		}
	    } catch(RuntimeException e) {
		System.err.println("error occurred on recording line " + lno);
		throw(e);
	    } finally {
		if(!closed) {
		    for(Callback cb : cbs)
			cb.closed();
		}
		try {
		    in.close();
		} catch(IOException e) {
		    throw(new RuntimeException(e));
		}
	    }
	}

	public void start() {
	    new HackThread(this::play, "Protocol playback thread").start();
	}
    }
}
