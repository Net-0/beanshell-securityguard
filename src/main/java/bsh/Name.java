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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

// TODO: rever essas doc tb!
/**
    What's in a name?  I'll tell you...
    Name() is a somewhat ambiguous thing in the grammar and so is this.
    <p>

    This class is a name resolver.  It holds a possibly ambiguous dot
    separated name and reference to a namespace in which it allegedly lives.
    It provides methods that attempt to resolve the name to various types of
    entities: e.g. an Object, a Class, a declared scripted BeanShell method.
    <p>

    Name objects are created by the factory method NameSpace getNameResolver(),
    which caches them subject to a class namespace change.  This means that
    we can cache information about various types of resolution here.
    Currently very little if any information is cached.  However with a future
    "optimize" setting that defeats certain dynamic behavior we might be able
    to cache quite a bit.
*/
/*
    <strong>Implementation notes</strong>
    <pre>
    Thread safety: all of the work methods in this class must be synchronized
    because they share the internal intermediate evaluation state.

    Note about invokeMethod():  We could simply use resolveMethod and return
    the MethodInvoker (BshMethod or JavaMethod) however there is no easy way
    for the AST (BSHMehodInvocation) to use this as it doesn't have type
    information about the target to resolve overloaded methods.
    (In Java, overloaded methods are resolved at compile time... here they
    are, of necessity, dynamic).  So it would have to do what we do here
    and cache by signature.  We now do that for the client in Reflect.java.

    Note on this.caller resolution:
    Although references like these do work:

        this.caller.caller.caller...   // works

    the equivalent using successive calls:

        // does *not* work
        for( caller=this.caller; caller != null; caller = caller.caller );

    is prohibited by the restriction that you can only call .caller on a
    literal this or caller reference.  The effect is that magic caller
    reference only works through the current 'this' reference.
    The real explanation is that This referernces do not really know anything
    about their depth on the call stack.  It might even be hard to define
    such a thing...

    For those purposes we provide :

        this.callstack

    </pre>
*/
class Name implements java.io.Serializable {
    // These do not change during evaluation
    private final NameSpace nameSpace; // TODO: verificar isso, n faz mais sentido puxarmos da CallStack ???
    private final String name;
    private final String[] nameParts;
    // private final boolean strictJava = false; // TODO: ver como receber esse parâmetro

    // ---------------------------------------------------------
    // The following instance variables mutate during evaluation and should
    // be reset by the reset() method where necessary

    // For evaluation
    /** Remaining text to evaluate */
    // private String evalName; // TODO: remover isso
    /**
        The last part of the name evaluated.  This is really only used for
        this, caller, and super resolution.
    */
    // private String lastEvalName;
    // private static String FINISHED = null; // null evalname and we're finished
    // private Object evalBaseObject;  // base object for current eval

    // TODO: rever todas essas pika
    // private int callstackDepth;     // number of times eval hit 'this.caller'

    //
    //  End mutable instance variables.
    // ---------------------------------------------------------

    // Begin Cached result structures
    // These are optimizations

    // Note: it's ok to cache class resolution here because when the class
    // space changes the namespace will discard cached names.

    /**
        This constructor should *not* be used in general.
        Use NameSpace getNameResolver() which supports caching.
        @see NameSpace getNameResolver().
    */
    // I wish I could make this "friendly" to only NameSpace
    Name( NameSpace nameSpace, String name ) {
        this.nameSpace = nameSpace;
        // value = s;
        this.name = name;
        this.nameParts = name.split("\\.");
    }

    public Object toObject(CallStack callStack, Interpreter interpreter) throws EvalError, UtilEvalError {
        return this.toObject(callStack, interpreter, false);
    }

