import java_cup.runtime.Symbol;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class P4 {
    public static void main(String... args) {
        if (args.length != 2) {
            System.err.println("please provide an input file and an output file!");
            System.exit(-1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        FileReader inputFile = null;

        try {
            inputFile = new FileReader(inputPath);
        }
        catch (FileNotFoundException ex) {
            System.err.println("Input File " + inputPath + "could not open! Please check Path!");
            System.exit(-1);
        }

        PrintWriter outputFile = null;

        try {
            outputFile = IO.openOutputFile(outputPath);
        }
        catch (IOException ex) {
            System.err.println("Output File " +outputPath + "could not be opend. Pleas check File!");
        }

        @SuppressWarnings("deprecation")
        parser P = new parser(new Yylex(inputFile));

        Symbol root = null;

        try {
            root = P.parse();
            System.out.println("Simple Programm pared correctly");

        }
        catch (Exception ex) {
            System.err.println("***ERROR*** during parsing:");
            System.err.println(ex.getMessage());
            System.exit(-1);
        }

        System.out.println("Do Semantic analyse...");

        ProgramNode progNode = ((ProgramNode)root.value);
        progNode.checkName();
        progNode.checkType();


        ((ASTnode)root.value).decompile(outputFile, 0);

        assert outputFile != null;
        outputFile.close();

        System.exit(0);
    }
}
