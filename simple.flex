import java_cup.runtime.*; // defines the Symbol class

// The generated scanner will return a Symbol for each token that it finds.
// A Symbol contains an Object field named value; that field will be of type
// TokenVal, defined below.
//
// A TokenVal object contains the line number on which the token occurs as
// well as the number of the character on that line that starts the token.
// Some tokens (e.g., literals) also include the value of the token.



class TokenVal {
 // fields
    int linenum;
    int charnum;
 // constructor
    TokenVal(int l, int c) {
        linenum = l;
	    charnum = c;
    }
}
// Integerliteral
class IntLitTokenVal extends TokenVal {
 // new field: the value of the integer literal
    int intVal;
 // constructor
    IntLitTokenVal(int l, int c, int val) {
        super(l,c);
	intVal = val;
    }
}
//ID Identifyer

class IdIdent extends TokenVal {
 // new field: the value of the integer literal
    String idVal;
 // constructor
    IdIdent(int l, int c, String val) {
        super(l,c);
		idVal = val;
    }
}

class StringIdent extends TokenVal {
	String strIdent;
	//
	StringIdent(int l, int c, String val) {
		super(l,c);
		strIdent = val;
	}
}

class string {
    public static StringBuilder stringLit = new StringBuilder();
}


// The following class is used to keep track of the character number at which
// the current token starts on its line.
class CharNum {
  static int num=1;
}

%%


