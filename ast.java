import java.io.*;
import java.util.ArrayList;
// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a "Simple" program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a sequence (for nodes that may have a variable number of children)
// or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         IdNode, ClassBodyNode
//     ClassBodyNode       DeclListNode
//     DeclListNode        sequence of DeclNode
//     FormalsListNode     sequence of FormalDeclNode
//     MethodBodyNode      DeclListNode, StmtListNode
//     StmtListNode        sequence of StmtNode
//     ExpListNode         sequence of ExpNode
//
//     DeclNode:
//       FieldDeclNode     TypeNode, IdNode
//       VarDeclNode       TypeNode, IdNode
//       MethodDeclNode    IdNode, FormalsListNode, MethodBodyNode
//       FormalDeclNode    TypeNode, IdNode
//
//     TypeNode:
//       IntNode             -- none --
//       BooleanNode         -- none --
//       StringNode          -- none --
//
//     StmtNode:
//       PrintStmtNode       ExpNode
//       AssignStmtNode      IdNode, ExpNode
//       IfStmtNode          ExpNode, StmtListNode
//       IfElseStmtNode      ExpNode, StmtListNode, StmtListNode
//       WhileStmtNode       ExpNode, StmtListNode
//       CallStmtNode        IdNode, ExpListNode
//       ReturnStmtNode      -- none --
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with sequences of kids, or internal
// nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode(X), BooleanNode(X), StringNode(X), IntLitNode(X),
//	  StrLitNode(X), TrueNode(X), FalseNode(X), IdNode(X), ReturnStmtNode(X)
//
// (2) Internal nodes with (possibly empty) sequences of children:
//        DeclListNode(-), FormalsListNode(-), StmtListNode(-), ExpListNode(-)
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode(-),    ClassBodyNode(-), MethodBodyNode(-),
//        FieldDeclNode(-),  VarDeclNode(-),   MethodDeclNode(-), FormalDeclNode(-),
//        PrintStmtNode(-),  AssignStmtNode(-),IfStmtNode(-),     IfElseStmtNode(-),
//        WhileStmtNode(-),  CallStmtNode(-),  UnaryExpNode(-),   BinaryExpNode(-),
//        UnaryMinusNode(-), NotNode(-),       PlusNode(-),       MinusNode(-),
//        TimesNode(-),      DivideNode(-),    AndNode(-),        OrNode(-),
//        EqualsNode(-),     NotEqualsNode(), LessNode(-),       GreaterNode(-),
//        LessEqNode(-),     GreaterEqNode(-)
//
// **********************************************************************

// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode { 
    // every subclass must provide an decompile operation
    abstract public void decompile(PrintWriter p, int indent);

    // this method can be used by the decompile methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
	for (int k=0; k<indent; k++) p.print(" ");
    }

    final int tab = 4;
}

// **********************************************************************
// ProgramNode, ClassBodyNode, DeclListNode, FormalsListNode,
// MethodBodyNode, StmtListNode, ExpListNode
// **********************************************************************
class ProgramNode extends ASTnode {
    public ProgramNode(IdNode id, ClassBodyNode classBody) {
	myId = id;
	myClassBody = classBody;
    }

    public void decompile(PrintWriter p, int indent) {
	p.print("public class ");
	myId.decompile(p, 0);
	p.println("{");
	myClassBody.decompile(p,tab);
	p.println("}");
    }

    public void checkName(){
        SymbolTable st = new SymbolTable();
        myId.checkInit(st, Types.ClassType, Types.ClassType,null);

        tp = new TablePrinter(false);
        tp.addToMap(st,0);
        myClassBody.checkName(st,tp);
        SymbolTable.Sym ms = st.lookup("main", 0, 0);
        if(!(ms != null && ms.getMyType() == Types.MethodType)){
            Errors.fatal(0, 0, "No main method declared");
        }
        tp.printMap();
        //System.out.println(st.toString(0));
    }

    public void checkType()
    {
        myClassBody.checkType();
    }
    // 2 kids
    private IdNode myId;
    private ClassBodyNode myClassBody;
    private TablePrinter tp;
}

class ClassBodyNode extends ASTnode {
    public ClassBodyNode(DeclListNode declList) {
	myDeclList = declList;
    }

    public void decompile(PrintWriter p, int indent) {
	myDeclList.decompile(p, indent);
    }

    public void checkName(SymbolTable st, TablePrinter tp){

        myDeclList.checkName(st,tp );
    }
    public void checkType()
    {
        myDeclList.checkType();
    }
    // 1 kid
    private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
    public DeclListNode(Sequence S) {
	myDecls = S;
    }

    public void decompile(PrintWriter p, int indent) {
	try {
	    for (myDecls.start(); myDecls.isCurrent(); myDecls.advance()) {
		((DeclNode)myDecls.getCurrent()).decompile(p, indent);
	    }
	} catch (NoCurrentException ex) {
	    System.err.println("unexpected NoCurrentException in DeclListNode.print");
	    System.exit(-1);
	}
    }

