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
import java.awt.image.BufferedImage;

public class ItemSpec implements GSprite.Owner, ItemInfo.SpriteOwner {
    private static final Object[] definfo = {
	new Object[] {new ItemInfo.Name.Default()}
    };
    public final Object[] info;
    public final ResData res;
    public final OwnerContext ctx;

    public ItemSpec(OwnerContext ctx, ResData res, Object[] info) {
	this.res = res;
	this.ctx = ctx;
	this.info = info;
    }

    public <T> T context(Class<T> cl) {return(ctx.context(cl));}
    public Resource getres() {return(res.res.get());}
    private Random rnd = null;
    public Random mkrandoom() {
	if(rnd == null)
	    rnd = new Random();
	return(rnd);
    }
    public GSprite sprite() {return(spr());}
    public Resource resource() {return(res.res.get());}

    private GSprite spr = null;
    public GSprite spr() {
	if(spr == null)
	    spr = GSprite.create(this, res.res.get(), res.sdt.clone());
	return(spr);
    }

    public BufferedImage image() {
	GSprite spr = spr();
	if(spr instanceof GSprite.ImageSprite)
	    return(((GSprite.ImageSprite)spr).image());
	return(null);
    }

    private List<ItemInfo> cinfo = null;
    public List<ItemInfo> info() {
	if(cinfo == null) {
	    Object[] info = this.info;
	    if(info == null)
		info = definfo;
	    cinfo = ItemInfo.buildinfo(this, info);
	}
	return(cinfo);
    }

    public String name() {
	ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, info());
	if(nm == null)
	    return(null);
	return(nm.str.text);
    }
}
