/* Generated by: JJTree: Do not edit this line. BSHAutoCloseable.java Version 1.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=false,NODE_PREFIX=BSH,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
/** Copyright 2018 Nick nickl- Lombard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package bsh;

import bsh.legacy.*;
import bsh.congo.parser.Node;

public class BSHAutoCloseable extends BSHTypedVariableDeclaration {
    private static final long serialVersionUID = 1L;
    public String typeName;
    public Class<?> type;
    public String name;
    public AutoCloseable ths;
    public Variable varThis;

    public BSHAutoCloseable(int id) { super(id);  }

    public Object eval(CallStack callstack, Interpreter interpreter)
            throws EvalError {

        renderTypeNode();
        this.type = evalType(callstack, interpreter);

        if (!AutoCloseable.class.isAssignableFrom(this.getType()))
            throw new EvalError("The resource type "+ this.type.getName()
                +" does not implement java.lang.AutoCloseable.", this, callstack);

        this.name = this.getDeclarators()[0].name;

        // we let BSHTypedVariableDeclaration do the heavy lifting
        super.eval(callstack, interpreter);

        try {
            this.varThis = callstack.top().getVariableImpl(this.getName(), true);
        } catch (UtilEvalError e) {
            throw e.toEvalError("Unable to evaluate the try-with-resource "
                + this.getName() + ". With message:" + e.getMessage(),
                this, callstack);
        }

        return Primitive.VOID;
    }

    public String getName() {
        return name;
    }

    public void close() {
        try {
            if (null != this.varThis)
                this.ths = (AutoCloseable) this.varThis.getValue();
            if (null != this.ths)
                this.ths.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Class<?> getType() {
        return this.type;
    }

    /** We may not always have a type node (loose typed resources).
     * Then we create the BSHType node and get the type
     * from the BSHVariableDeclarator AllocationExpression nodes. */
    private void renderTypeNode() {
        if (getChildCount() == 1) {
            Node tNode = new BSHType(ParserTreeConstants.JJTTYPE);
            Node ambigName = getChild(0);
            while (ambigName.getChildCount() > 0)
                if ((ambigName = ambigName.getChild(0)) instanceof BSHAmbiguousName)
                    break;
            BSHAmbiguousName ambigNew =
                    new BSHAmbiguousName(ParserTreeConstants.JJTAMBIGUOUSNAME);
            ambigNew.setParent(tNode);
            ambigNew.text = ((BSHAmbiguousName) ambigName).text;
            tNode.addChild(0, ambigNew);
            tNode.setParent(this);
            setNodes(tNode, getChild(0));
        }
    }
}
