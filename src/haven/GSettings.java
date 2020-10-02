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

import haven.render.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;

public class GSettings extends State implements Serializable {
    public static final Slot<GSettings> slot = new Slot<>(Slot.Type.SYS, GSettings.class);
    private static final List<Field> settings;

    static {
	List<Field> buf = new ArrayList<>();
	for(Field f : GSettings.class.getFields()) {
	    if(Setting.class.isAssignableFrom(f.getType()))
		buf.add(f);
	}
	settings = buf;
    }

    private GSettings() {
    }

    public GSettings(GSettings from) {
	try {
	    for(Field f : settings)
		f.set(this, f.get(from));
	} catch(IllegalAccessException e) {
	    throw(new AssertionError(e));
	}
    }

    public static class SettingException extends RuntimeException {
	public SettingException(String msg) {
	    super(msg);
	}
    }

    public abstract class Setting<T> implements Serializable, Cloneable {
	public final String nm;
	public T val;

	public Setting(String nm) {
	    this.nm = nm.intern();
	}

	public abstract T parse(String val);
	public void validate(Environment env, T val) {}
	public abstract T defval();

	@SuppressWarnings("unchecked")
	public Setting<T> clone() {
	    try {
		return((Setting<T>)super.clone());
	    } catch(CloneNotSupportedException e) {
		throw(new AssertionError(e));
	    }
	}
    }

    public abstract class BoolSetting extends Setting<Boolean> {
	public BoolSetting(String nm) {super(nm);}

	public Boolean parse(String val) {
	    try {
		return(Utils.parsebool(val));
	    } catch(IllegalArgumentException e) {
		throw(new SettingException("Not a boolean value: " + e));
	    }
	}
    }

    public abstract class EnumSetting<E extends Enum<E>> extends Setting<E> {
	private final Class<E> real;

	public EnumSetting(String nm, Class<E> real) {
	    super(nm);
	    this.real = real;
	}

	public E parse(String val) {
	    E f = null;
	    val = val.toUpperCase();
	    for(E e : EnumSet.allOf(real)) {
		if(e.name().toUpperCase().startsWith(val)) {
		    if(f != null)
			throw(new SettingException("Multiple settings with this abbreviation: " + f.name() + ", " + e.name()));
		    f = e;
		}
	    }
	    if(f == null)
		throw(new SettingException("No such setting: " + val));
	    return(f);
	}
    }

    public abstract class IntSetting extends Setting<Integer> {
	public IntSetting(String nm) {super(nm);}

	public Integer parse(String val) {
	    try {
		return(Integer.parseInt(val));
	    } catch(NumberFormatException e) {
		throw(new SettingException("Not an integer value: " + val));
	    }
	}
    }

    public abstract class FloatSetting extends Setting<Float> {
	public FloatSetting(String nm) {super(nm);}

	public Float parse(String val) {
	    try {
		return(Float.parseFloat(val));
	    } catch(NumberFormatException e) {
		throw(new SettingException("Not a floating-point value: " + val));
	    }
	}
    }

    public BoolSetting lshadow = new BoolSetting("sdw") {
	    public Boolean defval() {return(true);}
	};
    public IntSetting shadowres = new IntSetting("sres") {
	    public Integer defval() {return(0);}
	};
    public BoolSetting vsync = new BoolSetting("vsync") {
	    public Boolean defval() {return(true);}
	};
    public abstract class HertzSetting extends FloatSetting {
	public HertzSetting(String nm) {super(nm);}

	public Float parse(String val) {
	    Float ret = super.parse(val);
	    if(ret == 0f)
		return(Float.POSITIVE_INFINITY);
	    return(ret);
	}

	public void validate(Environment env, Float val) {
	    if(!Float.isFinite(val) && (val != Float.POSITIVE_INFINITY))
		throw(new SettingException("Not a numeric framerate"));
	    if(val <= 0)
		throw(new SettingException("Not a positive framerate"));
	}
    }
    public FloatSetting hz = new HertzSetting("hz") {
	    public Float defval() {return(Float.POSITIVE_INFINITY);}
	};
    public FloatSetting bghz = new HertzSetting("bghz") {
	    public Float defval() {return(5f);}
	};
    public FloatSetting rscale = new FloatSetting("rscale") {
	    public Float defval() {return(1.0f);}
	    public void validate(Environment env, Float val) {
		if(!Float.isFinite(val))
		    throw(new SettingException("Not a finite render-scale"));
		if(val <= 0)
		    throw(new SettingException("Not a positive render-scale"));
	    }
	};

