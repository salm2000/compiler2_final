package edu.compiler.ast.template;

import edu.compiler.ast.*;
import java.util.ArrayList;
import java.util.List;

public final class TemplateAst {
    private TemplateAst() {}
    public abstract static class Node extends AstNode {
        protected Node(SourceLocation p) { super(p); }
        @Override public final <R> R accept(AstVisitor<R> visitor) { return visitor.visit(this); }
    }
    public abstract static class Expression extends Node { protected Expression(SourceLocation p) { super(p); } }
    public static final class Document extends Node {
        public final String name; public final List<Node> nodes;
        public Document(SourceLocation p, String name, List<Node> nodes) { super(p); this.name=name; this.nodes=List.copyOf(nodes); }
        @Override public String kind() { return "TemplateDocument"; }
        @Override public String detail() { return name; }
        @Override public List<Node> children() { return nodes; }
    }
    public static final class Element extends Node {
        public final String tag, closingTag; public final boolean selfClosing; public final List<Attribute> attributes; public final List<Node> nodes;
        public Element(SourceLocation p, String tag, String closingTag, boolean selfClosing, List<Attribute> attrs, List<Node> nodes) {
            super(p); this.tag=tag; this.closingTag=closingTag; this.selfClosing=selfClosing; this.attributes=List.copyOf(attrs); this.nodes=List.copyOf(nodes);
        }
        @Override public String kind() { return "HtmlElement"; }
        @Override public String detail() { return tag; }
        @Override public List<Node> children() { ArrayList<Node> all=new ArrayList<>(attributes); all.addAll(nodes); return all; }
    }
    public static final class Attribute extends Node {
        public final String name, rawValue;
        public Attribute(SourceLocation p, String name, String rawValue) { super(p); this.name=name; this.rawValue=rawValue; }
        @Override public String kind() { return "HtmlAttribute"; }
        @Override public String detail() { return name + "=" + rawValue; }
        @Override public List<Node> children() { return List.of(); }
    }
    public static final class Text extends Node {
        public final String text;
        public Text(SourceLocation p, String text) { super(p); this.text=text; }
        @Override public String kind() { return "Text"; }
        @Override public String detail() { return text.replace("\n", "\\n"); }
        @Override public List<Node> children() { return List.of(); }
    }
    public static final class Interpolation extends Node {
        public final Expression expression;
        public Interpolation(SourceLocation p, Expression expression) { super(p); this.expression=expression; }
        @Override public String kind() { return "Interpolation"; }
        @Override public List<Expression> children() { return List.of(expression); }
    }
    public static final class ForBlock extends Node {
        public final String variable; public final Expression iterable; public final List<Node> body;
        public ForBlock(SourceLocation p, String variable, Expression iterable, List<Node> body) { super(p); this.variable=variable; this.iterable=iterable; this.body=List.copyOf(body); }
        @Override public String kind() { return "ForBlock"; }
        @Override public String detail() { return variable; }
        @Override public List<Node> children() { ArrayList<Node> all=new ArrayList<>(); all.add(iterable); all.addAll(body); return all; }
    }
    public static final class IfBlock extends Node {
        public final List<Branch> branches; public final List<Node> elseBody;
        public IfBlock(SourceLocation p, List<Branch> branches, List<Node> elseBody) { super(p); this.branches=List.copyOf(branches); this.elseBody=List.copyOf(elseBody); }
        @Override public String kind() { return "IfBlock"; }
        @Override public List<Node> children() { ArrayList<Node> all=new ArrayList<>(branches); all.addAll(elseBody); return all; }
    }
    public static final class Branch extends Node {
        public final Expression condition; public final List<Node> body;
        public Branch(SourceLocation p, Expression condition, List<Node> body) { super(p); this.condition=condition; this.body=List.copyOf(body); }
        @Override public String kind() { return "TemplateBranch"; }
        @Override public List<Node> children() { ArrayList<Node> all=new ArrayList<>(); all.add(condition); all.addAll(body); return all; }
    }
    public static final class Name extends Expression {
        public final String name; public Name(SourceLocation p,String name){super(p);this.name=name;}
        @Override public String kind(){return "TemplateName";} @Override public String detail(){return name;} @Override public List<Node> children(){return List.of();}
    }
    public static final class Literal extends Expression {
        public final String raw; public Literal(SourceLocation p,String raw){super(p);this.raw=raw;}
        @Override public String kind(){return "TemplateLiteral";} @Override public String detail(){return raw;} @Override public List<Node> children(){return List.of();}
    }
    public static final class Unary extends Expression {
        public final String operator; public final Expression operand;
        public Unary(SourceLocation p,String operator,Expression operand){super(p);this.operator=operator;this.operand=operand;}
        @Override public String kind(){return "TemplateUnaryExpr";} @Override public String detail(){return operator;} @Override public List<Expression> children(){return List.of(operand);}
    }
    public static final class Binary extends Expression {
        public final Expression left,right; public final String operator;
        public Binary(SourceLocation p,Expression left,String operator,Expression right){super(p);this.left=left;this.operator=operator;this.right=right;}
        @Override public String kind(){return "TemplateBinaryExpr";} @Override public String detail(){return operator;} @Override public List<Expression> children(){return List.of(left,right);}
    }
    public static final class AttributeAccess extends Expression {
        public final Expression receiver; public final String name;
        public AttributeAccess(SourceLocation p,Expression receiver,String name){super(p);this.receiver=receiver;this.name=name;}
        @Override public String kind(){return "TemplateAttribute";} @Override public String detail(){return name;} @Override public List<Expression> children(){return List.of(receiver);}
    }
    public static final class Subscript extends Expression {
        public final Expression receiver,index;
        public Subscript(SourceLocation p,Expression receiver,Expression index){super(p);this.receiver=receiver;this.index=index;}
        @Override public String kind(){return "TemplateSubscript";} @Override public List<Expression> children(){return List.of(receiver,index);}
    }
    public static final class CssStylesheetRef extends Node {
        public final String path;
        public CssStylesheetRef(SourceLocation p, String path) { super(p); this.path=path; }
        @Override public String kind(){return "CssStylesheetRef";} @Override public String detail(){return path;} @Override public List<Node> children(){return List.of();}
    }
}
