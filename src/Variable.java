class Variable {
    private final String name;
    private final Scope scope;
    private final boolean isMutable;

    Variable(String name, Scope scope, boolean isMutable) {
        this.name = name;
        this.scope = scope;
        this.isMutable = isMutable;
    }

    boolean isMutable() {
        return isMutable;
    }

    String getName() {
        return (scope == Scope.CLASS ? "this." : "") + name;
    }
}
