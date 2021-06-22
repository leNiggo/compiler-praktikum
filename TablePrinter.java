import java.util.LinkedList;
import java.util.List;

public class TablePrinter {

    public class Contain{
        public SymbolTable st;
        public int indent;
        public int id;
        public Contain(SymbolTable sta , int indenta,int ida){
            st = sta;
            indent = indenta;
            id = ida;
        }
    }
    public boolean doPrint;
    private List<Contain> myList = new LinkedList<>();
    public int runvar = 0;
    public TablePrinter(boolean doPrint)
    {
        this.doPrint = doPrint;
        myList = new LinkedList<>();
    }

    public void addToMap(SymbolTable sym, int indent)
    {
        //Contain con = new Contain(sym,indent,runvar);
        runvar++;
        myList.add(new Contain(sym,indent,runvar));
    }
    public int test(){return 5;}
    public void printMap()
    {
        if(doPrint)
        {
            int i = 0;
            for (Contain con: myList
                 ) {
                System.out.println("Namespace " + con.id+":");
                System.out.printf(con.st.toString(con.indent));
                i++;
            }
        }

    }
}
