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
import java.util.prefs.*;

public class MapPrefs extends AbstractPreferences {
    public static final MapPrefs ROOT = new MapPrefs(null, "", Collections.emptyMap());
    private final Map<? super String, ? super String> props;

    public MapPrefs(MapPrefs parent, String name, Map<? super String, ? super String> props) {
	super(parent, name);
	this.props = props;
    }

    public MapPrefs(String name, Map<? super String, ? super String> props) {
	this(ROOT, name, props);
    }

    public String getSpi(String key) {
	Object rv = props.get(key);
	return((rv instanceof String) ? (String)rv : null);
    }

    public void putSpi(String key, String val) {
	props.put(key, val);
    }

    public void removeSpi(String key) {
	props.remove(key);
    }

    public String[] keysSpi() {
	List<String> buf = new ArrayList<>();
	for(Map.Entry<? super String, ? super String> ent : props.entrySet()) {
	    if((ent.getKey() instanceof String) && (ent.getValue() instanceof String))
		buf.add((String)ent.getValue());
	}
	return(buf.toArray(new String[0]));
    }

    public String[] childrenNamesSpi() {return(new String[]{});}

    public AbstractPreferences childSpi(String name) {
	return(new MapPrefs(this, name, Collections.emptyMap()));
    }

    public void removeNodeSpi() {}
    public void flushSpi() {}
    public void syncSpi() {}
}
