package edu.uob;

import java.util.*;

public interface TableStructure{
    boolean addRow(String[] rowValues, boolean idProvided);
    
    boolean addColumn(String colName);
    
    String[] getRow(int rowNum);

    List<String> getColumn(String colName);
    
    boolean removeRow(int rowNum);

    boolean removeColumn(String colName);
    
    int getNumEntries();

    int getNumAttributes();

    boolean checkAttributeExists(String colName);

    TableStructure selectRows(boolean[] selectionList);
    
    boolean updateTable(boolean[] selectionList, String[][] nameValPair);
    
    String printTable();

    ConditionResult evaluateCondition(String colName, String comparator, String condValue, int rowNum);
    
    String getTableName();

    List<Integer> getIDValues();
}
