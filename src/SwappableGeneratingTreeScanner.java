import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

import java.util.List;
import java.util.Set;

// adapted from https://github.com/mbrdl/JPlag/blob/java-semantic-tokens/languages/java/src/main/java/de/jplag/java/TokenGeneratingTreeScanner.java
final class SwappableGeneratingTreeScanner extends TreeScanner<Void, Void> {

    private final Source source;
    private List<Swappable> swappableList;
    private Swappable swappable; // corresponds to last element in swappableList if not null
    private VariableRegistry variableRegistry;

    private static final Set<String> IMMUTABLES = Set.of(
            // from https://medium.com/@bpnorlander/java-understanding-primitive-types-and-wrapper-objects-a6798fb2afe9
            "byte", "short", "int", "long", "float", "double", "boolean", "char", // primitives
            "Byte", "Short", "Integer", "Long", "Float", "Double", "Boolean", "Character", "String");


    SwappableGeneratingTreeScanner(Source source, List<Swappable> swappableList) { // todo oopsie daisy
        this.source = source;
        this.swappableList = swappableList; // we add to this list
        this.swappable = null;
        variableRegistry = new VariableRegistry();
    }

    private void updateSwappable(int lineNumber) {
        if (swappable == null || lineNumber != swappable.getLineNumber()) {
            swappable = new Swappable(lineNumber);
            swappableList.add(swappable);
        }
    }

    private boolean isOwnMemberSelect(MemberSelectTree memberSelect) {
        return memberSelect.toString().equals("this");
    }

    private boolean isMutable(Tree classTree) {
        // classTree is null if `var` keyword is used
        return classTree == null || !IMMUTABLES.contains(classTree.toString());
    }

    @Override
    public Void visitBlock(BlockTree node, Void unused) {
        // kind of weird since in the case of for loops and catches, two scopes are introduced
        // but I'm pretty sure that's how Java does it internally as well
        variableRegistry.enterLocalScope();
        super.visitBlock(node, null);
        variableRegistry.exitLocalScope();
        return null;
    }

    @Override
    public Void visitClass(ClassTree node, Void unused) {
        for (var member : node.getMembers()) {
            if (member.getKind() == Tree.Kind.VARIABLE) {
                VariableTree variableTree = (VariableTree) member;
                String name = variableTree.getName().toString();
                boolean mutable = isMutable(variableTree.getType());
                variableRegistry.registerMemberVariable(name, mutable);
            }
        }
        super.visitClass(node, unused);
        variableRegistry.clearMemberVariables();
        return null;
    }

    @Override
    public Void visitMethod(MethodTree node, Void unused) {
        variableRegistry.enterLocalScope();
        super.visitMethod(node, unused);
        variableRegistry.exitLocalScope();
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void unused) {
        variableRegistry.enterLocalScope();
        super.visitForLoop(node, unused);
        variableRegistry.exitLocalScope();
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
        variableRegistry.enterLocalScope();
        super.visitEnhancedForLoop(node, unused);
        variableRegistry.exitLocalScope();
        return null;
    }

    @Override
    public Void visitCatch(CatchTree node, Void unused) {
        variableRegistry.enterLocalScope();
        super.visitCatch(node, null); // can leave this since catch parameter is variable declaration and thus always generates a token
        variableRegistry.exitLocalScope();
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void unused) {
        variableRegistry.setNextOperation(NextOperation.WRITE);
        super.visitAssignment(node, null);
        return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
        variableRegistry.setNextOperation(NextOperation.READ_WRITE);
        super.visitCompoundAssignment(node, null);
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree node, Void unused) {
        if (Set.of(Tree.Kind.PREFIX_INCREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.POSTFIX_DECREMENT)
            .contains(node.getKind())) {
            variableRegistry.setNextOperation(NextOperation.READ_WRITE);
        }
        super.visitUnary(node, null);
        return null;
    }

    @Override
    public Void visitVariable(VariableTree node, Void unused) {
        if (variableRegistry.inLocalScope()) {
            String name = node.getName().toString();
            boolean mutable = isMutable(node.getType());
            Variable variable = variableRegistry.registerLocalVariable(name, mutable);
            updateSwappable(source.getLineNumber(node));
            swappable.addWrite(variable);  // manually add variable to semantics since identifier isn't visited
        } // no else since member variable defs are registered on class visit
        super.visitVariable(node, null);
        return null;
    }


    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
        scan(node.getTypeArguments(), null);
        // differentiate bar() and this.bar() (ignore) from bar.foo() (don't ignore)
        // look at cases foo.bar()++ and foo().bar++
        updateSwappable(source.getLineNumber(node));
        swappable.markUnswappable();
        variableRegistry.setIgnoreNextOperation(true);
        variableRegistry.setMutableWrite(true);
        scan(node.getMethodSelect(), null);  // foo.bar() is a write to foo
        scan(node.getArguments(), null);  // foo(bar) is a write to bar
        variableRegistry.setMutableWrite(false);
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void unused) {
        if (isOwnMemberSelect(node)) {
            updateSwappable(source.getLineNumber(node));
            variableRegistry.registerVariableOperation(node.getIdentifier().toString(), true, swappable);
        }
        variableRegistry.setIgnoreNextOperation(false);  // don't ignore the foo in foo.bar()
        super.visitMemberSelect(node, null);
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void unused) {
        updateSwappable(source.getLineNumber(node));
        variableRegistry.registerVariableOperation(node.toString(), false, swappable);
        super.visitIdentifier(node, null);
        return null;
    }
}
