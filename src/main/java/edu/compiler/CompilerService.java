package edu.compiler;

import edu.compiler.ast.python.PythonAst.Module;
import edu.compiler.ast.template.TemplateAst.Document;
import edu.compiler.builder.PythonAstBuilder;
import edu.compiler.builder.TemplateAstBuilder;
import edu.compiler.diagnostics.Diagnostic;
import edu.compiler.frontend.Frontend;
import edu.compiler.generator.CodeGenerator;
import edu.compiler.semantic.CssValidator;
import edu.compiler.semantic.SemanticAnalyzer;
import edu.compiler.symbols.SymbolTable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class CompilerService {
    public ProjectResult analyze(Path source,Path templatesDirectory,Path cssDirectory)throws IOException{
        List<Diagnostic> diagnostics=new ArrayList<>();
        String sourceText=Files.readString(source,StandardCharsets.UTF_8);
        var pythonParse=Frontend.parsePython(source.toString(),sourceText);diagnostics.addAll(pythonParse.diagnostics());
        Module module=pythonParse.successful()?(Module)new PythonAstBuilder(source.toString()).visit(pythonParse.tree()):null;
        Map<String,Document> templates=new TreeMap<>();
        if(templatesDirectory!=null&&Files.isDirectory(templatesDirectory))for(Path file:files(templatesDirectory,".html")){
            String name=templatesDirectory.relativize(file).toString().replace('\\','/');
            var parsed=Frontend.parseTemplate(file.toString(),Files.readString(file,StandardCharsets.UTF_8));diagnostics.addAll(parsed.diagnostics());
            if(parsed.successful())templates.put(name,(Document)new TemplateAstBuilder(name).visit(parsed.tree()));
        }
        if(cssDirectory!=null&&Files.isDirectory(cssDirectory))for(Path file:files(cssDirectory,".css")){
            var parsed=Frontend.parseCss(file.toString(),Files.readString(file,StandardCharsets.UTF_8));diagnostics.addAll(parsed.diagnostics());
            if(parsed.successful())diagnostics.addAll(new CssValidator().validate(file.toString(),parsed.tree()));
        }
        SymbolTable symbols=new SymbolTable();
        if(module!=null&&diagnostics.stream().noneMatch(x->x.severity()==Diagnostic.Severity.ERROR)){
            var semantic=new SemanticAnalyzer().analyze(module,templates);symbols=semantic.symbols();diagnostics.addAll(semantic.diagnostics());
        }
        return new ProjectResult(module,Map.copyOf(templates),symbols,List.copyOf(diagnostics));
    }
    public ProjectResult compile(Path source,Path templatesDirectory,Path cssDirectory,Path output)throws IOException{
        ProjectResult result=analyze(source,templatesDirectory,cssDirectory);if(!result.successful())return result;
        Files.createDirectories(output);
        Files.writeString(output.resolve("app.py"),new CodeGenerator().generate(result.python(),result.templates()),StandardCharsets.UTF_8);
        if(cssDirectory!=null&&Files.isDirectory(cssDirectory))for(Path file:files(cssDirectory,".css")){
            Path relative=cssDirectory.relativize(file);Path target=output.resolve("static").resolve(relative);Files.createDirectories(target.getParent());Files.copy(file,target,StandardCopyOption.REPLACE_EXISTING);
        }
        return result;
    }
    private List<Path> files(Path root,String extension)throws IOException{try(Stream<Path> stream=Files.walk(root)){return stream.filter(Files::isRegularFile).filter(x->x.getFileName().toString().endsWith(extension)).sorted().toList();}}
}

