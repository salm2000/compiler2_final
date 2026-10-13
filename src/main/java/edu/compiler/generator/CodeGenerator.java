package edu.compiler.generator;

import edu.compiler.ast.python.PythonAst.FunctionDef;
import edu.compiler.ast.python.PythonAst.Module;
import edu.compiler.ast.template.TemplateAst.Document;
import java.util.List;
import java.util.Map;

public final class CodeGenerator {
    public String generate(Module module, Map<String,Document> templates) {
        var runtime=new WebRuntimeEmitter();
        List<FunctionDef> functions=module.statements.stream().filter(FunctionDef.class::isInstance).map(FunctionDef.class::cast).toList();
        return runtime.prelude()+new PythonEmitter().emit(module)+new TemplateEmitter().emit(templates)+runtime.finish(functions);
    }
}
