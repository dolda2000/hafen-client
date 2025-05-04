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
import java.nio.file.*;
import javax.sound.sampled.*;
import dolda.xiphutil.*;

public class Audio {
    public static final Config.Variable<String> outname = Config.Variable.prop("haven.audio-output", "");
    public static final AudioFormat fmt = new AudioFormat(44100, 16, 2, true, false);
    public static boolean enabled = true;
    public static double volume = Double.parseDouble(Utils.getpref("sfxvol", "1.0"));
    private static int bufsize = Utils.getprefi("audiobuf", Math.round(fmt.getSampleRate() * 0.05f)) * fmt.getFrameSize();
    private static Player player;

    public static void setvolume(double volume) {
	Audio.volume = volume;
	Utils.setpref("sfxvol", Double.toString(volume));
    }

    public interface CS {
	public int get(double[][] buf, int len);
    }

    public interface Clip extends Resource.IDLayer<String> {
	public CS stream();
	public default String layerid() {return("");}
	public default double bvol() {return(1.0);}
    }
    public static final Class<Clip> clip = Clip.class;

    public static class Mixer implements CS {
	public final boolean cont;
	private final Collection<CS> clips = new LinkedList<CS>();

	public Mixer(boolean continuous) {
	    this.cont = continuous;
	}

	public Mixer() {
	    this(false);
	}

	public int get(double[][] dst, int ns) {
	    int nch = dst.length;
	    double[][] buf = new double[nch][ns];
	    int max = 0;
	    synchronized(clips) {
		clip: for(Iterator<CS> i = clips.iterator(); i.hasNext();) {
		    CS cs = i.next();
		    int left = ns;
		    int boff = 0;
		    while(left > 0) {
			int ret = cs.get(buf, left);
			if(ret < 0) {
			    i.remove();
			    continue clip;
			}
			if(boff + ret > max) {
			    for(int ch = 0; ch < nch; ch++)
				Arrays.fill(dst[ch], max, boff + ret, 0.0);
			    max = boff + ret;
			}
			for(int ch = 0; ch < nch; ch++) {
			    for(int sm = 0; sm < ret; sm++)
				dst[ch][boff + sm] += buf[ch][sm];
			}
			left -= ret;
			boff += ret;
		    }
		}
	    }
	    if(cont) {
		for(int ch = 0; ch < nch; ch++) {
		    for(int sm = max; sm < ns; sm++)
			dst[ch][sm] = 0;
		}
		return(ns);
	    } else {
		return((max > 0)?max:-1);
	    }
	}

	public void add(CS clip) {
	    synchronized(clips) {
		clips.add(clip);
	    }
	}

	public void stop(CS clip) {
	    synchronized(clips) {
		for(Iterator<CS> i = clips.iterator(); i.hasNext();) {
		    if(i.next() == clip) {
			i.remove();
			break;
		    }
		}
	    }
	}

	public boolean playing(CS clip) {
	    synchronized(clips) {
		for(CS cs : clips) {
		    if(cs == clip)
			return(true);
		}
	    }
	    return(false);
	}

	public int size() {
	    synchronized(clips) {
		return(clips.size());
	    }
	}

	public boolean empty() {
	    synchronized(clips) {
		return(clips.isEmpty());
	    }
	}

	public Collection<CS> current() {
	    synchronized(clips) {
		return(new ArrayList<CS>(clips));
	    }
	}

	public void clear() {
	    synchronized(clips) {
		clips.clear();
	    }
	}
    }

    public static class PCMClip implements CS {
	public static final int UN8 = 0, SN8 = 1, SN16 = 2, SN32 = 3;
	public final InputStream clip;
	public final int sch, sfmt, ssz;
	public int size = -1;
	private final byte[] dbuf = new byte[256];
	private int head = 0, tail = 0;
	private boolean eof = false;

	public PCMClip(InputStream clip, int nch, int sfmt) {
	    this.clip = clip;
	    this.sch = nch;
	    switch(this.sfmt = sfmt) {
	    case UN8:  ssz = 1; break;
	    case SN8:  ssz = 1; break;
	    case SN16: ssz = 2; break;
	    case SN32: ssz = 4; break;
	    default: throw(new IllegalArgumentException("sfmt " + sfmt));
	    }
	}

	public PCMClip size(int size) {
	    this.size = size;
	    return(this);
	}

