import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


// adapted from https://github.com/mbrdl/JPlag/blob/java-semantic-tokens/language-api/src/main/java/de/jplag/semantics/VariableHelper.java
public class VariableHelper {
    private Map<String, Stack<Variable>> localVariables; // map local variable name to variable
    private Stack<Set<String>> localVariablesByScope; // stack of local variable names in scope

    public VariableHelper() {
        this.localVariables = new HashMap<>();
        this.localVariablesByScope = new Stack<>();
    }

    public boolean inLocalScope() {
        return !localVariablesByScope.isEmpty();
    }

    public Variable getVariable(String variableName) {
        Stack<Variable> variableIdStack = localVariables.getOrDefault(variableName, null);
        return variableIdStack != null ? variableIdStack.peek() : null;
    }

    public void registerLocalVariable(String name) {
        Variable variable = new Variable(name);
        localVariables.putIfAbsent(variable.name(), new Stack<>());
        localVariables.get(variable.name()).push(variable);
        localVariablesByScope.peek().add(variable.name());
    }

    public void enterLocalScope() {
        localVariablesByScope.add(new HashSet<>());
    }

    public void exitLocalScope() {
        for (String variableName : localVariablesByScope.pop()) {
            Stack<Variable> variableStack = localVariables.get(variableName);
            variableStack.pop();
            if (variableStack.isEmpty()) {
                localVariables.remove(variableName);
            }
        }
    }
}
