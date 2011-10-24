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
import com.articulate.sigma.*;

import java.io.*;
import java.util.*;

/** ***************************************************************
 * A class representing a clause set (or, more precisely,
 * a multi-set of clauses). 
 */    
public class ClauseSet {

    ArrayList<Clause> clauses = new ArrayList<Clause>();
             
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

       if (clauses.size() > 0) {
           Clause result = clauses.get(0);
           clauses.remove(result); 
           return result;
       }
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
            clauseres.add(clauses.get(i));
            indices.add(new Integer(clauses.get(i).length()));
        }
    }
        
    /** ***************************************************************
     * Parse a sequence of clauses from st and add them to the
     * set. Return number of clauses parsed.
     */ 
    public int parse(StreamTokenizer_s st) {

        int count = 0;
        while (st.ttype != StreamTokenizer_s.TT_EOF) {
            Clause clause = new Clause();
            clause.parse(st);
            add(clause);
        }
        return count;
    }
}
