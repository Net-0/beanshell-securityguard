package bsh.internals.type;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

// e.g., ?
//       ? super T
//       ? extends T
public final class BshWildcardType implements WildcardType {
    private final Type[] lowerBounds; // e.g., ? super T
    private final Type[] upperBounds; // e.g., ? extends T

    // TODO: tem q verificar conflito entre upperBounds!
    // TODO: tem q verificar para n ter lowerBound e upperBound ao msm tempo!
    public BshWildcardType(Type lowerBound, Type[] upperBounds) {
        if (lowerBound != null && upperBounds != null)
            throw new IllegalArgumentException("Can't have both lower-bound and upper-bounds simultaneously");
        if (upperBounds != null && upperBounds.length == 0)
            throw new IllegalArgumentException("Can't an empty array be an upper-bounds!");

        // TODO: os valores default de lowerBound e upperBound n deveriam ser definidos aqui!
        this.lowerBounds = lowerBound == null ? new Type[0] : new Type[] { lowerBound };
        this.upperBounds = upperBounds == null ? new Type[] { Object.class } : upperBounds;
    }

	@Override
	public Type[] getLowerBounds() {
		return this.lowerBounds;
	}

	@Override
	public Type[] getUpperBounds() {
		return this.upperBounds;
	}

    // TODO: ver um getTypeName() e/ou um toString() para esse type também!!!

}

// // TODO: make unit test for it too!
// if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
//     WildcardType wildcard = (WildcardType) type;
//     Type bound = wildcard.getLowerBounds().length > 0 ? wildcard.getLowerBounds()[0] : wildcard.getUpperBounds()[0];
//     return getRawType(bound);
// }