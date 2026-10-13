lexer grammar PythonSubsetLexer;

options { superClass=PythonLexerBase; }

tokens { INDENT, DEDENT }

DEF:'def'; RETURN:'return'; IF:'if'; ELIF:'elif'; ELSE:'else'; FOR:'for'; IN:'in'; WHILE:'while';
BREAK:'break'; CONTINUE:'continue'; PASS:'pass'; AND:'and'; OR:'or'; NOT:'not'; TRUE:'True'; FALSE:'False'; NONE:'None';
AT:'@'; COLON:':'; COMMA:','; DOT:'.'; LPAREN:'('; RPAREN:')'; LBRACK:'['; RBRACK:']'; LBRACE:'{'; RBRACE:'}';
PLUS_ASSIGN:'+='; MINUS_ASSIGN:'-='; STAR_ASSIGN:'*='; SLASH_ASSIGN:'/='; FLOORDIV_ASSIGN:'//='; MOD_ASSIGN:'%=';
EQEQ:'=='; NOTEQ:'!='; LTE:'<='; GTE:'>='; ASSIGN:'='; LT:'<'; GT:'>'; PLUS:'+'; MINUS:'-'; STAR:'*'; FLOORDIV:'//'; SLASH:'/'; MOD:'%';
FLOAT: DIGIT+ '.' DIGIT+;
INTEGER: DIGIT+;
STRING: '\'' (ESC | ~['\\\r\n])* '\'' | '"' (ESC | ~["\\\r\n])* '"';
NAME: [a-zA-Z_] [a-zA-Z_0-9]*;
NEWLINE: ('\r'? '\n' | '\r') [ \t]* {onNewLine();};
COMMENT: '#' ~[\r\n]* -> skip;
WS: [ \t]+ -> skip;
fragment DIGIT: [0-9];
fragment ESC: '\\' .;

