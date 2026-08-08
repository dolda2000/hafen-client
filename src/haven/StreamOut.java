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
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import haven.render.*;
import haven.render.Pipe;

public class StreamOut {
    public static final Config.Variable<Ratio> rate = Config.Variable.propr("haven.streamrate", Ratio.of(30));
    public final NutPipe out;
    public final Coord sz;
    public final Audio.CS audio;
    private final Queue<ByteBuffer> free = new LinkedList<>();
    private ByteBuffer obuf = null;
    private Thread ot = null;
    private boolean running = true;

    public StreamOut(Coord sz, Audio.CS audio, WritableByteChannel out) throws IOException {
	this.sz = sz;
	this.audio = audio;
	this.out = new NutPipe(out, sz, rate.get(), Ratio.of(Audio.SAMPLE_RATE), 2);
    }

    public StreamOut(Coord sz, Audio.CS audio, Path out) throws IOException {
	this(sz, audio, Files.newByteChannel(out, StandardOpenOption.WRITE));
    }

    private Ratio vabspos = Ratio.z;
    private long aabspos = 0;
    private void writeaudio(Ratio vadv) throws IOException {
	vabspos = vabspos.add(vadv);
	long apos = vabspos.mul(Audio.SAMPLE_RATE).floor();
	int delta = (int)(apos - aabspos);
	aabspos = apos;
	while(delta > 0) {
	    double[][] buf = new double[2][delta];
	    int rv = audio.get(buf, delta);
	    if(rv < 0) {
		out.audioframe(buf, 0, delta);
		break;
	    } else if(rv > 0) {
		out.audioframe(buf, 0, rv);
		delta -= rv;
	    }
	}
    }

    private void writeframe(ByteBuffer data) throws IOException {
	byte[] buf = new byte[sz.x * sz.y * 3];
	for(int y = sz.y - 1, oy = 0; y >= 0; y--, oy++) {
	    data.limit((y + 1) * sz.x * 3).position(y * sz.x * 3);
	    data.get(buf, oy * sz.x * 3, sz.x * 3);
	}
	out.videoframe(buf);
    }

    private void routput(Ratio rate) {
	Ratio dur = rate.rcp();
	Ratio last = Ratio.rtime();
	ByteBuffer data = null;
	main: while(running) {
	    synchronized(this) {
		if(obuf != null) {
		    if(data != null) {
			data.clear();
			free.add(data);
		    }
		    data = obuf;
		    obuf = null;
		}
	    }
	    while(true) {
		Ratio now = Ratio.rtime();
		if(now.sub(last).ceil() > 5) {
		    Warning.warn("streamout frame timing reset");
		    last = now;
		    break;
		} else if(now.gt(last.add(dur))) {
		    last = last.add(dur);
		    break;
		}
		try {
		    long nanos = last.add(dur).sub(now).mul(1000000000).ceil();
		    Thread.sleep(nanos / 1000000, (int)(nanos % 1000000));
		} catch(InterruptedException e) {
		    continue main;
		}
	    }
	    try {
		writeframe(data);
		writeaudio(dur);
	    } catch(IOException e) {
		running = false;
		throw(new RuntimeException(e));
	    }
	}
    }

    private void output() {
	try {
	    routput(rate.get());
	    try {
		out.close();
	    } catch(IOException e) {
		new Warning(e, "could not close stream-out channel").issue();
	    }
	} finally {
	    synchronized(this) {
		if(running && (obuf != null) && (ot != null)) {
		    ot = new HackThread(this::output, "Stream-out");
		    ot.start();
		}
	    }
	}
    }

    private void fin(ByteBuffer buf) {
	synchronized(this) {
	    if(obuf != null)
		free.add(obuf);
	    obuf = buf;
	    this.notifyAll();
	    if(ot == null) {
		ot = new HackThread(this::output, "Stream-out");
		ot.start();
	    }
	}
    }

