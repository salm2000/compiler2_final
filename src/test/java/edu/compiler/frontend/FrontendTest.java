package edu.compiler.frontend;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FrontendTest {
    @Test void parsesIndentedPythonAndRoute() {
        String source = """
            app = Application()
            items = [{"name": "Book"}]
            @app.route("/", methods=["GET"])
            def home():
                for item in items:
                    if item["name"] == "Book":
                        return render_template("home.html", things=items)
                return "empty"
            """;
        var result = Frontend.parsePython("app.py", source);
        assertTrue(result.successful(), () -> result.diagnostics().toString());
    }

    @Test void parsesNestedTemplate() {
        String source = "<ul>{% for x in things %}<li>{% if x.price > 0 %}{{ x.name }}{% else %}free{% endif %}</li>{% endfor %}</ul>";
        var result = Frontend.parseTemplate("home.html", source);
        assertTrue(result.successful(), () -> result.diagnostics().toString());
    }

    @Test void parsesCssSubset() {
        var result = Frontend.parseCss("style.css", ".card, #main { padding: 10px; color: #fff; }");
        assertTrue(result.successful(), () -> result.diagnostics().toString());
    }
}
