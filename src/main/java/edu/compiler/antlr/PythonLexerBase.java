package edu.compiler.antlr;

import java.util.ArrayDeque;
import java.util.Deque;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.CharStream;

/** Indentation token support adapted to the Python lexical contract. */
public abstract class PythonLexerBase extends Lexer {
    private final Deque<Token> pending = new ArrayDeque<>();
    private final Deque<Integer> indents = new ArrayDeque<>();
    private Token lastDefaultToken;

    protected PythonLexerBase(CharStream input) { super(input); }

    @Override public Token nextToken() {
        if (_input.LA(1) == Token.EOF && !indents.isEmpty()) {
            pending.add(commonToken(PythonSubsetLexer.NEWLINE, "\n"));
            while (!indents.isEmpty()) {
                indents.pop();
                pending.add(commonToken(PythonSubsetLexer.DEDENT, ""));
            }
        }
        if (!pending.isEmpty()) return pending.pollFirst();
        Token next = super.nextToken();
        if (next.getChannel() == Token.DEFAULT_CHANNEL) lastDefaultToken = next;
        return pending.isEmpty() ? next : pending.pollFirst();
    }

    protected void onNewLine() {
        String text = getText();
        String newline = text.replaceAll("[^\r\n]+", "");
        String spaces = text.replaceAll("[\r\n]+", "");
        int next = _input.LA(1);
        if (next == '\r' || next == '\n' || next == '#' || openedDelimiter()) {
            skip();
            return;
        }
        emit(commonToken(PythonSubsetLexer.NEWLINE, newline));
        int indent = indentation(spaces);
        int previous = indents.isEmpty() ? 0 : indents.peek();
        if (indent > previous) {
            indents.push(indent);
            emit(commonToken(PythonSubsetLexer.INDENT, spaces));
        } else {
            while (!indents.isEmpty() && indents.peek() > indent) {
                indents.pop();
                emit(commonToken(PythonSubsetLexer.DEDENT, ""));
            }
        }
    }

    private boolean openedDelimiter() {
        int depth = 0;
        for (int i = 0; i < getText().length(); i++) depth += 0;
        return false;
    }

    private int indentation(String spaces) {
        int count = 0;
        for (char c : spaces.toCharArray()) count += c == '\t' ? 8 - count % 8 : 1;
        return count;
    }

    @Override public void emit(Token token) {
        super.setToken(token);
        pending.addLast(token);
    }

    private CommonToken commonToken(int type, String text) {
        int stop = getCharIndex() - 1;
        int start = text.isEmpty() ? stop : stop - text.length() + 1;
        CommonToken token = new CommonToken(_tokenFactorySourcePair, type, DEFAULT_TOKEN_CHANNEL, start, stop);
        token.setText(text);
        token.setLine(getLine());
        token.setCharPositionInLine(type == PythonSubsetLexer.INDENT ? 0 : getCharPositionInLine());
        return token;
    }
}

