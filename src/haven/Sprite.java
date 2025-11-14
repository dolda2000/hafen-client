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
    }

    public class RecOwner implements Owner {
	public Random mkrandoom() {return(owner.mkrandoom());}
	public <T> T context(Class<T> cl) {return(owner.context(cl));}

	public Resource getres() {return(res);}

	public String toString() {
	    return(String.format("#<rec-owner of %s, owned by %s>", Sprite.this, owner));
	}
    }

    public static class UIOwner implements Owner {
	public final Widget wdg;

	public UIOwner(Widget wdg) {
	    this.wdg = wdg;
	}

	public Random mkrandoom() {return(new Random());}
	public <T> T context(Class<T> cl) {return(Widget.wdgctx.context(cl, wdg));}
    }

    public static interface CDel {
	public void delete();
    }

    public static interface CUpd {
	public void update(Message sdt);
    }

    public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
	public FactMaker() {super(Factory.class);}
	{
	    add(new Direct<>(Factory.class));
	    add(new StaticCall<>(Factory.class, "mksprite", Sprite.class, new Class<?>[] {Owner.class, Resource.class, Message.class},
				 (make) -> (owner, res, sdt) -> make.apply(new Object[] {owner, res, sdt})));
	    add(new Construct<>(Factory.class, Sprite.class, new Class<?>[] {Owner.class, Resource.class, Message.class},
				(cons) -> (owner, res, sdt) -> cons.apply(new Object[] {owner, res, sdt})));
	    add(new Construct<>(Factory.class, Sprite.class, new Class<?>[] {Owner.class, Resource.class},
				(cons) -> (owner, res, sdt) -> cons.apply(new Object[] {owner, res})));
	}}

    @Resource.PublishedCode(name = "spr", instancer = FactMaker.class)
    public interface Factory {
	public Sprite create(Owner owner, Resource res, Message sdt);
    }

    public interface Mill<S extends Sprite> {
	public S create(Owner owner);

	public static class FromRes implements Mill<Sprite> {
	    public final Indir<Resource> res;
	    public final byte[] sdt;

	    public FromRes(Indir<Resource> res, byte[] sdt) {
		this.res = res;
		this.sdt = sdt;
	    }

	    public Sprite create(Owner owner) {
		return(Sprite.create(owner, res.get(), new MessageBuf(sdt)));
	    }

	    public String toString() {
		return(String.format("#<res-mill %s %s>", res, Utils.hex.enc(sdt)));
	    }
	}

	public static Mill<Sprite> of(Indir<Resource> res, byte[] sdt) {
	    return(new FromRes(res, sdt));
	}
	public static Mill<Sprite> of(Indir<Resource> res, Message sdt) {
	    return(new FromRes(res, sdt.bytes()));
	}
	public static Mill<Sprite> of(Resource res, Message sdt) {
	    return(new FromRes(res.indir(), sdt.bytes()));
	}
	public static Mill<Sprite> of(ResData dat) {
	    return(new FromRes(dat.res, dat.sdt.bytes()));
	}
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

    public void age() {
    }

    public void dispose() {
    }

    public String toString() {
	return(String.format("#<%s %s of %s>", this.getClass().getSimpleName(), (res == null) ? null : res.name, owner));
    }
}
