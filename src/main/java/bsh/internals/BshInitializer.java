// package bsh.internals;

// import bsh.CallStack;
// import bsh.EvalError;
// import bsh.NameSpace;

// // TODO: apagar essa merda ??
// public class BshInitializer {
//     protected BshField field;
//     protected BshFunction<CallStack, ?> function;

//     private BshClass declaringClass;

//     public BshInitializer(BshField field) {
//         this.field = field;
//     }

//     public BshInitializer(BshFunction<CallStack, ?> function) {
//         this.function = function;
//     }

//     // TODO: remover esse setDeclaringClass daqui ? só redundância desnecessária!
//     protected final void setDeclaringClass(BshClass bshClass) {
//         if (this.declaringClass != null)
//             throw new IllegalStateException("Can't re-define the declaring class!");
//         this.declaringClass = bshClass;
//     }

//     public Object initialize() throws EvalError {
//         // TODO: rever essa merda
//         return null;
//         // // TODO: esse callStack n deveria ter uma referência ao declaringCallingStack ?
//         // final NameSpace nameSpace = new NameSpace(this.declaringClass.nameSpace, this.declaringClass.name, this.declaringClass);
//         // final CallStack callStack = new CallStack(nameSpace);

//         // if (this.field != null)
//         //     return this.field.initializer.apply(callStack);
//         // return this.function.apply(callStack);
//     }
// }
