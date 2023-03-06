import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    private static final JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

    public static void main(String[] args) {
        Set<File> sourceFiles = Arrays.stream(args).map(File::new).collect(Collectors.toSet());
        for (Source source: parseFiles(sourceFiles)) {
            Deque<Swappable> swappables = new LinkedList<>();
            new SwappableGeneratingTreeScanner(source, swappables).scan(source.getAst(), null);
            String result = String.join("", swappables.stream().map(Swappable::toString).toList());
            Path path = Paths.get(source.getUri());
            path = path.resolveSibling(path.getFileName() + "-lines.yaml");
            try {
                Files.write(path, result.getBytes());
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }

    // adapted from https://github.com/jplag/JPlag/blob/main/languages/java/src/main/java/de/jplag/java/JavacAdapter.java
    public static Iterable<Source> parseFiles(Set<File> files) {
        Collection<Source> sources = new LinkedList<>();
        var listener = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = javac.getStandardFileManager(listener, null, StandardCharsets.UTF_8)) {
            var javaFiles = fileManager.getJavaFileObjectsFromFiles(files);
            JavaCompiler.CompilationTask task = javac.getTask(null, fileManager, listener, List.of("-proc:none"), null, javaFiles);
            Trees trees = Trees.instance(task);
            SourcePositions positions = trees.getSourcePositions();
            for (CompilationUnitTree ast : ((JavacTask) task).parse()) {
                sources.add(new Source(ast, positions));
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
            System.exit(-1);
        }
        return sources;
    }
}
