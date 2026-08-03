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

package haven.iosys.audio.ffi;

import java.util.*;
import java.nio.*;
import haven.*;
import haven.iosys.*;
import haven.iosys.audio.*;
import haven.ffi.misc.Alsa;
import haven.ffi.posix.LibC;
import haven.Audio.*;
import static haven.iosys.audio.AudioSystem.*;
import static haven.ffi.misc.Alsa.*;

@AudioSystem.Available(name = "alsa")
public class ASound implements AudioSystem {
    private final Alsa alsa;

    private static Providers.Factory<ASound> factory = new Providers.Factory<ASound>() {
	private ASound instance = null;

	public ASound open(String... args) {
	    synchronized(this) {
		if(instance == null)
		    instance = new ASound();
	    }
	    return(instance);
	}

	public boolean experimental() {return(true);}
    };
    public static Providers.Factory<ASound> get() {
	return(factory);
    }

    public ASound() {
	try {
	    alsa = Alsa.get();
	    /* Basic availability test */
	    sinkline(Utils.map().put(SPEC_RATE, 44100).map());
	} catch(Exception e) {
	    throw(new Unavailable("ALSA library not avilable", e));
	}
    }

    public class AlsaPlayer extends HackThread implements Player {
	private final Pcm pcm;
	private final CS stream;
	private final Ratio rate;
	private final int fsz, nch, period, bufsz;
	private volatile boolean stop = false;

	AlsaPlayer(Pcm pcm, HwParams hwc, SwParams swc, CS stream) {
	    super("ALSA audio player");
	    this.pcm = pcm;
	    this.stream = stream;
	    this.rate = hwc.rate_numden();
	    this.nch = hwc.channels();
	    this.fsz = this.nch * 2;
	    this.period = (int)hwc.period_size();
	    this.bufsz = (int)hwc.buffer_size();
	    setPriority(Thread.MAX_PRIORITY);
	    setDaemon(true);
	    start();
	    // Debug.dump(period, bufsz, hwc.rate_numden(), hwc.period_size(), hwc.periods(), hwc.buffer_size());
	}

	private int fillbuf(ByteBuffer buf) {
	    int nf = buf.remaining() / fsz;
	    double[][] val = new double[nch][nf];
	    int ret = stream.get(val, nf);
	    if(ret <= 0)
		return(-1);
	    for(int i = 0; i < ret; i++) {
		for(int o = 0; o < nch; o++) {
		    short iv = (short)(Utils.clip(val[o][i], -1, 1) * 32767.0);
		    buf.putShort(iv);
		}
	    }
	    return(ret);
	}

	private boolean xrwarn = false;
	private double fxrun = -1;
	private int xruns = 0;
	public void run() {
	    try {
		ByteBuffer buf = ByteBuffer.allocateDirect(bufsz * fsz);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		while(true) {
		    if(Thread.interrupted())
			throw(new InterruptedException());
		    try {
			int avail = (int)alsa.snd_pcm_avail_update(pcm);
			if(avail > period) {
			    buf.position(0).limit(Math.min(avail, bufsz) * fsz);
			    int ret = fillbuf(buf);
			    if(ret < 0)
				return;
			    buf.flip();
			    while(buf.remaining() > 0) {
				int wf = alsa.snd_pcm_writei(pcm, buf, buf.remaining() / fsz);
				buf.position(buf.position() + (wf * fsz));
			    }
			}
			alsa.snd_pcm_wait(pcm, 100);
		    } catch(AlsaException e) {
			if(e.code != -LibC.EPIPE)
			    throw(e);
			alsa.snd_pcm_prepare(pcm);
			if(fxrun < 0)
			    fxrun = Utils.rtime();
			xruns++;
		    }
		    if(stop)
			break;
		    if((fxrun >= 0) && (Utils.rtime() - fxrun > 2)) {
			if(xruns > 1) {
			    if(!xrwarn)
				xrwarn = true;
			    else
				Warning.warn(xruns + " alsa underruns detected; audio buffer size too small?");
			}
			fxrun = -1;
			xruns = 0;
		    }
		}
	    } catch(InterruptedException e) {
	    } finally {
		pcm.close();
	    }
	}

	public void stop(boolean async) {
	    try {
		synchronized(this) {
		    stop = true;
		    while(!async && isAlive())
			this.join();
		}
	    } catch(InterruptedException e) {
		Thread.currentThread().interrupt();
	    }
	}