    public EnumSetting<JOGLPanel.SyncMode> syncmode = new EnumSetting<JOGLPanel.SyncMode>("syncmode", JOGLPanel.SyncMode.class) {
	public JOGLPanel.SyncMode defval() {
	    return(JOGLPanel.SyncMode.FRAME);
	}
    };

    public Setting<?> find(String name) {
	try {
	    for(Field f : settings) {
		Setting<?> set = ((Setting<?>)f.get(this));
		if(set.nm.equals(name))
		    return(set);
	    }
	    return(null);
	} catch(IllegalAccessException e) {
	    throw(new AssertionError(e));
	}
    }

    @SuppressWarnings("unchecked")
    private <T> Setting<T> update(Setting<T> set, T val) {
	Setting<T> ret = set.clone();
	ret.val = val;
	return(ret);
    }

    private <T> GSettings supdate(Setting<T> set, T val) {
	if(Utils.eq(set.val, val))
	    return(this);
	GSettings ret = new GSettings(this);
	for(Field f : settings) {
	    try {
		if(f.get(this) == set) {
		    f.set(ret, update(set, val));
		    break;
		}
	    } catch(IllegalAccessException e) {
		throw(new AssertionError(e));
	    }
	}
	return(ret);
    }

    private static <T> void validate0(Environment env, Setting<T> set) {
	set.validate(env, set.val);
    }
    public GSettings validate(Environment env) {
	for(Field f : settings) {
	    try {
		validate0(env, (Setting<?>)f.get(this));
	    } catch(IllegalAccessException e) {
		throw(new AssertionError(e));
	    }
	}
	return(this);
    }

    public <T> GSettings update(Environment env, Setting<T> set, T val) {
	GSettings ret = supdate(set, val);
	ret.validate(env);
	return(ret);
    }

    private static <T> void setdef(Setting<T> set) {
	set.val = set.defval();
    }
    public static GSettings defaults() {
	GSettings ret = new GSettings();
	for(Field f : settings) {
	    try {
		setdef((Setting<?>)f.get(ret));
	    } catch(IllegalAccessException e) {
		throw(new AssertionError(e));
	    }
	}
	return(ret);
    }

    private Object savedata() {
	try {
	    Map<String, Object> ret = new HashMap<String, Object>();
	    for(Field f : settings) {
		Setting<?> s = (Setting<?>)f.get(this);
		ret.put(s.nm, s.val);
	    }
	    return(ret);
	} catch(IllegalAccessException e) {
	    throw(new AssertionError(e));
	}
    }

    public void save() {
	Utils.setprefb("gconf", Utils.serialize(savedata()));
    }

    @SuppressWarnings("unchecked")
    private <T> Setting<T> iExistOnlyToIntroduceATypeVariableSinceJavaSucks(Setting<T> s, Object val) {
	return(update(s, (T)val));
    }

    public static GSettings load(Object data, boolean failsafe) {
	GSettings gs = defaults();
	Map<?, ?> dat = (Map)data;
	try {
	    for(Field f : settings) {
		Setting<?> s = (Setting<?>)f.get(gs);
		if(dat.containsKey(s.nm)) {
		    try {
			f.set(gs, gs.iExistOnlyToIntroduceATypeVariableSinceJavaSucks(s, dat.get(s.nm)));
		    } catch(SettingException | ClassCastException e) {
			if(!failsafe)
			    throw(e);
		    }
		}
	    }
	} catch(IllegalAccessException e) {
	    throw(new AssertionError(e));
	}
	return(gs);
    }

    public static GSettings load(boolean failsafe) {
	byte[] data = Utils.getprefb("gconf", null);
	if(data == null) {
	    return(defaults());
	} else {
	    Object dat;
	    try {
		dat = Utils.deserialize(data);
	    } catch(Exception e) {
		dat = null;
	    }
	    if(dat == null)
		return(defaults());
	    return(load(dat, failsafe));
	}
    }

    public haven.render.sl.ShaderMacro shader() {return(null);}
    public void apply(Pipe p) {p.put(slot, this);}
}