    public void accept(Render g, Pipe state) {
	if(!running)
	    return;
	ByteBuffer buf;
	synchronized(this) {
	    if(free.isEmpty())
		buf = Utils.mkbbuf(sz.x * sz.y * 3);
	    else
		buf = free.remove();
	}
	g.pget(state, FragColor.fragcol, Area.sized(sz), new VectorFormat(3, NumberFormat.UNORM8), buf, this::fin);
    }

    public void close() {
	synchronized(this) {
	    running = false;
	    if(ot == null) {
		try {
		    out.close();
		} catch(IOException e) {
		    new Warning(e, "could not close stream-out channel").issue();
		}
	    }
	}
    }

    public static class NutEncoder {
	/* See https://ffmpeg.org/~michael/nut.txt */
	private static final int MAX_DISTANCE = (1024 * 32) - 1;
	private static final byte[] SIG = "nut/multimedia container\0".getBytes(Utils.utf8);
	private static final int NUT_PIPE = 2;
	private static long NM = 0x4e4d7a561f5f04adl;
	private static long NS = 0x4e5311405bf2f9dbl;
	private static long NI = 0x4e4911405bf2f9dbl;
	private static long NK = 0x4e4be4adeeca4569l;
	public final Coord vdim;
	public final Ratio vrate, arate;
	public final int achan;
	private final FrameCode[] fcs = new FrameCode[256];
	private final StreamInfo[] streams = new StreamInfo[2];
	private final Encoder out;

	public static class CRC32 {
	    public static final CRC32 IEEE = new CRC32(0x04c11db7, false, 0);
	    private final int[] tab = new int[256];
	    private final int init;

	    public CRC32(int gen, boolean le, int init) {
		if(le) {
		    for(int i = 0; i < 256; i++) {
			int c = i;
			for(int o = 0; o < 8; o++)
			    c = (c >> 1) ^ (gen & (-(c & 1)));
			tab[i] = c;
		    }
		} else {
		    for(int i = 0; i < 256; i++) {
			int c = i << 24;
			for(int o = 0; o < 8; o++)
			    c = (c << 1) ^ (gen & (c >> 31));
			for(int o = 0; o < 32; o += 8)
			    tab[i] |= ((c >> o) & 0xff) << (24 - o);
		    }
		}
		this.init = init;
	    }

	    public int calc(byte[] data, int off, int len) {
		int ret = init;
		for(int i = 0; i < len; i++)
		    ret = tab[(ret & 0xff) ^ (data[i + off] & 0xff)] ^ (ret >>> 8);
		return(ret);
	    }

	    public int calc(byte[] data) {
		return(calc(data, 0, data.length));
	    }
	}

	public static class Encoder {
	    public final OutputStream fp;

	    public Encoder(OutputStream fp) {
		this.fp = fp;
	    }


	    public Encoder write(byte b) throws IOException {
		fp.write(b); return(this);
	    }

	    public Encoder write(byte[] b) throws IOException {
		fp.write(b); return(this);
	    }

	    public Encoder f(byte b) throws IOException {
		write(b); return(this);
	    }

	    public Encoder f(short b) throws IOException {
		write((byte)(b >>> 8)); write((byte)b);
		return(this);
	    }

	    public Encoder f(int b) throws IOException {
		write((byte)(b >>> 24)); write((byte)(b >>> 16)); write((byte)(b >>> 8)); write((byte)b);
		return(this);
	    }

	    /* Checksums seem to be written little-endian, despite what the spec says... */
	    public Encoder fl(int b) throws IOException {
		write((byte)b); write((byte)(b >>> 8)); write((byte)(b >>> 16)); write((byte)(b >>> 24));
		return(this);
	    }

	    public Encoder f(long b) throws IOException {
		write((byte)(b >>> 56)); write((byte)(b >>> 48)); write((byte)(b >>> 40)); write((byte)(b >>> 32));
		write((byte)(b >>> 24)); write((byte)(b >>> 16)); write((byte)(b >>>  8)); write((byte)(b >>>  0));
		return(this);
	    }

