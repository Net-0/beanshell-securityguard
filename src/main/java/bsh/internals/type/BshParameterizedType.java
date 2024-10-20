package bsh.internals.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class BshParameterizedType implements ParameterizedType {

    private final Type rawType;
    private final Type[] typeArguments;

    public BshParameterizedType(Type rawType, Type[] typeArguments) {
        this.rawType = rawType;
        this.typeArguments = typeArguments;
    }

	// @Override
	// public Type[] getActualTypeArguments() {
	// 	// TODO Auto-generated method stub
	// 	throw new UnsupportedOperationException("Unimplemented method 'getActualTypeArguments'");
	// }

	// @Override
	// public Type getOwnerType() {
	// 	// TODO Auto-generated method stub
	// 	throw new UnsupportedOperationException("Unimplemented method 'getOwnerType'");
	// }

	// @Override
	// public Type getRawType() {
	// 	// TODO Auto-generated method stub
	// 	throw new UnsupportedOperationException("Unimplemented method 'getRawType'");
	// }

    @Override
    public final Type[] getActualTypeArguments() { return this.typeArguments; }

    @Override
    public final Type getRawType() { return this.rawType; }

    @Override // TODO: ver oq fazer em relação à isso!
    public final Type getOwnerType() { return null; }

    @Override
    public String getTypeName() { // TODO: implementar um próprio semelhante ao feito pela OpenJDK!
        return ParameterizedType.super.getTypeName();
    }

    // static ParameterizedType createParameterizedType(Type rawType, Type[] typeArguments) {
        // return new ParameterizedType() {
        // };
    // }
}
