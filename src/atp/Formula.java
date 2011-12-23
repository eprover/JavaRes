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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

/** ***************************************************************
 * Datatype for the complete first-order formula, including 
 * meta-information like type and name.
 */
public class Formula {

    // Counter for generating new clause names.
    public static int formulaIdCounter = 0;
    public BareFormula form = null;
    public String type = "plain";
    public String name = "";
    public static String defaultPath = "/home/apease/Programs/TPTP-v5.2.0";

    /** ***************************************************************
     * Return a string representation of the formula.
     */
    public String toString() {

        return "fof(" + name + "," + type + "," + form + ").";
    }
    
    /** ***************************************************************
     * Set the name. If no name is given, generate a default name.
     */
    public void setName(String n) {

        if (!Term.emptyString(n))
            name = n;
        else {
            name = "f" + Integer.toString(formulaIdCounter);
            formulaIdCounter++;        
        }
    }

    /** ***************************************************************
     * Parse a formula in (slightly simplified) TPTP-3 syntax. It is
     *  written 
     *      fof(<name>, <type>, <lformula>).
     *  where <name> is a lower-case ident, type is a lower-case ident
     *  from a specific list, and <lformula> is a Formula.
     *  
     *  For us, all clause types are essentially the same, so we only
     *  distinguish "axiom", "conjecture", and "negated_conjecture", and
     *  map everything else to "plain".
     */
    public static Formula parse(Lexer lex) throws IOException, ParseException {

        lex.acceptLit("fof");
        lex.acceptTok(Lexer.OpenPar);
        String name = lex.lookLit();
        lex.acceptTok(Lexer.IdentLower);
        lex.acceptTok(Lexer.Comma);
        String type = lex.lookLit();
        if (!type.equals("axiom") && !type.equals("conjecture") && !type.equals("negated_conjecture"))
            type = "plain";
        lex.acceptTok(Lexer.IdentLower);
        lex.acceptTok(Lexer.Comma);

        BareFormula bform = BareFormula.parse(lex);
        
        lex.acceptTok(Lexer.ClosePar);
        lex.acceptTok(Lexer.FullStop);

        Formula f = new Formula();
        f.form = bform;
        f.name = name;
        f.type = type;
        return f;
    }
    
    /** ***************************************************************
     */
    public static ClauseSet file2clauses(Lexer lex) {
        
        ClauseSet cs = new ClauseSet();
        while (lex.type != Lexer.EOFToken) {
            try {
                String id = lex.look();
                //System.out.println("INFO in Formula.file2clauses(): id: " + lex.literal);
                if (id.equals("include")) {
                    lex.next();
                    lex.next();
                    if (lex.type != Lexer.OpenPar)
                        throw new ParseException("Error in Formula.file2clauses(): expected '(', found " + lex.literal,0);
                    lex.next();
                    String name = lex.literal;
                    if (name.charAt(0) == '\'') {
                        String filename = defaultPath + File.separator + name.substring(1,name.length()-1);
                        File f = new File(filename);
                        System.out.println("INFO in Formula.file2clauses(): start reading file: " + filename);
                        Lexer lex2 = new Lexer(f);
                        cs.addAll(file2clauses(lex2));
                        System.out.println("INFO in Formula.file2clauses(): completed reading file: " + filename);
                    }
                    lex.next();
                    if (lex.type != Lexer.ClosePar)
                        throw new ParseException("Error in Formula.file2clauses(): expected ')', found " + lex.literal,0);
                    lex.next();
                    if (lex.type != Lexer.FullStop)
                        throw new ParseException("Error in Formula.file2clauses(): expected '.', found " + lex.literal,0);
                }
                if (id.equals("fof")) {
                    Formula f = Formula.parse(lex);
                    System.out.println("INFO in Formula.file2clauses(): fof: " + f);
                    if (f.form != null) 
                        cs.addAll(Clausifier.clausify(f.form));                    
                }
                if (id.equals("cnf")) {
                    Clause clause = new Clause();
                    clause = clause.parse(lex);
                    System.out.println("INFO in Formula.file2clauses(): cnf: " + clause);
                    cs.add(clause);
                }
            }
            catch (ParseException p) {
                System.out.println(p.getMessage());
                p.printStackTrace();
                return cs;
            }
            catch (IOException p) {
                System.out.println(p.getMessage());
                p.printStackTrace();
                return cs;
            }
        }
        return cs;
    }
    
    /** ***************************************************************
     */
    private static ClauseSet file2clauses(String filename) {
        
        FileReader fr = null;
        try {
            File fin = new File(filename);
            fr = new FileReader(fin);
            if (fr != null) {
                Lexer lex = new Lexer(fin);
                return file2clauses(lex);
            }
        }
        catch (IOException e) {
            System.out.println("Error in Formula.file2clauses(): File error reading " + filename + ": " + e.getMessage());
            return null;
        }
        finally {
            try {
                if (fr != null) fr.close();
            }
            catch (Exception e) {
                System.out.println("Exception in Formula.file2clauses()" + e.getMessage());
            }
        }  
        return null;
    }
    
    /** ***************************************************************
     * Setup function for clause/literal unit tests. Initialize
     * variables needed throughout the tests.
     */
    public static String wformulas = "fof(small, axiom, ![X]:(a(x) | ~a=b))." + 
        "fof(complex, conjecture, (![X]:a(X)|b(X)|?[X,Y]:(p(X,f(Y))))<=>q(g(a),X))." + 
        "fof(clean, conjecture, ((((![X]:a(X))|b(X))|(?[X]:(?[Y]:p(X,f(Y)))))<=>q(g(a),X)))." + 
        "fof(queens_p,axiom,(queens_p => ![I,J]:((le(s(n0),I)& le(I,n) & le(s(I),J) & le(J,n) )=> ( p(I) != p(J) & plus(p(I),I) != plus(p(J),J) & minus(p(I),I) != minus(p(J),J) ) ) )).";
            
    /** ***************************************************************
     */
    public static void testWrappedFormula() {
        
        try {
            Lexer lex = new Lexer(wformulas); 
            Formula f1 = Formula.parse(lex);
            System.out.println("Result 1: " + f1);
            System.out.println();
            Formula f2 = Formula.parse(lex);
            System.out.println("Result 2: " + f2);
            System.out.println();
            Formula f3 = Formula.parse(lex);
            System.out.println("Result 3: " + f3);
            System.out.println();
            Formula f4 = Formula.parse(lex);
            System.out.println("Result 4: " + f4);
            System.out.println();
        }
        catch (Exception e) {
            System.out.println("Error in Formula.testWrappedFormula()");
            System.out.println(e.getMessage());
            e.printStackTrace();            
        }
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {
        
        //testWrappedFormula();
        if (args.length < 1)
            System.out.println("Error in main(), expected a filename");
        else
            System.out.println(file2clauses(args[0]));
    }
}
