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
import java.util.*;

/** ***************************************************************
    A simple implementation of first-order formulas and their associated
    meta-information. 

    See literals.py for the definition of atoms.

    A formula is either a first-order-atom, or build from pre-existing
    formulas using the various logical connectives and quantifiers:

    Assume F,G are arbitrary formulas and X is an arbitrary variable. Then

    (~F)
    (F&G)
    (F|G)
    (F->G)
    (F<=>G)
    (F<~>G)
    (F<-G)
    (F~&G)
    (F~|G)
    (![X]:F)
    (?[X]:F)

    are formulas.

    The set of all formulas for a given signature is denoted as
    Formulas(P,F,V).

    In the external representation, some parentheses can be omitted. Lists
    of either conjunctively or disjunctively connected subformula are
    assumed to associate left. (F & G & H) is equivalent to ((F&G)&H)

    Formulas are represented on two levels: The actual logical formula is
    a recursive data structure. This is wrapped in a container that
    associates the formula with its meta-information. The implementation
    uses literals as the base case, not atoms. That allows us to reuse
    some code for parsing and printing infix equality, but also to
    represent a formula in Negation Normal Form without any negations in
    the frame of the formula.
    
        This is a class representing a naked first-order formula
        formula. Operators are represented as strings, an empty operator
        indicates an atomic formula. child1 and child2 are the subformulas
        (child2 may be empty). In the case of atomic formula, child1 is an
        atom (representing a term). In the case of quantified formulae,
        child1 is a plain string (i.e. the term representing the variable)
        and child2 is the formula quantified over.
 */
public class BareFormula {

    public String op = "";
    public BareFormula child1 = null;
    public Literal lit1 = null;        // either child1 or lit1 must be null
    public BareFormula child2 = null;
    public Literal lit2 = null;        // either child2 or lit2 (or both) must be null
        
    public static int level = 0;
    
    /** ***************************************************************
     * a logical operator other than a quantifier or negation
     */
    private boolean logOp(String s) {
        
        return s.equals("&") || s.equals("|") || s.equals("->") || 
            s.equals("<-") || s.equals("<=>") || s.equals( "<~>") || 
            s.equals("~|") || s.equals("~&");
    }
    
    /** ***************************************************************
     */
    public static boolean isQuantifier(String s) {
        
        return s.equals("?") || s.equals("!");
    }    
    
    /** ***************************************************************
     * a logical operator other than a quantifier,  negation, 'and' or
     * 'or'
     * @return null if not one of these operators and the operator 
     * otherwise
     */
    private static String isBinaryConnective(StreamTokenizer_s st) throws IOException {

        if (st.ttype == '~') {           
            st.nextToken();
            if (st.ttype == '|' || st.ttype == '&') 
                return "~" + Character.toString((char) st.ttype);
            else
                return null;
        }
        if (st.ttype == '<') {
            st.nextToken();
            if (st.ttype == '=') {
                st.nextToken();
                if (st.ttype == '>')
                    return "<=>";
                else {
                    st.pushBack();
                    return null;
                }
            }
            if (st.ttype == '-') {
                st.nextToken();
                if (st.ttype == '>')
                    return "<->";
                else {
                    st.pushBack();
                    return "<-";
                }                    
            }                
        }
        if (st.ttype == '-') {
            st.nextToken();
            if (st.ttype == '>')
                return "->";
            else {
                st.pushBack();
                return null;
            }
        }
        return null;
    }
    
    /** ***************************************************************
     * Return a string representation of the formula.
     */
    public String toString() {

        String arg1 = null;
        if (child1 != null)        
            arg1 = child1.toString();
        if (lit1 != null)
            arg1 = lit1.toString();
        String arg2 = null;
        if (child2 != null)        
            arg2 = child2.toString();
        if (lit2 != null)
            arg2 = lit2.toString();
        
        if (Term.emptyString(op))      
            return arg1;        
        if (op.equals("~"))      
            return "(~" + arg1 + ")";        
        if (logOp(op)) 
            return "(" + arg1 + op + arg2 + ")";        
        else {
            if (!op.equals("!") && !op.equals("?")) {
                System.out.println("Error in BareFormula.toString(): bad operator: " + op);
                return null;
            }
            return "(" + op + "[" + arg1 + "]:" + arg2 + ")"; 
        }  
    }
        
