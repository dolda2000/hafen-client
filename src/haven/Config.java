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

import java.net.URL;
import java.io.PrintStream;
import static haven.Utils.getprop;

public class Config {
    public static byte[] authck;
    public static String authuser;
    public static String authserv;
    public static String defserv;
    public static URL resurl, mapurl;
    public static boolean fullscreen;
    public static boolean dbtext;
    public static boolean bounddb;
    public static boolean profile;
    public static boolean nolocalres, fscache;
    public static String resdir;
    public static boolean nopreload;
    public static String loadwaited, allused;
    public static Coord wndsz = new Coord(800, 600);
    public static boolean wndlock = true;
    
    static {
	try {
	    String p;
	    if((p = getprop("haven.authck", null)) != null)
		authck = Utils.hex2byte(p);
	    authuser = getprop("haven.authuser", null);
	    authserv = getprop("haven.authserv", null);
	    defserv = getprop("haven.defserv", null);
	    if(!(p = getprop("haven.resurl", "https://www.havenandhearth.com/res/")).equals(""))
		resurl = new URL(p);
	    if(!(p = getprop("haven.mapurl", "http://www.havenandhearth.com/mm/")).equals(""))
		mapurl = new URL(p);
	    fullscreen = getprop("haven.fullscreen", "off").equals("on");
	    if((p = getprop("haven.wndsz", null)) != null) {
		int x = p.indexOf("x");
		if(x >= 0)
		    wndsz = new Coord(Integer.parseInt(p.substring(0, x)), Integer.parseInt(p.substring(x + 1)));
	    }
	    wndlock = getprop("haven.wndlock", "on").equals("on");
	    loadwaited = getprop("haven.loadwaited", null);
	    allused = getprop("haven.allused", null);
	    dbtext = getprop("haven.dbtext", "off").equals("on");
	    bounddb = getprop("haven.bounddb", "off").equals("on");
	    profile = getprop("haven.profile", "off").equals("on");
	    nolocalres = getprop("haven.nolocalres", "").equals("yesimsure");
	    fscache = getprop("haven.fscache", "on").equals("on");
	    resdir = getprop("haven.resdir", null);
	    nopreload = getprop("haven.nopreload", "no").equals("yes");
	} catch(java.net.MalformedURLException e) {
	    throw(new RuntimeException(e));
	}
    }
    
    private static void usage(PrintStream out) {
	out.println("usage: haven.jar [-hdfPL] [-s WxH] [-u USER] [-C HEXCOOKIE] [-r RESDIR] [-U RESURL] [-A AUTHSERV] [SERVER]");
    }

    public static void cmdline(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "hdPU:fr:A:u:C:Ls:");
	if(opt == null) {
	    usage(System.err);
	    System.exit(1);
	}
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage(System.out);
		System.exit(0);
		break;
	    case 'd':
		dbtext = true;
		break;
	    case 'P':
		profile = true;
		break;
	    case 'f':
		fullscreen = true;
		break;
	    case 'r':
		resdir = opt.arg;
		break;
	    case 'A':
		authserv = opt.arg;
		break;
	    case 'U':
		try {
		    resurl = new URL(opt.arg);
		} catch(java.net.MalformedURLException e) {
		    System.err.println(e);
		    System.exit(1);
		}
		break;
	    case 'u':
		authuser = opt.arg;
		break;
	    case 'C':
		authck = Utils.hex2byte(opt.arg);
		break;
	    case 's':
		{
		    int x = opt.arg.indexOf("x");
		    if(x >= 0)
			wndsz = new Coord(Integer.parseInt(opt.arg.substring(0, x)), Integer.parseInt(opt.arg.substring(x + 1)));
		}
		break;
	    case 'L':
		wndlock = false;
		break;
	    }
	}
	if(opt.rest.length > 0)
	    defserv = opt.rest[0];
    }
}
