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

    A simple implementation of substitutions.

    Definition: A substitution sigma is a function sigma:V->Terms(F,V)
    with the property that sigma(X)=X for all but finitely many variables
    X from V.

    A substitution is continued to terms recursively:
    sigma(f(t1, ..., tn)) = f(sigma(t1), ..., sigma(t2))

    Substitutions are customarily represented by the Greek letter simga.

    Footnote:
    If more than one substitution is needed, the second one is usually
    called tau, and further ones are denoted with primes or subscripts. 
    
    Substitutions map variables to terms. Substitutions as used here
    are always fully expanded, i.e. each variable is bound directly to
    the term it maps too.
*/

package atp;
import java.io.StringReader;
import java.util.*;

import atp.Parser.Term;

import com.articulate.sigma.StreamTokenizer_s;

public class Substitutions {

    public HashMap<Term,Term> subst = new HashMap<Term,Term>();
    
    /** ***************************************************************
     * Return a print representation of the substitution.
     */    
    public String toString() {

        StringBuffer result = new StringBuffer();
        result.append("{");
        Iterator<Term> it = subst.keySet().iterator();
        while (it.hasNext()) {
            Term key = it.next();
            Term value = subst.get(key);
            result.append(key + "<-" + value);
            if (it.hasNext())
                result.append(",");
        }
        return result.toString();
    }

    /** ***************************************************************
     * Return a copy of the substitution.
     */    
    public Substitutions copy() {

        Substitutions result = new Substitutions();
        Iterator<Term> it = subst.keySet().iterator();
        while (it.hasNext()) {
            Term key = it.next();
            Term value = subst.get(key);
            result.subst.put(key.termCopy(),value.termCopy());
        }
        return null;
    }
    
    /** ***************************************************************
     * Return a copy of the substitution.
     */    
    public boolean equals(Substitutions s2) {

        if (subst.keySet().size() != s2.subst.keySet().size())
            return false;
        Iterator<Term> it = subst.keySet().iterator();
        while (it.hasNext()) {
            Term key = it.next();
            if (!subst.get(key).equals(s2.subst.get(key)))
                return false;
        }
        return true;
    }

    /** ***************************************************************
     * Apply the substitution to a term. Return the result.
     */    
    public Parser.Term apply(Parser.Term term) {
        
        Parser p = new Parser();
        Term res = p.new Term();
        if (term.termIsVar()) {
            res = subst.get(term);
            return res;
        }
        else {
            res.t = term.t;
            for (int i = 0; i < term.subterms.size(); i++)
                res.subterms.add(apply(term.subterms.get(i)));
            return res;
        }
    }
    
    /** ***************************************************************
     * Apply the substitution to a term. Return the result.
     */    
    public Parser.Term applyBinding(Term var, Term term) {
         
        Parser p = new Parser();
        Term res = p.new Term();
        if (term.termIsVar()) {
            res.t = subst.get(term).t;
            return res;
        }
        else {
            res.t = term.t;
            for (int i = 0; i < term.subterms.size(); i++)
                res.subterms.add(apply(term.subterms.get(i)));
            return res;
        }
    }
    
    /** ***************************************************************
     * Apply a new binding to an existing substitution.
     */    
    public void composeBinding(Term var, Term term) {

        Iterator<Term> it = subst.keySet().iterator();
        while (it.hasNext()) {
            Term key = it.next();
            Term bound = subst.get(key);
            subst.put(key,apply(bound));
        }
        if (!subst.containsKey(var))
            subst.put(var,term);        
    }

    /** ***************************************************************
     * Set up test content.  
     */
    static String example1 = "f(X, g(Y))";
    static String example2 = "a";
    static String example3 = "b";
    static String example4 = "f(a, g(a))";     
    static String example5 = "f(a, g(b))";    
    static String example6 = "X"; 
    static String example7 = "Y"; 
    static String example8 = "Z"; 
    
    static Term t1 = null;
    static Term t2 = null;
    static Term t3 = null;
    static Term t4 = null;
    static Term t5 = null;
    static Term t6 = null;
    static Term t7 = null;
    static Term t8 = null;    
    
    static Substitutions s1 = new Substitutions();
    static Substitutions s2 = new Substitutions();
    
    /** ***************************************************************
     * Set up test content.  
     */
    public void setupTests() {
        
        Parser p = new Parser();
        Term t = p.new Term();
        t1 = t.parse(new StreamTokenizer_s(new StringReader(example1)));
        t2 = t.parse(new StreamTokenizer_s(new StringReader(example2)));
        t3 = t.parse(new StreamTokenizer_s(new StringReader(example3)));
        t4 = t.parse(new StreamTokenizer_s(new StringReader(example4)));
        t5 = t.parse(new StreamTokenizer_s(new StringReader(example5)));
        t6 = t.parse(new StreamTokenizer_s(new StringReader(example6)));
        t7 = t.parse(new StreamTokenizer_s(new StringReader(example7)));
    }
    
    /** ***************************************************************
     * Set up test content.  
     */
    public static void testEquality() {
        
        s1.subst.put(t6,t2);
        s1.subst.put(t6,t2);
        s2.subst.put(t6,t2);
        s2.subst.put(t6,t3);
    }
    
    /** ***************************************************************
     * Test basic stuff.  
     */
    public static void testSubstBasic() {
            
        System.out.println("---------------------");
        System.out.println("INFO in testSubstBasic()");
        Substitutions tau = s1.copy();
        System.out.println("should be true.  Value: " + tau.apply(t6).equals(s1.apply(t6)));        
        System.out.println("should be true.  Value: " + tau.apply(t7).equals(s1.apply(t7)));        
        System.out.println("should be true.  Value: " + tau.apply(t8).equals(s1.apply(t8)));
    }
            
    /** ***************************************************************
     * Check application of substitutions.  
     */
    public static void testSubstApply() {
        
        System.out.println("---------------------");
        System.out.println("INFO in testSubstApply()");
        System.out.println("should be same: " + s1.apply(t1).toString() + " and " + example4);
        System.out.println("should be true: " + s1.apply(t1).equals(t4));
        System.out.println("should be true: " + s1.apply(t1).equals(t5));
    }

}