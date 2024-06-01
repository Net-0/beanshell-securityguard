package bsh;

class BSHLambdaExpression extends SimpleNode {

    String singleParamName;

    private boolean initializedValues = false;
    private Modifiers[] paramsModifiers;
    private Class<?>[] paramsTypes;
    private String[] paramsNames;
    private SimpleNode body;

    public BSHLambdaExpression(int i) {
        super(i);
    }

    private void initValues(CallStack callstack) throws EvalError {
        if (this.initializedValues) return;
        if (this.jjtGetNumChildren() == 2) {
            BSHFormalParameters parameters = (BSHFormalParameters) this.jjtGetChild(0);
            this.paramsTypes = parameters.eval(callstack, null);
            this.paramsModifiers = parameters.getParamModifiers();
            this.paramsNames = parameters.getParamNames();
            this.body = (SimpleNode) this.jjtGetChild(1);
        } else {
            this.paramsTypes = new Class[] { null };
            this.paramsModifiers = new Modifiers[] { null };
            this.paramsNames = new String[] { this.singleParamName };
            this.body = (SimpleNode) this.jjtGetChild(0);
        }
        this.initializedValues = true;
    }

    @Override
    public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
        System.out.println("---------------------------------");
        this.dumpClasses("");
        System.out.println("---------------------------------");
        this.initValues(callstack);
        return BshLambda.fromLambdaExpression(this, callstack.top(), this.paramsModifiers, this.paramsTypes, this.paramsNames, this.body);
    }

    @Override
    public Class<?> getEvalReturnType(NameSpace nameSpace) throws EvalError {
        CallStack callStack = new CallStack(nameSpace);
        this.initValues(callStack);
        BshLambda lambda = BshLambda.fromLambdaExpression(this, nameSpace, this.paramsModifiers, this.paramsTypes, this.paramsNames, this.body);
        return lambda.dummyType;
    }

}
