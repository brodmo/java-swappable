// from https://github.com/mbrdl/JPlag/blob/java-semantic-tokens/language-api/src/main/java/de/jplag/semantics/VariableHelper.java
public record Variable(String name, VariableId id) {

    public Variable(String name) {
        this(name, new VariableId());
    }

    @Override
    public String toString() {
        return name + "[" + id + "]";
    }
}