LineTerminator 	= [\r|\n|\r\n]
InputCharacter 	= [^\r\n]
Digit			= [0-9]
WhiteSpace     	= [ \t\f]
Letters 		= [a-zA-Z_]
Special_Char    = [&!#]
Id 				= {Letters} ({Letters} | {Digit})*
Num				= {Digit}+

//Comment
Comment = {TraditionalComment} | {EndOfLineComment} | {DocumentationComment}

TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"

EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}?
DocumentationComment = "/**" {CommentContent} "*"+ "/"
CommentContent       = ( [^*] | \*+ [^/*] )*

// Fix less vs code bug */

//String
StringCharacter = [^\n\r\"\\]


// The next 3 lines are included so that we can use the generated scanner
// with java CUP (the Java parser generator)
%implements java_cup.runtime.Scanner
%function next_token
%type java_cup.runtime.Symbol

%state STRING

// Tell JLex what to do on end-of-file
%eofval{
return new Symbol(sym.EOF);
%eofval}


// Turn on line counting
%line
%%



<YYINITIAL> {
//keywords

"public" 	{
				Symbol S = new Symbol(sym.PUBLIC, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"static" 	{
				Symbol S = new Symbol(sym.STATIC, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}

"class" 	{
				Symbol S = new Symbol(sym.CLASS, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"return" 	{
				Symbol S = new Symbol(sym.RETURN, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"if"		{
				Symbol S = new Symbol(sym.IF, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}

"else" 		{
				Symbol S = new Symbol(sym.ELSE, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"do" 		{
				Symbol S = new Symbol(sym.DO, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"while" 	{
				Symbol S = new Symbol(sym.DO, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"int" 		{
				Symbol S = new Symbol(sym.INT, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"boolean" 		{
				Symbol S = new Symbol(sym.BOOLEAN, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"String" 	{
				Symbol S = new Symbol(sym.STRING, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"void" 	{
				Symbol S = new Symbol(sym.VOID, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"true" 	{
				Symbol S = new Symbol(sym.TRUE, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"false" 	{
				Symbol S = new Symbol(sym.FALSE, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}
"System.out.println" 	{
				Symbol S = new Symbol(sym.PRINT, new TokenVal(yyline+1, CharNum.num));
				CharNum.num+= yytext().length();
				return S;

			}



			//symbols
"{"	   {
			Symbol S = new Symbol(sym.LCURLY, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
	   }
"}"	   {
			Symbol S = new Symbol(sym.RCURLY, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
	   }

"("	   {
			Symbol S = new Symbol(sym.LPAREN, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
	   }
")"	   {
			Symbol S = new Symbol(sym.RPAREN, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
	   }
","	   {
			Symbol S = new Symbol(sym.COMMA, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
	   }

"!"	   {
			Symbol S = new Symbol(sym.NOT, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
	   }

";"		{
			Symbol S = new Symbol(sym.SEMICOLON, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
	   	}


 		//calc operators

"+"	   {
			Symbol S = new Symbol(sym.PLUS, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
	   }
	   
	   
"-"	   {
			Symbol S = new Symbol(sym.MINUS, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
	   }

"*"	   {
			Symbol S = new Symbol(sym.TIMES, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	   		return S;
	   }

"/"	  	{
			Symbol S = new Symbol(sym.DIVIDE, new TokenVal(yyline+1, CharNum.num));
	    	CharNum.num++;
	    	return S;
		}

// COMPARATIVE OPERATORS

"==" 	{
			Symbol S = new Symbol(sym.EQUALS, new TokenVal(yyline+1, CharNum.num));
			CharNum.num+= yytext().length();
			return S;
		
		}
	
"<=" 	{
			Symbol S = new Symbol(sym.LESSEQ, new TokenVal(yyline+1, CharNum.num));
			CharNum.num+= yytext().length();
			return S;
		
		}

">=" 	{
			Symbol S = new Symbol(sym.GREATEREQ, new TokenVal(yyline+1, CharNum.num));
			CharNum.num+= yytext().length();
			return S;
		
		}

"<" 	{
			Symbol S = new Symbol(sym.LESS, new TokenVal(yyline+1, CharNum.num));
			CharNum.num+= yytext().length();
			return S;
		
		}	

">" 	{
			Symbol S = new Symbol(sym.GREATER, new TokenVal(yyline+1, CharNum.num));
			CharNum.num+= yytext().length();
			return S;
		
		}

"!=" 	{
			Symbol S = new Symbol(sym.NOTEQUALS, new TokenVal(yyline+1, CharNum.num));
			CharNum.num+= yytext().length();
			return S;
		
		}

//logic operators
"&&" 	{
			Symbol S = new Symbol(sym.AND, new TokenVal(yyline+1, CharNum.num));
			CharNum.num+= yytext().length();
			return S;
		
		}

"||" 	{
			Symbol S = new Symbol(sym.OR, new TokenVal(yyline+1, CharNum.num));
			CharNum.num+= yytext().length();
			return S;
		
		}


//assing

"=" 	{	
			Symbol S = new Symbol(sym.ASSIGN, new TokenVal(yyline+1, CharNum.num));
			CharNum.num+= yytext().length();
			return S;
		}
//String

 \"                             { yybegin(STRING); string.stringLit.setLength(0); }

//Numbers

{Num}   {
	    try{
                     int val = Integer.parseInt(yytext(),10);
                     Symbol S = new Symbol(sym.INTLITERAL, new IntLitTokenVal(yyline+1, CharNum.num, val));
                     CharNum.num += yytext().length();
                     return S;
                   }
                   catch (NumberFormatException numx){
                     Errors.fatal(yyline+1, CharNum.num,
                     			 "Buffer Overflow");
                     	    CharNum.num+= yytext().length();

                   }
	   }




//Coments

{Comment}                      { /* ignore */ }

// WhiteSpace

{WhiteSpace}+               {CharNum.num += yytext().length();}
{LineTerminator}+           {CharNum.num = 1;}




}

<STRING> {
  \"                                {
                                        yybegin(YYINITIAL);
                                        Symbol S = new Symbol(sym.STRINGLITERAL, new StringIdent(yyline+1, CharNum.num, string.stringLit.toString()));
                                        CharNum.num += yytext().length();
                                        return S;
                                    }

  {StringCharacter}+             { string.stringLit.append( yytext() ); }

  /* escape sequences */
  \\b                          { string.stringLit.append('\b'); }
  \\t                          { string.stringLit.append('\t'); }
  \\n                          { string.stringLit.append("\n"); }
  \\f                          { string.stringLit.append('\f'); }
  \\r                          { string.stringLit.append('\r'); }
  \\\"                         { string.stringLit.append('\"'); }
  \\'                          { string.stringLit.append('\''); }
  \\\\                         { string.stringLit.append('\\'); }



  /* error cases */
  \\.                            { Errors.fatal(yyline+1, CharNum.num, "Illegal Escape"); CharNum.num+= yytext().length();  }
  {LineTerminator}               { Errors.fatal(yyline+1, CharNum.num, "Illegal  STring"); CharNum.num+=yytext().length();}
}

//Id

{Id} 	{	
			String val = yytext();
			Symbol S = new Symbol(sym.ID, new IdIdent(yyline+1, CharNum.num, val));
			CharNum.num += yytext().length();
			return S;
		}



//other


.	   {Errors.fatal(yyline+1, CharNum.num,
			 "ignoring illegal character: " + yytext());
	    CharNum.num++;
	   }