    public void checkName(SymbolTable st, TablePrinter tp){
        try {
            for (myDecls.start(); myDecls.isCurrent(); myDecls.advance()) {
                ((DeclNode)myDecls.getCurrent()).checkName(st,tp);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in DeclListNode.print");
            System.exit(-1);
        }
    }

    public void checkType()
    {
        try {
            for (myDecls.start(); myDecls.isCurrent(); myDecls.advance()) {
                ((DeclNode)myDecls.getCurrent()).checkType();
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in DeclListNode.print");
            System.exit(-1);
        }
    }
  // sequence of kids (DeclNodes)
  private Sequence myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(Sequence S) {
	myFormals = S;
    }

    public void decompile(PrintWriter p, int indent) {
        try {
            for (myFormals.start(); myFormals.isCurrent();) { // myFormals.advance()
                ((FormalDeclNode)myFormals.getCurrent()).decompile(p, indent);
                //p.print(", ");
                myFormals.advance();
                if(myFormals.isCurrent()){
                    p.print(", ");
                }
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.print");
            System.exit(-1);
        }
    }
    public void checkName(SymbolTable st, TablePrinter tp)
    {
        //myParams = new ArrayList<Integer>();
        try {
            for (myFormals.start(); myFormals.isCurrent(); myFormals.advance()) { // myFormals.advance()
                ((FormalDeclNode)myFormals.getCurrent()).checkName(st,tp);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.print");
            System.exit(-1);
        }
    }

    public ArrayList<Integer> getMethodParams()
    {
        myParams = new ArrayList<Integer>();
        try {
            for (myFormals.start(); myFormals.isCurrent(); myFormals.advance()) {
                myParams.add(((FormalDeclNode)myFormals.getCurrent()).getParamType());
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.print");
            System.exit(-1);
        }
        return myParams;
    }
  // sequence of kids (FormalDeclNodes)
    private Sequence myFormals;
    private ArrayList<Integer> myParams;
}

class MethodBodyNode extends ASTnode {
    public MethodBodyNode(VarDeclListNode varDeclList, StmtListNode stmtList) {
	myDeclList = varDeclList;
	myStmtList = stmtList;
    }

    public void decompile(PrintWriter p, int indent) {
        //doIndent(p, indent);
        myDeclList.decompile(p,indent);
        //doIndent(p, indent);
        myStmtList.decompile(p,indent);
        //p.println("baum");
    }

    public void checkName(SymbolTable st, TablePrinter tp){
        myDeclList.checkName(st,tp);
        myStmtList.checkName(st,tp);
    }

    public int checkType(int expected){

        return myStmtList.checkType(expected);
    }
    public void checkReturn(){

    }
    // 2 kids
    private VarDeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
    public StmtListNode(Sequence S) {
	myStmts = S;
    }

    public void decompile(PrintWriter p, int indent) {
        try {
            for (myStmts.start(); myStmts.isCurrent(); myStmts.advance()) {
                ((StmtNode)myStmts.getCurrent()).decompile(p, indent);
                p.print("\n");
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in StmtListNode.decompile");
            System.exit(-1);
        }
    }

    public void checkName(SymbolTable st,TablePrinter tp){
        try {
            for (myStmts.start(); myStmts.isCurrent(); myStmts.advance()) {
                if(myStmts.getCurrent() instanceof BracketStmtNode)
                {
                    ((BracketStmtNode) myStmts.getCurrent()).checkName(st,tp);
                }
                else if(myStmts.getCurrent() instanceof IfElseStmtNode)
                {
                    ((IfElseStmtNode) myStmts.getCurrent()).checkName(st,tp);
                }
                else if(myStmts.getCurrent() instanceof IfStmtNode)
                {
                    ((IfStmtNode) myStmts.getCurrent()).checkName(st,tp);
                }
                else if(myStmts.getCurrent() instanceof WhileStmtNode)
                {
                    ((WhileStmtNode) myStmts.getCurrent()).checkName(st,tp);
                }
                else {
                    ((StmtNode) myStmts.getCurrent()).checkName(st);
                }
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in StmtListNode.checkName");
            System.exit(-1);
        }
    }

    public int checkType(int expected)
    {
        int returnType;
        int returnVal = Types.ErrorType;

        if(expected == Types.MethodType)
        {
            returnVal = expected;
        }
        boolean myReturn = false;
        try {
            for (myStmts.start(); myStmts.isCurrent(); myStmts.advance()){
                if(myStmts.getCurrent() instanceof ReturnStmtNode)
                {
                    myReturn = true;
                    returnType= ((ReturnStmtNode) myStmts.getCurrent()).checkType(expected);
                    if(returnType == Types.ErrorType)
                    {
                        returnVal = Types.ErrorType;
                    }
                    else
                    {
                        returnVal = returnType;
                    }
                }
            }

            for (myStmts.start(); myStmts.isCurrent(); myStmts.advance()) {
                if(myStmts.getCurrent() instanceof IfElseStmtNode)
                {
                    returnType= ((IfElseStmtNode) myStmts.getCurrent()).checkType(expected);
                    if(returnType == Types.ErrorType && !myReturn)
                    {
                        returnVal = Types.ErrorType;
                    }
                    else if(returnType !=Types.ErrorType)
                    {
                        returnVal = returnType;
                    }
                }
                /*
                else if(myStmts.getCurrent() instanceof IfStmtNode)
                {
                    returnType= ((IfStmtNode) myStmts.getCurrent()).checkType(expected);
                    if(returnType == Types.ErrorType && !myReturn)
                    {
                        returnVal = Types.ErrorType;
                    }
                    else if(returnType !=Types.ErrorType)
                    {
                        returnVal = returnType;
                    }
                }
                */

                else if(myStmts.getCurrent() instanceof WhileStmtNode)
                {
                    returnType= ((WhileStmtNode) myStmts.getCurrent()).checkType(expected);
                    if(returnType == Types.ErrorType && !myReturn)
                    {
                        returnVal = Types.ErrorType;
                    }
                    else if(returnType !=Types.ErrorType)
                    {
                        returnVal = returnType;
                    }
                }
                else if(myStmts.getCurrent() instanceof BracketStmtNode)
                {
                    returnType= ((BracketStmtNode) myStmts.getCurrent()).checkType(expected);
                    if(returnType == Types.ErrorType && !myReturn)
                    {
                        returnVal = Types.ErrorType;
                    }
                    else if(returnType !=Types.ErrorType)
                    {
                        returnVal = returnType;
                    }
                }
                else if(myStmts.getCurrent() instanceof ReturnStmtNode)
                {

                }
                else {
                    ((StmtNode) myStmts.getCurrent()).checkType(expected);
                }

            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in StmtListNode.checkName");
            System.exit(-1);
        }
        return returnVal;
    }


    // sequence of kids (StmtNodes)
    private Sequence myStmts;
}

class VarDeclListNode extends ASTnode {
    public VarDeclListNode(Sequence S) {
        myVarDecl = S;
    }

    public void decompile(PrintWriter p, int indent) {
        try {
            for (myVarDecl.start(); myVarDecl.isCurrent(); myVarDecl.advance()) {
                ((VarDeclNode) myVarDecl.getCurrent()).decompile(p, indent);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in VarDeclListNode.print");
            System.exit(-1);
        }
    }

    public void checkName(SymbolTable st, TablePrinter tp){
        try {
            for (myVarDecl.start(); myVarDecl.isCurrent(); myVarDecl.advance()) {
                ((VarDeclNode) myVarDecl.getCurrent()).checkName(st,tp);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in VarDeclListNode.print");
            System.exit(-1);
        }
    }
    private Sequence myVarDecl;
}

class ExpListNode extends ASTnode {
    public ExpListNode(Sequence S) {
	myExps = S;
    }
    public void decompile(PrintWriter p, int indent) {
        try {
            for (myExps.start(); myExps.isCurrent(); ){//myExps.advance()) {
                ((ExpNode)myExps.getCurrent()).decompile(p, indent);
                //p.print("\n");
                myExps.advance();
                if(myExps.isCurrent()){
                    p.print(", ");
                }
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.print");
            System.exit(-1);
        }
    }
    public void checkName(SymbolTable st)
    {
        try {
            for (myExps.start(); myExps.isCurrent();myExps.advance()){
                ((ExpNode)myExps.getCurrent()).checkName(st);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.print");
            System.exit(-1);
        }
    }


    public boolean checkType(ArrayList<Integer> params)
    {
        ArrayList<Integer> runList = new ArrayList<>();
        boolean returnVal = true;
        if(myExps.length() == params.size())
        {
            try {
                int i = 0;
                for (myExps.start(); myExps.isCurrent(); myExps.advance()) {
                    int checkVal= ((ExpNode) myExps.getCurrent()).checkType(params.get(i));
                    runList.add(checkVal);
                    i++;
                }
                returnVal = runList.equals(params);
            } catch (NoCurrentException ex) {
                System.err.println("unexpected NoCurrentException in FormalsListNode.print");
                System.exit(-1);
            }
        }
        else
        {
            returnVal = false;
        }
        return returnVal;
    }
    public boolean callExpCheckType(ArrayList<Integer> params)
    {
        ArrayList<Integer> runList = new ArrayList<>();
        boolean returnVal = true;
        if(myExps.length() == params.size())
        {
            try {
                int i = 0;
                for (myExps.start(); myExps.isCurrent(); myExps.advance()) {
                    int checkVal= ((ExpNode) myExps.getCurrent()).callExpCheckType(params.get(i));
                    runList.add(checkVal);
                    i++;
                }
                returnVal = runList.equals(params);
            } catch (NoCurrentException ex) {
                System.err.println("unexpected NoCurrentException in FormalsListNode.print");
                System.exit(-1);
            }
        }
        else
        {
            returnVal = false;
        }
        return returnVal;
    }

    // sequence of kids (ExpNodes)
    private Sequence myExps;
    private ArrayList<Integer> myParams = new ArrayList<>();
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************
abstract class DeclNode extends ASTnode
{
    public abstract void checkName(SymbolTable st, TablePrinter tp);
    public abstract void checkType();
}

class FieldDeclNode extends DeclNode {
    public FieldDeclNode(TypeNode type, IdNode id) {
	myType = type;
	myId = id;
    }
    public void decompile(PrintWriter p, int indent) {
	doIndent(p, indent);
	p.print("static ");
	myType.decompile(p, indent);
	p.print(" ");
	myId.decompile(p, indent);
	p.println(";");
    }
    public void checkName(SymbolTable st, TablePrinter tp){
        myId.checkInit(st,myType.getType(),myType.getType(),null);
    }

    public void checkType(){

    }
    // 2 kids
    private TypeNode myType;
    private IdNode myId;
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id) {
	myType = type;
	myId = id;
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p,indent);
        myType.decompile(p, indent);
        p.print(" ");
        myId.decompile(p,indent);
        p.print(";\n");
    }

    // 2 kids
    private TypeNode myType;
    private IdNode myId;
    public void checkName(SymbolTable st, TablePrinter tp){
        myId.checkInit(st,myType.getType(),myType.getType(),null);
    }
    public void checkType(){

    }
}

abstract class MethodDeclNode extends DeclNode{
}

class MethodDeclVoidNode extends MethodDeclNode {
    public MethodDeclVoidNode(IdNode id, FormalsNode formals,
			  MethodBodyNode body) {
	myId = id;
	myFormals = formals;
	myBody = body;
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("public static void ");
        myId.decompile(p,indent);
        myFormals.decompile(p,indent);
        p.print("{\n");
        myBody.decompile(p,indent + tab);
        doIndent(p, indent);
        p.print("}\n");
    }
    public void checkName(SymbolTable st, TablePrinter tp){
        SymbolTable newSt = new SymbolTable();
        newSt.setMyParent(st);
        tp.addToMap(newSt,1);
        myFormals.checkName(newSt,tp);
        myParams = myFormals.getMethodParams();

        myId.checkInit(st,myType,myRetType,myParams);
        myBody.checkName(newSt,tp);

        //System.out.println(newSt.toString(1));
    }
    public void checkType(){
        if(myBody.checkType(myRetType)==Types.ErrorType)
        {
            Errors.fatal(myId.getMyLineNum(),myId.getMyCharNum(),"Missing return statement");
        }
    }
    // 3 kids
    private IdNode myId;
    private FormalsNode myFormals;
    private MethodBodyNode myBody;
    private int myType = Types.MethodType;
    private int myRetType = Types.MethodType;
    private ArrayList<Integer> myParams;
}

class MethodDeclIntNode extends MethodDeclNode {
    public MethodDeclIntNode(IdNode id, FormalsNode formals,
                              MethodBodyNode body) {
        myId = id;
        myFormals = formals;
        myBody = body;
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("public static int ");
        myId.decompile(p,indent);
        myFormals.decompile(p,indent);
        p.print("{\n");
        myBody.decompile(p,indent + tab);
        doIndent(p,indent);
        p.print("}\n");
    }
    public void checkName(SymbolTable st, TablePrinter tp){

        SymbolTable newSt = new SymbolTable();
        newSt.setMyParent(st);
        tp.addToMap(newSt,1);
        myFormals.checkName(newSt,tp);
        myParams = myFormals.getMethodParams();
        myId.checkInit(st,myType,myRetType,myParams);
        myBody.checkName(newSt,tp);

    }
    public void checkType(){
        if(myBody.checkType(myRetType)==Types.ErrorType)
        {
            Errors.fatal(myId.getMyLineNum(),myId.getMyCharNum(),"Missing return statement");
        }

    }
     // 3 kids
    private IdNode myId;
    private FormalsNode myFormals;
    private MethodBodyNode myBody;
    private int myType = Types.MethodType;
    private int myRetType = Types.IntType;
    private ArrayList<Integer> myParams;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
	myType = type;
	myId = id;
    }

    public void decompile(PrintWriter p, int indent) {
        myType.decompile(p,indent);
        p.print(" ");
        myId.decompile(p,indent);
    }
    public void checkName(SymbolTable st, TablePrinter tp){
        myId.checkInit(st, myType.getType(), myType.getType(), null);
    }

    public int getParamType()
    {
        return myType.getType();
    }
    public void checkType(){

    }
    private TypeNode myType;
    private IdNode myId;
}

class FormalsNode extends DeclNode {
    public FormalsNode( FormalsListNode formalList) {
        myList = formalList;
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myList.decompile(p,indent);
        p.print(")");

    }
    public void checkName(SymbolTable st, TablePrinter tp){
        myList.checkName(st,tp);
    }
    public ArrayList<Integer> getMethodParams()
    {
        return myList.getMethodParams();
    }
    public void checkType(){

    }
    // 2 kids
    private FormalsListNode myList;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************
abstract class TypeNode extends ASTnode {
    public abstract int getType();
}

class IntNode extends TypeNode
{
    public IntNode() {
    }

    public void decompile(PrintWriter p, int indent) {
	p.print("int");
    }

    @Override
    public int getType() {
        return Types.IntType;
    }
}

class BooleanNode extends TypeNode
{
    public BooleanNode() {
    }

    public void decompile(PrintWriter p, int indent)  {
        p.print("boolean");
    }{
    }
    @Override
    public int getType() {
        return Types.BoolType;
    }
}

class StringNode extends TypeNode
{
    public StringNode() {
    }

    public void decompile(PrintWriter p, int indent)  {
        p.print("String");
    }

    @Override
    public int getType() {
        return Types.StringType;
    }

}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    public abstract void checkName(SymbolTable st);
    public abstract int checkType(int expected);
}

class PrintStmtNode extends StmtNode {
    public PrintStmtNode(ExpNode exp) {
	myExp = exp;
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p,indent);
        p.print("System.out.println(");
        myExp.decompile(p,indent);;
        p.print(");");
    }
    public void checkName(SymbolTable st){
        myExp.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        return Types.MethodType;
    }

    // 1 kid
    private ExpNode myExp;
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(IdNode id, ExpNode exp) {
	myId = id;
	myExp = exp;
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p,indent);
        myId.decompile(p,indent);
        p.print(" = ");
        myExp.decompile(p,indent);
        p.print(";");
    }
    public void checkName(SymbolTable st){
        myId.checkName(st);
        myExp.checkName(st);
    }

    @Override
    public int checkType(int expected) {

        return myExp.checkType(myId.getMyRetTyp());
    }
    // 2 kids
    private IdNode myId;
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, StmtListNode slist) {
	myExp = exp;
	myStmtList = slist;
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p,indent);
        p.print("if(");
        myExp.decompile(p,indent);
        p.print("){\n");
        myStmtList.decompile(p,indent + tab);
        doIndent(p,indent);
        p.print("}");
    }

    public void checkName(SymbolTable st)
    {
        System.out.println("Wrong method");
    }
    public void checkName(SymbolTable st,TablePrinter tp){

        myExp.checkName(st);
        myStmtList.checkName(st,tp);
    }

    @Override
    public int checkType(int expected) {
        return myStmtList.checkType(expected);
    }
    // 2 kids
    private ExpNode myExp;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, StmtListNode slist1,
			  StmtListNode slist2) {
	myExp = exp;
	myThenStmtList = slist1;
	myElseStmtList = slist2;
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p,indent);
        p.print("if(");
        myExp.decompile(p,indent);
        p.print("){\n");
        myThenStmtList.decompile(p,indent + tab);
        doIndent(p,indent);
        p.print("}\n");
        doIndent(p,indent);
        p.print("else{\n");
        myElseStmtList.decompile(p,indent + tab);
        doIndent(p,indent);
        p.print("}");
    }
    public void checkName(SymbolTable st)
    {
        System.out.println("Wrong method");
    }
    public void checkName(SymbolTable st,TablePrinter tp){

        myExp.checkName(st);
        myThenStmtList.checkName(st,tp);
        myElseStmtList.checkName(st,tp);
    }
    @Override
    public int checkType(int expected) {
        int typeThen = myThenStmtList.checkType(expected);
        int typeElse = myElseStmtList.checkType(expected);
        if(typeThen == Types.ErrorType||typeElse == Types.ErrorType)
        {
            return Types.ErrorType;
        }
        return expected;
    }

    // 3 kids
    private ExpNode myExp;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, StmtListNode slist) {
	myExp = exp;
	myStmtList = slist;
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p,indent);
        p.print("do{\n");
        myStmtList.decompile(p,indent + tab);
        doIndent(p,indent);
        p.print("}while(");
        myExp.decompile(p,indent);
        p.print(")");
    }

    public void checkName(SymbolTable st)
    {
        System.out.println("Wrong method");
    }

    public void checkName(SymbolTable st,TablePrinter tp){

        myExp.checkName(st);
        myStmtList.checkName(st,tp);
    }

    @Override
    public int checkType(int expected) {
        return myStmtList.checkType(expected);
    }

    private ExpNode myExp;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(IdNode id, ExpListNode elist) {
	myId = id;
	myExpList = elist;
    }

    public CallStmtNode(IdNode id) {
	myId = id;
	myExpList = new ExpListNode(new Sequence());
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p,indent);
        myId.decompile(p,indent);
        p.print("(");
        myExpList.decompile(p,indent);
        p.print(");");
    }

    public void checkName(SymbolTable st){
        myId.checkName(st);
        myParams = myId.getParams();
        myExpList.checkName(st);
    }

    @Override
    public int checkType(int expected) {

        return myId.checkReturn(expected);
    }

    private IdNode myId;
    private ExpListNode myExpList;
    private ArrayList<Integer> myParams;
    private SymbolTable.Sym myRef;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode( int lineNum,int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    ReturnStmtNode(ExpNode exp, int lineNum,int charNum){
        myExp = exp;
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.decompile(p, indent);
        }
        p.print(";");
    }
    public void checkName(SymbolTable st){
        if(myExp!=null) {
            myExp.checkName(st);
        }
    }


    @Override
    public int checkType(int expected) {
        int returnVal = expected;
        int expType= 0;
        if(myExp == null)
        {
            if(expected != Types.MethodType) {
                String expString = Types.ToString(expected);
                String provString = "void";
                Errors.fatal(myLineNum,myCharNum,"Returntype mismatch--expected: "+ expString + " provided: "+ provString);
            }
        }
        else if(expected != (expType=myExp.returnStmtCheckType(expected)))
        {
            String expString = Types.ToString(expected);
            String provString = Types.ToString(expType);
            if(expected == Types.MethodType)
            {
                expString = "void";
            }
            if(expType == Types.MethodType)
            {
                provString = "void";
            }

            Errors.fatal(myLineNum,myCharNum,"Returntype mismatch--expected: "+ expString + " provided: "+ provString);
        }
        return returnVal;
    }


    private ExpNode myExp;
    private int myLineNum;
    private int myCharNum;
}

class BracketStmtNode extends StmtNode{
    public BracketStmtNode(VarDeclListNode vdl, StmtListNode sl){
        myVarDeclList = vdl;
        myStmtList = sl;
    }