    // TODO: teste para preferencia, 'local variables' e então 'variable of local this'! Talvez essa regra deveria ser do NameSpace ?
    synchronized public Object toObject(CallStack callStack, Interpreter interpreter, boolean autoAllocateThis) throws EvalError, UtilEvalError {
        // if (this.nameParts.length == 1) {
        //     Object variable = this.nameSpace.getVariable(this.name);
        //     if (variable != Primitive.VOID) return variable;
        // }
        // TODO: ver para resolver 'this' e 'super'!
        final boolean strictJava = interpreter.getStrictJava(); // TODO: isso não deveria vir do 'interpreter', deveria ser um campo de Name!!!!
        final NameSpace nameSpace = callStack.top();

        // Try straightforward class name first
        Class<?> _class = nameSpace.getClass(this.name);
        if (_class != null) return new ClassIdentifier(_class); // TODO: maybe cache the 'ClassIdentifier' ??

        /*
            "this" // TODO: lógica separada devido á generated classes!
            "this.namespace"
            "this.variables"
            "this.methods"
            "this(.caller)*"

            "super" // TODO: lógica separada devido á generated classes!
            "super.namespace"
            "super.variables"
            "super.methods"
            "super.super" // TODO: como fazer essa merda ???

            "global"
            "global.namespace"
            "global.variables"
            "global.methods"
        */
        

        Object baseObj = null;
        int startIndex = 0;

        // Note: try to resolve special members
        if (!strictJava && this.nameParts.length > 1) {
            startIndex = 2;
            switch (this.nameParts[0] + "." + this.nameParts[1]) {
                case "this.namespace": baseObj = nameSpace; break;
                case "this.variables": baseObj = nameSpace.getVariableNames(); break;
                case "this.methods": baseObj = nameSpace.getMethodNames(); break;
                case "super.namespace": baseObj = nameSpace; break;
                case "super.variables": baseObj = nameSpace.getVariableNames(); break;
                case "super.methods": baseObj = nameSpace.getMethodNames(); break;
                case "global.namespace": baseObj = nameSpace; break;
                case "global.variables": baseObj = nameSpace.getVariableNames(); break;
                case "global.methods": baseObj = nameSpace.getMethodNames(); break;
                case "this.interpreter": baseObj = interpreter; break;
                case "this.caller": { // TODO: podemos ter um '.namespace' dps do 'caller' ....
                    int callerCount = 1;
                    String field = null;
                    for (int i = 1; i < this.nameParts.length; i++)
                        if (this.nameParts[i].equals("caller")) callerCount++;
                        else { field = this.nameParts[i]; break; }

                    startIndex = callerCount+2;
                    NameSpace ns = callStack.get(callerCount);
                    if (field != null) {
                        switch (field) {
                            case "namespace": baseObj = ns; break;
                            case "variables": baseObj = ns.getVariableNames(); break;
                            case "methods": baseObj = ns.getMethodNames(); break;
                            default: startIndex--;
                        }
                    } else {
                        baseObj = new This(ns);
                    }
                }
            }
        }

        if (baseObj == null) {
            if (this.nameParts[0].equals("this")) {
                baseObj = nameSpace.getThis();
                if (baseObj == null) {
                    if (strictJava) throw new UtilEvalError("Can't resolve 'this'");
                    baseObj = new This(nameSpace); // Note: wrap the current nameSpace as a 'this'
                }
                startIndex = 1;
            } else if (this.nameParts[0].equals("super")) {
                This _this = nameSpace.getThis();
                if (_this == null) {
                    if (strictJava) throw new UtilEvalError("Can't resolve 'super'");
                    _this = new This(nameSpace); // Note: wrap the current nameSpace as a 'this'
                }
                baseObj = _this.getSuper();
                startIndex = 1;
            } else {
                try {
                    baseObj = Reflect.getNameSpaceVariable(nameSpace, this.nameParts[0], callStack, strictJava);
                    startIndex = 1;
                } catch (NoSuchElementException e) {
                    // Try to resolve the begin of the name as a class name
                    Class<?> __class = null;
                    List<String> classNameParts = new ArrayList<>(this.nameParts.length);
                    for (int j = 0; j < this.nameParts.length && __class == null; j++) {
                        classNameParts.add(this.nameParts[j]);
                        String className = String.join(".", classNameParts);
                        __class = nameSpace.getClass(className);
                        if (__class != null) { startIndex = j+1; break; }
                    }
                    if (__class != null) baseObj = new ClassIdentifier(__class);
                }
            }
        }

        // TODO: see it
        if (baseObj == null) {
            if (strictJava || !autoAllocateThis)
                throw new UtilEvalError("Can't resolve the name " + this.name); // TODO: trocar o RuntimeException por outro tipo de erro

            final String baseName = this.nameParts[0];
            startIndex = 1;
            baseObj = new This(new NameSpace(nameSpace, "auto: " + baseName));
            nameSpace.setLocalVariable(baseName, null, baseObj, null);

            //     NameSpace targetNameSpace = (baseObj == null) ? this.nameSpace : ((This)baseObj).nameSpace;
            //     Object obj = new NameSpace(targetNameSpace, "auto: "+varName ).getThis(interpreter);
            //     targetNameSpace.setVariable( varName, obj, false, evalBaseObject == null );
            //     return completeRound( varName, suffix(evalName), obj );
        }

        for (int i = startIndex; i < this.nameParts.length; i++) {
            final String memberName = this.nameParts[i];

            if (baseObj instanceof ClassIdentifier) { // Resolve class member
                final Class<?> __class = ((ClassIdentifier) baseObj).getTargetClass();

                if (memberName.equals("this")) { // e.g., MyClass.this
                    This _this = nameSpace.getThis(__class);
                    if (!Reflect.isGeneratedClass(__class) || _this == null)
                        throw new UtilEvalError("Can't find enclosing 'this' instance of " + __class.getName());
                    baseObj = _this;
                    continue;
                }
                if (memberName.equals("super")) { // e.g., MyClass.super
                    This _this = nameSpace.getThis(__class);
                    if (!Reflect.isGeneratedClass(__class) || _this == null)
                        throw new UtilEvalError("Can't find enclosing 'super' instance of " + __class.getName());
                    baseObj = _this.getSuper();
                    continue;
                }

                try { // Try to get a static field. e.g., Collections.EMPTY_LIST
                    baseObj = Reflect.getStaticField(__class, memberName, callStack, strictJava);
                    continue;
                } catch (NoSuchFieldException e) {}

                // Try to get an inner class. e.g., Map.Entry
                String innerClassName = __class.getName() + "$" + memberName;
                Class<?> innerClass = nameSpace.getClass(innerClassName);

                // TODO: verificar o pq disso, tlvz exigir um 'static' de inner classes geradas ?
                // if (null == namespace.classInstance && Reflect.isGeneratedClass(c) && !Reflect.getClassModifiers(c).hasModifier("static"))
                //     throw new UtilEvalError("an enclosing instance that contains "+ clas.getName() + "." + field + " is required");

                if (innerClass != null) { baseObj = new ClassIdentifier(innerClass); continue; }

                // Try to get a static bean property
                if (!strictJava) { // TODO: poderia haver um .getObjectProperty() só para Class<?>, seria + performático!
                    Object value = Reflect.getStaticBeanProperty(__class, memberName, callStack, strictJava);
                    if (value != Primitive.VOID) { baseObj = value; continue; }
                }
            } else {
                try {
                    baseObj = Reflect.getField(baseObj, memberName, callStack, strictJava);
                    continue;
                } catch (NoSuchFieldException e) {}

                if (!strictJava) {
                    Object value = Reflect.getObjectProperty(baseObj, memberName, callStack, strictJava);
                    if (value != Primitive.VOID) { baseObj = value; continue; }

                    // TODO: see it
                    if (autoAllocateThis) {
                        baseObj = new This(new NameSpace(nameSpace, "auto: " + memberName));
                        nameSpace.setLocalVariable(memberName, null, baseObj, null);
                        continue;
                    }
                }
            }

            // TODO: fazer um teste para isso, criar um 'This' em alguns casos quando não conseguir resolver o valor!
            // // No variable or class found in 'this' type ref.
            // // if autoAllocateThis then create one; a child 'this'.
            // if ((baseObj == null || baseObj instanceof This) && autoAllocateThis) {
            //     NameSpace targetNameSpace = (baseObj == null) ? this.nameSpace : ((This)baseObj).nameSpace;
            //     Object obj = new NameSpace(targetNameSpace, "auto: "+varName ).getThis(interpreter);
            //     targetNameSpace.setVariable( varName, obj, false, evalBaseObject == null );
            //     return completeRound( varName, suffix(evalName), obj );
            // }

            // TODO: ver isso melhor
            throw baseObj == null
                    ? new RuntimeException("Can't resolve the name " + this.name)
                    : new RuntimeException("Can't resolve " + this.nameParts[i] + " for " + String.join(".", Arrays.copyOfRange(this.nameParts, 0, i)));
        }

        return baseObj;
    }

