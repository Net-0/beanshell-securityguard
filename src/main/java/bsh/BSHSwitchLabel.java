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

class BSHSwitchLabel extends SimpleNode {
    boolean isDefault;

    public BSHSwitchLabel(int id) { super(id); }

    public Object eval(CallStack callStack, Interpreter interpreter) throws EvalError {
        // return jjtGetChild(0).eval(callStack, interpreter);
        final Node valueNode = this.jjtGetChild(0);

        try {
            return valueNode.eval(callStack, interpreter);
        } catch (EvalError e) {
            final String valueNodeText = valueNode.getText().trim();
            // System.out.println("BSHSwitchLabel.eval() -> strictJava: " + strictJava);
            // System.out.println("BSHSwitchLabel.eval() -> valueNode.getText(): " + valueNode.getText());
            // System.out.println("BSHSwitchLabel.eval() -> valueNode.getText().matches(\"\\w+\"): " + valueNode.getText().trim().matches("\\w+"));

            // If it's strict java or the valueNode isn't just a simple name, then we can re-throw the error
            if (interpreter.getStrictJava() || !valueNodeText.matches("\\w+"))
                throw e;

            // System.out.println("BSHSwitchLabel.eval() -> e.getCause() == null: " + e.getCause() == null);
            // System.out.println("BSHSwitchLabel.eval() -> e.getCause().getMessage().startsWith(\"Can't resolve the name \"): " + e.getCause().getMessage().startsWith("Can't resolve the name "));
            
            // TODO: melhorar essas mensagens do EvalError tá uma merda validar isso! Talvez um stack trace customizado ? Isso seria muito maneiro!
            // If the error message isn't that wasn't able to resolve 
            if (e.getCause() == null || !e.getCause().getMessage().startsWith("Can't resolve the name " + valueNodeText))
                throw e;

            return Primitive.VOID;
        }
    }

    @Override
    public String toString() {
        return super.toString() + ": " + (isDefault ? "default" : "case");
    }
}
