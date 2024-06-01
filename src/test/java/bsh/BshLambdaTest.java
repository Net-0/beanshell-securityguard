package bsh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

// @Ignore
@SuppressWarnings("unchecked")
public class BshLambdaTest {

    @FunctionalInterface
    public static interface CharSupplier { char getAsChar(); }

    @FunctionalInterface
    public static interface ByteSupplier { byte getAsByte(); }

    @FunctionalInterface
    public static interface ShortSupplier { short getAsShort(); }

    @FunctionalInterface
    public static interface FloatSupplier { float getAsFloat(); }

    @FunctionalInterface
    public static interface ListGenerator { List<?> getList(Object value); }

    @FunctionalInterface
    public static interface Executor { void exec() throws NullPointerException; }

    @FunctionalInterface
    public static interface Executor2 { void exec() throws IOException, NullPointerException; }

    @FunctionalInterface
    public static interface IntSupplierGenerator { IntSupplier generate(); }

    @FunctionalInterface
    public static interface LambdaAllArgs {
        boolean exec(
            char arg1,
            boolean arg2,
            byte arg3,
            short arg4,
            int arg5,
            long arg6,
            float arg7,
            double arg8,
            Object arg9
        );
    }

    public static class InvalidBshLambdaWrapper {}

    @FunctionalInterface
    public static interface ListSupplier<T extends List<?>> { T getAsList(); }

    @FunctionalInterface
    public static interface MapSupplier<T extends Map<?, ?>> { T getAsMap(); }

    @FunctionalInterface
    public static interface ClassSupplier { Class<?> getAsClass(); }

    @FunctionalInterface
    public static interface PredicateSupplier<T> { Predicate<T> getAsPredicate(); }

