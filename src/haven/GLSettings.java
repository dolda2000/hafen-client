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
import java.lang.reflect.*;
import javax.media.opengl.*;

/*
 * XXX: Hmmpf. This whole thing seems very overly complex, but I
 * really want to avoid duplicating the validation checks in every
 * place that changes a value.
 */
public class GLSettings implements java.io.Serializable {
    public final GLConfig cfg;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

    private GLSettings(GLConfig cfg) {
	this.cfg = cfg;
    }

    public static class SettingException extends RuntimeException {
	public SettingException(String msg) {
	    super(msg);
	}
    }

    public abstract class Setting<T> implements java.io.Serializable {
	public final String nm;
	public T val;

	public Setting(String nm) {
	    this.nm = nm.intern();
	    settings.add(this);
	}

	public abstract void set(String val);
	public abstract void validate(T val);
	public abstract T defval();
	public void set(T val) {
	    validate(val);
	    this.val = val;
	}
    }

    public abstract class BoolSetting extends Setting<Boolean> {
	public BoolSetting(String nm) {super(nm);}

	public void set(String val) {
	    boolean bval;
	    try {
		bval = Utils.parsebool(val);
	    } catch(IllegalArgumentException e) {
		throw(new SettingException("Not a boolean value: " + e));
	    }
	    set(bval);
	}
    }

    public abstract class EnumSetting<E extends Enum<E>> extends Setting<E> {
	private final Class<E> real;

	public EnumSetting(String nm, Class<E> real) {
	    super(nm);
	    this.real = real;
	}

	public void set(String val) {
	    E eval;
	    try {
		eval = Enum.valueOf(real, val.toUpperCase());
	    } catch(IllegalArgumentException e) {
		throw(new SettingException("No such setting: " + e));
	    }
	    set(eval);
	}
    }

    public final BoolSetting usedl = new BoolSetting("usedl") {
	    public Boolean defval() {return(true);}
	    public void validate(Boolean val) {}
	};
    public final BoolSetting fsaa = new BoolSetting("fsaa") {
	    public Boolean defval() {return(false);}
	    public void validate(Boolean val) {
		if(val && !cfg.havefsaa())
		    throw(new SettingException("FSAA is not supported."));
	    }
	};
    public final BoolSetting shuse = new BoolSetting("shuse") {
	    public Boolean defval() {return(cfg.haveglsl());}
	    public void validate(Boolean val) {
		if(val && !cfg.haveglsl())
		    throw(new SettingException("GLSL is not supported."));
	    }
	};

    public final BoolSetting flight = new BoolSetting("flight") {
	    public Boolean defval() {return(false);}
	    public void validate(Boolean val) {
		if(val) {
		    if(!cfg.haveglsl()) throw(new SettingException("Per-pixel lighting requires a shader-compatible video card."));
		    if(!shuse.val) throw(new SettingException("Per-pixel lighting requires shader usage."));
		}
	    }
	};

    public final BoolSetting cel = new BoolSetting("cel") {
	    public Boolean defval() {return(false);}
	    public void validate(Boolean val) {
		if(val) {
		    if(!flight.val) throw(new SettingException("Cel-shading requires per-fragment lighting."));
		}
	    }
	};

    public final BoolSetting lshadow = new BoolSetting("sdw") {
	    public Boolean defval() {return(false);}
	    public void validate(Boolean val) {
		if(val) {
		    if(!flight.val) throw(new SettingException("Shadowed lighting requires per-fragment lighting."));
		    if(!cfg.havefbo()) throw(new SettingException("Shadowed lighting requires a video card supporting framebuffers."));
		}
	    }
	};

    public Iterable<Setting<?>> settings() {
	return(settings);
    }

    public Object savedata() {
	Map<String, Object> ret = new HashMap<String, Object>();
	for(Setting<?> s : settings)
	    ret.put(s.nm, s.val);
	return(ret);
    }

    public void save() {
	Utils.setprefb("glconf", Utils.serialize(savedata()));
    }

    private static <T> void iAmRunningOutOfNamesToInsultJavaWith(Setting<T> s) {
	s.val = s.defval();
    }

    public static GLSettings defconf(GLConfig cfg) {
	GLSettings gs = new GLSettings(cfg);
	for(Setting<?> s : gs.settings)
	    iAmRunningOutOfNamesToInsultJavaWith(s);
	return(gs);
    }

    @SuppressWarnings("unchecked")
    private static <T> void iExistOnlyToIntroduceATypeVariableSinceJavaSucks(Setting<T> s, Object val) {
	s.set((T)val);
    }

    public static GLSettings load(Object data, GLConfig cfg, boolean failsafe) {
	GLSettings gs = defconf(cfg);
	Map<?, ?> dat = (Map)data;
	for(Setting<?> s : gs.settings) {
	    if(dat.containsKey(s.nm)) {
		try {
		    iExistOnlyToIntroduceATypeVariableSinceJavaSucks(s, dat.get(s.nm));
		} catch(SettingException e) {
		    if(!failsafe)
			throw(e);
		}
	    }
	}
	return(gs);
    }

    public static GLSettings load(GLConfig cfg, boolean failsafe) {
	byte[] data = Utils.getprefb("glconf", null);
	if(data == null) {
	    return(defconf(cfg));
	} else {
	    Object dat;
	    try {
		dat = Utils.deserialize(data);
	    } catch(Exception e) {
		dat = null;
	    }
	    if(dat == null)
		return(defconf(cfg));
	    return(load(dat, cfg, failsafe));
	}
    }
}
