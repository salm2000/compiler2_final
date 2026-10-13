package edu.compiler.ast;

@FunctionalInterface
public interface AstVisitor<R> {
    R visit(AstNode node);
}

