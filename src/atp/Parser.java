package atp;

import com.articulate.sigma.*;
import java.io.*;
import java.text.ParseException;
import java.util.*;

public class Parser {
	
    ArrayList<Formula> forms = new ArrayList<Formula>();
	String source = "";
	int pos = -1;
	String name = "";
    int startLine = 0;
    
	/** ***************************************************************
	* A composite term f(t1, ..., tn) is represented by the list
	* [f lt1, ..., ltn], where lt1, ..., ltn are lists representing the
	* subterms.
	"X"          -> "X"
	"g(X, f(Y))" -> ["g", "X", ["f", "Y"]]
	"g(a,b)"      -> ["g", ["a"], ["b"]]
	 */
	public class Term {
	    
	    public String t = "";  // lowercase is a constant, uppercase is a variable
	    public ArrayList<Term> subterms = new ArrayList<Term>();	// empty if not composite
	    public boolean negated = false;
		
	    public String toString() {
	            
	        StringBuffer result = new StringBuffer();
	        if (negated)
	            result.append('-');
	        result.append(t);
	        if (subterms.size() > 0) {
	            result.append('(');
	            for (int i = 0; i < subterms.size(); i++) {
	                result.append(subterms.get(i).toString());
	                if (i < subterms.size()-1)
	                    result.append(", ");
	            }
	            result.append(')');     
	        }
	        return result.toString();
	    }
	    
	    /** ***************************************************************
         */
        public Parser.Term parse(StreamTokenizer_s st) {
                   
            try {
                //System.out.println("Entering: " + this);
                String errStr = ""; 
                String pred = "";
                Parser.Term newT = new Term();
                do {
                    st.nextToken();  
                    //System.out.println(st.ttype + " " + st.sval);
                    switch (st.ttype) {
                        case StreamTokenizer.TT_WORD :
                            /* if (StringUtil.emptyString(t)) {
                                System.out.println("adding " + t + " to self");
                                t = st.sval;
                            }
                            else { */                                
                                newT.t = st.sval;
                                //System.out.println("adding " + newT.t + " to newT");
                            //}
                            break;
                        case StreamTokenizer.TT_EOL :  
                            startLine++;
                            break;
                        case StreamTokenizer.TT_EOF :  
                            return newT;                         
                        case ',':  // 44
                            subterms.add(newT);
                            //System.out.println("hit comma, newT: " + newT);
                            newT = new Term();
                            break;
                        case '(':  // 40
                            //System.out.println("descending: " + newT);
                            newT.parse(st);
                            //subterms.add(newT);
                            //System.out.println("returning from descent: " + newT);
                            break;
                        case ')':  // 41
                            subterms.add(newT);
                            //System.out.println("returning: " + newT);
                            return this;
                    }
                } while (st.ttype != StreamTokenizer.TT_EOF);
            }
            catch (Exception ex) {
                System.out.println("Error in Term.parse(): " + ex.getMessage());
                System.out.println("Error in Term.parse(): token:" + st.ttype);
                if (st.ttype == StreamTokenizer.TT_WORD)
                    System.out.println("Error in Term.parse(): token:" + st.ttype);            
                ex.printStackTrace();
            }
            return null;
        }    

	}
	   /** ***************************************************************
	    * atom - predicate symbol with term arguments
	    * literal - atom or negated atom
	     */
	    public class Literal {
	        
	        public String t = "";  // lowercase is a constant, uppercase is a variable
	        public ArrayList<Term> subterms = new ArrayList<Term>();    // empty if not composite
	        public boolean negated = false;
	        
	        public String toString() {
	                
	            StringBuffer result = new StringBuffer();
	            if (negated)
	                result.append('-');
	            result.append(t);
	            if (subterms.size() > 0) {
	                result.append('(');
	                for (int i = 0; i < subterms.size(); i++) {
	                    result.append(subterms.get(i).toString());
	                    if (i < subterms.size()-1)
	                        result.append(", ");
	                }
	                result.append(')');     
	            }
	            return result.toString();
	        }
	        
	        /** ***************************************************************
	         */
	        public Parser.Term parse(StreamTokenizer_s st) {
	                   
	            try {
	                //System.out.println("Entering: " + this);
	                String errStr = ""; 
	                String pred = "";
	                Parser.Term newT = new Term();
	                do {
	                    st.nextToken();  
	                    //System.out.println(st.ttype + " " + st.sval);
	                    switch (st.ttype) {
	                        case StreamTokenizer.TT_WORD :
	                            /* if (StringUtil.emptyString(t)) {
	                                System.out.println("adding " + t + " to self");
	                                t = st.sval;
	                            }
	                            else { */                                
	                                newT.t = st.sval;
	                                //System.out.println("adding " + newT.t + " to newT");
	                            //}
	                            break;
	                        case StreamTokenizer.TT_EOL :  
	                            startLine++;
	                            break;
	                        case StreamTokenizer.TT_EOF :  
	                            return newT;                         
	                        case ',':  // 44
	                            subterms.add(newT);
	                            //System.out.println("hit comma, newT: " + newT);
	                            newT = new Term();
	                            break;
	                        case '-': 
	                            negated = true;
	                            //System.out.println("hit comma, newT: " + newT);
	                            break;
	                        case '(':  // 40
	                            //System.out.println("descending: " + newT);
	                            newT.parse(st);
	                            //subterms.add(newT);
	                            //System.out.println("returning from descent: " + newT);
	                            break;
	                        case ')':  // 41
	                            subterms.add(newT);
	                            //System.out.println("returning: " + newT);
	                            return this;
	                    }
	                } while (st.ttype != StreamTokenizer.TT_EOF);
	            }
	            catch (Exception ex) {
	                System.out.println("Error in Term.parse(): " + ex.getMessage());
	                System.out.println("Error in Term.parse(): token:" + st.ttype);
	                if (st.ttype == StreamTokenizer.TT_WORD)
	                    System.out.println("Error in Term.parse(): token:" + st.ttype);            
	                ex.printStackTrace();
	            }
	            return null;
	        }    

	    }
	    
