package atp;

/*
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

import java.io.*;
import java.text.ParseException;

/** ***************************************************************
 * Datatype for the complete first-order formula, including 
 * meta-information like type and name.
 */
public class Formula {

    // Counter for generating new clause names.
    public static int formulaIdCounter = 0;
    public BareFormula form = null;
    public String type = "plain";
    public String name = "";

    /** ***************************************************************
     * Return a string representation of the formula.
     */
    public String toString() {

        return "fof(" + name + "," + type + "," + form + ").";
    }
    
    /** ***************************************************************
     * Set the name. If no name is given, generate a default name.
     */
    public void setName(String n) {

        if (!Term.emptyString(n))
            name = n;
        else {
            name = "f" + Integer.toString(formulaIdCounter);
            formulaIdCounter++;        
        }
    }

    /** ***************************************************************
     * Parse a formula in (slightly simplified) TPTP-3 syntax. It is
     *  written 
     *      fof(<name>, <type>, <lformula>).
     *  where <name> is a lower-case ident, type is a lower-case ident
     *  from a specific list, and <lformula> is a Formula.
     *  
     *  For us, all clause types are essentially the same, so we only
     *  distinguish "axiom", "conjecture", and "negated_conjecture", and
     *  map everything else to "plain".
     */
    public static Formula parse(StreamTokenizer_s st) 
    throws IOException, ParseException {

        st.nextToken();
        if (!st.sval.equals("fof"))
            throw new ParseException("Error in Formula.parse(): expected 'fof', found " + st.sval,0);
        st.nextToken();
        if (st.ttype != '(')
            throw new ParseException("Error in Formula.parse(): expected '(', found " + st.ttype,0);
        st.nextToken();
        String name = st.sval;
        if (!Character.isLowerCase(name.charAt(0)))
            throw new ParseException("Error in Formula.parse(): expected lower case identifier, found " + st.sval,0);
        st.nextToken();
        if (st.ttype != ',')
            throw new ParseException("Error in Formula.parse(): expected ',', found " + st.ttype,0);

        st.nextToken();
        String type = st.sval;
        if (!type.equals("axiom") && !type.equals("conjecture") && !type.equals("negated_conjecture"))
            type = "plain";
        st.nextToken();
        if (st.ttype != ',')
            throw new ParseException("Error in Formula.parse(): expected ',', found " + st.ttype,0);

        BareFormula bform = BareFormula.parse(st);
        
        //st.nextToken();
        if (st.ttype != ')')
            throw new ParseException("Error in Formula.parse(): expected ')', found " + st.ttype,0);
        st.nextToken();
        if (st.ttype != '.')
            throw new ParseException("Error in Formula.parse(): expected '.', found " + st.ttype,0);

        Formula f = new Formula();
        f.form = bform;
        f.name = name;
        f.type = type;
        return f;
    }
    
    /** ***************************************************************
     * Setup function for clause/literal unit tests. Initialize
     * variables needed throughout the tests.
     */
    public static String wformulas = "fof(small, axiom, ![X]:(a(x) | ~a=b))." + 
        "fof(complex, conjecture, (![X]:a(X)|b(X)|?[X,Y]:(p(X,f(Y))))<=>q(g(a),X))." + 
        "fof(clean, conjecture, ((((![X]:a(X))|b(X))|(?[X]:(?[Y]:p(X,f(Y)))))<=>q(g(a),X))).";
            
    /** ***************************************************************
     */
    public static void testWrappedFormula() {
        
        try {
            StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(wformulas));
            Term.setupStreamTokenizer(st);      
            Formula f1 = Formula.parse(st);
            System.out.println(f1);
            Formula f2 = Formula.parse(st);
            System.out.println(f2);
            Formula f3 = Formula.parse(st);
            System.out.println(f3);
        }
        catch (Exception e) {
            System.out.println("Error in Formula.testWrappedFormula()");
            System.out.println(e.getMessage());
            e.printStackTrace();            
        }
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {
        
        testWrappedFormula();
    }
}
