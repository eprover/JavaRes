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
import java.util.*;

public class Prover2 {
    
    private static String doc = "prover.py 0.1\n" + 
        "\n" + 
        "Usage: prover.py [options] <problem_file>\n" + 
        "\n" + 
        "This is a straightforward implementation of a simple resolution-based\n" + 
        "prover for first-order clausal logic. Problem file should be in\n" + 
        "(restricted) TPTP-3 CNF syntax. Unsupported features include single-\n" + 
        "and double quoted strings and includes. Equality is parsed, but not\n" + 
        "interpreted so far.\n" + 
        "\n" + 
        "Options:\n" + 
        "\n" + 
        " -h\n" + 
        "--help\n" +
        "Print this help.\n" +
        "\n" +
        " -to\n" +
        "--timeout\n" +
        "Must be followed by an integer, which is a timeout in seconds.\n" +
        "\n" +
        " -t\n" +
        "--delete-tautologies\n" +
        "Discard the given clause if it is a tautology.\n" +
        "\n" +
        " -f\n" +
        "--forward-subsumption\n" +
        "Discard the given clause if it is subsumed by a processed clause.\n" +
        "\n" +
        " -b\n" +
        "--backward-subsumption\n" +
        "Discard processed clauses if they are subsumed by the given clause.\n" +
        " --experiment\n" +
        "Run an experiment to total times for all tests in a given directory.\n" +
        " --allOpts\n" +
        "Run all options.  Ignore -tfb command line options and try in all combination.\n" +
        " --allStrat\n" +
        "Run all clause selection strategies.\n" +
        " -d\n" +
        "Generate proof output in dot-graph format\n";

    public static String errors = "";
    
