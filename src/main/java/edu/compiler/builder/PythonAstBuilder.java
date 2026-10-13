package edu.compiler.builder;

import edu.compiler.antlr.PythonSubsetParser;
import edu.compiler.antlr.PythonSubsetParserBaseVisitor;
import edu.compiler.ast.SourceLocation;
import edu.compiler.ast.python.PythonAst.*;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public final class PythonAstBuilder extends PythonSubsetParserBaseVisitor<Node> {
    private final String file;
    public PythonAstBuilder(String file) { this.file = file; }

    @Override public Node visitFileInput(PythonSubsetParser.FileInputContext ctx) {
        List<Statement> statements = new ArrayList<>();
        for (var statement : ctx.statement()) statements.add((Statement) visit(statement));
        return new edu.compiler.ast.python.PythonAst.Module(at(ctx), statements);
    }
    @Override public Node visitStatement(PythonSubsetParser.StatementContext ctx) {
        return visit(ctx.getChild(0));
    }
    @Override public Node visitDecoratedFunction(PythonSubsetParser.DecoratedFunctionContext ctx) {
        Route route = (Route) visit(ctx.routeDecorator());
        return function(ctx.functionDef(), route);
    }
    @Override public Node visitRouteDecorator(PythonSubsetParser.RouteDecoratorContext ctx) {
        List<String> methods = new ArrayList<>();
        if (ctx.listLiteral() == null) methods.add("GET");
        else {
            ListLiteral list = (ListLiteral) visit(ctx.listLiteral());
            for (Expression value : list.values) methods.add(strip(((Literal) value).raw).toUpperCase());
        }
        return new Route(at(ctx), strip(ctx.STRING().getText()), methods);
    }
    @Override public Node visitFunctionDef(PythonSubsetParser.FunctionDefContext ctx) { return function(ctx, null); }
    private FunctionDef function(PythonSubsetParser.FunctionDefContext ctx, Route route) {
        List<String> parameters = ctx.parameterList() == null ? List.of()
            : ctx.parameterList().NAME().stream().map(t -> t.getText()).toList();
        return new FunctionDef(at(ctx), ctx.NAME().getText(), parameters, route, (Block) visit(ctx.suite()));
    }
    @Override public Node visitSuite(PythonSubsetParser.SuiteContext ctx) {
        List<Statement> statements = new ArrayList<>();
        if (ctx.simpleStatement() != null) statements.add((Statement) visit(ctx.simpleStatement()));
        else for (var statement : ctx.statement()) statements.add((Statement) visit(statement));
        return new Block(at(ctx), statements);
    }
    @Override public Node visitSimpleStatement(PythonSubsetParser.SimpleStatementContext ctx) { return visit(ctx.smallStatement()); }
    @Override public Node visitSmallStatement(PythonSubsetParser.SmallStatementContext ctx) {
        if (ctx.assignment() != null) return visit(ctx.assignment());
        if (ctx.returnStatement() != null) return visit(ctx.returnStatement());
        if (ctx.expression() != null) return new ExprStatement(at(ctx), expression(ctx.expression()));
        return new Flow(at(ctx), ctx.getText());
    }
    @Override public Node visitAssignment(PythonSubsetParser.AssignmentContext ctx) {
        return new Assign(at(ctx), target(ctx.assignTarget()), ctx.getChild(1).getText(), expression(ctx.expression()));
    }
    private Expression target(PythonSubsetParser.AssignTargetContext ctx) {
        if (ctx.primary() == null) return new Name(at(ctx), ctx.NAME().getText());
        Expression receiver = (Expression) visit(ctx.primary());
        if (ctx.DOT() != null) return new Attribute(at(ctx), receiver, ctx.NAME().getText());
        return new Subscript(at(ctx), receiver, expression(ctx.expression()));
    }
    @Override public Node visitReturnStatement(PythonSubsetParser.ReturnStatementContext ctx) {
        return new Return(at(ctx), ctx.expression() == null ? null : expression(ctx.expression()));
    }
    @Override public Node visitIfStatement(PythonSubsetParser.IfStatementContext ctx) {
        List<Branch> branches = new ArrayList<>();
        for (int i=0; i<ctx.expression().size(); i++)
            branches.add(new Branch(at(ctx.expression(i)), expression(ctx.expression(i)), (Block) visit(ctx.suite(i))));
        Block otherwise = ctx.suite().size() > ctx.expression().size() ? (Block) visit(ctx.suite(ctx.suite().size()-1)) : null;
        return new If(at(ctx), branches, otherwise);
    }
    @Override public Node visitForStatement(PythonSubsetParser.ForStatementContext ctx) {
        return new For(at(ctx), ctx.NAME().getText(), expression(ctx.expression()), (Block) visit(ctx.suite()));
    }
    @Override public Node visitWhileStatement(PythonSubsetParser.WhileStatementContext ctx) {
        return new While(at(ctx), expression(ctx.expression()), (Block) visit(ctx.suite()));
    }

    private Expression expression(PythonSubsetParser.ExpressionContext ctx) { return (Expression) visit(ctx); }
    @Override public Node visitExpression(PythonSubsetParser.ExpressionContext ctx) { return visit(ctx.orExpression()); }
    @Override public Node visitOrExpression(PythonSubsetParser.OrExpressionContext ctx) { return fold(ctx, ctx.andExpression().stream().map(x -> (Expression)visit(x)).toList()); }
    @Override public Node visitAndExpression(PythonSubsetParser.AndExpressionContext ctx) { return fold(ctx, ctx.notExpression().stream().map(x -> (Expression)visit(x)).toList()); }
    @Override public Node visitNotExpression(PythonSubsetParser.NotExpressionContext ctx) {
        return ctx.NOT() == null ? visit(ctx.comparison()) : new Unary(at(ctx), "not", (Expression) visit(ctx.notExpression()));
    }
    @Override public Node visitComparison(PythonSubsetParser.ComparisonContext ctx) { return fold(ctx, ctx.additive().stream().map(x -> (Expression)visit(x)).toList()); }
    @Override public Node visitAdditive(PythonSubsetParser.AdditiveContext ctx) { return fold(ctx, ctx.multiplicative().stream().map(x -> (Expression)visit(x)).toList()); }
    @Override public Node visitMultiplicative(PythonSubsetParser.MultiplicativeContext ctx) { return fold(ctx, ctx.unary().stream().map(x -> (Expression)visit(x)).toList()); }
    @Override public Node visitUnary(PythonSubsetParser.UnaryContext ctx) {
        return ctx.primary() != null ? visit(ctx.primary()) : new Unary(at(ctx), ctx.getChild(0).getText(), (Expression)visit(ctx.unary()));
    }
    private Expression fold(ParserRuleContext ctx, List<Expression> operands) {
        Expression result = operands.get(0);
        for (int i=1; i<operands.size(); i++) result = new Binary(at(ctx), result, ctx.getChild(i*2-1).getText(), operands.get(i));
        return result;
    }
    @Override public Node visitPrimary(PythonSubsetParser.PrimaryContext ctx) {
        Expression result = (Expression) visit(ctx.atom());
        for (var trailer : ctx.trailer()) {
            if (trailer.DOT() != null) result = new Attribute(at(trailer), result, trailer.NAME().getText());
            else if (trailer.LBRACK() != null) result = new Subscript(at(trailer), result, expression(trailer.expression()));
            else {
                List<Argument> args = new ArrayList<>();
                if (trailer.argumentList() != null) for (var argument : trailer.argumentList().argument()) args.add((Argument)visit(argument));
                result = new Call(at(trailer), result, args);
            }
        }
        return result;
    }
    @Override public Node visitArgument(PythonSubsetParser.ArgumentContext ctx) {
        return new Argument(at(ctx), ctx.NAME() == null ? null : ctx.NAME().getText(), expression(ctx.expression()));
    }
    @Override public Node visitAtom(PythonSubsetParser.AtomContext ctx) {
        if (ctx.listLiteral() != null) return visit(ctx.listLiteral());
        if (ctx.dictLiteral() != null) return visit(ctx.dictLiteral());
        if (ctx.expression() != null) return visit(ctx.expression());
        if (ctx.NAME() != null) return new Name(at(ctx), ctx.NAME().getText());
        return new Literal(at(ctx), ctx.getText());
    }
    @Override public Node visitListLiteral(PythonSubsetParser.ListLiteralContext ctx) {
        return new ListLiteral(at(ctx), ctx.expression().stream().map(this::expression).toList());
    }
    @Override public Node visitDictLiteral(PythonSubsetParser.DictLiteralContext ctx) {
        List<DictEntry> entries = ctx.dictEntry().stream().map(x -> (DictEntry)visit(x)).toList();
        return new DictLiteral(at(ctx), entries);
    }
    @Override public Node visitDictEntry(PythonSubsetParser.DictEntryContext ctx) {
        return new DictEntry(at(ctx), expression(ctx.expression(0)), expression(ctx.expression(1)));
    }
    private SourceLocation at(ParserRuleContext ctx) { return at(ctx.getStart()); }
    private SourceLocation at(Token token) { return new SourceLocation(file, token.getLine(), token.getCharPositionInLine()+1); }
    private String strip(String text) { return text.length() >= 2 ? text.substring(1, text.length()-1) : text; }
}

