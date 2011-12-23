/*
A simple lexical analyser that converts a string into a sequence of
tokens.  Java's StreamTokenizer can't be used since it only can
"push back" one token.
     
This will convert a string into a sequence of
tokens that can be inspected and processed in-order. It is a bit
of an overkill for the simple application, but makes actual
parsing later much easier and more robust than a quicker hack.
        
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
import java.util.regex.*;
import java.text.*;

public class Lexer {

    public int ttype = 0;
    public String sval = "";
       
    public static final String NoToken        = "No Token";
    public static final String WhiteSpace     = "White Space";
    public static final String Newline        = "Newline";
    public static final String HashComment    = "HashComment";
    public static final String PerComment     = "PerComment";
    public static final String IdentUpper     = "Identifier starting with capital letter";
    public static final String IdentLower     = "Identifier starting with lower case letter";
    public static final String DefFunctor     = "Defined symbol (starting with a $)";
    public static final String Integer        = "Positive or negative Integer";
    public static final String QuotedString   = "Quoted string";   
    public static final String FullStop       = ". (full stop)";
    public static final String OpenPar        = "(";
    public static final String ClosePar       = ")";
    public static final String OpenSquare     = "[";
    public static final String CloseSquare    = "]";
    public static final String Comma          = ",";
    public static final String Colon          = ":";
    public static final String EqualSign      = "=";
    public static final String NotEqualSign   = "!=";
    public static final String Nand           = "~&";
    public static final String Nor            = "~|";
    public static final String Or             = "|";
    public static final String And            = "&";
    public static final String Implies        = "=>";   
    public static final String BImplies       = "<=";    
    public static final String Equiv          = "<=>";
    public static final String Xor            = "<~>";
    public static final String Universal      = "!";
    public static final String Existential    = "?";
    public static final String Negation       = "~";
    public static final String EOFToken       = "*EOF*";

    public String filename = "";
    public String type = "";
    public String literal = "";
    public String line = null;
    public int pos = 0;  // character position on the current line
    public LineNumberReader input = null;
    public ArrayDeque<String> tokenStack = new ArrayDeque<String>();

    /** This array contains all of the compiled Pattern objects that
     * will be used by methods in this file. */
    private static LinkedHashMap<String,Pattern> tokenDefs = new LinkedHashMap<String,Pattern>();
    
    public static ArrayList<String> andOr = new ArrayList<String>();
    public static ArrayList<String> binaryRel = new ArrayList<String>();
    public static ArrayList<String> quant = new ArrayList<String>(); 

    /** ***************************************************************
     */
    public Lexer() {
        init();
    }
    
    /** ***************************************************************
     */
    public Lexer(String s) {
        
        init();
        //source = s;
        input = new LineNumberReader(new StringReader(s));
    }
  
    /** ***************************************************************
     */
    public Lexer(File f) {
        
        init();
        //source = file2string(f);
        try {
            input = new LineNumberReader(new FileReader(f));
        }
        catch (FileNotFoundException fnf) {
            System.out.println("Error in Lexer(): File not found: " + f);
            System.out.println(fnf.getMessage());
            fnf.printStackTrace();
        }
    }
    
    /** ***************************************************************
     */
    public String file2string(File f) {

        String result = null;
        DataInputStream in = null;

        try {
            byte[] buffer = new byte[(int) f.length()];
            in = new DataInputStream(new FileInputStream(f));
            in.readFully(buffer);
            result = new String(buffer);
        } 
        catch (IOException e) {
            throw new RuntimeException("IO problem in fileToString", e);
        } 
        finally {
            try {
                in.close();
            } 
            catch (IOException e) { /* ignore it */
            }
        }
        return result;
    }
    
    /** ***************************************************************
     * Return the line number of the token by counting all the
     * newlines in the position up to the current token.
     */
    private int linepos() {

        return input.getLineNumber();
        //return source.substring(0,pos).split(" ").length + 1;
    }        

    /** ***************************************************************
     */
    private static void init() {
        
        tokenDefs.put(FullStop,    Pattern.compile("\\."));                   
        tokenDefs.put(OpenPar,     Pattern.compile("\\("));                   
        tokenDefs.put(ClosePar,    Pattern.compile("\\)"));                   
        tokenDefs.put(OpenSquare,  Pattern.compile("\\["));                   
        tokenDefs.put(CloseSquare, Pattern.compile("\\]"));                   
        tokenDefs.put(Comma,       Pattern.compile(","));                 
        tokenDefs.put(Colon,       Pattern.compile(":"));                  
        tokenDefs.put(Nor,         Pattern.compile("~\\|"));               
        tokenDefs.put(Nand,        Pattern.compile("~&"));                   
        tokenDefs.put(Or,          Pattern.compile("\\|"));                   
        tokenDefs.put(And,         Pattern.compile("&"));                   
        tokenDefs.put(Implies,     Pattern.compile("=>"));              
        tokenDefs.put(Equiv,       Pattern.compile("<=>"));                  
        tokenDefs.put(BImplies,    Pattern.compile("<="));                  
        tokenDefs.put(Xor,         Pattern.compile("<~>"));                     
        tokenDefs.put(EqualSign,   Pattern.compile("="));                 
        tokenDefs.put(NotEqualSign, Pattern.compile("!="));                  
        tokenDefs.put(Negation,    Pattern.compile("~"));                   
        tokenDefs.put(Universal,   Pattern.compile("!"));
        tokenDefs.put(Existential, Pattern.compile("\\?"));
        tokenDefs.put(Newline,     Pattern.compile("\\n"));
        tokenDefs.put(WhiteSpace,  Pattern.compile("\\s+"));
        tokenDefs.put(IdentLower,  Pattern.compile("[a-z][_a-z0-9_A-Z]*"));
        tokenDefs.put(IdentUpper,  Pattern.compile("[_A-Z][_a-z0-9_A-Z]*"));
        tokenDefs.put(DefFunctor,  Pattern.compile("\\$[_a-z0-9_A-Z]*"));
        tokenDefs.put(HashComment, Pattern.compile("#[^\\n]*"));
        tokenDefs.put(PerComment,  Pattern.compile("%[^\\n]*"));
        tokenDefs.put(QuotedString,Pattern.compile("'[^']*'"));
        
        andOr.add(And);
        andOr.add(Or);
        
        binaryRel.add(Nor);
        binaryRel.add(Xor); 
        binaryRel.add(Nand); 
        binaryRel.add(Equiv); 
        binaryRel.add(Implies); 
        binaryRel.add(BImplies); 
        
        quant.add(Universal);
        quant.add(Existential);
    }
    
    /** ***************************************************************
     * Return the next token without consuming it.
     */
    public String look() throws ParseException {

        String res = next();
        //System.out.println("INFO in Lexer.look(): " + res);
        tokenStack.push(res);
        return res;
    }

    /** ***************************************************************
     * Return the literal value of the next token, i.e. the string
     * generating the token.
     */
    public String lookLit() throws ParseException {

        look();
        return literal;
    }
            
    /** ***************************************************************
     * Take a list of expected token types. Return True if the
     * next token is expected, False otherwise.
     */
    public boolean testTok(ArrayList<String> tokens) throws ParseException {

        look();
        for (int i = 0; i < tokens.size(); i++) {
            if (type.equals(tokens.get(i))) {
                //System.out.println("INFO in Lexer.testTok(): found token");
                return true;
            }
        }
        //System.out.println("INFO in Lexer.testTok(): didn't find tokens with type: " + type + " for list " + tokens);
        return false;
    }

    /** ***************************************************************
     * Convenience method
     */
    public boolean testTok(String tok) throws ParseException {

        ArrayList<String> tokens = new ArrayList<String>();
        tokens.add(tok);
        return testTok(tokens);
    }

    /** ***************************************************************
     * Take a list of expected token types. If the next token is
     * not among the expected ones, exit with an error. Otherwise do
     * nothing. 
     */
    public void checkTok(String tok) throws ParseException {

        ArrayList<String> tokens = new ArrayList<String>();
        tokens.add(tok);
        checkTok(tokens);
    }

    /** ***************************************************************
     * Take a list of expected token types. If the next token is
     * not among the expected ones, exit with an error. Otherwise do
     * nothing. 
     */
    public void checkTok(ArrayList<String> tokens) throws ParseException {

        look();
        for (int i = 0; i < tokens.size(); i++) {
            if (type.equals(tokens.get(i)))
                return;
        }
        throw new ParseException("Error in Lexer.checkTok(): Unexpected token '" + type + "'",linepos());
    }

    /** ***************************************************************
     */
    public String acceptTok(String token) throws ParseException {

        ArrayList<String> tokens = new ArrayList<String>();
        tokens.add(token);
        checkTok(tokens);
        return next();
    }

    /** ***************************************************************
     * Take a list of expected token types. If the next token is
     * among the expected ones, consume and return it. Otherwise, exit 
     * with an error. 
     */
    public String acceptTok(ArrayList<String> tokens) throws ParseException {

        checkTok(tokens);
        return next();
    }

    /** ***************************************************************
     */
    public boolean testLit(String litval) throws ParseException {

        ArrayList<String> litvals = new ArrayList<String>();
        litvals.add(litval);
        return testLit(litvals);
    }
    
    /** ***************************************************************
     * Take a list of expected literal strings. Return True if the
     * next token's string value is among them, False otherwise. 
     */
    public boolean testLit(ArrayList<String> litvals) throws ParseException {

        lookLit();
        for (int i = 0; i < litvals.size(); i++) {
            if (literal.equals(litvals.get(i)))
                return true;
        }
        return false;
    }
    
    /** ***************************************************************
     */
    private void checkLit(String litval) throws ParseException {

        ArrayList<String> litvals = new ArrayList<String>();
        litvals.add(litval);
        checkLit(litvals);
    }

    /** ***************************************************************
     * Take a list of expected literal strings. If the next token's
     * literal is not among the expected ones, exit with an
     * error. Otherwise do nothing. 
     */
    private void checkLit(ArrayList<String> litvals) throws ParseException {

        if (!testLit(litvals)) {
            look();
            throw new ParseException("Error in Lexer.checkLit(): " + literal + " not in " + litvals, linepos());
        }
    }

    /** ***************************************************************
     * Take a list of expected literal strings. If the next token's
     * literal is among the expected ones, consume and return the
     * literal. Otherwise, exit with an error. 
     */
    public String acceptLit(ArrayList<String> litvals) throws ParseException {

        checkLit(litvals);
        return next();
    }
    
    /** ***************************************************************
     * Take a list of expected literal strings. If the next token's
     * literal is among the expected ones, consume and return the
     * literal. Otherwise, exit with an error. 
     */
    public String acceptLit(String litval) throws ParseException {

        ArrayList<String> litvals = new ArrayList<String>();
        litvals.add(litval);
        checkLit(litvals);
        return next();
    }
    
    /** ***************************************************************
     * Return next semantically relevant token. 
     */
    public String next() throws ParseException {

        String res = nextUnfiltered();
        while ((type.equals(WhiteSpace) || type.equals(HashComment) || 
                type.equals(PerComment)) && !res.equals(EOFToken)) {
            res = nextUnfiltered();
        }
        //System.out.println("INFO in next(): returning token: " + res);
        return res;
    }
    
    /** ***************************************************************
     * Return next token, including tokens ignored by most languages. 
     */
    private String nextUnfiltered() throws ParseException {

        if (tokenStack.size() > 0)
            return tokenStack.pop();
        else {
            if (line == null || line.length() <= pos) {
                try {
                    do {
                        line = input.readLine();
                    } while (line != null && line.length() == 0);    
                    //System.out.println("INFO in Lexer.nextUnfiltered(): " + line);
                    pos = 0;
                }
                catch (IOException ioe) {
                    System.out.println("Error in Lexer.nextUnfiltered()");
                    System.out.println(ioe.getMessage());
                    ioe.printStackTrace();
                    return EOFToken;
                }
                if (line == null) {
                    //System.out.println("INFO in Lexer.nextUnfiltered(): returning eof");
                    type = EOFToken;
                    return EOFToken;
                }
            }
            Iterator<String> it = tokenDefs.keySet().iterator();
            while (it.hasNext()) {  // Go through all the token definitions and process the first one that matches
                String key = it.next();
                Pattern value = tokenDefs.get(key);
                Matcher m = value.matcher(line.substring(pos));
                //System.out.println("INFO in Lexer.nextUnfiltered(): checking: " + key + " against: " + source.substring(pos));
                if (m.lookingAt()) {
                    //System.out.println("INFO in Lexer.nextUnfiltered(): got token against source: " + source.substring(pos));
                    literal = line.substring(pos + m.start(),pos + m.end());
                    pos = pos + m.end();
                    type = key;
                    //System.out.println("INFO in Lexer.nextUnfiltered(): got token: " + literal + " type: " + type + 
                    //        " at pos: " + pos + " with regex: " + value);
                    return m.group();
                }
            }
            if (pos + 4 > line.length())
                if (pos - 4 < 0)
                    throw new ParseException("Error in Lexer.nextUnfiltered(): no matches in token list for " + 
                            line.substring(0,line.length()) + "... at line " + input.getLineNumber(),pos);
                else
                    throw new ParseException("Error in Lexer.nextUnfiltered(): no matches in token list for " + 
                            line.substring(pos - 4,line.length()) + "... at line " + input.getLineNumber(),pos);
            else
                throw new ParseException("Error in Lexer.nextUnfiltered(): no matches in token list for " + 
                        line.substring(pos,pos+4) + "... at line " + input.getLineNumber(),pos);
        }
    }

    /** ***************************************************************
     * Return a list of all tokens in the source. 
     */
    private ArrayList<String> lex() throws ParseException {

        ArrayList<String> res = new ArrayList<String>();
        while (!testTok(EOFToken)) {
            String tok = next();
            //System.out.println("INFO in Lexer.lex(): " + tok);
            res.add(tok);
        }
        return res;
    }

    /** ***************************************************************
     ** ***************************************************************
     */
    private static String example1 = "f(X,g(a,b))";
    private static String example2 = "# Comment\nf(X,g(a,b))";
    private static String example3 = "cnf(test,axiom,p(a)|p(f(X))).";
    private static String example4 = "^";
    private static String example5 = "fof(test,axiom,![X,Y]:?[Z]:~p(X,Y,Z)).";
    
    /** ***************************************************************
     * Test that comments and whitespace are normally ignored. 
     */
    private static void testLex() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testLex()");
        Lexer lex1 = new Lexer(example1);
        Lexer lex2 = new Lexer(example2);
        try {
            ArrayList<String> res1 = lex1.lex();
            System.out.println("INFO in Lexer.testLex(): completed parsing example 1: " + example1);
            ArrayList<String> res2 = lex2.lex();
            System.out.println("INFO in Lexer.testLex(): should be true: " + res1.equals(res2));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     * Test that self.example 1 is split into the expected tokens. 
     */
    private static void testTerm() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testTerm()");
        Lexer lex1 = new Lexer(example1);
        try {
            lex1.acceptTok(IdentLower); // f
            lex1.acceptTok(OpenPar);    // (
            lex1.acceptTok(IdentUpper); // X
            lex1.acceptTok(Comma);      // ,
            lex1.acceptTok(IdentLower); // g
            lex1.acceptTok(OpenPar);    // (
            lex1.acceptTok(IdentLower); // a
            lex1.acceptTok(Comma);      // ,
            lex1.acceptTok(IdentLower); // b
            lex1.acceptTok(ClosePar);   // )
            lex1.acceptTok(ClosePar);   // )
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    private static boolean compareArrays(ArrayList<String> s1, ArrayList<String> s2) {
        
        if (s1.size() != s2.size())
            return false;
        for (int i = 0; i < s1.size(); i++) 
            if (!s1.get(i).equals(s2.get(i)))
                return false;
        return true;
    }
    
    /** ***************************************************************
     * Perform lexical analysis of a clause, then rebuild it and
     * compare that the strings are the same. 
     */
    private static void testClause() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testClause()");
        Lexer lex = new Lexer(example3);
        try {
            ArrayList<String> toks = lex.lex();
            System.out.println(toks);
            System.out.println("Should be true: (tokens == 20): " + (toks.size() == 20) + " actual: " + toks);
            StringBuffer rebuild = new StringBuffer();
            for (int i = 0; i < toks.size(); i++)
                rebuild.append(toks.get(i));
            System.out.println(rebuild.toString() + " " + example3);
            System.out.println("Should be true: " + rebuild.toString().equals(example3));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     * Perform lexical analysis of a formula, then rebuild it and
     * compare that the strings are the same. 
     */
    private static void testFormula() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testFormula()");
        Lexer lex = new Lexer(example5);
        try {
            ArrayList<String> toks = lex.lex();
            System.out.println(toks);
            System.out.println("Should be true: (tokens == 29): " + (toks.size() == 29));
            StringBuffer rebuild = new StringBuffer();
            for (int i = 0; i < toks.size(); i++)
                rebuild.append(toks.get(i));
            System.out.println(rebuild.toString());
            System.out.println("Should be true: " + rebuild.toString().equals(example5));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     * Check the positive case of AcceptLit(). 
     */
    private static void testAcceptLit() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testAcceptLit()");
        Lexer lex = new Lexer(example3);
        try {
            lex.acceptLit("cnf");
            lex.acceptLit("(");
            lex.acceptLit("test");
            lex.acceptLit(",");
            lex.acceptLit("axiom");
            lex.acceptLit(",");
            lex.acceptLit("p");
            lex.acceptLit("(");
            lex.acceptLit("a");
            lex.acceptLit(")");
            lex.acceptLit("|");
            lex.acceptLit("p");
            lex.acceptLit("(");
            lex.acceptLit("f");
            lex.acceptLit("(");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     * Provoke different errors. 
     */
    private static void testErrors() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testErrors(): Should throw three errors");
        Lexer lex = null;
        try {
            lex = new Lexer(example4);
            lex.look(); 
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        try {
            lex = new Lexer(example1);
            lex.checkTok(EqualSign); 
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        try {
            lex = new Lexer(example1);
            lex.checkLit("abc");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {
        
        testLex();
        testTerm();
        testClause();
        testFormula();
        testAcceptLit();
        testErrors();
    }
}
