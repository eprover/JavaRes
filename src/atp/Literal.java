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

import atp.Parser.Formula;

import com.articulate.sigma.*;
import com.articulate.sigma.STP2.FormulaRating;

import java.io.*;
import java.text.ParseException;
import java.util.*;

/** ***************************************************************
 * atom - predicate symbol with term arguments
 * literal - atom or negated atom
  */
public class Literal {
    
    public String op = "";
    public Term lhs = null;  // if there's no op, then the literal is just a term, held in lhs
    public Term rhs = null;
    boolean negated = false;
     
     /** ***************************************************************
      */
     public String toString() {
             
         StringBuffer result = new StringBuffer();
         if (negated)
             result.append("~");
         if (!StringUtil.emptyString(op)) {
             if (op.equals("=") || op.equals("!="))
                 result.append(lhs + op + rhs);
             else
                 result.append(lhs);
         }
         else 
             result.append(lhs);         
         return result.toString();
     }
     
     /** ***************************************************************
      */
     public boolean equals(Object l_obj) {
        
         assert !l_obj.getClass().getName().equals("Literal") : "Literal.equals() passed object not of type Literal"; 
         Literal l = (Literal) l_obj;
         if (!lhs.equals(l.lhs))
             return false;
         if (!StringUtil.emptyString(op)) {
             if (!op.equals(l.op))
                 return false;
             if (!rhs.equals(l.rhs))
                 return false;
         }
         else
             if (!StringUtil.emptyString(l.op))
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
         result.op = op;
         if (lhs != null)
             result.lhs = lhs.deepCopy();
         if (rhs != null)
             result.rhs = rhs.deepCopy();
         result.negated = negated;  
         return result;
     }
     
     /** ***************************************************************
      * Return true if the atoms of self and other are structurally 
      * identical to each other, but the sign is the opposite.
      */
     public boolean isOpposite(Literal other) {

         if (StringUtil.emptyString(op))
             return this.isNegative() != other.isNegative() &&
                  lhs.equals(other.lhs);
         else {
             return this.isNegative() != other.isNegative() &&
                 op.equals(other.op) && 
                 lhs.equals(other.lhs) && 
                 rhs.equals(other.rhs);
         }
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
      
         return !StringUtil.emptyString(op);
     }
     
     /** ***************************************************************
      */
     public ArrayList<Term> collectVars() {
         
         ArrayList result = new ArrayList();
         result.addAll(lhs.collectVars());
         if (rhs != null)
             result.addAll(rhs.collectVars());
         return result;
     }

     /** ***************************************************************
      * Return a copy of self, instantiated with the given substitution.
      */
     public Literal instantiate(Substitutions subst) {

         Literal newLit = new Literal();
         newLit.negated = negated;
         newLit.lhs = subst.apply(lhs);
         if (!StringUtil.emptyString(op))
             newLit.rhs = subst.apply(rhs); 
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
                 
         int result = 0;
         if (!StringUtil.emptyString(op))
             result = result + fweight;
         if (lhs != null)
             result = result + lhs.termWeight(fweight, vweight);
         if (rhs != null)
             result = result + rhs.termWeight(fweight, vweight);         
         return result;
     }
     