    @Test
    public void test_lambda_expression_empty_lambda() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> {}");
        Assert.assertEquals(null, lambda.invoke(new Object[0], new Class[0]));
    }

    @Test
    public void test_lambda_expression_no_body_return_number() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> 123");
        Assert.assertEquals(123, lambda.invoke(new Object[0], new Class[0]));
    }

    @Test
    public void test_lambda_expression_no_body_return_new_object() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> new ArrayList()");
        Assert.assertEquals(new ArrayList<>(), lambda.invoke(new Object[0], new Class[0]));
    }

    @Test
    public void test_lambda_expression_no_body_return_static_member() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.EMPTY_LIST");
        Assert.assertEquals(Collections.EMPTY_LIST, lambda.invoke(new Object[0], new Class[0]));
    }

    @Test
    public void test_lambda_expression_no_body_return_invoke_static_member() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.emptyMap()");
        Assert.assertEquals(Collections.emptyMap(), lambda.invoke(new Object[0], new Class[0]));
    }

    @Test
    public void test_lambda_expression_single_param_return_param() throws Throwable {
        Object obj = new Object();
        BshLambda lambda = (BshLambda) TestUtil.eval("p -> p");
        Assert.assertTrue(obj == lambda.invoke(new Object[] { obj }, new Class[0]));
    }

    @Test
    public void test_lambda_expression_wrong_args_length() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("(a, b, c, d) -> {}");
        RuntimeEvalError error = Assert.assertThrows(RuntimeEvalError.class, () -> lambda.invoke(new Object[] { null, null, null }, new Class[0]));
        Assert.assertTrue(error.getMessage().contains("Wrong number of arguments!"));
    }

    @Test
    public void test_lambda_expression_too_much_args() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("(a, b) -> {}");
        RuntimeEvalError error = Assert.assertThrows(RuntimeEvalError.class, () -> lambda.invoke(new Object[] { null, null, null }, new Class[0]));
        Assert.assertTrue(error.getMessage().contains("Wrong number of arguments!"));
    }

    @Test
    public void test_converted_lambda_expression_empty_lambda() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> {}");
        Runnable lambda = bshLambda.convertTo(Runnable.class);
        lambda.run();
    }

    @Test
    public void test_converted_lambda_expression_no_body_return_number() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> 123");
        Supplier<Integer> lambda = bshLambda.convertTo(Supplier.class);
        Assert.assertEquals(Integer.valueOf(123), lambda.get());
    }

    @Test
    public void test_converted_lambda_expression_no_body_return_new_object() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> new ArrayList()");
        Supplier<List<?>> lambda = bshLambda.convertTo(Supplier.class);
        Assert.assertEquals(new ArrayList<>(), lambda.get());
    }

    @Test
    public void test_converted_lambda_expression_no_body_return_static_member() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> Collections.EMPTY_LIST");
        Supplier<List<?>> lambda = bshLambda.convertTo(Supplier.class);
        Assert.assertEquals(Collections.EMPTY_LIST, lambda.get());
    }

    @Test
    public void test_converted_lambda_expression_no_body_return_invoke_static_member() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> Collections.emptyMap()");
        Supplier<Map<?, ?>> lambda = bshLambda.convertTo(Supplier.class);
        Assert.assertEquals(Collections.emptyMap(), lambda.get());
    }

    @Test
    public void test_converted_lambda_expression_single_param_return_param() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("num -> num * 2");
        Function<Integer, Integer> lambda = bshLambda.convertTo(Function.class);
        Assert.assertEquals(Integer.valueOf(10), lambda.apply(5));
    }

    @Test
    public void test_converted_invalid_functional_interface() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> {}");
        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class, () -> bshLambda.convertTo(Object.class));
        Assert.assertTrue(error.getMessage().contains("This class isn't a valid Functional Interface:"));
    }

    @Test
    public void test_casted_lambda_expression() throws Throwable {
        Object result = TestUtil.eval("(Function<Integer, Integer>) num -> num * 2");
        Assert.assertTrue(result instanceof Function);
        Function<Integer, Integer> lambda = (Function<Integer, Integer>) result;
        Assert.assertEquals(Integer.valueOf(8), lambda.apply(4));
    }

    @Test
    public void test_assigned_lambda_expression() throws Throwable {
        Object result = TestUtil.eval(
            "Function<Integer, Integer> lambda = num -> num * 2;",
            "return lambda;"
        );
        Assert.assertTrue(result instanceof Function);
        Function<Integer, Integer> lambda = (Function<Integer, Integer>) result;
        Assert.assertEquals(Integer.valueOf(16), lambda.apply(8));
    }

    @Test
    public void test_method_reference() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("new ArrayList<>()::equals");
        Assert.assertTrue(lambda.invokeBoolean(new Object[] { new ArrayList<>() }, new Class[0]));
    }

    @Test
    public void test_method_reference_of_static_method() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("Collections::emptyList");
        Assert.assertEquals(Collections.emptyList(), lambda.invoke(new Object[0], new Class[0]));
    }

    @Test
    public void test_method_reference_of_constructor() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("Integer::new");
        Assert.assertEquals(43, lambda.invoke(new Object[] { "43" }, new Class[0]));
    }

    @Test
    public void test_method_reference_wrong_args_length() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("new ArrayList<>()::equals");
        RuntimeEvalError error = Assert.assertThrows(RuntimeEvalError.class, () -> lambda.invokeBoolean(new Object[0], new Class[0]));
        Assert.assertTrue(error.getMessage().contains("Can't invoke lambda"));
    }

    @Test
    public void test_converted_method_reference() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("new ArrayList<>()::equals");
        Predicate<Object> lambda = bshLambda.convertTo(Predicate.class);
        Assert.assertTrue(lambda.test(new ArrayList<>()));
    }

    @Test
    public void test_converted_method_reference_of_static_method() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("Collections::emptyList");
        Supplier<List<?>> lambda = bshLambda.convertTo(Supplier.class);
        Assert.assertEquals(Collections.emptyList(), lambda.get());
    }

    @Test
    public void test_converted_method_reference_of_constructor() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("Integer::new");
        Function<String, Integer> lambda = bshLambda.convertTo(Function.class);
        Assert.assertEquals((Integer) 32, lambda.apply("32"));
    }

    @Test
    public void test_casted_method_reference() throws Throwable {
        Predicate<Object> lambda = (Predicate<Object>) TestUtil.eval("(java.util.function.Predicate) new ArrayList<>()::equals");
        Assert.assertTrue(lambda.test(new ArrayList<>()));
    }

    @Test
    public void test_casted_method_reference_of_static_method() throws Throwable {
        Supplier<List<?>> lambda = (Supplier<List<?>>) TestUtil.eval("(java.util.function.Supplier) Collections::emptyList");
        Assert.assertEquals(Collections.emptyList(), lambda.get());
    }

    @Test
    public void test_casted_method_reference_of_constructor() throws Throwable {
        Function<String, Integer> lambda = (Function<String, Integer>) TestUtil.eval("(java.util.function.Function) Integer::new");
        Assert.assertEquals((Integer) 32, lambda.apply("32"));
    }

    @Test
    public void test_assigned_method_reference() throws Throwable {
        Predicate<Object> lambda = (Predicate<Object>) TestUtil.eval(
            "import java.util.function.Predicate;",
            "Predicate<Object> lambda = new ArrayList<>()::equals;",
            "return lambda;"
        );
        Assert.assertTrue(lambda.test(new ArrayList<>()));
    }

    @Test
    public void test_assigned_method_reference_of_static_method() throws Throwable {
        Supplier<List<?>> lambda = (Supplier<List<?>>) TestUtil.eval(
            "import java.util.function.Supplier;",
            "Supplier<List<?>> lambda = Collections::emptyList;",
            "return lambda;"
        );
        Assert.assertEquals(Collections.emptyList(), lambda.get());
    }

    @Test
    public void test_assigned_method_reference_of_constructor() throws Throwable {
        Function<String, Integer> lambda = (Function<String, Integer>) TestUtil.eval(
            "import java.util.function.Function;",
            "Function<String, Integer> lambda = Integer::new;",
            "return lambda;"
        );
        Assert.assertEquals((Integer) 32, lambda.apply("32"));
    }

    @Test
    public void test_invoke_method_with_lambda_expression_arg() throws Throwable {
        List<Number> expected = Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20);
        List<Number> result = (List<Number>) TestUtil.eval(
            "List<Number> nums1 = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);",
            "List<Number> nums2 = new ArrayList();",
            "nums1.forEach((num) -> nums2.add(num * 2));",
            "return nums2;"
        );
        Assert.assertEquals(expected, result);
    }

    @Test
    public void test_invoke_local_method_with_lambda_expression_arg_1() throws Throwable {
        List<Number> expected = Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20);
        List<Number> result = (List<Number>) TestUtil.eval(
            "import java.util.function.Consumer;",
            "void forNum(Consumer<Number> consumer) {",
            "   List<Number> nums1 = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);",
            "   nums1.forEach(consumer);",
            "}",
            "List<Number> nums2 = new ArrayList();",
            "forNum((num) -> nums2.add(num * 2));",
            "return nums2;"
        );
        Assert.assertEquals(expected, result);
    }

    @Test
    public void test_invoke_local_method_with_lambda_expression_arg_2() throws Throwable {
        List<Number> expected = Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20);
        List<Number> result = (List<Number>) TestUtil.eval(
            "import java.util.function.Consumer;",
            "void forNum(Consumer<Number> consumer) {",
            "   consumer.accept(1);",
            "   consumer.accept(2);",
            "   consumer.accept(3);",
            "   consumer.accept(4);",
            "   consumer.accept(5);",
            "   consumer.accept(6);",
            "   consumer.accept(7);",
            "   consumer.accept(8);",
            "   consumer.accept(9);",
            "   consumer.accept(10);",
            "}",
            "List<Number> nums2 = new ArrayList();",
            "forNum((num) -> nums2.add(num * 2));",
            "return nums2;"
        );
        Assert.assertEquals(expected, result);
    }

    @Test
    public void test_invoke_local_method_with_loose_type_lambda_expression_arg() throws Throwable {
        List<Number> expected = Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20);
        List<Number> result = (List<Number>) TestUtil.eval(
            "import bsh.BshLambda;",
            "import org.junit.Assert;",
            "void forNum(consumer) {",
            "   Assert.assertTrue(consumer instanceof BshLambda);",
            "   consumer.invoke(new Object[] { 1 }, new Class[0]);",
            "   consumer.invoke(new Object[] { 2 }, new Class[0]);",
            "   consumer.invoke(new Object[] { 3 }, new Class[0]);",
            "   consumer.invoke(new Object[] { 4 }, new Class[0]);",
            "   consumer.invoke(new Object[] { 5 }, new Class[0]);",
            "   consumer.invoke(new Object[] { 6 }, new Class[0]);",
            "   consumer.invoke(new Object[] { 7 }, new Class[0]);",
            "   consumer.invoke(new Object[] { 8 }, new Class[0]);",
            "   consumer.invoke(new Object[] { 9 }, new Class[0]);",
            "   consumer.invoke(new Object[] { 10 }, new Class[0]);",
            "}",
            "List<Number> nums2 = new ArrayList();",
            "forNum((num) -> nums2.add(num * 2));",
            "return nums2;"
        );
        Assert.assertEquals(expected, result);
    }

    @Test
    public void test_invoke_static_method_with_lambda_expression_arg() throws Throwable {
        List<Number> expected = Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20);
        List<Number> result = (List<Number>) TestUtil.eval(
            "import java.util.function.Consumer;",
            "class MyClass {",
            "   public static void forNum(Consumer<Number> consumer) {",
            "       List<Number> nums1 = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);",
            "       nums1.forEach(consumer);",
            "   }",
            "}",
            "List<Number> nums2 = new ArrayList();",
            "MyClass.forNum((num) -> nums2.add(num * 2));",
            "return nums2;"
        );
        Assert.assertEquals(expected, result);
    }

    @Test
    public void test_invoke_method_with_method_reference_arg() throws Throwable {
        List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        List<Integer> result = (List<Integer>) TestUtil.eval(
            "import java.util.stream.Collectors;",
            "class Parser {",
            "   public Integer parseInt(String num) { return new Integer(num); }",
            "}",
            "Parser parser = new Parser();",
            "List<String> nums = Arrays.asList(\"1\", \"2\", \"3\", \"4\", \"5\", \"6\", \"7\", \"8\");",
            "return nums.stream().map(parser::parseInt).collect(Collectors.toList());"
        );
        Assert.assertEquals(expected, result);
    }

    @Test
    public void test_invoke_method_with_method_reference_of_static_method_arg() throws Throwable {
        List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        List<Integer> result = (List<Integer>) TestUtil.eval(
            "import java.util.stream.Collectors;",
            "List<String> nums = Arrays.asList(\"1\", \"2\", \"3\", \"4\", \"5\", \"6\", \"7\", \"8\");",
            "return nums.stream().map(Integer::parseInt).collect(Collectors.toList());"
        );
        Assert.assertEquals(expected, result);
    }

    @Test
    public void test_invoke_method_with_method_reference_of_constructor_arg() throws Throwable {
        List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        List<Integer> result = (List<Integer>) TestUtil.eval(
            "import java.util.stream.Collectors;",
            "List<String> nums = Arrays.asList(\"1\", \"2\", \"3\", \"4\", \"5\", \"6\", \"7\", \"8\");",
            "return nums.stream().map(Integer::new).collect(Collectors.toList());"
        );
        Assert.assertEquals(expected, result);
    }

    @Test
    public void test_invoke_primitive() throws Throwable {
        BshLambda bshBooleanLambda = (BshLambda) TestUtil.eval("() -> false");
        BshLambda bshCharLambda = (BshLambda) TestUtil.eval("() -> 'k'");
        BshLambda bshNumberLambda = (BshLambda) TestUtil.eval("() -> 2");

        Assert.assertEquals(false, bshBooleanLambda.convertTo(BooleanSupplier.class).getAsBoolean());
        Assert.assertEquals('k', bshCharLambda.convertTo(CharSupplier.class).getAsChar());
        Assert.assertEquals(2, bshNumberLambda.convertTo(ByteSupplier.class).getAsByte());
        Assert.assertEquals(2, bshNumberLambda.convertTo(ShortSupplier.class).getAsShort());
        Assert.assertEquals(2, bshNumberLambda.convertTo(IntSupplier.class).getAsInt());
        Assert.assertEquals(2, bshNumberLambda.convertTo(LongSupplier.class).getAsLong());
        Assert.assertEquals(2, bshNumberLambda.convertTo(FloatSupplier.class).getAsFloat(), 0);
        Assert.assertEquals(2, bshNumberLambda.convertTo(DoubleSupplier.class).getAsDouble(), 0);
    }

    @Test
    public void test_cant_invoke_primitive() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> null");

        RuntimeEvalError errorBoolean = Assert.assertThrows(RuntimeEvalError.class, () -> bshLambda.convertTo(BooleanSupplier.class).getAsBoolean());
        Assert.assertTrue(errorBoolean.getMessage().contains("Can't convert null to boolean"));

        RuntimeEvalError errorChar = Assert.assertThrows(RuntimeEvalError.class, () -> bshLambda.convertTo(CharSupplier.class).getAsChar());
        Assert.assertTrue(errorChar.getMessage().contains("Can't convert null to char"));

        RuntimeEvalError errorByte = Assert.assertThrows(RuntimeEvalError.class, () -> bshLambda.convertTo(ByteSupplier.class).getAsByte());
        Assert.assertTrue(errorByte.getMessage().contains("Can't convert null to byte"));

        RuntimeEvalError errorShort = Assert.assertThrows(RuntimeEvalError.class, () -> bshLambda.convertTo(ShortSupplier.class).getAsShort());
        Assert.assertTrue(errorShort.getMessage().contains("Can't convert null to short"));

        RuntimeEvalError errorInt = Assert.assertThrows(RuntimeEvalError.class, () -> bshLambda.convertTo(IntSupplier.class).getAsInt());
        Assert.assertTrue(errorInt.getMessage().contains("Can't convert null to int"));

        RuntimeEvalError errorLong = Assert.assertThrows(RuntimeEvalError.class, () -> bshLambda.convertTo(LongSupplier.class).getAsLong());
        Assert.assertTrue(errorLong.getMessage().contains("Can't convert null to long"));

        RuntimeEvalError errorFloat = Assert.assertThrows(RuntimeEvalError.class, () -> bshLambda.convertTo(FloatSupplier.class).getAsFloat());
        Assert.assertTrue(errorFloat.getMessage().contains("Can't convert null to float"));

        RuntimeEvalError errorDouble = Assert.assertThrows(RuntimeEvalError.class, () -> bshLambda.convertTo(DoubleSupplier.class).getAsDouble());
        Assert.assertTrue(errorDouble.getMessage().contains("Can't convert null to double"));
    }

    @Test
    public void test_method_reference_not_assignable_invalid_params() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval(
            "class Parser {",
            "   public Integer parseInt(String num) { return new Integer(num); }",
            "}",
            "return new Parser()::parseInt"
        );
        boolean assignable = BshLambda.isAssignable(bshLambda.dummyType, BiFunction.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_method_reference_not_assignable_invalid_return_type() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval(
            "class Parser {",
            "   public int parseInt(String num, String arg2) { return new Integer(num); }",
            "}",
            "return new Parser()::parseInt"
        );
        boolean assignable = BshLambda.isAssignable(bshLambda.dummyType, BiFunction.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_method_reference_not_assignable_invalid_static() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval(
            "class Parser {",
            "   public int parseInt(String num, String arg2) { return new Integer(num); }",
            "}",
            "return Parser::parseInt"
        );
        boolean assignable = BshLambda.isAssignable(bshLambda.dummyType, BiFunction.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_method_reference_of_static_method_not_assignable() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("Integer::parseInt");
        boolean assignable = BshLambda.isAssignable(bshLambda.dummyType, BiFunction.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_method_reference_of_constructor_not_assignable() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("Integer::new");
        boolean assignable = BshLambda.isAssignable(bshLambda.dummyType, BiFunction.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_method_reference_of_constructor_not_assignable_invalid_return_type() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("Integer::new");
        boolean assignable = BshLambda.isAssignable(bshLambda.dummyType, ListGenerator.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_method_reference_throw_target_error() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval(
            "class MyInt {",
            "   public MyInt() {",
            "       throw new Exception(\"Error inside my constructor\");",
            "   }",
            "}",
            "return MyInt::new"
        );
        TargetError error = Assert.assertThrows(TargetError.class, () -> bshLambda.invokeImpl(new Object[0]));
        Assert.assertTrue(error.getMessage().contains("Error inside my constructor"));
    }

    @Test
    public void test_lambda_expression_not_assignable_invalid_params_length() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> {}");
        Assert.assertFalse(BshLambda.isAssignable(bshLambda.dummyType, Consumer.class, Types.JAVA_BASE_ASSIGNABLE));
    }

    @Test
    public void test_lambda_expression_typed_params() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval(
            "(",
            "   char arg1,",
            "   boolean arg2,",
            "   byte arg3,",
            "   short arg4,",
            "   int arg5,",
            "   long arg6,",
            "   float arg7,",
            "   double arg8,",
            "   Object arg9",
            ") -> {",
            "    return  arg1 == 'a' &&",
            "            arg2 == false &&",
            "            arg3 == 2 &&",
            "            arg4 == 37 &&",
            "            arg5 == 14 &&",
            "            arg6 == 108 &&",
            "            arg7 == 4.08f &&",
            "            arg8 == 921.123d &&",
            "            arg9 == Collections.EMPTY_SET",
            "   ;",
            "};"
        );
        LambdaAllArgs lambda = bshLambda.convertTo(LambdaAllArgs.class);
        Assert.assertTrue(lambda.exec('a', false, (byte) 2, (short) 37, 14, 108, 4.08f, 921.123d, Collections.EMPTY_SET));
        Assert.assertFalse(lambda.exec('b', false, (byte) 2, (short) 37, 14, 108, 4.08f, 921.123d, Collections.EMPTY_SET));
    }

    @Test
    public void test_lambda_expression_invalid_constructor() throws Throwable {
        Node node = null;
        NameSpace nameSpace = null;
        SimpleNode bodyNode = null;

        String expectedMsg = "The length of 'paramsModifiers', 'paramsTypes' and 'paramsNames' can't be different!";

        IllegalArgumentException error1 = Assert.assertThrows(IllegalArgumentException.class, () -> BshLambda.fromLambdaExpression(node, nameSpace, new Modifiers[0], new Class[] { Object.class }, new String[0], bodyNode));
        Assert.assertEquals(expectedMsg, error1.getMessage());

        IllegalArgumentException error2 = Assert.assertThrows(IllegalArgumentException.class, () -> BshLambda.fromLambdaExpression(node, nameSpace, new Modifiers[0], new Class[0], new String[] { "arg1" }, bodyNode));
        Assert.assertEquals(expectedMsg, error2.getMessage());
    }

    @Test
    public void test_lambda_expression_throw_exception() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> { throw new NullPointerException(\"Null error inside lambda\"); }");
        NullPointerException error = Assert.assertThrows(NullPointerException.class, () -> bshLambda.convertTo(Executor.class).exec());
        Assert.assertEquals("Null error inside lambda", error.getMessage());
    }

    @Test
    public void test_lambda_expression_throw_exception_2() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> { throw new NullPointerException(\"Null error inside lambda\"); }");
        NullPointerException error = Assert.assertThrows(NullPointerException.class, () -> bshLambda.convertTo(Executor2.class).exec());
        Assert.assertEquals("Null error inside lambda", error.getMessage());
    }

    @Test
    public void test_lambda_expression_unexpected_exception() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> { throw new NullPointerException(\"My Unexpected Error :P\"); }");
        Runnable lambda = bshLambda.convertTo(Runnable.class);
        RuntimeEvalError error = Assert.assertThrows(RuntimeEvalError.class, () -> lambda.run());
        Assert.assertTrue(error.getMessage().contains("Can't invoke lambda: Unexpected Exception: My Unexpected Error :P"));
    }

    @Test
    public void test_lambda_expression_break() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> { break; }");
        Object result = bshLambda.invoke(new Object[0], new Class[0]);
        Assert.assertEquals(null, result);
    }

    @Test
    public void test_cant_convert() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("Integer::parseInt");
        UtilEvalError error = Assert.assertThrows(UtilEvalError.class, () -> bshLambda.convertTo(ByteSupplier.class));
        Assert.assertTrue(error.getMessage().contains("This BshLambda can't be converted to"));
    }

    @Test
    public void test_lambda_expression_lambda_generator() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> () -> 3");
        IntSupplierGenerator lambda1 = bshLambda.convertTo(IntSupplierGenerator.class);
        IntSupplier lambda2 = lambda1.generate();
        Assert.assertEquals(3, lambda2.getAsInt());
    }

    @Test
    public void test_lambda_expression_return_null() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> null");
        IntSupplierGenerator lambda1 = bshLambda.convertTo(IntSupplierGenerator.class);
        Assert.assertEquals(null, lambda1.generate());
    }

    @Test
    public void test_lambda_expression_cant_assign_return() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> 3");
        IntSupplierGenerator lambda1 = bshLambda.convertTo(IntSupplierGenerator.class);
        RuntimeEvalError error = Assert.assertThrows(RuntimeEvalError.class, () -> lambda1.generate());
        Assert.assertTrue(error.getMessage().contains("Cannot assign Integer with value \"3\" to IntSupplier"));
    }

    @Test // The error of this test must never occurr!! This tests is just for 100% code coverage
    public void test_cant_create_functional_interface_wrapper() throws Throwable {
        BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> {}");
        try (MockedStatic<BshLambda> mockedStatic = Mockito.mockStatic(BshLambda.class)) {
            mockedStatic.when(() -> BshLambda.getClassForFI(Runnable.class)).thenReturn(InvalidBshLambdaWrapper.class);
            mockedStatic.when(() -> BshLambda.isAssignable(bshLambda.dummyType, Runnable.class, Types.BSH_ASSIGNABLE)).thenReturn(true);

            UtilEvalError error = Assert.assertThrows(UtilEvalError.class, () -> bshLambda.convertTo(Runnable.class));
            System.out.println("test_cant_create_functional_interface_wrapper -> error: " + error.getMessage());
            Assert.assertTrue(error.getMessage().contains("Can't create a instance for the generate class for the BshLambda: "));
        }
    }

    @Test
    public void test_lambda_expression_closure() throws Throwable {
        IntSupplier supplier = (IntSupplier) TestUtil.eval(
            "int num = 10;",
            "IntSupplier supplier = () -> num;",
            "num = 20;",
            "return supplier;"
        );
        Assert.assertEquals(10, supplier.getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_closure_1() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i = 0; i < suppliers.length; i++) {",
            "   int num = i * 3;",
            "   suppliers[i] = () -> num;",
            "}",
            "return suppliers;"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_closure_2() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final or effectivelly final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i = 0; i < suppliers.length; i++) {",
            "   suppliers[i] = () -> i * 3;",
            "}",
            "return suppliers;"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_closure_3() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final or effectivelly final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i = 0; i < suppliers.length; i++) suppliers[i] = () -> i * 3;",
            "return suppliers;"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_closure_4() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final/effectivelly-final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i = 0; i < suppliers.length; i++) {",
            "   suppliers[i] = () -> i * 3;",
            "   i++;",
            "}",
            "return suppliers;"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(null, suppliers[1]);
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(null, suppliers[3]);
    }

    @Test
    public void test_for_statement_lambda_expression_closure_5() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final/effectivelly-final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i = 0; i < suppliers.length; i++) {",
            "   i++;",
            "   suppliers[i] = () -> i * 3;",
            "}",
            "return suppliers;"
        );
        Assert.assertEquals(null, suppliers[0]);
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(null, suppliers[2]);
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_closure_6() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final/effectivelly-final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i = 0; i < suppliers.length; i++)",
            "   suppliers[i] = () -> i++ * 3;",
            "return suppliers;"
        );
        RuntimeEvalError error = Assert.assertThrows(RuntimeEvalError.class, () -> suppliers[0].getAsInt());
        Assert.assertTrue(error.getMessage().contains("Cannot re-assign final variable"));

        RuntimeEvalError error2 = Assert.assertThrows(RuntimeEvalError.class, () -> suppliers[1].getAsInt());
        Assert.assertTrue(error2.getMessage().contains("Cannot re-assign final variable"));

        RuntimeEvalError error3 = Assert.assertThrows(RuntimeEvalError.class, () -> suppliers[2].getAsInt());
        Assert.assertTrue(error3.getMessage().contains("Cannot re-assign final variable"));

        RuntimeEvalError error4 = Assert.assertThrows(RuntimeEvalError.class, () -> suppliers[3].getAsInt());
        Assert.assertTrue(error4.getMessage().contains("Cannot re-assign final variable"));
    }

    @Test
    public void test_for_statement_lambda_expression_closure_7() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final/effectivelly-final for lambdas!
        // So this test must ensure it!
        List<IntSupplier> suppliers = (List<IntSupplier>) TestUtil.eval(
            "List<IntSupplier> suppliers = new ArrayList<>();",
            "for (int i = 5; i < 10; i++) {",
            "   for (int i = 0; i < 4; i++) {",
            "       suppliers.add((IntSupplier) () -> i * 3);",
            "   }",
            "}",
            "return suppliers;"
        );
        Assert.assertEquals(20, suppliers.size());
        Assert.assertEquals(0, suppliers.get(0).getAsInt());
        Assert.assertEquals(3, suppliers.get(1).getAsInt());
        Assert.assertEquals(6, suppliers.get(2).getAsInt());
        Assert.assertEquals(9, suppliers.get(3).getAsInt());
        Assert.assertEquals(0, suppliers.get(4).getAsInt());
        Assert.assertEquals(3, suppliers.get(5).getAsInt());
        Assert.assertEquals(6, suppliers.get(6).getAsInt());
        Assert.assertEquals(9, suppliers.get(7).getAsInt());
        Assert.assertEquals(0, suppliers.get(8).getAsInt());
        Assert.assertEquals(3, suppliers.get(9).getAsInt());
        Assert.assertEquals(6, suppliers.get(10).getAsInt());
        Assert.assertEquals(9, suppliers.get(11).getAsInt());
        Assert.assertEquals(0, suppliers.get(12).getAsInt());
        Assert.assertEquals(3, suppliers.get(13).getAsInt());
        Assert.assertEquals(6, suppliers.get(14).getAsInt());
        Assert.assertEquals(9, suppliers.get(15).getAsInt());
        Assert.assertEquals(0, suppliers.get(16).getAsInt());
        Assert.assertEquals(3, suppliers.get(17).getAsInt());
        Assert.assertEquals(6, suppliers.get(18).getAsInt());
        Assert.assertEquals(9, suppliers.get(19).getAsInt());
    }

    @Test
    public void test_for_enhanced_lambda_expression_closure_1() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "int[] indexes = { 0, 1, 2, 3 };",
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i : indexes) {",
            "   int num = i * 3;",
            "   suppliers[i] = () -> num;",
            "}",
            "return suppliers;"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_enhanced_lambda_expression_closure_2() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final or effectivelly final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "int[] indexes = { 0, 1, 2, 3 };",
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i : indexes) {",
            "   suppliers[i] = () -> i * 3;",
            "}",
            "return suppliers;"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_enhanced_lambda_expression_closure_3() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final or effectivelly final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "int[] indexes = { 0, 1, 2, 3 };",
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i : indexes) suppliers[i] = () -> i * 3;",
            "return suppliers;"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_enhanced_lambda_expression_closure_4() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final/effectivelly-final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "int[] indexes = { 0, 1, 2, 3 };",
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i : indexes) {",
            "   suppliers[i] = () -> i * 3;",
            "   i++;",
            "}",
            "return suppliers;"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_enhanced_lambda_expression_closure_5() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final/effectivelly-final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "int[] indexes = { -1, 0, 1, 2 };",
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i : indexes) {",
            "   i++;",
            "   suppliers[i] = () -> i * 3;",
            "}",
            "return suppliers;"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_enhanced_lambda_expression_closure_6() throws Throwable {
        // In Java we can't use the '() -> i' expression
        // but all scoped variables must be final/effectivelly-final for lambdas!
        // So this test must ensure it!
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "int[] indexes = { 0, 1, 2, 3 };",
            "IntSupplier[] suppliers = new IntSupplier[4];",
            "for (int i : indexes)",
            "   suppliers[i] = () -> i++ * 3;",
            "return suppliers;"
        );
        RuntimeEvalError error = Assert.assertThrows(RuntimeEvalError.class, () -> suppliers[0].getAsInt());
        Assert.assertTrue(error.getMessage().contains("Cannot re-assign final variable"));

        RuntimeEvalError error2 = Assert.assertThrows(RuntimeEvalError.class, () -> suppliers[1].getAsInt());
        Assert.assertTrue(error2.getMessage().contains("Cannot re-assign final variable"));

        RuntimeEvalError error3 = Assert.assertThrows(RuntimeEvalError.class, () -> suppliers[2].getAsInt());
        Assert.assertTrue(error3.getMessage().contains("Cannot re-assign final variable"));

        RuntimeEvalError error4 = Assert.assertThrows(RuntimeEvalError.class, () -> suppliers[3].getAsInt());
        Assert.assertTrue(error4.getMessage().contains("Cannot re-assign final variable"));
    }

    @Test
    public void test_for_statement_lambda_expression_static_class_members_closure_1() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "    static int i = 0;",
            "    static IntSupplier[] getSuppliers() {",
            "        IntSupplier[] suppliers = new IntSupplier[4];",
            "        for (; i < suppliers.length; i++) {",
            "           int num = i * 3;",
            "           suppliers[i] = () -> num;",
            "        }",
            "        return suppliers;",
            "    }",
            "}",
            "return Foo.getSuppliers();"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_static_class_members_closure_2() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "    static int i = 0;",
            "    static IntSupplier[] getSuppliers() {",
            "        IntSupplier[] suppliers = new IntSupplier[4];",
            "        for (; i < suppliers.length; i++) {",
            "           suppliers[i] = () -> i * 3;",
            "        }",
            "        return suppliers;",
            "    }",
            "}",
            "return Foo.getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(12, suppliers[1].getAsInt());
        Assert.assertEquals(12, suppliers[2].getAsInt());
        Assert.assertEquals(12, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_static_class_members_closure_3() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "    static int i = 0;",
            "    static IntSupplier[] getSuppliers() {",
            "        IntSupplier[] suppliers = new IntSupplier[4];",
            "        for (; i < suppliers.length; i++) suppliers[i] = () -> i * 3;",
            "        return suppliers;",
            "    }",
            "}",
            "return Foo.getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(12, suppliers[1].getAsInt());
        Assert.assertEquals(12, suppliers[2].getAsInt());
        Assert.assertEquals(12, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_static_class_members_closure_4() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "   static int i = 0;",
            "   static IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++) {",
            "           suppliers[i] = () -> i * 3;",
            "           i++;",
            "       }",
            "       return suppliers;",
            "   }",
            "}",
            "return Foo.getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(null, suppliers[1]);
        Assert.assertEquals(12, suppliers[2].getAsInt());
        Assert.assertEquals(null, suppliers[3]);
    }

    @Test
    public void test_for_statement_lambda_expression_static_class_members_closure_5() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "   static int i = 0;",
            "   static IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++) {",
            "           i++;",
            "           suppliers[i] = () -> i * 3;",
            "       }",
            "       return suppliers;",
            "   }",
            "}",
            "return Foo.getSuppliers();"
        );
        Assert.assertEquals(null, suppliers[0]);
        Assert.assertEquals(12, suppliers[1].getAsInt());
        Assert.assertEquals(null, suppliers[2]);
        Assert.assertEquals(12, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_static_class_members_closure_6() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "   static int i = 0;",
            "   static IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++)",
            "          suppliers[i] = () -> i++ * 3;",
            "       return suppliers;",
            "   }",
            "}",
            "return Foo.getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(15, suppliers[1].getAsInt());
        Assert.assertEquals(18, suppliers[2].getAsInt());
        Assert.assertEquals(21, suppliers[3].getAsInt());

        Assert.assertEquals(24, suppliers[0].getAsInt());
        Assert.assertEquals(27, suppliers[1].getAsInt());
        Assert.assertEquals(30, suppliers[2].getAsInt());
        Assert.assertEquals(33, suppliers[3].getAsInt());

        Assert.assertEquals(36, suppliers[0].getAsInt());
        Assert.assertEquals(39, suppliers[1].getAsInt());
        Assert.assertEquals(42, suppliers[2].getAsInt());
        Assert.assertEquals(45, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_class_members_closure_1() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "    int i = 0;",
            "    IntSupplier[] getSuppliers() {",
            "        IntSupplier[] suppliers = new IntSupplier[4];",
            "        for (; i < suppliers.length; i++) {",
            "           int num = i * 3;",
            "           suppliers[i] = () -> num;",
            "        }",
            "        return suppliers;",
            "    }",
            "}",
            "return new Foo().getSuppliers();"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_class_members_closure_2() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "    int i = 0;",
            "    IntSupplier[] getSuppliers() {",
            "        IntSupplier[] suppliers = new IntSupplier[4];",
            "        for (; i < suppliers.length; i++) {",
            "           suppliers[i] = () -> i * 3;",
            "        }",
            "        return suppliers;",
            "    }",
            "}",
            "return new Foo().getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(12, suppliers[1].getAsInt());
        Assert.assertEquals(12, suppliers[2].getAsInt());
        Assert.assertEquals(12, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_class_members_closure_3() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "    int i = 0;",
            "    IntSupplier[] getSuppliers() {",
            "        IntSupplier[] suppliers = new IntSupplier[4];",
            "        for (; i < suppliers.length; i++) suppliers[i] = () -> i * 3;",
            "        return suppliers;",
            "    }",
            "}",
            "return new Foo().getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(12, suppliers[1].getAsInt());
        Assert.assertEquals(12, suppliers[2].getAsInt());
        Assert.assertEquals(12, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_class_members_closure_4() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "   int i = 0;",
            "   IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++) {",
            "           suppliers[i] = () -> i * 3;",
            "           i++;",
            "       }",
            "       return suppliers;",
            "   }",
            "}",
            "return new Foo().getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(null, suppliers[1]);
        Assert.assertEquals(12, suppliers[2].getAsInt());
        Assert.assertEquals(null, suppliers[3]);
    }

    @Test
    public void test_for_statement_lambda_expression_class_members_closure_5() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "   int i = 0;",
            "   IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++) {",
            "           i++;",
            "           suppliers[i] = () -> i * 3;",
            "       }",
            "       return suppliers;",
            "   }",
            "}",
            "return new Foo().getSuppliers();"
        );
        Assert.assertEquals(null, suppliers[0]);
        Assert.assertEquals(12, suppliers[1].getAsInt());
        Assert.assertEquals(null, suppliers[2]);
        Assert.assertEquals(12, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_class_members_closure_6() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "class Foo {",
            "   int i = 0;",
            "   IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++)",
            "          suppliers[i] = () -> i++ * 3;",
            "       return suppliers;",
            "   }",
            "}",
            "return new Foo().getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(15, suppliers[1].getAsInt());
        Assert.assertEquals(18, suppliers[2].getAsInt());
        Assert.assertEquals(21, suppliers[3].getAsInt());

        Assert.assertEquals(24, suppliers[0].getAsInt());
        Assert.assertEquals(27, suppliers[1].getAsInt());
        Assert.assertEquals(30, suppliers[2].getAsInt());
        Assert.assertEquals(33, suppliers[3].getAsInt());

        Assert.assertEquals(36, suppliers[0].getAsInt());
        Assert.assertEquals(39, suppliers[1].getAsInt());
        Assert.assertEquals(42, suppliers[2].getAsInt());
        Assert.assertEquals(45, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_enum_members_closure_1() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "enum Foo {",
            "   FIRST(), SECOND();",
            "   Foo() {}",
            "   int i = 0;",
            "   IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++) {",
            "           int num = i * 3;",
            "           suppliers[i] = () -> num;",
            "       }",
            "       return suppliers;",
            "   }",
            "}",
            "return Foo.FIRST.getSuppliers();"
        );
        Assert.assertEquals(0, suppliers[0].getAsInt());
        Assert.assertEquals(3, suppliers[1].getAsInt());
        Assert.assertEquals(6, suppliers[2].getAsInt());
        Assert.assertEquals(9, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_enum_members_closure_2() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "enum Foo {",
            "   FIRST(), SECOND();",
            "   Foo() {}",
            "   int i = 0;",
            "   IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "        for (; i < suppliers.length; i++) {",
            "           suppliers[i] = () -> i * 3;",
            "        }",
            "       return suppliers;",
            "   }",
            "}",
            "return Foo.FIRST.getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(12, suppliers[1].getAsInt());
        Assert.assertEquals(12, suppliers[2].getAsInt());
        Assert.assertEquals(12, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_enum_members_closure_3() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "enum Foo {",
            "   FIRST(), SECOND();",
            "   Foo() {}",
            "   int i = 0;",
            "   IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "        for (; i < suppliers.length; i++) suppliers[i] = () -> i * 3;",
            "       return suppliers;",
            "   }",
            "}",
            "return Foo.FIRST.getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(12, suppliers[1].getAsInt());
        Assert.assertEquals(12, suppliers[2].getAsInt());
        Assert.assertEquals(12, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_enum_members_closure_4() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "enum Foo {",
            "   FIRST(), SECOND();",
            "   Foo() {}",
            "   int i = 0;",
            "   IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++) {",
            "           suppliers[i] = () -> i * 3;",
            "           i++;",
            "       }",
            "       return suppliers;",
            "   }",
            "}",
            "return Foo.FIRST.getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(null, suppliers[1]);
        Assert.assertEquals(12, suppliers[2].getAsInt());
        Assert.assertEquals(null, suppliers[3]);
    }

    @Test
    public void test_for_statement_lambda_expression_enum_members_closure_5() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "enum Foo {",
            "   FIRST(), SECOND();",
            "   Foo() {}",
            "   int i = 0;",
            "   IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++) {",
            "           i++;",
            "           suppliers[i] = () -> i * 3;",
            "       }",
            "       return suppliers;",
            "   }",
            "}",
            "return Foo.FIRST.getSuppliers();"
        );
        Assert.assertEquals(null, suppliers[0]);
        Assert.assertEquals(12, suppliers[1].getAsInt());
        Assert.assertEquals(null, suppliers[2]);
        Assert.assertEquals(12, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_lambda_expression_enum_members_closure_6() throws Throwable {
        IntSupplier[] suppliers = (IntSupplier[]) TestUtil.eval(
            "enum Foo {",
            "   FIRST(), SECOND();",
            "   Foo() {}",
            "   int i = 0;",
            "   IntSupplier[] getSuppliers() {",
            "       IntSupplier[] suppliers = new IntSupplier[4];",
            "       for (; i < suppliers.length; i++)",
            "          suppliers[i] = () -> i++ * 3;",
            "       return suppliers;",
            "   }",
            "}",
            "return Foo.FIRST.getSuppliers();"
        );
        Assert.assertEquals(12, suppliers[0].getAsInt());
        Assert.assertEquals(15, suppliers[1].getAsInt());
        Assert.assertEquals(18, suppliers[2].getAsInt());
        Assert.assertEquals(21, suppliers[3].getAsInt());

        Assert.assertEquals(24, suppliers[0].getAsInt());
        Assert.assertEquals(27, suppliers[1].getAsInt());
        Assert.assertEquals(30, suppliers[2].getAsInt());
        Assert.assertEquals(33, suppliers[3].getAsInt());

        Assert.assertEquals(36, suppliers[0].getAsInt());
        Assert.assertEquals(39, suppliers[1].getAsInt());
        Assert.assertEquals(42, suppliers[2].getAsInt());
        Assert.assertEquals(45, suppliers[3].getAsInt());
    }

    @Test
    public void test_for_statement_method_reference_1() throws Throwable {
        Predicate<Object>[] predicators = (Predicate<Object>[]) TestUtil.eval(
            "Predicate<Object>[] predicators = new Predicate<Object>[4];",
            "for (int i = 0; i < predicators.length; i++) {",
            "   predicators[i] = (i+\"\")::equals;",
            "}",
            "return predicators;"
        );
        Assert.assertTrue(predicators[0].test("0"));
        Assert.assertTrue(predicators[1].test("1"));
        Assert.assertTrue(predicators[2].test("2"));
        Assert.assertTrue(predicators[3].test("3"));
    }

    @Test
    public void test_for_statement_method_reference_2() throws Throwable {
        Predicate<Object>[] predicators = (Predicate<Object>[]) TestUtil.eval(
            "Predicate<Object>[] predicators = new Predicate<Object>[4];",
            "for (int i = 0; i < predicators.length; i++) {",
            "   predicators[i] = i.toString()::equals;",
            "}",
            "return predicators;"
        );
        Assert.assertTrue(predicators[0].test("0"));
        Assert.assertTrue(predicators[1].test("1"));
        Assert.assertTrue(predicators[2].test("2"));
        Assert.assertTrue(predicators[3].test("3"));
    }

    @Test
    public void test_for_statement_method_reference_3() throws Throwable {
        Predicate<Object>[] predicators = (Predicate<Object>[]) TestUtil.eval(
            "String[] items = { \"0\", \"1\", \"2\", \"3\" };",
            "Predicate<Object>[] predicators = new Predicate<Object>[4];",
            "for (String i : items) {",
            "   predicators[Integer.parseInt(i)] = i::equals;",
            "}",
            "return predicators;"
        );
        Assert.assertTrue(predicators[0].test("0"));
        Assert.assertTrue(predicators[1].test("1"));
        Assert.assertTrue(predicators[2].test("2"));
        Assert.assertTrue(predicators[3].test("3"));
    }

    @Test
    public void test_method_reference_namespace() throws Throwable {
        Predicate<Object> predicate = (Predicate<Object>) TestUtil.eval(
            "String num = \"10\";",
            "Predicate<Object> predicate = num::equals;",
            "num = \"20\";",
            "return predicate;"
        );
        Assert.assertTrue(predicate.test("10"));
    }

    @Test
    public void test_invalid_method_reference() throws Throwable {
        EvalError error = Assert.assertThrows(EvalError.class, () -> TestUtil.eval("new ArrayList()::equals.toString();"));
        Assert.assertTrue(error.getMessage().contains("Method Reference must be the last suffix!"));
    }


    //////////////////////////////////////////////

    @Test
    public void test_lambda_expression_return_type_1() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> {}");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, Runnable.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_2() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> {}");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_3() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> 3");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_4() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> 3");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, Runnable.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_5() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> new ArrayList()");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, ListSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_6() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> new ArrayList()");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_7() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.EMPTY_LIST");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, ListSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_8() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.EMPTY_LIST");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_9() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.emptyMap()");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, MapSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_10() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.emptyMap()");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_11() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("p -> p");
        boolean assignable1 = BshLambda.isAssignable(lambda.dummyType, Function.class, Types.JAVA_BOX_TYPES_ASSIGABLE);
        Assert.assertTrue(assignable1);

        boolean assignable2 = BshLambda.isAssignable(lambda.dummyType, Function.class, Types.JAVA_BOX_TYPES_ASSIGABLE);
        Assert.assertTrue(assignable2);
    }

    @Test
    public void test_lambda_expression_return_type_12() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("p -> p");
        boolean assignable1 = BshLambda.isAssignable(lambda.dummyType, IntFunction.class, Types.JAVA_BOX_TYPES_ASSIGABLE);
        Assert.assertFalse(assignable1);

        boolean assignable2 = BshLambda.isAssignable(lambda.dummyType, ListGenerator.class, Types.JAVA_BOX_TYPES_ASSIGABLE);
        Assert.assertFalse(assignable2);
    }

    @Test
    public void test_lambda_expression_return_type_13() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> new Integer(123).toString().length()");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BOX_TYPES_ASSIGABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_14() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> new Integer(123).toString().length()");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, ListSupplier.class, Types.JAVA_BOX_TYPES_ASSIGABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_15() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.class");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, ClassSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_16() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.class");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_17() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> new List[] { new ArrayList<>() }[0].size()");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_18() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> new List[] { new ArrayList<>() }[0].size()");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, Runnable.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_19() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> new List[] { new ArrayList<>() }.length");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_20() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> new List[] { new ArrayList<>() }.length");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, Runnable.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_21() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> \"123\".toString()::equals");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, PredicateSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_22() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> \"123\".toString()::equals");
        boolean assignable1 = BshLambda.isAssignable(lambda.dummyType, Runnable.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable1);

        boolean assignable2 = BshLambda.isAssignable(lambda.dummyType, Supplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable2);
    }

    @Test
    public void test_lambda_expression_return_type_23() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> () -> 3");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplierGenerator.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_24() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("() -> () -> 3");
        boolean assignable1 = BshLambda.isAssignable(lambda.dummyType, Runnable.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable1);

        boolean assignable2 = BshLambda.isAssignable(lambda.dummyType, Supplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable2);
    }

    @Test
    public void test_lambda_expression_return_type_25() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("return () -> 3 + 4;");
        boolean assignable = BshLambda.isAssignable(lambda.dummyType, IntSupplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertTrue(assignable);
    }

    @Test
    public void test_lambda_expression_return_type_26() throws Throwable {
        BshLambda lambda = (BshLambda) TestUtil.eval("return () -> 3 + 4;");
        boolean assignable1 = BshLambda.isAssignable(lambda.dummyType, Runnable.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable1);

        boolean assignable2 = BshLambda.isAssignable(lambda.dummyType, Supplier.class, Types.JAVA_BASE_ASSIGNABLE);
        Assert.assertFalse(assignable2);
    }

    // BshLambda lambda = (BshLambda) TestUtil.eval("() -> (IntSupplier) () -> 3");
    // BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.EMPTY_LIST");
    // BshLambda lambda = (BshLambda) TestUtil.eval("() -> 123.toString()");
    // BshLambda lambda = (BshLambda) TestUtil.eval("() -> 123.toString()::equals");
    // BshLambda lambda = (BshLambda) TestUtil.eval("() -> Collections.class");
    // BshLambda lambda = (BshLambda) TestUtil.eval("() -> new List[] { new ArrayList<>() }[0].size()");

    // TODO:
    // 1. teste com lambda com referência para upper variable
    // 2. teste com lambda com referência para class member
    // 3. teste com lambda com referência para inline import

    // TODO: teste com 'this::mapper'
    // TODO: 'Supplier func = () -> () -> 123' deveria dar erro pois BshLambda não é assignable para Object!
    // TODO: testes com '() -> { return 123; };', '() -> { if (myVar == 123) return new ArrayList(); return new HashMap(); }', '() -> (myVar == 123) ? new ArrayList() : return new HashMap()'

    // public List<Class<?>> findAllClassesUsingClassLoader(String packageName) {
    //     InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(packageName.replaceAll("[.]", "/"));
    //     BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    //     return reader.lines()
    //       .filter(line -> line.endsWith(".class"))
    //       .map(line -> getClass(line, packageName))
    //       .collect(Collectors.toList());
    // }

    // private Class getClass(String className, String packageName) {
    //     try {
    //         return Class.forName(packageName + "."
    //           + className.substring(0, className.lastIndexOf('.')));
    //     } catch (ClassNotFoundException e) {
    //         // handle the exception
    //     }
    //     return null;
    // }

    static class _Test {
        private int bsh;

        void method() {
            // bsh.
        }
    }

    @Test
    public void test() throws Throwable {
        // InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream("packageName".replaceAll("[.]", "/"));

        // BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        // IntSupplier[] suppliers = (new Foo() {}).getSuppliers();
        // System.out.println("suppliers[0].getAsInt(): ");
        // class MyLoader extends ClassLoader {
        //     public MyLoader(ClassLoader parent) {
        //         super(parent);
        //     }

        //     Package[] getAllPackages() {
        //         return this.getPackages();
        //     }
        // }

        // System.out.println("resources: ");
        // Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources("java");
        // while (resources.hasMoreElements()) {
        //     System.out.println("    -> " + resources.nextElement());
        // }

        // System.out.println("Class Loaders: ");
        // for (ClassLoader cl = Thread.currentThread().getContextClassLoader(); cl != null; cl = cl.getParent()) {
        //     System.out.println("    -> Class Loader: " + cl);
        //     System.out.println("     - Packages:");
        //     for (Package pkg: cl.getDefinedPackages()) {
        //         System.out.println("         -> " + pkg);
        //     }
        // }
        // System.out.println("My Loader: all packages");
        // for (Package pkg: new MyLoader(ClassLoader.getSystemClassLoader()).getAllPackages()) {
        //     System.out.println("    -> " + pkg);
        // }

        // System.out.println("Class Loader: defined packages: ");
        // for (Package pkg: ClassLoader.getSystemClassLoader().getDefinedPackages()) {
        //     System.out.println("    -> " + pkg);
        // }

        // System.out.println("packages: ");
        // for (Package pkg: Package.getPackages()) {
        //     System.out.println("    -> " + pkg);
        // }

        // NameSpace nameSpace = new NameSpace("");
        // System.out.println("Class Manager packages:");
        // for (String pkg: nameSpace.getClassManager().getPackages()) {
        //     System.out.println("    -> " + pkg);
        // }

        // ClassLoader.getSystemClassLoader().getPackages
        // ClassLoader.getSystemClassLoader().getResources("java");

        // System.out.println("Type test: " + Types.isAssignable(null, Object.class, Types.JAVA_BASE_ASSIGNABLE));
        try {

            String name = "java.util.List.of";
            int lastDot = name.lastIndexOf('.');
            String className = name.substring(0, lastDot);
            String memberName = name.substring(lastDot+1);

            System.out.println("className: " + className);
            System.out.println("memberName: " + memberName);

            Object result = TestUtil.eval(
                "package bsh.test;"
            );
            // Object result = TestUtil.eval(
            //     // "bsh.system.abcd.xyz = 22;"
            //     "class MyClass {",
            //     "   setN() { num = 123; }",
            //     "   getN() { return num; }",
            //     "}",
            //     "MyClass obj = new MyClass();",
            //     "obj.setN();",
            //     "return obj.getN();"
            // );
            // System.out.println("result: " + result);
            // System.out.println("parent packages: ");
            // for (Package _package : classLoader.getParent().getParent().getDefinedPackages()) {
            //     System.out.println("    -> " + _package);
            // }
            // BshLambda lambda = (BshLambda) TestUtil.eval("new Integer(123).toString()");
            // BshLambda bshLambda = (BshLambda) TestUtil.eval("() -> {}");
            // bshLambda.convertTo(Object.class);
        } catch (Throwable t) {
            // // System.out.println("Throwable.message: " + t.getMessage());
            // System.out.println("Throwable.class: " + t.getClass());

            // PipedOutputStream pout = new PipedOutputStream();
            // pout.write(new byte[0]);

            // // System.out.println("Throwable: " + t.getMessage());
            System.out.println("--------------------------------------");
            t.printStackTrace();
            System.out.println("--------------------------------------");
            // t.getCause().printStackTrace();
            // System.out.println("--------------------------------------");
            // t.getCause().getCause().printStackTrace();
            // System.out.println("--------------------------------------");
        }

        // this.method(() -> null);
        // BshLambda lambda = (BshLambda) TestUtil.eval("(num) -> num * 2");
        // System.out.println(lambda.dummyType);
        // Function<Integer, Integer> mapper = lambda.to(Function.class);

        // List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        // List<Integer> resultList = list.stream().map(mapper).collect(Collectors.toList());

        // System.out.println("list: " + list);
        // System.out.println("resultList: " + resultList);


        // BshLambda lambda = (BshLambda) TestUtil.eval("System.out::println");
        // System.out.println("result: " + lambda.invoke("Hello World :D"));

        // BshLambda lambda = (BshLambda) TestUtil.eval("Collections::emptyList");
        // System.out.println("result: " + lambda.invoke(new Object[0], new Class[0]));

        // BshLambda lambda = (BshLambda) TestUtil.eval("() -> {}");
        // BshLambda lambda = (BshLambda) TestUtil.eval("abc -> {}");
        // BshLambda lambda = (BshLambda) TestUtil.eval("abc -> { 123; }");

        // BshLambda lambda = (BshLambda) TestUtil.eval("(abc) -> {}");
        // BshLambda lambda = (BshLambda) TestUtil.eval(
        //     "(abc, def, ghi) -> {",
        //     "   var jose = 123456;",
        //     "   return jose;",
        //     "}"
        // );

        // BshLambda lambda = (BshLambda) TestUtil.eval("() -> 123456");
        // BshLambda lambda = (BshLambda) TestUtil.eval("abc -> 123456");
        // BshLambda lambda = (BshLambda) TestUtil.eval("(abc) -> 123456");
        // BshLambda lambda = (BshLambda) TestUtil.eval("(abc, def, ghi) -> 123456");
        // BshLambda lambda = (BshLambda) TestUtil.eval("(final String abc, def, Object ghi) -> 123456");

        // BshLambda lambda = (BshLambda) TestUtil.eval("() -> 123");
        // BshLambda lambda = (BshLambda) TestUtil.eval("() -> 123");
        // BshLambda lambda = (BshLambda) TestUtil.eval("() -> {}");
        // BshLambda lambda = (BshLambda) TestUtil.eval("System.out::println");

        // BshLambda lambda = (BshLambda) TestUtil.eval("() -> () -> 123");
        // Object[] result = lambda.invokeObject(Object[].class, null, null, null);
        // Object result = lambda.invoke(null, null, null, null);
        // System.out.println("result: " + result);
    }

}
