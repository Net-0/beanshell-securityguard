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

import java.util.Arrays;

import bsh.internals.BshConstructor;
import bsh.internals.BshConsumer;
import bsh.internals.BshFunction;
import bsh.internals.BshLocalMethod;
import bsh.internals.BshMethod;
import bsh.internals.BshModifier;

// TODO: refatorar esse Node!
class BSHMethodDeclaration extends SimpleNode {
    public String name;

    // Begin Child node structure evaluated by insureNodesParsed

    // BSHReturnType returnTypeNode;
    // BSHFormalParameters paramsNode;
    // BSHBlock blockNode;
    // index of the first throws clause child node
    // int firstThrowsClause;

    // End Child node structure evaluated by insureNodesParsed

    public Modifiers modifiers = new Modifiers(Modifiers.METHOD);

    // TODO: remove it ? why cache it ???
    // Unsafe caching of type here.
    // Class<?> returnType;  // null (none), Void.TYPE, or a Class
    int numThrows = 0;
    // boolean isVarArgs;

    BSHMethodDeclaration(int id) { super(id); }

    // TODO: see this method!
    /**
        Set the returnTypeNode, paramsNode, and blockNode based on child
        node structure.  No evaluation is done here.
    */
    synchronized void insureNodesParsed() {
        // if (paramsNode != null) // there is always a paramsNode
        //     return;

        Object firstNode = jjtGetChild(0);
        // firstThrowsClause = 1;
        if (firstNode instanceof BSHReturnType) {
            // returnTypeNode = (BSHReturnType)firstNode;
            // paramsNode = (BSHFormalParameters)jjtGetChild(1);
            // if (jjtGetNumChildren() > 2+numThrows)
            //     blockNode = (BSHBlock)jjtGetChild(2+numThrows); // skip throws
            // ++firstThrowsClause;
        } else {
            // paramsNode = (BSHFormalParameters)jjtGetChild(0);
            // blockNode = (BSHBlock)jjtGetChild(1+numThrows); // skip throws
        }

        // paramsNode.insureParsed();
        // isVarArgs = paramsNode.isVarArgs;
    }

    /**
        Evaluate the return type node.
        @return the type or null indicating loosely typed return
    */
    private final Class<?> getReturnType(CallStack callstack, Interpreter interpreter) throws EvalError {
        if (this.isLooseTypedReturn()) return null;
        return ((BSHReturnType) this.jjtGetChild(0)).getType(callstack, interpreter);
    }

    private final BSHFormalParameters getParamsNode() {
        return (BSHFormalParameters) (this.isLooseTypedReturn() ? this.jjtGetChild(0) : this.jjtGetChild(1));
    }

    private final BSHBlock getBlockNode() {
        return (BSHBlock) (this.isLooseTypedReturn() ? this.jjtGetChild(1+this.numThrows) : this.jjtGetChild(2+this.numThrows));
    }

    // TODO: implementar generic exception types
    private final Class<?>[] getExceptionTypes(CallStack callstack, Interpreter interpreter) throws EvalError {
        final Class<?>[] exceptionTypes = new Class<?>[this.numThrows];
        final int firstThrowsClause = this.isLooseTypedReturn() ? 1 : 2;

        for (int i = 0; i < exceptionTypes.length; i++) {
            final BSHName nameNode = this.jjtGetChild(firstThrowsClause + i);
            exceptionTypes[i] = nameNode.toClass(callstack, interpreter);
        }

        return exceptionTypes;
    }

    // String getReturnTypeDescriptor(CallStack callstack, Interpreter interpreter, String defaultPackage) {
    //     insureNodesParsed();
    //     if ( returnTypeNode == null )
    //         return null;
    //     else
    //         return returnTypeNode.getTypeDescriptor(
    //             callstack, interpreter, defaultPackage );
    // }

    // BSHReturnType getReturnTypeNode() {
    //     insureNodesParsed();
    //     return returnTypeNode;
    // }

    /**
        Evaluate the declaration of the method.  That is, determine the
        structure of the method and install it into the caller's namespace.
    */
    public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
//         returnType = evalReturnType( callstack, interpreter );
//         evalNodes(callstack, interpreter);

//         // Install an *instance* of this method in the namespace.
//         // See notes in BshMethod

// // This is not good...
// // need a way to update eval without re-installing...
// // so that we can re-eval params, etc. when classloader changes
// // look into this
        final NameSpace namespace = callstack.top();
//         BshLocalMethod bshMethod = new BshLocalMethod( this, namespace, modifiers );
        final BshLocalMethod localMethod = this.toLocalMethod(callstack, interpreter);

