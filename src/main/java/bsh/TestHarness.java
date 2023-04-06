package bsh;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import bsh.congo.parser.Node;
import bsh.congo.parser.BeanshellParser;

public class TestHarness {

    static private FileSystem fileSystem = FileSystems.getDefault();

    static public void main(String[] args) throws IOException {
        if (args.length ==0) {
            usage();
        }
        Path path = fileSystem.getPath(args[0]);
        if (!Files.exists(path)) {
            System.err.println("File " + path + " does not exist.");
             System.exit(-1);
        }
        BeanshellParser parser = new BeanshellParser(path);
        List<Node> statements = parser.Statements();
        if (statements.size() == 1) statements.get(0).dump();
        for (Node n : parser.Statements()) {
            System.out.println(n.getClass().getSimpleName() + ":" + n.getLocation());
            System.out.println("------");
            n.dump();
            System.out.println("------");
        }
    }

    static void usage() {
        System.err.println("Usage: java bsh.TestHarness <filename>");
        System.exit(-1);
    }
}