	public String toString() {
	    PcmInfo info = alsa.snd_pcm_info(pcm);
	    return(String.format("ALSA player, \"%s\" (%d,%d), %s Hz, %d ch, buffer %d/%d",
				 alsa.snd_pcm_name(pcm), info.device(), info.subdevice(),
				 rate, nch, bufsz, period));
	}
    }

    public class AlsaSink implements SinkLine {
	public final String name;
	public final int defbuf;
	public final HwParams hw;

	public AlsaSink(String name, HwParams hw, int defbuf) {
	    this.name = name;
	    this.defbuf = defbuf;
	    this.hw = hw;
	}

	public Player open(CS stream, int bufsize) {
	    Pcm pcm = null;
	    try {
		pcm = alsa.snd_pcm_open(name, Alsa.SND_PCM_STREAM_PLAYBACK, Alsa.SND_PCM_NONBLOCK);
		HwParams hwc = hw.copy();
		hwc.buffer_size_near(pcm, bufsize);
		alsa.snd_pcm_hw_params(pcm, hwc);
		SwParams swc = alsa.snd_pcm_sw_params().current(pcm);
		swc.start_threshold(pcm, 1);
		alsa.snd_pcm_sw_params(pcm, swc);
		alsa.snd_pcm_prepare(pcm);
		Player ret = new AlsaPlayer(pcm, hwc, swc, stream);
		pcm = null;
		return(ret);
	    } catch(AlsaException e) {
		throw(new Unavailable(e));
	    } finally {
		if(pcm != null)
		    pcm.close();
	    }
	}

	public Player open(CS stream) {
	    return(open(stream, defbuf));
	}
    }

    public static class AlsaDevice implements SinkDevice {
	public final String name;
	public final String desc;

	public AlsaDevice(String name, String desc) {
	    this.name = name;
	    this.desc = desc;
	}

	public String id() {return(name);}
	public String desc() {return(desc);}

	public String toString() {
	    return(String.format("%s: %s", id(), desc()));
	}
    }

    public List<SinkDevice> sinkdevs() {
	DeviceHints hints = alsa.snd_device_name_hint(-1, "pcm");
	List<SinkDevice> ret = new ArrayList<>();
	for(int i = 0; i < hints.size(); i++) {
	    if(Utils.eq(hints.get(i, "IOID"), "Output"))
		ret.add(new AlsaDevice(hints.get(i, "NAME"), hints.get(i, "DESC")));
	}
	return(ret);
    }

    public SinkLine sinkline(Map<?, ?> spec) {
	int rate = intspec(spec, SPEC_RATE, null);
	int ch = intspec(spec, SPEC_CHANNELS, 2);
	int buf = intspec(spec, SPEC_BUFSIZE, 1024);
	Object outspec = spec.get(SPEC_SINKDEV);
	String pcmname;
	if(outspec == null) {
	    pcmname = "default";
	} else if(outspec instanceof AlsaDevice) {
	    pcmname = ((AlsaDevice)outspec).name;
	} else if(outspec instanceof Number) {
	    pcmname = sinkdevs().get(((Number)outspec).intValue()).id();
	} else if(outspec instanceof String) {
	    pcmname = (String)outspec;
	} else {
	    throw(new IllegalArgumentException(SPEC_SINKDEV + ": " + outspec));
	}
	HwParams fconf;
	try(Pcm pcm = alsa.snd_pcm_open(pcmname, Alsa.SND_PCM_STREAM_PLAYBACK, Alsa.SND_PCM_NONBLOCK)) {
	    HwParams conf = alsa.snd_pcm_hw_params().any(pcm);
	    conf.access(pcm, Alsa.SND_PCM_ACCESS_RW_INTERLEAVED);
	    conf.format(pcm, Alsa.SND_PCM_FORMAT_S16_LE);
	    conf.channels(pcm, ch);
	    conf.rate_near(pcm, rate, 0);
	    fconf = conf.copy();
	    conf.buffer_size_near(pcm, buf);
	    alsa.snd_pcm_hw_params(pcm, conf);
	    return(new AlsaSink(pcmname, fconf, buf));
	} catch(AlsaException e) {
	    throw(new Unavailable(e));
	}
    }
}
