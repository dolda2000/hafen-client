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

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class AnimGSprite extends GSprite implements GSprite.ImageSprite {
    public final Resource.Anim anim;
    public final Resource.Image ref;
    private int f, ft;

    public static final Factory fact = new Factory() {
	    public GSprite create(Owner owner, Resource res, Message sdt) {
		Resource.Anim anim = res.layer(Resource.animc);
		if(anim != null)
		    return(new AnimGSprite(owner, anim));
		return(null);
	    }
	};

    public AnimGSprite(Owner owner, Resource.Anim anim) {
	super(owner);
	this.anim = anim;
	this.ref = anim.f[0][0];
    }

    public void draw(GOut g) {
	for(Resource.Image img : anim.f[f])
	    g.image(img, Coord.z);
    }

    public Coord sz() {
	return(ref.ssz);
    }

    public BufferedImage image() {
	BufferedImage ret = TexI.mkbuf(ref.ssz);
	Graphics g = ret.getGraphics();
	for(Resource.Image img : anim.f[0])
	    g.drawImage(img.scaled(), img.so.x, img.so.y, null);
	g.dispose();
	return(ret);
    }

    public void tick(double dt) {
	ft += Math.round(dt * 1000);
	while(ft > anim.d) {
	    f = (f + 1) % anim.f.length;
	    ft -= anim.d;
	}
    }
}
