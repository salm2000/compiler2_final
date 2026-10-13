package edu.compiler.generator;

import edu.compiler.ast.template.TemplateAst.*;
import java.util.*;

public final class TemplateEmitter {
    private final StringBuilder out=new StringBuilder();
    public String emit(Map<String,Document> templates) {
        List<String> names=new ArrayList<>(templates.keySet());Collections.sort(names);
        for(String name:names)emitDocument(templates.get(name),functionName(name));
        out.append("\n__cpl_templates = {\n");for(String name:names)out.append("    ").append(pyString(name)).append(": ").append(functionName(name)).append(",\n");out.append("}\n");
        return out.toString();
    }
    private void emitDocument(Document document,String function){out.append("\ndef ").append(function).append("(__cpl_context):\n    __cpl_out = []\n");nodes(document.nodes,1);out.append("    return ''.join(__cpl_out)\n");}
    private void nodes(List<Node> nodes,int depth){for(Node node:nodes)node(node,depth);}
    private void node(Node node,int depth){
        String pad="    ".repeat(depth);
        if(node instanceof Text text)append(pad,pyString(text.text));
        else if(node instanceof Interpolation interpolation)append(pad,"__cpl_escape("+expression(interpolation.expression)+")");
        else if(node instanceof Element element){
            StringBuilder opening=new StringBuilder("<").append(element.tag);
            append(pad,pyString(opening.toString()));
            for(Attribute attribute:element.attributes)append(pad,pyString(" "+attribute.name+"=\"")+" + __cpl_render_attr("+pyString(attribute.rawValue)+", __cpl_context) + "+pyString("\""));
            append(pad,pyString(element.selfClosing?"/>":">"));nodes(element.nodes,depth);if(!element.selfClosing)append(pad,pyString("</"+element.tag+">"));
        } else if(node instanceof ForBlock loop){out.append(pad).append("for ").append(loop.variable).append(" in ").append(expression(loop.iterable)).append(":\n");out.append(pad).append("    __cpl_context = dict(__cpl_context, ").append(loop.variable).append('=').append(loop.variable).append(")\n");nodes(loop.body,depth+1);}
        else if(node instanceof IfBlock conditional){
            for(int i=0;i<conditional.branches.size();i++){Branch branch=conditional.branches.get(i);out.append(pad).append(i==0?"if ":"elif ").append(expression(branch.condition)).append(":\n");nodes(branch.body,depth+1);}
            if(!conditional.elseBody.isEmpty()){out.append(pad).append("else:\n");nodes(conditional.elseBody,depth+1);}
        }
    }
    private void append(String pad,String value){out.append(pad).append("__cpl_out.append(").append(value).append(")\n");}
    private String expression(Expression expression){
        if(expression instanceof Name name)return "__cpl_get(__cpl_context, "+pyString(name.name)+")";
        if(expression instanceof Literal literal)return literal.raw;
        if(expression instanceof Unary unary)return unary.operator.equals("not")?"(not "+expression(unary.operand)+")":"("+unary.operator+expression(unary.operand)+")";
        if(expression instanceof Binary binary)return "("+expression(binary.left)+" "+binary.operator+" "+expression(binary.right)+")";
        if(expression instanceof AttributeAccess attribute)return "__cpl_attr("+expression(attribute.receiver)+", "+pyString(attribute.name)+")";
        if(expression instanceof Subscript subscript)return expression(subscript.receiver)+"["+expression(subscript.index)+"]";
        throw new IllegalArgumentException("Unsupported template expression "+expression.kind());
    }
    public static String functionName(String template){return "__cpl_render_"+template.replaceAll("[^A-Za-z0-9_]","_")+"_"+Integer.toUnsignedString(template.hashCode(),36);}
    static String pyString(String value){return "'"+value.replace("\\","\\\\").replace("'","\\'").replace("\r","\\r").replace("\n","\\n")+"'";}
}

