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

package haven.iosys.tk;

import haven.*;
import java.util.*;

public interface Key {
    public String id();
    public Loc location();
    public Sym primary();
    public Sym primary(Collection<? extends Sym> of);
    public default boolean is(Sym sym) {
	return(primary() == sym);
    }

    public static interface Sym {
	public String id();
	public String nm();
    }

    public static enum Std implements Sym {
        ENTER("ret", "Enter", '\r'),
        BACKSPACE("bksp", "Backspace", '\b'),
        TAB("tab", "Tab", '\t'),
        CANCEL("cancel", "Cancel"),
        CLEAR("clear", "Clear"),
        SHIFT("shift", "Shift"),
        CONTROL("ctrl", "Control"),
        ALT("alt", "Alt"),
        PAUSE("pause", "Pause"),
        CAPSLOCK("capslock", "Caps Lock"),
        ESCAPE("esc", "Escape", '\u001b'),
        SPACE("sp", "Space", ' '),
        PAGEUP("pgup", "Page Up"),
        PAGEDOWN("pgdn", "Page Down"),
        END("end", "End"),
        HOME("home", "Home"),
        LEFT("left", "Left"),
        UP("up", "Up"),
        RIGHT("right", "Right"),
        DOWN("down", "Down"),
        COMMA("comma", "Comma", ','),
        MINUS("minus", "Minus", '-'),
        PERIOD("period", "Period", '.'),
        SLASH("slash", "Slash", '/'),
        SEMICOLON("semicolon", "Semicolon", ';'),
        EQUALS("equals", "Equals", '='),
        LEFTBRACKET("[", "Left Bracket", '['),
        RIGHTBRACKET("]", "Right Bracket", ']'),
        BACKSLASH("bkslash", "Backslash", '\\'),
	BAR("bar", "Bar", '|'),
        DELETE("del", "Delete"),
        NUMLOCK("numlock", "Num Lock"),
        SCROLLLOCK("scrlock", "Scroll Lock"),
        PRINTSCREEN("prtsc", "Print Screen"),
        INSERT("ins", "Insert"),
        HELP("help", "Help"),
        META("meta", "Meta"),
        BACKQUOTE("bkquote", "Back Quote", '`'),
        QUOTE("quote", "Quote", '\''),
 
        AMPERSAND("amp", "Ampersand", '&'),
        ASTERISK("mul", "Asterisk", '*'),
        DBLQUOTE("2quote", "Double Quote", '"'),
        LT("lt", "Less Than", '<'),
        GT("gt", "Greater Than", '>'),
        LEFTBRACE("{", "Left Brace", '{'),
        RIGHTBRACE("}", "Right Brace", '}'),
        AT("at", "At", '@'),
        COLON("colon", "Colon", ':'),
        CIRCUMFLEX("circ", "Circumflex", '^'),
        DOLLAR("dollar", "Dollar", '$'),
        EUROSIGN("euro", "Euro", '\u20ac'),
        EXCL("excl", "Exclamation Mark", '!'),
        INVEXCL("invexcl", "Inverted Exclamation Mark", '\u00a1'),
        LEFTPAREN("(", "Left Parenthesis", '('),
        RIGHTPAREN(")", "Right Parenthesis", ')'),
        NUMBERSIGN("number", "Number Sign", '#'),
        PLUS("plus", "Plus", '+'),
        UNDERSCORE("uscore", "Underscore", '_'),
        WINDOWS("win", "Windows"),
        MENU("menu", "Context Menu"),
        FINAL("final", "Final"),
        CONVERT("conv", "Convert"),
        NONCONVERT("nconv", "No Convert"),
        ACCEPT("accept", "Accept"),
        MODECHANGE("chmod", "Mode Change"),
        KANA("kana", "Kana"),
        KANJI("kanji", "Kanji"),
        ALNUM("alnum", "Alphanumeric"),
        KATAKANA("katakana", "Katakana"),
        HIRAGANA("hiragana", "Hiragana"),
        FULLWIDTH("fullwidth", "Fullwidth"),
        HALFWIDTH("halfwidth", "Halfwidth"),
        ROMAN("roman", "Roman Characters"),
        ALLCAND("allcand", "All Candidates"),
        PREVCAND("prevcand", "Previous Candidate"),
        KANALOCK("kanalock", "Kana Lock"),
        INPUTMETH("inputmeth", "Input Method"),
        CUT("cut", "Cut"),
        COPY("copy", "Copy"),
        PASTE("paste", "Paste"),
        UNDO("undo", "Undo"),
        AGAIN("again", "Again"),
        FIND("find", "Find"),
        PROPS("props", "Properties"),
        STOP("stop", "Stop"),
        COMPOSE("compose", "Compose"),
        ALTGR("altgr", "Alt Gr."),
        BEGIN("begin", "Begin"),
 
