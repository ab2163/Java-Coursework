package edu.uob;

import java.util.*;
import java.util.regex.*;

public class Tokeniser{
    public static List<Token> extractTokens(String commandStr){
        List<Token> tokens = new ArrayList<Token>();
        List<String> tokenStrings;

        //start off by splitting by spaces
        tokenStrings = splitBySpaces(commandStr);
        
        //now split the sub strings by non-plaintext characters
        List<String> splitSubStr;
        for(int strCnt = 0; strCnt < tokenStrings.size(); strCnt++){
            String subStr = tokenStrings.get(strCnt);
            splitSubStr = new ArrayList<String>();
            String plaintextStr = "";
            
            //go through all characters in sub-string
            for(char currChar : subStr.toCharArray()){
                
                if(Character.isLetterOrDigit(currChar)){
                    //group plaintext characters
                    plaintextStr += currChar;
                }else{
                    if(plaintextStr.length() > 0){
                        splitSubStr.add(plaintextStr);
                        plaintextStr = "";
                    }
                    //add non-plaintext characters as separate tokens
                    splitSubStr.add(Character.toString(currChar));
                }
            }
            
            //add any remaining plaintext from the sub-string
            if(plaintextStr.length() > 0)
                splitSubStr.add(plaintextStr);
            
            //a length greater than one means the string has been split
            if(splitSubStr.size() > 1){
                //replace the string at strCnt with the decomposed list
                tokenStrings.remove(strCnt);
                tokenStrings.addAll(strCnt, splitSubStr);
                //increase strCnt to offset changing size of list array
                strCnt += splitSubStr.size() - 1;
            }
        }
        
        //combine symbolic tokens as appropriate
        tokenStrings = combineSymbolTokens(tokenStrings);
        
        //convert token strings to tokens
        for(String subStr : tokenStrings) {
            tokens.add(new Token(subStr));
        }
        
        return tokens;
    }
    
    //after splitting apart token string, recombines select strings
    //e.g. "<" + "=" becomes "<="
    public static List<String> combineSymbolTokens(List<String> tokenStrings) {
        List<String> combinedStrings = new ArrayList<String>();
        
        Pattern compFirstChars = Pattern.compile("=|<|>|!");
        
        for(int strCnt = 0; strCnt < tokenStrings.size(); strCnt++){
            String currStr = tokenStrings.get(strCnt);
            int strAfter = tokenStrings.size() - strCnt - 1;
            String nextStr = (strAfter >= 1 ? tokenStrings.get(strCnt + 1) : null);
            String twoStrAfter = (strAfter >= 2 ? tokenStrings.get(strCnt + 2) : null);
            String threeStrAft = (strAfter >= 3 ? tokenStrings.get(strCnt + 3) : null);
            Matcher compChMatcher = compFirstChars.matcher(currStr);
            boolean strCombined = false;

            //deal with string literals
            //e.g. 'xyz\tuv', 'xyz!!)(', 'xyz ! abc'
            if(currStr.equals("'") && strAfter >= 1){
                String strLit = currStr;
                strCnt++;
                do{
                    currStr = tokenStrings.get(strCnt); //get next token string
                    strLit += currStr; //keep combining until closing quote
                    strCnt++;
                }
                while(strCnt < tokenStrings.size() && !currStr.equals("'"));

                //prevent overcounting of token
                strCnt--;

                combinedStrings.add(strLit);
                strCombined = true;
            }

            //combine to give "==", "<=", ">=", "!="
            if(compChMatcher.matches() && strAfter >= 1 && !strCombined){
                if(nextStr.equals("=")){
                    combinedStrings.add(currStr + nextStr);
                    strCnt++;
                    strCombined = true;
                }
            }

            //combine "+", "3", ".", "14" to "3.14"
            if((currStr.equals("+") || currStr.equals("-")) && strAfter >= 3 && !strCombined){
                if(isNumeric(nextStr) && twoStrAfter.equals(".") && isNumeric(threeStrAft)){
                    //do not add the "+" - there is no need
                    String decimalStr = "";
                    if(currStr.equals("-"))
                        decimalStr = "-";
                    decimalStr += (nextStr + twoStrAfter + threeStrAft);

                    combinedStrings.add(decimalStr);
                    strCnt += 3;
                    strCombined = true;
                }
            }

            //combine "-", "12" to "-12"
            if((currStr.equals("+") || currStr.equals("-")) && strAfter >= 1 && !strCombined){
                if(isNumeric(nextStr)){
                    //do not add the "+" - there is no need
                    String integerStr = "";
                    if(currStr.equals("-"))
                        integerStr = "-";
                    integerStr += nextStr;

                    combinedStrings.add(integerStr);
                    strCnt += 1;
                    strCombined = true;
                }
            }

            //combine "3", ".", "14" to "3.14"
            if(isNumeric(currStr) && strAfter >= 2 && !strCombined){
                if(nextStr.equals(".") && isNumeric(twoStrAfter)){
                    combinedStrings.add(currStr + nextStr + twoStrAfter);
                    strCnt += 2;
                    strCombined = true;
                }
            }
            
            //if none of the above happened then add string without combining
            if(!strCombined)
                combinedStrings.add(currStr);
        }
        return combinedStrings;
    }

    //splits a string by spaces but preserves spaces within string literals
    //e.g. "this string" is split into "this", "string"
    //e.g. "'this string'" is not split
    public static List<String> splitBySpaces(String commandStr){
        List<String> tokenStrings = new ArrayList<String>();

        //whether or not currently within string literal
        boolean withinStrLiteral = false;

        String currStr = "";
        for(char currChar : commandStr.toCharArray()){
            if(currChar == ' ' && !withinStrLiteral){
                tokenStrings.add(currStr);
                currStr = "";
            }
            else currStr += currChar;

            if(currChar == '\''){
                withinStrLiteral = !withinStrLiteral;
            }
        }

        //add the last sub-string from the loop
        tokenStrings.add(currStr);

        //finally - remove all empty strings from the list
        tokenStrings.removeAll(List.of(""));

        return tokenStrings;
    }

    public static boolean isNumeric(String strIn){
        for(char chOfStr : strIn.toCharArray())
            if(!Character.isDigit(chOfStr))
                return false;
        return true;
    }
}
