parser grammar PythonSubsetParser;

options { tokenVocab=PythonSubsetLexer; }

fileInput: (NEWLINE | statement)* EOF;
statement: decoratedFunction | functionDef | ifStatement | forStatement | whileStatement | simpleStatement;
decoratedFunction: routeDecorator NEWLINE functionDef;
routeDecorator: AT NAME DOT NAME LPAREN STRING (COMMA NAME ASSIGN listLiteral)? RPAREN;
functionDef: DEF NAME LPAREN parameterList? RPAREN COLON suite;
parameterList: NAME (COMMA NAME)*;
suite: simpleStatement | NEWLINE INDENT statement+ DEDENT;
simpleStatement: smallStatement NEWLINE;
smallStatement: assignment | returnStatement | BREAK | CONTINUE | PASS | expression;
assignment: assignTarget (ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | STAR_ASSIGN | SLASH_ASSIGN | FLOORDIV_ASSIGN | MOD_ASSIGN) expression;
assignTarget: NAME | primary DOT NAME | primary LBRACK expression RBRACK;
returnStatement: RETURN expression?;
ifStatement: IF expression COLON suite (ELIF expression COLON suite)* (ELSE COLON suite)?;
forStatement: FOR NAME IN expression COLON suite;
whileStatement: WHILE expression COLON suite;
expression: orExpression;
orExpression: andExpression (OR andExpression)*;
andExpression: notExpression (AND notExpression)*;
notExpression: NOT notExpression | comparison;
comparison: additive ((EQEQ | NOTEQ | LT | LTE | GT | GTE | IN) additive)*;
additive: multiplicative ((PLUS | MINUS) multiplicative)*;
multiplicative: unary ((STAR | SLASH | FLOORDIV | MOD) unary)*;
unary: (PLUS | MINUS) unary | primary;
primary: atom trailer*;
trailer: DOT NAME | LBRACK expression RBRACK | LPAREN argumentList? RPAREN;
argumentList: argument (COMMA argument)*;
argument: (NAME ASSIGN)? expression;
atom: NAME | INTEGER | FLOAT | STRING | TRUE | FALSE | NONE | listLiteral | dictLiteral | LPAREN expression RPAREN;
listLiteral: LBRACK (expression (COMMA expression)*)? RBRACK;
dictLiteral: LBRACE (dictEntry (COMMA dictEntry)*)? RBRACE;
dictEntry: expression COLON expression;

