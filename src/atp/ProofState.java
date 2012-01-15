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
     
    /* This determines if tautologies will be deleted. Tautologies in
       plain first-order logic (without equality) are clauses which
       contain two literals with the same atom, but opposite signs. */
    public boolean delete_tautologies = false;
    /* Forward-subsumption checks the given clause against already
       processed clauses, and discards it if it is subsumed. */
    public boolean forward_subsumption = false;
    /* Backwards subsumption checks the processed clauses against the
       given clause, and discards all processed clauses that are
       subsumed. */
    public boolean backward_subsumption = false;
    public HeuristicClauseSet unprocessed = null; // eval_functions.eval_funs .name
    public ClauseSet processed = null;
    public int initial_clause_count = 0;
    public int proc_clause_count    = 0;
    public int factor_count         = 0;
    public int resolvent_count      = 0;
    public int tautologies_deleted  = 0;
    public int forward_subsumed     = 0;
    public int backward_subsumed    = 0;
    public long time                = 0;  // in milliseconds
    public Clause res               = null;
    public String SZSresult         = "";  // result as specified by SZS "ontology"
    public String filename          = "";
    public String evalFunctionName  = "";
    
    /** ***************************************************************
     * Initialize the proof state with a set of clauses.
     */  
    public ProofState(ClauseSet clauses, EvalStructure efunctions) {

        unprocessed = new HeuristicClauseSet(clauses, efunctions);                                         
        processed   = new ClauseSet();
        for (Clause c:clauses.clauses) 
            unprocessed.addClause(c.deepCopy());
        initial_clause_count = unprocessed.length();
        proc_clause_count    = 0;
        factor_count         = 0;
        resolvent_count      = 0;
        tautologies_deleted  = 0;
        forward_subsumed     = 0;
        backward_subsumed    = 0;
        time                 = 0;
    }
    
    /** ***************************************************************
     */  
    public String toString() {
        
        return generateStatisticsString();
    }
    
    /** ***************************************************************
     */  
    public String toStringOpts() {
        
        StringBuffer sb = new StringBuffer();
        sb.append("file : " + filename + "\n");
        sb.append(" delete_tautologies : " + delete_tautologies + "\n");
        sb.append(" forward_subsumption : " + forward_subsumption + "\n");
        sb.append(" backward_subsumption : " + backward_subsumption + "\n");
        sb.append(" eval function name : " + evalFunctionName + "\n");
        //for (int i = 0; i < unprocessed.eval_functions.eval_funs.size(); i++)
        //    sb.append(" evalFn : " + unprocessed.eval_functions.eval_funs.get(i).name + "\n");
        sb.append(generateStatisticsString());
        return sb.toString();
    }
    
    /** ***************************************************************
     * Pick a clause from unprocessed and process it. If the empty
     * clause is found, return it. Otherwise return null.
     */  
    public Clause processClause() {

        Clause given_clause = unprocessed.extractBest();
        given_clause = given_clause.freshVarCopy();
        //System.out.println("#" + given_clause.toStringJustify());
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
     * Allow timeout to terminate the search.
     * The timeout needs to be made more sophisticated, with a system
     * interrupt, since just processing one clause could take infinite
     * time, and therefore a timeout in this method would never occur.
     */  
    public Clause saturate(int seconds) {

        long t1 = System.currentTimeMillis();
        while (unprocessed.length() > 0) {
            Clause res = processClause();
            if (res != null) {
                time = System.currentTimeMillis() - t1;
                return res;
            }
            if (((System.currentTimeMillis() - t1) / 1000.0) > seconds) {
                SZSresult = "timeout";
                time = System.currentTimeMillis() - t1;
                return null;
            }
        }
        return null;
    }
    
    /** ***************************************************************
     */  
    public Clause saturate() {

        return saturate(32000);
    }

    /** ***************************************************************
     * Return the proof state statistics in string form ready for output.
     */  
    public String generateStatisticsString() {

        StringBuffer sb = new StringBuffer();
        sb.append("# Filename           : " + filename + "\n");
        sb.append("# Initial clauses    : " + initial_clause_count + "\n");
        sb.append("# Processed clauses  : " + proc_clause_count + "\n");
        sb.append("# Factors computed   : " + factor_count + "\n");
        sb.append("# Resolvents computed: " + resolvent_count + "\n");
        sb.append("# Tautologies deleted: " + tautologies_deleted + "\n");
        sb.append("# Forward subsumed   : " + forward_subsumed + "\n");
        sb.append("# Backward subsumed  : " + backward_subsumed + "\n");
        sb.append("# time               : " + time + "\n");
        return sb.toString();
    }
    
    /** ***************************************************************
     */  
    public static String generateMatrixHeaderStatisticsString() {

        StringBuffer sb = new StringBuffer();
        sb.append("Filename,");
        sb.append("Delete_tautologies,");
        sb.append("Forward_subsumption,");
        sb.append("Backward_subsumption,");
        sb.append("Eval function name,");
        sb.append("Initial clauses,");
        sb.append("Processed clauses,");
        sb.append("Factors computed,");
        sb.append("Resolvents computed,");
        sb.append("Tautologies deleted,");
        sb.append("Forward subsumed,");
        sb.append("Backward subsumed,");
        sb.append("Time,");
        return sb.toString();
    }
    
    /** ***************************************************************
     * Return the proof state statistics in spreadsheet form.
     */  
    public String generateMatrixStatisticsString() {

        StringBuffer sb = new StringBuffer();
        sb.append(filename + ",");
        sb.append(delete_tautologies + ",");
        sb.append(forward_subsumption + ",");
        sb.append(backward_subsumption + ",");
        sb.append(evalFunctionName + ",");
        sb.append(initial_clause_count + ",");
        sb.append(proc_clause_count + ",");
        sb.append(factor_count + ",");
        sb.append(resolvent_count + ",");
        sb.append(tautologies_deleted + ",");
        sb.append(forward_subsumed + ",");
        sb.append(backward_subsumed + ",");
        sb.append(time);
        return sb.toString();
    }
    
    /** ***************************************************************
     * Get all clauses used in the proof
     */  
    public HashMap<String,Clause> searchProof(HashMap<String,Clause> clauseMap, Clause source) {
        
        //System.out.println("INFO in ProofState.searchProof(): source: " + source.toStringJustify());
        HashMap<String,Clause> proof = new HashMap<String,Clause>();
        proof.put(source.name,source);
        for (int i = 0; i < source.support.size(); i++) {
            Clause w = clauseMap.get(source.support.get(i));
            if (w == null)
                System.out.println("Error in ProofState.searchProof(): no clause for id " + source.support.get(i));
            else
                proof.putAll(searchProof(clauseMap,w));
        }
        return proof;
    }
       
    /** ***************************************************************
     */  
    public class GraphNode {
        String name = null;
        String clause = null;
        
          //list of nodes that this node supports
        ArrayList<String> pointersToNodes = new ArrayList<String>();
        ArrayList<String> pointersFromNodes = new ArrayList<String>();
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(name + " \n");
            sb.append("    (");
            for (int i = 0; i < pointersToNodes.size(); i++) {
                sb.append(pointersToNodes.get(i));
                if (i < pointersToNodes.size() - 1)
                    sb.append(",");
            }
            sb.append(")\n");
            sb.append("    (");
            for (int i = 0; i < pointersFromNodes.size(); i++) {
                sb.append(pointersFromNodes.get(i));
                if (i < pointersFromNodes.size() - 1)
                    sb.append(",");
            }
            sb.append(")\n");               
            return sb.toString();
        }
    }

    /** *************************************************************** 
     */  
    public String dotGraph(HashMap<String,GraphNode> graph) {
    
        StringBuffer sb = new StringBuffer();
        sb.append("digraph \"inference tree\" {\n");
        Iterator<String> it = graph.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            GraphNode gn = graph.get(key);
            sb.append(gn.name + " [shape=record label=\"{" + gn.name + " | " + gn.clause + "}\"];\n");
            for (int i = 0; i < gn.pointersToNodes.size(); i++) {
                sb.append(gn.name + " -> " + gn.pointersToNodes.get(i) + ";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }
    
    /** ***************************************************************
     * Create a graph of GraphNode, where each node has both pointers
     * and backpointers.  This routine reverses the effective direction
     * of the clauseMap pointers.  clauseMap points from clauses, to the 
     * clauses that support them.  The graph will point from the supporting
     * nodes to the supported nodes, along with backpointers that copy the
     * direction of the original clauseMap.
     * @param clauseMap is a list of clauses with backpointers
     * @return a map 
     */  
    public HashMap<String,GraphNode> createGraph(HashMap<String,Clause> clauseMap) {
    
        HashMap<String,GraphNode> graph = new HashMap<String,GraphNode>();
        Iterator<String> it = clauseMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Clause c = clauseMap.get(key);
            // System.out.println("INFO in ProofState.createGraph(): " + c.toString());
            for (int i = 0; i < c.support.size(); i++) {   // get backpointers from c
                String nodeStr = c.support.get(i);         // nodeStr points from c
                GraphNode node = null;                     // node is the source of a pointer to c
                if (graph.containsKey(nodeStr))            // a pointer from c
                    node = graph.get(nodeStr);
                else {
                    node = new GraphNode();
                    node.name = nodeStr;
                    graph.put(nodeStr,node);
                }                                
                node.pointersToNodes.add(c.name);
                if (Term.emptyString(node.clause)) {
                    Clause c2 = clauseMap.get(nodeStr);
                    node.clause = c2.toString(true);
                }
                
                String nodeStr2 = c.name;                   // nodeStr2 points to c
                GraphNode node2 = null;                     // node2 is c
                if (graph.containsKey(nodeStr2))
                    node2 = graph.get(nodeStr2);
                else {
                    node2 = new GraphNode();
                    node2.name = nodeStr2;
                    graph.put(nodeStr2,node2);
                }
                if (Term.emptyString(node2.clause))
                    node2.clause = c.toString(true);
                //System.out.println("INFO in ProofState.createGraph() 2 : " + c.toString());
                node2.pointersFromNodes.add(nodeStr); 
            }
        }
        return graph;
    }
    
    /** ***************************************************************
     */  
    public LinkedList<GraphNode> getBeginners(HashMap<String,GraphNode> graph) {
        
        LinkedList<GraphNode> result = new LinkedList<GraphNode>();
        Iterator<String> it = graph.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            GraphNode c = graph.get(key);
            if (c.pointersFromNodes == null || c.pointersFromNodes.size() == 0)
                result.add(c);
        }
        return result;
    }
    
    /** ***************************************************************
     * Assume that there are no cycles. Algorithm per
     * Kahn, A. B. (1962), "Topological sorting of large networks", 
     * Communications of the ACM 5 (11): 558–562, doi:10.1145/368996.369025.
     * http://en.wikipedia.org/wiki/Topological_sorting
     * 
     * L ← Empty list that will contain the sorted elements
     * S ← Set of all nodes with no incoming edges
     * while S is non-empty do
     *     remove a node n from S
     *     insert n into L
     *     for each node m with an edge e from n to m do
     *         remove edge e from the graph
     *         if m has no other incoming edges then
     *             insert m into S
     * if graph has edges then
     *     output error message (graph has at least one cycle)
     * else
     *     output message (proposed topologically sorted order: L)
     */  
    public HashMap<String,String> toposort(HashMap<String,GraphNode> graph, LinkedList<GraphNode> S) {
    
        int count = 0;
        HashMap<String,String> L = new HashMap<String,String>();        
        // L ← Empty list that will contain the sorted elements
        // S ← Set of all nodes with no incoming edges
        while (S.size() > 0) {
            GraphNode n = S.removeFirst();  // looking at pointers from n->m
            count++;
            String pointer = String.format("%05d", count);
            L.put(n.name, pointer);
            for (int i = 0; i < n.pointersToNodes.size(); i++) {  // node.pointersFromNode, pointersToNode
                String key = n.pointersToNodes.get(i);
                GraphNode m = graph.get(key);
                n.pointersToNodes.remove(key);
                m.pointersFromNodes.remove(n.name);
                i--;  // list has shrunk, so must the index
                if (m.pointersFromNodes.size() == 0) {
                    S.add(m);
                }
            }
        }
        return L;
    }

    /** ***************************************************************
     */  
    public String proof2String(TreeMap<String,Clause> proof) {
    
        StringBuffer sb = new StringBuffer();
        Iterator<String> it = proof.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Clause c = proof.get(key);           
            sb.append(String.format("%-5s", (c.name + ".")) + "\t" + c.toStringJustify() + "\n");
        }
        return sb.toString();
    }
    
    /** ***************************************************************
     * Rename the clauses (including the "support" list in each clause).
     */  
    public TreeMap<String,Clause> renumber(HashMap<String,Clause> clauseMap, HashMap<String,String> nameMap) {

        //System.out.println("\nINFO in ProofState.renumber() clauseMap: " + clauseMap);
        //System.out.println("\nINFO in ProofState.renumber() nameMap: " + nameMap);
        TreeMap<String,Clause> proof = new TreeMap<String,Clause>();        
        Iterator<String> it = clauseMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Clause c = clauseMap.get(key);               
            c.name = nameMap.get(c.name);
            if (c.name != null) {
                for (int i = 0; i < c.support.size(); i++) {             
                    String pointer = nameMap.get(c.support.get(i));
                    c.support.set(i,pointer);
                }
                //System.out.println("\nINFO in ProofState.renumber(): " + c);
                proof.put(c.name,c);
            }
        }
        return proof;
    }
 
    /** ***************************************************************
     */  
    public ArrayList<Term> extractAnswerRecurse(TreeMap<String,Clause> proof, String id, ArrayList<Term> vars) {
        
        //System.out.println("INFO in ProofState.extractAnswerRecurse(): checking: " + id);
        ArrayList<Term> newvars = new ArrayList<Term>();
        newvars.addAll(vars);
        Clause c = proof.get(id);
        newvars = c.subst.applyList(newvars);
        Iterator<String> it = proof.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Clause val = proof.get(key);
            for (int i = 0; i < val.support.size(); i++) {
                if (val.support.get(i).equals(id))
                    newvars = extractAnswerRecurse(proof,val.name,newvars);
            }
        }       
        return newvars; 
    }
    
    /** ***************************************************************
     * Extract an answer binding for each unbound variable in a 
     * negated conjecture.
     */  
    public String extractAnswer(TreeMap<String,Clause> proof, Clause conjecture) {
        
        //System.out.println("INFO in ProofState.extractAnswer(): conjecture: " + conjecture);
        Clause conjectureNorm = conjecture.normalizeVarCopy();
        //System.out.println("INFO in ProofState.extractAnswer(): conjectureNorm: " + conjectureNorm);
        StringBuffer sb = new StringBuffer();
        ArrayList<Term> vars = conjecture.collectVars();
        Iterator<String> it = proof.keySet().iterator();
        String conjectKey = "";
        Clause conjectValue = null;
        while (it.hasNext()) {  // find conjecture in proofs
            String key = it.next();
            Clause c = proof.get(key);
            if (c.normalizeVarCopy().equals(conjectureNorm)) {
                conjectKey = key;
                conjectValue = c;
            }
        }
        if (conjectValue == null) {
            System.out.println("Error in ProofState.extractAnswer(): conjecture: " + conjecture + " not found.");
            return null;
        }
        ArrayList<Term> map = extractAnswerRecurse(proof,conjectKey,vars);
        sb.append(map.toString());
        return sb.toString();
    }
    
    /** ***************************************************************
     * @param clauseMap is an output variable
     * @param graph is an output variable
     */  
    public void generateProofGraph(Clause res, HashMap<String,Clause> clauseMap, 
            HashMap<String,GraphNode> graph) {

        for (int i = 0; i < processed.length(); i++) {
            Clause c = processed.get(i);
            clauseMap.put(c.name, c);
        }
        clauseMap.putAll(searchProof(clauseMap,res));    // get just the clauses in the proof    
        graph.putAll(createGraph(clauseMap));            // turn into a graph with pointers and backpointers
    }

    /** ***************************************************************
     */  
    public TreeMap<String,Clause> generateProofTree(Clause res) {
    
        HashMap<String,Clause> clauseMap = new HashMap<String,Clause>();   
        HashMap<String,GraphNode> graph = new HashMap<String,GraphNode>();
        generateProofGraph(res,clauseMap,graph);
        //System.out.println("INFO in ProofState.generateProofTree() graph: " + graph);
        //System.out.println("\nINFO in ProofState.generateProofTree() clauseMap: " + clauseMap);
        //System.out.println("\nINFO in ProofState.generateProofTree() processed: " + processed);
        LinkedList<GraphNode> beginners = getBeginners(graph);      // get all clauses from KB and conjecture
        //System.out.println("\nINFO in ProofState.generateProofTree() beginners: " + beginners);
        HashMap<String,String> nameMap = toposort(graph, beginners);
        //System.out.println("\nINFO in ProofState.generateProofTree() nameMap: " + nameMap);
        TreeMap<String,Clause> proof = renumber(clauseMap,nameMap);
        return proof;
    }
    
    /** ***************************************************************
     */  
    public String generateStringProof(Clause res) {
    
        return proof2String(generateProofTree(res));
    }

    /** ***************************************************************
     */  
    public String generateDotGraphProof(Clause res) {
    
        HashMap<String,Clause> clauseMap = new HashMap<String,Clause>();   
        HashMap<String,GraphNode> graph = new HashMap<String,GraphNode>();
        generateProofGraph(res,clauseMap,graph);
        return dotGraph(graph);
    }
    
    /** ***************************************************************
     * Generate a proof String, built by traversing pointers in the
     * processed list from the $false clause.
     */  
    public String generateProof(Clause res, boolean dotgraph) {

        if (dotgraph)
            return generateDotGraphProof(res);
        else 
            return generateStringProof(res);
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
        Lexer lex = new Lexer(spec);        
        ClauseSet problem = new ClauseSet();
        problem.parse(lex);      
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
