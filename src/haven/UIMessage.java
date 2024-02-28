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

import java.awt.Color;

public interface UIMessage {
    public static final Text.Foundry msgfoundry = new Text.Foundry(Text.dfont, 14);

    public String text();
    public Color color();
    public Resource sfx();

    public static interface RWidget {
	public void msg(UIMessage msg);

	public static RWidget find(Widget w) {
	    for(Widget ch = w.child; ch != null; ch = ch.next) {
		RWidget ret = find(ch);
		if(ret != null)
		    return(ret);
	    }
	    if(w instanceof RWidget)
		return((RWidget)w);
	    return(null);
	}
    }

    public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
	public FactMaker() {super(Factory.class);}
	{
	    add(new Direct<>(Factory.class));
	    add(new StaticCall<>(Factory.class, "mkmessage", UIMessage.class, new Class<?>[] {OwnerContext.class, Object[].class},
				 (make) -> (owner, args) -> make.apply(new Object[] {owner, args})));
	    add(new Construct<>(Factory.class, UIMessage.class, new Class<?>[] {OwnerContext.class, Object[].class},
				(cons) -> (owner, args) -> cons.apply(new Object[] {owner, args})));
	}}
    @Resource.PublishedCode(name = "msg", instancer = FactMaker.class)
    public static interface Factory {
	public UIMessage format(OwnerContext owner, Object... args);
    }

    public static class Error implements UIMessage {
	public static final Color color = new Color(192, 0, 0);
	public static final Resource sfx = Resource.local().loadwait("sfx/error");
	public final String text;

	public Error(String text) {
	    this.text = text;
	}

	public String text() {return(text);}
	public Color color() {return(color);}
	public Resource sfx() {return(sfx);}
    }

    public static class Info implements UIMessage {
	public static final Color color = Color.WHITE;
	public static final Resource sfx = Resource.local().loadwait("sfx/msg");
	public final String text;

	public Info(String text) {
	    this.text = text;
	}

	public String text() {return(text);}
	public Color color() {return(color);}
	public Resource sfx() {return(sfx);}
    }
}
