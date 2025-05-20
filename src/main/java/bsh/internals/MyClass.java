package bsh.internals;

// TODO: oq aconteceria nesse cenário ?
public class MyClass {

    public MyClass() {
        // TODO: Cannot refer to 'this' nor 'super' while explicitly invoking a constructor
        // this(this);
        this(1, 2, 3);
    }

    public MyClass(Object obj) {
        
    }

    public MyClass(int a, int b, int c) {

    }
    
}
