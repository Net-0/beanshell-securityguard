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

class BSHMethodInvocation extends SimpleNode {
    String methodName;

    BSHMethodInvocation (int id) { super(id); }

    // BSHAmbiguousName getNameNode() {
    //     return (BSHAmbiguousName)jjtGetChild(0);
    // }

    BSHArguments getArgsNode() {
        return (BSHArguments)jjtGetChild(0);
    }

    /**
        Evaluate the method invocation with the specified callstack and
        interpreter
    */
    public Object eval(CallStack callStack, Interpreter interpreter) throws EvalError {
        NameSpace nameSpace = callStack.top();
        // BSHAmbiguousName nameNode = getNameNode();

        // // get caller info for assert fail
        // if ("fail".equals(nameNode.name))
        //     interpreter.getNameSpace().setNode(this);

        // TODO: verificar isso!
        // Do not evaluate methods this() or super() in class instance space
        // (i.e. inside a constructor)
        if (
            //nameSpace.getParent() != null && nameSpace.getParent().isClass &&
            ( methodName.equals("super") || methodName.equals("this"))
        )
            return Primitive.VOID;

        final Object[] args = getArgsNode().getArguments(callStack, interpreter);
        // Class<?>[] argsTypes = Types.getTypes(args);

        // // TODO: invokeDeclaredLocalMethod()
        // BshLocalMethod localMethod = nameSpace.getMethod(methodName, argsTypes);
        // if (localMethod != null)
        //     return localMethod.invoke(args);
        try {
            return Reflect.invokeNameSpaceMethod(nameSpace, methodName, args, callStack, interpreter.getStrictJava());
        } catch (NoSuchMethodException e) {
            throw new EvalError(e.getMessage(), this, callStack);
        } catch (UtilEvalError e) {
            throw e.toEvalError(this, callStack);
        }

        // TODO: fazer um Reflect.invokeImportedMethod(nameSpace, methodName, args, callStack) ??

        // TODO: não deveriamos tentar invocar static methods tb ???

        // // Attempt to invoke a method of 'this'
        // Object _this = nameSpace.getThis();
        // try {
        //     if (_this != null) return Reflect.invokeMethod(_this, methodName, args, callStack);
        // } catch (Throwable t) {
        //     // TODO: see it
        // }

        // TODO: invokeLocalMethod()

        // Name name = nameNode.getName(nameSpace);

        // try {
        //     return name.invokeMethod( interpreter, args, callstack, this);
        // } catch (ReflectError e) {
        //     throw new EvalException(
        //         "Error in method invocation: " + e.getMessage(),
        //             this, callstack, e);
        // } catch (InvocationTargetException e) {
        //     throw Reflect.targetErrorFromTargetException(
        //         e, name.toString(), callstack, this);
        // } catch ( UtilEvalError e ) {
        //     throw e.toEvalError( this, callstack );
        // }
    }
}