	    public Encoder v(long v) throws IOException {
		byte[] b = new byte[10]; int n = 1;
		b[0] = (byte)(v & 0x7f); v >>>= 7;
		while(v > 0) {
		    b[n++] = (byte)((v & 0x7f) | 0x80);
		    v >>>= 7;
		}
		while(n > 0)
		    write(b[--n]);
		return(this);
	    }

	    public Encoder s(long v) throws IOException {
		if(v > 0)
		    return(v((v << 1) - 1));
		else
		    return(v(-v << 1));
	    }

	    public Encoder vb(byte[] data) throws IOException {
		v(data.length);
		write(data);
		return(this);
	    }

	    public Encoder vb(String data) throws IOException {
		return(vb(data.getBytes(Utils.utf8)));
	    }

	    public Encoder packet(long startcode, byte[] data) throws IOException {
		Buffer head = new Buffer();
		head.f(startcode);
		head.v(data.length + 4);
		write(head.close());
		if(data.length > 0x1000) {
		    fl(CRC32.IEEE.calc(head.close()));
		}
		write(data);
		fl(CRC32.IEEE.calc(data));
		return(this);
	    }
	}

	public static class Buffer extends Encoder {
	    public Buffer() {
		super(new ByteArrayOutputStream());
	    }

	    public byte[] close() {
		return(((ByteArrayOutputStream)fp).toByteArray());
	    }
	}

	public static class StreamInfo {
	    public final int ptsshift;
	    public long lastpts = 0;

	    public StreamInfo(int ptsshift) {
		this.ptsshift = ptsshift;
	    }

	    private long lsb2full(long lsb) {
		long mask = (1l << ptsshift) - 1;
		long delta = lastpts - (mask >> 2);
		return(((lsb - delta) & mask) + delta);
	    }

	    public long codepts(long nextpts) {
		long ret = nextpts & ((1l << ptsshift) - 1);
		if(lsb2full(ret) == nextpts)
		    return(ret);
		return(nextpts + (1l << ptsshift));
	    }
	}

	public static class FrameCode {
	    public static final int KEY         = 1 <<  0;
	    public static final int EOR         = 1 <<  1;
	    public static final int CODED_PTS   = 1 <<  3;
	    public static final int STREAM_ID   = 1 <<  4;
	    public static final int SIZE_MSB    = 1 <<  5;
	    public static final int CHECKSUM    = 1 <<  6;
	    public static final int RESERVED    = 1 <<  7;
	    public static final int SM_DATA     = 1 <<  8;
	    public static final int HEADER_IDX  = 1 << 10;
	    public static final int MATCH_TIME  = 1 << 11;
	    public static final int CODED_FLAGS = 1 << 12;
	    public static final int INVALID     = 1 << 13;
	    public final int flags, sid, szmul, szlsb, ptsdelta, rescount, headidx;

	    public FrameCode(int flags, int sid, int szmul, int szlsb, int ptsdelta, int rescount, int headidx) {
		this.flags = flags; this.sid = sid;
		this.szmul = szmul; this.szlsb = szlsb; this.ptsdelta = ptsdelta;
		this.rescount = rescount; this.headidx = headidx;
	    }
	}

	public NutEncoder(WritableByteChannel fp, Coord vdim, Ratio vrate, Ratio arate, int achan) throws IOException {
	    this.vdim = vdim;
	    this.vrate = vrate;
	    this.arate = arate;
	    this.achan = achan;
	    this.out = new Encoder(new BufferedOutputStream(Channels.newOutputStream(fp)));
	    for(int i = 0; i < 256; i++)
		fcs[i] = new FrameCode(FrameCode.INVALID, 0, 1, 0, 0, 0, 0);
	    fcs[1] = new FrameCode(FrameCode.CODED_FLAGS, 0, 1, 0, 0, 0, 0);
	    streams[0] = new StreamInfo((vrate.floor() >= 1000) ? 14 : 7);
	    streams[1] = new StreamInfo((arate.floor() >= 1000) ? 14 : 7);
	    headers();
	}

	public void close() throws IOException {
	    out.fp.close();
	}

