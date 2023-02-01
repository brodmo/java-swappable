import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

import java.net.URI;

public class Source {

    private final CompilationUnitTree ast;
    private final SourcePositions positions;
    private final LineMap lineMap;

    public Source(CompilationUnitTree ast, SourcePositions positions) {
        this.ast = ast;
        this.positions = positions;
        this.lineMap = ast.getLineMap();
    }

    public URI getUri() {
        return ast.getSourceFile().toUri();
    }

    public CompilationUnitTree getAst() {
        return ast;
    }

    public int getLineNumber(Tree node) {
        return (int) lineMap.getLineNumber(positions.getStartPosition(ast, node));
    }
}
