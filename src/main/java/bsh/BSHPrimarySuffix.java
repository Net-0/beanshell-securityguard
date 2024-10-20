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
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/
package bsh;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import bsh.security.SecurityError;

class BSHPrimarySuffix extends SimpleNode {
    public static final int CLASS = 6,
            INDEX = 1,
            // NAME = 2,
            METHOD = 7,
            FIELD = 8,
            PROPERTY = 3,
            NEW = 4; // TODO: verificar isso

    public int operation;
    Object index;
    public String memberName;
    public boolean slice = false, step = false,
            hasLeftIndex = false, hasRightIndex = false,
            safeNavigate = false;

    BSHPrimarySuffix(int id) {
        super(id);
    }

    /*
     * Perform a suffix operation on the given object and return the
     * new value.
     * <p>
     * 
     * obj will be a Node when suffix evaluation begins, allowing us to
     * interpret it contextually. (e.g. for .class) Thereafter it will be
     * a value object or LHS (as determined by toLHS).
     * <p>
     * 
     * We must handle the toLHS case at each point here.
     * <p>
     */
    // TODO: remover o 'toLHS'!
    public Object doSuffix(Object baseObj, CallStack callStack, Interpreter interpreter) throws EvalError {
        // // Handle ".class" suffix operation
        // // Prefix must be a BSHType
        // if (operation == CLASS)
        // if (obj instanceof BSHType) {
        // return ((BSHType)obj).getType( callStack, interpreter );
        // } else
        // throw new EvalException("Attempt to use .class suffix on non class.", this,
        // callStack );

        // /*
        // Evaluate our prefix if it needs evaluating first.
        // If this is the first evaluation our prefix mayb be a Node
        // (directly from the PrimaryPrefix) - eval() it to an object.
        // If it's an LHS, resolve to a value.

        // Note: The ambiguous name construct is now necessary where the node
        // may be an ambiguous name. If this becomes common we might want to
        // make a static method nodeToObject() or something. The point is
        // that we can't just eval() - we need to direct the evaluation to
        // the context sensitive type of result; namely object, class, etc.
        // */
        // if (obj instanceof Node)
        // if (obj instanceof BSHAmbiguousName)
        // obj = ((BSHAmbiguousName)obj).toObject(callstack, interpreter);
        // else
        // obj = ((Node)obj).eval(callstack, interpreter);
        // else
        // if (obj instanceof LHS)
        // try {
        // obj = ((LHS)obj).getValue();
        // } catch ( UtilEvalError e ) {
        // throw e.toEvalError( this, callstack );
        // }

        try {
            switch (operation) { // TODO: cade o method invocation ?????
                case CLASS:
                    return doClass(baseObj, callStack, interpreter);
                case INDEX:
                    return doIndex(baseObj, callStack, interpreter);
                case METHOD:
                    return doMethod(baseObj, callStack, interpreter);
                case FIELD:
                    return doField(baseObj, callStack, interpreter);
                case PROPERTY:
                    return doProperty(baseObj, callStack, interpreter);
                case NEW:
                    return doNewInner(baseObj, callStack, interpreter);
                default:
                    throw new InterpreterError("Unknown suffix type");
            }
        } catch (ReflectError e) {
            throw new EvalError("reflection error: " + e, this, callStack, e);
        } catch (UtilEvalError e) {
            throw e.toEvalError(this, callStack);
        }
    }

    private Object doClass(Object baseObj, CallStack callStack, Interpreter interpreter) throws EvalError {
        // if (operation == CLASS)
        // TODO: verificar isso
        // if (obj instanceof BSHType) {
        // return ((BSHType)obj).getType( callstack, interpreter );
        // }

        if (baseObj instanceof ClassIdentifier)
            return ((ClassIdentifier) baseObj).getTargetClass();

        throw new EvalException("Attempt to use .class suffix on non class.", this, callStack);
    }

