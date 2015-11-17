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
import java.util.WeakHashMap;

import static haven.Utils.*;

public class Config {
    private static final PrefProvider<Boolean> TYPE_BOOL = new PrefProvider<Boolean>() {
        public Boolean get(String prefName, Boolean defaultValue) { return getprefb(prefName, defaultValue); }
        public void set(String prefName, Boolean value) { setprefb(prefName, value); }
    };
    private static final PrefProvider<Integer> TYPE_INT = new PrefProvider<Integer>() {
        public Integer get(String prefName, Integer defaultValue) { return getprefi(prefName, defaultValue); }
        public void set(String prefName, Integer value) { setprefi(prefName, value); }
    };
    private static final PrefProvider<Coord> TYPE_COORD = new PrefProvider<Coord>() {
        public Coord get(String prefName, Coord defaultValue) { return getprefc(prefName, defaultValue); }
        public void set(String prefName, Coord value) { setprefc(prefName, value); }
    };
    private static final PrefProvider<String[]> TYPE_STRING_ARRAY = new PrefProvider<String[]>() {
        private static final String SEPARATOR = ";";

        public String[] get(String prefName, String[] defaultValue) {
            return getpref(prefName, "").split(SEPARATOR); }

        public void set(String prefName, String[] values) {
            setpref(prefName, Utils.join(SEPARATOR, values)); }
    };

    public static String authuser = getprop("haven.authuser", null);
    public static String authserv = getprop("haven.authserv", null);
    public static String defserv = getprop("haven.defserv", "127.0.0.1");
    public static URL resurl = geturl("haven.resurl", "");
    public static URL mapurl = geturl("haven.mapurl", "");
    public static boolean dbtext = getprop("haven.dbtext", "off").equals("on");
    public static boolean bounddb = getprop("haven.bounddb", "off").equals("on");
    public static boolean profile = getprop("haven.profile", "off").equals("on");
    public static boolean profilegpu = getprop("haven.profilegpu", "off").equals("on");
    public static boolean fscache = getprop("haven.fscache", "on").equals("on");
    public static String resdir = getprop("haven.resdir", null);
    public static boolean nopreload = getprop("haven.nopreload", "no").equals("yes");
    public static String loadwaited = getprop("haven.loadwaited", null);
    public static String allused = getprop("haven.allused", null);
    public static int mainport = getint("haven.mainport", 1870);
    public static int authport = getint("haven.authport", 1871);
    public static boolean softres = getprop("haven.softres", "on").equals("on");
    public static byte[] authck = null;
    public static String prefspec = "hafen";

