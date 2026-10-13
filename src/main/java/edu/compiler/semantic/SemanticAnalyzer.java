package edu.compiler.semantic;

import edu.compiler.ast.AstNode;
import edu.compiler.ast.SourceLocation;
import edu.compiler.ast.python.PythonAst.*;
import edu.compiler.ast.template.TemplateAst;
import edu.compiler.diagnostics.Diagnostic;
import edu.compiler.symbols.Symbol;
import edu.compiler.symbols.SymbolTable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SemanticAnalyzer {
    private static final Set<String> INTRINSICS=Set.of("Application","len","range","str","int","float","print","render_template","redirect");
    private static final Map<String,int[]> ARITIES=Map.of(
        "Application",new int[]{0,0},"len",new int[]{1,1},"range",new int[]{1,3},"str",new int[]{0,1},
        "int",new int[]{1,1},"float",new int[]{1,1},"print",new int[]{0,Integer.MAX_VALUE},"redirect",new int[]{1,1});
    private static final Set<String> METHODS=Set.of("append","pop","remove","get","keys","values");
    private static final Pattern ROUTE_PARAM=Pattern.compile("<int:([A-Za-z_][A-Za-z0-9_]*)>");
    private static final Pattern ATTRIBUTE_BINDING=Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)");
    private final SymbolTable symbols=new SymbolTable();
    private final List<Diagnostic> diagnostics=new ArrayList<>();
    private final Map<String,FunctionDef> functions=new LinkedHashMap<>();
    private final Set<String> globals=new LinkedHashSet<>();
    private Map<String,TemplateAst.Document> templates=Map.of();

    public AnalysisResult analyze(edu.compiler.ast.python.PythonAst.Module module, Map<String,TemplateAst.Document> templates) {
        this.templates=Map.copyOf(templates);
        collectModule(module);
        validateRoutes();
        Scope moduleScope=new Scope("module",null); moduleScope.names.addAll(globals); moduleScope.names.addAll(functions.keySet());
        for(Statement statement:module.statements) checkStatement(statement,moduleScope,0,false,null);
        return new AnalysisResult(symbols,List.copyOf(diagnostics));
    }

    private void collectModule(edu.compiler.ast.python.PythonAst.Module module) {
        for(Statement statement:module.statements) {
            if(statement instanceof FunctionDef function) {
                if(functions.putIfAbsent(function.name,function)!=null) error(function,"SEM009","duplicate function declaration '"+function.name+"'");
                symbols.add(new Symbol(function.name,Symbol.Kind.FUNCTION,"module",function.location(),"Function("+function.parameters.size()+")"));
            } else if(statement instanceof Assign assign && assign.target instanceof Name name) {
                globals.add(name.name); symbols.add(new Symbol(name.name,Symbol.Kind.VARIABLE,"module",name.location(),typeOf(assign.value)));
            }
        }
    }
    private void validateRoutes() {
        Set<String> registrations=new HashSet<>();
        for(FunctionDef function:functions.values()) if(function.route!=null) {
            for(String method:function.route.methods) if(!registrations.add(method+" "+function.route.path)) error(function.route,"SEM003","duplicate route "+method+" "+function.route.path);
            List<String> expected=new ArrayList<>(); Matcher matcher=ROUTE_PARAM.matcher(function.route.path); while(matcher.find())expected.add(matcher.group(1));
            if(!expected.equals(function.parameters)) error(function,"SEM011","route parameters "+expected+" do not match handler parameters "+function.parameters);
        }
    }
    private void checkStatement(Statement statement, Scope scope, int loopDepth, boolean inFunction, FunctionDef owner) {
        if(statement instanceof FunctionDef function) {
            Scope local=new Scope("function:"+function.name,scope);
            Set<String> unique=new HashSet<>();
            for(String parameter:function.parameters) {
                if(!unique.add(parameter)) error(function,"SEM009","duplicate parameter '"+parameter+"'");
                local.names.add(parameter); symbols.add(new Symbol(parameter,Symbol.Kind.PARAMETER,local.label,function.location(),"Unknown"));
            }
            for(Statement child:function.body.statements) checkStatement(child,local,0,true,function);
        } else if(statement instanceof Assign assign) {
            checkExpression(assign.value,scope,owner);
            if(assign.target instanceof Name name) {
                scope.names.add(name.name);
                if(!scope.label.equals("module")) symbols.add(new Symbol(name.name,Symbol.Kind.VARIABLE,scope.label,name.location(),typeOf(assign.value)));
            } else checkExpression(assign.target,scope,owner);
        } else if(statement instanceof ExprStatement expr) checkExpression(expr.expression,scope,owner);
        else if(statement instanceof Return value) {
            if(!inFunction) error(value,"SEM008","return is only valid inside a function");
            if(value.expression!=null) checkExpression(value.expression,scope,owner);
        } else if(statement instanceof If conditional) {
            for(Branch branch:conditional.branches){checkExpression(branch.condition,scope,owner);for(Statement child:branch.body.statements)checkStatement(child,scope,loopDepth,inFunction,owner);}
            if(conditional.elseBody!=null)for(Statement child:conditional.elseBody.statements)checkStatement(child,scope,loopDepth,inFunction,owner);
        } else if(statement instanceof For loop) {
            checkExpression(loop.iterable,scope,owner); scope.names.add(loop.variable);
            symbols.add(new Symbol(loop.variable,Symbol.Kind.LOOP_VARIABLE,scope.label,loop.location(),"Unknown"));
            for(Statement child:loop.body.statements)checkStatement(child,scope,loopDepth+1,inFunction,owner);
        } else if(statement instanceof While loop) {
            checkExpression(loop.condition,scope,owner);for(Statement child:loop.body.statements)checkStatement(child,scope,loopDepth+1,inFunction,owner);
        } else if(statement instanceof Flow flow && (flow.keyword.equals("break")||flow.keyword.equals("continue")) && loopDepth==0)
            error(flow,"SEM008",flow.keyword+" is only valid inside a loop");
    }
    private void checkExpression(Expression expression, Scope scope, FunctionDef owner) {
        if(expression instanceof Name name) {
            if(!scope.resolves(name.name)&&!INTRINSICS.contains(name.name)&&!name.name.equals("request")) error(name,"SEM001","undefined name '"+name.name+"'");
        } else if(expression instanceof Unary unary) checkExpression(unary.operand,scope,owner);
        else if(expression instanceof Binary binary) {
            checkExpression(binary.left,scope,owner);checkExpression(binary.right,scope,owner);
            if(invalidOperands(binary)) error(binary,"SEM012","operator '"+binary.operator+"' does not support "+typeOf(binary.left)+" and "+typeOf(binary.right));
        } else if(expression instanceof ListLiteral list) list.values.forEach(x->checkExpression(x,scope,owner));
        else if(expression instanceof DictLiteral dict) dict.entries.forEach(x->{checkExpression(x.key,scope,owner);checkExpression(x.value,scope,owner);});
        else if(expression instanceof Attribute attribute) checkExpression(attribute.receiver,scope,owner);
        else if(expression instanceof Subscript subscript) {checkExpression(subscript.receiver,scope,owner);checkExpression(subscript.index,scope,owner);}
        else if(expression instanceof Call call) checkCall(call,scope,owner);
    }
    private void checkCall(Call call, Scope scope, FunctionDef owner) {
        if(call.callee instanceof Name name) {
            FunctionDef declared=functions.get(name.name);
            long positional=call.arguments.stream().filter(a->a.name==null).count();
            if(declared!=null&&positional!=declared.parameters.size())error(call,"SEM002","function '"+name.name+"' expects "+declared.parameters.size()+" arguments but received "+positional);
            else if(!INTRINSICS.contains(name.name)&&declared==null)error(call,"SEM007","unsupported function '"+name.name+"'");
            if(ARITIES.containsKey(name.name)){int[] range=ARITIES.get(name.name);if(positional<range[0]||positional>range[1])error(call,"SEM002","intrinsic '"+name.name+"' has invalid argument count");}
            if(name.name.equals("render_template")) checkRender(call);
        } else if(call.callee instanceof Attribute attribute) {
            if(!METHODS.contains(attribute.name))error(call,"SEM007","unsupported method '"+attribute.name+"'");
            checkExpression(attribute.receiver,scope,owner);
        } else checkExpression(call.callee,scope,owner);
        call.arguments.forEach(argument->checkExpression(argument.value,scope,owner));
    }
    private void checkRender(Call call) {
        if(call.arguments.isEmpty()||!(call.arguments.get(0).value instanceof Literal literal)||!(literal.raw.startsWith("\"")||literal.raw.startsWith("'"))){error(call,"SEM004","render_template requires a static template name");return;}
        String name=strip(literal.raw); TemplateAst.Document template=templates.get(name);
        if(template==null){error(call,"SEM004","template '"+name+"' was not found");return;}
        Set<String> bindings=new HashSet<>();
        for(int i=1;i<call.arguments.size();i++){Argument argument=call.arguments.get(i);if(argument.name!=null)bindings.add(argument.name);}
        validateTemplate(template,bindings);
    }
    private void validateTemplate(TemplateAst.Document template, Set<String> bindings) {
        Scope scope=new Scope("template:"+template.name,null);scope.names.addAll(bindings);
        bindings.forEach(name->symbols.add(new Symbol(name,Symbol.Kind.TEMPLATE_BINDING,scope.label,template.location(),"Unknown")));
        checkTemplateNodes(template.nodes,scope);
    }
    private void checkTemplateNodes(List<TemplateAst.Node> nodes, Scope scope) {
        Set<String> tags=Set.of("html","head","title","body","div","span","p","h1","h2","h3","ul","li","a","img","form","label","input","button","link");
        Set<String> attrs=Set.of("id","class","href","src","alt","type","name","value","method","action","rel");
        Set<String> voids=Set.of("img","input","link");
        for(TemplateAst.Node node:nodes) {
            if(node instanceof TemplateAst.Interpolation interpolation)checkTemplateExpression(interpolation.expression,scope);
            else if(node instanceof TemplateAst.ForBlock loop){
                checkTemplateExpression(loop.iterable,scope);
                if(loop.iterable instanceof TemplateAst.Literal literal&&literal.raw.matches("[0-9]+(\\.[0-9]+)?"))error(loop,"SEM006","template loop target is not iterable");
                Scope child=new Scope(scope.label+"/for",scope);child.names.add(loop.variable);symbols.add(new Symbol(loop.variable,Symbol.Kind.LOOP_VARIABLE,child.label,loop.location(),"Unknown"));checkTemplateNodes(loop.body,child);
            } else if(node instanceof TemplateAst.IfBlock conditional){for(TemplateAst.Branch branch:conditional.branches){checkTemplateExpression(branch.condition,scope);checkTemplateNodes(branch.body,scope);}checkTemplateNodes(conditional.elseBody,scope);}
            else if(node instanceof TemplateAst.Element element){
                if(!tags.contains(element.tag))error(element,"SEM013","unsupported HTML tag '"+element.tag+"'");
                if(!element.selfClosing&&!Objects.equals(element.tag,element.closingTag))error(element,"SEM013","HTML closing tag '</"+element.closingTag+">' does not match '<"+element.tag+">'");
                if(voids.contains(element.tag)&&!element.nodes.isEmpty())error(element,"SEM013","void HTML element '"+element.tag+"' cannot have children");
                for(TemplateAst.Attribute attribute:element.attributes){
                    if(!attrs.contains(attribute.name))error(attribute,"SEM013","unsupported HTML attribute '"+attribute.name+"'");
                    Matcher binding=ATTRIBUTE_BINDING.matcher(attribute.rawValue);
                    while(binding.find())if(!scope.resolves(binding.group(1)))error(attribute,"SEM005","template name '"+binding.group(1)+"' in attribute is not supplied by render context");
                }
                checkTemplateNodes(element.nodes,scope);
            }
        }
    }
    private void checkTemplateExpression(TemplateAst.Expression expression, Scope scope) {
        if(expression instanceof TemplateAst.Name name&&!scope.resolves(name.name))error(name,"SEM005","template name '"+name.name+"' is not supplied by render context");
        else for(AstNode child:expression.children())if(child instanceof TemplateAst.Expression nested)checkTemplateExpression(nested,scope);
    }
    private boolean invalidOperands(Binary binary) {
        String left=typeOf(binary.left),right=typeOf(binary.right);if(left.equals("Unknown")||right.equals("Unknown"))return false;
        if(Set.of("==","!=","<","<=",">",">=","and","or","in").contains(binary.operator))return false;
        if(binary.operator.equals("+")&&left.equals(right)&&(left.equals("String")||left.startsWith("List")))return false;
        return !(Set.of("Int","Float").contains(left)&&Set.of("Int","Float").contains(right));
    }
    private String typeOf(Expression expression) {
        if(expression instanceof Literal literal){if(literal.raw.matches("[0-9]+"))return "Int";if(literal.raw.matches("[0-9]+\\.[0-9]+"))return "Float";if(literal.raw.startsWith("\"")||literal.raw.startsWith("'"))return "String";if(literal.raw.equals("True")||literal.raw.equals("False"))return "Bool";if(literal.raw.equals("None"))return "None";}
        if(expression instanceof ListLiteral)return "List";if(expression instanceof DictLiteral)return "Dict";return "Unknown";
    }
    private void error(AstNode node,String code,String message){SourceLocation p=node.location();diagnostics.add(new Diagnostic(p.file(),p.line(),p.column(),code,Diagnostic.Severity.ERROR,message));}
    private String strip(String text){return text.substring(1,text.length()-1);}
    private static final class Scope {final String label;final Scope parent;final Set<String> names=new HashSet<>();Scope(String label,Scope parent){this.label=label;this.parent=parent;}boolean resolves(String name){return names.contains(name)||(parent!=null&&parent.resolves(name));}}
}

