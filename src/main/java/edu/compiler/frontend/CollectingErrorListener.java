package edu.compiler.frontend;

import edu.compiler.diagnostics.Diagnostic;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

final class CollectingErrorListener extends BaseErrorListener {
    private final String file;
    private final String code;
    private final List<Diagnostic> diagnostics;
    CollectingErrorListener(String file, String code, List<Diagnostic> diagnostics) {
        this.file = file; this.code = code; this.diagnostics = diagnostics;
    }
    @Override public void syntaxError(Recognizer<?, ?> recognizer, Object symbol, int line, int column,
                                      String message, RecognitionException error) {
        diagnostics.add(new Diagnostic(file, line, column + 1, code, Diagnostic.Severity.ERROR, message));
    }
}