    public void decompile(PrintWriter p, int indent){
        doIndent(p,indent);
        p.print("{\n");
        myVarDeclList.decompile(p,indent + tab);
        myStmtList.decompile(p, indent + tab);
        doIndent(p,indent);
        p.print("}");
    }
    public void checkName(SymbolTable st){
        //myVarDeclList.checkName(st,tp);
        //myStmtList.checkName(st);
        System.out.println("Wrong function");
    }

    public void checkName(SymbolTable st, TablePrinter tp){
        SymbolTable newSt = new SymbolTable();
        newSt.setMyParent(st);
        tp.addToMap(newSt,2);
        myVarDeclList.checkName(newSt,tp);
        myStmtList.checkName(newSt,tp);
    }

    @Override
    public int checkType(int expected) {
        return myStmtList.checkType(expected);
    }

    private VarDeclListNode myVarDeclList;
    private StmtListNode myStmtList;
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    public abstract void checkName(SymbolTable st);
    public abstract int checkType(int expected);
    public int returnStmtCheckType(int expected){
        return checkType(expected);
    }
    public int callExpCheckType(int expected){
        return checkType(expected);
    }
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int colNum, int intVal) {
	myLineNum = lineNum;
	myColNum = colNum;
	myIntVal = intVal;
    }

    public void decompile(PrintWriter p, int indent) {p.print(myIntVal);
    }

