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
        "--experiment\n" +
        "Run an experiment to total times for all tests in a given directory.\n";

    /** ***************************************************************
     * canonicalize options into a name/value list
     */
    public static HashMap<String,String> processOptions(String[] args) {
        
        HashMap<String,String> result = new HashMap<String,String>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                if (arg.equals("--delete-tautologies"))
                    result.put("delete-tautologies","true");
                if (arg.equals("--forward-subsumption"))
                    result.put("forward-subsumption","true");
                if (arg.equals("--backward_subsumption"))
                    result.put("backward_subsumption","true");
                if (arg.equals("--experiment"))
                    result.put("experiment", "true");
            }
            else if (arg.startsWith("-")) {
                for (int j = 1; j < arg.length(); j++) {
                    if (arg.charAt(j) == 't')
                        result.put("delete-tautologies","true");
                    if (arg.charAt(j) == 'f')
                        result.put("forward-subsumption","true");
                    if (arg.charAt(j) == 'b')
                        result.put("backward_subsumption","true");
                }
            }
            else
                result.put("filename",arg);
        }
        return result;
    }
    
    /** ***************************************************************
     * results are side effects on proofState
     */
    public static void setStateOptions(ProofState state, HashMap<String,String> opts) {
        
        if (opts.containsKey("delete-tautologies"))
            state.delete_tautologies = true;
        else if (opts.containsKey("forward-subsumption"))
            state.forward_subsumption = true;
        else if (opts.containsKey("backward_subsumption"))
            state.backward_subsumption = true;
    }
        
    /** ***************************************************************
     */
    public static ProofState processTestFile(String filename, HashMap<String,String> opts) {
        
        FileReader fr = null;
        try {
            File fin = new File(filename);
            System.out.println("INFO in Prover2.processTestFile(): reading file " + filename);
            fr = new FileReader(fin);
            if (fr != null) {
                StreamTokenizer_s st = new StreamTokenizer_s(fr);  
                Term.setupStreamTokenizer(st);
                ClauseSet cs = new ClauseSet();
                cs.parse(st);
                //System.out.println(cs);
                ClauseEvaluationFunction.setupEvaluationFunctions();
                ProofState state = new ProofState(cs,ClauseEvaluationFunction.PickGiven5);
                setStateOptions(state,opts);
                state.res = state.saturate(5);
                if (state.res != null)
                    return state;
                else 
                    return null;
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
            HashMap<String,String> opts = processOptions(args);  // canonicalize options
            if (opts.containsKey("experiment")) {
                File dir = new File(opts.get("filename")); 
                String[] children = dir.list();
                if (children != null) {
                    HashMap<String,String> results = new HashMap<String,String>();
                    for (int i = 0; i < children.length; i++) {
                        String filename = opts.get("filename") + File.separator + children[i];
                        if (filename.endsWith(".p")) {
                            System.out.println("************ Testing Problem " + children[i] + " **************");
                            ProofState state = processTestFile(filename,opts);
                            if (state != null) {
                                System.out.println(state.generateStatisticsString());
                                System.out.println("# SZS status Unsatisfiable");
                                System.out.println(state.generateProof(state.res,false));
                                state.SZSresult = "Unsatisfiable";
                            }
                            else
                                System.out.println("# SZS status: Satisfiable: " + state.SZSresult);      
                            results.put(filename,Long.toString(state.time) + " milliseconds, with status: " + state.SZSresult);
                        }
                    }
                    Iterator<String> it = results.keySet().iterator();
                    while (it.hasNext()) {
                        String key = it.next();
                        String value = results.get(key);
                        System.out.println(key + " : " + value);
                    }
                }
            }
            else {
                ProofState state = processTestFile(opts.get("filename"),opts);
                if (state != null) {
                    System.out.println(state.generateStatisticsString());
                    System.out.println("# SZS status Unsatisfiable");
                    System.out.println(state.generateProof(state.res,false));
                }
                else
                    System.out.println("# SZS status Satisfiable");                    
            }                            
        }
    }
}

