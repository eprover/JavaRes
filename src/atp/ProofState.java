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

    Implementation of the given-clause algorithm for saturation of clause
    sets under the rules of the resolution calculus. This improves on the
    very basic implementation in simplesat in several ways.

    - It supports heuristic clause selection, not just first-in first-out
    - It supports tautology deletion
    - It supports forward and backwards subsumption
    - It keeps some statistics to enable the user to understand the
      practical impact of different steps of the algorithm better.

    Most of these changes can be found in the method processClause().
*/
package atp;
import java.io.*;
import java.util.*;

/** ***************************************************************       
 * Top-level data structure for the prover. The complete knowledge
 * base is split into two sets, processed clauses and unprocessed
 * clauses. These are represented here as individual clause sets. The
 * main algorithm "processes" clauses and moves them from the
 * unprocessed into the processed set. Processing typically generates
 * several new clauses, which are direct consequences of the given
 * clause and the processed clauses. These new clauses are added to
 * the set of unprocessed clauses.
 * 
 * In addition to the clause sets, this data structure also maintains
 * a number of counters for statistics on the proof search.
 */ 
public class ProofState {
     
    /* This defines the clause selection heuristic, i.e. the order in
       which unprocessed clauses are selected for processing. */
    public static boolean heuristics = false;
    /* This determines if tautologies will be deleted. Tautologies in
       plain first-order logic (without equality) are clauses which
       contain two literals with the same atom, but opposite signs. */
    public static boolean delete_tautologies = false;
    /* Forward-subsumption checks the given clause against already
       processed clauses, and discards it if it is subsumed. */
    public static boolean forward_subsumption = false;
    /* Backwards subsumption checks the processed clauses against the
       given clause, and discards all processed clauses that are
       subsumed. */
    public static boolean backward_subsumption = false;
    public static HeuristicClauseSet unprocessed = null;
    public static ClauseSet processed = null;
    public static int initial_clause_count = 0;
    public static int proc_clause_count    = 0;
    public static int factor_count         = 0;
    public static int resolvent_count      = 0;
    public static int tautologies_deleted  = 0;
    public static int forward_subsumed     = 0;
    public static int backward_subsumed    = 0;
    
    public static int stepCount            = 999;
    
    /** ***************************************************************
     * Initialize the proof state with a set of clauses.
     */  
    public ProofState(ClauseSet clauses, EvalStructure efunctions) {

        unprocessed = new HeuristicClauseSet(clauses, efunctions);                                         
        processed   = new ClauseSet();
        for (Clause c:clauses.clauses) 
            unprocessed.addClause(c);
        initial_clause_count = unprocessed.length();
        proc_clause_count    = 0;
        factor_count         = 0;
        resolvent_count      = 0;
        tautologies_deleted  = 0;
        forward_subsumed     = 0;
        backward_subsumed    = 0;
    }
    
    /** ***************************************************************
     * Pick a clause from unprocessed and process it. If the empty
     * clause is found, return it. Otherwise return null.
     */  
    public Clause processClause() {

        Clause given_clause = unprocessed.extractBest();
        given_clause = given_clause.freshVarCopy();
        System.out.println("#" + given_clause.toStringJustify());
        if (given_clause.isEmpty())
            // We have found an explicit contradiction
            return given_clause;
        if (delete_tautologies && given_clause.isTautology()) {
            tautologies_deleted = tautologies_deleted + 1;
            return null;        
        }
        if (forward_subsumption && Subsumption.forwardSubsumption(processed, given_clause)) {
            //  If the given clause is subsumed by an already processed
            //  clause, all relevant inferences will already have been
            //  done with that more general clause. So, we can remove
            //  the given clause. We keep count of how many clauses
            //  we have removed this way.
            forward_subsumed = forward_subsumed + 1;
            return null;
        }

        if (backward_subsumption) {
            //  If the given clause subsumes any of the already
            //  processed clauses, it will "cover" for these less
            //  general clauses in the future, so we can remove them
            //  from the proof state. We keep count of the number
            //  of clauses removed. This typically happens less often
            //  than forward subsumption, because most heuristics prefer
            //  smaller clauses, which tend to be more general (thus the
            //  processed clauses are typically, if not universally, more
            //  general than the new given clause).
            int tmp = Subsumption.backwardSubsumption(given_clause, processed);
            backward_subsumed = backward_subsumed + tmp;
        }
        ClauseSet newClauses = new ClauseSet();
        ClauseSet factors = ResControl.computeAllFactors(given_clause);
        newClauses.addAll(factors);
        ClauseSet resolvents = ResControl.computeAllResolvents(given_clause, processed);
        newClauses.addAll(resolvents);
        proc_clause_count = proc_clause_count + 1;
        factor_count = factor_count + factors.length();
        resolvent_count = resolvent_count + resolvents.length();

        processed.add(given_clause);

        for (Clause c:newClauses.clauses)
            unprocessed.addClause(c);
        return null;
    }
    
    /** ***************************************************************
     * Main proof procedure. If the clause set is found unsatisfiable, 
     * return the empty clause as a witness. Otherwise return null.
     */  
    public Clause saturate() {

        while (unprocessed.length() > 0) {
            Clause res = processClause();
            if (res != null)
                return res;
        }
        return null;
    }

