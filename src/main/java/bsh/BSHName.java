package bsh;

import java.lang.reflect.Array;

class BSHName extends SimpleNode {
    String name;

    BSHName(int id) { super(id); }

    @Override
    public Object eval(CallStack callStack, Interpreter interpreter) throws EvalError {
        try {
            NameSpace nameSpace = callStack.top();
            if (this.name.equals("this")) return this.resolveThis(nameSpace, interpreter);
            if (this.name.equals("super")) return this.resolveSuper(nameSpace, interpreter);
    
            if ( this.name.equals("global") ) {
                This global = nameSpace.getGlobal( interpreter );
                if (global != null) return global;
            }
    
            Object variable = nameSpace.getVariable(this.name, true);
            if (variable != Primitive.VOID) return variable;
    
            return nameSpace.getPackageIdentifier(this.name);
        } catch (UtilEvalError e) {
            throw e.toEvalError(this, callStack);
        }
    }

    Object eval(Object baseObj, CallStack callStack, Interpreter interpreter) throws EvalError {
        if (baseObj instanceof ClassIdentifier) return this.eval((ClassIdentifier) baseObj, callStack, interpreter);
        // if (baseObj instanceof NameSpace) return this.eval((NameSpace) baseObj, interpreter);

        if (baseObj instanceof PackageIdentifier) {
            PackageIdentifier pkg = (PackageIdentifier) baseObj;
            ClassIdentifier _class = pkg.getClass(this.name);
            return _class != null ? _class : pkg.getSubPackage(this.name);
        }

        // length access on array?
        if (this.name.equals("length") && baseObj.getClass().isArray())
            return new Primitive(Array.getLength(baseObj));

        // Check for field on object
        try {
            return Reflect.getObjectFieldValue(baseObj, this.name);
        } catch(UtilEvalError e) {
            throw e.toEvalError(this, callStack);
        }
    }

    private Object eval(ClassIdentifier baseClass, CallStack callStack, Interpreter interpreter) throws EvalError {
        try {
            NameSpace namespace = callStack.top();
            Class<?> clas = baseClass.getTargetClass();
    
            // Class qualified 'this' reference from inner class. e.g. 'MyOuterClass.this'
            if ( this.name.equals("this") ) {
                // find the enclosing class instance space of the class name
                for (NameSpace ns = namespace; ns != null; ns = ns.getParent())
                    if ( ns.classInstance != null && ns.classInstance.getClass() == clas)
                        return ns.classInstance;
                throw new EvalError("Can't find enclosing 'this' instance of class: "+clas, this, callStack);
            }
    
            // static field?
            try {
                return Reflect.getStaticFieldValue(clas, this.name);
            } catch (ReflectError e) {}
    
            // inner class?
            String innerClassName = clas.getName() + "$" + this.name;
            Class<?> innerClass = namespace.getClass(innerClassName);
            if (null == namespace.classInstance && Reflect.isGeneratedClass(innerClass) && !Reflect.getClassModifiers(innerClass).hasModifier("static"))
                throw new EvalError("an enclosing instance that contains " + clas.getName() + "." + this.name + " is required", this, callStack);
            if ( innerClass != null ) return new ClassIdentifier(innerClass);
    
            // static bean property
            return Reflect.getObjectProperty(clas, this.name);
        } catch (UtilEvalError e) {
            throw e.toEvalError(this, callStack);
        }
    }

    private This resolveThis(NameSpace thisNameSpace, Interpreter interpreter) {
        // Init this for block namespace and methods
        This thiz = thisNameSpace.getThis( interpreter );
        thisNameSpace = thiz.getNameSpace();

        // This is class namespace or instance reference
        NameSpace classNameSpace = thisNameSpace.getClassNameSpace();

        return classNameSpace != null ? classNameSpace.getThis( interpreter ) : thiz;
    }

    private This resolveSuper(NameSpace thisNameSpace, Interpreter interpreter) {
        This zuper = thisNameSpace.getSuper( interpreter ); // Allow getSuper() to go through BlockNameSpace to the method's super
        thisNameSpace = zuper.getNameSpace();
        // super is now the closure's super or class instance

        // If we're a class instance and the parent is also a class instance then super means our parent.
        if ( thisNameSpace.getParent() != null && thisNameSpace.getParent().isClass )
            return thisNameSpace.getSuper( interpreter );

        return zuper;
    }
}

