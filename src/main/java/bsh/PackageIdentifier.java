package bsh;

/**
 * This class wraps a identifier to be resolved to a class.
 * It can load all packages untill reach the class, e.g.:
 *
 * <pre>
 * pkg1 = new Packageidentifier(classManager, "java"); // "java"
 * pkg2 = pkg1.getSubPackage("util"); // "java.util"
 * pkg3 = pkg2.getSubPackage("function"); // "java.util.function"
 * clazz = pkg3.getClass("Predicate"); // "java.util.function.Predicate"
 * </pre>
 */
class PackageIdentifier {
    private final BshClassManager classManager;
    private final String packageName;
    // private final List<String> availablePackages;

    PackageIdentifier(BshClassManager classManager, String identifier) {
        this.classManager = classManager;
        this.packageName = identifier;
    }

    // /** It's just a util class to we be able to get all the packages of a {@link ClassLoader} */
    // private static class ClassLoaderDumper extends ClassLoader {
    //     protected ClassLoaderDumper(ClassLoader parent) { super(parent); }
    //     public Package[] getPackages() { return super.getPackages(); }
    // }

    // /** Get a package by its name of a {@link BshClassManager} */
    // static Identifier from(String packageName, BshClassManager classManager) {
    //     List<String> availablePackages = classManager.getPackages().stream()
    //                                                 .filter((pkg) -> pkg.startsWith(packageName))
    //                                                 .collect(Collectors.toList());
    //     if (availablePackages.size() > 0) return new Identifier(classManager, packageName, availablePackages);
    //     return null;
    // }

    /**
     * Get a sub-package by it's name.
     *
     * <p>
     * E.g.: Supose that this package is <code>java</code>,
     * then you get the sub-package by name <code>"lang"</code>,
     * this method returns the package <code>java.lang</code>.
     *
     */
    // * <p>
    // * Obs.: return <code>null</code> if there is no package with the specified name
    PackageIdentifier getSubPackage(String name) {
        // final String packageName = this.identifier + "." + name;
        // List<String> availablePackages = this.availablePackages.stream()
        //                                       .filter(p -> p.startsWith(identifier))
        //                                       .collect(Collectors.toList());
        // if (availablePackages.size() > 0) return new Identifier(this.classManager, packageName, availablePackages);

        return new PackageIdentifier(this.classManager, this.packageName + "." + name);
    }

    /**
     * Get a class inside this package by it's name.
     *
     * <p>
     * E.g.: Supose that this package is <code>java.lang</code>,
     * then you get the class by name <code>"Integer"</code>,
     * this method returns the class <code>java.lang.Integer</code>
     *
     * <p>
     * Obs.: return <code>null</code> if there is no class with the specified name
     */
    ClassIdentifier getClass(String name) {
        final String className = this.packageName + "." + name;
        // for (String pkg: this.availablePackages)
        //     if (pkg.equals(this.identifier))
        //         return new ClassIdentifier(this.classManager.classForName(className));
        Class<?> clazz = this.classManager.classForName(className);
        return clazz == null ? null : new ClassIdentifier(clazz);
    }

    public String toString() {
        return "Package Identifier: " + this.packageName;
    }

}
