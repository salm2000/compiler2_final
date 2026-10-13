package edu.compiler.frontend;

import edu.compiler.diagnostics.Diagnostic;
import java.util.List;

public record ParseResult<T>(T tree, List<Diagnostic> diagnostics) {
    public boolean successful() { return diagnostics.stream().noneMatch(d -> d.severity() == Diagnostic.Severity.ERROR); }
}

