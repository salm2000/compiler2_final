package edu.compiler.generator;

import static org.junit.jupiter.api.Assertions.*;
import edu.compiler.ast.python.PythonAst.Module;
import edu.compiler.ast.template.TemplateAst.Document;
import edu.compiler.builder.PythonAstBuilder;
import edu.compiler.builder.TemplateAstBuilder;
import edu.compiler.frontend.Frontend;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodeGeneratorTest {
    @TempDir Path temp;
    @Test void generatedProgramUsesStdlibAndEscapesTemplates()throws Exception{
        String source="app = Application()\nvalues = [{\"name\": \"ok\"}]\n@app.route(\"/\")\ndef home():\n    return render_template(\"home.html\", title=values[0][\"name\"])\n";
        var pp=Frontend.parsePython("app.py",source);assertTrue(pp.successful(),()->pp.diagnostics().toString());
        Module module=(Module)new PythonAstBuilder("app.py").visit(pp.tree());
        var tp=Frontend.parseTemplate("home.html","<h1 class=\"x\">{{ title }}</h1>");assertTrue(tp.successful(),()->tp.diagnostics().toString());
        Document template=(Document)new TemplateAstBuilder("home.html").visit(tp.tree());
        String generated=new CodeGenerator().generate(module,Map.of("home.html",template));
        assertFalse(generated.contains("flask"));assertFalse(generated.contains("jinja"));
        Path file=temp.resolve("app.py");Files.writeString(file,generated,StandardCharsets.UTF_8);
        Process syntax=new ProcessBuilder("python","-m","py_compile",file.toString()).redirectErrorStream(true).start();
        assertTrue(syntax.waitFor(20,TimeUnit.SECONDS));assertEquals(0,syntax.exitValue(),()->read(syntax));
        String command="import runpy; d=runpy.run_path(r'"+file.toString().replace("\\","\\\\")+"'); print(d['render_template']('home.html', title='<x&'+chr(34)))";
        Process render=new ProcessBuilder("python","-c",command).redirectErrorStream(true).start();assertTrue(render.waitFor(20,TimeUnit.SECONDS));
        String output=read(render);assertEquals(0,render.exitValue(),output);assertTrue(output.contains("&lt;x&amp;&quot;"),output);
    }
    private String read(Process process){try{return new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);}catch(Exception e){return e.toString();}}
}
