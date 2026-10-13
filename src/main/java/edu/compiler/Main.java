package edu.compiler;

import edu.compiler.ast.AstPrinter;
import java.nio.file.Path;
import java.util.*;

public final class Main {
    private Main(){}
    public static void main(String[] args){int status=run(args);if(status!=0)System.exit(status);}
    static int run(String[] args){
        if(args.length<2||!Set.of("compile","ast","symbols").contains(args[0])){usage();return 2;}
        String command=args[0];Path source=Path.of(args[1]);Map<String,String> options=new HashMap<>();
        for(int i=2;i<args.length;i+=2){if(i+1>=args.length){System.err.println("Missing value for "+args[i]);return 2;}options.put(args[i],args[i+1]);}
        Path templates=Path.of(options.getOrDefault("--templates",source.toAbsolutePath().getParent().resolve("templates").toString()));
        Path css=Path.of(options.getOrDefault("--css",source.toAbsolutePath().getParent().resolve("static").toString()));
        try{
            CompilerService compiler=new CompilerService();
            ProjectResult result=command.equals("compile")?compiler.compile(source,templates,css,Path.of(options.getOrDefault("--out","output"))):compiler.analyze(source,templates,css);
            result.diagnostics().forEach(x->System.err.println(x));if(!result.successful())return 1;
            if(command.equals("ast")){
                AstPrinter printer=new AstPrinter();System.out.print(result.python().accept(printer));
                result.templates().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(x->System.out.print(x.getValue().accept(printer)));
            }else if(command.equals("symbols"))System.out.print(result.symbols().dump());
            else System.out.println("Compiled "+source+" successfully to "+Path.of(options.getOrDefault("--out","output")).toAbsolutePath());
            return 0;
        }catch(Exception error){System.err.println("Compiler failure: "+error.getMessage());return 2;}
    }
    private static void usage(){System.err.println("Usage: java -jar compiler.jar <compile|ast|symbols> SOURCE [--templates DIR] [--css DIR] [--out DIR]");}
}

