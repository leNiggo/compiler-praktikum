import com.sun.org.apache.bcel.internal.classfile.Code;

import java.io.PrintWriter;
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

    abstract public void codeGen();

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


    public void codeGen(){
        Codegen.dataSegment();
        Codegen.generateLabeled("_true", ".asciiz\t", "TrueLabel", "\"true\"");
        Codegen.generateLabeled("_false", ".asciiz\t", "FalseLabel", "\"false\"");
        Codegen.generateLabeled("_newLine", ".asciiz\t", "NewLine", "\"\\n\"");

        Codegen.textSegment();
        //Print

        Codegen.genLabel("_printBool");
        Codegen.genPop("$t0");
        Codegen.generate("beq","$t0","0", "_printFalse");
        //Codegen.generate("beq","$t0","-1", "_printTrue");TODO alles ausser 0 wird Moment als true gewertet
        Codegen.generate("la","$t0","_true");
        Codegen.genPush("$t0");
        Codegen.generate("j", "_printTrue");
        Codegen.genLabel("_printFalse");
        Codegen.generate("la","$t0","_false");
        Codegen.genPush("$t0");
        Codegen.genLabel("_printTrue");
        Codegen.generate("jr $ra");
        //beq
        //falsePath -->j False
        //trueLabel
        //TruePath
        //FalseLabel
        //Print end

        Codegen.genLabel("main");
        Codegen.genLabel(myId.getName());
        Codegen.generate("move","$s1", Codegen.SP);
        myClassBody.codeGen();
        Codegen.generate("move",Codegen.SP,"$s1");
        Codegen.generate("li", "$v0", 10);
        Codegen.generate("syscall");
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

    public void codeGen(){
        myDeclList.codeGen();
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
            int i = 0;
            for (myDecls.start(); myDecls.isCurrent(); myDecls.advance()) {
                ((DeclNode)myDecls.getCurrent()).checkType(4*i);
                i++;
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in DeclListNode.print");
            System.exit(-1);
        }
    }
    public void codeGen(){
        try {
            for (myDecls.start(); myDecls.isCurrent(); myDecls.advance()) {
                ((DeclNode)myDecls.getCurrent()).codeGen();
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

    public void checkType(int offset)
    {
        try {
            int i = offset;
            for (myFormals.start(); myFormals.isCurrent(); myFormals.advance()) { // myFormals.advance()
                ((FormalDeclNode)myFormals.getCurrent()).checkType(i*4);
                i++;
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.print");
            System.exit(-1);
        }
    }

    public void codeGen(){
        try {

            for (myFormals.start(); myFormals.isCurrent(); myFormals.advance()) { // myFormals.advance()
                ((FormalDeclNode)myFormals.getCurrent()).codeGen();
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.codeGen");
            System.exit(-1);
        }
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

    public int checkType(int expected, int offset){
        myDeclList.checkType(offset);
        return myStmtList.checkType(expected);
    }

    public void codeGen(){
        myDeclList.codeGen();
        myStmtList.codeGen();
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

    public void codeGen(){
        try {
            for (myStmts.start(); myStmts.isCurrent(); myStmts.advance()) {
                ((StmtNode)myStmts.getCurrent()).codeGen();
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in StmtListNode.decompile");
            System.exit(-1);
        }
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
            System.err.println("unexpected NoCurrentException in VarDeclListNode.decompile");
            System.exit(-1);
        }
    }

    public void checkName(SymbolTable st, TablePrinter tp){
        try {
            for (myVarDecl.start(); myVarDecl.isCurrent(); myVarDecl.advance()) {
                ((VarDeclNode) myVarDecl.getCurrent()).checkName(st,tp);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in VarDeclListNode.checkName");
            System.exit(-1);
        }
    }
    public void checkType(int offset){
        try {
            int i = offset;
            for (myVarDecl.start(); myVarDecl.isCurrent(); myVarDecl.advance()) {
                ((VarDeclNode) myVarDecl.getCurrent()).checkType(i);
                i += 4;
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in VarDeclListNode.checkType");
            System.exit(-1);
        }
    }


    public void codeGen(){
        try {
            for (myVarDecl.start(); myVarDecl.isCurrent(); myVarDecl.advance()) {
                ((VarDeclNode) myVarDecl.getCurrent()).codeGen();
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in VarDeclListNode.checkType");
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


    public boolean checkParams(ArrayList<Integer> params, String name)
    {
        ArrayList<Integer> runList = new ArrayList<>();
        boolean returnVal = true;
        if(myExps.length() == params.size())
        {
            try {
                int i = 0;
                for (myExps.start(); myExps.isCurrent(); myExps.advance()) {
                    int checkVal= ((ExpNode) myExps.getCurrent()).checkType();
                    runList.add(checkVal);
                    if(checkVal != params.get(i))
                    {
                        Errors.fatal(((ExpNode) myExps.getCurrent()).getLineNum(),((ExpNode) myExps.getCurrent()).getCharNum(),
                                "Parameter type mismatch for method call "+ name +"-- expected: "+Types.ToString(params.get(i))+" | provided: "+ Types.ToString(checkVal));
                    }
                    i++;
                }
            } catch (NoCurrentException ex) {
                System.err.println("unexpected NoCurrentException in FormalsListNode.print");
                System.exit(-1);
            }
        }
        else
        {
            try {

                for (myExps.start(); myExps.isCurrent(); myExps.advance()) {
                    ((ExpNode) myExps.getCurrent()).checkType();
                }
            } catch (NoCurrentException ex) {
                System.err.println("unexpected NoCurrentException in FormalsListNode.print");
                System.exit(-1);
            }
            returnVal = false;
        }
        return returnVal;
    }

    public void codeGen(){
        try {
            int i = 0;
            int offset;
            for (myExps.start(); myExps.isCurrent();myExps.advance()){
                ((ExpNode)myExps.getCurrent()).codeGen();
                //offset von fp berechnen = -8 + -4i
                //sw
                offset = 4*i + 8;
                //Codegen.generate("li","$t0", offset);
                Codegen.genPop("$t1");
                Codegen.generate("subu","$t0", Codegen.SP, offset);
                Codegen.generateIndexed("sw", "$t1","$t0",0);
                i++;
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.print");
            System.exit(-1);
        }
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
    public abstract void checkType(int offset);
    public static boolean isFirst = false;
}

class FieldDeclNode extends DeclNode {
    public FieldDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
        myId.setGlobal();
    }
    public void decompile(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("static ");
        myType.decompile(p, indent);
        p.print(" ");
        myId.decompile(p, indent);
        p.print("(" + myId.getOffset() + ")");
        p.println(";");
    }
    public void checkName(SymbolTable st, TablePrinter tp){
        myId.checkInit(st,myType.getType(),myType.getType(),null);
    }

    public void checkType(int offset){
        myId.setOffset(offset);
    }

    public void codeGen(){
        myId.codeGen();
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
        p.print("(" + myId.getOffset() +")");
        p.print(";\n");
    }

    // 2 kids
    private TypeNode myType;
    private IdNode myId;
    public void checkName(SymbolTable st, TablePrinter tp){
        myId.checkInit(st,myType.getType(),myType.getType(),null);
    }
    public void checkType(int offset){
        myId.setOffset(offset);
    }

    public void codeGen(){
        myId.codeGen();
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
    }
    public void checkType(int offset){
        myFormals.checkType(0);
        int nextOffset = 4 * (myParams.size());
        if(myBody.checkType(myRetType, nextOffset)==Types.ErrorType)
        {
            Errors.fatal(myId.getLineNum(),myId.getCharNum(),"Missing return statement in Method " + myId.getName());
        }
    }

    public void codeGen(){
        if(!isFirst)
        {
            Codegen.generate("j","_main");
            isFirst = true;
        }
        if(myId.getName().equals("main"))
        {
            Codegen.genLabel("_main");

        }
        else {
            Codegen.genLabel(myId.getName());
        }
        Codegen.genPush(Codegen.FP);
        Codegen.genPush("$ra");
        Codegen.generate("move",Codegen.FP, Codegen.SP);
        //Codegen.generateIndexed("la","$t0", Codegen.SP,0);
        myFormals.codeGen();
        myBody.codeGen();
        Codegen.generate("move",Codegen.SP,Codegen.FP);
        Codegen.genPop("$ra");
        //Codegen.generate("la",, "$t0");
        Codegen.genPop(Codegen.FP);
        //Codegen.generate("move",Codegen.FP, "$t0");
        if(!myId.getName().equals("main"))
        {
            Codegen.generate("jr","$ra");
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
    public void checkType(int offset){
        myFormals.checkType(0);
        int nextOffset = 4 * (myParams.size());
        if(myBody.checkType(myRetType, nextOffset)==Types.ErrorType)
        {
            Errors.fatal(myId.getLineNum(),myId.getCharNum(),"Missing return statement in Method " + myId.getName());
        }

    }

    public void codeGen(){

        if(!isFirst)
        {
            Codegen.generate("j","_main");
            isFirst = true;
        }
        if(myId.getName().equals("main"))
        {
            Codegen.genLabel("_main");

        }
        else {
            myId.codeGen();
        }
        Codegen.genPush(Codegen.FP);
        Codegen.genPush("$ra");
        Codegen.generate("move",Codegen.FP, Codegen.SP);
        //Codegen.generateIndexed("la","$t0", Codegen.SP,0);
        myFormals.codeGen();
        myBody.codeGen();
        //Codegen.generate("move",Codegen.FP, "$t0");
        if(!myId.getName().equals("main"))
        {
            Codegen.generate("jr","$ra");
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
        p.print("(" + myId.getOffset() +")");
    }
    public void checkName(SymbolTable st, TablePrinter tp){
        myId.checkInit(st, myType.getType(), myType.getType(), null);
    }

    public int getParamType()
    {
        return myType.getType();
    }
    public void checkType(int offset){
        myId.setOffset(offset);
    }
    public void codeGen(){
        myId.codeGen();
    }

    //
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
    public void checkType(int offset){
        myList.checkType(offset);
    }
    public void codeGen(){
        myList.codeGen();
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

    public void codeGen(){

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
    public void codeGen(){

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
    public void codeGen(){

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
        myExp.decompile(p,indent);
        p.print(");");
    }
    public void checkName(SymbolTable st){
        myExp.checkName(st);
    }

    @Override
    public int checkType(int expected) {
        myExpType = myExp.checkType();
        return Types.MethodType;
    }
    public void codeGen(){
        myExp.codeGen();


        int syscall = 1;

        switch (myExpType){
            case Types.IntType: syscall = 1;
                break;
            case Types.StringType: syscall = 4;
                break;
            case Types.BoolType: syscall = 4;
                Codegen.generate("jal", "_printBool");
                break;
            default: syscall = 1;
        }
        Codegen.genPop("$a0");
        Codegen.generate("li", "$v0",syscall);
        Codegen.generate("syscall");
        Codegen.generate("la","$a0","_newLine");
        Codegen.generate("li", "$v0",4);
        Codegen.generate("syscall");
    }
    // 1 kid
    private ExpNode myExp;
    private int myExpType = Types.ErrorType;
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(IdNode id, ExpNode exp, int lineNum, int ColNum) {
        myLineNum = lineNum;
        myCharNum = ColNum;
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
        int returnVal = Types.MethodType;
        int idVal = myId.checkType();
        int expVal = myExp.checkType();
        boolean isError = idVal == Types.ErrorType || expVal == Types.ErrorType;
        if(idVal != expVal && !isError)
        {
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum, myCharNum,"Assign type mismatch-- expected: "+Types.ToString(idVal)+" | provided: "+Types.ToString(expVal));

        }
        return returnVal;
    }
    public void codeGen(){
        myExp.codeGen();
        myId.codeGenAssign();
        Codegen.genPop("$t0");
        Codegen.genPop("$t1");
        Codegen.generateIndexed("sw","$t1","$t0",0,"Assign");
    }
    // 2 kids
    private int myLineNum;
    private int myCharNum;
    private IdNode myId;
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, StmtListNode slist, int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
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
        int expVal = myExp.checkType();
        if(expVal != Types.BoolType && expVal != Types.ErrorType)
        {
            Errors.fatal(myLineNum,myCharNum,"If-expression error-- expected: "+Types.ToString(Types.BoolType)+ " | provided: "+ Types.ToString(expVal));
        }
        return myStmtList.checkType(expected);
    }
    public void codeGen(){
        myExp.codeGen();
        Codegen.genPop("$t0");
        String label = Codegen.nextLabel();
        Codegen.generate("beq","$t0","0", label);
        myStmtList.codeGen();
        Codegen.genLabel(label);
    }
    // 2 kids

    private int myLineNum;
    private int myCharNum;
    private ExpNode myExp;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, StmtListNode slist1,
                          StmtListNode slist2, int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
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
        int expVal = myExp.checkType();
        if(expVal != Types.BoolType && expVal != Types.ErrorType)
        {
            Errors.fatal(myLineNum,myCharNum,"If-expression error-- expected: "+Types.ToString(Types.BoolType)+ " | provided: "+ Types.ToString(expVal));

        }
        int typeThen = myThenStmtList.checkType(expected);
        int typeElse = myElseStmtList.checkType(expected);
        if(typeThen == Types.ErrorType||typeElse == Types.ErrorType)
        {
            return Types.ErrorType;
        }
        return expected;
    }
    public void codeGen(){
        myExp.codeGen();
        Codegen.genPop("$t0");
        String labelTrue = Codegen.nextLabel();
        String labelFalse = Codegen.nextLabel();
        Codegen.generate("beq","$t0","0", labelFalse);
        myThenStmtList.codeGen();
        Codegen.generate("j",labelTrue);
        Codegen.genLabel(labelFalse);
        myElseStmtList.codeGen();
        Codegen.genLabel(labelTrue);
    }
    // 3 kids
    private int myLineNum;
    private int myCharNum;
    private ExpNode myExp;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, StmtListNode slist, int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
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
        int stmtVal = myStmtList.checkType(expected);
        int expVal = myExp.checkType();
        if(expVal != Types.BoolType && expVal != Types.ErrorType)
        {
            Errors.fatal(myLineNum,myCharNum,"While-expression error-- expected: "+Types.ToString(Types.BoolType)+ " | provided: "+ Types.ToString(expVal));

        }

        return stmtVal;
    }
    public void codeGen(){
        String label = Codegen.nextLabel();
        Codegen.genLabel(label);
        myStmtList.codeGen();

        myExp.codeGen();
        Codegen.genPop("$t0");
        Codegen.generate("beq","$t0","-1", label);

    }
    //
    private int myLineNum;
    private int myCharNum;
    private ExpNode myExp;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(IdNode id, ExpListNode elist, int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myId = id;
        myExpList = elist;
    }

    public CallStmtNode(IdNode id, int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
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
        if(!myExpList.checkParams(myId.getParams(),myId.getName()))
        {
            Errors.fatal(myLineNum,myCharNum,"Callstatement-- Wrong amount of parameters for Method " + myId.getName());
        }
        return myId.checkType();
    }
    public void codeGen(){

        myExpList.codeGen(); //8 vom Pointer aus füllen
        myId.codeGen();

    }
    //
    private int myLineNum;
    private int myCharNum;
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
        int expVal;
        if(myExp != null)
        {
            expVal = myExp.checkType();
        }
        else
        {
            expVal = Types.MethodType;
        }

        if (expVal != expected && expVal != Types.ErrorType)
        {
            String want = Types.ToString(expected);
            String have = Types.ToString(expVal);
            if(expected== Types.MethodType)
            {
                want = "void";
            }
            if(expVal== Types.MethodType)
            {
                have = "void";
            }
            Errors.fatal(myLineNum,myCharNum,"Return type mismatch: expected " + want + " | provided " + have);
        }
        else if(expVal == Types.ErrorType){
            expVal = expected;
        }
        return expVal;
    }
    public void codeGen(){
        if(myExp != null) {
            myExp.codeGen();
            Codegen.genPop("$t0");
        }
        Codegen.generate("move",Codegen.SP,Codegen.FP);
        Codegen.genPop("$ra");
        Codegen.genPop(Codegen.FP);

        if(myExp!=null)
        {
            Codegen.genPush("$t0");
        }

        Codegen.generate("jr", "$ra");
    }
    //
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

    public void codeGen(){

    }

    private VarDeclListNode myVarDeclList;
    private StmtListNode myStmtList;
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    public abstract void checkName(SymbolTable st);
    public abstract int checkType();
    public abstract int getLineNum();
    public abstract int getCharNum();
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int colNum, int intVal) {
        myLineNum = lineNum;
        myColNum = colNum;
        myIntVal = intVal;
    }

    public void decompile(PrintWriter p, int indent) {p.print(myIntVal);
    }

    public void checkName(SymbolTable st){}

    public int checkType(){
        return Types.IntType;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        Codegen.generate("li", "$t0", myIntVal);
        Codegen.genPush("$t0");
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
    public void checkName(SymbolTable st){}

    public int checkType(){
        return Types.StringType;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        String label = Codegen.nextLabel();
        Codegen.dataSegment();
        Codegen.generateLabeled(label,".asciiz\t","Erster Stringtest", "\"" + myStrVal + "\"");
        Codegen.textSegment();
        Codegen.generate("la", "$t0", label);
        Codegen.genPush("$t0");
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

    public void decompile(PrintWriter p, int indent){
        p.print("true");
    }

    public void checkName(SymbolTable st){}

    public int checkType(){
        return Types.BoolType;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        Codegen.generate("li","$t0","-1");
        Codegen.genPush("$t0");
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
    public void checkName(SymbolTable st){}

    public int checkType(){
        return Types.BoolType;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        Codegen.generate("li","$t0","0");
        Codegen.genPush("$t0");
    }

    private int myLineNum;
    private int myColNum;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode id, ExpListNode elist, int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myId = id;
        myExpList = elist;
    }

    public CallExpNode(IdNode id, int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myId = id;
        myExpList = new ExpListNode(new Sequence());
    }

    public void decompile(PrintWriter p, int indent) {
        myId.decompile(p,indent);
        p.print("(");
        myExpList.decompile(p,indent);
        p.print(")");
    }
    public void checkName(SymbolTable st)
    {
        myId.checkName(st);
        myExpList.checkName(st);
    }

    public int checkType(){
        if(!myExpList.checkParams(myId.getParams(), myId.getName()))
        {
            Errors.fatal(myLineNum,myCharNum,"Wrong amount of parameters in Method " + myId.getName());
        }
        return myId.checkType();
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myCharNum;
    }

    public void codeGen(){
        myExpList.codeGen();
        myId.codeGen();
    }

    // 2 kids
    private int myLineNum;
    private int myCharNum;
    private IdNode myId;
    private ExpListNode myExpList;
}

class BracketsNode extends ExpNode{
    public BracketsNode(ExpNode exp, int linenum, int charnum) {
        myExp = exp;
        myLineNum = linenum;
        myCharNum = charnum;
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

    public int checkType(){
        return myExp.checkType();
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myCharNum;
    }

    public void codeGen(){
        myExp.codeGen();
    }

    private ExpNode myExp;
    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode{
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void decompile(PrintWriter p, int indent) {
        p.print(myStrVal);
        //p.print("(" + myRef.isGlobal() + ")");
    }
    public void checkInit(SymbolTable st,int type, int retType, ArrayList<Integer> params){
        myRef=  st.insert(myStrVal,myLineNum,myCharNum);
        if(myRef != null ) {
            myRef.setType(type);
            myRef.setRetType(retType);
            myRef.setParams(params);
            if (isGlobal) myRef.setGlobal();
        }
        myType = type;
        myRetType = retType;
        myParams = params;
        isDeclaration = true;

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
        else
        {
            isDeclaration = true;
        }
    }

    public ArrayList<Integer> getParams()
    {
        return myParams;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myCharNum;
    }

    public int checkType(){
        return  myRetType;
    }

    public String getName(){
        return myStrVal;
    }

    public void setOffset(int offset) {
        myRef.setOffset(offset);
    }
    public int getOffset() {
        return myRef.getOffset();
    }

    public void codeGen(){

        if(myType == Types.MethodType)
        {
            if(!isDeclaration){
                Codegen.generate("jal", myStrVal);
            }
            else {
                Codegen.genLabel(myStrVal);
            }
        }
        else
        {
            if(isDeclaration){
                //System.out.println("new Var: " + myRef.name());
//                Codegen.generate("li", "$t0", 0);
//                Codegen.generateIndexed("sw","$t0", Codegen.SP,0);
                Codegen.generateWithComment("subu","Param/VarDecl", Codegen.SP, Codegen.SP, "4");

            }
            else
            {   //find varialble in stack via offset (from framepointer)
                //Prüfung auf Global

                if(myRef.isGlobal())
                {
                    Codegen.generate("subu", "$t0" , "$s1" , myRef.getOffset());
                }
                else
                {
                    Codegen.generate("subu", "$t0" , "$fp" , myRef.getOffset());
                }

                //return value
                Codegen.generateIndexed("lw", "$t1", "$t0",0);
                Codegen.genPush("$t1");
            }
        }
        /*
        setVariable;
         */
    }

    public void codeGenAssign(){
        //find adress in stack (from framepointer)
        // return offset
        if(myRef.isGlobal())
        {
            Codegen.generate("subu", "$t0" , "$s1" , myRef.getOffset());
        }
        else
        {
            Codegen.generate("subu", "$t0" , "$fp" , myRef.getOffset());
        }

        Codegen.genPush("$t0");
        //operation mit diesem zielregister
        //Im assign: sw $t0, [offset]($fp)
        //pop(t0)
        //copy fp in s1
        //s1 - t0
        //sw s1

    }

    public void setGlobal()
    {
        isGlobal = true;
    }

    private boolean isDeclaration = false;
    boolean isGlobal = false;
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

abstract class BinaryExpNode extends ExpNode{
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }
    public void checkName(SymbolTable st)
    {
        myExp1.checkName(st);
        myExp2.checkName(st);
    }

    public void codeGen(){
        //Codegen.generate("addi","$s0","$s1",4);
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
    public UnaryMinusNode(ExpNode exp, int lineNum, int ColNum) {

        super(exp);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType(){
        int returnVal = myExp.checkType();
        if(returnVal != Types.IntType && returnVal != Types.ErrorType)
        {
            Errors.fatal(myLineNum,myColNum,"Non-Integer applied to Unary minus, provided " + Types.ToString(returnVal));
            returnVal = Types.ErrorType;
        }
        else if(returnVal == Types.ErrorType)
        {
            returnVal = Types.IntType;
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp.codeGen();
        Codegen.genPop("$t0");
        Codegen.generate("li", "$t1", "-1");
        Codegen.generate("mul", "$t0", "$t0", "$t1");
        Codegen.genPush("$t0");
    }

    int myLineNum;
    int myColNum;
}

class NotNode extends UnaryExpNode
{
    public NotNode(ExpNode exp, int lineNum, int ColNum) {
        super(exp);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal =myExp.checkType();
        if(returnVal != Types.BoolType && returnVal != Types.ErrorType)
        {
            Errors.fatal(myLineNum,myColNum,"Non-Boolean expression applied to Not-Operator, provided " + Types.ToString(returnVal));
            returnVal = Types.ErrorType;
        }
        else if(returnVal == Types.ErrorType)
        {
            returnVal = Types.BoolType;
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp.codeGen();;
        Codegen.genPop("$t0");
        Codegen.generate("nor", "$t0", "$t0", "$t0");
        Codegen.genPush("$t0");
    }

    int myLineNum;
    int myColNum;

}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode
{
    public PlusNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType(){
        int returnVal = Types.IntType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.IntType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to left side of Plus Operator, provided " + Types.ToString(expVal1));;
        }
        if (expVal2 != Types.IntType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to right side of Plus Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.generate("add", "$t0", "$t0","$t1");
        Codegen.genPush("$t0");
    }

    int myLineNum;
    int myColNum;
}

class MinusNode extends BinaryExpNode
{
    public MinusNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.IntType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.IntType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to left side of Minus Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.IntType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to right side of Minus Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.generate("sub", "$t0", "$t0", "$t1");
        Codegen.genPush("$t0");
    }

    int myLineNum;
    int myColNum;
}

class TimesNode extends BinaryExpNode
{
    public TimesNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType(){
        int returnVal = Types.IntType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.IntType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to left side of Multiplication Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.IntType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to right side of Multiplication Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.generate("mul", "$t0", "$t0", "$t1");
        Codegen.genPush("$t0");
    }

    int myLineNum;
    int myColNum;
}

class PowerNode extends BinaryExpNode
{
    public PowerNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.IntType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.IntType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to left side of Potentiation Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.IntType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to right side of Potentiation Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t0"); //Potenz
        Codegen.genPop("$t1"); //Exponent

        Codegen.generate("addi", "$t3", 1); //Ergebnis

        String label = Codegen.nextLabel();
        String end = Codegen.nextLabel();
        Codegen.genLabel(label);
        Codegen.generate("beq", "$t0", "$0", end);
        Codegen.generate("mul", "$t3", "$t3" ,"$t1");
        Codegen.generate("sub", "$t0", "$t0", 1);
        Codegen.generate("j", label);

        Codegen.genLabel(end);
        Codegen.genPush("$t3");
    }

    int myLineNum;
    int myColNum;
}

class DivideNode extends BinaryExpNode
{
    public DivideNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.IntType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.IntType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to left side of Division Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.IntType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to right side of Division Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.generate("div" , "$t0", "$t0", "$t1");
        Codegen.genPush("$t0");
    }

    int myLineNum;
    int myColNum;
}

class AndNode extends BinaryExpNode
{
    public AndNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.BoolType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.BoolType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Boolean Expression applied to left side of And Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.BoolType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Boolean Expression applied to right side of And Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.generate("and", "$t0", "$t0", "$t1");
        Codegen.genPush("$t0");
    }

    int myLineNum;
    int myColNum;
}

class OrNode extends BinaryExpNode
{
    public OrNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.BoolType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.BoolType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Boolean Expression applied to left side of Or Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.BoolType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Boolean Expression applied to right side of Or Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.generate("or", "$t0", "$t0", "$t1");
        Codegen.genPush("$t0");
    }

    int myLineNum;
    int myColNum;
}

class EqualsNode extends BinaryExpNode
{
    public EqualsNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.BoolType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();

        boolean isEqual = expVal1 == expVal2;
        boolean isError = expVal1 == Types.ErrorType || expVal2 == Types.ErrorType;
        //
        if (!isEqual && !isError)
        {
            returnVal =Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Data Types at Equal Operator not equivalent: " + Types.ToString(expVal1) + " " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.genCompare("beq");
    }

    int myLineNum;
    int myColNum;
}

class NotEqualsNode extends BinaryExpNode
{
    public NotEqualsNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.BoolType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();

        boolean isEqual = expVal1 == expVal2;
        boolean isError = expVal1 == Types.ErrorType || expVal2 == Types.ErrorType;
        //
        if (!isEqual && !isError)
        {
            returnVal =Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Data Types at NotEqual Operator not equivalent: " + Types.ToString(expVal1) + " " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.genCompare("bne");
    }

    int myLineNum;
    int myColNum;
}

class LessNode extends BinaryExpNode
{
    public LessNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.BoolType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.IntType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to left side of Less Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.IntType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to right side of Less Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.genCompare("blt");
    }

    int myLineNum;
    int myColNum;
}

class GreaterNode extends BinaryExpNode
{
    public GreaterNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.BoolType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.IntType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to left side of Greater Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.IntType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to right side of Greater Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.genCompare("bgt");
    }

    int myLineNum;
    int myColNum;
}

class LessEqNode extends BinaryExpNode
{
    public LessEqNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.BoolType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.IntType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to left side of LessEqual Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.IntType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to right side of LessEqual Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }
    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.genCompare("ble");
    }

    int myLineNum;
    int myColNum;
}

class GreaterEqNode extends BinaryExpNode
{
    public GreaterEqNode(ExpNode exp1, ExpNode exp2, int lineNum, int ColNum) {
        super(exp1, exp2);
        myLineNum = lineNum;
        myColNum = ColNum;
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

    public int checkType()
    {
        int returnVal = Types.BoolType;
        int expVal1 = myExp1.checkType();
        int expVal2 = myExp2.checkType();
        if(expVal1 != Types.IntType && expVal1 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to left side of GreaterEqual Operator, provided " + Types.ToString(expVal1));
        }
        if (expVal2 != Types.IntType && expVal2 != Types.ErrorType){
            returnVal = Types.ErrorType;
            Errors.fatal(myLineNum,myColNum,"Non-Integer Expression applied to right side of GreaterEqual Operator, provided " + Types.ToString(expVal2));
        }
        return returnVal;
    }

    public void codeGen(){
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop("$t1");
        Codegen.genPop("$t0");
        Codegen.genCompare("bge");
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myColNum;
    }

    int myLineNum;
    int myColNum;
}