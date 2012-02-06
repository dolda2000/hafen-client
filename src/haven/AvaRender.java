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

import static haven.Resource.imgc;
import javax.media.opengl.GL;
import java.util.*;

public class AvaRender extends TexRT {
    List<Indir<Resource>> layers;
    List<Resource.Image> images;
    boolean loading;
    public static final Coord sz = new Coord(212, 249);
    
    public AvaRender(List<Indir<Resource>> layers) {
	super(sz);
	setlay(layers);
    }
    
    public void setlay(List<Indir<Resource>> layers) {
        this.layers = layers;
        loading = true;
    }

    public boolean subrend(GOut g) {
	if(!loading)
	    return(false);

	List<Resource.Image> images = new ArrayList<Resource.Image>();
	loading = false;
	for(Indir<Resource> r : layers) {
	    try {
		images.addAll(r.get().layers(imgc));
	    } catch(Loading e) {
		loading = true;
	    }
	}
	Collections.sort(images);
	if(images.equals(this.images))
	    return(false);
	this.images = images;

	g.gl.glClearColor(255, 255, 255, 0);
	g.gl.glClear(GL.GL_COLOR_BUFFER_BIT);
	for(Resource.Image i : images)
	    g.image(i.tex(), i.o);
        return(true);
    }
}
