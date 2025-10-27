package edu.uob;

import java.util.*;

public class Node{
    //nodes for parent and children
    Node parentNode;
    List<Node> childNodes;

    //whether node is leaf in tree
    boolean isLeaf;

    //linked token
    Token leafToken;

    //rule associated with node
    Rule nodeRule;

    public Node(Node parentNode, boolean isLeaf, Rule nodeRule, Token leafToken){
        //assign parent and child nodes
        this.parentNode = parentNode;
        childNodes = new ArrayList<Node>();

        //assign other variables
        this.isLeaf = isLeaf;
        this.nodeRule = nodeRule;
        this.leafToken = leafToken;

        if(parentNode != null){
            //link parent node to child
            parentNode.childNodes.add(this);
        }
    }

    //finds number of leaf nodes in tree
    public static int getNumberOfLeaves(Node rootNode){
        int nodeCount = 0;

        //return 0 in event of null node
        if(rootNode == null){
            return nodeCount;
        }

        if(rootNode.isLeaf){
            //a single leaf node counts as one
            return 1;
        }else{
            for(Node childNode : rootNode.childNodes){
                //add the sums recursively
                nodeCount += getNumberOfLeaves(childNode);
            }
        }
        return nodeCount;
    }

    //method to add child node to this node
    public void addChild(Node childNode){
        childNodes.add(childNode);
        childNode.parentNode = this;
    }

    public boolean checkAttributesExist(TableStructure table){
        //if you are an attribute node - check if you exist
        if(nodeRule.ruleName == RuleName.ATTRIBUTE){
            return table.checkAttributeExists(leafToken.tokenText);
        }
        //if it's another terminal node return true
        else if(nodeRule.ruleType == RuleType.TERM){
            return true;
        }
        //if it's non-terminal, check all child nodes
        else{
            for(Node childNode : childNodes){
                if(!childNode.checkAttributesExist(table))
                    return false;
            }
        }
        //return true for non-terminal node
        return true;
    }
    
    public ConditionResult checkConditionsTrue(TableStructure table, int rowNum){
        //if it's a terminal node return "no condition"
        if(nodeRule.ruleType == RuleType.TERM){
            return ConditionResult.NO_CONDITION;
        }
        
        //if you are a simple condition, evaluate and return
        else if(nodeRule.ruleName == RuleName.SIMPLE_COND){
            String attribute = childNodes.get(0).leafToken.tokenText;
            String comparator = childNodes.get(1).leafToken.tokenText;
            String value = childNodes.get(2).childNodes.get(0).leafToken.tokenText;
            return table.evaluateCondition(attribute, comparator, value, rowNum);
        }
        
        
        //if you are a compound condition
        else if(nodeRule.ruleName == RuleName.COMP_WITH_SIMP
                || nodeRule.ruleName == RuleName.COMP_WITH_BRACKET){
            //get the results of the child conditions
            ConditionResult firstCond = childNodes.get(0).checkConditionsTrue(table, rowNum);
            String comparator = childNodes.get(1).leafToken.tokenText;
            ConditionResult secondCond = childNodes.get(2).checkConditionsTrue(table, rowNum);
            
            //invalid comparison from either sub conditions is returned
            if(firstCond == ConditionResult.INVALID || secondCond == ConditionResult.INVALID)
                return ConditionResult.INVALID;
            
            //finally evaluate the compound condition
            if(comparator.equalsIgnoreCase("AND")){
                if(firstCond == ConditionResult.TRUE && secondCond == ConditionResult.TRUE)
                    return ConditionResult.TRUE;
            }else if(comparator.equalsIgnoreCase("OR")){
                if(firstCond == ConditionResult.TRUE || secondCond == ConditionResult.TRUE)
                    return ConditionResult.TRUE;
            }
            
            return ConditionResult.FALSE;
        }
        
        //if it's non-terminal, check all child nodes
        //return the highest priority condition result
        else{
            ConditionResult topResult = ConditionResult.NO_CONDITION;
            ConditionResult childResult;
            for(Node childNode : childNodes){
                childResult = childNode.checkConditionsTrue(table, rowNum);
                if(childResult.getVal() > topResult.getVal())
                    topResult = childResult;
            }
            
            return topResult;
        }
    }
    
    //finds the first node within tree of given rule types
    public Node findByRuleName(RuleName findName){
        if(nodeRule.ruleName == findName)
            return this;
        else{
            Node childResult;
            for(Node childNode : childNodes){
                childResult = childNode.findByRuleName(findName);
                if(childResult != null)
                    return childResult;
            }
        }
        return null;
    }
    
    //finds text from first matched leaf token of given rule
    public String findTokenString(RuleName findName){
        Node findNode = findByRuleName(findName);
        
        if(findNode != null){
            if(findNode.isLeaf)
                return findNode.leafToken.tokenText;
        }
        return null;
    }
    
    //searches through tree and returns list of all node values
    //which match the given rule type e.g. attribute names
    public List<String> getAttributesOrVals(RuleName ruleToSearch){
        List<String> attributesFound = new ArrayList<String>();
        
        //if you are an attribute then return the value
        if(nodeRule.ruleName == ruleToSearch){
            if(ruleToSearch == RuleName.ATTRIBUTE 
                    || ruleToSearch == RuleName.TABLE_NAME)
                attributesFound.add(leafToken.tokenText);
            else if(ruleToSearch == RuleName.VALUE)
                attributesFound.add(childNodes.get(0).leafToken.tokenText);
            return attributesFound;
        }
        
        //otherwise if you are terminal return nothing
        else if(nodeRule.ruleType == RuleType.TERM){
            return attributesFound;
        }
        
        //otherwise get attributes of your children
        else{
            for(Node childNode : childNodes)
                attributesFound.addAll(childNode.getAttributesOrVals(ruleToSearch));
            return attributesFound;
        }
    }

    //returns false if any table, database or column names use reserved words
    public boolean checkNoReservedWordsUsed(){
        String tableName = findTokenString(RuleName.TABLE_NAME);
        String databaseName = findTokenString(RuleName.DB_NAME);

        //check table name (if provided)
        if(tableName != null)
            if(SQLSimpGrammar.reservedWords.contains(tableName.toUpperCase()))
                return false;

        //check database name (if provided)
        if(databaseName != null)
            if(SQLSimpGrammar.reservedWords.contains(databaseName.toUpperCase()))
                return false;

        List<String> attributes = getAttributesOrVals(RuleName.ATTRIBUTE);

        //check all attributes
        for(String attribute : attributes)
            if(SQLSimpGrammar.reservedWords.contains(attribute.toUpperCase()))
                return false;

        return true;
    }
}