	private int read(byte[] buf, int off, int len) {
	    if(eof)
		return(-1);
	    if(size >= 0)
		len = Math.min(len, size);
	    try {
		int ret = clip.read(buf, off, len);
		if(ret < 0) {
		    eof = true;
		    clip.close();
		    return(-1);
		}
		if((size >= 0) && ((size -= ret) <= 0)) {
		    eof = true;
		    clip.close();
		}
		return(ret);
	    } catch(IOException e) {
		eof = true;
		try {
		    clip.close();
		} catch(IOException e2) {}
		return(-1);
	    }
	}

	public int get(double[][] dst, int ns) {
	    int nch = dst.length;
	    double[] dec = new double[sch];
	    for(int sm = 0; sm < ns; sm++) {
		while(tail - head < ssz * sch) {
		    if(head > 0) {
			for(int i = 0; i < tail - head; i++)
			    dbuf[i] = dbuf[head + i];
			tail -= head;
			head = 0;
		    }
		    int ret = read(dbuf, tail, dbuf.length - tail);
		    if(ret < 0)
			return((sm > 0) ? sm : -1);
		    tail += ret;
		}
		for(int ch = 0; ch < sch; ch++) {
		    switch(sfmt) {
		    case UN8:
			dec[ch] = ((dbuf[head++] & 0xff) - 0x80) * 0x1.0p-7;
			break;
		    case SN8:
			dec[ch] = dbuf[head++] * 0x1.0p-7;
			break;
		    case SN16:
			dec[ch] = ((int)(short)((dbuf[head++] & 0xff) |
						((dbuf[head++] & 0xff) << 8)))
			    * 0x1.0p-15;
			break;
		    case SN32:
			dec[ch] = ((dbuf[head++] & 0xff) |
				   ((dbuf[head++] & 0xff) << 8) |
				   ((dbuf[head++] & 0xff) << 16) |
				   ((dbuf[head++] & 0xff) << 24))
			    * 0x1.0p-31;
			break;
		    }
		}
		for(int ch = 0; ch < nch; ch++)
		    dst[ch][sm] = dec[ch % sch];
	    }
	    return(ns);
	}

	public static CS fromwav(InputStream clip) throws IOException {
	    if(s32(clip) != 0x46464952)
		throw(new IOException("Not a WAVE file (non-RIFF header)"));
	    int tsz = s32(clip);
	    if(s32(clip) != 0x45564157)
		throw(new IOException("Not a WAVE file (non-WAVE format)"));

	    int nch = -1, rate = -1, fmt = -1;
	    while(true) {
		int id, sz;
		try {
		    id = s32(clip);
		    sz = s32(clip);
		} catch(EOFException e) {
		    throw(new IOException("Malformed wave file (no data chunk)"));
		}
		if(id == 0x20746d66) {
		    if(sz < 16)
			throw(new IOException("Malformed wave file (too small fmt chunk)"));
		    int law = u16(clip);
		    if(law != 1)
			throw(new IOException("Not a PCM wave file (" + law + ")"));
		    nch = u16(clip);
		    rate = s32(clip);
		    int brate = s32(clip);
		    int blsz = u16(clip);
		    int bits = u16(clip);
		    for(int i = 16; i < sz; i++)
			u8(clip);
		    if((bits & 7) != 0)
			throw(new IOException("Malformed wave file (non-whole-byte sample size)"));
		    int ssz = bits >> 3;
		    if(((ssz * nch) != blsz) || ((ssz * nch * rate) != brate))
			throw(new IOException("Malformed wave file (non-matching sample sizes)"));
		    switch(ssz) {
		    case 1: fmt = UN8;  break;
		    case 2: fmt = SN16; break;
		    case 4: fmt = SN32; break;
		    default:
			throw(new IOException("Unexpected sample format: " + ssz));
		    }
		} else if(id == 0x61746164) {
		    if(nch < 0)
			throw(new IOException("Malformed wave file (no fmt chunk)"));
		    CS ret = new PCMClip(clip, nch, fmt).size(sz);
		    int orate = Math.round(Audio.fmt.getSampleRate());
		    if(rate != orate)
			ret = new Resampler(ret, rate, orate);
		    return(ret);
		} else {
		    byte[] disc = new byte[256];
		    for(int r = 0; r < sz;) {
			int rv = clip.read(disc, 0, Math.min(sz - r, disc.length));
			if(rv < 0)
			    throw(new EOFException("unexpected end-of-file"));
			r += rv;
		    }
		}
	    }
	}

