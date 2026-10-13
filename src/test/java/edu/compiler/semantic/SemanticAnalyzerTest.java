package edu.compiler.semantic;

import static org.junit.jupiter.api.Assertions.*;
import edu.compiler.ast.python.PythonAst.Module;
import edu.compiler.ast.template.TemplateAst.Document;
import edu.compiler.builder.PythonAstBuilder;
import edu.compiler.builder.TemplateAstBuilder;
import edu.compiler.frontend.Frontend;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SemanticAnalyzerTest {
    private Module python(String source){var p=Frontend.parsePython("app.py",source);assertTrue(p.successful(),()->p.diagnostics().toString());return (Module)new PythonAstBuilder("app.py").visit(p.tree());}
    private Document template(String name,String source){var p=Frontend.parseTemplate(name,source);assertTrue(p.successful(),()->p.diagnostics().toString());return (Document)new TemplateAstBuilder(name).visit(p.tree());}
    @Test void reportsRequiredCrossLanguageDiagnosticFamilies() {
        String source="""
            app = Application()
            value = missing
            def one(x):
                return x
            one()
            mystery(1)
            @app.route("/x/<int:id>", methods=["GET"])
            def first():
                return render_template("page.html", supplied=value)
            @app.route("/x/<int:id>", methods=["GET"])
            def second(id):
                return render_template("missing.html")
            """;
        Document page=template("page.html","<p>{{ absent }}</p>{% for x in 3 %}{{ x }}{% endfor %}");
        var result=new SemanticAnalyzer().analyze(python(source),Map.of("page.html",page));
        Set<String> codes=result.diagnostics().stream().map(d->d.code()).collect(Collectors.toSet());
        assertTrue(codes.containsAll(Set.of("SEM001","SEM002","SEM003","SEM004","SEM005","SEM006","SEM007","SEM011")),codes::toString);
    }
    @Test void acceptsBoundTemplateAndPrintsSymbols() {
        String source="""
            app = Application()
            things = [{"name": "book"}]
            @app.route("/")
            def home():
                return render_template("home.html", items=things)
            """;
        var result=new SemanticAnalyzer().analyze(python(source),Map.of("home.html",template("home.html","<ul>{% for item in items %}<li>{{ item.name }}</li>{% endfor %}</ul>")));
        assertTrue(result.successful(),()->result.diagnostics().toString());
        assertTrue(result.symbols().dump().contains("TEMPLATE_BINDING"));
    }
    @Test void rejectsUnsupportedCssProperty() {
        var parsed=Frontend.parseCss("style.css","p { position: fixed; }");
        assertEquals("SEM014",new CssValidator().validate("style.css",parsed.tree()).get(0).code());
    }
}
