/*
A simple implementation of first-order terms. 
Copyright 2010-2011 Adam Pease, apease@articulatesoftware.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA 
*/

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
        
        /** ***************************************************************
         * Check if the term is a variable. This assumes that t is a
         * well-formed term.
         */
        public boolean termIsVar() {
           
            return Character.isUpperCase(t.charAt(0));
        }
        
        /** ***************************************************************
         * Check if the term is a compound term. This assumes that t is a
         * well-formed term.
         */
        public boolean termIsCompound() {

            return !termIsVar();
        }
        
        /** ***************************************************************
         * Return True if term has no variables, False otherwise
         */
        public boolean termIsGround() { 
            
            if (!StringUtil.emptyString(t) && Character.isUpperCase(t.charAt(0)))
                return false;
            for (int i = 0; i < subterms.size(); i++)
                if (!subterms.get(i).termIsGround())
                    return false;
            return true;
        }
        
        /** ***************************************************************
         * Return the weight of the term,  counting fweight for each function symbol
         * occurrence, vweight for each variable occurrence. Examples: 
            #                  termWeight(f(a,b), 1, 1) = 3
            #                  termWeight(f(a,b), 2, 1) = 6
            #                  termWeight(f(X,Y), 2, 1) = 4
            #                  termWeight(X, 2, 1)      = 1
            #                  termWeight(g(a), 3, 1)   = 6
         */
        public int termWeight(int fweight, int vweight) {
            
            int total = 0;
            if (Character.isUpperCase(t.charAt(0)))
                total = vweight;
            else
                total = fweight;
            for (int i = 0; i < subterms.size(); i++)
                total = total + subterms.get(i).termWeight(fweight,vweight);
            return total;
        }
        
        /** ***************************************************************
         *Return the subterm of t at position pos (or None if pos is not a 
         * position in term). pos is a list of integers denoting branches, e.g.
        #                  subterm(f(a,b), [])        = f(a,b)
        #                  subterm(f(a,g(b)), [0])    = a
        #                  subterm(f(a,g(b)), [1])    = g(b)
        #                  subterm(f(a,g(b)), [1,0])  = b
        #                  subterm(f(a,g(b)), [3,0])  = None
         */
        public Term subterm(ArrayList<Integer> pos) {
            
            if (pos.size() == 0)
                return this;
            int index = pos.remove(pos.size()-1).intValue();
            if (index >= subterms.size())
                return null;
            if (pos.size() == 0)
                return subterms.get(index);
            else
                return subterms.get(index).subterm(pos);
        }
        
        /** ***************************************************************
         */
        public boolean equals(Term other) {
            
            if (!other.t.equals(t))
                return false;
            if (other.subterms.size() != subterms.size())
                return false;
            for (int i = 0; i < subterms.size(); i++)
                if (!subterms.get(i).equals(other.subterms.get(i)))
                    return false;
            return true;
        }
        
        /** ***************************************************************
         */
        public Term termCopy() {
            
            Term result = new Term();
            result.t = t;
            for (int i = 0; i < subterms.size(); i++)
                result.subterms.add(subterms.get(i).termCopy());
            return result;
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
                for (int i = 0; i < subterms.size(); i++) {
                    result.append(subterms.get(i).toString());
                    if (i < subterms.size()-1)
                        result.append(", ");
                }    
            }
            return result.toString();
        }
        
        /** ***************************************************************
         */
        public Parser.Literal parse(StreamTokenizer_s st) {
                   
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
                                t = st.sval;
                                //System.out.println("adding " + t + " to self");
                                //System.out.println("adding " + newT.t + " to newT");
                            //}
                            break;
                        case StreamTokenizer.TT_EOL :  
                            startLine++;
                            break;
                        case StreamTokenizer.TT_EOF :  
                            return this;                         
                        case ',':  // 44
                            subterms.add(newT);
                            //System.out.println("hit comma, newT: " + newT);
                            newT = new Term();
                            break;
                        case '-': 
                            negated = true;
                            //System.out.println("hit negation, newT: " + this);
                            break;
                        case '(':  // 40
                            //System.out.println("descending: " + newT);
                            newT.parse(st);
                            subterms.add(newT);
                            //System.out.println("returning from descent: " + newT);
                            break;
                        case ')':  // 41
                            subterms.add(newT);
                            //System.out.println("returning: " + this);
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
	                                lit.t = st.sval;
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
     * Set up test content.  
     */
	String example1 = "X";
	String example2 = "a";
	String example3 = "g(a,b)";
	String example4 = "g(X, f(Y))";     
	String example5 = "g(X, f(Y))";    
    String example6 = "f(X,g(a,b))";    
    String example7 = "-g(a,b)";    

	Term t1 = null;
	Term t2 = null;
	Term t3 = null;
	Term t4 = null;
	Term t5 = null;
	Term t6 = null;
	
    /** ***************************************************************
     * Set up test content.  
     */
	public void setupTests() {
	    
	    Parser p = new Parser();
	    Term t = new Term();
	    t1 = t.parse(new StreamTokenizer_s(new StringReader(example1)));
	    t2 = t.parse(new StreamTokenizer_s(new StringReader(example2)));
	    t3 = t.parse(new StreamTokenizer_s(new StringReader(example3)));
	    t4 = t.parse(new StreamTokenizer_s(new StringReader(example4)));
	    t5 = t.parse(new StreamTokenizer_s(new StringReader(example5)));
	    t6 = t.parse(new StreamTokenizer_s(new StringReader(example6)));
	}
	
	/** ***************************************************************
     * Test that parse() is working properly   
     */
    public void parseTest() {
        
        System.out.println("---------------------");
        System.out.println("INFO in parseTest()");
        System.out.println(t1 + " = " + example1);
        System.out.println(t2 + " = " + example2);
        System.out.println(t3 + " = " + example3);
        System.out.println(t4 + " = " + example4);
        System.out.println(t5 + " = " + example5);
        System.out.println(t6 + " = " + example6);
    }
    
    /** ***************************************************************
     * Test that parse() and toString() are dual. Start with terms, 
     * so that we are sure to get the canonical string representation.   
     */
    public void testToString() {

        System.out.println("---------------------");
        System.out.println("INFO in Parser.testToString(): all should be true");
        Term t = new Term();
        t = t.parse(new StreamTokenizer_s(new StringReader(t1.toString())));
        System.out.println(t1.toString().equals(t.toString()));
        t = t.parse(new StreamTokenizer_s(new StringReader(t2.toString())));
        System.out.println(t2.toString().equals(t.toString()));
        t = t.parse(new StreamTokenizer_s(new StringReader(t3.toString())));
        System.out.println(t3.toString().equals(t.toString()));
        t = t.parse(new StreamTokenizer_s(new StringReader(t4.toString())));
        System.out.println(t4.toString().equals(t.toString()));
        t = t.parse(new StreamTokenizer_s(new StringReader(t5.toString())));
        System.out.println(t5.toString().equals(t.toString()));
        t = t.parse(new StreamTokenizer_s(new StringReader(t6.toString())));
        System.out.println(t6.toString().equals(t.toString()));
    }
    
    /** ***************************************************************
     * Test if the classification function works as expected.  
     */
    public void testIsVar() {

        System.out.println("---------------------");
        System.out.println("INFO in testIsVar(): first true, rest false");
        System.out.println(t1.termIsVar());
        System.out.println(t2.termIsVar());
        System.out.println(t3.termIsVar());
        System.out.println(t4.termIsVar());
        System.out.println(t5.termIsVar());
        System.out.println(t6.termIsVar());
    }
    
    /** ***************************************************************
     * Test if the classification function works as expected.  
     */
    public void testIsCompound() {
        
        System.out.println("---------------------");
        System.out.println("INFO in testIsCompound(): first false, rest true");
        System.out.println(t1.termIsCompound());
        System.out.println(t2.termIsCompound());
        System.out.println(t3.termIsCompound());
        System.out.println(t4.termIsCompound());
        System.out.println(t5.termIsCompound());
        System.out.println(t6.termIsCompound());
    }
    
    /** ***************************************************************
     * Test if term equality works as expected.
     */
    public void testEquality() {
        
        System.out.println("---------------------");
        System.out.println("INFO in testEquality(): first ones true, last two false");
        System.out.println(t1.equals(t1));
        System.out.println(t2.equals(t2));
        System.out.println(t3.equals(t3));
        System.out.println(t4.equals(t4));
        System.out.println(t5.equals(t5));
        System.out.println(t6.equals(t6));
        System.out.println(t4.equals(t5));
        System.out.println(t1.equals(t4));
        System.out.println(t3.equals(t4));
    }
    
    /** ***************************************************************
     * Test if term copying works. 
     */
    public void testCopy() {

        System.out.println("---------------------");
        System.out.println("INFO in testCopy(): all true");
        Term t = new Term();
        t = t1.termCopy();
        System.out.println(t.equals(t1));
        t = t2.termCopy();
        System.out.println(t.equals(t2));
        t = t3.termCopy();
        System.out.println(t.equals(t3));
        t = t4.termCopy();
        System.out.println(t.equals(t4));
        t = t5.termCopy();
        System.out.println(t.equals(t5));
        t = t6.termCopy();
        System.out.println(t.equals(t6));
    }
    
    /** ***************************************************************
     * Test method for this class.  
     */
    public static void main(String[] args) {
        
        Parser p = new Parser();
        p.setupTests();
        p.parseTest();
        p.testToString();
        p.testIsVar();
        p.testIsCompound();
        p.testEquality();
        p.testCopy();
    }
        
}