    /** ***************************************************************
     * Return the proof state statistics in string form ready for output.
     */  
    public String generateStatisticsString() {

        StringBuffer sb = new StringBuffer();
        sb.append("# Initial clauses    : " + initial_clause_count + "\n");
        sb.append("# Processed clauses  : " + proc_clause_count + "\n");
        sb.append("# Factors computed   : " + factor_count + "\n");
        sb.append("# Resolvents computed: " + resolvent_count + "\n");
        sb.append("# Tautologies deleted: " + tautologies_deleted + "\n");
        sb.append("# Forward subsumed   : " + forward_subsumed + "\n");
        sb.append("# Backward subsumed  : " + backward_subsumed + "\n");
        return sb.toString();
    }
   
    /** ***************************************************************
     */  
    public String proof2String(TreeMap<String,Clause> proof, HashMap<String,String> nameMap) {
    
        StringBuffer sb = new StringBuffer();
        Iterator<String> it = proof.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Clause c = proof.get(key);
            c.name = nameMap.get(c.name);
            for (int i = 0; i < c.support.size(); i++) 
                c.support.set(i,nameMap.get(c.support.get(i)));            
            sb.append(String.format("%-5s", (nameMap.get(key) + ".")) + "\t" + c.toStringJustify() + "\n");
        }
        return sb.toString();
    }
    
    /** ***************************************************************
     */  
    public void renumber(TreeMap<String,Clause> proof, HashMap<String,String> nameMap) {
    
        int counter = 1;
        StringBuffer sb = new StringBuffer();
        Iterator<String> it = proof.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Clause c = proof.get(key);
            c.name = nameMap.get(c.name);
            for (int i = 0; i < c.support.size(); i++) 
                c.support.set(i,nameMap.get(c.support.get(i))); 
            nameMap.put(key, Integer.toString(counter++));
        }
    }
    
    /** ***************************************************************
     * @param proof is built as a side-effect
     */  
    public void generateProofRecurse(HashMap<String,Clause> clauseMap, HashMap<String,String> nameMap,
            Clause c, TreeMap<String,Clause> proof) {
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < c.support.size(); i++) {
            Clause newC = clauseMap.get(c.support.get(i));
            if (newC != null) {
                if (!proof.containsValue(newC)) {
                    stepCount--;
                    String newName = "step" + String.format("%5s", Integer.toString(stepCount)).replace(' ', '0');
                    nameMap.put(newC.name, newName);
                    proof.put(newName, newC);
                    generateProofRecurse(clauseMap,nameMap,newC,proof);
                }
            }
            else
                System.out.println("Error in : attempt to get non-existent clause: " + c.support.get(i));
        }
    }

    /** ***************************************************************
     * Return the proof.
     */  
    public String generateProof(Clause res) {

        HashMap<String,Clause> clauseMap = new HashMap<String,Clause>();
        for (int i = 0; i < processed.length(); i++) {
            Clause c = processed.get(i);
            clauseMap.put(c.name, c);
        }
        StringBuffer sb = new StringBuffer();
        TreeMap<String,Clause> proof = new TreeMap<String,Clause>();
        HashMap<String,String> nameMap = new HashMap<String,String>();
        String newName = "step" + String.format("%5s", Integer.toString(stepCount)).replace(' ', '0');
        nameMap.put(res.name, newName);
        proof.put(newName, res);
        generateProofRecurse(clauseMap,nameMap,res,proof);
        renumber(proof,nameMap);
        return proof2String(proof,nameMap);
    }
    
    /** ***************************************************************
     * ************ UNIT TESTS *****************
     */
    public static String spec1 = "cnf(axiom, a_is_true, a).\n" + 
        "cnf(negated_conjecture, is_a_true, ~a).\n";
    public static String spec3 = "cnf(p_or_q, axiom, p(X)|q(a)).\n" +
        "cnf(taut, axiom, p(X)|~p(X)).\n" +
        "cnf(not_p, axiom, ~p(a)).";
    
    /** ***************************************************************
     * Setup function for clause/literal unit tests. Initialize
     * variables needed throughout the tests.
     */
    public static void setUp() {

        delete_tautologies = true;
    }
    
    /** ***************************************************************
     * Evaluate the result of a saturation compared to the expected result.
     */
    public static void evalSatResult(ClauseSet cs, boolean provable) {
    
        System.out.println("SimpleProofState.evalSatResult(): problem: " + cs);
        ProofState prover = new ProofState(cs,ClauseEvaluationFunction.PickGiven5);
        Clause res = prover.saturate();

        if (provable) {
            if (res == null)
                System.out.println("# Failure: Should have found a proof!");
            else
                System.out.println("# Success: Proof found");
        }
        else {
            if (res != null)
                System.out.println("# Failure: Should not have found a proof!");
            else
                System.out.println("# Success: No proof found");
        }
        System.out.println(prover.generateStatisticsString());
    }
    
    /** ***************************************************************
     * Evaluate the result of a saturation compared to the expected result.
     */
    public static void evalSatResult(String spec, boolean provable) {

        System.out.println("INFO in ProofState.evalSatResult()");  
        StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(spec));        
        ClauseSet problem = new ClauseSet();
        problem.parse(st);      
        evalSatResult(problem,provable);
    }  
    
    /** ***************************************************************
     * Test that saturation works.
     */
    public static void testSaturation() {

        System.out.println("INFO in ProofState.testSaturation()");
        evalSatResult(spec1, true);
        evalSatResult(ClauseSet.parseFromFile("/home/apease/EProver/fod_pi/PYTHON/EXAMPLES/PUZ001-1.p"), true);
        //evalSatResult(spec3, false);
    }
    
    /** ***************************************************************
     * Test method for this class.
     */
    public static void main(String[] args) {
                
        ClauseEvaluationFunction.setupEvaluationFunctions();
        testSaturation();
    }    
}