    public static Pref<Coord> minimapPosition = new Pref<Coord>("haven.minimap.position", Coord.z, TYPE_COORD);
    public static Pref<Coord> minimapSize = new Pref<Coord>("haven.minimap.size", new Coord(300, 300), TYPE_COORD);
    public static Pref<Boolean> minimapShowRadius = new Pref<Boolean>("haven.minimap.radius", false, TYPE_BOOL);
    public static Pref<Boolean> minimapShowGrid = new Pref<Boolean>("haven.minimap.grid", false, TYPE_BOOL);
    public static Pref<Boolean> minimapEnableSave = new Pref<Boolean>("haven.minimap.save", true, TYPE_BOOL);
    public static Pref<Boolean> nightvision = new Pref<Boolean>("haven.nightvision", false, TYPE_BOOL);
    public static Pref<Boolean> snapCamera = new Pref<Boolean>("haven.display.snapcamera", false, TYPE_BOOL);
    public static Pref<Boolean> showKinNotifications = new Pref<Boolean>("haven.notifications.kinstatus", true, TYPE_BOOL);
    public static Pref<Coord> studyPosition = new Pref<Coord>("haven.study.position", Coord.z, TYPE_COORD);
    public static Pref<Boolean> studyVisible = new Pref<Boolean>("haven.study.visible", false, TYPE_BOOL);
    public static Pref<Boolean> showHungerMeter = new Pref<Boolean>("haven.meters.hunger", true, TYPE_BOOL);
    public static Pref<Boolean> showFepMeter = new Pref<Boolean>("haven.meters.fep", true, TYPE_BOOL);
    public static Pref<Boolean> showCustomIcons = new Pref<Boolean>("haven.minimap.customicons", true, TYPE_BOOL);
    public static Pref<Boolean> showClock = new Pref<Boolean>("haven.display.clock", true, TYPE_BOOL);
    public static Pref<Boolean> showServerGrid = new Pref<Boolean>("haven.display.servergrid", false, TYPE_BOOL);
    public static Pref<Boolean> showCustomDefenseBars = new Pref<Boolean>("haven.combat.defbar.visible", true, TYPE_BOOL);
    public static Pref<Integer> cupboardScale = new Pref<Integer>("haven.display.cupboardscale", 10, TYPE_INT);
    public static Pref<Boolean> enableAutoHearth = new Pref<Boolean>("haven.autohearth", false, TYPE_BOOL);
    public static Pref<Boolean> enableStrangerAlarm = new Pref<Boolean>("haven.alarm.strangers", true, TYPE_BOOL);
    public static Pref<Boolean> enablePonyAlarm = new Pref<Boolean>("haven.alarm.pony", true, TYPE_BOOL);
    public static Pref<Integer> alarmVolume = new Pref<Integer>("haven.alarm.volume", 500, TYPE_INT);
    public static Pref<Boolean> simplifyCrops = new Pref<Boolean>("simplecrops", false, TYPE_BOOL);
    public static Pref<Boolean> showFlavor = new Pref<Boolean>("showflo", true, TYPE_BOOL);
    public static Pref<Boolean> showQuality = new Pref<Boolean>("haven.display.quality", false, TYPE_BOOL);
    public static Pref<Integer> showQualityMode = new Pref<Integer>("haven.display.quality.mode", 0, TYPE_INT);
    public static Pref<Boolean> showQualityBackground = new Pref<Boolean>("haven.display.quality.back", true, TYPE_BOOL);
    public static Pref<Boolean> enableAutoStudy = new Pref<Boolean>("haven.autostudy", false, TYPE_BOOL);
    public static Pref<Boolean> useCupboardsForAutoStudy = new Pref<Boolean>("haven.autostudy.cupboards", true, TYPE_BOOL);
    public static Pref<Boolean> lockStudy = new Pref<Boolean>("studylock", false, TYPE_BOOL);
    public static Pref<Boolean> toggleTracking = new Pref<Boolean>("toggle.tracking", true, TYPE_BOOL);
    public static Pref<Boolean> highlightParty = new Pref<Boolean>("highlight.party", true, TYPE_BOOL);
    public static Pref<Boolean> enableMenuAnimation = new Pref<Boolean>("menu.animation", false, TYPE_BOOL);
    public static Pref<Boolean> enableCustomFlowerMenu = new Pref<Boolean>("menu.custom", false, TYPE_BOOL);
    public static Pref<Boolean> enableAccountStoring = new Pref<Boolean>("account.storing", false, TYPE_BOOL);
    public static Pref<Boolean> showGobInfo = new Pref<Boolean>("display.gobinfo", false, TYPE_BOOL);
    public static Pref<Boolean> showGobInfoForGrownTrees = new Pref<Boolean>("display.gobinfo.growntrees", false, TYPE_BOOL);
    public static Pref<Boolean> useControlForSortTransfer = new Pref<Boolean>("useControlForSortTransfer", false, TYPE_BOOL);
    public static Pref<Boolean> showCustomFKeysBelt = new Pref<Boolean>("display.fbelt", true, TYPE_BOOL);
    public static Pref<Boolean> disableTileTransitions = new Pref<Boolean>("tile.transitions.disabled", false, TYPE_BOOL);
    public static Pref<Boolean> showChatTimestamps = new Pref<Boolean>("chat.timestamp", true, TYPE_BOOL);
    public static Pref<Boolean> showHourglassPercentage = new Pref<Boolean>("hourglass.percentage", false, TYPE_BOOL);
    public static Pref<Boolean> showAimPercentage = new Pref<Boolean>("aim.percentage", false, TYPE_BOOL);
    public static Pref<Integer> nightvisionBrightness = new Pref<Integer>("haven.nightvision.brightness", 220, TYPE_INT);
    public static Pref<Boolean> disableWeatherEffects = new Pref<Boolean>("weather.disabled", false, TYPE_BOOL);
    public static Pref<Boolean> hideKinInfoForNonPlayers = new Pref<Boolean>("hide.nonplayer.kininfo", true, TYPE_BOOL);
    public static Pref<Integer> autopickRadius = new Pref<Integer>("autopick.radius", 3, TYPE_INT);
    public static Pref<Boolean> alwaysShowExtendedTooltips = new Pref<Boolean>("tooltips.extended", false, TYPE_BOOL);
    public static Pref<Boolean> hideModeEnabled = new Pref<Boolean>("hide.mode", false, TYPE_BOOL);
    public static Pref<Boolean> showGobPaths = new Pref<Boolean>("display.gob.paths", false, TYPE_BOOL);
    public static Pref<Boolean> displayMiniTowers = new Pref<Boolean>("display.minitowers", false, TYPE_BOOL);
    public static boolean screenshotMode;
    public static boolean enableAutoloader;

