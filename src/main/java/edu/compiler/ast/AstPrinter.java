package edu.compiler.ast;

public final class AstPrinter implements AstVisitor<String> {
    @Override public String visit(AstNode node) {
        StringBuilder out = new StringBuilder();
        append(out, node, 0);
        return out.toString();
    }

    private void append(StringBuilder out, AstNode node, int depth) {
        out.append("  ".repeat(depth)).append(node.kind());
        if (!node.detail().isBlank()) out.append('(').append(node.detail()).append(')');
        out.append(" @ ").append(node.location()).append('\n');
        for (AstNode child : node.children()) append(out, child, depth + 1);
    }
}

