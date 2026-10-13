lexer grammar CssLexer;

LBRACE:'{'; RBRACE:'}'; COLON:':'; SEMI:';'; COMMA:','; DOT:'.'; HASH:'#'; PERCENT:'%'; MINUS:'-';
NUMBER:[0-9]+ ('.' [0-9]+)?;
IDENT:[a-zA-Z_] [a-zA-Z_0-9-]*;
WS:[ \t\r\n]+ -> skip;

