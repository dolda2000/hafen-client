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

public class Buff {
    public static final Text.Foundry nfnd = new Text.Foundry("SansSerif", 10);
    int id;
    Indir<Resource> res;
    String tt = null;
    int ameter = -1;
    int nmeter = -1;
    int cmeter = -1;
    int cticks = -1;
    long gettime;
    Tex ntext = null;
    boolean major = false;
    
    public Buff(int id, Indir<Resource> res) {
	this.id = id;
	this.res = res;
    }
    
    Tex nmeter() {
	if(ntext == null)
	    ntext = new TexI(Utils.outline2(nfnd.render(Integer.toString(nmeter), Color.WHITE).img, Color.BLACK));
	return(ntext);
    }
    
    public String tooltip() {
	if(tt != null)
	    return(tt);
	return(res.get().layer(Resource.tooltip).t);
    }
}