	private void mainheader() throws IOException {
	    Buffer buf = new Buffer();
	    buf.v(4).v(1);	// version
	    buf.v(2);		// stream_count
	    buf.v(MAX_DISTANCE);
	    buf.v(2);		// time_base_count
	    buf.v(vrate.q).v(vrate.p);
	    buf.v(arate.q).v(arate.p);

	    int runpts = 0, runmul = 1, runsid = 0, runhdi = 0;
	    long runmatch = 1 - (1l << 62);
	    for(int i = 0; i < 256;) {
		FrameCode fc = fcs[i];
		int fno = 0;
		if(fc.ptsdelta != runpts) fno = 1;
		if(fc.szmul    != runmul) fno = 2;
		if(fc.sid      != runsid) fno = 3;
		if(fc.szlsb    != 0)      fno = 4;
		if(fc.rescount != 0)      fno = 5;
		if(fc.headidx  != runhdi) fno = 8;

		runpts = fc.ptsdelta;
		runsid = fc.sid;
		runmul = fc.szmul;
		runhdi = fc.headidx;

		int run;
		for(run = 0; i < 256; i++, run++) {
		    if(i == 'N') {
			run--;
			continue;
		    }
		    FrameCode rc = fcs[i];
		    if(rc.ptsdelta != runpts)         break;
		    if(rc.flags    != fc.flags)       break;
		    if(rc.sid      != runsid)         break;
		    if(rc.szmul    != runmul)         break;
		    if(rc.szlsb    != fc.szlsb + run) break;
		    if(rc.rescount != fc.rescount)    break;
		    if(rc.headidx  != fc.headidx)     break;
		}
		if(run != runmul - fc.szlsb)
		    fno = Math.max(fno, 6);

		buf.v(fc.flags);
		buf.v(fno);
		if(fno > 0) buf.s(fc.ptsdelta);
		if(fno > 1) buf.v(fc.szmul);
		if(fno > 2) buf.v(fc.sid);
		if(fno > 3) buf.v(fc.szlsb);
		if(fno > 4) buf.v(fc.rescount);
		if(fno > 5) buf.v(run);
		if(fno > 6) buf.v(runmatch);
		if(fno > 7) buf.v(fc.headidx);
	    }
	    buf.v(0);		// Don't bother with elision headers
	    buf.v(NUT_PIPE);

	    out.packet(NM, buf.close());
	}

	private void videoheader(int sid) throws IOException {
	    Buffer buf = new Buffer();
	    buf.v(sid);
	    buf.v(0);			// stream_class
	    buf.vb("RGB\u0018");	// fourcc
	    buf.v(0);			// time_base_id
	    buf.v(streams[sid].ptsshift);
	    buf.v(vrate.floor());	// max_pts_distance
	    buf.v(0);			// decode_delay
	    buf.v(0);			// stream_flags
	    buf.vb(new byte[0]);	// codec_specific_data

	    buf.v(vdim.x).v(vdim.y);
	    buf.v(0).v(0);		// sample_{width,height}
	    buf.v(0);			// colorspace_type

	    out.packet(NS, buf.close());
	}

	private void audioheader(int sid) throws IOException {
	    Buffer buf = new Buffer();
	    buf.v(sid);
	    buf.v(1);			// stream_class
	    buf.vb("PSD\u0010");	// fourcc
	    buf.v(1);			// time_base_id
	    buf.v(streams[sid].ptsshift);
	    buf.v(arate.floor()); 	// max_pts_distance
	    buf.v(0);			// decode_delay
	    buf.v(0);			// stream_flags
	    buf.vb(new byte[0]);	// codec_specific_data

	    buf.v(arate.p).v(arate.q);
	    buf.v(achan);		// channel_count

	    out.packet(NS, buf.close());
	}

	private void infoheader(int sid, String key, Object val) throws IOException {
	    Buffer buf = new Buffer();
	    buf.v(sid + 1);
	    buf.s(0).v(0).v(0);		// chapter info
	    buf.v(1);			// count
	    buf.vb(key);
	    if(val instanceof String) {
		buf.s(-1);
		buf.vb((String)val);
	    } else {
		throw(new IllegalArgumentException(String.valueOf(val)));
	    }

	    out.packet(NI, buf.close());
	}

