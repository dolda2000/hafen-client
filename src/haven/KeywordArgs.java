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

public class KeywordArgs {
    public final Map<String, Object> parsed = new HashMap<>();
    public final Object[] argv;
    public final Resource.Pool respool;

    public KeywordArgs(Object[] argv, Resource.Pool respool, String... pos) {
	this.argv = argv;
	this.respool = respool;
	int a = 0, p = 0;
	while(a < argv.length) {
	    if(argv[a] instanceof Map) {
		for(Map.Entry ent : ((Map<?, ?>)argv[a++]).entrySet()) {
		    parsed.put((String)ent.getKey(), ent.getValue());
		}
	    } else if(p < pos.length) {
		String spec = pos[p++];
		if(spec.startsWith("@")) {
		    Resource.Spec res = new Resource.Spec(respool, (String)argv[a++], Utils.iv(argv[a++]));
		    parsed.put(spec.substring(1), res);
		} else {
		    parsed.put(spec, argv[a++]);
		}
	    } else {
		/* Legacy form */
		Object[] kwa = (Object[])argv[a++];
		String key = (String)kwa[0];
		Object val;
		if((kwa.length == 3) && (kwa[1] instanceof String) && (kwa[2] instanceof Number))
		    val = new Resource.Spec(respool, (String)kwa[1], Utils.iv(kwa[2]));
		else
		    val = kwa[1];
		parsed.put(key, val);
	    }
	}
    }

    public KeywordArgs(Object[] argv, String... pos) {
	this(argv, null, pos);
    }

    public Object get(String nm, Object def) {
	return(parsed.getOrDefault(nm, def));
    }

    public static class MissingArgumentException extends RuntimeException {
	public final String name;
	public final Object[] argv;

	public MissingArgumentException(String name, Object[] argv) {
	    super("required argument missing: " + name);
	    this.name = name;
	    this.argv = argv;
	}
    }

    public Object get(String nm) {
	if(!parsed.containsKey(nm))
	    throw(new MissingArgumentException(nm, argv));
	return(parsed.get(nm));
    }
}
