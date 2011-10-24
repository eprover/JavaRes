/*
 * This module implements the rules of the simple resolution calculus,
    namely binary resolution and factoring.
    inference rule:

    Binary resolution:

    c1|a1     c2|~a2
    ---------------- where sigma=mgu(a1,a2)
     sigma(c1|c2)

    Note that c1 and c2 are arbitrary disjunctions of literals (each of
    which may be positive or negative). Both c1 and c2 may be empty.  Both
    a1 and a2 are atoms (so a1 and ~a2 are a positive and a negative
    literal, respectively).  Also, since | is AC (or, alternatively, the
    clauses are unordered multisets), the order of literals is irrelevant.

    Clauses are interpreted as implicitly universally quantified
    disjunctions of literals. This implies that the scope of the variables
    is a single clause. In other words, from a theoretical point of view,
    variables in different clauses are different. In practice, we have to
    enforce this explicitly by making sure that all clauses used as
    premises in an inference are indeed variable disjoint.

    Factoring:

       c|a|b
    ----------  where sigma = mgu(a,b)
    sigma(c|a)

    Again, c is an arbitray disjunction.
    
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

public class Resolution {

    /** ***************************************************************
     * Implementation of the Resolution rule. lit1 and lit2 are indices
     * of literals in clause1 and clause2, respectively, so clause1|lit1
     * and clause2|lit2 are literals.
     * Try to resolve clause1|lit1 against clause2|lit2. If this is
     * possible, return the resolvent. Otherwise, return None.
     */
    public static Clause resolution(Clause clause1, int lit1, Clause clause2, int lit2) {

        Literal l1 = clause1.getLiteral(lit1);
        Literal l2 = clause2.getLiteral(lit2);
        if (l1.isNegative() == l2.isNegative())
            return null;
        Substitutions sigma = Unification.mgu(l1.lhs, l2.lhs);
        //System.out.println("INFO in Resolution.resolution(): sigma " + sigma);
        if (sigma == null)
            return null;
        ArrayList<Literal> lits1 = new ArrayList<Literal> ();
       
        for (int i = 0; i < clause1.literals.size(); i++) {
            Literal l =  clause1.literals.get(i); 
            //System.out.println("INFO in Resolution.resolution(): literal " + l);
            if (!l.equals(l1))
                lits1.add(l.instantiate(sigma));
        }
        ArrayList<Literal> lits2 = new ArrayList<Literal> ();
        for (int i = 0; i < clause2.literals.size(); i++) {
            Literal l =  clause2.literals.get(i); 
            if (!l.equals(l2))
                lits2.add(l.instantiate(sigma));
        }
        lits1.addAll(lits2);
        Clause res = new Clause();
        res.addAll(lits1);
        res.removeDupLits();
        return res;
    }

    /** ***************************************************************
     * Check if it is possible to form a factor between lit1 and lit2. If
     * yes, return it, otherwise return None.
     */
    public static Clause factor(Clause clause, int lit1, int lit2) {

        Literal l1 = clause.getLiteral(lit1);
        Literal l2 = clause.getLiteral(lit2);
        if (l1.isNegative() != l2.isNegative())
            return null;
        Substitutions sigma = Unification.mgu(l1.lhs, l2.lhs);
        if (sigma == null)
            return null;
        ArrayList<Literal> lits = new ArrayList<Literal> ();
        for (int i = 0; i < clause.literals.size(); i++) {
            Literal l =  clause.literals.get(i); 
            if (!l.equals(l2))
                lits.add(l.instantiate(sigma));
        }
        Clause res = new Clause();
        res.addAll(lits);
        res.removeDupLits();
        return res;
    }

    /** ***************************************************************
     * ************ UNIT TESTS *****************
     */    
    public static Clause c1 = new Clause();
    public static Clause c2 = new Clause();
    public static Clause c3 = new Clause();
    public static Clause c4 = new Clause();
    
    /** ***************************************************************
     * Setup function for resolution testing
     */
    public static void setup() {

       String spec = "cnf(c1,axiom,p(a, X)|p(X,a)).\n" +
           "cnf(c2,axiom,~p(a,b)|p(f(Y),a)).\n" +
           "cnf(c3,axiom,p(Z,X)|~p(f(Z),X0)).\n" +
           "cnf(c4,axiom,p(X,X)|p(a,f(Y))).";
       StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(spec));
       c1.parse(st);
       c2.parse(st);
       c3.parse(st);
       c4.parse(st);
    }
    
    /** ***************************************************************
     * Test resolution
     */
    public static void testResolution() {

        Clause res1 = resolution(c1, 0, c2,0);
        assert res1 != null;
        System.out.println("Resolution.testResolution(): successful result: " + res1);

        Clause res2 = resolution(c1, 0, c3,0);
        assert res2 == null;
        System.out.println("Resolution.testResolution(): successful (null) result: " + res2);

        Clause res3 = resolution(c2, 0, c3,0);
        assert res3 != null;
        System.out.println("Resolution.testResolution(): successful result: " + res3);

        Clause res4 = resolution(c1, 0, c3,1);
        assert res4 == null;
        System.out.println("Resolution.testResolution(): successful (null) result: " + res4);
    }
    
    /** ***************************************************************
     * Test the factoring inference.
     */
    public static void testFactoring() {
  
        Clause f1 = factor(c1,0,1);
        assert f1 != null;
        assert f1.length()==1;
        System.out.println("Resolution.testFactoring(): successful result of length 1: Factor:" + f1);
        
        Clause f2 = factor(c2,0,1);
        assert f2 == null;
        System.out.println("Resolution.testFactoring(): successful (null) result: Factor:" + f2);

        Clause f4 = factor(c4,0,1);
        assert f4 == null;
        System.out.println("Resolution.testFactoring(): successful (null) result: Factor:" + f4);
    }
    
    /** ***************************************************************
     * Test method for this class.  
     */
    public static void main(String[] args) {
        
        setup();
        testResolution();
        testFactoring();
    }
}
