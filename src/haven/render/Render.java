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

import java.nio.*;
import haven.render.sl.Attribute;

public interface Render {
    public Environment env();
    public void draw(Pipe pipe, Model data);

    public default void draw(Pipe pipe, Model.Mode mode, short[] ind, Attribute tgt1, int nc1, float[] data1) {
	Model.Indices indb = null;
	if(ind != null)
	    indb = new Model.Indices(ind.length, NumberFormat.UINT16, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(ind));
	VertexArray vao = new VertexArray(new VertexArray.Buffer(data1.length / nc1, nc1, NumberFormat.FLOAT32, DataBuffer.Usage.EPHEMERAL, tgt1, DataBuffer.Filler.of(data1)));
	draw(pipe, new Model(mode, vao, indb));
    }
    public default void draw(Pipe pipe, Model.Mode mode, short[] ind, Attribute tgt1, int nc1, float[] data1, Attribute tgt2, int nc2, float[] data2) {
	Model.Indices indb = null;
	if(ind != null)
	    indb = new Model.Indices(ind.length, NumberFormat.UINT16, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(ind));
	VertexArray vao = new VertexArray(
	    new VertexArray.Buffer(data1.length / nc1, nc1, NumberFormat.FLOAT32, DataBuffer.Usage.EPHEMERAL, tgt1, DataBuffer.Filler.of(data1)),
	    new VertexArray.Buffer(data2.length / nc2, nc2, NumberFormat.FLOAT32, DataBuffer.Usage.EPHEMERAL, tgt2, DataBuffer.Filler.of(data2))
	);
	draw(pipe, new Model(mode, vao, indb));
    }
    public default void draw(Pipe pipe, Model.Mode mode, short[] ind, Attribute tgt1, int nc1, float[] data1, Attribute tgt2, int nc2, float[] data2, Attribute tgt3, int nc3, float[] data3) {
	Model.Indices indb = null;
	if(ind != null)
	    indb = new Model.Indices(ind.length, NumberFormat.UINT16, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(ind));
	VertexArray vao = new VertexArray(
	    new VertexArray.Buffer(data1.length / nc1, nc1, NumberFormat.FLOAT32, DataBuffer.Usage.EPHEMERAL, tgt1, DataBuffer.Filler.of(data1)),
	    new VertexArray.Buffer(data2.length / nc2, nc2, NumberFormat.FLOAT32, DataBuffer.Usage.EPHEMERAL, tgt2, DataBuffer.Filler.of(data2)),
	    new VertexArray.Buffer(data3.length / nc3, nc3, NumberFormat.FLOAT32, DataBuffer.Usage.EPHEMERAL, tgt3, DataBuffer.Filler.of(data3))
	);
	draw(pipe, new Model(mode, vao, indb));
    }
}
