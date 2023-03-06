import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

import java.net.URI;

class Source {

    private final CompilationUnitTree ast;
    private final SourcePositions positions;
    private final LineMap lineMap;

    Source(CompilationUnitTree ast, SourcePositions positions) {
        this.ast = ast;
        this.positions = positions;
        this.lineMap = ast.getLineMap();
    }

    URI getUri() {
        return ast.getSourceFile().toUri();
    }

    CompilationUnitTree getAst() {
        return ast;
    }

    int getLineNumber(Tree node) {
        return (int) lineMap.getLineNumber(positions.getStartPosition(ast, node));
    }
}
