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
    
    // TPTP file include paths
    public static String includePath = null;  
    public static String defaultPath = "/home/apease/Programs/TPTP-v5.3.0";

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
     * timeout if the total time to process the file exceeds a certain
     * amount.  Typically, this is called with a timeout equal to the timeout
     * for finding a refutation, so it should be more than adequate barring
     * an unusual situation.
     */
    public static ClauseSet file2clauses(Lexer lex, int timeout) {
        
        long t1 = System.currentTimeMillis();
        ClauseSet cs = new ClauseSet();
        System.out.println("# INFO in Formula.file2clauses(): reading file: " + lex.filename +
                " with read timeout: " + timeout); 
        System.out.print("#");  
        while (lex.type != Lexer.EOFToken) {
            try {
                if (lex.input.getLineNumber() % 1000 == 0)
                    System.out.print(".");
                if (((System.currentTimeMillis() - t1) / 1000.0) > timeout) 
                    return null;
                String id = lex.look();
                //System.out.println("INFO in Formula.file2clauses(): id: " + lex.literal + " " + lex.type);
                if (id.equals("include")) {
                    lex.next();
                    lex.next();
                    if (lex.type != Lexer.OpenPar)
                        throw new ParseException("Error in Formula.file2clauses(): expected '(', found " + lex.literal,0);
                    lex.next();
                    String name = lex.literal;
                    if (name.charAt(0) == '\'') {
                        String filename = null;
                        if (includePath == null)
                            filename = defaultPath + File.separator + name.substring(1,name.length()-1);
                        else
                            filename = includePath + File.separator + name.substring(1,name.length()-1);
                        File f = new File(filename);
                        //System.out.println("INFO in Formula.file2clauses(): start reading file: " + filename);
                        Lexer lex2 = new Lexer(f);
                        lex2.filename = filename;
                        System.out.println();
                        ClauseSet newcs = file2clauses(lex2,timeout);
                        if (newcs != null)
                            cs.addAll(newcs);
                        else
                            return null;
                        //System.out.println("INFO in Formula.file2clauses(): completed reading file: " + filename);
                    }
                    lex.next();
                    if (lex.type != Lexer.ClosePar)
                        throw new ParseException("Error in Formula.file2clauses(): expected ')', found " + lex.literal,0);
                    lex.next();
                    if (lex.type != Lexer.FullStop)
                        throw new ParseException("Error in Formula.file2clauses(): expected '.', found " + lex.literal,0);
                }
                else if (id.equals("fof")) {
                    Formula f = Formula.parse(lex);
                    //System.out.println("INFO in Formula.file2clauses(): fof: " + f);
                    if (f.form != null) 
                        cs.addAll(Clausifier.clausify(f.form));                    
                }
                else if (id.equals("cnf")) {
                    Clause clause = new Clause();
                    clause = clause.parse(lex);
                    //System.out.println("INFO in Formula.file2clauses(): cnf: " + clause);
                    cs.add(clause);
                }
                else if (lex.type == Lexer.EOFToken) {
                    System.out.println();
                    return cs;
                }
                else
                    throw new ParseException("Error in Formula.file2clauses: bad id: " + 
                            id + " at line " + lex.input.getLineNumber(),0);
            }
            catch (ParseException p) {
                System.out.println();
                System.out.println(p.getMessage());
                p.printStackTrace();
                return cs;
            }
            catch (IOException p) {
                System.out.println();
                System.out.println(p.getMessage());
                p.printStackTrace();
                return cs;
            }
        }
        System.out.println();
        return cs;
    }
    
    /** ***************************************************************
     */
    public static ClauseSet file2clauses(Lexer lex) {
        return file2clauses(lex,10000);
    }
          
    /** ***************************************************************
     */
    private static ClauseSet file2clauses(String filename, int timeout) {
        
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
     */
    private static ClauseSet file2clauses(String filename) {
        return file2clauses(filename,10000);
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
