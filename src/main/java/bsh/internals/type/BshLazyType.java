package bsh.internals.type;

import java.io.InvalidClassException;
import java.lang.reflect.Type;
import java.util.concurrent.Future;

// TODO: como fica os tipos q são arrays ?
// TODO: como fica a questão de types q apontam para uma Generated Class antes de ela ter sido carregada no BshClassManager ??

// TODO: realmente precisa disso se BshClass é um Type ?
public final class BshLazyType implements Type {
    private final String className;
    private final int arrayDimensions;
    // private final Future<Class<?>> futureClass;

    public BshLazyType(String className, int arrayDimensions) {
        this.className = className;
        this.arrayDimensions = arrayDimensions;
        // this.futureClass = futureClass;
    }

    @Override
    public final String getTypeName() {
        final char[] arraySuffixes = new char[this.arrayDimensions*2];
        for (int i = 0; i < this.arrayDimensions;) {
            arraySuffixes[i++] = '[';
            arraySuffixes[i++] = ']';
        }
        // TODO: o sufixo de arrayDimensions está funcionando ?
        return this.className + new String(arraySuffixes);
    }

    public final String getDescriptor() {
        final char[] arrayChars = new char[this.arrayDimensions];
        for (int i = 0; i < this.arrayDimensions; i++)
            arrayChars[i] = '[';
        // final String arrayDescriptor = new String(arrayChars); // Specify how many array dimension this type has
        return new String(arrayChars) + 'L' + this.className.replace('.', '/') + ';';
    }

    public final String getInternalName() {
        // TODO: manter essa validação ?
        if (this.arrayDimensions != 0)
            throw new IllegalStateException("Can't get the internal name of the BshLazyType because it's array type");
        return this.className.replace('.', '/');
    }

    // public final Class<?> toClass() throws IllegalStateException {
    //     if (this.futureClass.isDone())
	// 		try {
	// 			return this.futureClass.get();
	// 		} catch (Exception e) {
    //             throw new IllegalStateException("The Class<?> can't be get!", e);
	// 		}
    //     throw new IllegalStateException("The Class<?> can't be get yet!");
    // }

    // // if (type instanceof TypeVariable<?>) // Handle type variables (like T or R)
    // //     return "T" + ((TypeVariable<?>) type).getName() + ";";
    // static TypeVariable<?> createTypeVariable(String name, Type[] bounds) {
    //     // TODO: see it!
    //     // bounds = bounds == null || bounds.length == 0 ? new Type[] { Object.class } : bounds;

    //     return new TypeVariable<GenericDeclaration>() {
    //         public String getName() { return name; }
    //         public <T extends Annotation> T getAnnotation(Class<T> annotationClass) { return null; }
    //         public Annotation[] getAnnotations() { return new Annotation[0]; }
    //         public Annotation[] getDeclaredAnnotations() { return new Annotation[0]; }
    //         public AnnotatedType[] getAnnotatedBounds() { throw new UnsupportedOperationException("Unimplemented method 'getAnnotatedBounds'"); }
    //         public Type[] getBounds() { return bounds; }
    //         public GenericDeclaration getGenericDeclaration() { throw new UnsupportedOperationException("Unimplemented method 'getGenericDeclaration'"); };
    //     };
    // }

    // static ParameterizedType createParameterizedType(Type rawType, Type[] typeArguments) {
    //     return new ParameterizedType() {
    //         public Type[] getActualTypeArguments() { return typeArguments; }
    //         public Type getRawType() { return rawType; }
    //         public Type getOwnerType() { return null; }
    //     };
    // }

}
