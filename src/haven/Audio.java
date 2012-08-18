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
import javax.sound.sampled.*;
import dolda.xiphutil.*;

public class Audio {
    public static boolean enabled = true;
    private static Player player;
    public static final AudioFormat fmt = new AudioFormat(44100, 16, 2, true, false);
    private static Collection<CS> ncl = new LinkedList<CS>();
    private static Object queuemon = new Object();
    private static Collection<Runnable> queue = new LinkedList<Runnable>();
    private static int bufsize = 32768;
    public static double volume = 1.0;
    
    static {
	volume = Double.parseDouble(Utils.getpref("sfxvol", "1.0"));
    }
    
    public static void setvolume(double volume) {
	Audio.volume = volume;
	Utils.setpref("sfxvol", Double.toString(volume));
    }

    public interface CS {
	public int get(double[][] sample);
    }
    
    public static class DataClip implements CS {
	public final int rate;
	public boolean eof;
	public double vol, sp;
	private InputStream clip;
	private final int trate;
	private int ack = 0;
	private final byte[] buf = new byte[256];
	private int dp = 0, dl = 0;

	public DataClip(InputStream clip, int rate, double vol, double sp) {
	    this.clip = clip;
	    this.rate = rate;
	    this.vol = vol;
	    this.sp = sp;
	    this.trate = (int)fmt.getSampleRate();
	}

	public DataClip(InputStream clip, double vol, double sp) {
	    this(clip, 44100, vol, sp);
	}
	
	public DataClip(InputStream clip) {
	    this(clip, 1.0, 1.0);
	}
	
	public void finwait() throws InterruptedException {
	    synchronized(this) {
		if(eof)
		    return;
		wait();
	    }
	}
	
	public int get(double[][] buf) {
	    if(eof)
		return(-1);
	    try {
		for(int off = 0; off < buf[0].length; off++) {
		    ack += rate * sp;
		    while(ack >= trate) {
			for(int i = 0; i < 2; i++) {
			    if(dl - dp < 2) {
				if(dl > dp) {
				    this.buf[0] = this.buf[dp];
				    dl = 1;
				} else {
				    dl = 0;
				}
				while(dl < 2) {
				    int ret = clip.read(this.buf, dl, this.buf.length - dl);
				    if(ret < 0) {
					synchronized(this) {
					    eof = true;
					    notifyAll();
					}
					return(off);
				    }
				    dl += ret;
				}
				dp = 0;
			    }
			    int b1 = this.buf[dp++] & 0xff;
			    int b2 = this.buf[dp++] & 0xff;
			    int v = b1 + (b2 << 8);
			    if(v >= 32768)
				v -= 65536;
			    buf[i][off] = ((double)v / 32768.0) * vol;
			}
			ack -= trate;
		    }
		}
		return(buf[0].length);
	    } catch(IOException e) {
		synchronized(this) {
		    eof = true;
		    notifyAll();
		}
		return(-1);
	    }
	}
    }
    
    public static double[][] pcmi2f(byte[] pcm, int ch) {
	if(pcm.length % (ch * 2) != 0)
	    throw(new IllegalArgumentException("Uneven samples in PCM data"));
	int sm = pcm.length / (ch * 2);
	double[][] ret = new double[ch][sm];
	int off = 0;
	for(int i = 0; i < sm; i++) {
	    for(int o = 0; o < ch; o++) {
		int b1 = pcm[off++] & 0xff;
		int b2 = pcm[off++] & 0xff;
		int v = b1 + (b2 << 8);
		if(v >= 32768)
		    v -= 65536;
		ret[o][i] = (double)v / 32768.0;
	    }
	}
	return(ret);
    }
	
    private static class Player extends HackThread {
	private Collection<CS> clips = new LinkedList<CS>();
	private int srate, nch = 2;
	
	Player() {
	    super("Haven audio player");
	    setDaemon(true);
	    srate = (int)fmt.getSampleRate();
	}
	
	private void fillbuf(byte[] dst, int off, int len) {
	    int ns = len / (2 * nch);
	    double[][] val = new double[nch][ns];
	    double[][] buf = new double[nch][ns];
	    synchronized(clips) {
		clip: for(Iterator<CS> i = clips.iterator(); i.hasNext();) {
		    int left = ns;
		    CS cs = i.next();
		    int boff = 0;
		    while(left > 0) {
			int ret = cs.get(buf);
			if(ret < 0) {
			    i.remove();
			    continue clip;
			}
			for(int ch = 0; ch < nch; ch++) {
			    for(int sm = 0; sm < ret; sm++)
				val[ch][sm + boff] += buf[ch][sm];
			}
			left -= ret;
		    }
		}
	    }
	    for(int i = 0; i < ns; i++) {
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
		}
	    }
	}