    public void checkName(SymbolTable st)
    {

    }

    @Override
    public int checkType(int expected) {
        if(expected != Types.ErrorType){
            if(expected != Types.IntType) {
                String expString = Types.ToString(expected);
                if(expected == Types.MethodType)
                {
                    expString = "void";
                }
                Errors.fatal(myLineNum, myColNum, "Type mismatch--expected: " + expString + " provided: " + Types.ToString(Types.IntType));
            }
        }

        return Types.IntType;
    }
    @Override
    public int returnStmtCheckType(int expected){
        if(expected != Types.ErrorType){
            if(expected != Types.IntType) {
                String expString = Types.ToString(expected);
                if(expected == Types.MethodType)
                {
                    expString = "void";
                }
                //Errors.fatal(myLineNum, myColNum, "Returntype mismatch--expected: " + expString + " provided: " + Types.ToString(Types.IntType));
            }
        }

        return Types.IntType;
    }

    @Override
    public int callExpCheckType(int expected) {
        if(expected != Types.ErrorType){
            if(expected != Types.IntType) {
                String expString = Types.ToString(expected);
                if(expected == Types.MethodType)
                {
                    expString = "void";
                }
                Errors.fatal(myLineNum, myColNum, "Parameter type mismatch--expected: " + expString + " provided: " + Types.ToString(Types.IntType));
            }
        }

        return Types.IntType;
    }

