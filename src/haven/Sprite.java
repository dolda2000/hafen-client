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

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.util.*;
import java.util.function.*;
import java.lang.reflect.Constructor;
import haven.render.*;

public abstract class Sprite implements RenderTree.Node {
    public final Resource res;
    public final Owner owner;
    public static List<Factory> factories = new LinkedList<Factory>();
    static {
	factories.add(SpriteLink.sfact);
	factories.add(SkelSprite.fact);
	factories.add(AnimSprite.fact);
	factories.add(StaticSprite.fact);
	factories.add(AudioSprite.fact);
    }

    public interface Owner extends OwnerContext {
	public Random mkrandoom();
	public Resource getres();
	@Deprecated
	public default Glob glob() {return(context(Glob.class));}
    }

    public class RecOwner implements Owner, Skeleton.HasPose {
	public Random mkrandoom() {return(owner.mkrandoom());}
	public <T> T context(Class<T> cl) {return(owner.context(cl));}

	public Resource getres() {return(res);}

	public Skeleton.Pose getpose() {return(Skeleton.getpose(Sprite.this));}
    }

    public static interface CDel {
	public void delete();
    }

    public static interface CUpd {
	public void update(Message sdt);
    }

    public static class FactMaker implements Resource.PublishedCode.Instancer {
	public Factory make(Class<?> cl, Resource ires, Object... args) {
	    if(Factory.class.isAssignableFrom(cl))
		return(Resource.PublishedCode.Instancer.stdmake(cl.asSubclass(Factory.class), ires, args));
	    try {
		Function<Object[], Sprite> make = Utils.smthfun(cl, "mksprite", Sprite.class, Owner.class, Resource.class, Message.class);
		return(new Factory() {
			public Sprite create(Owner owner, Resource res, Message sdt) {
			    return(make.apply(new Object[] {owner, res, sdt}));
			}
		    });
	    } catch(NoSuchMethodException e) {}
	    if(Sprite.class.isAssignableFrom(cl)) {
		Class<? extends Sprite> scl = cl.asSubclass(Sprite.class);
		try {
		    Function<Object[], ? extends Sprite> make = Utils.consfun(scl, Owner.class, Resource.class);
		    return((owner, res, sdt) -> make.apply(new Object[]{owner, res}));
		} catch(NoSuchMethodException e) {}
		try {
		    Function<Object[], ? extends Sprite> make = Utils.consfun(scl, Owner.class, Resource.class, Message.class);
		    return((owner, res, sdt) -> make.apply(new Object[]{owner, res, sdt}));
		} catch(NoSuchMethodException e) {}
	    }
	    throw(new RuntimeException("Could not find any suitable constructor for dynamic sprite"));
	}
    }

    @Resource.PublishedCode(name = "spr", instancer = FactMaker.class)
    public interface Factory {
	public Sprite create(Owner owner, Resource res, Message sdt);
    }

    public static class ResourceException extends RuntimeException {
	public Resource res;

	public ResourceException(String msg, Resource res) {
	    super(msg + " (" + res + ", from " + res.source + ")");
	    this.res = res;
	}

	public ResourceException(String msg, Throwable cause, Resource res) {
	    super(msg + " (" + res + ", from " + res.source + ")", cause);
	    this.res = res;
	}
    }

    protected Sprite(Owner owner, Resource res) {
	this.res = res;
	this.owner = owner;
    }

    public static int decnum(Message sdt) {
	if(sdt == null)
	    return(0);
	int ret = 0, off = 0;
	while(!sdt.eom()) {
	    ret |= sdt.uint8() << off;
	    off += 8;
	}
	return(ret);
    }

    public static Sprite create(Owner owner, Resource res, Message sdt) {
	{
	    Factory f = res.getcode(Factory.class, false);
	    if(f != null)
		return(f.create(owner, res, sdt));
	}
	for(Factory f : factories) {
	    Sprite ret = f.create(owner, res, sdt);
	    if(ret != null)
		return(ret);
	}
	/* XXXRENDER
	throw(new ResourceException("Does not know how to draw resource " + res.name, res));
	*/
	return(new Sprite(owner, res) {});
    }

    public void draw(GOut g) {}

    public boolean tick(double dt) {
	return(false);
    }

    public void gtick(Render g) {
    }

    public void dispose() {
    }
}
