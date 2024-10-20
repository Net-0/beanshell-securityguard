package bsh.internals.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.concurrent.Future;

// TODO: fazer um BshAnnotatedType para podermos implementar a parte de Annotation ?

// e.g., <T>
//       <T extends Number>
//       <T extends List<?> & Runnable>
public final class BshTypeVariable<D extends GenericDeclaration> implements TypeVariable<D> {
    private final String name;
    private final Type[] bounds;
    private final Future<D> futureGD;

    public BshTypeVariable(String name, Type[] bounds, Future<D> futureGD) {
        this.name = name;
        // TODO: os valores defaults n deveriam ser definidos aqui??
        this.bounds = bounds == null ? new Type[] { Object.class } : bounds;
        this.futureGD = futureGD;

        // CompletableFuture<D> futureGD = new CompletableFuture<>();
        // futureGD.comple
    }

	@Override // TODO: rever essa implementação
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return null;
	}

	@Override // TODO: rever essa implementação
	public Annotation[] getAnnotations() {
		return new Annotation[0];
	}

	@Override // TODO: rever essa implementação
	public Annotation[] getDeclaredAnnotations() {
		return new Annotation[0];
	}

	@Override // TODO: rever essa implementação
	public AnnotatedType[] getAnnotatedBounds() {
		return new AnnotatedType[0];
	}

	@Override
	public Type[] getBounds() {
		return this.bounds;
	}

	@Override
	public D getGenericDeclaration() throws IllegalStateException {
        if (this.futureGD.isDone())
			try {
				return this.futureGD.get();
			} catch (Exception e) {
                throw new IllegalStateException("The Generic Declaration can't be get!", e);
			}
        throw new IllegalStateException("The Generic Declaration can't be get yet!");
	}

	@Override
	public String getName() {
		return this.name;
	}
    
}

// // TODO: make a unit tests for it, it's always the first bound!!!!
// if (type instanceof TypeVariable<?>) { // Handle type variables (like T or R)
//     TypeVariable<?> typeVar = ((TypeVariable<?>) type);
//     return getRawType(typeVar.getBounds()[0]);
// }