    static {
	String p;
	if((p = getprop("haven.authck", null)) != null)
	    authck = Utils.hex2byte(p);
    }

    public static Pref<String[]> getDeckNames(String accname, String charname) {
        String prefName = String.format("decknames@%s@%s", accname, charname);
        return new Pref<String[]>(prefName, new String[0], TYPE_STRING_ARRAY);
    }

	private static int getint(String name, int def) {
	String val = getprop(name, null);
	if(val == null)
	    return(def);
	return(Integer.parseInt(val));
    }

    private static URL geturl(String name, String def) {
	String val = getprop(name, def);
	if(val.equals(""))
	    return(null);
	try {
	    return(new URL(val));
	} catch(java.net.MalformedURLException e) {
	    throw(new RuntimeException(e));
	}
    }

    private static void usage(PrintStream out) {
	out.println("usage: haven.jar [OPTIONS] [SERVER[:PORT]]");
	out.println("Options include:");
	out.println("  -h                 Display this help");
	out.println("  -d                 Display debug text");
	out.println("  -P                 Enable profiling");
	out.println("  -G                 Enable GPU profiling");
	out.println("  -U URL             Use specified external resource URL");
	out.println("  -r DIR             Use specified resource directory (or HAVEN_RESDIR)");
	out.println("  -A AUTHSERV[:PORT] Use specified authentication server");
	out.println("  -u USER            Authenticate as USER (together with -C)");
	out.println("  -C HEXCOOKIE       Authenticate with specified hex-encoded cookie");
    }

    public static void cmdline(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "hdPGU:r:A:u:C:");
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
	    case 'G':
		profilegpu = true;
		break;
	    case 'r':
		resdir = opt.arg;
		break;
	    case 'A':
		int p = opt.arg.indexOf(':');
		if(p >= 0) {
		    authserv = opt.arg.substring(0, p);
		    authport = Integer.parseInt(opt.arg.substring(p + 1));
		} else {
		    authserv = opt.arg;
		}
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
	    }
	}
	if(opt.rest.length > 0) {
	    int p = opt.rest[0].indexOf(':');
	    if(p >= 0) {
		defserv = opt.rest[0].substring(0, p);
		mainport = Integer.parseInt(opt.rest[0].substring(p + 1));
	    } else {
		defserv = opt.rest[0];
	    }
	}
    }

    static {
	Console.setscmd("stats", new Console.Command() {
		public void run(Console cons, String[] args) {
		    dbtext = Utils.parsebool(args[1]);
		}
	    });
	Console.setscmd("profile", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args[1].equals("none") || args[1].equals("off")) {
			profile = profilegpu = false;
		    } else if(args[1].equals("cpu")) {
			profile = true;
		    } else if(args[1].equals("gpu")) {
			profilegpu = true;
		    } else if(args[1].equals("all")) {
			profile = profilegpu = true;
		    }
		}
	    });
    }

    public static class Pref<T> {
        private final PrefProvider<T> provider;
        private final String name;
        private final WeakHashMap<PrefListener<T>, Object> listeners = new WeakHashMap<PrefListener<T>, Object>();
        private T value;

        public Pref(String name, T defaultValue, PrefProvider<T> provider) {
            this.name = name;
            this.provider = provider;
            this.value = provider.get(name, defaultValue);
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            if (value == this.value)
                return;
            this.value = value;
            provider.set(name, value);
            for (PrefListener<T> listener : listeners.keySet())
                listener.changed(value);
        }

        public void addListener(PrefListener<T> listener) {
            listeners.put(listener, null);
        }

        public void removeListener(PrefListener<T> listener) {
            listeners.remove(listener);
        }
    }

    public interface PrefListener<T> {
        void changed(T value);
    }

    private interface PrefProvider<T> {
        T get(String prefName, T defaultValue);
        void set(String prefName, T value);
    }
}