	/** ***************************************************************
	 * formula - literal or compound formula
	 * compound formula - logical operator formula [formula]*
	 *   (but conforming to arity of logical operators)
	 */
	public class Formula {
	    
		String logop = ""; 
		ArrayList<String> varlist = new ArrayList<String>();
		ArrayList<Formula> formula = new ArrayList<Formula>();	
		Term lit = new Term();
		
		public String toString() {
		    StringBuffer result = new StringBuffer();
		    result.append(logop);
		    if (logop.equals("!") || logop.equals("?")) {
		        result.append('[');
		        for (int i = 0; i < varlist.size(); i++) {
		            result.append(varlist.get(i).toString());
		            if (i < varlist.size()-1)
		                result.append(", ");
		        }
		        result.append(']');
		    }
		    return result.toString();
		}
		
		/** ***************************************************************
	     * This routine sets up the StreamTokenizer_s so that it parses TPTP.
	     */
	    public void setupStreamTokenizer(StreamTokenizer_s st) {

	        st.whitespaceChars(0,32);
	        st.ordinaryChars(33,44);   // !"#$%&'()*+,
	        st.wordChars(45,46);       // -
	        st.ordinaryChars(46,47);   // ./
	        st.wordChars(48,58);       // 0-9:
	        st.ordinaryChar(59);       // ;
	        st.ordinaryChars(60,62);   // <=>
	        st.ordinaryChars(63,64);   // ?@
	        st.wordChars(65,90);       // A-Z
	        st.ordinaryChars(91,94);   // [\]^
	        st.wordChars(95,95);       // _
	        st.ordinaryChar(96);       // `
	        st.wordChars(97,122);      // a-z
	        st.ordinaryChars(123,255); // {|}~
	        // st.parseNumbers();
	        st.quoteChar('"');
	        st.commentChar('#');
	        st.eolIsSignificant(true);
	    }
	    
		/** ***************************************************************
	     */
	    protected void parse(StreamTokenizer_s st) {
	        
	        int lastVal = 0;
	    
	        try {
	            String errStr = "";
	            setupStreamTokenizer(st);
	            Formula f = new Formula();
	            Formula currentFormula = f;
	            Term l = new Term();
	            boolean inLiteral = false;            
	            do {
	                lastVal = st.ttype;
	                st.nextToken();
	               
	                switch (st.ttype) {
	                    case StreamTokenizer.TT_WORD :   
	                        if (st.sval.equals("=>"))
	                            currentFormula.logop = "=>";
	                        else if (st.sval.equals("<=>"))
	                            currentFormula.logop = "<=>";
	                        else {
	                            if (StringUtil.emptyString(l.t))
	                                l.t = st.sval;
	                            else {
	                                inLiteral = true;
	                                Term t = new Term();
	                                t.t = st.sval;
	                                l.t.add(t);
	                            }
	                        }
	                            
	                        break;
	                    case StreamTokenizer.TT_EOL :  
	                        startLine++;
	                        break;
	                    case '.':       // end of statement
	                        forms.add(f);
	                        f = new Formula();
	                        break;
	                    case '(':
	                        break;
	                    case ')':
	                        break;
	                    case '!':       // for all
	                        st.nextToken();
	                        if (st.ttype != '[') {
	                            errStr = "Unexpected character '" + st.ttype + "'";
	                            throw new ParseException(errStr, startLine);
	                        }
	                        break;
	                    case '?':       // exists
	                        break;
	                    case '&':       // and
	                        currentFormula.logop = "&";
	                        break;
	                    case '|':       // or
	                        currentFormula.logop = "|";
	                        break;
	                    case '-':       // not
	                        if (inLiteral)
	                            l.negated = true;
	                        break;

	                }
	            } while (st.ttype != StreamTokenizer.TT_EOF);
	        }
	        catch (Exception ex) {
	            System.out.println("Error in Parser.parse(): " + ex.getMessage());
	            System.out.println("Error in Parser.parse(): token:" + st.ttype);
	            if (st.ttype == StreamTokenizer.TT_WORD)
	                System.out.println("Error in Parser.parse(): token:" + st.ttype);            
	            ex.printStackTrace();
	        }
	    }
	}
    
    /** ***************************************************************
     * Test method for this class.  
     */
    public static void main(String[] args) {
        
        Parser p = new Parser();
        Parser.Term t = p.new Term();
        Parser.Term newT = t.parse(new StreamTokenizer_s(new StringReader("f(X,g(a,b))")));
        System.out.println("result: " + newT);
        System.out.println("--------------");
        t = p.new Term();
        newT = t.parse(new StreamTokenizer_s(new StringReader("X")));
        System.out.println("result: " + newT);
        System.out.println("--------------");
        t = p.new Term();
        newT = t.parse(new StreamTokenizer_s(new StringReader("g(X, f(Y))")));
        System.out.println("result: " + newT);
        System.out.println("--------------");
        t = p.new Term();
        newT = t.parse(new StreamTokenizer_s(new StringReader("g(a,b)")));
        System.out.println("result: " + newT);
        System.out.println("--------------");
        Parser.Term l = p.new Term();
        l = l.parse(new StreamTokenizer_s(new StringReader("-g(a,b)")));
        System.out.println("result: " + newT);
        System.out.println("--------------");                
    }
        
}
