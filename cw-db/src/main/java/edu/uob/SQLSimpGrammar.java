package edu.uob;

import java.util.*;

public class SQLSimpGrammar{
    //reserved SQL keywords
    static List<String> reservedWords = List.of("USE", "CREATE", "DATABASE", "TABLE", "DROP",
            "ALTER", "INSERT", "INTO", "VALUES", "SELECT", "FROM", "WHERE", "UPDATE", "SET", "DELETE",
            "JOIN", "ON", "ADD", "DROP", "LIKE", "AND", "OR");

    //create rules for top-level SQL commands
    static Rule command = new Rule(RuleName.COMMAND, RuleType.SEQ);
    static Rule commandType = new Rule(RuleName.COMMAND_TYPE, RuleType.OR);
    static Rule use = new Rule(RuleName.USE, RuleType.SEQ);
    static Rule create = new Rule(RuleName.CREATE, RuleType.OR);
    static Rule createDB = new Rule(RuleName.CREATE_DB, RuleType.SEQ);
    static Rule createTable = new Rule(RuleName.CREATE_TABLE, RuleType.OR);
    static Rule crTableNoAttr = new Rule(RuleName.CR_TABLE_NO_ATTR, RuleType.SEQ);
    static Rule crTableAttr = new Rule(RuleName.CR_TABLE_ATTR, RuleType.SEQ);
    static Rule drop = new Rule(RuleName.DROP, RuleType.OR);
    static Rule dropDB = new Rule(RuleName.DROP_DB, RuleType.SEQ);
    static Rule dropTable = new Rule(RuleName.DROP_TABLE, RuleType.SEQ);
    static Rule alter = new Rule(RuleName.ALTER, RuleType.SEQ);
    static Rule insert = new Rule(RuleName.INSERT, RuleType.SEQ);
    static Rule select = new Rule(RuleName.SELECT, RuleType.OR);
    static Rule selectNoCond = new Rule(RuleName.SELECT_NO_COND, RuleType.SEQ);
    static Rule selectCond = new Rule(RuleName.SELECT_COND, RuleType.SEQ);
    static Rule update = new Rule(RuleName.UPDATE, RuleType.SEQ);
    static Rule delete = new Rule(RuleName.DELETE, RuleType.SEQ);
    static Rule join = new Rule(RuleName.JOIN, RuleType.SEQ);

    //create intermediate SQL rules
    static Rule nameValList = new Rule(RuleName.NAME_VALUE_LIST, RuleType.OR);
    static Rule nameValListRec = new Rule(RuleName.NAME_VALUE_RECUR, RuleType.SEQ);
    static Rule nameValPair = new Rule(RuleName.NAME_VALUE_PAIR, RuleType.SEQ);
    static Rule valList = new Rule(RuleName.VALUE_LIST, RuleType.OR);
    static Rule valListRec = new Rule(RuleName.VALUE_LIST_RECUR, RuleType.SEQ);
    static Rule value = new Rule(RuleName.VALUE, RuleType.OR);
    static Rule wildAttrList = new Rule(RuleName.WILD_ATTR_LIST, RuleType.OR);
    static Rule attrList = new Rule(RuleName.ATTR_LIST, RuleType.OR);
    static Rule attrListRec = new Rule(RuleName.ATTR_LIST_RECUR, RuleType.SEQ);
    static Rule condition = new Rule(RuleName.CONDITION, RuleType.OR);
    static Rule compoundStartBr = new Rule(RuleName.COMP_WITH_BRACKET, RuleType.SEQ);
    static Rule compoundStartSimp = new Rule(RuleName.COMP_WITH_SIMP, RuleType.SEQ);
    static Rule bracketCond = new Rule(RuleName.BRACKET_COND, RuleType.SEQ);
    static Rule simpleCond = new Rule(RuleName.SIMPLE_COND, RuleType.SEQ);
    static Rule attribute = new Rule(RuleName.ATTRIBUTE, "[a-zA-Z0-9]+");
    static Rule databaseName = new Rule(RuleName.DB_NAME, "[a-zA-Z0-9]+");
    static Rule tableName = new Rule(RuleName.TABLE_NAME, "[a-zA-Z0-9]+");

    //create terminal rules for valid data types
    static Rule integerLit = new Rule(RuleName.INTEGER_LITERAL, "[+-]?[0-9]+");
    static Rule floatLit = new Rule(RuleName.FLOAT_LITERAL, "[+-]?[0-9]+\\.[0-9]+");
    static Rule stringLit;
    static Rule nullLiteral = new Rule(RuleName.NULL_LITERAL, "NULL");

    //create terminal rules for valid symbols
    static Rule asteriskLit = new Rule(RuleName.ASTERISK_LITERAL, "\\*");
    static Rule comparator = new Rule(RuleName.COMPARATOR, "==|>|<|>=|<=|!=|LIKE");
    static Rule semicolonLit = new Rule(RuleName.SEMICOLON_LITERAL, ";");
    static Rule commaLit = new Rule(RuleName.COMMA_LITERAL, ",");
    static Rule equalsLit = new Rule(RuleName.EQUALS_LITERAL, "=");
    static Rule opParLit = new Rule(RuleName.OP_PAREN_LITERAL, "\\(");
    static Rule clParLit = new Rule(RuleName.CL_PAREN_LITERAL, "\\)");
    static Rule spaceLit = new Rule(RuleName.SPACE_LITERAL, " ");

