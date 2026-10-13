parser grammar TemplateParser;

options { tokenVocab=TemplateLexer; }

document: node* EOF;
node: element | interpolation | forBlock | ifBlock | HTML_TEXT | BAD_TEXT_CHAR;
element: TAG_OPEN TAG_NAME attribute* (TAG_SELF_CLOSE | TAG_CLOSE node* TAG_OPEN TAG_SLASH TAG_NAME TAG_CLOSE);
attribute: TAG_NAME TAG_EQUALS ATTR_STRING;
interpolation: EXPR_OPEN templateExpression EXPR_CLOSE;
forBlock: STMT_OPEN T_FOR T_NAME T_IN templateExpression STMT_CLOSE node* STMT_OPEN T_ENDFOR STMT_CLOSE;
ifBlock: STMT_OPEN T_IF templateExpression STMT_CLOSE node* elifBlock* elseBlock? STMT_OPEN T_ENDIF STMT_CLOSE;
elifBlock: STMT_OPEN T_ELIF templateExpression STMT_CLOSE node*;
elseBlock: STMT_OPEN T_ELSE STMT_CLOSE node*;
templateExpression: tOr;
tOr: tAnd (T_OR tAnd)*;
tAnd: tNot (T_AND tNot)*;
tNot: T_NOT tNot | tComparison;
tComparison: tAdditive ((T_EQEQ | T_NOTEQ | T_LT | T_LTE | T_GT | T_GTE | T_IN) tAdditive)*;
tAdditive: tMultiplicative ((T_PLUS | T_MINUS) tMultiplicative)*;
tMultiplicative: tUnary ((T_STAR | T_SLASH | T_FLOORDIV | T_MOD) tUnary)*;
tUnary: (T_PLUS | T_MINUS) tUnary | tPrimary;
tPrimary: tAtom (T_DOT T_NAME | T_LBRACK templateExpression T_RBRACK)*;
tAtom: T_NAME | T_INTEGER | T_FLOAT | T_STRING | T_TRUE | T_FALSE | T_NONE | T_LPAREN templateExpression T_RPAREN;