	public void stop(CS clip) {
	    synchronized(clips) {
		for(Iterator<CS> i = clips.iterator(); i.hasNext();) {
		    if(i.next() == clip) {
			i.remove();
			return;
		    }
		}
	    }
	}

	public void run() {
	    SourceDataLine line = null;
	    try {
		try {
		    line = (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, fmt));
		    line.open(fmt, bufsize);
		    line.start();
		} catch(Exception e) {
		    e.printStackTrace();
		    return;
		}
		byte[] buf = new byte[1024];
		while(true) {
		    if(Thread.interrupted())
			throw(new InterruptedException());
		    synchronized(queuemon) {
			Collection<Runnable> queue = Audio.queue;
			Audio.queue = new LinkedList<Runnable>();
			for(Runnable r : queue)
			    r.run();
		    }
		    synchronized(ncl) {
			synchronized(clips) {
			    for(CS cs : ncl)
				clips.add(cs);
			    ncl.clear();
			}
		    }
		    fillbuf(buf, 0, 1024);
		    for(int off = 0; off < buf.length; off += line.write(buf, off, buf.length - off));
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
    }

    private static synchronized void ckpl() {
	if(enabled) {
	    if(player == null) {
		player = new Player();
		player.start();
	    }
	} else {
	    ncl.clear();
	}
    }
    
    public static void play(CS clip) {
	synchronized(ncl) {
	    ncl.add(clip);
	}
	ckpl();
    }

    public static void stop(CS clip) {
	Player pl = player;
	if(pl != null)
	    pl.stop(clip);
    }
    
    public static void play(final InputStream clip, final double vol, final double sp) {
	play(new DataClip(clip, vol, sp));
    }

    public static void play(byte[] clip, double vol, double sp) {
	play(new DataClip(new ByteArrayInputStream(clip), vol, sp));
    }
    
    public static void play(byte[] clip) {
	play(clip, 1.0, 1.0);
    }
    
    public static void queue(Runnable d) {
	synchronized(queuemon) {
	    queue.add(d);
	}
	ckpl();
    }

    private static void playres(Resource res) {
	Collection<Resource.Audio> clips = res.layers(Resource.audio);
	int s = (int)(Math.random() * clips.size());
	Resource.Audio clip = null;
	for(Resource.Audio cp : clips) {
	    clip = cp;
	    if(--s < 0)
		break;
	}
	try {
	    play(new VorbisStream(new ByteArrayInputStream(clip.coded)).pcmstream(), 1.0, 1.0);
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static void play(final Resource clip) {
	queue(new Runnable() {
		public void run() {
		    if(clip.loading)
			queue.add(this);
		    else
			playres(clip);
		}
	    });
    }
    
    public static void play(final Indir<Resource> clip) {
	queue(new Runnable() {
		public void run() {
		    try {
			playres(clip.get());
		    } catch(Loading e) {
			queue.add(this);
		    }
		}
	    });
    }
    
    public static byte[] readclip(InputStream in) throws IOException {
	AudioInputStream cs;
	try {
	    cs = AudioSystem.getAudioInputStream(fmt, AudioSystem.getAudioInputStream(in));
	} catch(UnsupportedAudioFileException e) {
	    throw(new IOException("Unsupported audio encoding"));
	}
	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	byte[] bbuf = new byte[65536];
	while(true) {
	    int rv = cs.read(bbuf);
	    if(rv < 0)
		break;
	    buf.write(bbuf, 0, rv);
	}
	return(buf.toByteArray());
    }
    
    public static void main(String[] args) throws Exception {
	Collection<DataClip> clips = new LinkedList<DataClip>();
	for(int i = 0; i < args.length; i++) {
	    if(args[i].equals("-b")) {
		bufsize = Integer.parseInt(args[++i]);
	    } else {
		DataClip c = new DataClip(new FileInputStream(args[i]));
		clips.add(c);
	    }
	}
	for(DataClip c : clips)
	    play(c);
	for(DataClip c : clips)
	    c.finwait();
    }
    
    static {
	Console.setscmd("sfx", new Console.Command() {
		public void run(Console cons, String[] args) {
		    play(Resource.load(args[1]));
		}
	    });
	Console.setscmd("sfxvol", new Console.Command() {
		public void run(Console cons, String[] args) {
		    setvolume(Double.parseDouble(args[1]));
		}
	    });
    }
}
