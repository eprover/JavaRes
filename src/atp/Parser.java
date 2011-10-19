/*
A simple implementation of first-order terms. 
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
import java.text.ParseException;
import java.util.*;

public class Parser {
    
    String source = "";
    int pos = -1;
    String name = "";
    int startLine = 0;    
        
    /** ***************************************************************
     * formula - literal or compound formula
     * compound formula - logical operator formula [formula]*
     *   (but conforming to arity of logical operators)
     */
    public class Formula {
        
        String logop = ""; 
        ArrayList<String> varlist = new ArrayList<String>();
        ArrayList<Formula> formula = new ArrayList<Formula>();    
        Term lit = new Term();
        
        public String toString() {
            StringBuffer result = new StringBuffer();
            result.append(logop);
            if (logop.equals("!") || logop.equals("?")) {
                result.append('[');
                for (int i = 0; i < varlist.size(); i++) {
                    result.append(varlist.get(i).toString());
                    if (i < varlist.size()-1)
                        result.append(", ");
                }
                result.append(']');
            }
            return result.toString();
        }
        
        /** ***************************************************************
         * This routine sets up the StreamTokenizer_s so that it parses TPTP.
         */
        public void setupStreamTokenizer(StreamTokenizer_s st) {

            st.whitespaceChars(0,32);
            st.ordinaryChars(33,44);   // !"#$%&'()*+,
            st.wordChars(45,46);       // -
            st.ordinaryChars(46,47);   // ./
            st.wordChars(48,58);       // 0-9:
            st.ordinaryChar(59);       // ;
            st.ordinaryChars(60,62);   // <=>
            st.ordinaryChars(63,64);   // ?@
            st.wordChars(65,90);       // A-Z
            st.ordinaryChars(91,94);   // [\]^
            st.wordChars(95,95);       // _
            st.ordinaryChar(96);       // `
            st.wordChars(97,122);      // a-z
            st.ordinaryChars(123,255); // {|}~
            // st.parseNumbers();
            st.quoteChar('"');
            st.commentChar('#');
            st.eolIsSignificant(true);
        }
        
        /** ***************************************************************
         */
        protected void parse(StreamTokenizer_s st) {
            
            int lastVal = 0;
        
            try {
                String errStr = "";
                setupStreamTokenizer(st);
                Formula f = new Formula();
                Formula currentFormula = f;
                Term l = new Term();
                boolean inLiteral = false;            
                do {
                    lastVal = st.ttype;
                    st.nextToken();
                   
                    switch (st.ttype) {
                        case StreamTokenizer.TT_WORD :   
                            if (st.sval.equals("=>"))
                                currentFormula.logop = "=>";
                            else if (st.sval.equals("<=>"))
                                currentFormula.logop = "<=>";
                            else {
                                if (StringUtil.emptyString(l.t))
                                    l.t = st.sval;
                                else {
                                    inLiteral = true;
                                    Term t = new Term();
                                    lit.t = st.sval;
                                }
                            }
                                
                            break;
                        case StreamTokenizer.TT_EOL :  
                            startLine++;
                            break;
                        case '.':       // end of statement
                            forms.add(f);
                            f = new Formula();
                            break;
                        case '(':
                            break;
                        case ')':
                            break;
                        case '!':       // for all
                            st.nextToken();
                            if (st.ttype != '[') {
                                errStr = "Unexpected character '" + st.ttype + "'";
                                throw new ParseException(errStr, startLine);
                            }
                            break;
                        case '?':       // exists
                            break;
                        case '&':       // and
                            currentFormula.logop = "&";
                            break;
                        case '|':       // or
                            currentFormula.logop = "|";
                            break;
                        case '-':       // not
                            if (inLiteral)
                                l.negated = true;
                            break;

                    }
                } while (st.ttype != StreamTokenizer.TT_EOF);
            }
            catch (Exception ex) {
                System.out.println("Error in Parser.parse(): " + ex.getMessage());
                System.out.println("Error in Parser.parse(): token:" + st.ttype);
                if (st.ttype == StreamTokenizer.TT_WORD)
                    System.out.println("Error in Parser.parse(): token:" + st.ttype);            
                ex.printStackTrace();
            }
        }
    }
    

        
}