    // TODO: pq diabos isso está aqui ??????
    // /**
    //     @return the enclosing class body namespace or null if not in a class.
    // */
    // static NameSpace getClassNameSpace( NameSpace thisNameSpace )
    // {
    //     if ( null == thisNameSpace )
    //         return null;

    //     // is a class instance
    //     if ( thisNameSpace.isClass )
    //         return thisNameSpace;

    //     // is a method parent is a class
    //     if ( thisNameSpace.isMethod
    //             && thisNameSpace.getParent() != null
    //             && thisNameSpace.getParent().isClass )
    //         return thisNameSpace.getParent();

    //     return null;
    // }

    private ClassNotFoundException toClassExceptionCache = null;
    private Class<?> toClassCache = null;
    // TODO: não seria mais correto chamar de toType() ? o 'return null' para "var" é devido à tipagem, não à classe!!!!
    synchronized public Class<?> toClass() throws ClassNotFoundException {
        if (toClassExceptionCache != null) throw toClassExceptionCache;
        if (toClassCache != null) return toClassCache;

        // TODO: It shouldn't be here!
        if (this.name.equals("var")) return null;

        // /* Try straightforward class name first */
        // Class<?> clas = namespace.getClass( evalName );
        Class<?> _class = this.nameSpace.getClass(this.name);
        if (_class != null) return _class;

        // Note: this will try to solve the name as an inner class name
        // e.g., "com.my.package.a.b.c" -> "com.my.package.a.b$c", "com.my.package.a$b$c", "com.my.package$a$b$c"
        for (int i = this.nameParts.length-1; i > 0; i--) {
            String[] baseClassName = Arrays.copyOfRange(this.nameParts, 0, i);
            String[] innerClassName = Arrays.copyOfRange(this.nameParts, i, this.nameParts.length);
            String className = String.join(".", baseClassName) + "$" + String.join("$", innerClassName);
            Class<?> __class = this.nameSpace.getClass(className);
            if (__class != null) return __class;
        }

        throw new ClassNotFoundException(this.name);
    }