    private int myLineNum;
    private int myColNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int colNum, String strVal) {
	myLineNum = lineNum;
	myColNum = colNum;
	myStrVal = strVal;
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("\"" + myStrVal + "\"");
    }
    public void checkName(SymbolTable st)
    {

    }

    @Override
    public int checkType(int expected) {
        if(expected != Types.ErrorType){
            if(expected != Types.StringType) {
                String expString = Types.ToString(expected);
                if(expected == Types.MethodType)
                {
                    expString = "void";
                }
                Errors.fatal(myLineNum, myColNum, "Type mismatch--expected: " + expString + " provided: " + Types.ToString(Types.StringType));
            }
        }
        return Types.StringType;
    }

    @Override
    public int returnStmtCheckType(int expected)
    {
        if(expected != Types.ErrorType){
            if(expected != Types.StringType) {
                String expString = Types.ToString(expected);
                if(expected == Types.MethodType)
                {
                    expString = "void";
                }
                //Errors.fatal(myLineNum, myColNum, "Returntype mismatch--expected: " + expString + " provided: " + Types.ToString(Types.StringType));
            }
        }
        return Types.StringType;
    }

    @Override
    public int callExpCheckType(int expected) {
        if(expected != Types.ErrorType){
            if(expected != Types.StringType) {
                String expString = Types.ToString(expected);
                if(expected == Types.MethodType)
                {
                    expString = "void";
                }
                Errors.fatal(myLineNum, myColNum, "Parameter type mismatch--expected: " + expString + " provided: " + Types.ToString(Types.StringType));
            }
        }
        return Types.StringType;
    }

    private int myLineNum;
    private int myColNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int colNum) {
	myLineNum = lineNum;
	myColNum = colNum;
    }

    public void decompile(PrintWriter p, int indent) {p.print("true");
    }
    public void checkName(SymbolTable st)
    {

    }

    @Override
    public int checkType(int expected) {

        if(expected != Types.ErrorType) {
            if (expected != Types.BoolType) {
                String expString = Types.ToString(expected);
                if (expected == Types.MethodType) {
                    expString = "void";
                }
                Errors.fatal(myLineNum, myColNum, "Type mismatch--expected: " + expString + " provided: " + Types.ToString(Types.BoolType));
            }
        }
        return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected)
    {
        if(expected != Types.ErrorType) {
            if (expected != Types.BoolType) {
                String expString = Types.ToString(expected);
                if (expected == Types.MethodType) {
                    expString = "void";
                }
                //Errors.fatal(myLineNum, myColNum, "Returntype mismatch--expected: " + expString + " provided: " + Types.ToString(Types.BoolType));
            }
        }
        return Types.BoolType;
    }

    @Override
    public int callExpCheckType(int expected) {
        if(expected != Types.ErrorType) {
            if (expected != Types.BoolType) {
                String expString = Types.ToString(expected);
                if (expected == Types.MethodType) {
                    expString = "void";
                }
                Errors.fatal(myLineNum, myColNum, "Parameter type mismatch--expected: " + expString + " provided: " + Types.ToString(Types.BoolType));
            }
        }
        return Types.BoolType;
    }

    private int myLineNum;
    private int myColNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int colNum) {
	myLineNum = lineNum;
	myColNum = colNum;
    }

    public void decompile(PrintWriter p, int indent) {p.print("false");
    }
    public void checkName(SymbolTable st)
    {

    }

    @Override
    public int checkType(int expected) {
        if(expected != Types.ErrorType){
            if(expected != Types.BoolType) {
                String expString = Types.ToString(expected);
                if(expected == Types.MethodType)
                {
                    expString = "void";
                }
                Errors.fatal(myLineNum, myColNum, "Type mismatch--expected: " + expString + " provided: " + Types.ToString(Types.BoolType));
            }
        }
        return Types.BoolType;
    }


    @Override
    public int returnStmtCheckType(int expected)
    {
        if(expected != Types.ErrorType){
            if(expected != Types.BoolType) {
                String expString = Types.ToString(expected);
                if(expected == Types.MethodType)
                {
                    expString = "void";
                }
                //Errors.fatal(myLineNum, myColNum, "Returntype mismatch--expected: " + expString + " provided: " + Types.ToString(Types.BoolType));
            }
        }
        return Types.BoolType;
    }

    @Override
    public int callExpCheckType(int expected) {
        if(expected != Types.ErrorType) {
            if (expected != Types.BoolType) {
                String expString = Types.ToString(expected);
                if (expected == Types.MethodType) {
                    expString = "void";
                }
                Errors.fatal(myLineNum, myColNum, "Parameter type mismatch--expected: " + expString + " provided: " + Types.ToString(Types.BoolType));
            }
        }
        return Types.BoolType;
    }
    private int myLineNum;
    private int myColNum;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode id, ExpListNode elist) {
        myId = id;
        myExpList = elist;
    }

    public CallExpNode(IdNode id) {
        myId = id;
        myExpList = new ExpListNode(new Sequence());
    }

    public void decompile(PrintWriter p, int indent) {
        myId.decompile(p,indent);
        p.print("(");
        myExpList.decompile(p,indent);
        p.print(");");
    }
    public void checkName(SymbolTable st)
    {
        myId.checkName(st);
        myExpList.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        if(expected != Types.ErrorType) {
            myExpList.callExpCheckType(myId.getParams());
        }
        return myId.checkType(expected);
    }

    @Override
    public int returnStmtCheckType(int expected) {
        if(expected != Types.ErrorType) {
            if (!myExpList.checkType(myId.getParams())) {
                Errors.fatal(myId.getMyLineNum(), myId.getMyCharNum(), "Parameter type mismatch");
            }
        }
        return myId.returnStmtCheckType(expected);
    }
    // 2 kids
    private IdNode myId;
    private ExpListNode myExpList;
}

