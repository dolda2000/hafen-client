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

public class Astronomy {
    private static final double DAY = 86400; // 1 day in seconds
    private static final double LUNAR_MONTH = 259200; // 30 days in seconds
    private static final double YEAR = 31536000; // 365 days in seconds

    double dt, mp, yt;
    boolean night;
    int hh,mm,day;
    public static final String[] phase ={"New Moon",
            "Waxing Crescent",
            "First Quarter",
            "Waxing Gibbous",
            "Full Moon",
            "Waning Gibbous",
            "Last Quarter",
            "Waning Crescent"};

    public Astronomy(double dt, double mp, double yt) {
        this(dt, mp, yt, (dt < 0.25) || (dt > 0.75));
    }

    public Astronomy(double dt, double mp, double yt, boolean night) {
        this.dt = dt;
        this.mp = mp;
        this.yt = yt;
        this.night = night;
        this.hh = (int)(24*dt);
        this.mm = (int)(60*(24*dt - hh));
        this.day = (int)(365*yt);
        this.night = night;
    }

    public static Astronomy fromGlobalTime(long time) {
        long secs = time / 1000;
        return new Astronomy(frac(secs / DAY), frac(secs / LUNAR_MONTH), frac(secs / YEAR));
    }

    private static double frac(double x) {
        return x - (int)x;
    }
}
