package edu.compiler;

import edu.compiler.ast.python.PythonAst.Module;
import edu.compiler.ast.template.TemplateAst.Document;
import edu.compiler.diagnostics.Diagnostic;
import edu.compiler.symbols.SymbolTable;
import java.util.List;
import java.util.Map;

public record ProjectResult(Module python, Map<String,Document> templates, SymbolTable symbols, List<Diagnostic> diagnostics) {
    public boolean successful(){return diagnostics.stream().noneMatch(x->x.severity()==Diagnostic.Severity.ERROR);}
}