     /** ***************************************************************
      * An atom is either a conventional atom, in which case it's 
      * syntactically identical to a term, or it is an equational literal, 
      * of the form 't1=t2' or 't1!=t2', where t1 and t2 are terms.
      * In either case, we represent the atom as a first-order
      * term. Equational literals are represented at terms with faux
      * function symbols "=" and "!=". 
      */
     public Literal parseAtom(StreamTokenizer_s st) {
                
         Term.setupStreamTokenizer(st);
         try {
             //System.out.println("Entering Literal.parseAtom(): " + this);
             lhs = new Term();
             //System.out.println("INFO in Literal.parseAtom(): (1) token:" + st.ttype + " " + Character.toString((char) st.ttype) + "  word:" + st.sval);
             lhs.parse(st);
             if (st.ttype != st.TT_EOF)
                 st.nextToken();
             else 
                 return this;
             if (st.ttype == '=' || st.ttype == '!') {
                 if (st.ttype == '!') {
                     st.nextToken();
                     if (st.ttype != '=')
                         throw new Exception("!= expected");
                     op = "=";
                     negated = !negated;
                 }
                 else
                     op = "=";
                 rhs = new Term();

                 //System.out.println("INFO in Literal.parseAtom(): (2) token:" + st.ttype + " " + Character.toString((char) st.ttype) + "  word:" + st.sval);
                 rhs = rhs.parse(st);    
             }
             //System.out.println("Exiting Literal.parseAtom(): " + this);
             return this;
         }
         catch (Exception ex) {
             System.out.println("Error in Literal.parseAtom(): " + ex.getMessage());
             if (st.ttype == StreamTokenizer.TT_WORD)
                 System.out.println("Error in Literal.parseAtom(): word token:" + st.sval);  
             else
                 System.out.println("Error in Literal.parseAtom(): token:" + st.ttype + " " + Character.toString((char) st.ttype));
             ex.printStackTrace();
         }
         return null;
     }   
     
     /** ***************************************************************
      *  Parse a literal. A literal is an optional negation sign '~', 
      *  followed by an atom. 
      */
     public Literal parseLiteral(StreamTokenizer_s st) {
                
         Term.setupStreamTokenizer(st);
         try {
             //System.out.println("Entering Literal.parseLiteral(): " + this);

             //System.out.println("INFO in Literal.parseLiteral(): token:" + st.ttype + " " + Character.toString((char) st.ttype) + "  word:" + st.sval);
             st.nextToken();
             if (st.ttype == '|')
                 st.nextToken();
             //System.out.println("INFO in Literal.parseLiteral(): token:" + st.ttype + " " + Character.toString((char) st.ttype) + "  word:" + st.sval);
             if (st.ttype == '~') {
                 negated = true;
                 //System.out.println("INFO in Literal.parseLiteral(): it's negated");
                 st.nextToken();  //restored 10/17
                 st.pushBack();
             }
             else
                 st.pushBack();
             //System.out.println("INFO in Literal.parseLiteral(): token:" + st.ttype + " " + Character.toString((char) st.ttype) + "  word:" + st.sval);
             this.parseAtom(st);
             //System.out.println("Exiting Literal.parseLiteral(): " + this);
             return this;
         }
         catch (Exception ex) {
             System.out.println("Error in Literal.parseLiteral(): " + ex.getMessage());
             if (st.ttype == StreamTokenizer.TT_WORD)
                 System.out.println("Error in Literal.parseLiteral(): word token:" + st.sval);  
             else
                 System.out.println("Error in Literal.parseLiteral(): token:" + st.ttype + " " + Character.toString((char) st.ttype));
             ex.printStackTrace();
         }
         return null;
     }   
     
