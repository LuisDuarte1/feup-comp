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
STRING : 'String' ;

INTEGER : '0' | [1-9][0-9]* ;
ID : LETTER (LETTER | DIGIT | UNDERSCR | DOLLAR)* ;

LETTER : [a-zA-Z] ;
DIGIT : [0-9] ;

LINE_COMMENT : '//' (.*?) [\r\n] -> skip;
MULTI_LINE_COMMENT : '/*' (.*?) '*/' -> skip;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT modules+=ID (DOT modules+=ID)* SEMI
    ;

classDecl locals[boolean hasParent=false]
    : CLASS name=ID ('extends' {$hasParent = true;} parent=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : typename=type name=ID SEMI
    ;

type
    : INT LBRACKET RBRACKET #IntArrayType
    | INT ELLIPSIS #IntVarargsType
    | BOOL #BoolType
    | STRING #StrType
    | INT #IntType
    | name= ID  #ObjectType
    ;

methodDecl locals[boolean isPublic=false] //Guarantees that methodDecl always has an isPublic value
    : (PUBLIC {$isPublic=true;})? 'static' returnType='void' name='main'
        LPAREN STRING LBRACKET RBRACKET ID RPAREN
        LCURLY varDecl* stmt* RCURLY #MainMethod
    | (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt* RETURN returnExpr=expr SEMI RCURLY #Method

    ;

param
    : typename=type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #BlockStmt
    | 'if' LPAREN ifCond=expr RPAREN ifExpr=stmt 'else' elseExpr=stmt #IfStmt
    | 'while' LPAREN whileCond=expr RPAREN whileExpr=stmt #WhileStmt
    | expr SEMI #ExprStmt
    | expr EQUALS expr SEMI #AssignStmt
    | expr LBRACKET arrayIdx=expr RBRACKET EQUALS expr SEMI #ListAssignStmt
    ;

expr
    : LPAREN expr RPAREN #PriorityExpr
    | NOT expr #UnaryExpr
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr op= LESS expr #BinaryExpr
    | expr op= LOGICAL_AND expr #BinaryExpr
    | expr LBRACKET expr RBRACKET #ListAccess
    | expr DOT 'length' #LengthCall
    | expr DOT name=ID LPAREN
        (expr (COMMA expr)*)?
        RPAREN #MethodCall
    | NEW name=ID LPAREN (expr (COMMA expr)*)? RPAREN #NewMethod
    | NEW INT LBRACKET expr RBRACKET #NewArray
    | LBRACKET (expr (COMMA expr)*)? RBRACKET #Array

    | value=INTEGER #IntegerLiteral//
    | 'true' #TrueLiteral
    | 'false' #FalseLiteral
    | 'this' #ThisLiteral
    | name=ID #VarRefExpr //
    ;