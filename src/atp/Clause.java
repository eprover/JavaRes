/*
A simple implementation of first-order clauses.

See Literal.java for the definition of atoms and literals.

A logical clause in our sense is a multi-set of literals, implicitly
representing the universally quantified disjunction of these literals.

The set of all clauses for a given signature is denoted as
Clauses(P,F,V).

We represent a clause as a list of literals. The actual clause data
structure contains additional information that is useful, but not
strictly necessary from a logic/calculus point of view.

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

/** ***************************************************************
*/
public class Clause {

    ArrayList<Literal> literals = new ArrayList<Literal>(); 
    String type = "";
    String name = "";
    // evaluation = None
    
    /** ***************************************************************
     */
    public String toString() {
            
        StringBuffer result = new StringBuffer();
        result.append("cnf(" + name + "," + type + "," + 
                Literal.literalList2String(literals) + ")");
        return result.toString();
    }
    
    /** ***************************************************************
     * Parse a clause. A clause in (slightly simplified) TPTP-3 syntax 
     * is written as
     *    cnf(<name>, <type>, <literal list>).
     * where <name> is a lower-case ident, type is a lower-case ident
     * from a specific list, and <literal list> is a "|" separated list
     * of literals, optionally enclosed in parenthesis.
     * For us, all clause types are essentially the same, so we only
     * distinguish "axiom", "negated_conjecture", and map everything else
     * to "plain".
     */
    public Clause parse(StreamTokenizer_s st) {
               
        try {
             st.nextToken(); 
             if (st.ttype != StreamTokenizer.TT_WORD || !st.sval.equals("cnf"))
                 throw new Exception("\"cnf\" expected.");
             st.nextToken(); 
             if (st.ttype != '(')
                 throw new Exception("Open paren expected.");
             st.nextToken(); 
             if (st.ttype == StreamTokenizer.TT_WORD && Character.isLowerCase(st.sval.charAt(0)))
                 name = st.sval;
             else 
                 throw new Exception("Identifier expected.");
             st.nextToken(); 
             if (st.ttype != ',')
                 throw new Exception("Comma expected.");
             st.nextToken(); 
             if (st.ttype == StreamTokenizer.TT_WORD && Character.isLowerCase(st.sval.charAt(0))) {                 
                 type = st.sval;
                 if (!type.equals("axiom") && !type.equals("negated_conjecture"))
                     type = "plain";
             }
             else 
                 throw new Exception("Clause type enumeration expected.");
             st.nextToken(); 
             if (st.ttype != ',')
                 throw new Exception("Comma expected.");
             st.nextToken(); 
             st.pushBack();
             if (st.ttype == '(') {
                 st.nextToken();
                 literals = Literal.parseLiteralList(st);
                 st.nextToken(); 
                 if (st.ttype != ')')
                     throw new Exception("Close paren expected.");
             }
             else
                 literals = Literal.parseLiteralList(st);
             //st.nextToken(); 
             if (st.ttype != ')')
                 throw new Exception("Close paren expected.");
             st.nextToken(); 
             if (st.ttype != '.')
                 throw new Exception("Period expected.");
             return this;
        }
        catch (Exception ex) {
            if (st.ttype == StreamTokenizer.TT_EOF)
                return this;
            System.out.println("Error in Clause.parse(): " + ex.getMessage());
            if (st.ttype == StreamTokenizer.TT_WORD)
                System.out.println("Error in Clause.parse(): word token:" + st.sval); 
            else
                System.out.println("Error in Clause.parse(): token:" + st.ttype);
            ex.printStackTrace();
        }
        return this;
    }  
    
    /** ***************************************************************
     */
    public static String str1 = "";
    
    /** ***************************************************************
     *  Setup function for clause/literal unit tests. Initialize
     *  variables needed throughout the tests.
     */
    public static void setup() {

        str1 = "cnf(test,axiom,p(a)|p(f(X))).\n" +
               "cnf(test,axiom,(p(a)|p(f(X)))).\n" +
               "cnf(test3,lemma,(p(a)|~p(f(X)))).\n" +
               "cnf(taut,axiom,p(a)|q(a)|~p(a)).\n" +
               "cnf(dup,axiom,p(a)|q(a)|p(a)).";
    }
    
    /** ***************************************************************
     *  Test that basic literal parsing works correctly.
     */
    public static void testClauses() {     

        StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(str1));
        Clause c1 = new Clause();
        c1.parse(st);
        System.out.println(c1);
        Clause c2 = new Clause();
        c2.parse(st);
        System.out.println(c2);
        Clause c3 = new Clause();
        c3.parse(st);
        System.out.println(c3);
        Clause c4 = new Clause();
        c4.parse(st);
        System.out.println(c4);
        Clause c5 = new Clause();
        c5.parse(st); 
        System.out.println(c5);
    }
    
    /** ***************************************************************
     * Test method for this class.  
     */
    public static void main(String[] args) {
        
        setup();
        testClauses();
    }

}