    /**
     * Array index or bracket expression implementation.
     * 
     * @param baseObj     array or list instance
     * @param callstack   the evaluation call stack
     * @param interpreter the evaluation interpreter
     * @return data as per index expression or LHS for assignment
     * @throws EvalError with evaluation exceptions
     */
    private Object doIndex(Object baseObj, CallStack callstack, Interpreter interpreter) throws ReflectError, EvalError, UtilEvalError {
        // TODO: verificar esse método!
        final boolean strictJava = interpreter.getStrictJava();

        // Map or Entry index access not applicable to strict java
        if (!strictJava) {
            // allow index access for Map
            if (baseObj instanceof Map) {
                final Object key = jjtGetChild(0).eval(callstack, interpreter);
                return Reflect.getObjectProperty(baseObj, key, callstack, strictJava);
            }

            // allow index access for Map.Entry
            if (baseObj instanceof Entry<?, ?>) {
                final Object key = jjtGetChild(0).eval(callstack, interpreter);
                return Reflect.getObjectProperty(baseObj, key, callstack, strictJava);
            }
        }

        final Class<?> _class = baseObj.getClass();
        if ((strictJava || !(baseObj instanceof List<?>)) && !_class.isArray())
            throw new EvalError("Not an array or List type", this, callstack);

        final int length = baseObj instanceof List<?> ? ((List<?>) baseObj).size() : Array.getLength(baseObj);

        // allow index access for a Map.Entry array.
        if (!strictJava && Entry[].class.isAssignableFrom(_class)) {
            final Object key = this.jjtGetChild(0).eval(callstack, interpreter);

            if ((key instanceof Primitive && ((Primitive) key).isNumber()) || Primitive.isWrapperType(key.getClass())) {
                int _index = (int) Primitive.castWrapper(Integer.TYPE, key);
                if (length < _index || -length > _index) // It'd be an index out of bounds, so try to solve as a property!
                    return Reflect.getObjectProperty(baseObj, key, callstack, strictJava);
            } else {
                return Reflect.getObjectProperty(baseObj, key, callstack, strictJava);
            }
        }

        int index = getIndexAux(baseObj, 0, callstack, interpreter, this);

        // Negative index or slice expressions not applicable to strict java
        if (!strictJava) {
            if (0 > index)
                index = length + index;
            if (this.slice) {
                int rindex = 0, stepby = 0;
                if (this.step) {
                    Integer step = null;
                    if (hasLeftIndex && hasRightIndex && jjtGetNumChildren() == 3)
                        step = getIndexAux(baseObj, 2, callstack, interpreter, this);
                    else if ((!hasLeftIndex || !hasRightIndex) && jjtGetNumChildren() == 2)
                        step = getIndexAux(baseObj, 1, callstack, interpreter, this);
                    else if (!hasLeftIndex && !hasRightIndex) {
                        step = getIndexAux(baseObj, 0, callstack, interpreter, this);
                        index = 0;
                    }
                    if (null != step) {
                        if (step == 0)
                            throw new UtilEvalError("array slice step cannot be zero");
                        stepby = step;
                    }
                }
                if (hasLeftIndex && hasRightIndex)
                    rindex = getIndexAux(baseObj, 1, callstack, interpreter, this);
                else if (!hasRightIndex)
                    rindex = length;
                else {
                    rindex = index;
                    index = 0;
                }
                if (0 > rindex)
                    rindex = length + rindex;
                if (baseObj.getClass().isArray())
                    return BshArray.slice(baseObj, index, rindex, stepby);
                return BshArray.slice((List<Object>) baseObj, index, rindex, stepby);
            }
        } else if (this.slice)
            throw new EvalError("expected ']' but found ':'", this, callstack);

        // try {
        return BshArray.getIndex(baseObj, index);
        // } catch ( UtilEvalError e ) {
        // throw e.toEvalError("Error array get index", this, callstack);
        // }
    }

    private Object doMethod(Object obj, CallStack callStack, Interpreter interpreter) throws ReflectError, EvalError, UtilEvalError {
        // Safe Navigate operator ?. abort on null
        if (this.safeNavigate && Primitive.NULL == obj)
            throw SafeNavigate.doAbort();

        // Method invocation
        final BSHArguments argsNode = this.jjtGetChild(0);
        final Object[] args = argsNode.getArguments(callStack, interpreter);

        try {
            if (obj instanceof ClassIdentifier) {
                Class<?> _class = ((ClassIdentifier) obj).clas;
                return Reflect.invokeStaticMethod(_class, memberName, args, callStack, interpreter.getStrictJava());
            }

            return Reflect.invokeMethod(obj, this.memberName, args, callStack, interpreter.getStrictJava());
        } catch (NoSuchMethodException e) {
            // final Class<?> _class = Types.getType(obj);
            // final Class<?>[] argsTypes = Reflect.getTypes(args);
            // final String msg = String.format("The method %s(%s) is undefined for the type
            // %s", this.memberName, String.join(", ", Reflect.prettyNames(argsTypes)),
            // Types.prettyName(_class));
            // throw new EvalError(msg, this, callStack);
            throw new EvalError(e.getMessage(), this, callStack);
        }
    }

