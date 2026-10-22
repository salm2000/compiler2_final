package edu.compiler.ast.python;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import edu.compiler.ast.AstNode;
import edu.compiler.ast.AstVisitor;
import edu.compiler.ast.SourceLocation;

public final class PythonAst {
    private PythonAst() {}

    public abstract static class Node extends AstNode {
        protected Node(SourceLocation location) { super(location); }
        @Override public final <R> R accept(AstVisitor<R> visitor) { return visitor.visit(this); }
    }
    public abstract static class Statement extends Node { protected Statement(SourceLocation p) { super(p); } }
    public abstract static class Expression extends Node { protected Expression(SourceLocation p) { super(p); } }

    public static final class Module extends Node {
        public final List<Statement> statements;
        public Module(SourceLocation p, List<Statement> statements) { super(p); this.statements = List.copyOf(statements); }
        @Override public String kind() { return "PythonModule"; }
        @Override public List<Statement> children() { return statements; }
    }
    public static final class Block extends Node {
        public final List<Statement> statements;
        public Block(SourceLocation p, List<Statement> statements) { super(p); this.statements = List.copyOf(statements); }
        @Override public String kind() { return "Block"; }
        @Override public List<Statement> children() { return statements; }
    }
    public static final class Route extends Node {
        public final String path;
        public final List<String> methods;
        public Route(SourceLocation p, String path, List<String> methods) { super(p); this.path = path; this.methods = List.copyOf(methods); }
        @Override public String kind() { return "RouteDecorator"; }
        @Override public String detail() { return String.join(",", methods) + " " + path; }
        @Override public List<Node> children() { return List.of(); }
    }
    public static final class FunctionDef extends Statement {
        public final String name; public final List<String> parameters; public final Route route; public final Block body;
        public FunctionDef(SourceLocation p, String name, List<String> parameters, Route route, Block body) {
            super(p); this.name=name; this.parameters=List.copyOf(parameters); this.route=route; this.body=body;
        }
        @Override public String kind() { return "FunctionDef"; }
        @Override public String detail() { return name + ", params=" + parameters; }
        @Override public List<Node> children() { return route == null ? List.of(body) : List.of(route, body); }
    }
    public static final class Assign extends Statement {
        public final Expression target, value; public final String operator;
        public Assign(SourceLocation p, Expression target, String operator, Expression value) { super(p); this.target=target; this.operator=operator; this.value=value; }
        @Override public String kind() { return operator.equals("=") ? "Assign" : "AugAssign"; }
        @Override public String detail() { return operator; }
        @Override public List<Expression> children() { return List.of(target, value); }
    }
    public static final class ExprStatement extends Statement {
        public final Expression expression;
        public ExprStatement(SourceLocation p, Expression expression) { super(p); this.expression=expression; }
        @Override public String kind() { return "ExprStatement"; }
        @Override public List<Expression> children() { return List.of(expression); }
    }
    public static final class Return extends Statement {
        public final Expression expression;
        public Return(SourceLocation p, Expression expression) { super(p); this.expression=expression; }
        @Override public String kind() { return "Return"; }
        @Override public List<Expression> children() { return expression == null ? List.of() : List.of(expression); }
    }
    public static final class If extends Statement {
        public final List<Branch> branches; public final Block elseBody;
        public If(SourceLocation p, List<Branch> branches, Block elseBody) { super(p); this.branches=List.copyOf(branches); this.elseBody=elseBody; }
        @Override public String kind() { return "If"; }
        @Override public List<Node> children() {
            ArrayList<Node> result = new ArrayList<>(branches); if (elseBody != null) result.add(elseBody); return result;
        }
    }
    public static final class Branch extends Node {
        public final Expression condition; public final Block body;
        public Branch(SourceLocation p, Expression condition, Block body) { super(p); this.condition=condition; this.body=body; }
        @Override public String kind() { return "Branch"; }
        @Override public List<Node> children() { return List.of(condition, body); }
    }
    public static final class For extends Statement {
        public final String variable; public final Expression iterable; public final Block body;
        public For(SourceLocation p, String variable, Expression iterable, Block body) { super(p); this.variable=variable; this.iterable=iterable; this.body=body; }
        @Override public String kind() { return "For"; }
        @Override public String detail() { return variable; }
        @Override public List<Node> children() { return List.of(iterable, body); }
    }
    public static final class While extends Statement {
        public final Expression condition; public final Block body;
        public While(SourceLocation p, Expression condition, Block body) { super(p); this.condition=condition; this.body=body; }
        @Override public String kind() { return "While"; }
        @Override public List<Node> children() { return List.of(condition, body); }
    }
    public static final class Flow extends Statement {
        public final String keyword;
        public Flow(SourceLocation p, String keyword) { super(p); this.keyword=keyword; }
        @Override public String kind() { return Character.toUpperCase(keyword.charAt(0)) + keyword.substring(1); }
        @Override public List<Node> children() { return List.of(); }
    }
    public static final class Name extends Expression {
        public final String name;
        public Name(SourceLocation p, String name) { super(p); this.name=name; }
        @Override public String kind() { return "Name"; }
        @Override public String detail() { return name; }
        @Override public List<Node> children() { return List.of(); }
    }
    public static final class Literal extends Expression {
        public final String raw;
        public Literal(SourceLocation p, String raw) { super(p); this.raw=raw; }
        @Override public String kind() { return "Literal"; }
        @Override public String detail() { return raw; }
        @Override public List<Node> children() { return List.of(); }
    }
    public static final class ListLiteral extends Expression {
        public final List<Expression> values;
        public ListLiteral(SourceLocation p, List<Expression> values) { super(p); this.values=List.copyOf(values); }
        @Override public String kind() { return "ListLiteral"; }
        @Override public List<Expression> children() { return values; }
    }
    public static final class DictLiteral extends Expression {
        public final List<DictEntry> entries;
        public DictLiteral(SourceLocation p, List<DictEntry> entries) { super(p); this.entries=List.copyOf(entries); }
        @Override public String kind() { return "DictLiteral"; }
        @Override public List<DictEntry> children() { return entries; }
    }
    public static final class DictEntry extends Node {
        public final Expression key, value;
        public DictEntry(SourceLocation p, Expression key, Expression value) { super(p); this.key=key; this.value=value; }
        @Override public String kind() { return "DictEntry"; }
        @Override public List<Expression> children() { return List.of(key, value); }
    }
    public static final class Unary extends Expression {
        public final String operator; public final Expression operand;
        public Unary(SourceLocation p, String operator, Expression operand) { super(p); this.operator=operator; this.operand=operand; }
        @Override public String kind() { return "UnaryExpr"; }
        @Override public String detail() { return operator; }
        @Override public List<Expression> children() { return List.of(operand); }
    }
    public static final class Binary extends Expression {
        public final Expression left, right; public final String operator;
        public Binary(SourceLocation p, Expression left, String operator, Expression right) { super(p); this.left=left; this.operator=operator; this.right=right; }
        @Override public String kind() { return "BinaryExpr"; }
        @Override public String detail() { return operator; }
        @Override public List<Expression> children() { return List.of(left, right); }
    }
    public static final class Attribute extends Expression {
        public final Expression receiver; public final String name;
        public Attribute(SourceLocation p, Expression receiver, String name) { super(p); this.receiver=receiver; this.name=name; }
        @Override public String kind() { return "AttributeAccess"; }
        @Override public String detail() { return name; }
        @Override public List<Expression> children() { return List.of(receiver); }
    }
    public static final class Subscript extends Expression {
        public final Expression receiver, index;
        public Subscript(SourceLocation p, Expression receiver, Expression index) { super(p); this.receiver=receiver; this.index=index; }
        @Override public String kind() { return "SubscriptAccess"; }
        @Override public List<Expression> children() { return List.of(receiver, index); }
    }
    public static final class Call extends Expression {
        public final Expression callee; public final List<Argument> arguments;
        public Call(SourceLocation p, Expression callee, List<Argument> arguments) { super(p); this.callee=callee; this.arguments=List.copyOf(arguments); }
        @Override public String kind() { return "Call"; }
        @Override public List<Node> children() { return Stream.concat(Stream.of(callee), arguments.stream()).toList(); }
    }
    public static final class Argument extends Node {
        public final String name; public final Expression value;
        public Argument(SourceLocation p, String name, Expression value) { super(p); this.name=name; this.value=value; }
        @Override public String kind() { return "Argument"; }
        @Override public String detail() { return name == null ? "positional" : name; }
        @Override public List<Expression> children() { return List.of(value); }
    }
}

