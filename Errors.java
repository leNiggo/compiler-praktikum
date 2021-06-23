// Errors
//
// This class is used to generate warning and fatal error messages.

class Errors {
    static private boolean wereErrors = false;

    static void fatal(int lineNum, int charNum, String msg) {
        System.err.println(lineNum + ":" + charNum + " **ERROR** " + msg);
        wereErrors = true;
    }

    static void warn(int lineNum, int charNum, String msg) {
        System.err.println(lineNum + ":" + charNum + " **WARNING** " + msg);
    }

    static boolean wereErrors() {
        return wereErrors;
    }
}