        DEADGRAVE("deadgrave", "Dead Grave"),
        DEADACUTE("deadacute", "Dead Acute"),
        DEADCIRCUMFLEX("deadcirc", "Dead Circumflex"),
        DEADTILDE("deadtilde", "Dead Tilde"),
        DEADMACRON("deadmacron", "Dead Macron"),
        DEADBREVE("deadbreve", "Dead Breve"),
        DEADABOVEDOT("deadabovedot", "Dead Above Dot"),
        DEADDIAERESIS("deaddia", "Dead Diaeresis"),
        DEADABOVERING("deadabovering", "Dead Above Ring"),
        DEADDOUBLEACUTE("dead2acute", "Dead Double Acute"),
        DEADCARON("deadcaron", "Dead Caron"),
        DEADCEDILLA("deadcedilla", "Dead Cedilla"),
        DEADOGONEK("deadogonek", "Dead Ogonek"),
        DEADIOTA("deadiota", "Dead Iota"),
        DEADVOICED("deadvoiced", "Dead Voiced"),
        DEADSEMIVOICED("deadsemivoiced", "Dead Semivoiced"),
 
        N0("0", "0", '0'),
        N1("1", "1", '1'),
        N2("2", "2", '2'),
        N3("3", "3", '3'),
        N4("4", "4", '4'),
        N5("5", "5", '5'),
        N6("6", "6", '6'),
        N7("7", "7", '7'),
        N8("8", "8", '8'),
        N9("9", "9", '9'),
 
        A("a", "A", 'A'),
        B("b", "B", 'B'),
        C("c", "C", 'C'),
        D("d", "D", 'D'),
        E("e", "E", 'E'),
        F("f", "F", 'F'),
        G("g", "G", 'G'),
        H("h", "H", 'H'),
        I("i", "I", 'I'),
        J("j", "J", 'J'),
        K("k", "K", 'K'),
        L("l", "L", 'L'),
        M("m", "M", 'M'),
        N("n", "N", 'N'),
        O("o", "O", 'O'),
        P("p", "P", 'P'),
        Q("q", "Q", 'Q'),
        R("r", "R", 'R'),
        S("s", "S", 'S'),
        T("t", "T", 'T'),
        U("u", "U", 'U'),
        V("v", "V", 'V'),
        W("w", "W", 'W'),
        X("x", "X", 'X'),
        Y("y", "Y", 'Y'),
        Z("z", "Z", 'Z'),
 
        F1("f1", "F1"),
        F2("f2", "F2"),
        F3("f3", "F3"),
        F4("f4", "F4"),
        F5("f5", "F5"),
        F6("f6", "F6"),
        F7("f7", "F7"),
        F8("f8", "F8"),
        F9("f9", "F9"),
        F10("f10", "F10"),
        F11("f11", "F11"),
        F12("f12", "F12"),
        F13("f13", "F13"),
        F14("f14", "F14"),
        F15("f15", "F15"),
        F16("f16", "F16"),
        F17("f17", "F17"),
        F18("f18", "F18"),
        F19("f19", "F19"),
        F20("f20", "F20"),
        F21("f21", "F21"),
        F22("f22", "F22"),
        F23("f23", "F23"),
        F24("f24", "F24"),
 
