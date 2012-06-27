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
 * atom - predicate symbol with term arguments
 * literal - atom or negated atom
  */
public class Literal {
    
    public Term atom = null;  
    boolean negated = false;

    /** ***************************************************************
     */
    public Literal() {
    }
    
    /** ***************************************************************
     */
    public Literal(Term t) {
        atom = t;
    }
    
     /** ***************************************************************
      */
     public String toString() {
             
         StringBuffer result = new StringBuffer();
         if (negated)
             result.append("~");
         if (!Term.emptyString(atom.getFunc())) {
             if (atom.getFunc().equals("=") || atom.getFunc().equals("!="))
                 result.append(atom.getArgs().get(0) + atom.getFunc() + atom.getArgs().get(1));
             else
                 result.append(atom);
         }
         else 
             result.append(atom);         
         return result.toString();
     }
     
     /** ***************************************************************
      */
     public String toKIFString() {
             
         StringBuffer result = new StringBuffer();
         if (negated)
             result.append("(not ");
         if (!Term.emptyString(atom.getFunc())) {
             if (atom.getFunc().equals("="))
                 result.append("(equals " + atom.getArgs().get(0).toKIFString() + " " + atom.getArgs().get(1).toKIFString() + ")");
             else if (atom.getFunc().equals("!="))
                 result.append("(not (equals " + atom.getArgs().get(0).toKIFString() + " " + atom.getArgs().get(1).toKIFString() + "))");
             else
                 result.append(atom.getArgs().get(0));
         }
         else 
             result.append(atom.getArgs().get(0).toKIFString());  
         if (negated)
             result.append(")");
         return result.toString();
     }
     
     /** ***************************************************************
      */
     public boolean equals(Object l_obj) {
        
         if (!l_obj.getClass().getName().equals("atp.Literal")) {
         	System.out.println("# Error: Literal.equals() passed object not of type Literal:" + l_obj.getClass());
         	Exception e = new Exception("DEBUG");
         	e.printStackTrace();
         }
         Literal l = (Literal) l_obj;
         if (negated != l.negated)
             return false;
         if (!atom.equals(l.atom))
             return false;
         return true;
     }
     
     /** ***************************************************************
      * should never be called so throw an error.
      */   
     public int hashCode() {
         assert false : "Literal.hashCode not designed";
         return 0;
     }
     
     /** ***************************************************************
      */
     public Literal deepCopy() {
         
         Literal result = new Literal();         
         result.atom = atom.deepCopy();
         result.negated = negated;  
         return result;
     }
     
     /** ***************************************************************
      * Return true if the atoms of self and other are structurally 
      * identical to each other, but the sign is the opposite.
      */
     public boolean isOpposite(Literal other) {

         return this.isNegative() != other.isNegative() &&
              atom.equals(other.atom);
     }
     
     /** ***************************************************************
      */
     public Literal negate() {
         negated = !negated;
         return this.deepCopy();
     }
     
     /** ***************************************************************
      */
     public boolean isNegative() {
        
         return !isPositive();
     }
     
     /** ***************************************************************
      */
     public boolean isPositive() {
        
         return !negated;
     }
     
     /** ***************************************************************
      */
     public boolean isEquational() {
      
         return atom.getFunc().equals("=");
     }

     /** ***************************************************************
      * Return True if the atom is $true.
      */
     public boolean atomIsConstTrue() {

         return atom.getFunc().equals("$true");
     }
     
     /** ***************************************************************
      * Return True if the atom is $false.
      */
     public boolean atomIsConstFalse() {

         return atom.getFunc().equals("$false");
     }
     
     /** ***************************************************************
      * Return True if the literal is of the form $true or ~$false.
      */
     public boolean isPropTrue() {

         return ((isNegative() && atomIsConstFalse())
                 || 
                 (isPositive() && atomIsConstTrue()));
     }
     
     /** ***************************************************************
      * Return True if the literal is of the form $false or ~$true.
      */
     public boolean isPropFalse() {

         return ((isNegative() && atomIsConstTrue())
                 ||
                 (isPositive() && atomIsConstFalse()));
     }
     
