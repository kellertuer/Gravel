header {
	package etc;}
class StringLexer extends Lexer;


WS      : ( ' ' | '\t' | '\n' { newline(); }
        | '\r' ) { $setType(Token.SKIP); }
        ;

LKLAMMER  : '(' ;
RKLAMMER  : ')' ;
MUL    	  : '*' ;
DIV 	  : '/' ;
EXP	      : '^' ;
PLUS      :  '+' ;
MINUS     : '-' ;
SEMI      : ';' ;

protected
ZIFFER   : '0'..'9' ;
NUM     : (ZIFFER)+ ('.' (ZIFFER)+ )? ;

class StringParser extends Parser;
options { buildAST = true; }

expr returns [double r=0] {double x;}
	: r=mexpr (PLUS x=mexpr {r+=x;} | MINUS x=mexpr {r-=x;})*
	;
				
mexpr returns [double r=0] {double x;}
	: r=dexpr (MUL x=dexpr {r *=x;} | DIV x=dexpr {r /=x;})*
	;
	
dexpr returns [double r=0] {double x;}
	: r=atom (EXP^ x=atom {r = Math.pow(r,x);})* 
	;

atom returns [double r=0]
	: i:NUM {r=(double)Double.parseDouble(i.getText());}
    | LKLAMMER! r=expr RKLAMMER!
  	;

/* class StringTreeWalker extends TreeParser;

expr returns [double r] { double a, b; r = 0; }
        : #(PLUS a=expr b=expr ) {r=a+b;}
        | #(MINUS a=expr b=expr ) {r=a-b;}
        | #(EXP a=expr b=expr ) {r=Math.pow(a,b);}
        | #(MUL a=expr b=expr ) {r=a*b;}
        | #(DIV a=expr b=expr ) {r=a/b;}
        | i:NUM {r=(double)Double.parseDouble(i.getText());}
        ; */