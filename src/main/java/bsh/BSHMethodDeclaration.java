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

import java.lang.reflect.Type;
import java.util.Arrays;

import bsh.internals.BshClass;
import bsh.internals.BshConstructor;
// import bsh.internals.BshConsumer;
import bsh.internals.BshLocalMethod;
import bsh.internals.BshMethod;
import bsh.internals.BshModifier;
import bsh.internals.BshParameter;

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
    private final Type getReturnType(CallStack callstack, Interpreter interpreter) throws EvalError {
        // if (this.isLooseTypedReturn()) return null;
        // return ((BSHReturnType) this.jjtGetChild(0)).getType(callstack, interpreter);

        return this.isLooseTypedReturn()
                ? null
                : this.<BSHReturnType>jjtGetChild(0)._toType(callstack, interpreter);
    }

    private final BSHFormalParameters getParamsNode() {
        return (BSHFormalParameters) (this.isLooseTypedReturn() ? this.jjtGetChild(0) : this.jjtGetChild(1));
    }

    private final BSHBlock getBlockNode() {
        return (BSHBlock) (this.isLooseTypedReturn() ? this.jjtGetChild(1+this.numThrows) : this.jjtGetChild(2+this.numThrows));
    }

    // TODO: implementar generic exception types
    private final Type[] getExceptionTypes(CallStack callstack, Interpreter interpreter) throws EvalError {
        final Type[] exceptionTypes = new Type[this.numThrows];
        final int firstThrowsClause = this.isLooseTypedReturn() ? 1 : 2;

        for (int i = 0; i < exceptionTypes.length; i++) {
            final BSHName nameNode = this.jjtGetChild(firstThrowsClause + i);
            exceptionTypes[i] = nameNode._toType(callstack, interpreter);
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

    protected BshLocalMethod toLocalMethod(CallStack callStack, Interpreter interpreter) throws EvalError {
        final int mods = this.modifiers.getModifiers() & BshModifier.METHOD_MODIFIERS;
        final NameSpace declaringNameSpace = callStack.top();
        final Type returnType = this.getReturnType(callStack, interpreter);
        final BshParameter[] parameters = this.getParamsNode().toParameters(callStack, interpreter, false);

        // // TODO: solve the problem that 'paramsNode.paramTypes' isn't being defined before .eval()
        // this.paramsNode.eval(callstack, interpreter);
        final BshLocalMethod.BodyFunction<?> body = (_callStack, args) -> {
            final NameSpace nameSpace = new NameSpace(declaringNameSpace, this.name);
            // final NameSpace nameSpace = new BshMethodNameSpace(declaringNameSpace, _class, thisArg, "<init>", args, parameters);
            // final CallStack _callStack = new CallStack(nameSpace);

            nameSpace.setArguments(parameters, args);

            Object result;

            _callStack.push(nameSpace);
            if (BshModifier.isSynchronized(mods))
                synchronized (this) { // TODO: teste unitário para local-methods synchronized
                    result = this.getBlockNode().eval(_callStack, interpreter);
                }
            else
                result = this.getBlockNode().eval(_callStack, interpreter);
            _callStack.pop();

            // try {
            // Object result = this.getBlockNode().eval(_callStack, interpreter);
            // TODO: fazer + testes para os tipos de retornos!
            // if (this.returnType == void.class || bodyReturn == Primitive.VOID) return null;
            if (returnType == void.class) return null;
            if (interpreter.getStrictJava() && !(result instanceof ReturnControl)) return null;

            result = result instanceof ReturnControl ? ((ReturnControl) result).value : result;
            // final Object result = this.returnType.isPrimitive() ? Primitive.unwrap(bodyReturn) : bodyReturn;

            try {
                final Class<?> rawReturnType = bsh.internals.Types.getRawType(returnType);
                return Primitive.unwrap(Types.castObject(Primitive.unwrap(result), rawReturnType, Types.ASSIGNMENT));
            } catch (UtilEvalError e) {
                throw e.toEvalError(this, _callStack);
            }
        };

        // NameSpace namespace = callstack.top();
        return new BshLocalMethod(
            mods, // TODO: ver os modifiers!
            returnType,
            name,
            parameters,
            this.getExceptionTypes(callStack, interpreter),
            body,
            callStack.top()
        );
    }

    // TODO: esse é um bom nome de método ?
    // TODO: validar os nodes ao invés de dar um 'eval()' usando CallStack e Interpreter ? Seria + rápido e o método seria + simples!
    protected boolean isValidMethod(CallStack callstack, Interpreter interpreter) throws EvalError {
        // TODO: pq diabos eu preciso desse 'returnType' ?
        // final Class<?> returnType = this.getReturnType(callstack, interpreter);
        // if (interpreter.getStrictJava() && returnType == null)
        if (interpreter.getStrictJava() && this.isLooseTypedReturn())
            return false;

        if (interpreter.getStrictJava())
            return !this.isLooseTypedReturn() && !this.getParamsNode().isLooseTyped();

        return true;
    }

    protected BshMethod toMethod(int classMods, CallStack callStack, Interpreter interpreter) throws EvalError {
        // TODO: validar os métodos de interfaces, eles só podem ser public!
        // TODO: validar métodos abstract -- só podem existir em classes abstratas!
        final boolean strictJava = interpreter.getStrictJava();

        final int mods = BshModifier.isInterface(classMods)
                            ? (this.modifiers.getModifiers() & BshModifier.METHOD_MODIFIERS) | BshModifier.PUBLIC
                            : this.modifiers.getModifiers() & BshModifier.METHOD_MODIFIERS;
        final Type returnType = this.getReturnType(callStack, interpreter);
        final NameSpace declaringNameSpace = callStack.top();
        final BshParameter[] parameters = this.getParamsNode().toParameters(callStack, interpreter, false);

        // TODO: como fica os final fields ?
        //    - final fields com initializers
        //    - final fields sem initializers
        //    - definindo final fields dentro ou fora do constructor
        final BshMethod.BodyFunction<?, ?> body = (_callStack, _class, thisArg, args) -> {
            final This __this = thisArg != null ? BshClass.fromGeneratedClass(_class).getThisFromObject(thisArg) : null;
            final NameSpace nameSpace = new NameSpace(declaringNameSpace, _class.getName() + "." + this.name, _class, __this);
            nameSpace.setArguments(parameters, args);

            // TODO: e se chamar o body direto no Reflect ? assim teriamos a CallStack mais completa dentro do script, porém como fica o synchronized ?
            // try {
                _callStack.push(nameSpace);
                final Object result = this.getBlockNode().eval(_callStack, interpreter);
                _callStack.pop();

                if (strictJava) {
                    if (returnType == void.class) return null;
                    if (result instanceof ReturnControl) return result;
                    // TODO: melhorar essa mensagem ai
                    throw new EvalError("O método não possui um return!", this, null);
                } else {
                    return result instanceof ReturnControl ? ((ReturnControl) result).value : result;
                }

                // TODO: fazer + testes para os tipos de retornos!
                // if (this.returnType == void.class || bodyReturn == Primitive.VOID) return null;
                // if (returnType == void.class || !(result instanceof ReturnControl)) return null;
                // final Object resultValue = ((ReturnControl) result).value;
                // final Object resultValue = Primitive.unwrap(((ReturnControl) result).value);
                // final Object result = this.returnType.isPrimitive() ? Primitive.unwrap(bodyReturn) : bodyReturn;
                // if (returnType == null) return resultValue;

                // final Object castedResultValue = Types.castObject(resultValue, returnType, Types.ASSIGNMENT);
                // _callStack.pop();
                // return Primitive.unwrap(castedResultValue);
            // } catch (UtilEvalError e) {
            //     throw e.toEvalError(this, _callStack);
            // }
        };

        // // // TODO: solve the problem that 'paramsNode.paramTypes' isn't being defined before .eval()
        // // this.paramsNode.eval(callstack, interpreter);
        // BshFunction<CallStack, ?> body = (bodyCS) -> {
        //     // try {
        //         // System.out.println("BshMethod.body -> bodyCS -> " + bodyCS);
        //         // System.out.println("BshMethod.body -> bodyCS.top().getAllVariableNames(): " + Arrays.asList(bodyCS.top().getVariableNames()));
        //         final Object result = this.getBlockNode().eval(bodyCS, interpreter);
        //         // TODO: fazer + testes para os tipos de retornos!
        //         // if (this.returnType == void.class || bodyReturn == Primitive.VOID) return null;
        //         if (returnType == void.class || !(result instanceof ReturnControl)) return null;

        //         final Object resultValue = Primitive.unwrap(((ReturnControl) result).value);
        //         // result = Primitive.unwrap(result);

        //         // final Object result = this.returnType.isPrimitive() ? Primitive.unwrap(bodyReturn) : bodyReturn;
        //         if (returnType == null) return resultValue;

        //         try {
        //             return Primitive.unwrap(Types.castObject(resultValue, returnType, Types.ASSIGNMENT));
        //         } catch (UtilEvalError e) {
        //             throw e.toEvalError(this, bodyCS);
        //         }
        //         // final String msg = String.format("Can't assign %s to %s", Types.prettyName(Types.getType(result)), Types.prettyName(returnType));
        //         // throw new EvalError(msg, null, bodyCS);
    
        //     // } catch (TargetError e) {
        //     //     for (Class<?> exceptionType: exceptionTypes)
        //     //         if (exceptionType.isInstance(e.getTarget()))
        //     //             throw e.getTarget();
        //     //     throw new InterpreterError("Can't invoke method: Unexpected Exception: " + e.getTarget().getMessage(), e.getTarget());
        //     // } catch (EvalError e) {
        //     //     throw new InterpreterError("Can't invoke method: " + e.getMessage(), e);
        //     // }
        // };

        // NameSpace namespace = callstack.top();
        return new BshMethod(mods, returnType == null ? Object.class : returnType, name, parameters, this.getExceptionTypes(callStack, interpreter), body);
    }

    protected boolean isValidConstructor(String classSimpleName) throws EvalError {
        return this.isLooseTypedReturn() && this.name.equals(classSimpleName);
    }

    protected BshConstructor toConstructor(CallStack callStack, Interpreter interpreter, boolean isEnum) throws EvalError {
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
        final int mods = this.modifiers.getModifiers() & BshModifier.CONSTRUCTOR_MODIFIERS;
        final NameSpace declaringNameSpace = callStack.top();
        final BshParameter[] parameters = this.getParamsNode().toParameters(callStack, interpreter, isEnum);

        final BshConstructor.BodyFunction<?> body = (_callStack, _class, thisArg, args) -> {
            final This __this = thisArg != null ? BshClass.fromGeneratedClass(_class).getThisFromObject(thisArg) : null;
            final NameSpace nameSpace = new NameSpace(declaringNameSpace, _class.getName() + ".<init>", _class, __this);
            nameSpace.setArguments(parameters, args);

            // System.out.println("-----------------------------------------");
            // System.out.println("BshConstructor.body.() -> _class: " + _class);
            // System.out.println("BshConstructor.body.() -> thisArg: " + thisArg);
            // System.out.println("BshConstructor.body.() -> args: " + Arrays.asList(args));
            // this.getBlockNode().dump("");
            // System.out.println("-----------------------------------------");
            
            _callStack.push(nameSpace);
            this.getBlockNode().eval(new CallStack(nameSpace), interpreter);
            _callStack.pop();
        };

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

        // TODO: adicionar o suporte aos parâmetros implícitos de Enums!
        // this.getBlockNode().eval(bodyCS, interpreter);
        final Node[] blockNodes = this.getBlockNode().jjtGetChildren();
        final BSHMethodInvocation firstMethodInvocationNode = blockNodes.length > 0 && blockNodes[0] instanceof BSHMethodInvocation ? ((BSHMethodInvocation) blockNodes[0]) : null;
        final BSHMethodInvocation constructorChainNode = firstMethodInvocationNode != null && (firstMethodInvocationNode.methodName.equals("this") || firstMethodInvocationNode.methodName.equals("super")) ? firstMethodInvocationNode : null;

        // this.getBlockNode().eval(callStack, interpreter)

        boolean superChain;
        int chainArgsLength;
        BshConstructor.ChainArgsSupplier<?> chainArgsSupplier;

        if (constructorChainNode == null) {
            // TODO: ver um suporte melhor doq essa merda para constructor chain
            superChain = true;
            chainArgsLength = isEnum ? 2 : 0;
            chainArgsSupplier = (_callStack, _class, thisArg, args) -> isEnum ? new Object[] { args[0], args[1] } : new Object[0];
        } else {
            superChain = constructorChainNode.methodName.equals("super");
            chainArgsLength = constructorChainNode.getArgsNode().jjtGetNumChildren() + (isEnum ? 2 : 0);
            chainArgsSupplier = (_callStack, _class, thisArg, args) -> {
                final This __this = thisArg != null ? BshClass.fromGeneratedClass(_class).getThisFromObject(thisArg) : null;
                final NameSpace nameSpace = new NameSpace(declaringNameSpace, _class.getName() + ".<init>", _class, __this);
                nameSpace.setArguments(parameters, args);

                _callStack.push(nameSpace);
                final Object[] baseChainArgs = constructorChainNode.getArgsNode().getArguments(_callStack, interpreter);
                _callStack.pop();
                if (!isEnum) return baseChainArgs;

                final Object[] chainArgs = new Object[chainArgsLength];
                chainArgs[0] = args[0];
                chainArgs[1] = args[1];
                System.arraycopy(baseChainArgs, 0, chainArgs, 2, baseChainArgs.length);
                return chainArgs;
            };
        }


        // TODO: oq fazer caso o constructor de um Enum já tenha um parâmetro explícito chamado de 'name' ou 'ordinal' ?

        // NameSpace namespace = callstack.top();
        return new BshConstructor(
            mods,
            parameters,
            this.getExceptionTypes(callStack, interpreter),
            superChain,
            chainArgsLength,    // TODO: see it!
            chainArgsSupplier,  // TODO: see it!
            body
        );
    }

    public String toString() {
        return super.toString() + ": " + name;
    }
}
