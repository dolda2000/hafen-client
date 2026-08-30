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

package haven.ffi.objc;

import java.util.*;
import java.util.function.*;
import java.lang.foreign.*;
import haven.*;
import haven.ffi.objc.Runtime.*;
import static haven.ffi.FUtils.*;

public class ObjectTracker<T extends NSObject> {
    public final Runtime.Class cls;
    private final Runtime rt = Runtime.get();
    private final Map<Integer, T> reg = new CacheMap<>(CacheMap.RefType.WEAK);
    private int nextkey = 0;

    public ObjectTracker(Runtime.Class cls) {
	this.cls = cls;
    }

    public void reg(T obj) {
	ID id = obj.id();
	int key;
	synchronized(this) {
	    key = nextkey++;
	    rt.object_getIvar(id, cls, "java", ValueLayout.JAVA_INT).set(ValueLayout.JAVA_INT, 0, key);
	    reg.put(key, obj);
	}
	Finalizer.finalize(obj, () -> {
	    synchronized(this) {
		reg.remove(key);
		rt.release(id);
	    }
	});
    }

    public T get(MemorySegment objp) {
	ID obj = rt.id(objp);
	int key = rt.object_getIvar(obj, cls, "java", ValueLayout.JAVA_INT).get(ValueLayout.JAVA_INT, 0);
	T java;
	synchronized(this) {
	    java = reg.get(key);
	}
	if(java == null)
	    throw(new RuntimeException(String.format("object not properly retained: %s (%s)", obj, cls)));
	return(java);
    }

    public <R> R wrap(MemorySegment objp, Function<? super T, ?extends R> fun, R eret) {
	return(upcallwrap(() -> fun.apply(get(objp)), eret));
    }

    public void wrap(MemorySegment objp, Consumer<? super T> fun) {
	upcallwrap(() -> fun.accept(get(objp)));
    }
}
