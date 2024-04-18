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
TRUE : 'true' ;
FALSE : 'false' ;

CLASS : 'class' ;
INT : 'int' ;
BOOL : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;
STRING : 'String' ;
MAIN : 'main';
LENGTH : 'length';
THIS : 'this';

INTEGER : '0' | [1-9][0-9]* ;
ID : (LETTER | UNDERSCR | DOLLAR)(LETTER | DIGIT | UNDERSCR | DOLLAR)*;

LETTER : [a-zA-Z] ;
DIGIT : [0-9] ;

LINE_COMMENT : '//' (.*?) [\r\n] -> skip;
MULTI_LINE_COMMENT : '/*' (.*?) '*/' -> skip;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT modules+=(ID | LENGTH | MAIN) (DOT modules+=(ID | LENGTH | MAIN))* SEMI
    ;

classDecl locals[boolean hasParent=false]
    : CLASS name=(ID | LENGTH | MAIN) ('extends' {$hasParent = true;} parent=(ID | LENGTH | MAIN))?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : typename=type name=(ID | MAIN | LENGTH | STRING) SEMI
    ;

type
    : INT LBRACKET RBRACKET #IntArrayType
    | INT ELLIPSIS #IntVarargsType
    | BOOL #BoolType
    | STRING #StrType
    | INT #IntType
    | name= (ID | LENGTH | MAIN)  #ObjectType
    ;

methodDecl locals[boolean isPublic=false] //Guarantees that methodDecl always has an isPublic value
    : (PUBLIC {$isPublic=true;})? 'static' returnType='void' name=MAIN
        LPAREN STRING LBRACKET RBRACKET (ID | LENGTH | MAIN) RPAREN
        LCURLY varDecl* stmt* RCURLY #MainMethod
    | (PUBLIC {$isPublic=true;})?
        returnType=type name=(ID | LENGTH | MAIN)
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt* RETURN returnExpr=expr SEMI RCURLY #Method

    ;

param
    : typename=type name=(ID | LENGTH | MAIN | THIS)
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
    | op= NOT expr #UnaryExpr
    | object=expr DOT name=(ID | LENGTH | MAIN) LPAREN
        (expr (COMMA expr)*)?
        RPAREN #MethodCall
    | expr LBRACKET expr RBRACKET #ListAccess
    | expr DOT LENGTH #LengthCall
    | expr op=(MUL | DIV) expr #BinaryExpr //
    | expr op=(ADD | SUB) expr #BinaryExpr //
    | expr op= LESS expr #BinaryExpr
    | expr op= LOGICAL_AND expr #BinaryExpr
    | NEW name=(ID | LENGTH | MAIN) LPAREN (expr (COMMA expr)*)? RPAREN #NewObject
    | NEW INT LBRACKET expr RBRACKET #NewArray
    | LBRACKET (expr (COMMA expr)*)? RBRACKET #Array
    | value=INTEGER #IntegerLiteral//
    | value=(TRUE | FALSE) #BooleanLiteral
    | 'this' #ThisLiteral
    | name=(ID | LENGTH | MAIN | THIS) #VarRefExpr //
    ;