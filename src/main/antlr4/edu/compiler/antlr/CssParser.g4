parser grammar CssParser;

options { tokenVocab=CssLexer; }

stylesheet: rule* EOF;
rule: selector (COMMA selector)* LBRACE declaration+ RBRACE;
selector: IDENT | DOT IDENT | HASH IDENT;
declaration: property COLON value+ SEMI;
property: IDENT (MINUS IDENT)*;
value: IDENT | NUMBER (IDENT | PERCENT)? | HASH (IDENT | NUMBER) | MINUS NUMBER (IDENT | PERCENT)?;

