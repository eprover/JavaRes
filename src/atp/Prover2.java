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
        "Discard processed clauses if they are subsumed by the given clause.\n";

    /** ***************************************************************
     * Test method for this class.  
     */
    public static void main(String[] args) {
          
        if (args[0].equals("-h") || args[0].equals("--help")) {
            System.out.println(doc);
            return;
        }
        if (!Term.emptyString(args[0])) {
            ClauseSet problem = new ClauseSet();
            FileReader fr = null;
            try {
                File fin  = new File(args[1]);
                fr = new FileReader(fin);
                if (fr != null) {
                    StreamTokenizer_s st = new StreamTokenizer_s(fr);  
                    Term.setupStreamTokenizer(st);
                    ClauseSet cs = new ClauseSet();
                    cs.parse(st);
                    ClauseEvaluationFunction.setupEvaluationFunctions();
                    ProofState state = new ProofState(cs,ClauseEvaluationFunction.PickGiven5);
                    if (args[0].equals("-t") || args[0].equals("--delete-tautologies"))
                        state.delete_tautologies = true;
                    else if (args[0].equals("-f") || args[0].equals("--forward-subsumption"))
                        state.forward_subsumption = true;
                    else if (args[0].equals("-b") || args[0].equals("--backward_subsumption"))
                        state.backward_subsumption = true;
                    Clause res = state.saturate();
                    System.out.println(state.generateStatisticsString());
                    if (res != null) {
                        System.out.println("# SZS status Unsatisfiable");
                        System.out.println(state.generateProof(res,false));
                    }
                    else
                        System.out.println("# SZS status Satisfiable");
                }
            }
            catch (IOException e) {
                System.out.println("Error in Prover2.main(): File error reading " + args[1] + ": " + e.getMessage());
                return;
            }
            finally {
                try {
                    if (fr != null) fr.close();
                }
                catch (Exception e) {
                    System.out.println("Exception in Prover2.main()" + e.getMessage());
                }
            }                        
        }
    }
}

