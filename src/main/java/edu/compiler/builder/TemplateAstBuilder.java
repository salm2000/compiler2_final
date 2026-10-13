package edu.compiler.builder;

import edu.compiler.antlr.TemplateParser;
import edu.compiler.antlr.TemplateParserBaseVisitor;
import edu.compiler.ast.SourceLocation;
import edu.compiler.ast.template.TemplateAst.*;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public final class TemplateAstBuilder extends TemplateParserBaseVisitor<Node> {
    private final String file;
    public TemplateAstBuilder(String file) { this.file=file; }
    @Override public Node visitDocument(TemplateParser.DocumentContext ctx) { return new Document(at(ctx), file, nodes(ctx.node())); }
    @Override public Node visitNode(TemplateParser.NodeContext ctx) {
        if (ctx.element()!=null) return visit(ctx.element());
        if (ctx.interpolation()!=null) return visit(ctx.interpolation());
        if (ctx.forBlock()!=null) return visit(ctx.forBlock());
        if (ctx.ifBlock()!=null) return visit(ctx.ifBlock());
        return new Text(at(ctx), ctx.getText());
    }
    @Override public Node visitElement(TemplateParser.ElementContext ctx) {
        List<Attribute> attrs=ctx.attribute().stream().map(x -> (Attribute)visit(x)).toList();
        boolean self=ctx.TAG_SELF_CLOSE()!=null;
        String close=self?null:ctx.TAG_NAME(1).getText();
        return new Element(at(ctx),ctx.TAG_NAME(0).getText(),close,self,attrs,nodes(ctx.node()));
    }
    @Override public Node visitAttribute(TemplateParser.AttributeContext ctx) {
        String raw=ctx.ATTR_STRING().getText(); raw=raw.substring(1,raw.length()-1);
        return new Attribute(at(ctx),ctx.TAG_NAME().getText(),raw);
    }
    @Override public Node visitInterpolation(TemplateParser.InterpolationContext ctx) { return new Interpolation(at(ctx), expression(ctx.templateExpression())); }
    @Override public Node visitForBlock(TemplateParser.ForBlockContext ctx) {
        return new ForBlock(at(ctx),ctx.T_NAME().getText(),expression(ctx.templateExpression()),nodes(ctx.node()));
    }
    @Override public Node visitIfBlock(TemplateParser.IfBlockContext ctx) {
        List<Branch> branches=new ArrayList<>();
        branches.add(new Branch(at(ctx),expression(ctx.templateExpression()),nodes(ctx.node())));
        for(var branch:ctx.elifBlock()) branches.add((Branch)visit(branch));
        List<Node> otherwise=ctx.elseBlock()==null?List.of():nodes(ctx.elseBlock().node());
        return new IfBlock(at(ctx),branches,otherwise);
    }
    @Override public Node visitElifBlock(TemplateParser.ElifBlockContext ctx) { return new Branch(at(ctx),expression(ctx.templateExpression()),nodes(ctx.node())); }
    private List<Node> nodes(List<TemplateParser.NodeContext> contexts) { return contexts.stream().map(x->visit(x)).toList(); }
    private Expression expression(TemplateParser.TemplateExpressionContext ctx){return (Expression)visit(ctx);}
    @Override public Node visitTemplateExpression(TemplateParser.TemplateExpressionContext ctx){return visit(ctx.tOr());}
    @Override public Node visitTOr(TemplateParser.TOrContext ctx){return fold(ctx,ctx.tAnd().stream().map(x->(Expression)visit(x)).toList());}
    @Override public Node visitTAnd(TemplateParser.TAndContext ctx){return fold(ctx,ctx.tNot().stream().map(x->(Expression)visit(x)).toList());}
    @Override public Node visitTNot(TemplateParser.TNotContext ctx){return ctx.T_NOT()==null?visit(ctx.tComparison()):new Unary(at(ctx),"not",(Expression)visit(ctx.tNot()));}
    @Override public Node visitTComparison(TemplateParser.TComparisonContext ctx){return fold(ctx,ctx.tAdditive().stream().map(x->(Expression)visit(x)).toList());}
    @Override public Node visitTAdditive(TemplateParser.TAdditiveContext ctx){return fold(ctx,ctx.tMultiplicative().stream().map(x->(Expression)visit(x)).toList());}
    @Override public Node visitTMultiplicative(TemplateParser.TMultiplicativeContext ctx){return fold(ctx,ctx.tUnary().stream().map(x->(Expression)visit(x)).toList());}
    @Override public Node visitTUnary(TemplateParser.TUnaryContext ctx){return ctx.tPrimary()!=null?visit(ctx.tPrimary()):new Unary(at(ctx),ctx.getChild(0).getText(),(Expression)visit(ctx.tUnary()));}
    private Expression fold(ParserRuleContext ctx,List<Expression> operands){Expression result=operands.get(0);for(int i=1;i<operands.size();i++)result=new Binary(at(ctx),result,ctx.getChild(i*2-1).getText(),operands.get(i));return result;}
    @Override public Node visitTPrimary(TemplateParser.TPrimaryContext ctx){
        Expression result=(Expression)visit(ctx.tAtom()); int expressionIndex=0;
        for(int i=1;i<ctx.getChildCount();){
            String token=ctx.getChild(i).getText();
            if(token.equals(".")){result=new AttributeAccess(at(ctx),result,ctx.getChild(i+1).getText());i+=2;}
            else {result=new Subscript(at(ctx),result,expression(ctx.templateExpression(expressionIndex++)));i+=3;}
        }
        return result;
    }
    @Override public Node visitTAtom(TemplateParser.TAtomContext ctx){
        if(ctx.templateExpression()!=null)return visit(ctx.templateExpression());
        if(ctx.T_NAME()!=null)return new Name(at(ctx),ctx.T_NAME().getText());
        return new Literal(at(ctx),ctx.getText());
    }
    private SourceLocation at(ParserRuleContext ctx){return at(ctx.getStart());}
    private SourceLocation at(Token token){return new SourceLocation(file,token.getLine(),token.getCharPositionInLine()+1);}
}

