import java.io.*;

public class Codegen{
    // file into which generated code is written
    // public static PrintWriter p = null;
    public static PrintWriter p;
    static int counter = 0;
    static int compareCounter = 0;

    static {
        try {
            p = IO.openOutputFile("Assembler.a");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // values of true and false
    public static final String TRUE = "-1";
    public static final String FALSE = "0";

    // registers
    public static final String FP = "$fp";
    public static final String SP = "$sp";
    //add other registers needed.

    // for pretty printing generated code
    private static final int MAXLEN = 4;


    // for generating labels
    private static int currLabel = 0;


    // **********************************************************************
    // **********************************************************************
    // GENERATE OPERATIONS
    // **********************************************************************
    // **********************************************************************

    // **********************************************************************
    // generateWithComment (string args -- perhaps empty)
    //    given:  op code, and args
    //    do:     write nicely formatted code (ending with new line)
    // **********************************************************************
    public static void generateWithComment(String opcode, String comment,
                                           String arg1, String arg2,
                                           String arg3) {
        int space = MAXLEN - opcode.length() + 2;
        p.print("\t" + opcode);
        if (arg1 != "") {
            for (int k = 1; k <= space; k++) p.print(" ");
            p.print(arg1);
            if (arg2 != "") {
                p.print(", " + arg2);
                if (arg3 != "") p.print(", " + arg3);
            }
        }
        if (comment != "") p.print("\t\t#" + comment);
        p.println();

    }

    public static void generateWithComment(String opcode, String comment,
                                           String arg1, String arg2) {
        int space = MAXLEN - opcode.length() + 2;
        p.print("\t" + opcode);
        if (arg1 != "") {
            for (int k = 1; k <= space; k++) p.print(" ");
            p.print(arg1);
            if (arg2 != "") {
                p.print(", " + arg2);
            }
        }
        if (comment != "") p.print("\t\t#" + comment);
        p.println();
    }

    public static void generateWithComment(String opcode, String comment,
                                           String arg1) {
        int space = MAXLEN - opcode.length() + 2;
        p.print("\t" + opcode);
        if (arg1 != "") {
            for (int k = 1; k <= space; k++) p.print(" ");
            p.print(arg1);
        }
        if (comment != "") p.print("\t\t#" + comment);
        p.println();
    }

    public static void generateWithComment(String opcode, String comment) {
        p.print("\t" + opcode);
        if (comment != "") p.print("\t\t#" + comment);
        p.println();
    }

    // **********************************************************************
    // generate (string args -- perhaps empty)
    //    given:  op code, and args
    //    do:     write nicely formatted code (ending with new line)
    // **********************************************************************
    public static void generate(String opcode, String arg1, String arg2,
                                String arg3) {
        int space = MAXLEN - opcode.length() + 2;

        p.print("\t" + opcode);
        if (arg1 != "") {
            for (int k = 1; k <= space; k++) p.print(" ");
            p.print(arg1);
            if (arg2 != "") {
                p.print(", " + arg2);
                if (arg3 != "") p.print(", " + arg3);
            }
        }
        p.println();
    }

    public static void generate(String opcode, String arg1, String arg2) {
        int space = MAXLEN - opcode.length() + 2;

        p.print("\t" + opcode);
        if (arg1 != "") {
            for (int k = 1; k <= space; k++) p.print(" ");
            p.print(arg1);
            if (arg2 != "") {
                p.print(", " + arg2);
            }
        }
        p.println();
    }

    public static void generate(String opcode, String arg1) {
        int space = MAXLEN - opcode.length() + 2;

        p.print("\t" + opcode);
        if (arg1 != "") {
            for (int k = 1; k <= space; k++) p.print(" ");
            p.print(arg1);
        }
        p.println();
    }

    public static void generate(String opcode) {
        p.print("\t" + opcode);
        p.println();
    }

    // **********************************************************************
    // generate (two string args, one int)
    //    given:  op code and args
    //    do:     write nicely formatted code (ending with new line)
    // **********************************************************************
    public static void generate(String opcode, String arg1, String arg2,
                                int arg3) {
        int space = MAXLEN - opcode.length() + 2;

        p.print("\t" + opcode);
        if (arg1 != "") {
            for (int k = 1; k <= space; k++) p.print(" ");
            p.print(arg1);
            if (arg2 != "") {
                p.print(", " + arg2);
                p.print(", " + arg3);
            }
        }
        p.println();
    }


    // **********************************************************************
    // generate (one string arg, one int)
    //    given:  op code and args
    //    do:     write nicely formatted code (ending with new line)
    // **********************************************************************
    public static void generate(String opcode, String arg1, int arg2) {
        int space = MAXLEN - opcode.length() + 2;

        p.print("\t" + opcode);
        if (arg1 != "") {
            for (int k = 1; k <= space; k++) p.print(" ");
            p.print(arg1);
            p.print(", " + arg2);
        }
        p.println();
    }


    // **********************************************************************
    // generateIndexed
    //    given:  op code, target register T1 (as string), indexed register T2
    //            (as string), - offset xx (int), and optional comment
    //    do:     write nicely formatted code (ending with new line):
    //                 op T1, xx(T2) #comment
    // **********************************************************************
    public static void generateIndexed(String opcode, String arg1,
                                       String arg2, int arg3, String comment)
    {
        int space = MAXLEN - opcode.length() + 2;
        p.print("\t" + opcode);
        for (int k = 1; k <= space; k++) p.print(" ");
        p.print(arg1);
        p.print(", " + arg3);
        p.print("(" + arg2 + ")");

        if (comment != "") p.print("\t\t#" + comment);
        p.println();
    }

    public static void generateIndexed(String opcode, String arg1,
                                       String arg2, int arg3) {
        int space = MAXLEN - opcode.length() + 2;
        p.print("\t" + opcode);
        for (int k = 1; k <= space; k++) p.print(" ");
        p.print(arg1);
        p.print(", " + arg3);
        p.println("(" + arg2 + ")");

    }

    // **********************************************************************
    // generateLabeled (string args -- perhaps empty)
    //    given:  label, op code, comment, and arg
    //    do:     write nicely formatted code (ending with new line)
    // **********************************************************************
    public static void generateLabeled(String label, String opcode,
                                       String comment, String arg1) {
        int space = MAXLEN - opcode.length() + 2;
        if(label != "") p.print(label + ":");
        p.print("\t" + opcode);
        for (int k = 1; k <= space; k++) p.print(" ");
        if (arg1 != "") p.print(arg1);

        if (comment != "") p.print("\t\t#" + comment);
        p.println();
    }

    public static void generateLabeled(String label, String opcode,
                                       String comment) {
        int space = MAXLEN - opcode.length() + 2;
        if(label != "") p.print(label + ":");
        p.print("\t" + opcode);
        for (int k = 1; k <= space; k++) p.print(" ");

        if (comment != "") p.print("\t\t#" + comment);
        p.println();
    }

    // **********************************************************************
    // genPush
    //    generate code to push the given value onto the stack
    // **********************************************************************
    public static void genPush(String s) {
        generateIndexed("sw", s, SP, 0, "PUSH");
        generate("subu", SP, SP, 4);
    }

    // **********************************************************************
    // genPop
    //    generate code to pop into the given register
    // **********************************************************************
    public static void genPop(String s) {
        generate("addu", SP, SP, 4);
        generateIndexed("lw", s, SP, 0, "POP");

    }

    // **********************************************************************
    // genCompare
    //   given: a branch op code (whose two operands are in T0 and T1)
    //   generate this code:
    //       1. branch to truelab using the given op code
    //       3. push false
    //       4. goto falselab
    //       5. truelab: push true
    //       6. falselab:
    public static void genCompare(String op){
        p.print(op + " ");
        p.print("$t0, $t1, ");
        p.print("_truelab"+ compareCounter);
        p.println();
        generate("li", "$t0", 0);
        genPush("$t0");
        p.print("j " + "_falseLab" + compareCounter);
        p.println();
        genLabel("_truelab" + compareCounter);
        generate("li", "$t0", -1);
        genPush("$t0");
        p.println();
        genLabel("_falseLab" + compareCounter);
        compareCounter++;

    }

    // **********************************************************************
    // genLabel
    //   given:    label L and comment (comment may be empty)
    //   generate: L:    # comment
    // **********************************************************************
    public static void genLabel(String label, String comment) {
        p.print(label + ":");

        if (comment != "") p.print("\t\t#" + comment);
        p.println();
    }

    public static void genLabel(String label) {
        p.print(label + ":");
        p.println();
    }

    // **********************************************************************
    // Return a different label each time:
    //        ._L0 ._L1 ._L2, etc.
    // **********************************************************************
    public static String nextLabel() {
        String nextLabel = "_L" + counter;
        counter ++;
        return nextLabel;
    }

    public static void dataSegment(){
        p.print(".data");
        p.println();
    }

    public static void textSegment(){
        p.print(".text");
        p.println();
    }

    public static PrintWriter getOutput()
    {
        return p;
    }

    public static void closePrinter()
    {
        p.close();
    }
}