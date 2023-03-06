import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to assist in generating token semantics. For languages similar in structure to Java/C
 */
class VariableRegistry {
    private Map<String, Variable> unscopedVariables;
    private Deque<Map<String, Variable>> memberVariables; // map member variable name to stack of variables
    private Map<String, Deque<Variable>> localVariables; // map local variable name to stack of variables
    private Deque<Set<String>> localVariablesByScope; // stack of local variable names in scope
    private NextOperation nextOperation;
    private boolean ignoreNextOperation;
    private boolean mutableWrite;

    VariableRegistry() {
        this.unscopedVariables = new HashMap<>();
        this.memberVariables = new LinkedList<>();
        this.localVariables = new HashMap<>();
        this.localVariablesByScope = new LinkedList<>();
        this.nextOperation = NextOperation.READ; // the default
        this.ignoreNextOperation = false;
        this.mutableWrite = false;
    }

    void setNextOperation(NextOperation nextOperation) {
        this.nextOperation = nextOperation;
    }

    void setIgnoreNextOperation(boolean ignoreNextOperation) {
        this.ignoreNextOperation = ignoreNextOperation;
    }

    void setMutableWrite(boolean mutableWrite) {
        this.mutableWrite = mutableWrite;
    }

    boolean inLocalScope() {
        return !localVariablesByScope.isEmpty();
    }

    private Variable getMemberVariable(String variableName) {
        Map<String, Variable> currentMemberVariables = memberVariables.peek();
        return currentMemberVariables != null ? memberVariables.getLast().get(variableName) : null;
    }

    private Variable getVariable(String variableName) {
        Deque<Variable> variableIdStack = localVariables.get(variableName);
        if (variableIdStack != null)
            return variableIdStack.getLast();
        Variable variable = getMemberVariable(variableName);
        return variable != null ? variable : unscopedVariables.get(variableName);
        /* todo track global variables -> hard, how to differentiate SomeClass.staticAttr++ from String.join(...)
        // problem here: all String.joins (for example) are registered as writes to String
        // get global variable, register if it doesn't exist
        variable = globalVariables.get(variableName);
        if (variable != null)
            return variable;
        variable = new Variable(variableName, false, true);
        globalVariables.put(variableName, variable);
        return variable;
         */
    }

    void registerVariable(String variableName, Scope scope, boolean mutable) {
        Variable variable = new Variable(variableName, scope, mutable);
        switch (scope) {
            case FILE -> unscopedVariables.put(variableName, variable);
            case CLASS -> memberVariables.getLast().put(variableName, variable);
            case LOCAL -> {
                localVariables.putIfAbsent(variableName, new LinkedList<>());
                localVariables.get(variableName).addLast(variable);
                localVariablesByScope.getLast().add(variableName);
            }
        }
    }

    void enterClass() {
        memberVariables.addLast(new HashMap<>());
    }

    void exitClass() {
        memberVariables.removeLast();
    }

    void registerVariableOperation(String variableName, boolean isOwnMember, Swappable swappable) {
        if (ignoreNextOperation) {
            ignoreNextOperation = false;
            return;
        }
        Variable variable = isOwnMember ? getMemberVariable(variableName) : getVariable(variableName);
        if (variable != null) {
            if (nextOperation.isRead)
                swappable.addRead(variable);
            if (nextOperation.isWrite || (mutableWrite && variable.isMutable()))
                swappable.addWrite(variable);
        } else if (nextOperation.isWrite || mutableWrite) {
            swappable.markUnswappable();
        }
        nextOperation = NextOperation.READ;
    }

    void enterLocalScope() {
        localVariablesByScope.addLast(new HashSet<>());
    }

    void exitLocalScope() {
        for (String variableName : localVariablesByScope.removeLast()) {
            Deque<Variable> variableStack = localVariables.get(variableName);
            variableStack.removeLast();
            if (variableStack.isEmpty())
                localVariables.remove(variableName);
        }
    }
}
