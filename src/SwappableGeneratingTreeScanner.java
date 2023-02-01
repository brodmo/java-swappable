import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

import javax.lang.model.element.Name;
import java.util.List;

// adapted from https://github.com/mbrdl/JPlag/blob/java-semantic-tokens/languages/java/src/main/java/de/jplag/java/TokenGeneratingTreeScanner.java
final class SwappableGeneratingTreeScanner extends TreeScanner<Void, Void> {

    private final Source source;
    private List<Swappable> swappableList;
    private Swappable swappable; // corresponds to last element in swappableList if not null
    private VariableHelper variableHelper;
    private boolean ignoreNext;
    
    SwappableGeneratingTreeScanner(Source source, List<Swappable> swappableList) {
        this.source = source;
        this.swappableList = swappableList; // we add to this list
        this.swappable = null;
        variableHelper = new VariableHelper();
        ignoreNext = false;
    }

    private void registerVariableOccurrence(Name variableName, int lineNumber) {
        Variable variable = variableHelper.getVariable(variableName.toString());
        if (variable != null) {
            updateSwappable(lineNumber);
            swappable.addConflict(variable.id().toString());
        }
    }

    private void updateSwappable(int lineNumber) {
        if (swappable == null || lineNumber != swappable.getLineNumber()) {
            swappable = new Swappable(lineNumber);
            swappableList.add(swappable);
        }
    }

    private boolean isOwnMemberSelect(MemberSelectTree memberSelect) {
        return memberSelect.getExpression().toString().equals("this");
    }

    private boolean isVariable(ExpressionTree expressionTree) {
        return expressionTree.getKind() == Tree.Kind.IDENTIFIER
                || (expressionTree.getKind() == Tree.Kind.MEMBER_SELECT && isOwnMemberSelect((MemberSelectTree) expressionTree));
    }

    @Override
    public Void visitBlock(BlockTree node, Void unused) {
        // kind of weird since in the case of for loops and catches, two scopes are introduced
        // but I'm pretty sure that's how Java does it internally as well
        variableHelper.enterLocalScope();
        super.visitBlock(node, unused);
        variableHelper.exitLocalScope();
        return null;
    }

    @Override
    public Void visitMethod(MethodTree node, Void unused) {
        variableHelper.enterLocalScope();
        super.visitMethod(node, unused);
        variableHelper.exitLocalScope();
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void unused) {
        variableHelper.enterLocalScope();
        super.visitForLoop(node, unused);
        variableHelper.exitLocalScope();
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
        variableHelper.enterLocalScope();
        super.visitEnhancedForLoop(node, unused);
        variableHelper.exitLocalScope();
        return null;
    }

    @Override
    public Void visitCatch(CatchTree node, Void unused) {
        variableHelper.enterLocalScope();
        super.visitCatch(node, unused);
        variableHelper.exitLocalScope();
        return null;
    }

    @Override
    public Void visitVariable(VariableTree node, Void unused) {
        if (variableHelper.inLocalScope()) {
            String name = node.getName().toString();
            variableHelper.registerLocalVariable(name);
            registerVariableOccurrence(node.getName(), source.getLineNumber(node));
        }
        super.visitVariable(node, unused);
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void unused) {
        if (!ignoreNext) {
            registerVariableOccurrence(node.getName(), source.getLineNumber(node));
        }
        ignoreNext = false;
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
        super.visitMethodInvocation(node, unused);
        if (isVariable(node.getMethodSelect())) {
            ignoreNext = true;
        }
        updateSwappable(source.getLineNumber(node));
        swappable.notSwappable();
        return null;
    }
}
