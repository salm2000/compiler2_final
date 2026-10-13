package edu.compiler.frontend;

import edu.compiler.antlr.*;
import edu.compiler.diagnostics.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;

public final class Frontend {
    private Frontend() {}

    public static ParseResult<PythonSubsetParser.FileInputContext> parsePython(String file, String source) {
        var lexer = new PythonSubsetLexer(CharStreams.fromString(source, file));
        var parser = new PythonSubsetParser(new CommonTokenStream(lexer));
        List<Diagnostic> diagnostics = configure(file, lexer, parser);
        return new ParseResult<>(parser.fileInput(), List.copyOf(diagnostics));
    }

    public static ParseResult<TemplateParser.DocumentContext> parseTemplate(String file, String source) {
        var lexer = new TemplateLexer(CharStreams.fromString(source, file));
        var parser = new TemplateParser(new CommonTokenStream(lexer));
        List<Diagnostic> diagnostics = configure(file, lexer, parser);
        return new ParseResult<>(parser.document(), List.copyOf(diagnostics));
    }

    public static ParseResult<CssParser.StylesheetContext> parseCss(String file, String source) {
        var lexer = new CssLexer(CharStreams.fromString(source, file));
        var parser = new CssParser(new CommonTokenStream(lexer));
        List<Diagnostic> diagnostics = configure(file, lexer, parser);
        return new ParseResult<>(parser.stylesheet(), List.copyOf(diagnostics));
    }

    private static List<Diagnostic> configure(String file, Lexer lexer, Parser parser) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        lexer.removeErrorListeners();
        lexer.addErrorListener(new CollectingErrorListener(file, "LEX001", diagnostics));
        parser.removeErrorListeners();
        parser.addErrorListener(new CollectingErrorListener(file, "SYN001", diagnostics));
        return diagnostics;
    }
}