class BracketsNode extends ExpNode
{
    public BracketsNode(ExpNode exp) {
       myExp = exp;
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp.decompile(p,indent);
        p.print(")");
    }

    public void checkName(SymbolTable st)
    {
        if(myExp!=null)
        {
            myExp.checkName(st);
        }
    }
    @Override
    public int checkType(int expected) {
        return myExp.checkType(expected);
    }

    @Override
    public int returnStmtCheckType(int expected){
        return myExp.returnStmtCheckType(expected);
    }
    private ExpNode myExp;
}

class IdNode extends ExpNode
{
    public IdNode(int lineNum, int charNum, String strVal) {
	myLineNum = lineNum;
	myCharNum = charNum;
	myStrVal = strVal;
    }

    public void decompile(PrintWriter p, int indent) {
	p.print(myStrVal);
    }
    public void checkInit(SymbolTable st,int type, int retType, ArrayList<Integer> params){
        myRef=  st.insert(myStrVal,myLineNum,myCharNum);
        if(myRef != null ){
            myRef.setType(type);
            myRef.setRetType(retType);
            myRef.setParams(params);
        }
        myType = type;
        myRetType = retType;
        myParams = params;
    }
    public void checkName(SymbolTable st)
    {
        myRef= st.lookup(myStrVal,myLineNum,myCharNum);
        if(myRef != null)
        {
            myType=myRef.getMyType();
            myRetType=myRef.getMyReturnType();
            myParams=myRef.getMyParams();
        }
    }

