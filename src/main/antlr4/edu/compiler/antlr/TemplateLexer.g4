lexer grammar TemplateLexer;

EXPR_OPEN: '{{' -> pushMode(CODE);
STMT_OPEN: '{%' -> pushMode(CODE);
TAG_OPEN: '<' -> pushMode(TAG);
HTML_TEXT: ~[<{]+;
BAD_TEXT_CHAR: .;

mode TAG;
TAG_SLASH: '/';
TAG_CLOSE: '>' -> popMode;
TAG_SELF_CLOSE: '/>' -> popMode;
TAG_EQUALS: '=';
TAG_NAME: [a-zA-Z] [a-zA-Z0-9-]*;
ATTR_STRING: '"' (~["\r\n])* '"' | '\'' (~['\r\n])* '\'';
TAG_WS: [ \t\r\n]+ -> skip;

mode CODE;
EXPR_CLOSE: '}}' -> popMode;
STMT_CLOSE: '%}' -> popMode;
T_FOR:'for'; T_IN:'in'; T_ENDFOR:'endfor'; T_IF:'if'; T_ELIF:'elif'; T_ELSE:'else'; T_ENDIF:'endif';
T_AND:'and'; T_OR:'or'; T_NOT:'not'; T_TRUE:'True'; T_FALSE:'False'; T_NONE:'None';
T_EQEQ:'=='; T_NOTEQ:'!='; T_LTE:'<='; T_GTE:'>='; T_LT:'<'; T_GT:'>';
T_PLUS:'+'; T_MINUS:'-'; T_STAR:'*'; T_FLOORDIV:'//'; T_SLASH:'/'; T_MOD:'%'; T_DOT:'.';
T_LPAREN:'('; T_RPAREN:')'; T_LBRACK:'['; T_RBRACK:']';
T_FLOAT: [0-9]+ '.' [0-9]+;
T_INTEGER: [0-9]+;
T_STRING: '\'' ('\\' . | ~['\\\r\n])* '\'' | '"' ('\\' . | ~["\\\r\n])* '"';
T_NAME: [a-zA-Z_] [a-zA-Z_0-9]*;
CODE_WS: [ \t\r\n]+ -> skip;