	private static int u8(InputStream clip) throws IOException {
	    int ret = clip.read();
	    if(ret < 0)
		throw(new EOFException("unexpected end-of-file"));
	    return(ret & 0xff);
	}
	private static int u16(InputStream clip) throws IOException {
	    return(u8(clip) | (u8(clip) << 8));
	}
	private static int s32(InputStream clip) throws IOException {
	    return(u8(clip) | (u8(clip) << 8) | (u8(clip) << 16) | (u8(clip) << 24));
	}
    }

    public static class VorbisClip implements CS {
	public final VorbisStream clip;
	private float[][] data = new float[1][0];
	private int dp = 0;

	public VorbisClip(VorbisStream clip) {
	    this.clip = clip;
	}

	public VorbisClip(InputStream bs) throws IOException {
	    this(new VorbisStream(bs));
	}

	public int get(double[][] dst, int ns) {
	    int nch = dst.length;
	    if(data == null)
		return(-1);
	    for(int sm = 0; sm < ns; sm++) {
		while(dp >= data[0].length) {
		    try {
			if((data = clip.decode()) == null)
			    return((sm > 0)?sm:-1);
		    } catch(IOException e) {
			return(-1);
		    }
		    dp = 0;
		}
		for(int ch = 0; ch < nch; ch++)
		    dst[ch][sm] = data[ch % clip.chn][dp];
		dp++;
	    }
	    return(ns);
	}
    }

    public static class VolAdjust implements CS {
	public final CS bk;
	public double vol = 1.0, bal = 0.0;
	private double[] cvol = {};

	public VolAdjust(CS bk, double vol) {
	    this.bk = bk;
	    this.vol = vol;
	}

	public VolAdjust(CS bk) {
	    this(bk, 1.0);
	}

	public int get(double[][] dst, int ns) {
	    int nch = dst.length;
	    int ret = bk.get(dst, ns);
	    if(ret < 0)
		return(ret);
	    if(cvol.length != nch)
		cvol = new double[nch];
	    for(int i = 0; i < cvol.length; i++)
		cvol[i] = vol;
	    if(bal < 0)
		cvol[1] *= 1.0 + bal;
	    if(bal > 0)
		cvol[0] *= 1.0 - bal;
	    for(int ch = 0; ch < nch; ch++) {
		for(int sm = 0; sm < ret; sm++)
		    dst[ch][sm] *= cvol[ch % cvol.length];
	    }
	    return(ret);
	}
    }

    public static class Resampler implements CS {
	public final CS bk;
	public double irate, orate;
	public double sp = 1.0;
	private double ack;
	private double[] lval = {0}, nval = {0};
	private double[][] data = {};
	private int dp = 0, dl = 0;

	public Resampler(CS bk, double irate, double orate) {
	    this.bk = bk;
	    this.irate = irate;
	    this.orate = orate;
	}

	public Resampler(CS bk, double irate) {
	    this(bk, irate, fmt.getSampleRate());
	}

	public Resampler(CS bk) {
	    this(bk, fmt.getSampleRate());
	}

	public int get(double[][] dst, int ns) {
	    int nch = dst.length;
	    if(nval.length != nch) {
		nval = new double[nch];
		lval = new double[nch];
	    }
	    if(data.length != nch)
		data = new double[nch][512];
	    double esp = sp * irate / orate;
	    for(int sm = 0; sm < ns; sm++) {
		ack += esp;
		while(ack >= 1.0) {
		    while(dp >= dl) {
			if((dl = bk.get(data, 512)) < 0)
			    return( (sm > 0) ? sm : -1);
			dp = 0;
		    }
		    for(int ch = 0; ch < nch; ch++) {
			lval[ch] = nval[ch];
			nval[ch] = data[ch][dp];
		    }
		    dp++;
		    ack -= 1.0;
		}
		for(int ch = 0; ch < nch; ch++)
		    dst[ch][sm] = (lval[ch] * (1.0 - ack)) + (nval[ch] * ack);
	    }
	    return(ns);
	}

	public Resampler sp(double sp) {this.sp = sp; return(this);}
    }

    public static class Monitor implements CS {
	public final CS bk;
	public boolean eof = false;

	public Monitor(CS bk) {
	    this.bk = bk;
	}

	public int get(double[][] dst, int ns) {
	    int ret = bk.get(dst, ns);
	    if((ret < 0) && !eof) {
		eof = true;
		eof();
	    }
	    return(ret);
	}

	protected void eof() {
	    synchronized(this) {
		notifyAll();
	    }
	}

	public void finwait() throws InterruptedException {
	    synchronized(this) {
		while(!eof) {
		    wait();
		}
	    }
	}
    }

