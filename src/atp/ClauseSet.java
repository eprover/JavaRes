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

package atp;
import java.io.*;
import java.util.*;
import java.text.*;

/** ***************************************************************
 * A class representing a clause set (or, more precisely,
 * a multi-set of clauses). 
 */    
public class ClauseSet {

    public ArrayList<Clause> clauses = new ArrayList<Clause>();
             
    /** ***************************************************************
     * Return a string representation of the clause set.
     */                            
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < clauses.size(); i++)
            sb.append(clauses.get(i) + "\n");
       return sb.toString();
    }

    /** ***************************************************************
     * Return number of clauses in set.
     */ 
    public int length() {

        return clauses.size();
    }
    
    /** ***************************************************************
     * get a clause
     */ 
    public Clause get(int i) {

        assert i < clauses.size() : "index out of bounds: " + Integer.toString(i) + 
                                    " with clause list length: " + Integer.toString(clauses.size());
        return clauses.get(i);
    }
    
    /** ***************************************************************
     * Add a clause to the clause set.
     */ 
    public void add(Clause clause) {

        clauses.add(clause);
    }
    
    /** ***************************************************************
     * Add a clause to the clause set.
     */ 
    public void addAll(ClauseSet clauseSet) {

        clauses.addAll(clauseSet.clauses);
    }

    /** ***************************************************************
     * Add a clause to the clause set.
     */ 
    public void addAll(ArrayList<Clause> clauseSet) {

        clauses.addAll(clauseSet);
    }
    
    /** ***************************************************************
     * Remove a clause from the clause set and return it.
     */ 
    public Clause extractClause(Clause clause) {

        clauses.remove(clause);
        return clause;
    }
    
    /** ***************************************************************
     * Extract and return the first clause.
     */ 
   public Clause extractFirst() {

       if (clauses.size() > 0) 
           return clauses.remove(0);        
       else
           return null;
   }

    /** ***************************************************************
     * Return a list of tuples (clause, literal-index) such that the
     * set includes at least all literals that can potentially be
     * resolved against lit. In the naive and obviously correct first
     * implementation, this simply returns a list of all
     * literal-indices for all clauses.
     * @return a side effect on @param clauseres and @param indices
     */ 
    public void getResolutionLiterals(Literal lit, ArrayList<Clause> clauseres, ArrayList<Integer> indices) {

        assert clauseres.size() == 0 : "non empty result variable clauseres passed to ClauseSet.getResolutionLiterals()";
        assert indices.size() == 0 : "non empty result variable indices passed to ClauseSet.getResolutionLiterals()";
        for (int i = 0; i < clauses.size(); i++) {
            Clause c = clauses.get(i);
            for (int j = 0; j < c.length(); j++) {
                clauseres.add(clauses.get(i));
                indices.add(new Integer(j));
            }
        }
    }
        
    /** ***************************************************************
     * Parse a sequence of clauses from st and add them to the
     * set. Return number of clauses parsed.
     */ 
    public int parse(Lexer lex) {

        int count = 0;
        while (lex.type != Lexer.EOFToken) {
            Clause clause = new Clause();
            try {
                Clause c = clause.parse(lex);
                if (c == null)
                    return 0;
            }
            catch (ParseException p) {
                System.out.println(p.getMessage());
                return 0;
            }

            if (clause.literals.size() > 0)
                add(clause);
        }
        return count;
    }
    
    /** ***************************************************************
     * Parse a clause set from a file.
     */ 
    public static ClauseSet parseFromFile(String filename) {
            
        if (filename != null && filename != "") {
            FileReader fr = null;
            try {
                File fin  = new File(filename);
                if (fr != null) {
                    Lexer lex = new Lexer(fin);               
                    ClauseSet cs = new ClauseSet();
                    cs.parse(lex);
                    return cs;
                }
            }
            catch (Exception e) {
                System.out.println("Error in ClauseSet.parseFromFile(): File error reading " + filename + ": " + e.getMessage());
                return null;
            }
            finally {
                try {
                    if (fr != null) fr.close();
                }
                catch (Exception e) {
                    System.out.println("Exception in ClauseSet.parseFromFile()" + e.getMessage());
                }
            }                
        }   
        return null;
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {

        String spec2 = "cnf(axiom, humans_are_mortal, mortal(X)|~human(X)).\n" + 
        "cnf(axiom, socrates_is_human, human(socrates)).\n" +
        "cnf(negated_conjecture, is_socrates_mortal, ~mortal(socrates)).\n";
        Lexer lex = new Lexer(spec2);        
        ClauseSet problem = new ClauseSet();
        problem.parse(lex);
        System.out.println("ClauseSet test.  Expected: "); 
        System.out.println(spec2);
        System.out.println("Actual: ");
        System.out.println(problem);
    }
}
