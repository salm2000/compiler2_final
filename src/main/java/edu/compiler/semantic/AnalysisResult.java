package edu.compiler.semantic;

import edu.compiler.diagnostics.Diagnostic;
import edu.compiler.symbols.SymbolTable;
import java.util.List;

public record AnalysisResult(SymbolTable symbols, List<Diagnostic> diagnostics) {
    public boolean successful() { return diagnostics.stream().noneMatch(d -> d.severity()==Diagnostic.Severity.ERROR); }
}