    /** ***************************************************************
     * Return True if self is structurally equal to other.
     */
    public boolean equals(BareFormula other) {

        if (op != null) {
            if (!op.equals(other.op))
                return false;
        }
        else
            if (other.op != null)
                return false;
        
        if (child1 != null) {
            if (!child1.equals(other.child1))
                return false;
        }
        else
            if (other.child1 != null)
                return false;
        
        if (lit1 != null) {
            if (!lit1.equals(other.lit1))
                return false;
        }
        else
            if (other.lit1 != null)
                return false;        

        if (lit2 != null) {
            if (!lit2.equals(other.lit2))
                return false;
        }
        else
            if (other.lit2 != null)
                return false;
        
        if (child2 != null) {
            if (!child2.equals(other.child2))
                return false;
        }
        else
            if (other.child2 != null)
                return false;
        
        return true;
    }
    
    /** ***************************************************************
     */
    public BareFormula deepCopy() {
        
        if (lit1 != null && child1 != null) {
            System.out.println("Error in BareFormula.deepCopy(): lit1 & child1 are both non-null");
            return null;
        }
        if (lit2 != null && child2 != null) {
            System.out.println("Error in BareFormula.deepCopy(): lit2 & child2 are both non-null");
            return null;
        }
        BareFormula result = new BareFormula();
        result.op = op;
        if (lit1 != null)            
            result.lit1 = lit1.deepCopy();
        if (lit2 != null)
            result.lit2 = lit2.deepCopy(); 
        if (child1 != null)            
            result.child1 = child1.deepCopy();
        if (child2 != null)            
            result.child2 = child2.deepCopy();
        return result;
    }

    /** ***************************************************************
     * Substitute one variable for another
     */
    public BareFormula substitute(Substitutions subst) {
        
        //System.out.println("INFO in BareFormula.substitute(): "  + this + " " + subst);
        BareFormula result = deepCopy();
        if (child1 != null)
            result.child1 = child1.substitute(subst);
        if (child2 != null)
            result.child2 = child2.substitute(subst);
        if (lit1 != null)
            result.lit1 = lit1.instantiate(subst);
        if (lit2 != null)
            result.lit2 = lit2.instantiate(subst);        
        return result;
    }
    
    /** ***************************************************************
     * Parse the "remainder" of a formula starting with the given quantor.
     * Stream tokenizer will be pointing on the opening square bracket to start. 
     */
    public static BareFormula parseQuantified(StreamTokenizer_s st, String quantor) 
    throws ParseException, IOException {

        st.nextToken();
        st.pushBack();
        if (!Character.isUpperCase(st.sval.charAt(0)))
            throw new ParseException("expected upper case identifier, found " + st.sval,0);
        Literal var = new Literal();
        var = var.parseLiteral(st);
        BareFormula rest = null;
        if (st.ttype == ',') 
            rest = parseQuantified(st, quantor);        
        else {
            if (st.ttype != ']')
                throw new ParseException("Error in BareFormula.parseQuantified(): expected ']', found " + st.ttype,0);
            st.nextToken();
            if (st.ttype != ':')
                throw new ParseException("Error in BareFormula.parseQuantified(): expected ':', found " + st.ttype,0);
            st.nextToken();
            rest = parseUnitaryFormula(st);            
        }
        BareFormula result = new BareFormula();
        result.op = quantor;
        result.lit1 = var;
        result.child2 = rest;  // not sure if this is right
        return result;
    }

    /** ***************************************************************
     * Parse a "unitary" formula (following TPTP-3 syntax terminology). 
     * This can be the first unitary formula of a binary formula, of course.
     * It expects stream pointer to be on the first token.
     */
    public static BareFormula parseUnitaryFormula(StreamTokenizer_s st) 
    throws IOException, ParseException {

        BareFormula res = null;
        if (st.ttype == '?' || st.ttype == '!') {
            String quantor = Character.toString((char) st.ttype);
            st.nextToken();
            if (st.ttype != '[')
                throw new ParseException("Error in BareFormula.parseQuantified(): expected '[',found '" + 
                        (char) st.ttype + "'",0);
            res = parseQuantified(st, quantor);
        }
        else if (st.ttype == '(') {
            st.nextToken();                      
            res = BareFormula.parseRecurse(st);
            if (st.ttype != ')')
                throw new ParseException("Error in BareFormula.parseUnitaryFormula(): expected ')', found " + 
                        (char) st.ttype,0);
            st.nextToken();
        }
        else if (st.ttype == '~') {
            st.nextToken();
            BareFormula subform = parseUnitaryFormula(st);
            res = new BareFormula();
            res.op = "~";
            res.child1 = subform;
        }
        else {
            st.pushBack();
            Literal lit = new Literal();
            lit = lit.parseLiteral(st);  // stream pointer looks at token after literal
            res = new BareFormula();
            res.op = "";
            res.lit1 = lit;
        }
        return res;
    }

