package bsh;

class BSHSpecialMembers extends SimpleNode {

    String base; // "this" or "super"
    String member;

    int callerCount; // How many times we must get the ".caller"

    BSHSpecialMembers(int i) { super(i); }

    @Override
    public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
        switch (this.member) {
            case "callstack": return callstack;
            case "namespace": return callstack.top();
            case "variables": return callstack.top().getVariableNames();
            case "methods": return callstack.top().getMethodNames();
            case "interpreter": {
                if (!this.base.equals("this"))
                    throw new EvalError("Can only call .interpreter on literal 'this'", this, callstack);
                return interpreter;
            }
            case "caller": {
                if (!this.base.equals("this"))
                    throw new EvalError("Can only call .caller on literal 'this' or literal '.caller'", this, callstack);
                return callstack.get( this.callerCount ).getThis( interpreter );
            }
            default: throw new EvalError("Can't resolve " + this.base + "." + this.member, this, callstack);
        }
    }

}