     /** ***************************************************************
      *  Parse a list of literals separated by "|" (logical or). As per 
      *  TPTP 3 syntax, the single word "$false" is interpreted as the
      *  false literal, and ignored.
      */
     public static ArrayList<Literal> parseLiteralList(StreamTokenizer_s st) {
                
         ArrayList<Literal> res = new ArrayList<Literal>();
         Term.setupStreamTokenizer(st);
         try {
             Literal l = new Literal();
             l.parseLiteral(st);
             //System.out.println("INFO in Literal.parseLiteralList(): (pre-loop result): " + l);
             if (!l.toString().equals("$false")) 
                 res.add(l);                          
             while (st.ttype == '|') {               
                 l = new Literal();
                 //st.nextToken();   // removed 10/17
                 l.parseLiteral(st);
                 //System.out.println("INFO in Literal.parseLiteralList(): (post loop result): " + l);
                 //System.out.println("INFO in Literal.parseLiteralList(): (post loop result): " + st.ttype + " " + Character.toString((char) st.ttype) + "  word:" + st.sval);

                 if (!l.toString().equals("$false") && !StringUtil.emptyString(l.toString())) 
                     res.add(l);                                   
             }
             return res;
         }
         catch (Exception ex) {
             System.out.println("Error in parseLiteralList(): " + ex.getMessage());
             if (st.ttype == StreamTokenizer.TT_WORD)
                 System.out.println("Error in parseLiteralList(): word token:" + st.sval);  
             else
                 System.out.println("Error in parseLiteralList(): token:" + st.ttype + " " + Character.toString((char) st.ttype));
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

         if (this.isNegative() != other.isNegative())
             return false;
         else
             return subst.match(lhs, other.lhs);
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
     
     public static String input1 = "p(X)  ~q(f(X,a), b)  ~a=b  a!=b  ~a!=f(X,b)  p(X)  ~p(X)";
     public static String input2 = "p(X)|~q(f(X,a), b)|~a=b|a!=b|~a!=f(X,b)";
     public static String input3 = "$false";
     public static String input4 = "$false|~q(f(X,a), b)|$false";
     public static String input5 = "p(a)|p(f(X))";
         
     /** ***************************************************************
      * Setup function for clause/literal unit tests. Initialize
      * variables needed throughout the tests.
      */
     public static void setup() {
         
         a1 = new Literal();
         StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(input1));
         Term.setupStreamTokenizer(st);
         a1 = a1.parseLiteral(st);
         System.out.println("INFO in Literal.setup(): finished parsing a1: " + a1);
         
         a2 = new Literal();
         a2 = a2.parseLiteral(st);
         System.out.println("INFO in Literal.setup(): finished parsing a2: " + a2);
         
         a3 = new Literal();
         a3 = a3.parseLiteral(st);
         System.out.println("INFO in Literal.setup(): finished parsing a3: " + a3);
         
         a4 = new Literal();
         a4 = a4.parseLiteral(st);
         System.out.println("INFO in Literal.setup(): finished parsing a4: " + a4);
         
         a5 = new Literal();
         a5 = a5.parseLiteral(st);
         System.out.println("INFO in Literal.setup(): finished parsing a5: " + a5);
         
         a6 = new Literal();
         a6 = a6.parseLiteral(st);
         System.out.println("INFO in Literal.setup(): finished parsing a6: " + a6);
         
         a7 = new Literal();
         a7 = a7.parseLiteral(st);
         System.out.println("INFO in Literal.setup(): finished parsing a7: " + a7);
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
     }
         
     /** ***************************************************************
      * Test literal list parsing and printing.
      */
     public static void testLitList() {

         System.out.println("-------------------------------------------------");
         System.out.println("INFO in testLitList(): all true");
         
         System.out.println("input2: " + input2);
         StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(input2));
         Term.setupStreamTokenizer(st);
         ArrayList<Literal> l2 = parseLiteralList(st);
         System.out.println(l2);
         System.out.println(l2.size() == 5); 
         System.out.println();
         
         System.out.println("input3: " + input3);
         st = new StreamTokenizer_s(new StringReader(input3));
         ArrayList<Literal> l3 = parseLiteralList(st);
         System.out.println(l3);
         System.out.println(l3.size() == 0); 
         System.out.println();
         
         System.out.println("input4: " + input4);
         st = new StreamTokenizer_s(new StringReader(input4));
         ArrayList<Literal> l4 = parseLiteralList(st);
         System.out.println(l4);
         System.out.println(l4.size() == 1);     
         System.out.println();
         
         System.out.println("input5: " + input5);
         st = new StreamTokenizer_s(new StringReader(input5));
         ArrayList<Literal> l5 = parseLiteralList(st);
         System.out.println(l5);
         System.out.println(l5.size() == 2);  
     }
     
     /** ***************************************************************
      * Test instantiation.
      */
     public static void testInstantiate() {
         
     }
     
     /** ***************************************************************
     */
    public static void main(String[] args) {
        
        setup();
        testLiterals();
        testLitWeight();
        testLitList();
        testInstantiate();
    }

}