    public static abstract class Repeater implements CS {
	private CS cur = null;

	public int get(double[][] buf, int ns) {
	    while(true) {
		if(cur == null) {
		    if((cur = cons()) == null)
			return(-1);
		}
		int ret = cur.get(buf, ns);
		if(ret >= 0)
		    return(ret);
		cur = null;
	    }
	}

	protected abstract CS cons();
    }

    public static class LDump implements CS {
	public final CS bk;
	private double val = 0.0;
	private int n = 0, iv;

	public LDump(CS bk, int iv) {
	    this.bk = bk;
	    this.iv = iv;
	}

	public LDump(CS bk) {
	    this(bk, 44100);
	}

	public int get(double[][] buf, int ns) {
	    int nch = buf.length;
	    int ret = bk.get(buf, ns);
	    if(ret < 0) {
		if(n > 0)
		    System.err.println(val / n);
	    } else {
		for(int ch = 0; ch < nch; ch++) {
		    for(int sm = 0; sm < ret; sm++) {
			val += Math.abs(buf[ch][sm]);
			if(++n >= 44100) {
			    System.err.print((val / n) + " ");
			    val = 0;
			    n = 0;
			}
		    }
		}
	    }
	    return(ret);
	}
    }

    private static class Player extends HackThread {
	private final CS stream;
	private final int nch;
	private volatile boolean reopen = false;

	Player(CS stream) {
	    super("Haven audio player");
	    this.stream = stream;
	    nch = fmt.getChannels();
	    setDaemon(true);
	}

	private int fillbuf(byte[] dst, int off, int len) {
	    int ns = len / (2 * nch);
	    double[][] val = new double[nch][ns];
	    int left = ns, wr = 0;
	    while(left > 0) {
		int ret = stream.get(val, left);
		if(ret <= 0)
		    return((wr > 0)?wr:-1);
		for(int i = 0; i < ret; i++) {
		    for(int o = 0; o < nch; o++) {
			int iv = (int)(val[o][i] * volume * 32767.0);
			if(iv < 0) {
			    if(iv < -32768)
				iv = -32768;
			    iv += 65536;
			} else {
			    if(iv > 32767)
				iv = 32767;
			}
			dst[off++] = (byte)(iv & 0xff);
			dst[off++] = (byte)((iv & 0xff00) >> 8);
			wr += 2;
		    }
		}
		left -= ret;
	    }
	    return(wr);
	}

	static SourceDataLine getline() throws LineUnavailableException {
	    javax.sound.sampled.Mixer mixer;
	    String spec = outname.get();
	    if(spec.equals("")) {
		mixer = AudioSystem.getMixer(null);
	    } else {
		javax.sound.sampled.Mixer.Info f = null, f2 = null;
		int fs = 0;
		for(javax.sound.sampled.Mixer.Info info : AudioSystem.getMixerInfo()) {
		    String nm = info.getName();
		    int s = 0;
		    if(nm.equals(spec))
			s = 3;
		    else if(nm.equalsIgnoreCase(spec))
			s = 2;
		    else if(nm.toLowerCase().indexOf(spec.toLowerCase()) >= 0)
			s = 1;
		    if(s > fs) {
			f = info;
			f2 = null;
			fs = s;
		    } else if((s > 0) && (s == fs)) {
			f2 = info;
		    }
		}
		if(f == null)
		    throw(new LineUnavailableException(String.format("no mixer found by name: %s", spec)));
		else if(f2 != null)
		    throw(new LineUnavailableException(String.format("multiple mixers found by name `%s': %s and %s", spec, f.getName(), f2.getName())));
		mixer = AudioSystem.getMixer(f);
	    }
	    return((SourceDataLine)mixer.getLine(new DataLine.Info(SourceDataLine.class, fmt)));
	}

	public void run() {
	    SourceDataLine line = null;
	    try {
		while(true) {
		    synchronized(this) {
			reopen = false;
			this.notifyAll();
		    }
		    try {
			line = getline();
			line.open(fmt, bufsize);
			line.start();
		    } catch(Exception e) {
			new Warning(e, "could not open audio output").issue();
			return;
		    }
		    byte[] buf = new byte[bufsize / 2];
		    while(true) {
			if(Thread.interrupted())
			    throw(new InterruptedException());
			int ret = fillbuf(buf, 0, buf.length);
			if(ret < 0)
			    return;
			for(int off = 0; off < ret; off += line.write(buf, off, ret - off));
			if(reopen)
			    break;
		    }
		    line.close();
		    line = null;
		}
	    } catch(InterruptedException e) {
	    } finally {
		synchronized(Audio.class) {
		    player = null;
		}
		if(line != null)
		    line.close();
	    }
	}