        // // TODO: verificar isso!
        // if (!namespace.isMethod && !namespace.isClass)
        //     interpreter.getClassManager().addListener(localMethod);

        // else if (namespace.isMethod && !paramsNode.isListener()) {
        //     interpreter.getClassManager().addListener(paramsNode);
        //     paramsNode.setListener(true);
        // }

        namespace.setMethod(localMethod);
        return Primitive.VOID;
    }

    // private void evalNodes(CallStack callstack, Interpreter interpreter) throws EvalError {
    //     insureNodesParsed();

    //     // validate that the throws names are class names
    //     for(int i=firstThrowsClause; i<numThrows+firstThrowsClause; i++)
    //         ((BSHAmbiguousName)jjtGetChild(i)).toClass(
    //             callstack, interpreter );

    //     paramsNode.eval( callstack, interpreter );

    //     // if strictJava mode, check for loose parameters and return type
    //     if ( interpreter.getStrictJava() )
    //     {
    //         for(int i=0; i<paramsNode.paramTypes.length; i++)
    //             if ( paramsNode.paramTypes[i] == null )
    //                 // Warning: Null callstack here.  Don't think we need
    //                 // a stack trace to indicate how we sourced the method.
    //                 throw new EvalException(
    //             "(Strict Java Mode) Undeclared argument type, parameter: " +
    //                 paramsNode.getParamNames()[i] + " in method: "
    //                 + name, this, null );

    //         if ( returnType == null )
    //             // Warning: Null callstack here.  Don't think we need
    //             // a stack trace to indicate how we sourced the method.
    //             throw new EvalException(
    //             "(Strict Java Mode) Undeclared return type for method: "
    //                 + name, this, null );
    //     }
    // }


    /////////////////////////////////////////////////////////////////////////////////////////////////

    protected final boolean isLooseTypedReturn() {
        return !(this.jjtGetChild(0) instanceof BSHReturnType);
    }

    protected BshLocalMethod toLocalMethod(CallStack callstack, Interpreter interpreter) throws EvalError {
        final Class<?> returnType = this.getReturnType(callstack, interpreter);

        // // TODO: solve the problem that 'paramsNode.paramTypes' isn't being defined before .eval()
        // this.paramsNode.eval(callstack, interpreter);
        BshFunction<CallStack, ?> body = (bodyCS) -> {
            // try {
                Object result = this.getBlockNode().eval(bodyCS, interpreter);
                // TODO: fazer + testes para os tipos de retornos!
                // if (this.returnType == void.class || bodyReturn == Primitive.VOID) return null;
                if (returnType == void.class) return null;
                if (interpreter.getStrictJava() && !(result instanceof ReturnControl)) return null;

                result = result instanceof ReturnControl ? ((ReturnControl) result).value : result;
                // final Object result = this.returnType.isPrimitive() ? Primitive.unwrap(bodyReturn) : bodyReturn;

                try {
                    return Primitive.unwrap(Types.castObject(Primitive.unwrap(result), returnType, Types.ASSIGNMENT));
                } catch (UtilEvalError e) {
                    throw e.toEvalError(this, bodyCS);
                }
        };

        // NameSpace namespace = callstack.top();
        return new BshLocalMethod(
            this.modifiers.getModifiers() & BshModifier.METHOD_MODIFIERS, // TODO: ver os modifiers!
            returnType,
            name,
            this.getParamsNode().toParameters(callstack, interpreter),
            this.getExceptionTypes(callstack, interpreter),
            body,
            callstack.top()
        );
    }

    // TODO: validar os nodes ao invés de dar um 'eval()' usando CallStack e Interpreter ? Seria + rápido e o método seria + simples!
    protected boolean isValidMethod(CallStack callstack, Interpreter interpreter) throws EvalError {
        final Class<?> returnType = this.getReturnType(callstack, interpreter);
        if (interpreter.getStrictJava() && returnType == null)
            return false;

        if (interpreter.getStrictJava())
            return !this.isLooseTypedReturn() && !this.getParamsNode().isLooseTyped();

        return true;
    }

    protected BshMethod toMethod(CallStack callstack, Interpreter interpreter) throws EvalError {
        final Class<?> returnType = this.getReturnType(callstack, interpreter);

        // // TODO: solve the problem that 'paramsNode.paramTypes' isn't being defined before .eval()
        // this.paramsNode.eval(callstack, interpreter);
        BshFunction<CallStack, ?> body = (bodyCS) -> {
            // try {
                // System.out.println("BshMethod.body -> bodyCS -> " + bodyCS);
                // System.out.println("BshMethod.body -> bodyCS.top().getAllVariableNames(): " + Arrays.asList(bodyCS.top().getVariableNames()));
                final Object result = this.getBlockNode().eval(bodyCS, interpreter);
                // TODO: fazer + testes para os tipos de retornos!
                // if (this.returnType == void.class || bodyReturn == Primitive.VOID) return null;
                if (returnType == void.class || !(result instanceof ReturnControl)) return null;

                final Object resultValue = Primitive.unwrap(((ReturnControl) result).value);
                // result = Primitive.unwrap(result);

                // final Object result = this.returnType.isPrimitive() ? Primitive.unwrap(bodyReturn) : bodyReturn;
                if (returnType == null) return resultValue;

                try {
                    return Primitive.unwrap(Types.castObject(resultValue, returnType, Types.ASSIGNMENT));
                } catch (UtilEvalError e) {
                    throw e.toEvalError(this, bodyCS);
                }
                // final String msg = String.format("Can't assign %s to %s", Types.prettyName(Types.getType(result)), Types.prettyName(returnType));
                // throw new EvalError(msg, null, bodyCS);
    
            // } catch (TargetError e) {
            //     for (Class<?> exceptionType: exceptionTypes)
            //         if (exceptionType.isInstance(e.getTarget()))
            //             throw e.getTarget();
            //     throw new InterpreterError("Can't invoke method: Unexpected Exception: " + e.getTarget().getMessage(), e.getTarget());
            // } catch (EvalError e) {
            //     throw new InterpreterError("Can't invoke method: " + e.getMessage(), e);
            // }
        };

        // NameSpace namespace = callstack.top();
        return new BshMethod(
            this.modifiers.getModifiers() & BshModifier.METHOD_MODIFIERS,
            returnType,
            name,
            this.getParamsNode().toParameters(callstack, interpreter),
            this.getExceptionTypes(callstack, interpreter),
            body
        );
    }

    protected boolean isValidConstructor(String classSimpleName) throws EvalError {
        return this.isLooseTypedReturn() && this.name.equals(classSimpleName);
    }

    protected BshConstructor toConstructor(CallStack callstack, Interpreter interpreter) throws EvalError {
        // Class<?> returnType = this.evalReturnType( callstack, interpreter );
        // returnType = returnType == null ? Object.class : returnType;

        // List<Class<?>> exceptionTypes = new ArrayList<>();

        // // validate that the throws names are class names
        // for (int i=firstThrowsClause; i<numThrows+firstThrowsClause; i++) {
        //     BSHAmbiguousName ban = (BSHAmbiguousName) this.jjtGetChild(i);
        //     exceptionTypes.add(ban.toClass(callstack, interpreter));
        // }

        // insureNodesParsed();

        // // TODO: solve the problem that 'paramsNode.paramTypes' isn't being defined before .eval()
        // this.paramsNode.eval(callstack, interpreter);

        final BshConsumer<CallStack> body = (bodyCS) -> this.getBlockNode().eval(bodyCS, interpreter);
        // BshConsumer<CallStack> body = (bodyCS) -> {
        //     this.getBlockNode().eval(bodyCS, interpreter);
            // try {
            //     if (!interpreter.getStrictJava())
            //     for (Node node: this.body)
            //         if (node instanceof BSHClassDeclaration)
            //             node.eval(callStack, interpreter);
        
            //     for (Node node: this.body) {
            //         Object ret = node.eval(callStack, interpreter);
            //         // statement or embedded block evaluated a return statement
            //         if ( ret instanceof ReturnControl ) break;
            //     }
            // } catch (TargetError te) { // TODO: fazer teste validando erros!
            //     Throwable e = te.getTarget();
            //     for (Class<?> exceptionType: this.exceptionTypes)
            //         if (exceptionType.isInstance(e))
            //             throw e;
            //     throw new InterpreterError("Unexpected Error", e);
            // } catch (Throwable t) { // TODO: isso é o suficiente ?? Precisa coloicar o for-loop validando o tipo da exception aqui ?
            //     throw new InterpreterError("Unexpected Error", t);
            // }
        // };

        // NameSpace namespace = callstack.top();
        return new BshConstructor(
            this.modifiers.getModifiers() & BshModifier.CONSTRUCTOR_MODIFIERS,
            this.getParamsNode().toParameters(callstack, interpreter),
            this.getExceptionTypes(callstack, interpreter),
            BshConstructor.NO_CHAIN, // TODO: see it!
            null, // TODO: see it!
            body
        );
    }

    public String toString() {
        return super.toString() + ": " + name;
    }
}
