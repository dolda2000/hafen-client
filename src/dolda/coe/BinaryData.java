package dolda.coe;

public interface BinaryData {
    public static final int T_END = 0;
    public static final int T_INT = 1;
    public static final int T_STR = 2;
    public static final int T_BIT = 3;
    public static final int T_NIL = 4;
    public static final int T_SYM = 5;
    public static final int T_CON = 6;
    public static final int T_DCL = 7;

    public static final int INT_REF = 1;

    public static final int STR_SYM = 1;

    public static final int BIT_BFLOAT = 1;
    public static final int BIT_DFLOAT = 2;

    public static final int CON_SEQ = 0;
    public static final int CON_SET = 1;
    public static final int CON_MAP = 2;
    public static final int CON_OBJ = 3;

    public static final int NIL_FALSE = 1;
    public static final int NIL_TRUE = 2;
}
