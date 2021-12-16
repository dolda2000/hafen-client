package dolda.coe;

import java.util.*;
import java.lang.reflect.*;

public interface ObjectData<T> {
    static final Map<Class<?>, ObjectData<?>> encoders = new HashMap<>();
    static final Map<Class, List<ObjectData>> scache = new HashMap<>();

    public void encode(T obj, Map<Symbol, Object> buf);

    public static <T> void register(Class<T> type, ObjectData<? super T> encoder) {
	synchronized(encoders) {
	    encoders.put(type, encoder);
	    scache.clear();
	}
    }

    static List<ObjectData> getstructure(Class<?> cl) {
	List<Class<?>> co = new ArrayList<>();
	for(Class<?> p = cl; p != null; p = p.getSuperclass())
	    co.add(p);
	Collections.reverse(co);
	List<ObjectData> eo = new ArrayList<>();
	for(Class<?> p : co) {
	    ObjectData pe = encoders.get(p);
	    if(pe != null)
		eo.add(pe);
	    for(Class<?> ifc : p.getInterfaces()) {
		ObjectData ie = encoders.get(ifc);
		if(!eo.contains(ie))
		    eo.add(ie);
	    }
	}
	return(eo);
    }

    static List<ObjectData> structure(Class<?> cl) {
	synchronized(encoders) {
	    List<ObjectData> ret = scache.get(cl);
	    if(ret == null)
		scache.put(cl, ret = getstructure(cl));
	    return(ret);
	}
    }

    @SuppressWarnings("unchecked")
    public static Map<Symbol, Object> encode(Object obj) {
	Map<Symbol, Object> ret = new HashMap<>();
	for(ObjectData enc : structure(obj.getClass())) {
	    enc.encode(obj, ret);
	}
	return(ret);
    }

    static Symbol s(String nm) {return(Symbol.get(nm));}

    static Object init() {
	register(Throwable.class, (t, buf) -> {
		buf.put(s("traceback"), t.getStackTrace());
		try {
		    buf.put(s("message"), t.getMessage());
		} catch(Exception e) {
		    buf.put(s("message-error"), e);
		}
		buf.put(s("cause"), t.getCause());
		buf.put(s("suppressed"), t.getSuppressed());
	    });

	register(StackTraceElement.class, (el, buf) -> {
		buf.put(s("file"), el.getFileName());
		buf.put(s("line"), el.getLineNumber());
		buf.put(s("class"), el.getClassName());
		buf.put(s("method"), el.getMethodName());
		buf.put(s("native"), el.isNativeMethod());
	    });

	register(java.io.Serializable.class, (obj, buf) -> {
		for(Field f : obj.getClass().getFields()) {
		    if((f.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0) {
			try {
			    buf.put(Symbol.get("java/field", f.getName()), f.get(obj));
			} catch(Exception e) {
			}
		    }
		}
	    });
	return(null);
    }

    static final Object dummy = init();
}