    private Object doField(Object obj, CallStack callStack, Interpreter interpreter) throws ReflectError, EvalError, UtilEvalError {
        // Safe Navigate operator ?. abort on null
        if (this.safeNavigate && Primitive.NULL == obj) // TODO: oq fazer ? Isso n é strictJava!
            throw SafeNavigate.doAbort();

        final boolean strictJava = interpreter.getStrictJava();

        try {
            return Reflect.getField(obj, memberName, null, strictJava);
        } catch (NoSuchFieldException e) {
            // TODO: see it better later
            if (interpreter.getStrictJava())
                throw new EvalError(e.getMessage(), this, callStack);
            // TODO: see it better later
            return Reflect.getObjectProperty(obj, memberName, null, strictJava);
        }
    }

    /** Property access. */
    private Object doProperty(Object obj, CallStack callStack, Interpreter interpreter) throws ReflectError, EvalError, UtilEvalError {
        final boolean strictJava = interpreter.getStrictJava();
        if (strictJava)
            throw new EvalError("Property expression aren't java strict!", this, callStack);

        if (obj == Primitive.VOID)
            throw new EvalError("Attempt to access property on undefined variable or class name", this, callStack);

        if (obj instanceof Primitive)
            throw new EvalError("Attempt to access property on a primitive", this, callStack);

        Object value = this.jjtGetChild(0).eval(callStack, interpreter);

        if (!(value instanceof String))
            throw new EvalError("Property expression must be a String or identifier.", this, callStack);

        // TODO: verificar esse callStack.top()
        Object val = Reflect.getObjectProperty(obj, (String) value, callStack, strictJava);
        return null == val ? Primitive.NULL : Primitive.unwrap(val);
    }

    /*
     * Instance.new InnerClass() implementation
     */
    private Object doNewInner(Object obj, CallStack callstack, Interpreter interpreter) throws EvalError {
        BSHAllocationExpression alloc = (BSHAllocationExpression) jjtGetChild(0);
        if (Reflect.isGeneratedClass(obj.getClass())) {
            callstack.pop();
            // callstack.push(Reflect.getThisNS(obj)); // TODO: verificar isso!
            return alloc.eval(callstack, interpreter);
        }

        return alloc.constructFromEnclosingInstance(obj, callstack, interpreter);
    }