	private long lastnk = -1;
	private void syncpoint(int timebase, long pts) throws IOException {
	    if(lastnk != -1)
		throw(new AssertionError("multiple syncpoints not yet supported"));
	    Buffer buf = new Buffer();
	    buf.v(pts * 2);
	    buf.v(0);

	    lastnk = 0;
	    out.packet(NK, buf.close());
	}

	private void headers() throws IOException {
	    out.write(SIG);
	    mainheader();
	    videoheader(0);
	    audioheader(1);
	    infoheader(-1, "encoder", "Haven streamout");
	    /* Apparently an initial syncpoint is still rqeuired in
	     * PIPE mode as a terminator for info headers. */
	    syncpoint(0, 0);
	}

	private void frame(int sid, boolean key, long pts, byte[] data) throws IOException {
	    Buffer buf = new Buffer();
	    StreamInfo s = streams[sid];
	    /* Just do all the things. No need for compression. */
	    FrameCode fc = fcs[1];
	    int flags = FrameCode.STREAM_ID | FrameCode.CHECKSUM;
	    if(pts != s.lastpts + fc.ptsdelta) flags |= FrameCode.CODED_PTS;
	    if(data.length >= fc.szmul) flags |= FrameCode.SIZE_MSB;
	    if(key) flags |= FrameCode.KEY;

	    if((data.length % fc.szmul) != fc.szlsb)
		throw(new AssertionError());
	    buf.f((byte)1);		// framecode
	    if((fc.flags & FrameCode.CODED_FLAGS) != 0)
		buf.v((fc.flags ^ flags) & ~FrameCode.CODED_FLAGS);
	    if((flags & FrameCode.STREAM_ID) != 0)
		buf.v(sid);
	    if((flags & FrameCode.CODED_PTS) != 0)
		buf.v(s.codepts(pts));
	    if((flags & FrameCode.SIZE_MSB) != 0)
		buf.v(data.length / fc.szmul);
	    if((flags & FrameCode.HEADER_IDX) != 0)
		throw(new AssertionError());

	    out.write(buf.close());
	    if((flags & FrameCode.CHECKSUM) != 0)
		out.fl(CRC32.IEEE.calc(buf.close()));
	    if((flags & FrameCode.SM_DATA) != 0)
		throw(new AssertionError());
	    out.write(data);

	    s.lastpts = pts;
	}

	public void videoframe(long pts, byte[] pixels) throws IOException {
	    if(pixels.length != (vdim.x * vdim.y * 3))
		throw(new IllegalArgumentException());
	    frame(0, true, pts, pixels);
	}

	public void audioframe(long pts, double[][] samples, int off, int len) throws IOException {
	    if(samples.length != achan)
		throw(new IllegalArgumentException());
	    byte[] data = new byte[len * achan * 2];
	    for(int i = 0, o = 0; i < len; i++) {
		for(int c = 0; c < achan; c++) {
		    int pc = (int)Math.round(Utils.clip(samples[c][i + off], -1.0, 1.0) * 32767);
		    data[o++] = (byte)(pc >>> 0);
		    data[o++] = (byte)(pc >>> 8);
		}
	    }
	    frame(1, true, pts, data);
	}
    }

    public static class NutPipe extends NutEncoder {
	public long vpts = 0, apts = 0;

	public NutPipe(WritableByteChannel fp, Coord vdim, Ratio vrate, Ratio arate, int achan) throws IOException {
	    super(fp, vdim, vrate, arate, achan);
	}

	public void videoframe(byte[] pixels) throws IOException {
	    videoframe(vpts++, pixels);
	}

	public void audioframe(double[][] samples, int off, int len) throws IOException {
	    if(len > 4096)
		throw(new IOException("audio-frame splitting not yet supported"));
	    audioframe(apts, samples, off, len);
	    apts += len;
	}
    }
}