    /** ***************************************************************
     * Parse the rest of the associative formula that starts with head
     * and continues ([&|] form *).
     * It expects stream to be pointing at the operator.
     */
    public static BareFormula parseAssocFormula(StreamTokenizer_s st, BareFormula head) 
    throws IOException, ParseException {

        String op = Character.toString((char) st.ttype);
        while (op.equals(Character.toString((char) st.ttype))) {
            st.nextToken();
            BareFormula next = parseUnitaryFormula(st);
            BareFormula newhead = new BareFormula();
            newhead.op = op;
            newhead.child1 = head;
            newhead.child2 = next;
            head = newhead;
        }
        return head;
    }

    /** ***************************************************************
     * Parse a (naked) formula.  Stream pointer must be at the start 
     * of the expression.
     */
    public static BareFormula parseRecurse(StreamTokenizer_s st) 
    throws IOException, ParseException {
      
        BareFormula res = parseUnitaryFormula(st);
        if (st.ttype == '|' || st.ttype == '&') 
            res = parseAssocFormula(st, res.deepCopy());        
        else {
            if (st.ttype != StreamTokenizer_s.TT_EOF) {
                String op = isBinaryConnective(st);
                if (op != null) {
                    st.nextToken();
                    BareFormula rest = parseUnitaryFormula(st);
                    BareFormula lhs = res.deepCopy();
                    res = new BareFormula();
                    res.child1 = lhs;
                    res.op = op;
                    res.child2 = rest;  
                }
            }
        }
        return res;
    }
    
    /** ***************************************************************
     * Parse a (naked) formula.  Stream pointer must be before the start 
     * of the expression.
     */
    public static BareFormula parse(StreamTokenizer_s st) throws IOException, ParseException {

        st.nextToken(); 
        return parseRecurse(st);
    }

    /** ***************************************************************
     * Convert a string to a BareFormula
     */
    public static BareFormula string2form(String s) {

        try {
            StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(s));
            Term.setupStreamTokenizer(st);
            return BareFormula.parse(st);
        }
        catch (Exception e) {
            System.out.println("Error in BareFormula.string2form()");
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /** ***************************************************************
     * Setup function for clause/literal unit tests. Initialize
     * variables needed throughout the tests.
     */
    public static String nformulas = "![X]:(a(x) | ~a=b)" +
            "(![X]:a(X)|b(X)|?[X,Y]:(p(X,f(Y))))<=>q(g(a),X)" +
            "((((![X]:a(X))|b(X))|(?[X]:(?[Y]:p(X,f(Y)))))<=>q(g(a),X))";
                        
    /** ***************************************************************
     * Test that basic parsing and functionality works.
     */
    public static void testNakedFormula() {

        try {
            System.out.println("INFO in BareFormula.testNakedFormula()");
            StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(nformulas));
            Term.setupStreamTokenizer(st);
            System.out.println("Parsing formula: " + nformulas);
            BareFormula f1 = BareFormula.parse(st);
            System.out.println("INFO in BareFormula.testNakedFormula(): f1: " + f1);
            System.out.println();
            
            st.pushBack();
            BareFormula f2 = BareFormula.parse(st);
            System.out.println("INFO in BareFormula.testNakedFormula(): f2: " + f2);
            System.out.println();
            st.pushBack();
            BareFormula f3 = BareFormula.parse(st);
            System.out.println("INFO in BareFormula.testNakedFormula(): f3: " + f3);
            System.out.println();
            System.out.println("all should be true:");
            System.out.println(f2 + " should be equal " + f3 + ":" + f2.equals(f3));
            System.out.println(f3 + " should be equal " + f2 + ":" + f3.equals(f2));
            System.out.println(f1 + " should not be equal " + f2 + ":" + !f1.equals(f2));
            System.out.println(f2 + " should not be equal " + f1 + ":" + !f2.equals(f1));
            
        }
        catch (Exception e) {
            System.out.println("Error in BareFormula.testNakedFormula()");
            System.out.println(e.getMessage());
            e.printStackTrace();            
        }
    }
        
    /** ***************************************************************
     */
    public static void main(String[] args) {
        
        testNakedFormula();
    }
}