    //create terminal rules for SQL keywords
    static Rule alterType = new Rule(RuleName.ALTERATION_TYPE, "ADD|DROP");
    static Rule boolLit = new Rule(RuleName.BOOLEAN_LITERAL, "TRUE|FALSE");
    static Rule boolOperator = new Rule(RuleName.BOOL_OPERATOR, "AND|OR");
    static Rule useLit = new Rule(RuleName.USE_LITERAL, "USE");
    static Rule createLit = new Rule(RuleName.CREATE_LITERAL, "CREATE");
    static Rule DBLit = new Rule(RuleName.DB_LITERAL, "DATABASE");
    static Rule tableLit = new Rule(RuleName.TABLE_LITERAL, "TABLE");
    static Rule dropLit = new Rule(RuleName.DROP_LITERAL, "DROP");
    static Rule alterLit = new Rule(RuleName.ALTER_LITERAL, "ALTER");
    static Rule insertLit = new Rule(RuleName.INSERT_LITERAL, "INSERT");
    static Rule intoLit = new Rule(RuleName.INTO_LITERAL, "INTO");
    static Rule valuesLit = new Rule(RuleName.VALUES_LITERAL, "VALUES");
    static Rule selectLit = new Rule(RuleName.SELECT_LITERAL, "SELECT");
    static Rule fromLit = new Rule(RuleName.FROM_LITERAL, "FROM");
    static Rule whereLit = new Rule(RuleName.WHERE_LITERAL, "WHERE");
    static Rule updateLit = new Rule(RuleName.UPDATE_LITERAL, "UPDATE");
    static Rule setLit = new Rule(RuleName.SET_LITERAL, "SET");
    static Rule deleteLit = new Rule(RuleName.DELETE_LITERAL, "DELETE");
    static Rule joinLit = new Rule(RuleName.JOIN_LITERAL, "JOIN");
    static Rule andLit = new Rule(RuleName.AND_LITERAL, "AND");
    static Rule onLit = new Rule(RuleName.ON_LITERAL, "ON");

    static{
        String stringLitRegEx = String.join("", "'[", "!", "#", "\\$", "%", "&", "\\(",
                "\\)", "\\*", "\\+", ",", "\\-", "\\.", "/", ":", ";", ">", "=", "<", "\\?", "@",
                "\\[", "\\\\", "\\]", "\\^", "_", "`", "\\{", "\\}", "~", "a-zA-z0-9", " ", "]", "*'");
        stringLit = new Rule(RuleName.STRING_LITERAL, stringLitRegEx);

        //set the rule relations as defined by BNF grammar
        command.setSubRules(List.of(commandType, semicolonLit));
        commandType.setSubRules(List.of(use, create, drop, alter, insert, select, update, delete, join));
        use.setSubRules(List.of(useLit, databaseName));
        create.setSubRules(List.of(createDB, createTable));
        createDB.setSubRules(List.of(createLit, DBLit, databaseName));
        createTable.setSubRules(List.of(crTableAttr, crTableNoAttr));
        crTableNoAttr.setSubRules(List.of(createLit, tableLit, tableName));
        crTableAttr.setSubRules(List.of(createLit, tableLit, tableName, opParLit, attrList, clParLit));
        drop.setSubRules(List.of(dropDB, dropTable));
        dropDB.setSubRules(List.of(dropLit, DBLit, databaseName));
        dropTable.setSubRules(List.of(dropLit, tableLit, tableName));
        alter.setSubRules(List.of(alterLit, tableLit, tableName, alterType, attribute));
        insert.setSubRules(List.of(insertLit, intoLit, tableName, valuesLit, opParLit, valList, clParLit));
        select.setSubRules(List.of(selectCond, selectNoCond));
        selectNoCond.setSubRules(List.of(selectLit, wildAttrList, fromLit, tableName));
        selectCond.setSubRules(List.of(selectLit, wildAttrList, fromLit, tableName, whereLit, condition));
        update.setSubRules(List.of(updateLit, tableName, setLit, nameValList, whereLit, condition));
        delete.setSubRules(List.of(deleteLit, fromLit, tableName, whereLit, condition));
        join.setSubRules(List.of(joinLit, tableName, andLit, tableName, onLit, attribute, andLit, attribute));
        nameValList.setSubRules(List.of(nameValListRec, nameValPair));
        nameValListRec.setSubRules(List.of(nameValPair, commaLit, nameValList));
        nameValPair.setSubRules(List.of(attribute, equalsLit, value));
        valList.setSubRules(List.of(valListRec, value));
        valListRec.setSubRules(List.of(value, commaLit, valList));
        value.setSubRules(List.of(stringLit, boolLit, floatLit, integerLit, nullLiteral));
        wildAttrList.setSubRules(List.of(attrList, asteriskLit));
        attrList.setSubRules(List.of(attrListRec, attribute));
        attrListRec.setSubRules(List.of(attribute, commaLit, attrList));
        compoundStartBr.setSubRules(List.of(bracketCond, boolOperator, condition));
        compoundStartSimp.setSubRules(List.of(simpleCond, boolOperator, condition));
        bracketCond.setSubRules(List.of(opParLit, condition, clParLit));
        simpleCond.setSubRules(List.of(attribute, comparator, value));
    }
}