    // TODO: não faz sentido termos esse método aqui!
    /* */
    synchronized public LHS toLHS(CallStack callStack, Interpreter interpreter) throws EvalError, UtilEvalError {
        // // Should clean this up to a single return statement
        // reset();
        // LHS lhs;

        // // Simple (non-compound) variable assignment e.g. x=5;
        // if ( !isCompound(evalName) )
        // {
        //     if ( evalName.equals("this") )
        //         throw new UtilEvalError("Can't assign to 'this'." );

        //     if (namespace.isClass) // Loose type field
        //         lhs = new LHS( namespace, evalName );
        //     else
        //         lhs = new LHS( namespace, evalName, false/*bubble up if allowed*/);
        //     return lhs;
        // }

        // // Field e.g. foo.bar=5;
        // Object obj = null;
        // try {
        //     while( evalName != null && isCompound( evalName ) )
        //     {
        //         obj = consumeNextObjectField( callstack, interpreter,
        //             false/*forcclass*/, true/*autoallocthis*/ );
        //     }
        // }
        // catch( UtilEvalError e ) {
        //     throw new UtilEvalError( "LHS evaluation: " + e.getMessage(), e);
        // }

        // // Finished eval and its a class.
        // if ( evalName == null && obj instanceof ClassIdentifier )
        //     throw new UtilEvalError("Can't assign to class: " + value );

        // if ( obj == null )
        //     throw new UtilEvalError("Error in LHS: " + value );

        // // e.g. this.x=5;  or someThisType.x=5;
        // if ( obj instanceof This )
        // {
        //     // dissallow assignment to magic fields
        //     if (
        //         evalName.equals("namespace")
        //         || evalName.equals("variables")
        //         || evalName.equals("methods")
        //         || evalName.equals("caller")
        //     )
        //         throw new UtilEvalError(
        //             "Can't assign to special variable: "+evalName );

        //     Interpreter.debug("found This reference evaluating LHS");
        //     /*
        //         If this was a literal "super" reference then we allow recursion
        //         in setting the variable to get the normal effect of finding the
        //         nearest definition starting at the super scope.  On any other
        //         resolution qualified by a 'this' type reference we want to set
        //         the variable directly in that scope. e.g. this.x=5;  or
        //         someThisType.x=5;

        //         In the old scoping rules super didn't do this.
        //     */
        //     boolean localVar = !lastEvalName.equals("super");
        //     return new LHS( ((This)obj).namespace, evalName, localVar );
        // }

        // if ( evalName != null )
        // {
        //     try {
        //         if ( obj instanceof ClassIdentifier )
        //         {
        //             Class<?> clas = ((ClassIdentifier)obj).getTargetClass();
        //             lhs = Reflect.getLHSStaticField(clas, evalName);
        //             return lhs;
        //         } else {
        //             lhs = Reflect.getLHSObjectField(obj, evalName);
        //             return lhs;
        //         }
        //     } catch(ReflectError e) {
        //         return new LHS(obj, evalName);
        //     }
        // }

        // throw new InterpreterError("Internal error in lhs...");
        // TODO: verificar esse método
        // throw new RuntimeException("Not implemented!");

        // final String name = ((BSHAmbiguousName) this.jjtGetChild(0)).name;
        final boolean strictJava = interpreter.getStrictJava();
        final int lastDotIndex = this.name.lastIndexOf('.');

        if (this.name.equals("this") || this.name.endsWith(".this"))
            throw new UtilEvalError("Can't assign to 'this'." );

        // e.g. global.myConstants.abc = 10
        if (lastDotIndex > 0) {
            final String baseName = this.name.substring(0, lastDotIndex);
            final String memberName = this.name.substring(lastDotIndex + 1);
            final Object baseObj = this.nameSpace.getNameResolver(baseName).toObject(callStack, interpreter, true);
            return new LHS(baseObj, memberName, callStack, strictJava);
        }

        // e.g. abc = 10
        return new LHS(this.nameSpace, this.name, strictJava);
    }

    // TODO: faz sentido ter esse método ? Um .contains(".") direto não é melhor ?
    static final boolean isCompound(final String value) {
        return value != null && value.indexOf('.') > 0;
    }

    // end compound name routines

    public String toString() { return this.name; }

}

