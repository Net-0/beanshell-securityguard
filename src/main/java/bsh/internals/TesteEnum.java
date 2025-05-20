package bsh.internals;

public enum TesteEnum {
    VAL1("1val") {
        // TODO: isso n pode ser usado
        // static int meuNum2;
        // static Object get2() { return null; }
        // public TesteEnum(int a) { super(a + ""); }

        // VAL3("123"), VAL("456");

        int meuNum;
        Object get() { return str + "1"; }
    },
    VAL2("2val") {
        Object get() { return str + "2"; }
    };
    String str = "";
    TesteEnum(String s) {
        str = s;

        // TesteEnum.
    }
}