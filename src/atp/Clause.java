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
import java.io.*;
import java.util.*;
import java.text.*;

/** ***************************************************************
*/
public class Clause {

    public static int clauseIDcounter = 0;
    public ArrayList<Literal> literals = new ArrayList<Literal>(); 
    private String type = "plain";
    public String name = "";
    public ArrayList<String> support = new ArrayList<String>();  // clauses from which this clause is derived
    public String rationale = "input";                           // if not input, reason for derivation
    public ArrayList<Integer> evaluation = null;  // Must be the same order as clause evaluation function list in EvalStructure
    
    /** ***************************************************************
     * Print for use by GraphViz.  Convert vertical bar to HTML code and
     * just print the formula with no informational wrapper.
     */
    public String toString(boolean forDot) {
            
        if (!forDot)
            return toString();
        else {
            String temp = Literal.literalList2String(literals);
            return temp.replaceAll("\\|", "&#124;");
        }
    }
    
    /** ***************************************************************
     */
    public String toString() {
            
        StringBuffer result = new StringBuffer();
        result.append("cnf(" + name + "," + type + "," + 
                Literal.literalList2String(literals) + ").");
        return result.toString();
    }
    
    /** ***************************************************************
     * Create a string representation of the Clause with reference to
     * an inference rule and its supporting axioms if it was generated
     * in inference.
     */
    public String toStringJustify() {
            
        StringBuffer result = new StringBuffer();
        result.append("cnf(" + name + "," + type + "," + 
                Literal.literalList2String(literals) + ").");
        if (support.size() > 0) {
            result.append(" : " + rationale + "[");
            result.append(support.get(0));
            for (int i = 1; i < support.size(); i++) {
                result.append(",");
                result.append(support.get(i));
            }
            result.append("]");
        }
        return result.toString();
    }
    
    /** ***************************************************************
     */
    public void createName() {
            
        name = "c" + Integer.toString(clauseIDcounter);
        clauseIDcounter++;
    }
    
    /** ***************************************************************
     */
    public void addEval(ArrayList<Integer> e) {
            
        evaluation = e;
    }

    /** ***************************************************************
     */
    public Clause deepCopy() {
                
        return deepCopy(0);
    }
    
