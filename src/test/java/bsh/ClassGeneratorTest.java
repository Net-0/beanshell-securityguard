/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
/****************************************************************************/

package bsh;

import static bsh.TestUtil.eval;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.function.IntSupplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import bsh.internals.BshModifier;


// interface A {

// }

// interface B {}

// interface C extends A, B, A {
    
// }

// TODO: fazer um PerformanceTest calculando o tempo de determinadas operações com BeanShell para garantir que os scripts sejam performáticos
// TODO: fazer um MemoryUsageTest calculando o uso de memória em terminadas tarefas, para garantir um uso razoável de memório! ( importante para verificar o custo de alguns caches )

@RunWith(FilteredTestRunner.class)
public class ClassGeneratorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void create_class_with_default_constructor() throws Exception {
        eval("class X1 {}");
    }

    @Test
    public void creating_class_should_not_set_accessibility() throws Exception {
        boolean current = Capabilities.haveAccessibility();
        Capabilities.setAccessibility(false);
        assertFalse("pre: no accessibility should be set", Capabilities.haveAccessibility());
        TestUtil.eval("class X1 {}");
        assertFalse("post: no accessibility should be set", Capabilities.haveAccessibility());
        Capabilities.setAccessibility(current);
    }

    @Test
    public void create_instance() throws Exception {
        assertNotNull(
            eval(
                "class X2 {}",
                "return new X2();"
        ));
    }


    @Test
    public void constructor_args() throws Exception {
        final Object[] oa = (Object[]) eval(
            "class X3 implements IntSupplier {",
                "final Object _instanceVar;",
                "public X3(Object arg) { _instanceVar = arg; }",
                "public int getAsInt() { return _instanceVar; }",
            "}",
            "return new Object[] { new X3(0), new X3(1) } ");
        assertEquals(0, ( (IntSupplier) oa[0] ).getAsInt());
        assertEquals(1, ( (IntSupplier) oa[1] ).getAsInt());
    }


    @Test
    public void call_protected_constructor_from_script() throws Exception {
        final Object[] oa = (Object[]) TestUtil.eval(
            "class X4 implements java.util.concurrent.Callable {",
                "final Object _instanceVar;",
                "X4(Object arg) { _instanceVar = arg; }",
                "public Object call() { return _instanceVar; }",
            "}",
            "return new Object[] { new X4(0), new X4(1) } ");
        assertEquals(0, ( (Callable<?>) oa[0] ).call());
        assertEquals(1, ( (Callable<?>) oa[1] ).call());
    }

    @Test
    public void class_with_abstract_method_must_be_abstract() throws Exception {
        thrown.expect(EvalError.class);
        thrown.expectMessage(containsString("Test is not abstract and does not override abstract method x() in Test"));
        final Interpreter interpreter = new Interpreter();
        interpreter.setStrictJava(true);
        interpreter.eval("class Test { abstract void x(); }");
        interpreter.getNameSpace().clear();
    }

    @Test
    public void verify_public_accesible_modifiers() throws Exception {
        TestUtil.cleanUp();
        boolean current = Capabilities.haveAccessibility();
        Capabilities.setAccessibility(false);

        Class<?> cls = (Class<?>) TestUtil.eval(
            "abstract class X6 {",
                "public Object public_var;",
                "private Object private_var = null;",
                "protected Object protected_var = null;",
                "public final Object public_final_var = 0;",
                "final Object final_var = 0;",
                "static Object static_var;",
                "static final Object static_final_var = null;",
                "volatile Object volatile_var;",
                "transient Object transient_var;",
                "no_type_var = 0;",
                "Object no_modifier_var;",
                "X6() {}",
                "just_method() {}",
                "void void_method() {}",
                "Object type_method() {}",
                "synchronized sync_method() {}",
                "final final_method() {}",
                "static static_method() {}",
                "static final static_final_method() {}",
                "abstract abstract_method() {}",
                "public public_method() {}",
                "private private_method() {}",
                "protected protected_method() {}",
            "}",
            "return X6.class;");

        // public class
        // assertTrue("class has public modifier", Reflect.getClassModifiers(cls).hasModifier("public"));
        // assertTrue("class has abstract modifier", Reflect.getClassModifiers(cls).hasModifier("abstract"));

        // public static variables
        // assertTrue("static_var has public modifier", var(cls, "static_var", "public"));
        assertTrue("static_var has static modifier", var(cls, "static_var", "static"));
        // assertTrue("static_final_var has public modifier", var(cls, "static_final_var", "public"));
        assertTrue("static_final_var has static modifier", var(cls, "static_final_var", "static"));
        assertTrue("static_final_var has final modifier", var(cls, "static_final_var", "final"));

        // public static methods
        // assertTrue("static_method has public modifier", meth(cls, "static_method", "public"));
        assertTrue("static_method has static modifier", meth(cls, "static_method", "static"));
        // assertTrue("static_final_method has public modifier", meth(cls, "static_final_method", "public"));
        assertTrue("static_final_method has static modifier", meth(cls, "static_final_method", "static"));
        assertTrue("static_final_method has final modifier", meth(cls, "static_final_method", "final"));

        // public instance variables
        assertTrue("public_var has public modifier", var(cls, "public_var", "public"));
        assertFalse("private_var does not have public modifier", var(cls, "private_var", "public"));
        assertTrue("private_var has private modifier", var(cls, "private_var", "private"));
        assertFalse("protected_var does not have public modifier", var(cls, "protected_var", "public"));
        assertTrue("protected_var has protected modifier", var(cls, "protected_var", "protected"));
        assertTrue("public_final_var has public modifier", var(cls, "public_final_var", "public"));
        assertTrue("public_final_var has final modifier", var(cls, "public_final_var", "final"));
        // assertTrue("final_var has public modifier", var(cls, "final_var", "public"));
        // assertTrue("final_var has final modifier", var(cls, "final_var", "final"));
        // assertTrue("transient_var has public modifier", var(cls, "transient_var", "public"));
        assertTrue("transient_var has transient modifier", var(cls, "transient_var", "transient"));
        // assertTrue("volatile_var has public modifier", var(cls, "volatile_var", "public"));
        assertTrue("volatile_var has volatile modifier", var(cls, "volatile_var", "volatile"));
        // assertTrue("no_modifier_var has public modifier", var(cls, "no_modifier_var", "public"));
        assertTrue("no_type_var has public modifier", var(cls, "no_type_var", "public"));

        // public instance methods
        // assertTrue("constructor has public modifier", meth(cls, "X6", "public"));
        // assertTrue("just_method has public modifier", meth(cls, "just_method", "public"));
        // assertTrue("void_method has public modifier", meth(cls, "void_method", "public"));
        // assertTrue("type_method has public modifier", meth(cls, "type_method", "public"));
        // assertTrue("sync_method has public modifier", meth(cls, "sync_method", "public"));
        assertTrue("sync_method has synchronized modifier", meth(cls, "sync_method", "synchronized"));
        // assertTrue("final_method has public modifier", meth(cls, "final_method", "public"));
        // assertTrue("final_method has final modifier", meth(cls, "final_method", "final"));
        // assertTrue("abstract_method has public modifier", meth(cls, "abstract_method", "public"));
        assertTrue("abstract_method has abstract modifier", meth(cls, "abstract_method", "abstract"));
        assertTrue("public_method has public modifier", meth(cls, "public_method", "public"));
        assertFalse("private_method does not have public modifier", meth(cls, "private_method", "public"));
        assertTrue("private_method has private modifier", meth(cls, "private_method", "private"));
        assertFalse("protected_method does not have public modifier", meth(cls, "protected_method", "public"));
        assertTrue("protected_method has protected modifier", meth(cls, "protected_method", "protected"));

        Capabilities.setAccessibility(current);
    }

    private boolean var(Class<?> type, String var, String modKW) throws Exception {
        final int mod = Modifiers.fromKeyword(modKW);
        final int mods = type.getDeclaredField(var).getModifiers();
        return (mods & mod) != 0; // Verifiy if the field has the specified modifier
    }

    private boolean meth(Class<?> type, String meth, String modKW) throws Exception {
        final int mod = Modifiers.fromKeyword(modKW);
        final int mods = type.getDeclaredMethod(meth, new Class[0]).getModifiers();
        return (mods & mod) != 0;
    }

    @Test
    public void outer_namespace_visibility() throws Exception {
        final IntSupplier supplier = (IntSupplier) eval(
            "class X4 implements IntSupplier {",
                "public int getAsInt() { return var; }",
            "}",
            "var = 0;",
            "a = new X4();",
            "var = 1;",
            "return a;");
        assertEquals(1, supplier.getAsInt());
    }


    @Test
    public void static_fields_should_be_frozen() throws Exception {
        final IntSupplier supplier =  (IntSupplier)eval(
                "var = 0;",
                "class X5 implements IntSupplier {",
                    "static final Object VAR = var;",
                    "public int getAsInt() { return VAR; }",
                "}",
                "var = 1;", // class not initialized yet
                "a = new X5();", // lazy initialize
                "var = 2;", // constant X5.VAR unchanged
                "return a;"
        );
        assertEquals(1, supplier.getAsInt());
    }

   @Test // TODO: see it!
    public void primitive_data_types_class() throws Exception {
        // Object object = eval("class Test { public static final int x = 4; }; new Test();");
        // assertThat(Reflect.getVariable(object, "x").getValue(), instanceOf(Primitive.class));
        // object = eval("class Test { public static int x = 1; }; new Test();");
        // assertThat(Reflect.getVariable(object, "x").getValue(), instanceOf(Primitive.class));
        // object = eval("class Test { public final int x = 1; }; new Test();");
        // assertThat(Reflect.getVariable(object, "x").getValue(), instanceOf(Primitive.class));
        // object = eval("class Test { static final int x = 1; }; new Test();");
        // assertThat(Reflect.getVariable(object, "x").getValue(), instanceOf(Primitive.class));
        // object = eval("class Test { public int x = 1; }; new Test();");
        // assertThat(Reflect.getVariable(object, "x").getValue(), instanceOf(Primitive.class));
        // object = eval("class Test { static int x = 1; }; new Test();");
        // assertThat(Reflect.getVariable(object, "x").getValue(), instanceOf(Primitive.class));
        // object = eval("class Test { final int x = 1; }; new Test();");
        // assertThat(Reflect.getVariable(object, "x").getValue(), instanceOf(Primitive.class));
        // object = eval("class Test { int x = 1; }; new Test();");
        // assertThat(Reflect.getVariable(object, "x").getValue(), instanceOf(Primitive.class));
        // object = eval("class Test { x = 1; }; new Test();");
        // assertThat(Reflect.getVariable(object, "x").getValue(), instanceOf(Primitive.class));
    }

   @Test
    public void primitive_data_types_interface() throws Exception {
        // Class<?> type = (Class<?>) eval("interface Test { public static final int x = 4; }; Test.class;");
        // assertThat(Reflect.getVariable(type, "x").getValue(), instanceOf(Primitive.class));
        // type = (Class<?>) eval("interface Test { public static int x = 1; }; Test.class;");
        // assertThat(Reflect.getVariable(type, "x").getValue(), instanceOf(Primitive.class));
        // type = (Class<?>) eval("interface Test { public final int x = 1; }; Test.class;");
        // assertThat(Reflect.getVariable(type, "x").getValue(), instanceOf(Primitive.class));
        // type = (Class<?>) eval("interface Test { static final int x = 1; }; Test.class;");
        // assertThat(Reflect.getVariable(type, "x").getValue(), instanceOf(Primitive.class));
        // type = (Class<?>) eval("interface Test { public int x = 1; }; Test.class;");
        // assertThat(Reflect.getVariable(type, "x").getValue(), instanceOf(Primitive.class));
        // type = (Class<?>) eval("interface Test { static int x = 1; }; Test.class;");
        // assertThat(Reflect.getVariable(type, "x").getValue(), instanceOf(Primitive.class));
        // type = (Class<?>) eval("interface Test { final int x = 1; }; Test.class;");
        // assertThat(Reflect.getVariable(type, "x").getValue(), instanceOf(Primitive.class));
        // type = (Class<?>) eval("interface Test { int x = 1; }; Test.class;");
        // assertThat(Reflect.getVariable(type, "x").getValue(), instanceOf(Primitive.class));
        // type = (Class<?>) eval("interface Test { x = 1; }; Test.class;");
        // assertThat(Reflect.getVariable(type, "x").getValue(), instanceOf(Primitive.class));
    }

   @Test
    public void unwrapped_return_types_class() throws Exception {
        Object x = eval("class Test { public static final int x = 4; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("class Test { public static int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("class Test { public final int x = 1; }; new Test().x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("class Test { static final int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("class Test { public int x = 1; }; new Test().x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("class Test { static int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("class Test { final int x = 1; }; new Test().x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("class Test { int x = 1; }; new Test().x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("class Test { x = 1; }; new Test().x;");
        assertThat(x, instanceOf(Integer.class));
    }

   @Test
    public void unwrapped_return_types_interface() throws Exception {
        Object x = eval("interface Test { public static final int x = 4; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("interface Test { public static int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("interface Test { public final int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("interface Test { static final int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("interface Test { public int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("interface Test { static int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("interface Test { final int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("interface Test { int x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
        x = eval("interface Test { x = 1; }; Test.x;");
        assertThat(x, instanceOf(Integer.class));
    }

    @Test
    public void interface_constant_fields() throws Exception {
        // In java interface constants are all public final string
        assertEquals(1, eval("interface Test { public static final int x = 1; }; Test.x;"));
        assertEquals(1, eval("interface Test { static final int x = 1; }; Test.x;"));
        assertEquals(1, eval("interface Test { final int x = 1; }; Test.x;"));
        assertEquals(1, eval("interface Test { public static int x = 1; }; Test.x;"));
        assertEquals(1, eval("interface Test { static int x = 1; }; Test.x;"));
        assertEquals(1, eval("interface Test { public static int x = 1; }; Test.x;"));
        assertEquals(2, eval("interface Test { int x = 2; }; Test.x;"));
        assertEquals(3, eval("interface Test { x = 3; }; Test.x;"));
    }

    @Test
    public void interface_constant_field_illegal_modifier() throws Exception {
        thrown.expect(EvalError.class);
        thrown.expectMessage(containsString("Illegal modifier for interface field x. "
                + "Only public static & final are permitted."));

        eval("interface Test { protected int x = 2; };");
    }

    @Test
    public void class_static_fields() throws Exception {
        assertEquals(1, eval("class Test { public static final int x = 1; }; Test.x;"));
        assertEquals(1, eval("class Test { static final int x = 1; }; Test.x;"));
        assertEquals(1, eval("class Test { public static int x = 1; }; Test.x;"));
        assertEquals(1, eval("class Test { static int x = 1; }; Test.x;"));
        assertEquals(1, eval("class Test { public static int x = 1; }; Test.x;"));
        assertEquals("1", eval("class Test { public static String x = \"1\"; }; Test.x;"));
        assertEquals(0, eval("class Test { public static int x; }; Test.x;"));
        assertEquals(0, eval("class Test { public static final int x = 0; }; Test.x;"));
    }

    @Test
    public void class_instance_fields() throws Exception {
        assertEquals(1, eval("class Test { public final int x = 1; }; new Test().x;"));
        assertEquals(1, eval("class Test { final int x = 1; }; new Test().x;"));
        assertEquals(1, eval("class Test { public int x = 1; }; new Test().x;"));
        assertEquals(1, eval("class Test { int x = 1; }; new Test().x;"));
        assertEquals(1, eval("class Test { x = 1; }; new Test().x;"));
        assertEquals("1", eval("class Test { public String x = \"1\"; }; new Test().x;"));
        assertEquals(0, eval("class Test { int x; }; new Test().x;"));
        assertEquals(0, eval("class Test { final int x = 0; }; new Test().x;"));
        assertEquals(4, eval("class Test { int x = 4; }; new Test().x;"));
        assertEquals(5, eval("class Test { x = 5; }; new Test().x;"));
        assertEquals(6, eval("class Test { int x; Test() { x=6; } }; new Test().x;"));
    }

    @Test
    public void fields_edge_cases() throws Exception {
        assertEquals(7, eval("class Test { ITest in; int x; class ITest { out() { 7; } } Test() { in = new ITest(); x = in.out(); } }; new Test().x;"));
        assertEquals(8, eval("class Test { ITest in; class ITest { out() { 8; } } Test() { in = new ITest(); } } new Test().in.out();"));
    }

    @Test
    public void define_interface_with_constants() throws Exception {
        // all interface fields are public static final in java
        eval("interface Test { public static final int x = 1; }");
        eval("interface Test { static final int x = 1; }");
        eval("interface Test { final int x = 1; }");
        eval("interface Test { public static int x = 1; }");
        eval("interface Test { static int x = 1; }");
        eval("interface Test { int x = 1; }");





        class MyClass {



            void doSomething() {

                class SecundaryClass {

                    void myMethod() {
                        this.myMethod();;
                        MyClass.this.doSomething();
                        MyClass.super.getClass();
                    }

                }

            }

        }
    }

    // TODO: NameSpace não pode setar outro BshClassManager caso já tenha um!!! Isso é importante para impedir que o usuário utilize os métodos utilitários para dar 'bind()' e quebrar o funcionamento interno de generated classes!
    // TODO: test cuja as tipagens de methods e fields são a própria classe ou inner classes!!!
    // TODO: test no BeanShell pois isso n é permitido -> class MyClass extends HashSet<?>
    // TODO: test validanto se o método não está duplicado ( usar methodName + methodDescriptor, tlvz methodSignature?? )
    // TODO: implementar suporte à 'super()' e 'this()' nos construtores!!!!
    // TODO: add interface 'default' impl support!
    // TODO: add teste trocando o package do NameSpace ( Note: deveria ter uma limitação quanto à trocar package no strictJava? )
    // TODO: teste usando o TypeReference do jackson para verificar o suporte de generics!

    @Test
    public void test() throws Throwable {

        // System.out.println("Class Modifiers: " + Modifier.toString(Modifier.classModifiers()));

        // String.format(null, null)
        // Method method = String.class.getMethod("format", String.class, Object[].class);
        // Object result = method.invoke(null, "Formando %s -> %s", 10, 20);
        // System.out.println("result: " + result);

        try {
            // Object result = Reflect.invokeStaticMethod(String.class, "format", new Object[] { "Formando %s -> %s", 10, 20 }, new CallStack());

            // // Class<?> _class = (Class<?>)
            Object result =
            eval(
                // "package myclasses.com;",
                "public class MyClass {",
                "   public static String myMsg = \"Bye-bye world :P\"; ",
                // "   getMyMsg() { return this.myMsg; }",
                "   _getMyMsg() { return MyClass.myMsg; }",

                "   public String myStrValue;",
                "   public int counter;",
                "   public static int counter2;",
                "   private int counter3;",
                "   private static int counter4;",
                "   public void increaseCounter() throws IOException { this.counter++; }", // Note: the 'this' is optional here!
                "   public static void increaseCounter2() { counter2++; }", // Note: can't use 'this' here!
                "   private void increaseCounter3() { this.counter3++; }", // Note: the 'this' is optional here!
                "   private static void increaseCounter4() throws NullPointerException { counter4++; }", // Note: can't use 'this' here!
                "   public doSomething() { return this; }", // Note: Loose-typed method 1
                "   public static doSomething(a, b, c, d) {", // Note: Loose-typed method 2
                "       System.out.println(\"a: \" + a); ",
                "       System.out.println(\"b: \" + b); ",
                "       System.out.println(\"c: \" + c); ",
                "       System.out.println(\"d: \" + d); ",
                "   }",
                "   MyClass(Object a) {}",
                "   public MyClass(int a, Object b) { System.out.println(\"Hello urmon :P\"); }",
                "   private MyClass() {}",
                "}",
                // "return new MyClass(null).doSomething();"
                // "return new MyClass(null).getMyMsg();" // TODO: ver esse problema de recursividade!
                // "return new MyClass(null)._getMyMsg();"
                "return MyClass.myMsg;"
            );

            // // System.out.println("_class: " + _class.toGenericString());
            // // System.out.println("------------------ fields ------------------");
            // // for (Field field: _class.getDeclaredFields())
            // //     System.out.println(" - " + field);
            // // System.out.println("--------------------------------------------");
            // // System.out.println("----------------- methods ------------------");
            // // for (Method method: _class.getDeclaredMethods())
            // //     System.out.println(" - " + method);
            // // System.out.println("--------------------------------------------");
            // // System.out.println("--------------- constructors ---------------");
            // // for (Constructor<?> constructor: _class.getDeclaredConstructors())
            // //     System.out.println(" - " + constructor);
            // // System.out.println("--------------------------------------------");

            // // Method method = _class.getDeclaredMethod("doSomething", Object.class, Object.class, Object.class, Object.class);
            // // method.invoke(null, 4, 3, 2, 1);
            // // _class.getDeclaredConstructor(int.class, Object.class).newInstance(0, null);

            System.out.println("result: " + result);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        
        // try {
        //     String result = (String) eval(
        //         "import com.fasterxml.jackson.databind.ObjectMapper;",
        //         "import com.fasterxml.jackson.databind.SerializationFeature;",
        //         "",
        //         "class MyClass {",
        //         "   public String myStrValue;",
        //         "   public int counter;",
        //         "   private long longCount;",
        //         "   public void increaseCounter() {",
        //         "       this.counter++;",
        //         "   }",
        //         "}",
        //         "ObjectMapper mapper = new ObjectMapper();",
        //         "mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);",
        //         "return mapper.writeValueAsString(new MyClass());"
        //     );
        //     // System.out.println("result: " + result);
        //     Files.write(Paths.get("/home/net0/git/beanshell-securityguard/result.json"), result.getBytes());
        // } catch (Throwable t) {
        //     t.printStackTrace();
        // }

        // class MyClass<T extends Runnable & List<?>> {
        //     public T create() { return null; }
        // }

        // System.out.println("method: " + MyClass.class.getDeclaredMethods()[0].toString());
        // System.out.println("method: " + MyClass.class.getDeclaredMethods()[0].toGenericString());

        // class SuperClass {
        //     SuperClass(Object a, List b) {}
        //     SuperClass(Object a, Map b) {}
        // }

        // class MyClass1 extends SuperClass {

        //     MyClass1(Object a) {
        //         // System.out.println("aaaa");
        //         // if (a != null) super(a, a);
        //         // else super(null, null);
        //         super(a, a instanceof List ? (List) a : (Map) a);
        //     }

        // }

        // TODO: testes unitários devem incluir além da signature normal, validação para o nome dos parâmetros de métodos e construtores!

        // TODO: implement support for static fields and setting them in constructor!
        // class MyClass2 {
        //     public int myNum = 0;

        //     MyClass2() {}

        //     void doSomething() {
        //         System.out.println("this.myNum: " + this.myNum);
        //     }
        // }
        // class MyClass3 extends MyClass2 {
        //     public final int myNum = 30;

        //     MyClass3() {
        //         // this.myNum = 20;
        //         this.doSomething(); 
        //     }
        // }
        // new MyClass3();

        // Reflect.invokeMethod(Object thisArg, String methodName, Object[] args):
        // - BshClassManager.invokeMethod() // TODO: precisa ser no 'bcm' ? Talvez pelo 'memberCache' ?
        //  - Note1: iterate over super-classes ( maybe interfaces too? ) // TODO: and for interfaces default impl?
        //  - Note2: collect all accessible methods
        //  - Note3: Access Validation // TODO: c.getDeclaredMethods() to always get the methods declared at that class!
        //      - method.isPublic
        //      - thisArg instanceof ThisFromObject && method.isProtected
        //      - nameSpace.getDeclaringClasses().contains(method.declaringClass)
        //      - nameSpace.package.equals(method.declaringClass.package) // TODO: verify it!
        //  - Note4: validate the best signature!
        //  - Note5: invoke and return it! // TODO: Primitive.unwrap(thisArg)

        // Reflect.invokeMethod(Object thisArg, String methodName, Object[] args):
        // - BshClassManager.invokeMethod() // TODO: precisa ser no 'bcm' ? Talvez pelo 'memberCache' ?
        //  - Note1: iterate over super-classes ( maybe interfaces too? ) // TODO: and for interfaces default impl?
        //  - Note2: collect all accessible methods
        //  - Note3: Access Validation // TODO: c.getDeclaredMethods() to always get the methods declared at that class!
        //      - method.isPublic
        //      - nameSpace.getDeclaringClasses().contains(method.declaringClass)
        //      - nameSpace.package.equals(method.declaringClass.package) // TODO: verify it!
        //  - Note4: validate the best signature!
        //  - Note5: invoke and return it!

        // class MyLoader extends ClassLoader {
        //     void doSomething() {
        //         this.defineClass("", new byte[0], 0, 0); // Note: visible because we are with 'this'
        //         getSystemClassLoader().defineClass("", new byte[0], 0, 0); // Note: not visible
        //     }
        // }
    }

    // @Test
    // public void throws_signature() throws Exception {
    //     Class<?> _class = (Class<?>) eval("class MyClassImpl<T> { public void doSomething() throws NullPointerException {} }");
    //     Method method = _class.getDeclaredMethods()[0];
    //     System.out.println(method.toString()); // "public void MyClassImpl.doSomething()"
    //     assertEquals("public void MyClassImpl.doSomething() throws NullPointerException", method.toString());
    // }

    // @Test
    // public void method_generic_signature_1() throws Exception {
    //     try {
    //         Class<?> _class = (Class<?>) eval("class MyClassImpl<T> { public void doSomething(T obj) {} }");
    //         Method method = null;
    //         for (Method _method: _class.getDeclaredMethods()) if (_method.getName().equals("doSomething")) method = _method;

    //         System.out.println(method.toGenericString());

    //         assertEquals("public void MyClassImpl.doSomething(T obj)", method.toString());
    //     } catch (Throwable e) {
    //         e.printStackTrace();
    //     }
    // }

    // @Test
    // public void method_generic_signature_2() throws Exception {
    //     Class<?> _class = (Class<?>) eval("class MyClassImpl { public <T> void doSomething(T obj) {} }");
    //     Method method = null;
    //     for (Method _method: _class.getDeclaredMethods()) if (_method.getName().equals("doSomething")) method = _method;

    //     assertEquals("public <T> void MyClassImpl.doSomething(T obj)", method.toString());
    // }

    // @Test
    // public void method_generic_signature_3() throws Exception {
    //     Class<?> _class = (Class<?>) eval("class MyClassImpl<T> { public <R> R doSomething(T obj) {} }");
    //     Method method = null;
    //     for (Method _method: _class.getDeclaredMethods()) if (_method.getName().equals("doSomething")) method = _method;

    //     assertEquals("public <R> R MyClassImpl.doSomething(T obj)", method.toString());
    // }
}


// class A {
//     protected int getNum() { return 0; }
// }

// class B extends A {
//     // public int getNum() { return 10; }
//     protected int getNum2() {
//         new A().getNum();

//         return this.getNum();
//     }
// }

// class C extends B {
//     public int getNum3() {
//         this.getNum();
//         return 23;
//     }
// }        ((java.lang.reflect.Method) null).isVarArgs();