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

import java.io.*;
import java.util.*;

/** ***************************************************************
A composite term f(t1, ..., tn) is represented by the list
[f lt1, ..., ltn], where lt1, ..., ltn are lists representing the
subterms.
"X"          -> "X"
"g(X, f(Y))" -> ["g", "X", ["f", "Y"]]
"g(a,b)"      -> ["g", ["a"], ["b"]]
*/
public class Term {
    
public String t = "";  // lowercase is a constant, uppercase is a variable
public ArrayList<Term> subterms = new ArrayList<Term>();    // empty if not composite

    /** ***************************************************************
     * @param s An input Object, expected to be a String.
     * @return true if s == null or s is an empty String, else false.
     */
    public static boolean emptyString(Object s) {
    
        return ((s == null)
                || ((s instanceof String)
                    && s.equals("")));
    }
    
    /** ***************************************************************
     */
    public String toString() {
            
        StringBuffer result = new StringBuffer();
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
     * This routine sets up the StreamTokenizer_s so that it parses TPTP.
     */
    public static void setupStreamTokenizer(StreamTokenizer_s st) {

        st.whitespaceChars(0,32);
        st.ordinaryChars(33,35);   // !"#
        st.wordChars(36,36);       // $
        st.ordinaryChars(37,45);   // %&'()*+,-
        st.ordinaryChars(46,47);   // ./
        st.wordChars(48,57);       // 0-9
        st.ordinaryChars(58,59);   // :;
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
        st.commentChar('%');
        st.eolIsSignificant(false);
    }
    
    /** ***************************************************************
     */
    public Term parseTermList(Lexer lex) {
               
        try {
            //System.out.println("in Term.parseTermList(): " + lex.literal);
            Term newT = new Term();            
            subterms.add(newT.parse(lex));
            lex.next();
            while (lex.literal.equals(",")) {
                newT = new Term();
                subterms.add(newT.parse(lex));
                lex.next();
                //System.out.println("in Term.parseTermList(): next token: " + lex.literal);
            }
            return this;
        }
        catch (Exception ex) {
            System.out.println("Error in Term.parseTermList(): " + ex.getMessage());
            System.out.println("Error in Term.parseTermList(): token:" + lex.literal);            
            ex.printStackTrace();
        }
        return null;
    }  
    
    /** ***************************************************************
     * This routine expects the tokenizer to be set before the starting token.
     * A term is either a variable or a function, where a function can have
     * 0 arguments, and therefore be just a constant.
     */
    public Term parse(Lexer lex) {
               
        try {   
            //System.out.println("INFO in Term.parse(): before next token: " + lex.literal);
            lex.next(); 
            //if (!lex.type.equals(Lexer.IdentLower) && !lex.type.equals(Lexer.IdentUpper))
                //lex.next();
            //System.out.println("INFO in Term.parse(): after next token: " + lex.literal);
            if (!lex.type.equals(Lexer.IdentLower) && !lex.type.equals(Lexer.IdentUpper))
                throw new Exception("Expected a word."); 
            if (lex.type.equals(Lexer.IdentUpper)) {
                t = lex.literal;
                return this;
            }
            else {
                if (lex.type.equals(Lexer.IdentLower)) {
                    //System.out.println("lower case term: " + lex.literal);  
                    t = lex.literal;
                    if (lex.look().equals("(")) {
                        lex.next();
                        parseTermList(lex); 
                        if (!lex.literal.equals(")"))
                            throw new Exception("Close paren expected.");
                        //System.out.println("INFO in Term.parse(): got close paren: " + lex.literal);
                        return this;
                    }
                    else {
                        return this;
                    }
                }
                else {
                    if (lex.literal.equals("$false"))
                        t = lex.literal;
                    else
                        if (lex.type == Lexer.EOFToken)
                            return this;
                        else
                            throw new Exception("Identifier " + lex.literal + " with type " + lex.type + 
                                    " doesn't start with upper or lower case letter."); 
                }                  
            }                
        }
        catch (Exception ex) {
            if (lex.literal == lex.EOFToken)
                return this;
            System.out.println("Error in Term.parse(): " + ex.getMessage());
            System.out.println("Error in Term.parse(): word token:" + lex.literal); 
            ex.printStackTrace();
        }
        return this;
    }  
    
    /** ***************************************************************
     */
    public static Term string2Term(String s) {
        
        Term t = new Term();
        Lexer lex = new Lexer(s);
        return t.parse(lex);
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
        
        if (!Term.emptyString(t) && Character.isUpperCase(t.charAt(0)))
            return false;
        for (int i = 0; i < subterms.size(); i++)
            if (!subterms.get(i).termIsGround())
                return false;
        return true;
    }
    
    /** ***************************************************************
     */
    public ArrayList<Term> collectVars() {
        
        ArrayList<Term> result = new ArrayList<Term>();
        if (termIsVar())
            result.add(this);
        for (int i = 0; i < subterms.size(); i++)
            result.addAll(subterms.get(i).collectVars());
        return result;
    }
    
    /** ***************************************************************
     */
    public String getFunc() {
               
        return t;
    }
    
    /** ***************************************************************
     */
    public ArrayList<Term> getArgs() {
               
        return subterms;
    }
    
    /** ***************************************************************
     * Return the weight of the term,  counting fweight for each function symbol
     * occurrence, vweight for each variable occurrence. Examples: 
     *                  termWeight(f(a,b), 1, 1) = 3
     *                  termWeight(f(a,b), 2, 1) = 6
     *                  termWeight(f(X,Y), 2, 1) = 4
     *                  termWeight(X, 2, 1)      = 1
     *                  termWeight(g(a), 3, 1)   = 6
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
     * Return the subterm of t at position pos (or None if pos is not a 
     * position in term). pos is a list of integers denoting branches, e.g.
     *                 subterm(f(a,b), [])        = f(a,b)
     *                 subterm(f(a,g(b)), [0])    = a
     *                 subterm(f(a,g(b)), [1])    = g(b)
     *                 subterm(f(a,g(b)), [1,0])  = b
     *                 subterm(f(a,g(b)), [3,0])  = None
     * Note that pos will be destroyed.
     */
    public Term subterm(ArrayList<Integer> pos) {
                    
        if (pos.size() == 0)
            return this;
        int index = pos.remove(0).intValue();
        if (index >= subterms.size())
            return null;
        if (pos.size() == 0)
            return subterms.get(index);
        else
            return subterms.get(index).subterm(pos);
    }
    
    /** ***************************************************************
     */
    @Override public boolean equals(Object other_obj) {
        
        assert other_obj != null : "Term.equals() argument is null";
        assert !other_obj.getClass().getName().equals("Term") : "Term.equals() passed object not of type Term"; 
        Term t2 = (Term) other_obj;
        //System.out.println("INFO in Term.equals(): term:" + this + " other: " + other);
        if (!t2.t.equals(t))
            return false;
        if (t2.subterms.size() != subterms.size())
            return false;
        for (int i = 0; i < subterms.size(); i++)
            if (!subterms.get(i).equals(t2.subterms.get(i)))
                return false;
        return true;
    }
    
    /** ***************************************************************
     */
    @Override public int hashCode() {
    
        int total = 0;
 //       if (negated) 
 //           total = 1;
        if (subterms.size() < 1)
            return total + t.hashCode() * 2;
        else {
            for (int i = 0; i < subterms.size(); i++)
                total = total + subterms.get(i).hashCode();
            return total;
        }
    }
    
    /** ***************************************************************
     */
    public Term deepCopy() {
        
        Term result = new Term();
        result.t = t;
        for (int i = 0; i < subterms.size(); i++)
            result.subterms.add(subterms.get(i).deepCopy());
        return result;
    }
    
    /** ***************************************************************
     * ************ UNIT TESTS *****************
     * Set up test content.  
     */
    String example1 = "X";
    String example2 = "a";
    String example3 = "g(a,b)";
    String example4 = "g(X, f(Y))";     
    String example5 = "g(X, f(Y))";    
    String example6 = "f(X,g(a,b))";    
    String example7 = "g(X)";   

    Term t1 = null;
    Term t2 = null;
    Term t3 = null;
    Term t4 = null;
    Term t5 = null;
    Term t6 = null;
    Term t7 = null;
    
    /** ***************************************************************
     * Set up test content.  
     */
    public void setupTests() {       
        
        t1 = string2Term(example1);
        t2 = string2Term(example2);
        t3 = string2Term(example3);
        t4 = string2Term(example4);
        t5 = string2Term(example5);
        t6 = string2Term(example6);
        t7 = string2Term(example7);
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
        System.out.println(t7 + " = " + example7);
    }
    
    /** ***************************************************************
     * Test that parse() and toString() are dual. Start with terms, 
     * so that we are sure to get the canonical string representation.   
     */
    public void testToString() {

        System.out.println("---------------------");
        System.out.println("INFO in Term.testToString(): all should be true");
        Term t = new Term();
        t = string2Term(t1.toString());
        System.out.println(t1.toString().equals(t.toString()));
        t = new Term();
        t = string2Term(t2.toString());
        System.out.println(t2.toString().equals(t.toString()));
        t = new Term();
        t = string2Term(t3.toString());
        System.out.println(t3.toString().equals(t.toString()));
        t = new Term();
        t = string2Term(t4.toString());
        System.out.println(t4.toString().equals(t.toString()));
        t = new Term();
        t = string2Term(t5.toString());
        System.out.println(t5.toString().equals(t.toString()));
        t = new Term();
        t = string2Term(t6.toString());
        System.out.println(t6.toString().equals(t.toString()));
        t = new Term();
        t = string2Term(t7.toString());
        System.out.println(t7.toString().equals(t.toString()));
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
        t = t1.deepCopy();
        System.out.println(t.equals(t1));
        t = t2.deepCopy();
        System.out.println(t.equals(t2));
        t = t3.deepCopy();
        System.out.println(t.equals(t3));
        t = t4.deepCopy();
        System.out.println(t.equals(t4));
        t = t5.deepCopy();
        System.out.println(t.equals(t5));
        t = t6.deepCopy();
        System.out.println(t.equals(t6));
    }
    
    /** ***************************************************************
     * Test term weight function
     */
    public void testTermWeight() {

        System.out.println("---------------------");
        System.out.println("INFO in testTermWeight()");
        System.out.println("Expected: 3 actual: " + t3.termWeight(1,1));
        System.out.println("Expected: 6 actual: " + t3.termWeight(2,1));
        System.out.println("Expected: 1 actual: " + t1.termWeight(2,1));
    }
    
    /** ***************************************************************
     * Test subterm function
     */
    public void testSubTerm() {

        // t6 = "f(X,g(a,b))";
        System.out.println("---------------------");
        System.out.println("INFO in testSubTerm()");
        ArrayList<Integer> al = new ArrayList<Integer>();
        System.out.println("Expected: f(X,g(a,b)) actual: " + t6.subterm(al));
        al.add(new Integer(0));
        System.out.println("Expected: X actual: " + t6.subterm(al));
        al = new ArrayList<Integer>();
        al.add(new Integer(1));
        System.out.println("Expected: g(a,b) actual: " + t6.subterm(al));
        al = new ArrayList<Integer>();
        al.add(new Integer(1));
        al.add(new Integer(0));
        System.out.println("Expected: a actual: " + t6.subterm(al));
        al = new ArrayList<Integer>();
        al.add(new Integer(3));
        al.add(new Integer(0));
        System.out.println("Expected: null actual: " + t6.subterm(al));
    }

    /** ***************************************************************
     * Test method for this class.  
     */
    public static void main(String[] args) {
        
        Term p = new Term();
        p.setupTests();
        p.parseTest();
        p.testToString();
        p.testIsVar();
        p.testIsCompound();
        p.testEquality();
        p.testCopy();
        p.testTermWeight();
        p.testSubTerm();
    }
}
