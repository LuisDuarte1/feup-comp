grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
COMMA : ',' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
UNDERSCR : '_' ;
DOT : '.' ;
DOLLAR : '$' ;
ELLIPSIS : '...' ;
MUL : '*' ;
ADD : '+' ;
SUB : '-' ;
DIV : '/' ;
NOT : '!' ;
LOGICAL_AND : '&&' ;
LESS : '<' ;
NEW : 'new';

CLASS : 'class' ;
INT : 'int' ;
BOOL : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;

INTEGER : '0' | [1-9][0-9]* ;
ID : LETTER (LETTER | DIGIT | UNDERSCR | DOLLAR)* ;

LETTER : [a-zA-Z] ;
DIGIT : [0-9] ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT ID (DOT ID)* SEMI
    ;

classDecl
    : CLASS name=ID ('extends' ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : INT LBRACKET RBRACKET
    | INT ELLIPSIS
    | BOOL
    | name= INT
    | ID ;

methodDecl locals[boolean isPublic=false] //Guarantees that methodDecl always has an isPublic value
    : (PUBLIC {$isPublic=true;})? 'static' 'void' name='main'
        LPAREN'String'LBRACKET RBRACKET ID RPAREN
        LCURLY varDecl* stmt* RCURLY
    | (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt* RETURN expr SEMI RCURLY

    ;

param
    : type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #MultipleStmts
    | 'if' LPAREN expr RPAREN stmt 'else' stmt #IfStmt
    | 'while' LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | expr EQUALS expr SEMI #AssignStmt
    | expr LBRACKET expr RBRACKET EQUALS expr SEMI #AssignBracketStmt
    ;

expr
    : LPAREN expr RPAREN #PriorityExpr
    | NOT expr #UnaryExpr
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr op=LESS expr #BinaryExpr
    | expr op=LOGICAL_AND expr #BinaryExpr
    | expr LBRACKET expr RBRACKET #ListAccess
    | expr DOT 'length' #MethodCall
    | expr DOT ID LPAREN
        (expr (COMMA expr)*)?
        RPAREN #MethodCall
    | NEW name=ID LPAREN (expr (COMMA expr)*)? RPAREN #NewMethod
    | NEW INT LBRACKET expr RBRACKET #NewArray
    | LBRACKET (expr (COMMA expr)*)? RBRACKET #List
    | value=INTEGER #IntegerLiteral //
    | 'true' #TrueExpr
    | 'false' #FalseExpr
    | 'this' #ThisExpr
    | name=ID #VarRefExpr //
    ;