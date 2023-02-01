import java.util.HashSet;
import java.util.Set;


public class Swappable {
    private final int lineNumber;
    private boolean swappable;
    private Set<String> conflicts;

    Swappable(int lineNumber) {
        this.lineNumber = lineNumber;
        this.swappable = true;
        this.conflicts = new HashSet<>();
    }

    int getLineNumber() {
        return lineNumber;
    }

    void notSwappable() {
        this.swappable = false;
    }

    void addConflict(String conflict) {
        conflicts.add(conflict);
    }

    public String toString() {
        return "%d:%n  swappable: %b%n  conflicts: [%s]%n".formatted(lineNumber, swappable, String.join(", ", conflicts));
    }
}
