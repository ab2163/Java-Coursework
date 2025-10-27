package edu.uob;

import java.util.*;
import java.util.regex.*;

public class Rule{

    //name of the rule - from an enumeration
    RuleName ruleName;

    //rules which fall "below" this rule in grammar
    List<Rule> subRules;

    //there are three generic rule types
    RuleType ruleType;

    //regex pattern which is checked against for terminal rules
    Pattern terminalCheck;

    //constructor for "or" and sequential type rules
    public Rule(RuleName ruleName, RuleType ruleType){
        this.ruleName = ruleName;
        this.ruleType = ruleType;
    }

    //constructor for terminal type rules
    public Rule(RuleName ruleName, String regExStr){
        this.ruleName = ruleName;
        this.ruleType = RuleType.TERM;
        terminalCheck = Pattern.compile(regExStr, Pattern.CASE_INSENSITIVE);
    }

    //for terminal rules only - check whether token valid
    public boolean checkToken(Token token){
        Matcher matcher = terminalCheck.matcher(token.tokenText);
        return matcher.matches();
    }

    public void setSubRules(List<Rule> subRules){
        this.subRules = subRules;
    }

    //finds out whether a boolean operator which is not nested within brackets exists
    //used in deciding which "condition" sub-rules to pick
    public static boolean findNakedBool(List<Token> tokens, int startingToken){
        //counting opening and closing brackets
        int bracketCnt = 0;

        for(int tokCnt = startingToken; tokCnt < tokens.size(); tokCnt++){
            //count the opening and closing brackets
            if(tokens.get(tokCnt).tokenText.equals("(")){
                bracketCnt++;
            }else if(tokens.get(tokCnt).tokenText.equals(")")){
                bracketCnt--;
            }else if(bracketCnt < 0){
                //negative bracket count means reached end of bracket scope
                return false;
            }

            //find a "naked" boolean operator
            //defined as a boolean operator without enclosing brackets
            if(SQLSimpGrammar.boolOperator.checkToken(tokens.get(tokCnt))){
                if(bracketCnt == 0){
                    return true;
                }
            }
        }

        //otherwise no "naked" boolean operators exist
        return false;
    }
    
    //try and "map out" tokens against this rule
    public Node parseTokens(List<Token> tokens, int startingToken){
        //if you're trying to fit a rule beyond the number of tokens return null
        if(startingToken >= tokens.size()){
            return null;
        }

        Node parentNode = new Node(null, false, this, null);

        //logic for condition sub-rules must be separately handled
        if(ruleName == RuleName.CONDITION){
            Node subNodeTree = parseConditionTokens(tokens, startingToken);

            //for a successful match, create the tree and return
            if(subNodeTree != null){
                parentNode.addChild(subNodeTree);
                return parentNode;
            }
        }

        //deal with "or" rules by checking whether any one rule works
        else if(ruleType == RuleType.OR){
            for(Rule subRule : subRules){
                Node subNodeTree = subRule.parseTokens(tokens, startingToken);

                //for a successful match, create the tree and return
                if(subNodeTree != null){
                    parentNode.addChild(subNodeTree);
                    return parentNode;
                }
            }
        }

        //deal with "terminal" nodes by checking whether the token matches
        else if(ruleType == RuleType.TERM){
            if(checkToken(tokens.get(startingToken))){
                parentNode.isLeaf = true;
                parentNode.leafToken = tokens.get(startingToken);
                return parentNode;
            }
        }

        //deal with "sequential" rules by checking all of them work
        else if(ruleType == RuleType.SEQ){
            for(Rule subRule : subRules){
                Node subNodeTree = subRule.parseTokens(tokens, startingToken);

                //a rule failure means we return null
                if(subNodeTree == null){
                    return null;
                }else {
                    //increase token positional count by tokens parsed
                    startingToken += Node.getNumberOfLeaves(subNodeTree);

                    //add sub-tree to parent node
                    parentNode.addChild(subNodeTree);
                }
            }
            return parentNode;
        }

        //return null if no rules match
        return null;
    }

    public Node parseConditionTokens(List<Token> tokens, int startingToken){
        Node subNodeTree;

        if(tokens.get(startingToken).tokenText.equals("(")){
            if(Rule.findNakedBool(tokens, startingToken)){
                //if starts with bracket and "naked" boolean operators exist
                //then it must be a compound condition starting with bracketed condition
                subNodeTree = SQLSimpGrammar.compoundStartBr.parseTokens(tokens, startingToken);
            }else{
                //if starts with bracket and NO "naked" boolean operators exist
                //then it must be a bracketed condition
                subNodeTree = SQLSimpGrammar.bracketCond.parseTokens(tokens, startingToken);
            }
        }else{
            //there should always be at least 4 tokens left to parse the condition
            //return null if not
            if(startingToken + 3 >= tokens.size())
                return null;
            else if(SQLSimpGrammar.boolOperator.checkToken(tokens.get(startingToken + 3))){
                //if does not start with bracket and 4th token IS boolean operator
                //then it must be a compound condition starting with simple condition
                subNodeTree = SQLSimpGrammar.compoundStartSimp.parseTokens(tokens, startingToken);
            }else{
                //if does not start with bracket and 5th token is not boolean operator
                //then it must be a simple condition
                subNodeTree = SQLSimpGrammar.simpleCond.parseTokens(tokens, startingToken);
            }
        }

        return subNodeTree;
    }
}