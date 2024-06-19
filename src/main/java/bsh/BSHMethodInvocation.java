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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

class BSHMethodInvocation extends SimpleNode
{
    String methodName;

    BSHMethodInvocation (int id) { super(id); }

    // BSHAmbiguousName getNameNode() {
    //     return (BSHAmbiguousName)jjtGetChild(0);
    // }

    BSHArguments getArgsNode() {
        return (BSHArguments)jjtGetChild(1);
    }

    // /**
    //     Evaluate the method invocation with the specified callstack and
    //     interpreter
    // */
    // private Object __eval( CallStack callstack, Interpreter interpreter ) throws EvalError {
    //     NameSpace nameSpace = callstack.top();
    //     // BSHAmbiguousName nameNode = getNameNode();

    //     // get caller info for assert fail
    //     if ("fail".equals(this.methodName))
    //         interpreter.getNameSpace().setNode(this);

    //     // Do not evaluate methods this() or super() in class instance space (i.e. inside a constructor)
    //     if ( nameSpace.getParent() != null && nameSpace.getParent().isClass
    //         && ( this.methodName.equals("super") || this.methodName.equals("this") )
    //     )
    //         return Primitive.VOID;

    //     Name name = nameNode.getName(nameSpace);
    //     Object[] args = this.getArgsNode().getArguments(callstack, interpreter);

    //     try {
    //         return name.invokeMethod( interpreter, args, callstack, this);
    //     } catch (ReflectError e) {
    //         throw new EvalError(
    //             "Error in method invocation: " + e.getMessage(),
    //                 this, callstack, e);
    //     } catch (InvocationTargetException e) {
    //         throw Reflect.targetErrorFromTargetException(
    //             e, name.toString(), callstack, this);
    //     } catch ( UtilEvalError e ) {
    //         throw e.toEvalError( this, callstack );
    //     }
    // }

    // private static final Pattern NO_OVERRIDE_RE = Pattern.compile("eval|assert");
    @Override
    public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
        NameSpace nameSpace = callstack.top();

        // get caller info for assert fail
        if ("fail".equals(this.methodName)) interpreter.getNameSpace().setNode(this);

        // Do not evaluate methods this() or super() in class instance space (i.e. inside a constructor)
        final boolean parentNameSpaceIsClass = nameSpace.getParent() != null && nameSpace.getParent().isClass;
        final boolean alternativeConstructor = this.methodName.equals("super") || this.methodName.equals("this");
        if (parentNameSpaceIsClass && alternativeConstructor) return Primitive.VOID;

        Object[] args = this.getArgsNode().getArguments(callstack, interpreter);
        Class<?>[] argTypes = Types.getTypes( args );

        // Check for existing method
        BshMethod meth = null;
        try {
            meth = nameSpace.getMethod( this.methodName, argTypes );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError("Local method invocation", this, callstack );
        }

        // If defined, invoke it
        if ( meth != null ) {
            // whether to use callstack.top or new child of declared name space
            // enables late binding for closures and nameSpace chaining #676
            boolean overrideChild = !nameSpace.isMethod
                    && !meth.isScriptedObject
                    && nameSpace.isChildOf(meth.declaringNameSpace)
                    && !nameSpace.getParent().isClass
                    && !this.methodName.equals("eval")
                    && !this.methodName.equals("assert"); // && !NO_OVERRIDE_RE.matcher(this.methodName).matches()

            return meth.invoke( args, interpreter, callstack, this, overrideChild );
        }

        // Look for a BeanShell command
        return nameSpace.invokeCommand(methodName, args, interpreter, callstack, this);
    }

    Object eval(Object baseObj, CallStack callStack, Interpreter interpreter) throws EvalError {
        try {
            if (baseObj instanceof ClassIdentifier) return this.eval((ClassIdentifier) baseObj, callStack, interpreter);
    
            Object[] args = this.getArgsNode().getArguments(callStack, interpreter);
            if (baseObj == Primitive.NULL)
                throw new UtilTargetError(new NullPointerException("Null Pointer in Method Invocation of " +this.methodName+"()"));
    
            // enum block members will be in namespace only
            if ( baseObj.getClass().isEnum() ) {
                NameSpace thisNamespace = Reflect.getThisNS(baseObj);
                if ( null != thisNamespace ) {
                    BshMethod m = thisNamespace.getMethod(this.methodName, Types.getTypes(args), true);
                    if ( null != m ) return m.invoke(args, interpreter, callStack, this);
                }
            }
    
            // found an object and it's not an undefined variable
            return Reflect.invokeObjectMethod(baseObj, this.methodName, args, interpreter, callStack, this);
        } catch (UtilEvalError e) {
            throw e.toEvalError(this, callStack);
        }
    }

    private Object eval(ClassIdentifier baseClassIdentifier, CallStack callStack, Interpreter interpreter) throws EvalError {
        Class<?> clas = baseClassIdentifier.getTargetClass();
        BshClassManager bcm = interpreter.getClassManager();
        Object[] args = this.getArgsNode().getArguments(callStack, interpreter);

        try {
            return Reflect.invokeStaticMethod(bcm, clas, methodName, args, this);
        } catch (InvocationTargetException e) {
            throw Reflect.targetErrorFromTargetException(e, this.methodName, callStack, this);
        } catch (UtilEvalError e) {
            throw e.toEvalError(this, callStack);
        }
    }

    // @Override
    // public Class<?> getEvalReturnType(NameSpace nameSpace) throws EvalError {
    //     try {
    //         String name = this.getNameNode().text;
    //         Class<?>[] argsTypes = this.getArgsNode().getArgumentsType(nameSpace);

    //         if (!name.contains(".")) throw new EvalError("Não implementado ainda!", this, null);

    //         int lastDotIndex = name.lastIndexOf('.');
    //         String baseName = name.substring(0, lastDotIndex);
    //         String methodName = name.substring(lastDotIndex+1);

    //         CallStack callStack = new CallStack(nameSpace);
    //         Interpreter interpreter = new Interpreter(nameSpace);
    //         Object base = nameSpace.getNameResolver(baseName).toObject(callStack, interpreter, false);

    //         boolean isStatic = base instanceof ClassIdentifier;
    //         String wrongStaticMsg = isStatic ? "non static" : "static";
    //         Class<?> baseType = isStatic ? ((ClassIdentifier) base).clas : (Class<?>) base;

    //         try {
    //             Method method = baseType.getMethod(methodName, argsTypes);
    //             if (Reflect.isStatic(method) != isStatic)
    //                 throw new UtilEvalError("Can't get " + wrongStaticMsg + " method: " + StringUtil.methodString(method));
    //             return method.getReturnType();
    //         } catch (Throwable t) {
    //             throw new UtilEvalError("Can't see the return type of method '" + methodName + "'", t);
    //         }
    //     } catch (UtilEvalError e) {
    //         throw e.toEvalError(this, null);
    //     }

    //     // // return baseType instanceof ClassIdentifier
    //     // //         ? ((ClassIdentifier) baseType).clas.getMethod(methodName, argTypes)
    //     // //         : ((Class<?>) baseType).getMethod(methodName, argsTypes);

    //     // System.out.println("BSHMethodInvocation -> baseName: " + baseName);
    //     // System.out.println("BSHMethodInvocation -> methodName: " + methodName);

    //     // // Object baseType = nameNode.getEvalReturnType(nameSpace);

    //     // // TODO Auto-generated method stub
    //     // return super.getEvalReturnType(nameSpace);
    // }
}

