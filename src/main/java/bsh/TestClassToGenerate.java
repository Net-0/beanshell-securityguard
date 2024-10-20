package bsh;

import bsh.internals.BshClass;

public class TestClassToGenerate {
    
    public static Object doSomething(Object a) throws Throwable {
        return (Object) BshClass.getDeclaredMethods(TestClassToGenerate.class)[0].invoke((Object) null, new Object[] { a });
    }

}
