// from https://github.com/mbrdl/JPlag/blob/java-semantic-tokens/language-api/src/main/java/de/jplag/semantics/VariableId.java
public record VariableId(String id) {
    private static long counter;

    public VariableId() {
        this(Long.toString(counter++));
    }

    @Override
    public String toString() {
        return id;
    }
}
