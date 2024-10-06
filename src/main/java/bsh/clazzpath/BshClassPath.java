package bsh.clazzpath;

import java.util.Objects;

final class BshPackage {
    private final BshClassPath classPath;
    private final String name; // e.g., 'java.util'
    private final String[] classNames; // This is an array with just the public classes e.g., 'Map', 'List', 'Map$Entry'

    public final boolean containsClass(String className) {
        for (String _className: this.classNames)
            if (Objects.equals(_className, className))
                return true;
        return false;
    }

    public final byte[] getClassBytes(String className) {
        if (!this.containsClass(className)) throw new IllegalArgumentException("There is no class with this name!");
        String classFilePath = this.name.replace('.', '/') + "/" + className;
    }
}

public final class BshClassPath {
    private final String path; // The file path to read the classes e.g., '/home/foo/my/dir', '/home/foo/libs/my-lib.jar'
    private final BshPackage[] packages;

    {
        Thread.currentThread().getContextClassLoader().getDefinedPackage(path);
    }

}
