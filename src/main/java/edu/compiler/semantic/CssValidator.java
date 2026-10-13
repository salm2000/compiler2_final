package edu.compiler.semantic;

import edu.compiler.antlr.CssParser;
import edu.compiler.diagnostics.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CssValidator {
    private static final Set<String> PROPERTIES=Set.of("color","background-color","font-size","margin","padding","width","height","border","display","text-align");
    public List<Diagnostic> validate(String file, CssParser.StylesheetContext sheet) {
        List<Diagnostic> result=new ArrayList<>();
        for(var rule:sheet.rule_())for(var declaration:rule.declaration()){
            String property=declaration.property().getText();
            if(!PROPERTIES.contains(property))result.add(new Diagnostic(file,declaration.start.getLine(),declaration.start.getCharPositionInLine()+1,"SEM014",Diagnostic.Severity.ERROR,"unsupported CSS property '"+property+"'"));
        }
        return result;
    }
}

