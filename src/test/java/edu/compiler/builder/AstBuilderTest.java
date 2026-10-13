package edu.compiler.builder;

import static org.junit.jupiter.api.Assertions.*;
import edu.compiler.ast.AstPrinter;
import edu.compiler.ast.python.PythonAst.Module;
import edu.compiler.ast.template.TemplateAst.Document;
import edu.compiler.frontend.Frontend;
import org.junit.jupiter.api.Test;

class AstBuilderTest {
    @Test void buildsAndPrintsIndependentTreesWithLocations() {
        var pythonParse=Frontend.parsePython("app.py","x = [1, 2]\ndef total(a):\n    return a + x[0]\n");
        Module python=(Module)new PythonAstBuilder("app.py").visit(pythonParse.tree());
        String pythonTree=python.accept(new AstPrinter());
        assertTrue(pythonTree.contains("FunctionDef(total"));
        assertTrue(pythonTree.contains("SubscriptAccess @ app.py:3"));

        var templateParse=Frontend.parseTemplate("page.html","<h1>{{ title }}</h1>");
        Document template=(Document)new TemplateAstBuilder("page.html").visit(templateParse.tree());
        String templateTree=template.accept(new AstPrinter());
        assertTrue(templateTree.contains("HtmlElement(h1)"));
        assertTrue(templateTree.contains("TemplateName(title)"));
    }
}
