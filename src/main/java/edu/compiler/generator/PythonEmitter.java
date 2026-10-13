package edu.compiler.generator;

import edu.compiler.ast.python.PythonAst.*;
import java.util.stream.Collectors;

public final class PythonEmitter {
    private final StringBuilder out=new StringBuilder();
    public String emit(edu.compiler.ast.python.PythonAst.Module module) {
        for(Statement statement:module.statements)statement(statement,0);
        return out.toString();
    }
    private void statement(Statement statement,int depth) {
        String pad="    ".repeat(depth);
        if(statement instanceof FunctionDef function){
            out.append("\n").append(pad).append("def ").append(function.name).append('(').append(String.join(", ",function.parameters)).append("):\n");
            block(function.body,depth+1);
        } else if(statement instanceof Assign assign)out.append(pad).append(expression(assign.target)).append(' ').append(assign.operator).append(' ').append(expression(assign.value)).append('\n');
        else if(statement instanceof ExprStatement expression)out.append(pad).append(expression(expression.expression)).append('\n');
        else if(statement instanceof Return value)out.append(pad).append("return").append(value.expression==null?"":" "+expression(value.expression)).append('\n');
        else if(statement instanceof If conditional){
            for(int i=0;i<conditional.branches.size();i++){Branch branch=conditional.branches.get(i);out.append(pad).append(i==0?"if ":"elif ").append(expression(branch.condition)).append(":\n");block(branch.body,depth+1);}
            if(conditional.elseBody!=null){out.append(pad).append("else:\n");block(conditional.elseBody,depth+1);}
        } else if(statement instanceof For loop){out.append(pad).append("for ").append(loop.variable).append(" in ").append(expression(loop.iterable)).append(":\n");block(loop.body,depth+1);}
        else if(statement instanceof While loop){out.append(pad).append("while ").append(expression(loop.condition)).append(":\n");block(loop.body,depth+1);}
        else if(statement instanceof Flow flow)out.append(pad).append(flow.keyword).append('\n');
    }
    private void block(Block block,int depth){if(block.statements.isEmpty())out.append("    ".repeat(depth)).append("pass\n");else block.statements.forEach(x->statement(x,depth));}
    public String expression(Expression expression) {
        if(expression instanceof Name name)return name.name;
        if(expression instanceof Literal literal)return literal.raw;
        if(expression instanceof ListLiteral list)return "["+list.values.stream().map(this::expression).collect(Collectors.joining(", "))+"]";
        if(expression instanceof DictLiteral dict)return "{"+dict.entries.stream().map(x->expression(x.key)+": "+expression(x.value)).collect(Collectors.joining(", "))+"}";
        if(expression instanceof Unary unary)return unary.operator.equals("not")?"(not "+expression(unary.operand)+")":"("+unary.operator+expression(unary.operand)+")";
        if(expression instanceof Binary binary)return "("+expression(binary.left)+" "+binary.operator+" "+expression(binary.right)+")";
        if(expression instanceof Attribute attribute)return expression(attribute.receiver)+"."+attribute.name;
        if(expression instanceof Subscript subscript)return expression(subscript.receiver)+"["+expression(subscript.index)+"]";
        if(expression instanceof Call call)return expression(call.callee)+"("+call.arguments.stream().map(x->(x.name==null?"":x.name+"=")+expression(x.value)).collect(Collectors.joining(", "))+")";
        throw new IllegalArgumentException("Unsupported expression "+expression.kind());
    }
}