	void reopen(boolean async) {
	    try {
		synchronized(this) {
		    reopen = true;
		    while(!async && reopen && isAlive())
			this.wait();
		}
	    } catch(InterruptedException e) {
		Thread.currentThread().interrupt();
	    }
	}
    }

    private static Player ckpl(boolean creat) {
	synchronized(Audio.class) {
	    if(enabled) {
		if(player == null) {
		    if(creat) {
			player = new Player(new Mixer(true));
			player.start();
		    } else {
			return(null);
		    }
		}
		return(player);
	    } else {
		return(null);
	    }
	}
    }

    public static void play(CS clip) {
	if(clip == null)
	    throw(new NullPointerException());
	Player pl = ckpl(true);
	if(pl != null)
	    ((Mixer)pl.stream).add(clip);
    }

    public static void stop(CS clip) {
	Player pl = ckpl(false);;
	if(pl != null)
	    ((Mixer)pl.stream).stop(clip);
    }

    private static Map<Resource, Clip> resclips = new HashMap<>();
    public static Clip resclip(Resource res) {
	Collection<Clip> clips = res.layers(Audio.clip, null);
	int sz = clips.size();
	if(sz == 0)
	    throw(new Resource.NoSuchLayerException("no audio clips in " + res.name));
	if(sz == 1)
	    return(clips.iterator().next());
	synchronized(resclips) {
	    Clip ret = resclips.get(res);
	    if(ret != null)
		return(ret);
	    List<Clip> clipl = new ArrayList<>(clips);
	    if(clipl.size() == 2) {
		ret = new Clip() {
			public CS stream() {
			    return(clipl.get((int)(Math.random() * 2)).stream());
			}
		    };
	    } else {
		ret = new Clip() {
			int last = -1;

			public CS stream() {
			    int c;
			    if(last < 0) {
				c = (int)(Math.random() * clipl.size());
			    } else {
				c = (int)(Math.random() * (clipl.size() - 1));
				if(c >= last)
				    c++;
			    }
			    return(clipl.get(last = c).stream());
			}
		    };
	    }
	    resclips.put(res, ret);
	    return(ret);
	}
    }

    public static CS fromres(Resource res) {
	return(resclip(res).stream());
    }

    public static void play(Resource res) {
	play(fromres(res));
    }

    public static int bufsize() {
	return(bufsize / fmt.getFrameSize());
    }

    public static void bufsize(int nsz, boolean async) {
	bufsize = nsz * fmt.getFrameSize();
	Player pl = ckpl(false);
	if(pl != null)
	    pl.reopen(async);
	Utils.setprefi("audiobuf", nsz);
    }

    public static void main(String[] args) throws Exception {
	if(args[0].equals("play")) {
	    Collection<Monitor> clips = new LinkedList<Monitor>();
	    for(int i = 1; i < args.length; i++) {
		if(args[i].equals("-b")) {
		    bufsize = Integer.parseInt(args[++i]);
		} else {
		    Monitor c = new Monitor(PCMClip.fromwav(Files.newInputStream(Utils.path(args[i]))));
		    clips.add(c);
		}
	    }
	    for(Monitor c : clips)
		play(c);
	    for(Monitor c : clips)
		c.finwait();
	} else if(args[0].equals("outputs")) {
	    for(javax.sound.sampled.Mixer.Info m : AudioSystem.getMixerInfo()) {
		System.out.printf("%s\t%s (%s %s)\n", m.getName(), m.getDescription(), m.getVendor(), m.getVersion());
	    }
	}
    }

    static {
	Console.setscmd("sfx", new Console.Command() {
		public void run(Console cons, String[] args) {
		    play(Loading.waitfor(Resource.remote().load(args[1])));
		}
	    });
	Console.setscmd("sfxvol", new Console.Command() {
		public void run(Console cons, String[] args) {
		    setvolume(Double.parseDouble(args[1]));
		}
	    });
	Console.setscmd("audiobuf", new Console.Command() {
		public void run(Console cons, String[] args) throws Exception {
		    int nsz = Integer.parseInt(args[1]);
		    if(nsz > fmt.getSampleRate())
			throw(new Exception("Rejecting buffer longer than 1 second"));
		    bufsize(nsz, false);
		}
	    });
    }
}
