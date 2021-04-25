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

import java.util.function.*;

public abstract class ACheckBox extends Widget {
    public boolean a = false;

    public ACheckBox() {}
    public ACheckBox(Coord sz) {super(sz);}

    public Supplier<Boolean> state = () -> this.a;
    public ACheckBox state(Supplier<Boolean> state) {this.state = state; return(this);}
    public boolean state() {
	return(state.get());
    }

    public Consumer<Boolean> changed = a -> {
	if(canactivate)
	    wdgmsg("ch", a ? 1 : 0);
    };
    public ACheckBox changed(Consumer<Boolean> changed) {this.changed = changed; return(this);}
    public void changed(boolean val) {changed.accept(val);}

    public Consumer<Boolean> set = a -> {
	if(this.a != a) {
	    this.a = a;
	    changed(a);
	}
    };
    public ACheckBox set(Consumer<Boolean> set) {this.set = set; return(this);}
    public void set(boolean a) {set.accept(a);}

    public Runnable click = () -> set(!state());
    public ACheckBox click(Runnable click) {this.click = click; return(this);}
    public void click() {click.run();}

    public boolean gkeytype(java.awt.event.KeyEvent ev) {
	click();
	return(true);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "ch") {
	    this.a = ((Integer)args[0]) != 0;
	} else {
	    super.uimsg(msg, args);
	}
    }
}