    private static int getIndexAux(Object obj, int idx, CallStack callstack, Interpreter interpreter, Node callerInfo)
            throws EvalError {
        int index;
        try {
            Object indexVal = callerInfo.jjtGetChild(idx).eval(callstack, interpreter);
            if (!(indexVal instanceof Primitive))
                indexVal = Types.castObject(indexVal, Integer.TYPE, Types.ASSIGNMENT);
            index = (int) Primitive.castWrapper(Integer.TYPE, indexVal);
        } catch (Exception e) {
            Interpreter.debug("doIndex: " + e);
            throw new EvalError("Array index does not evaluate to an integer.", callerInfo, callstack, e);
        }
        return index;
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    protected final LHS toLHS(Object baseObj, CallStack callStack, Interpreter interpreter) throws EvalError {
        switch (this.operation) { // TODO: cade o method invocation ?????
            // case CLASS: throw new EvalException("Can't assign .class", this, callStack );
            // // TODO: mudar a mensagem de erro para LHS ?
            case INDEX:
                return toIndexLHS(baseObj, callStack, interpreter);
            // case NAME: return doName(obj, toLHS, callstack, interpreter);
            // case METHOD: throw new EvalError("The left-hand side of an assignment must be
            // a variable", this, callStack);
            case FIELD:
                return toFieldLHS(baseObj, callStack, interpreter);
            case PROPERTY:
                return toPropertyLHS(baseObj, callStack, interpreter);
            // case NEW: throw new EvalError("The left-hand side of an assignment must be a
            // variable", this, callStack);
            // default: throw new InterpreterError( "Unknown suffix type" );
            default:
                throw new EvalError("The left-hand side of an assignment must be a variable", this, callStack);
        }
    }

    // TODO: verificar esse método!
    private final LHS toIndexLHS(Object baseObj, CallStack callStack, Interpreter interpreter) throws EvalError {
        final boolean strictJava = interpreter.getStrictJava();

        // Map or Entry index access not applicable to strict java
        if (!strictJava) {
            // allow index access for Map
            if (baseObj instanceof Map<?, ?>) {
                final Object key = this.jjtGetChild(0).eval(callStack, interpreter);
                return new LHS(baseObj, key, callStack, strictJava);
            }

            // allow index access for Map.Entry
            if (baseObj instanceof Entry<?, ?>) {
                final Entry<?, ?> entry = (Entry<?, ?>) baseObj;
                final Object key = this.jjtGetChild(0).eval(callStack, interpreter);
                if (Objects.equals(entry.getKey(), key))
                    return new LHS(entry, strictJava);
                throw new EvalError("No such property: " + key, this, callStack);
            }
        }

        final Class<?> _class = baseObj.getClass();
        if ((strictJava || !(baseObj instanceof List<?>)) && !_class.isArray())
            throw new EvalError("Not an array or List type", this, callStack);

        int length = baseObj instanceof List<?> ? ((List<?>) baseObj).size() : Array.getLength(baseObj);

        // allow index access for an Map.Entry[]
        if (!strictJava && Entry[].class.isAssignableFrom(_class)) {
            Object key = jjtGetChild(0).eval(callStack, interpreter);

            if ((key instanceof Primitive && ((Primitive) key).isNumber()) || Primitive.isWrapperType(key.getClass())) {
                final int index = (int) Primitive.castWrapper(Integer.TYPE, key);
                if (length > index && -length < index)
                    return new LHS(baseObj, index, strictJava);
            }

            for (Entry<?, ?> entry : (Entry[]) baseObj)
                if (Objects.equals(entry.getKey(), key))
                    return new LHS(entry, strictJava);

            // TODO: ver um erro melhor com uma mensagem melhor dps!
            throw new RuntimeException("There is no valid key");
        }

        int index = getIndexAux(baseObj, 0, callStack, interpreter, this);

        // Negative index or slice expressions not applicable to strict java
        if (!strictJava) {
            if (0 > index)
                index = length + index;
            if (this.slice)
                throw new EvalError("cannot assign to array slice", this, callStack);
        } else if (this.slice)
            throw new EvalError("expected ']' but found ':'", this, callStack);

        return new LHS(baseObj, index, strictJava);
    }

    private final LHS toFieldLHS(Object obj, CallStack callStack, Interpreter interpreter)
            throws EvalError, ReflectError {
        // Safe Navigate operator ?. abort on null
        if (this.safeNavigate && Primitive.NULL == obj) // TODO: como o LHS funciona para safeNavigate ??
            throw SafeNavigate.doAbort();

        return new LHS(obj, this.memberName, callStack, interpreter.getStrictJava());
    }

    private final LHS toPropertyLHS(Object baseObj, CallStack callStack, Interpreter interpreter) throws EvalError {
        if (interpreter.getStrictJava())
            throw new EvalError("Property expression aren't java strict!", this, callStack);

        if (baseObj == Primitive.VOID)
            throw new EvalError("Attempt to access property on undefined variable or class name", this, callStack);

        if (baseObj instanceof Primitive)
            throw new EvalError("Attempt to access property on a primitive", this, callStack);

        Object value = this.jjtGetChild(0).eval(callStack, interpreter);
        if (value instanceof String)
            return new LHS(baseObj, value, callStack, interpreter.getStrictJava());

        throw new EvalError("Property expression must be a String or identifier.", this, callStack);
    }

    @Override
    public String toString() {
        if (operation == INDEX)
            return super.toString() + ":INDEX [" + hasLeftIndex + ":" + slice + " " + hasRightIndex + ":" + step + "]";
        if (operation == FIELD)
            return super.toString() + ":FIELD " + memberName;
        if (operation == METHOD)
            return super.toString() + ":METHOD " + memberName + "()";
        if (operation == PROPERTY)
            return super.toString() + ":PROPERTY {}";
        if (operation == NEW)
            return super.toString() + ":NEW new";
        if (operation == CLASS)
            return super.toString() + ":CLASS class";
        return super.toString() + ":NO OPERATION";
    }
}
