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
                Literal.literalList2String(literals) + ").");
        return result.toString();
    }

    /** ***************************************************************
     */
    public Clause deepCopy() {
        
        Clause result = new Clause();
        result.name = name;
        result.type = type;
        for (int i = 0; i < literals.size(); i++) 
            result.literals.add(literals.get(i).deepCopy());
        return result;
    }
    
    /** ***************************************************************
     * @param start is the starting index of the literal list to copy
     */
    public Clause deepCopy(int start) {
        
        Clause result = new Clause();
        result.name = name;
        result.type = type;
        for (int i = start; i < literals.size(); i++) 
            result.literals.add(literals.get(i).deepCopy());
        return result;
    }
 
    /** ***************************************************************
     */
    public int length() {
               
        return literals.size();
    }
    
    /** ***************************************************************
     */
    public void add(Literal l) {
        
        literals.add(l);
    }
    
    /** ***************************************************************
     */
    public void addAll(ArrayList<Literal> l) {
        
        literals.addAll(l);
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
     * @return the parsed clause.  Note also that this is the side effect 
     * on the clause instance
     */
    public Clause parse(StreamTokenizer_s st) {
               
        try {
            Term.setupStreamTokenizer(st);
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
                System.out.println("Error in Term.parseTermList(): token:" + st.ttype + " " + Character.toString((char) st.ttype));  
            ex.printStackTrace();
        }
        return this;
    }  
    
    /** ***************************************************************
     */
    public Clause string2Clause(String s) {
    
        StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(s));
        Term.setupStreamTokenizer(st);
        return parse(st);
    }
    
    /** ***************************************************************
     * Return true if the clause is empty.
     */
    public boolean isEmpty() {

        return literals.size() == 0;
    }

    /** ***************************************************************
     * Return true if the clause is a unit clause.
     */
    public boolean isUnit() {

        return literals.size() == 1;
    }
    
    /** ***************************************************************
     * Return true if the clause is a Horn clause.
     */
    public boolean isHorn() {

        ArrayList<Literal> tmp = new ArrayList<Literal>();
        for (int i = 0; i < literals.size(); i++) 
            if (literals.get(i).isPositive())
                tmp.add(literals.get(i));
        return tmp.size() <= 1;
    }
    
    /** ***************************************************************
     * Return the indicated literal of the clause. Position is an
     * integer from 0 to litNumber (exclusive).
     */
    public Literal getLiteral(int position) {

        if (position >= 0 && position < literals.size())
            return literals.get(position);
        else
            return null;
    }
    
    /** ***************************************************************
     * Insert all variables in self into the set res and return it. 
     */
    public ArrayList<Term> collectVars() {

        ArrayList<Term> res = new ArrayList<Term>();
        for (int i = 0; i < literals.size(); i++)
            res.addAll(literals.get(i).collectVars());
        return res;
    }
    
    /** ***************************************************************
     * Return the symbol-count weight of the clause.
     */
    public int weight(int fweight, int vweight) {

        int res = 0;
        for (int i = 0; i < literals.size(); i++)
            res = res + literals.get(i).weight(fweight, vweight);
        return res;
    }
    
    /** ***************************************************************
     * Return an instantiated copy of self. Name and type are copied
     * and need to be overwritten if that is not desired.
     */
    public Clause instantiate(Substitutions subst) {

        Clause newC = new Clause();
        newC.type = type;
        newC.name = name;
        for (int i = 0; i < literals.size(); i++)
            newC.literals.add(literals.get(i).instantiate(subst));
        return newC;
    }
    
    /** ***************************************************************
     * Return a copy of self with fresh variables.
     */
    public Clause freshVarCopy() {

        ArrayList<Term> vars = collectVars();
        Substitutions s = Substitutions.freshVarSubst(vars);
        return instantiate(s);
    }
    
    /** ***************************************************************
     * Remove duplicated literals from clause.
     */
    public void removeDupLits() {

        ArrayList<Literal> res = new ArrayList<Literal>();
        for (int i = 0; i < literals.size(); i++) {
            if (!Literal.litInLitList(literals.get(i),res))
                res.add(literals.get(i));
        }
        literals = res;
    }

    /** ***************************************************************
     * Check if a clause is a simple tautology, i.e. if it contains
     * two literals with the same atom, but different signs.
     */
    public boolean isTautology() {

        if (literals.size() < 2)
            return false;
        for (int i = 0; i < literals.size(); i++) {
            for (int j = 1; j < literals.size(); i++) {
                if (literals.get(i).isOpposite(literals.get(j)))
                    return true;
            }
        }
        return false;     
    }
    
    /** ***************************************************************
     * ************ UNIT TESTS *****************
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
