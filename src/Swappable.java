import java.util.HashSet;
import java.util.Set;


class Swappable {
    private final int lineNumber;
    private boolean unswappable;
    private Set<Variable> reads;
    private Set<Variable> writes;

    Swappable(int lineNumber) {
        this.lineNumber = lineNumber;
        this.unswappable = false;
        this.reads = new HashSet<>();
        this.writes = new HashSet<>();
    }

    int getLineNumber() {
        return lineNumber;
    }

    void markUnswappable() {
        this.unswappable = true;
    }

    void addRead(Variable var) {
        reads.add(var);
    }

    void addWrite(Variable var) {
        writes.add(var);
    }

    @Override
    public String toString() {
        String reads = String.join(", ", this.reads.stream().map(Variable::getName).toList());
        String writes = String.join(", ", this.writes.stream().map(Variable::getName).toList());
        return "%d:%n  unswappable: %b%n  reads: [%s]%n  writes: [%s]%n".formatted(lineNumber, unswappable, reads, writes);
    }
}