    @Override
    public int checkType(int expected) {
        if(expected != Types.ErrorType) {
            if (expected != myRetType) {
                String expString = Types.ToString(expected);
                Errors.fatal(myLineNum, myCharNum, "Type mismatch--expected: " + expString + " provided: " + Types.ToString(myRetType));
                return Types.ErrorType;
            }
        }
        return myRetType;
    }

    @Override
    public int returnStmtCheckType(int expected){
        if(expected != Types.ErrorType) {
            if (expected != myRetType) {
                String expString = Types.ToString(expected);
                //Errors.fatal(myLineNum, myCharNum, "Returntype mismatch--expected: " + expString + " provided: " + Types.ToString(myRetType));
                return Types.ErrorType;
            }
        }
        return myRetType;
    }


    @Override
    public int callExpCheckType(int expected) {
        if(expected != Types.ErrorType) {
            if (expected != myRetType) {
                String expString = Types.ToString(expected);
                Errors.fatal(myLineNum, myCharNum, "Parameter type mismatch--expected: " + expString + " provided: " + Types.ToString(myRetType));
                return Types.ErrorType;
            }
        }
        return myRetType;
    }

    public int getMyRetTyp(){
        return myRetType;
    }
    public ArrayList<Integer> getParams()
    {
        return myParams;
    }


