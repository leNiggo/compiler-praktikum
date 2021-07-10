// Errors
//
// This class is used to generate warning and fatal error messages.

class Errors {

    static int errorsFound = 0;
    static int warningsFound = 0;

    static void fatal(int lineNum, int charNum, String msg) {
        System.err.println(lineNum + ":" + charNum + " **ERROR** " + msg);
        errorsFound ++;
        System.err.flush();
    }

    static void warn(int lineNum, int charNum, String msg) {
        System.err.println(lineNum + ":" + charNum + " **WARNING** " + msg);
        warningsFound ++;
        System.err.flush();
    }

    static int getErrors(){
        return errorsFound;
    }

    static int getWarnings(){
        return warningsFound;
    }
}
