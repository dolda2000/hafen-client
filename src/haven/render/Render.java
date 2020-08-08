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

package haven.render;

import java.util.function.*;
import java.nio.*;
import haven.*;
import haven.render.sl.*;

public interface Render extends Disposable {
    public Environment env();
    public void submit(Render sub);
    public void draw(Pipe pipe, Model data);
    public void clear(Pipe pipe, FragData buf, FColor val);
    public void clear(Pipe pipe, double val);

    public <T extends DataBuffer> void update(T buf, DataBuffer.PartFiller<? super T> data, int from, int to);
    public <T extends DataBuffer> void update(T buf, DataBuffer.Filler<? super T> data);

    public void pget(Pipe pipe, FragData buf, Area area, VectorFormat fmt, Consumer<ByteBuffer> callback);
    public void pget(Texture.Image img, VectorFormat fmt, Consumer<ByteBuffer> callback);
    public void timestamp(Consumer<Long> callback);
    public void fence(Runnable callback);

    public default void draw(Pipe pipe, Model.Mode mode, short[] ind, VertexArray.Layout fmt, int n, float[] data) {
	Model.Indices indb = null;
	if(ind != null)
	    indb = new Model.Indices(ind.length, NumberFormat.UINT16, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(ind));
	VertexArray vao = new VertexArray(fmt, new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(data)));
	Model model = new Model(mode, vao, indb, 0, n);
	draw(pipe, model);
	model.dispose();
    }
}
