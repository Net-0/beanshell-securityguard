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

class BSHSwitchStatement extends SimpleNode implements ParserConstants {

    public BSHSwitchStatement(int id) { super(id); }

    public Object eval(CallStack callStack, Interpreter interpreter) throws EvalError {
        final boolean strictJava = interpreter.getStrictJava();
        final NameSpace nameSpace = callStack.top();
        final int numchild = jjtGetNumChildren();
        int child = 0;
        final BSHAssignment switchExp = this.jjtGetChild(child++); // TODO: simplificar essa implementação ?
        final Object switchVal = switchExp.eval(callStack, interpreter);

        // TODO: ver isso!
        // TODO: se usávamos .importStatic() na Enum, isso n abria "brecha" para considerarmos static fields normais da classe ? isso está correto ?
        // TODO: se importávamos direto no NameSpace, isso n fazia a variável indiretamente acessível dps ?
        // // import enum constants
        // if ( Primitive.unwrap(switchVal) != null && switchVal.getClass().isEnum() )
        //     callstack.top().importStatic( switchVal.getClass() );

        if (switchVal instanceof Enum) {
            final NameSpace switchCaseNS = new NameSpace(nameSpace, nameSpace.getName() + "/SwitchCase");
            try {
                switchCaseNS.setEnumConstants(((Enum<?>) switchVal).getClass());
            } catch (UtilEvalError e) {
                throw e.toEvalError(this, callStack);
            }
            callStack.swap(switchCaseNS);
        }

        /*
            Note: this could be made clearer by adding an inner class for the
            cases and an object context for the child traversal.
        */
        // first label
        BSHSwitchLabel label;
        Node node;
        ReturnControl returnControl=null;

        // get the first label
        if ( child >= numchild )
            throw new EvalException("Empty switch statement.", this, callStack );
        label = this.jjtGetChild(child++);

        // TODO: validar que só pode ser feito switch case de alguns primitivos e Strings ?

        // while more labels or blocks and haven't hit return control
        while (child < numchild && returnControl == null) {
            // if label is default or equals switchVal
            if (label.isDefault || primitiveEquals(switchVal, label.eval(callStack, interpreter), callStack, switchExp)) {
                // execute nodes, skipping labels, until a break or return
                while (child < numchild) {
                    node = jjtGetChild(child++);
                    if (node instanceof BSHSwitchLabel)
                        continue;
                    // eval it
                    Object value = node.eval(callStack, interpreter);

                    // should check to disallow continue here?
                    if (value instanceof ReturnControl) {
                        // TODO: e se for um 'return' ou 'continue' aqui ?
                        returnControl = (ReturnControl)value;
                        break;
                    }
                }
            } else {
                // skip nodes until next label
                while (child < numchild) {
                    node = jjtGetChild(child++);
                    if ( node instanceof BSHSwitchLabel ) {
                        label = (BSHSwitchLabel) node;
                        break;
                    }
                }
            }
        }

        if (switchVal instanceof Enum)
            callStack.swap(nameSpace);

        if (returnControl != null && returnControl.kind == RETURN)
            return returnControl;
        else
            return Primitive.VOID;
    }

    /**
        Helper method for testing equals on two primitive or boxable objects.
        yuck: factor this out into Primitive.java
    */
    private boolean primitiveEquals(Object switchVal, Object targetVal, CallStack callstack, Node switchExp) throws EvalError {
        if (targetVal == Primitive.VOID)
            return false;
        if ( switchVal instanceof Primitive || targetVal instanceof Primitive )
            try {
                // binaryOperation can return Primitive or wrapper type
                Object result = Operators.binaryOperation(switchVal, targetVal, ParserConstants.EQ);
                result = Primitive.unwrap(result);
                return result.equals(Boolean.TRUE);
            } catch (UtilEvalError e) {
                throw e.toEvalError("Switch value: "+switchExp.getText()+": ", this, callstack);
            }
        else
            return switchVal.equals( targetVal );
    }

    @Override
    public String toString() {
        return super.toString() + ": switch";
    }
}

