###
# This Makefile can be used to make a parser for the Simple language
# (Yylex.class) and to make a program (P3.class) that tests the parser and
# the unparse methods in ast.java.
#
# The default makes both the parser and the test program.
#
# make clean removes all generated files.
#
###

P3.class: P3.java parser.class Yylex.class ASTnode.class IO.class
	javac -g P3.java

parser.java: simple.cup
	java java_cup.Main < simple.cup

parser.class: parser.java ASTnode.class Yylex.class Errors.class
	javac parser.java

Yylex.class: Yylex.java sym.class Errors.class
	javac Yylex.java

ASTnode.class: ast.java Sequence.class NoCurrentException.class
	javac -g ast.java

Yylex.java: simple.flex sym.class
	jflex simple.flex

sym.class: sym.java
	javac -g sym.java

sym.java: simple.cup
	java java_cup.Main < simple.cup

Errors.class: Errors.java
	javac Errors.java

Sequence.class: Sequence.java
	javac -g Sequence.java

NoCurrentException.class: NoCurrentException.java
	javac NoCurrentException.java

IO.class: IO.java
	javac -g IO.java

###
# clean
###
clean:
	rm -f *~ *.class parser.java Yylex.java

###
# submit
###

submit:
	zip submit.zip *.java test.sim Makefile simple.flex simple.cup test.sim  

###
# handout
###

handout:
	zip handout.zip test.sim Makefile simple.grammar ast.java Errors.java IO.java NoCurrentException.java P3.java Sequence.java simple.cup simple.grammar




