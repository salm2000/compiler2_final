package edu.compiler.report;

import edu.compiler.ProjectResult;
import edu.compiler.ast.AstPrinter;
import edu.compiler.symbols.Symbol;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

public final class HtmlReportWriter {
    public void writeAst(ProjectResult result, Path destination) throws IOException {
        AstPrinter printer = new AstPrinter();
        StringBuilder tree = new StringBuilder(result.python().accept(printer));
        result.templates().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(entry -> tree.append('\n').append(entry.getValue().accept(printer)));
        write(destination, page("AST report", "Python and template abstract syntax trees",
            "<pre class=\"tree\">" + escape(tree.toString()) + "</pre>"));
    }

    public void writeSymbols(ProjectResult result, Path destination) throws IOException {
        StringBuilder rows = new StringBuilder();
        result.symbols().symbols().stream()
            .sorted(Comparator.comparing(Symbol::scope).thenComparing(Symbol::name))
            .forEach(symbol -> rows.append("<tr><td>").append(escape(symbol.name()))
                .append("</td><td><span class=\"kind\">").append(escape(symbol.kind().toString()))
                .append("</span></td><td>").append(escape(symbol.scope()))
                .append("</td><td>").append(escape(symbol.location().toString()))
                .append("</td><td>").append(escape(symbol.inferredType())).append("</td></tr>"));
        String table = "<div class=\"table-wrap\"><table><thead><tr><th>Name</th><th>Kind</th><th>Scope</th><th>Location</th><th>Type</th></tr></thead><tbody>"
            + rows + "</tbody></table></div>";
        write(destination, page("Symbol table", "Collected Python and template symbols", table));
    }

    private void write(Path destination, String html) throws IOException {
        Path absolute = destination.toAbsolutePath();
        if (absolute.getParent() != null) Files.createDirectories(absolute.getParent());
        Files.writeString(absolute, html, StandardCharsets.UTF_8);
    }

    private String page(String title, String subtitle, String content) {
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>%s</title>
              <style>
                :root { color-scheme: light dark; font-family: Inter, Segoe UI, sans-serif; }
                body { margin: 0; background: #0f172a; color: #e2e8f0; }
                header { padding: 2rem clamp(1rem, 5vw, 4rem); background: linear-gradient(135deg, #1d4ed8, #0f766e); }
                h1 { margin: 0 0 .4rem; font-size: 2rem; }
                header p { margin: 0; color: #dbeafe; }
                main { padding: 2rem clamp(1rem, 5vw, 4rem); }
                .tree { padding: 1.5rem; overflow: auto; line-height: 1.55; border: 1px solid #334155; border-radius: 12px; background: #111827; color: #d1fae5; }
                .table-wrap { overflow-x: auto; border: 1px solid #334155; border-radius: 12px; }
                table { width: 100%%; border-collapse: collapse; background: #111827; }
                th, td { padding: .8rem 1rem; text-align: left; border-bottom: 1px solid #334155; white-space: nowrap; }
                th { color: #93c5fd; background: #1e293b; }
                tr:hover td { background: #172033; }
                .kind { padding: .2rem .5rem; border-radius: 999px; background: #164e63; color: #cffafe; font-size: .8rem; }
              </style>
            </head>
            <body><header><h1>%s</h1><p>%s</p></header><main>%s</main></body>
            </html>
            """.formatted(escape(title), escape(title), escape(subtitle), content);
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

