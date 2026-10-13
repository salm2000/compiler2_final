package edu.compiler.report;

import static org.junit.jupiter.api.Assertions.*;
import edu.compiler.CompilerService;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlReportWriterTest {
    @TempDir Path temp;
    @Test void writesEscapedStandaloneAstAndSymbolReports() throws Exception {
        Path example=Path.of("examples","tasks");
        var result=new CompilerService().analyze(example.resolve("app.py"),example.resolve("templates"),example.resolve("static"));
        assertTrue(result.successful(),()->result.diagnostics().toString());
        Path ast=temp.resolve("reports/ast.html");Path symbols=temp.resolve("reports/symbols.html");
        HtmlReportWriter writer=new HtmlReportWriter();writer.writeAst(result,ast);writer.writeSymbols(result,symbols);
        String astHtml=Files.readString(ast,StandardCharsets.UTF_8);String symbolHtml=Files.readString(symbols,StandardCharsets.UTF_8);
        assertTrue(astHtml.contains("<!doctype html>"));assertTrue(astHtml.contains("PythonModule"));assertTrue(astHtml.contains("TemplateDocument"));
        assertTrue(astHtml.contains("id=\"astGraph\""));assertTrue(astHtml.contains("const astData ="));assertTrue(astHtml.contains("class: 'edge'"));
        assertTrue(astHtml.contains("Expand all"));assertTrue(astHtml.contains("Searchable text tree"));
        assertFalse(astHtml.contains("<int:id>"));
        assertTrue(symbolHtml.contains("<table>"));assertTrue(symbolHtml.contains("dashboard"));assertTrue(symbolHtml.contains("TEMPLATE_BINDING"));
    }
}
