package dolda.coe;

import java.util.*;
import java.lang.ref.*;

public class Symbol {
    private static final boolean[] isafe, qsafe;
    private static final Map<Symbol, Reference<Symbol>> interned = new WeakHashMap<>();
    public final String name, ns;

    static {
	isafe = new boolean[256];
	qsafe = new boolean[256];
	String is = "abcdefghijklmnopqrstuvwxyz" +
		    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
		    "$%^&*_=/.";
	String qs = "0123456789-+";
	for(int i = 0; i < is.length(); i++) {
	    int c = is.charAt(i);
	    isafe[c] = qsafe[c] = true;
	}
	for(int i = 0; i < qs.length(); i++) {
	    int c = qs.charAt(i);
	    qsafe[c] = true;
	}
    }

    private Symbol(String ns, String name) {
	if((ns == null) || (name == null))
	    throw(new NullPointerException());
	this.ns = ns.intern();
	this.name = name.intern();
    }

    public int hashCode() {
	return((ns.hashCode() * 31) + name.hashCode());
    }

    public boolean equals(Symbol that) {
	return((this.ns == that.ns) && (this.name == that.name));
    }

    public boolean equals(Object that) {
	return((that instanceof Symbol) && equals((Symbol)that));
    }

    public static String quote(String str) {
	if(str.length() == 0)
	    return("||");
	char c = str.charAt(0);
	safe: if((c >= 256) || isafe[c]) {
	    for(int i = 1; i < str.length(); i++) {
		c = str.charAt(i);
		if((c < 256) && !qsafe[c])
		    break safe;
	    }
	    return(str);
	}
	StringBuilder buf = new StringBuilder();
	buf.append('|');
	boolean[] cs = isafe;
	for(int i = 0; i < str.length(); i++) {
	    c = str.charAt(i);
	    if((c < 256) && !cs[c]) {
		buf.append('\\');
		buf.append(c);
	    } else {
		buf.append(c);
	    }
	}
	buf.append('|');
	return(buf.toString());
    }

    public String toString() {
	if(ns.equals(""))
	    return(quote(name));
	return(quote(ns) + ":" + quote(name));
    }

    public static Symbol get(String ns, String name) {
	Symbol ret = new Symbol(ns, name);
	synchronized(interned) {
	    Reference<Symbol> ref = interned.get(ret);
	    Symbol p = (ref == null) ? null : ref.get();
	    if(p != null)
		ret = p;
	    else
		interned.put(ret, new WeakReference<>(ret));
	}
	return(ret);
    }

    public static Symbol get(String name) {
	return(get("", name));
    }
}
