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

class BSHPrimaryExpression extends SimpleNode {
    private static final long serialVersionUID = 1L;
    private Object cached = null;
    boolean isArrayExpression = false;
    boolean isMapExpression = false;

    BSHPrimaryExpression(int id) { super(id); }

    /** Clear the eval cache.  */
    public void clearCache() {
        cached = null;
    }

    /** Called from BSHArrayInitializer during node creation informing us
     * that we are an array expression.
     * If parent BSHAssignment has an ASSIGN operation then this is a map
     * expression. If the initializer reference has multiple dimensions
     * it gets configure as being a map in array.
     * @param init reference to the calling array initializer */
    void setArrayExpression(BSHArrayInitializer init) { // TODO: verificar isso
        this.isArrayExpression = true;
        if ( parent instanceof BSHAssignment
                && ((BSHAssignment) parent).operator != null
                && (isMapExpression = (((BSHAssignment) parent).operator
                        == ParserConstants.ASSIGN))
                && init.jjtGetParent() instanceof BSHArrayInitializer )
            init.setMapInArray(true);
    }

    /** Evaluate to a value object. */
    // public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    //     return eval( callstack, interpreter );
    // }

    /** Evaluate to a value object. */
    public LHS toLHS(CallStack callStack, Interpreter interpreter) throws EvalError {
        // // loosely typed map expression new {a=1, b=2} are treated
        // // as non assignment (LHS) to retrieve Map.Entry key values
        // // then wrapped in a MAP_ENTRY type LHS for value assignment.
        // return (LHS) eval( interpreter.getStrictJava() || !isMapExpression, callstack, interpreter );

        // try {
            // final NameSpace nameSpace = callStack.top();

            if (this.jjtGetNumChildren() == 1) {
                if (!(this.jjtGetChild(0) instanceof BSHAmbiguousName)) // TODO: ver melhor esse EvalError
                    throw new EvalError("The left-hand side of an assignment must be a variable", this, callStack);

                final BSHAmbiguousName nameNode = this.jjtGetChild(0);
                return nameNode.toLHS(callStack, interpreter);
            }

            Object baseObj = this.jjtGetChild(0).eval(callStack, interpreter);
            for (int i = 1; i < this.jjtGetNumChildren() - 1; i++) {
                final BSHPrimarySuffix suffixNode = this.jjtGetChild(i);
                baseObj = suffixNode.doSuffix(baseObj, callStack, interpreter);
            }

            final BSHPrimarySuffix lastSuffixNode = this.jjtGetChild(this.jjtGetNumChildren() - 1);
            return lastSuffixNode.toLHS(baseObj, callStack, interpreter);
        // } catch (UtilEvalError e) {
        //     throw e.toEvalError(null, callStack); // TODO: verificar isso
        // }
    }

    // /*
    //     Our children are a prefix expression and any number of suffixes.
    //     <p>

    //     We don't eval() any nodes until the suffixes have had an
    //     opportunity to work through them.  This lets the suffixes decide
    //     how to interpret an ambiguous name (e.g. for the .class operation).
    // */
    // TODO: remover o 'toLHS', isso não deveria ser tratado aqui!
    public Object eval(CallStack callStack, Interpreter interpreter) throws EvalError {
        Object baseObj = this.jjtGetChild(0) instanceof BSHType
                            ? new ClassIdentifier( this.<BSHType>jjtGetChild(0).getType(callStack, interpreter) )
                            : this.jjtGetChild(0).eval(callStack, interpreter);

        for (int i = 1; i < this.jjtGetNumChildren(); i++) {
            final BSHPrimarySuffix suffixNode = this.jjtGetChild(i);
            baseObj = suffixNode.doSuffix(baseObj, callStack, interpreter);
        }
        return baseObj;

        // TODO: verificar as outras possibilidades no código abaixo:

        // // We can cache array expressions evaluated during type inference
        // if ( isArrayExpression && null != cached )
        //     return cached;

        // Object obj = jjtGetChild(0);

        // for( int i=1; i < jjtGetNumChildren(); i++ )
        //     obj = ((BSHPrimarySuffix) jjtGetChild(i)).doSuffix(obj, callStack, interpreter);

        // /*
        //     If the result is a Node eval() it to an object or LHS
        //     (as determined by toLHS)
        // */
        // if ( obj instanceof Node )
        //     if ( obj instanceof BSHAmbiguousName )
        //         if ( toLHS ) // TODO: n deveria ter um .toLHS() no BSHAmbiguousName
        //             obj = ((BSHAmbiguousName) obj).toLHS(callStack, interpreter);
        //         else
        //             obj = ((BSHAmbiguousName) obj).toObject(callStack, interpreter);
        //     else
        //         // Some arbitrary kind of node
        //         if ( toLHS ) // is this right?
        //             throw new EvalException("Can't assign to prefix.", this, callStack );
        //         else
        //             obj = ((Node) obj).eval(callStack, interpreter);

        // if ( isMapExpression ) {
        //     if ( obj == Primitive.VOID )
        //         throw new EvalException("illegal use of undefined variable or 'void' literal", this, callStack );
        //     // we have a valid map expression return an assignable Map.Entry
        //     obj = new LHS(obj);
        // }

        // if ( isArrayExpression )
        //     cached = obj;
        // return obj;
    }
}

