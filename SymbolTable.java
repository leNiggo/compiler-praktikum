import java.util.ArrayList;
import java.util.Hashtable;

public class SymbolTable {

	public class Sym {
		public Sym (String id) { myName = id; }
		public String name () { return myName; }


		public void setType(int type){
			myType = type;
		}
		public void setRetType(int retType){
			myReturnType = retType;
		}
		public void setParams(ArrayList<Integer> params){
			myParams = params;
		}
		public void setMySymTab(SymbolTable st){
			mySymTab =st;
		}
		public void setOffset(int offset){this.offset = offset;}

		public int getOffset(){return offset;}
		public int getMyType(){return myType;}
		public int getMyReturnType(){return myReturnType;}
		public ArrayList<Integer> getMyParams(){
			return myParams;
		}

		public void setGlobal(){
			isGlobal = true;
		}

		public boolean isGlobal(){
			return isGlobal;
		}
		// private fields
		private String myName;

		private int myType;
		private int myReturnType;
		private SymbolTable mySymTab;
		private ArrayList<Integer> myParams;
		private int lineNum = 9999999;
		private int charNum = 9999999;
		private boolean isGlobal = false;
		private int offset;

	}


	Hashtable table;

	SymbolTable () { table = new Hashtable(); }
	private SymbolTable myParent;

	public Sym lookup (String name, int linenum, int charnum) {
		Sym returnSym;
		returnSym = lookup_rec(name);
		if(returnSym == null&&!name.equals("main")) {
			Errors.fatal(linenum, charnum, "Variable was not declared: " + name);
		}
		return returnSym;
	}

	public Sym lookup_rec(String name){
		Object returnSym;
		if ((returnSym= table.get(name) )== null){
			if(myParent != null ) {
				returnSym= myParent.lookup_rec(name);
			}
		}
		return (Sym) returnSym;
	}

	public Sym insert (String name, int linenum, int charnum) {
		if (table.containsKey(name)) {
			Errors.fatal(linenum, charnum, "Already declared: "+name);
			return null;
			//return (Sym) table.get(name);
		}
		Sym sym = new Sym(name);
		sym.setMySymTab(this);
		table.put(name, sym);
		return sym;
	}

	public String toString(int indent)
	{
		StringBuilder strBind = new StringBuilder();
		StringBuilder strBout = new StringBuilder();
		for(int i = 0; i<indent;i++)
		{
			strBind.append("\t");
		}
		strBind.append("Layer "+indent + " " );
		String ind = strBind.toString();
		for (Object obj: table.keySet()
		) {
			Sym symbol = (Sym) table.get(obj);
			int type = symbol.myType;
			int retType = symbol.myReturnType;
			String name = (String) obj;

			strBout.append(ind + name+":\t" + Types.ToString(type) + "\t"+Types.ToString(retType));

			if(symbol.myParams !=  null){
				strBout.append(" --ParamList: ");
				for (Integer i: symbol.myParams
				) {
					strBout.append(Types.ToString(i)+ " ");
				}
			}
			strBout.append("\n");
		}
		return strBout.toString();
	}

	public void setMyParent(SymbolTable st)
	{
		myParent = st;
	}

}