        NP0("np0", "Numpad 0", '0'),
        NP1("np1", "Numpad 1", '1'),
        NP2("np2", "Numpad 2", '2'),
        NP3("np3", "Numpad 3", '3'),
        NP4("np4", "Numpad 4", '4'),
        NP5("np5", "Numpad 5", '5'),
        NP6("np6", "Numpad 6", '6'),
        NP7("np7", "Numpad 7", '7'),
        NP8("np8", "Numpad 8", '8'),
        NP9("np9", "Numpad 9", '9'),
        NP_DIV("npdiv", "Numpad /", '/'),
        NP_MUL("npmul", "Numpad *", '*'),
        NP_SUB("npsub", "Numpad -", '-'),
        NP_ADD("npadd", "Numpad +", '+'),
        NP_SEP("npsep", "Numpad ,", ','),
        NP_DEC("npdec", "Numpad .", '.'),
        NP_UP("npup", "Numpad Up"),
        NP_DOWN("npdown", "Numpad Down"),
        NP_LEFT("npleft", "Numpad Left"),
        NP_RIGHT("npright", "Numpad Right");
 
        public final String id, nm;
	public final char ch;
 
        Std(String id, String nm, char ch) {
            this.id = ("std:" + id).intern();
            this.nm = nm;
	    this.ch = ch;
        }
        Std(String id, String nm) {
	    this(id, nm, '\0');
	}
 
        public String id() {return(id);}
	public String nm() {return(nm);}
 
        public final static List<Std> NUMBERS = new EnumInterval<Std>(N0, N9);
        public final static List<Std> PADNUMBERS = new EnumInterval<Std>(NP0, NP9);
        public final static List<Std> FKEYS = new EnumInterval<Std>(F1, F24);
        public final static List<Std> LATIN = new EnumInterval<Std>(A, Z);
        public final static Set<Std> MODKEYS = EnumSet.of(SHIFT, CONTROL, META, ALT, ALTGR, WINDOWS);
	public final static Set<Std> ALL = new HashSet<Std>(Arrays.asList(Std.values()));
    }

    public static interface Loc {
	public String id();

	public static enum Std implements Loc {
	    ESC,  FK01, FK02, FK03, FK04, FK05, FK06, FK07, FK08, FK09, FK10, FK11, FK12,         PRSC, SCLK, PAUS,

	    TLDE, AE01, AE02, AE03, AE04, AE05, AE06, AE07, AE08, AE09, AE10, AE11, AE12, BKSP,   INS,  HOME, PGUP,   NMLK, KPDV, KPMU, KPSU,
	    TAB,  AD01, AD02, AD03, AD04, AD05, AD06, AD07, AD08, AD09, AD10, AD11, AD12,         DEL,  END,  PGDN,   KP7,  KP8,  KP9,  KPAD,
	    CAPS, AC01, AC02, AC03, AC04, AC05, AC06, AC07, AC08, AC09, AC10, AC11, BKSL, RTRN,                       KP4,  KP5,  KP6,
	    LFSH, LSGT, AB01, AB02, AB03, AB04, AB05, AB06, AB07, AB08, AB09, AB10,       RTSH,         UP,           KP1,  KP2,  KP3,  KPEN,
	    LCTL, LWIN, LALT,                   SPCE,                   RALT, RWIN, MENU, RCTL,   LEFT, DOWN, RGHT,   KP0,        KPDL;

	    public String id() {return(("std:" + this.name()).intern());}
	}
    }

    public static enum Mod {
	SHIFT, CONTROL, META, ALT, SUPER, ALTGR
    }
}