    /** ***************************************************************
     * canonicalize options into a name/value list.
     * If the --allOpts flag is set remove all other subsumption/deletion
     * options, since all will be tried.
     * @return a HashMap of name/value pairs, or null if there's an error.
     */
    public static HashMap<String,String> processOptions(String[] args) {
        
        HashMap<String,String> result = new HashMap<String,String>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                if (arg.equals("--allOpts"))
                    result.put("allOpts", "true");
                if (arg.equals("--experiment"))
                    result.put("experiment", "true");
                if (arg.equals("--allStrat"))
                    result.put("allStrat", "true");
                if (arg.equals("--delete-tautologies"))
                    result.put("delete-tautologies","true");
                if (arg.equals("--forward-subsumption"))
                    result.put("forward-subsumption","true");
                if (arg.equals("--backward_subsumption"))
                    result.put("backward_subsumption","true");
                if (arg.equals("--timeout")) {
                    try {
                         int val = Integer.parseInt(args[i+1]);
                    }
                    catch (NumberFormatException n) {
                        return null;
                    }
                    result.put("timeout",args[i+1]);
                }
            }
            else if (arg.startsWith("-")) {
                for (int j = 1; j < arg.length(); j++) {
                    if (arg.charAt(j) == 't')
                        result.put("delete-tautologies","true");
                    if (arg.charAt(j) == 'f')
                        result.put("forward-subsumption","true");
                    if (arg.charAt(j) == 'b')
                        result.put("backward_subsumption","true");
                    if (arg.charAt(j) == 'd')
                        result.put("dotgraph","true");
                    if (arg.equals("-to")) {
                        try {
                             int val = Integer.parseInt(args[i+1]);
                        }
                        catch (NumberFormatException n) {
                            return null;
                        }
                        result.put("timeout",args[i+1]);
                    }
                }
            }
            else
                result.put("filename",arg);
        }
        if (result.containsKey("allOpts")) {
            if (result.containsKey("delete-tautologies"))                
                result.remove("delete-tautologies");
            if (result.containsKey("forward-subsumption"))
                result.remove("forward-subsumption");
            if (result.containsKey("backward_subsumption"))
                result.remove("backward_subsumption");
        }
        return result;
    }
    
    /** ***************************************************************
     * results are side effects on proofState
     */
    public static void setStateOptions(ProofState state, HashMap<String,String> opts) {
        
        if (opts.containsKey("delete-tautologies"))
            state.delete_tautologies = true;
        if (opts.containsKey("forward-subsumption"))
            state.forward_subsumption = true;
        if (opts.containsKey("backward_subsumption"))
            state.backward_subsumption = true;
    }

    /** ***************************************************************
     */
    public static ArrayList<EvalStructure> setAllEvalOptions() {
        
        ArrayList<EvalStructure> result = new ArrayList<EvalStructure>();
        result.add(ClauseEvaluationFunction.FIFOEval);
        result.add(ClauseEvaluationFunction.SymbolCountEval);
        result.add(ClauseEvaluationFunction.PickGiven5);
        result.add(ClauseEvaluationFunction.PickGiven2);
        return result;
    }
        
    /** ***************************************************************
     */
    public static ArrayList<ProofState> setAllStateOptions(ClauseSet clauses, EvalStructure efunctions) {
        
        ArrayList<ProofState> result = new ArrayList<ProofState>();
        for (int i = 0; i < 8; i++) {
            ProofState state = new ProofState(clauses,efunctions);
            if ((i & 1) == 0)
                state.delete_tautologies = false;
            else
                state.delete_tautologies = true;
            if ((i & 2) == 0)
                state.forward_subsumption = false;
            else
                state.forward_subsumption = true;
            if ((i & 4) == 0)
                state.backward_subsumption = false;
            else
                state.backward_subsumption = true;
            result.add(state);
        }
        return result;
    }

    /** ***************************************************************
     */
    private static int getTimeout(HashMap<String,String> opts) {
        
        if (opts.containsKey("timeout"))
            return Integer.parseInt(opts.get("timeout"));
        else
            return 5;
    }
    
    /** ***************************************************************
     */
    private static ProofState processTestFile(String filename, HashMap<String,String> opts, ArrayList<EvalStructure> evals) {
        
        FileReader fr = null;
        try {
            File fin = new File(filename);
            fr = new FileReader(fin);
            if (fr != null) {
                StreamTokenizer_s st = new StreamTokenizer_s(fr);  
                Term.setupStreamTokenizer(st);
                ClauseSet cs = new ClauseSet();
                cs.parse(st);                 
                for (int i = 0; i < evals.size(); i++) {
                    EvalStructure eval = evals.get(i);
                    if (opts.containsKey("allOpts")) {
                        ArrayList<ProofState> states = setAllStateOptions(cs,evals.get(i));
                        for (int j = 0; j < states.size(); j++) {
                            ProofState state = states.get(j);
                            int timeout = getTimeout(opts);
                            state.filename = filename;
                            state.evalFunctionName = eval.name;                            
                            state.res = state.saturate(timeout);
                            if (state.res != null)
                                System.out.println(state.generateMatrixStatisticsString());
                        }
                    }
                    else {
                        ProofState state = new ProofState(cs,evals.get(i)); 
                        setStateOptions(state,opts);
                        int timeout = getTimeout(opts);
                        state.res = state.saturate(timeout);
                        if (state.res != null)
                            return state;
                    }
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error in Prover2.processTestFile(): File error reading " + filename + ": " + e.getMessage());
            return null;
        }
        finally {
            try {
                if (fr != null) fr.close();
            }
            catch (Exception e) {
                System.out.println("Exception in Prover2.processTestFile()" + e.getMessage());
            }
        }  
        return null;
    }
    
    /** ***************************************************************
     * Test method for this class.  
     */
    public static void main(String[] args) {
          
        if (args[0].equals("-h") || args[0].equals("--help")) {
            System.out.println(doc);
            return;
        }
        if (!Term.emptyString(args[0])) {
            ClauseEvaluationFunction.setupEvaluationFunctions();
            ArrayList<EvalStructure> evals = null;
            HashMap<String,String> opts = processOptions(args);  // canonicalize options
            if (opts == null) {
                System.out.println("Error in Prover2.main(): bad command line options.");
                return;
            }
                
            if (opts.containsKey("allStrat")) 
                evals = setAllEvalOptions();            
            else {
                evals = new ArrayList<EvalStructure>();
                evals.add(ClauseEvaluationFunction.PickGiven5);
            }
            boolean dotgraph = false;
            if (opts.containsKey("dotgraph"))
                dotgraph = true;
            if (opts.containsKey("experiment")) {
                ProofState.generateMatrixHeaderStatisticsString();
                File dir = new File(opts.get("filename")); 
                String[] children = dir.list();
                if (children != null) {
                    for (int i = 0; i < children.length; i++) {
                        String filename = opts.get("filename") + File.separator + children[i];
                        if (filename.endsWith(".p")) 
                            processTestFile(filename,opts,evals);                        
                    }
                }
            }
            else {
                evals = new ArrayList<EvalStructure>();
                evals.add(ClauseEvaluationFunction.PickGiven5);
                ProofState state = processTestFile(opts.get("filename"),opts,evals);
                if (state != null) {
                    System.out.println(state.generateStatisticsString());
                    System.out.println("# SZS status Unsatisfiable");
                    System.out.println(state.generateProof(state.res,dotgraph));
                }
                else
                    System.out.println("# SZS status Satisfiable");                    
            }                            
        }
    }
}

