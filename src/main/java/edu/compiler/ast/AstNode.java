package edu.compiler.ast;

import java.util.List;

public abstract class AstNode {
    private final SourceLocation location;
    protected AstNode(SourceLocation location) { this.location = location; }
    public final SourceLocation location() { return location; }
    public abstract String kind();
    public String detail() { return ""; }
    public abstract List<? extends AstNode> children();
    public abstract <R> R accept(AstVisitor<R> visitor);
}