    public int getMyLineNum(){
        return myLineNum;
    }

    public int getMyCharNum(){
        return myCharNum;
    }

    public int checkReturn(int expected)
    {
        if(expected == myRetType)
        {
            return myRetType;
        }
        return Types.ErrorType;
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private int myType = Types.ErrorType;
    private int myRetType = Types.ErrorType;
    private SymbolTable.Sym myRef;
    private ArrayList<Integer> myParams = new ArrayList<>();

}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
	myExp = exp;
    }
    public void checkName(SymbolTable st)
    {

    }



    // one child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode
{
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
	myExp1 = exp1;
	myExp2 = exp2;
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    // two kids
    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode
{
    public UnaryMinusNode(ExpNode exp) {
	super(exp);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        p.print("-");
        myExp.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp.checkName(st);
    }
    @Override
    public int checkType(int expected) {
        myExp.checkType(Types.IntType);
        return Types.IntType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        return checkType(expected);
    }
}

class NotNode extends UnaryExpNode
{
    public NotNode(ExpNode exp) {
	super(exp);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        p.print("!");
        myExp.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp.checkName(st);
    }
    @Override
    public int checkType(int expected) {
        myExp.checkType(Types.BoolType);
        return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.BoolType);
        return Types.BoolType;
    }

}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode
{
    public PlusNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("+");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.IntType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.IntType);
        return Types.IntType;
    }
}

class MinusNode extends BinaryExpNode
{
    public MinusNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("-");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }
    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.IntType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.IntType);
        return Types.IntType;
    }

}

class TimesNode extends BinaryExpNode
{
    public TimesNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("*");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.IntType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.IntType);
        return Types.IntType;
    }
}

class PowerNode extends BinaryExpNode
{
    public PowerNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("**");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.IntType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.IntType);
        return Types.IntType;
    }
}

class DivideNode extends BinaryExpNode
{
    public DivideNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("/");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.IntType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.IntType);
        return Types.IntType;
    }
}

class AndNode extends BinaryExpNode
{
    public AndNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("&&");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.BoolType);
        myExp2.checkType(Types.BoolType);
        return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.BoolType);
        return Types.BoolType;
    }
}

class OrNode extends BinaryExpNode
{
    public OrNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("||");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    @Override
    public int checkType(int expected) {
            myExp1.checkType(Types.BoolType);
            myExp2.checkType(Types.BoolType);

        return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.BoolType);
        return Types.BoolType;
    }
}

class EqualsNode extends BinaryExpNode
{
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("==");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    @Override
    public int checkType(int expected) {
       myExp1.checkType(Types.IntType);
       myExp2.checkType(Types.IntType);
       return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.BoolType);
        return Types.BoolType;
    }
}

class NotEqualsNode extends BinaryExpNode
{
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("!=");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.BoolType);
        return Types.BoolType;
    }
}

class LessNode extends BinaryExpNode
{
    public LessNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("<");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.BoolType);
        return Types.BoolType;
    }
}

class GreaterNode extends BinaryExpNode
{
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(">");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }
    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.BoolType);
        return Types.BoolType;
    }
}

class LessEqNode extends BinaryExpNode
{
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("<=");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }
    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.BoolType);
        return Types.BoolType;
    }
}

class GreaterEqNode extends BinaryExpNode
{
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(">=");
        myExp2.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }
    @Override
    public int checkType(int expected) {
        myExp1.checkType(Types.IntType);
        myExp2.checkType(Types.IntType);
        return Types.BoolType;
    }
    @Override
    public int returnStmtCheckType(int expected) {
        checkType(Types.BoolType);
        return Types.BoolType;
    }
}