     /** ***************************************************************
      */
     public ArrayList<String> getConstantStrings() {

         return atom.getConstantStrings();
     }
     
     /** ***************************************************************
      */
     public ArrayList<Term> collectVars() {
         
         return atom.collectVars();
     }
     
     /** ***************************************************************
      * Get all functions
      */
     public ArrayList<String> collectFuns() {

         return atom.collectFuns();
     }
     
     /** ***************************************************************
      * Collect function- and predicate symbols into the signature. 
      * Return the signature
      */
     public Signature collectSig(Signature sig) {

         return atom.collectSig(sig);
     }
     
     /** ***************************************************************
      * Return a copy of self, instantiated with the given substitution.
      */
     public Literal substitute(Substitutions subst) {

         //System.out.println("INFO in Literal.substitute(): "  + this + " " + subst);
         Literal newLit = deepCopy();
         newLit.atom = subst.apply(atom);
         return newLit;
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
     public int weight(int fweight, int vweight) {
                         
         return atom.weight(fweight,vweight);
     }
     
     /** ***************************************************************
      * An atom is either a conventional atom, in which case it's 
      * syntactically identical to a term, or it is an equational literal, 
      * of the form 't1=t2' or 't1!=t2', where t1 and t2 are terms.
      * In either case, we represent the atom as a first-order
      * term. Equational literals are represented at terms with faux
      * function symbols "=" and "!=". 
      * The parser must be pointing to the token before the atom.
      */
     private Literal parseAtom(Lexer lex) {
                
         //System.out.println("INFO in Literal.parseAtom(): " + lex.literal);  
         try {
             atom = new Term();
             atom.parse(lex);
             ArrayList<String> tokens = new ArrayList<String>();
             tokens.add(Lexer.EqualSign); 
             tokens.add(Lexer.NotEqualSign);
             if (lex.testTok(tokens)) {
                 // The literal is equational. We get the actual operator, '=' or '!=', followed by the
                 // other side of the (in)equation
                 String op  = lex.next();
                 Term lhs = atom;
                 Term rhs = new Term();
                 rhs = rhs.parse(lex);
                 atom = new Term(op, lhs, rhs);        
             }
             return this;
         }
         catch (Exception ex) {
             System.out.println("Error in Literal.parseAtom(): " + ex.getMessage());
             System.out.println("Error in Literal.parseAtom(): token:" + lex.type + " " + lex.literal);
             ex.printStackTrace();
         }
         return null;
     }   
     
     /** ***************************************************************
      *  Parse a literal. A literal is an optional negation sign '~', 
      *  followed by an atom. 
      *  @return the Literal.  Note that there is a side effect on this Literal.
      */
     public Literal parseLiteral(Lexer lex) {
                
         //System.out.println("INFO in Literal.parseLiteral(): " + lex.literal);  
         try {
             String s = lex.look();
             if (s == Lexer.Or) {
                 lex.next();
                 lex.next();
             }
             if (lex.type == Lexer.Negation) {
                 negated = true;
                 lex.next();   // pointer will be left on the negation
             }
             this.parseAtom(lex);
             //System.out.println("INFO in Literal.parseLiteral(): exiting with pointer at: " + lex.literal);  
             return this;
         }
         catch (Exception ex) {
             System.out.println("Error in Literal.parseLiteral(): " + ex.getMessage());
             System.out.println("Error in Literal.parseLiteral(): token:" + lex.type + " " + lex.literal);
             ex.printStackTrace();
         }
         return null;
     }   
     
     /** ***************************************************************
      *  Parse a list of literals separated by "|" (logical or). As per 
      *  TPTP 3 syntax, the single word "$false" is interpreted as the
      *  false literal, and ignored.
      */
     public static ArrayList<Literal> parseLiteralList(Lexer lex) {
                
         //System.out.println("INFO in Literal.parseLiteralList(): " + lex.literal);  
         ArrayList<Literal> res = new ArrayList<Literal>();
         try {
             Literal l = new Literal();
             if (lex.look().equals("$false"))
                 lex.next();
             else {
                 l.parseLiteral(lex);
                 res.add(l);
             }
             //if (!l.toString().equals("$false")) 
             //    res.add(l);                          
             while (lex.look().equals(Lexer.Or)) {     
                 lex.next();
                 l = new Literal();
                 if (lex.look().equals("$false"))
                     lex.next();
                 else {
                     l.parseLiteral(lex);
                     res.add(l);
                 }                                  
             }
             return res;
         }
         catch (Exception ex) {
             System.out.println("Error in parseLiteralList(): " + ex.getMessage());
             System.out.println("Error in parseLiteralList(): token:" + lex.type + " " + lex.literal);
             ex.printStackTrace();
         }
         return null;
     }   
     
     /** ***************************************************************
      *  Convert a literal list to a textual representation that can be
      *  parsed back.
      */
     public static String literalList2String(ArrayList<Literal> l) {

         StringBuffer result = new StringBuffer();
         if (l == null || l.size() < 1)
             return "$false";
         result.append(l.get(0).toString());
         for (int i = 1; i < l.size(); i++) 
              result.append("|" + l.get(i).toString());
         return result.toString();
     }
     
     /** ***************************************************************
      *  Return true if (a literal equal to) lit is in litlist, false
      *  otherwise.
      */
     public static boolean litInLitList(Literal lit, ArrayList<Literal>litList) {

         for (int i = 0; i < litList.size(); i++)
             if (lit.equals(litList.get(i)))
                 return true;
         return false;
     }

     /** ***************************************************************
      * Return true if (a literal equal to) lit is in litlist, false
      * otherwise.
      */
     public static boolean oppositeInLitList(Literal lit, ArrayList<Literal>litList) {

         for (int i = 0; i < litList.size(); i++)
             if (lit.isOpposite(litList.get(i)))
                 return true;
         return false;
     }

     /** ***************************************************************
      *  Try to extend subst a match from self to other. Return True on
      *  success, False otherwise. In the False case, subst is unchanged.
      */
     public boolean match(Literal other, BacktrackSubstitution subst) {

    	 //System.out.println("Literal.match(): this: " + this + " other: " + other + " op: " + op);
         if (this.isNegative() != other.isNegative())
             return false;
         else 
             return subst.match(atom, other.atom);         
     }
     
     /** ***************************************************************
      */
     public static Literal string2lit(String s) {
         
    	 //System.out.println("Literal.string2lit(): s: " + s);
         Lexer lex = new Lexer(s);
         Literal l = new Literal();
         return l.parseLiteral(lex);
     }
     
     /** ***************************************************************
      * ************ UNIT TESTS *****************
      */
     public static Literal a1 = null;
     public static Literal a2 = null;
     public static Literal a3 = null;
     public static Literal a4 = null;
     public static Literal a5 = null;
     public static Literal a6 = null;
     public static Literal a7 = null;
     public static Literal a8 = null;
     
     public static String input1 = "p(X)  ~q(f(X,a), b)  ~a=b  a!=b  ~a!=f(X,b)  p(X)  ~p(X) p(a)";
     public static String input2 = "p(X)|~q(f(X,a), b)|~a=b|a!=b|~a!=f(X,b)";
     public static String input3 = "$false";
     public static String input4 = "$false|~q(f(X,a), b)|$false";
     public static String input5 = "p(a)|p(f(X))";
     public static String input6 = "foo(bar,vaz)|f(X1,X2)!=g(X4,X5)|k(X1,X1)!=k(a,b)";
         
     /** ***************************************************************
      * Setup function for clause/literal unit tests. Initialize
      * variables needed throughout the tests.
      */
     public static void setup() {
                  
         Lexer lex = new Lexer(input1);
         
         System.out.println("INFO in Literal.setup(): input: " + input1);
         a1 = new Literal();
         a1 = a1.parseLiteral(lex);
         System.out.println("INFO in Literal.setup(): finished parsing a1: " + a1);
         System.out.println("INFO in Literal.setup(): pointing at token: " + lex.literal);
         
         a2 = new Literal();
         a2 = a2.parseLiteral(lex);
         System.out.println("INFO in Literal.setup(): finished parsing a2: " + a2);
         System.out.println("INFO in Literal.setup(): pointing at token: " + lex.literal);
         
         a3 = new Literal();
         a3 = a3.parseLiteral(lex);
         System.out.println("INFO in Literal.setup(): finished parsing a3: " + a3);
         System.out.println("INFO in Literal.setup(): pointing at token: " + lex.literal);
         
         a4 = new Literal();
         a4 = a4.parseLiteral(lex);
         System.out.println("INFO in Literal.setup(): finished parsing a4: " + a4);
         System.out.println("INFO in Literal.setup(): pointing at token: " + lex.literal);
         
         a5 = new Literal();
         a5 = a5.parseLiteral(lex);
         System.out.println("INFO in Literal.setup(): finished parsing a5: " + a5);
         System.out.println("INFO in Literal.setup(): pointing at token: " + lex.literal);
         
         a6 = new Literal();
         a6 = a6.parseLiteral(lex);
         System.out.println("INFO in Literal.setup(): finished parsing a6: " + a6);
         System.out.println("INFO in Literal.setup(): pointing at token: " + lex.literal);
         
         a7 = new Literal();
         a7 = a7.parseLiteral(lex);
         System.out.println("INFO in Literal.setup(): finished parsing a7: " + a7);
         System.out.println("INFO in Literal.setup(): pointing at token: " + lex.literal);
         
         a8 = new Literal();
         a8 = a8.parseLiteral(lex);
         System.out.println("INFO in Literal.setup(): finished parsing a8: " + a8);
         System.out.println("INFO in Literal.setup(): pointing at token: " + lex.literal);
     }
     
     /** ***************************************************************
      *  Test that basic literal literal functions work correctly.
      */
     public static void testLiterals() {

         System.out.println("---------------------");
         System.out.println("INFO in testLiterals(): all true");
         System.out.println("a1: " + a1);
         System.out.println("is positive:" + a1.isPositive());
         System.out.println("is not equational: " + !a1.isEquational());
         ArrayList vars = a1.collectVars();
         System.out.println("Number of variables. Should be 1 :" + vars.size());

         System.out.println();
         System.out.println("a2: " + a2);
         System.out.println("is positive:" + !a2.isNegative());
         System.out.println("is not equational: " + !a2.isEquational());
         vars = a2.collectVars();
         System.out.println("Number of variables. Should be 1 :" + vars.size());
         
         System.out.println();
         System.out.println("a3: " + a3);
         System.out.println("is positive:" + !a3.isNegative());
         System.out.println("is equational: " + a3.isEquational());
         System.out.println(a3 + " equals " + a4 + " :" + a3.equals(a4));
         vars = a3.collectVars();
         System.out.println("Number of variables. Should be 0 :" + vars.size());
         
         System.out.println();
         System.out.println("a4: " + a4);
         System.out.println("is negative:" + a4.isNegative());
         System.out.println("is equational: " + a4.isEquational());
         System.out.println(a4 + " equals " + a3 + " :" + a4.equals(a3));
         vars = a4.collectVars();
         System.out.println("Number of variables. Should be 0 :" + vars.size());
         
         System.out.println();
         System.out.println("a5: " + a5);
         System.out.println("is positive:" + !a5.isNegative());
         System.out.println("is equational: " + a5.isEquational());
         vars = a5.collectVars();
         System.out.println("Number of variables. Should be 1 :" + vars.size());   
         
         System.out.println();
         System.out.println("a6: " + a6);
         System.out.println("is positive:" + !a6.isNegative());
         System.out.println("is not equational: " + !a6.isEquational());
         vars = a6.collectVars();
         System.out.println("Number of variables. Should be 1 :" + vars.size());   
         
         System.out.println();
         System.out.println("a7: " + a7);
         System.out.println("is positive:" + !a7.isNegative());
         System.out.println("is not equational: " + !a7.isEquational());
         vars = a7.collectVars();
         System.out.println("Number of variables. Should be 1 :" + vars.size());   
         
         System.out.println();
         System.out.println("a8: " + a8);
         System.out.println("is positive:" + !a8.isNegative());
         System.out.println("is not equational: " + !a8.isEquational());
         vars = a8.collectVars();
         System.out.println("Number of variables. Should be 0 :" + vars.size());   
     }

     /** ***************************************************************
      * Test the weight function.
      */
     public static void testLitWeight() {

         System.out.println("---------------------");
         System.out.println("INFO in testLitWeight(): all true");
         System.out.println(a1.weight(2,1) == 3);
         System.out.println(a2.weight(2,1) == 9);
         System.out.println(a3.weight(2,1) == 6);
         System.out.println(a4.weight(2,1) == 6);
         System.out.println(a5.weight(2,1) == 9);
         System.out.println(a6.weight(2,1) == 3);
         System.out.println(a7.weight(2,1) == 3);
         System.out.println(a8.weight(2,1) == 4);
     }
         
     /** ***************************************************************
      * Test literal list parsing and printing.
      */
     public static void testLitList() {

         System.out.println("-------------------------------------------------");
         System.out.println("INFO in testLitList(): all true");
         
         System.out.println("input2: " + input2);
         Lexer lex = new Lexer(input2);                                
         ArrayList<Literal> l2 = parseLiteralList(lex);
         System.out.println(l2);
         System.out.println(l2.size() == 5); 
         System.out.println();
       
         System.out.println("input3: " + input3);
         lex = new Lexer(input3);
         ArrayList<Literal> l3 = parseLiteralList(lex);
         System.out.println(l3);
         System.out.println(l3.size() == 0); 
         System.out.println();
         
         System.out.println("input4: " + input4);
         lex = new Lexer(input4);
         ArrayList<Literal> l4 = parseLiteralList(lex);
         System.out.println(l4);
         System.out.println(l4.size() == 1);     
         System.out.println();
         
         System.out.println("input5: " + input5);
         lex = new Lexer(input5);
         ArrayList<Literal> l5 = parseLiteralList(lex);
         System.out.println(l5);
         System.out.println(l5.size() == 2);
         System.out.println();         
         
         System.out.println("input6: " + input6);
         lex = new Lexer(input6);
         ArrayList<Literal> l6 = parseLiteralList(lex);
         System.out.println(l6);
         System.out.println(l6.size() == 3);  
     }
     
     /** ***************************************************************
      */
     public static void testTokens() {
         
         System.out.println("-------------------------------------------------");
         System.out.println("INFO in testTokens():");
         Lexer lex = new Lexer("0 1 2 3");
         try {
             lex.next();
             System.out.println(lex.literal);
             lex.next();
             System.out.println(lex.literal);
             lex.look();
             lex.next();

             System.out.println(lex.literal);
         }
         catch (Exception e) {
             System.out.println(e.getMessage());
         }
     }

     /** ***************************************************************
      * Test signature collection.
      */
     public static void testSig() {

         System.out.println("-------------------------------------------------");
         System.out.println("INFO in testSig(): all true");
         Signature sig = new Signature();
         sig = a1.collectSig(sig);
         sig = a2.collectSig(sig);
         sig = a3.collectSig(sig);
         sig = a4.collectSig(sig);
         sig = a5.collectSig(sig);
         sig = a6.collectSig(sig);
         sig = a7.collectSig(sig);
         sig = a8.collectSig(sig);
         
         sig.addFun("mult", 2);

         System.out.println(sig);
         System.out.println(sig.isPred("q"));
         System.out.println(!sig.isPred("unknown"));
         System.out.println(!sig.isPred("a"));
         System.out.println(sig.isFun("a"));
         System.out.println(!sig.isFun("unknown"));
         System.out.println(!sig.isFun("q"));

         System.out.println(sig.getArity("b") == 0);
         System.out.println(sig.getArity("p") == 1) ; 
     }
     
     /** ***************************************************************
     */
    public static void main(String[] args) {
        
        setup();
        testLiterals();
        testLitWeight();
        testLitList();
        testSig();
        //testTokens();
    }

}
