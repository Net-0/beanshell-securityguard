// package bsh.internals;

// import java.util.HashSet;
// import java.util.Set;

// // TODO: remover o 'public'!
// // TODO: melhorar essa classe!
// class BshThis {
//     protected final Object thisObj; // TODO: The instance being wrapped
//     // TODO: trocar a validação, o + correto seria validar se o último que chamou foi o 'BshConstructor'!
//     protected boolean constructing = false; // TODO: indicate if we're inside a constructor!
//     // TODO: lançar exception para final fields q n forem setados!
//     protected final Set<String> finalFieldsSet = new HashSet<>(); // TODO: dar um .clear() when 'constructing = false'
//     // private final boolean strictJava = false; // TODO: see it!
//     // private final Map<String, Object> dynamicFields = new HashMap<>(); // TODO: dynamic created fields

//     protected BshThis(Object thisObj) { this.thisObj = thisObj; }
//     // protected Object unwrap() { return this.thisObj; }
// }