    /** ***************************************************************
     * @param start is the starting index of the literal list to copy
     */
    public Clause deepCopy(int start) {
        
        Clause result = new Clause();
        result.name = name;
        result.type = type;
        result.rationale = rationale;
        for (int i = 0; i < support.size(); i++)  
            result.support.add(support.get(i));
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
    public Clause parse(Lexer lex) throws ParseException {
               
        try {
            //System.out.println("INFO in Clause.parse(): " + lex.literal);
            lex.next(); 
            //if (st.ttype == '%')
            //    return this;
            if (!lex.literal.equals("cnf"))
                throw new Exception("\"cnf\" expected.");
            lex.next(); 
            if (!lex.type.equals(Lexer.OpenPar))
                throw new Exception("Open paren expected. Instead found '" + lex.literal + "' with clause so far " + this);
            lex.next(); 
            if (lex.type == Lexer.IdentLower)
                name = lex.literal;
            else 
                throw new Exception("Identifier expected.");
            lex.next(); 
            if (!lex.type.equals(Lexer.Comma))
                throw new Exception("Comma expected. Instead found '" + lex.literal + "' with clause so far " + this);
            lex.next(); 
            if (lex.type == Lexer.IdentLower) {                 
                type = lex.literal;
                //if (!type.equals("axiom") && !type.equals("negated_conjecture"))
                //   type = "plain";
            }
            else 
                throw new Exception("Clause type enumeration expected.");
            lex.next(); 
            if (!lex.type.equals(Lexer.Comma))
                throw new Exception("Comma expected. Instead found '" + lex.literal + "' with clause so far " + this);
            String s = lex.look(); 
            //System.out.println("INFO in Clause.parse() (2): found token: " + s);
            if (s.equals(Lexer.OpenPar)) {
                //System.out.println("INFO in Clause.parse(): found open paren at start of bare clause");
                lex.next();
                literals = Literal.parseLiteralList(lex);
                lex.next(); 
                if (!lex.type.equals(Lexer.ClosePar))
                    throw new Exception("Literal list close paren expected. Instead found '" + lex.literal + "' with clause so far " + this);
            }
            else {
                literals = Literal.parseLiteralList(lex);
            }

            lex.next();
            if (!lex.type.equals(Lexer.ClosePar))
                throw new Exception("Clause close paren expected. Instead found '" + lex.literal + "' with clause so far " + this);
            lex.next(); 
            if (!lex.type.equals(Lexer.FullStop))
                throw new Exception("Period expected. Instead found '" + lex.literal + "' with clause so far " + this);
            //System.out.println("INFO in Clause.parse(): completed parsing: " + this);
            return this;
        }
        catch (Exception ex) {
            Prover2.errors = "input error";
            if (lex.type == Lexer.EOFToken)
                return this;
            System.out.println("Error in Clause.parse(): " + ex.getMessage());
            System.out.println("Error in Term.parseTermList(): token:" + lex.literal);  
            ex.printStackTrace();
            throw (new ParseException("input error",0));
        }
    }  
    
    /** ***************************************************************
     */
    public Clause string2Clause(String s) throws ParseException {
    
        Lexer lex = new Lexer(s);
        return parse(lex);
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

        Clause newC = deepCopy();
        newC.literals = new ArrayList<Literal>();
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
            for (int j = 1; j < literals.size(); j++) {
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

        str1 = "cnf(test1,axiom,p(a)|p(f(X))).\n" +
               "cnf(test2,axiom,(p(a)|p(f(X)))).\n" +
               "cnf(test3,lemma,(p(a)|~p(f(X)))).\n" +
               "cnf(taut,axiom,p(a)|q(a)|~p(a)).\n" +
               "cnf(dup,axiom,p(a)|q(a)|p(a)).\n" +
               "cnf(c6,axiom,f(f(X1,X2),f(X3,g(X4,X5)))!=f(f(g(X4,X5),X3),f(X2,X1))|k(X1,X1)!=k(a,b)).\n";       
    }
    
    /** ***************************************************************
     *  Test that basic literal parsing works correctly.
     */
    public static void testClauses() {     

        System.out.println("INFO in Clause.testClauses(): expected results: \n" + str1);
        System.out.println("results:");
        Lexer lex = new Lexer(str1);
                
        try {
        Clause c1 = new Clause();        
        c1.parse(lex);
        assert c1.toString().equals("cnf(test1,axiom,p(a)|p(f(X))).") : 
               "Failure. " + c1.toString() + " not equal to cnf(test1,axiom,p(a)|p(f(X))).";
        System.out.println("c1: " + c1);
        
        Clause c2 = new Clause();
        c2.parse(lex);
        assert c2.toString().equals("cnf(test2,axiom,(p(a)|p(f(X)))).") : 
            "Failure. " + c2.toString() + " not equal to cnf(test2,axiom,(p(a)|p(f(X)))).";
        System.out.println("c2: " + c2);
        
        Clause c3 = new Clause();
        c3.parse(lex);
        assert c3.toString().equals("cnf(test3,lemma,(p(a)|~p(f(X)))).") : 
            "Failure. " + c3.toString() + " not equal to cnf(test3,lemma,(p(a)|~p(f(X)))).";
        System.out.println("c3: " + c3);
        
        Clause c4 = new Clause();
        c4.parse(lex);
        assert c4.toString().equals("cnf(taut,axiom,p(a)|q(a)|~p(a)).") : 
            "Failure. " + c4.toString() + " not equal to cnf(taut,axiom,p(a)|q(a)|~p(a)).";
        System.out.println("c4: " + c4);
        
        Clause c5 = new Clause();
        c5.parse(lex);
        assert c5.toString().equals("cnf(dup,axiom,p(a)|q(a)|p(a)).") : 
            "Failure. " + c5.toString() + " not equal to cnf(dup,axiom,p(a)|q(a)|p(a)).";
        System.out.println("c5: " + c5);
  
        Clause c6 = new Clause();
        c6.parse(lex);
        assert c6.toString().equals("cnf(c6,axiom,(f(f(X1,X2),f(X3,g(X4,X5)))!=f(f(g(X4,X5),X3),f(X2,X1))|k(X1,X1)!=k(a,b))).") : 
            "Failure. " + c6.toString() + " not equal to cnf(c6,axiom,(f(f(X1,X2),f(X3,g(X4,X5)))!=f(f(g(X4,X5),X3),f(X2,X1))|k(X1,X1)!=k(a,b))).";
        System.out.println("c6: " + c6);
        }
        catch (ParseException p) {
            System.out.println(p.getMessage());
        }
    }
    
    /** ***************************************************************
     * Test method for this class.  
     */
    public static void main(String[] args) {
        
        setup();
        testClauses();
    }

}
