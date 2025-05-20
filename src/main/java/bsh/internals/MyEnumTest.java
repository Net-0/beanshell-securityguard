package bsh.internals;

public enum MyEnumTest {
    VAL1,
    VAL2;
 
    int val;
 
    static void method() throws Throwable {
        // BshClass var0 = BshClass.fromGeneratedClass(MyEnumTest.class);
        // var0.findConstructorIndex((Object[])var0.enumConstants[0].argsSupplier.get());
        // var0.findConstructorIndex((Object[])var0.enumConstants[1].argsSupplier.get());
    }
 
    private MyEnumTest(
        // int someShit
    ) {
        // super(); // TODO: construtores de enum n podem ter super()!

    //    BshClass.initialize(MyEnumTest.class, this);
    //    BshClass.getDeclaredConstructors(MyEnumTest.class)[0].construct(this, new Object[]{var1});
    }
 
    private Object MyEnumTest(int a) {
        return null;
    //    return (Object)BshClass.getDeclaredMethods(MyEnumTest.class)[0].invoke(this, new Object[]{a});
    }
}
