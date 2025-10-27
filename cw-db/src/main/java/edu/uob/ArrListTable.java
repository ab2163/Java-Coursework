package edu.uob;

import java.util.*;

public class ArrListTable implements TableStructure{
    String tableName;
    List<List<String>> tableData;
    List<Integer> idValues;

    //keeps track of ID values assigned
    int lastAssignedID;

    public ArrListTable(String tableName){
        this.tableName = tableName;
        tableData = new ArrayList<List<String>>();
        idValues = new ArrayList<Integer>();
        //add "0" to idValues to maintain same length as other cols
        idValues.add(0);
        addColumn("id");
        lastAssignedID = 0;
    }

    public ArrListTable(String tableName, String[] attributeList){
        this(tableName);
        for(String attribute : attributeList){
            addColumn(attribute);
        }
    }

    //returns true if column added successfully
    public boolean addColumn(String colName){
        //should not insert same column name twice
        if(checkAttributeExists(colName))
            return false;

        //create column and add the column title
        List<String> newColumn = new ArrayList<String>();
        newColumn.add(colName);

        //add empty string values as data
        for(int rowCnt = 0; rowCnt < getNumEntries(); rowCnt++){
            newColumn.add("NULL");
        }

        tableData.add(newColumn);
        return true;
    }

    public List<String> getColumn(String colName){
        for(List<String> column : tableData)
            if(column.get(0).equalsIgnoreCase(colName))
                return column;
        return null;
    }

    //returns true if column successfully removed
    public boolean removeColumn(String colName){
        //cannot remove "id" column
        if(colName.equalsIgnoreCase("id"))
            return false;
        
        List<String> column = getColumn(colName);

        //only remove column if it exists
        if(column != null) {
            tableData.remove(column);
            return true;
        }
        else return false;
    }

    public int getNumEntries(){
        //minus one to discount the header row
        return idValues.size() - 1;
    }

    //includes "id" in number of attributes
    public int getNumAttributes(){
        return tableData.size();
    }

    public boolean checkAttributeExists(String colName){
        return getColumn(colName) != null;
    }

    //returns true if row successfully added
    public boolean addRow(String[] rowValues, boolean idProvided){
        //check length of inputs matches
        if(idProvided && getNumAttributes() != rowValues.length)
            return false;
        if(!idProvided && (getNumAttributes() - 1 != rowValues.length) )
            return false;
                
        List<String> idColumn = getColumn("id");
        
        if(idProvided){
            //if id value provided, check it's an integer
            int idValue;
            try{ idValue = Integer.parseInt(rowValues[0]); }
            catch(NumberFormatException notInt){ return false; }
           
            //then copy and add to idValues
            idColumn.add(rowValues[0]);
            idValues.add(idValue);
            //note - no updating of lastAssignedID - must be handled separately
        }else{
            //otherwise must generate id value
            lastAssignedID++;
            idColumn.add(Integer.toString(lastAssignedID));
            idValues.add(lastAssignedID);
        }
        
        //add the values
        //start colCnt from 1 to skip id column
        //offset index by 1 for rowValues if id not provided
        for(int colCnt = 1; colCnt < getNumAttributes(); colCnt++){
            tableData.get(colCnt).add(rowValues[colCnt - (idProvided ? 0 : 1)]);
        }
        
        return true;
    }
    
    public String[] getRow(int rowNum){
        String[] myRow = new String[getNumAttributes()];
        
        for(int colCnt = 0; colCnt < getNumAttributes(); colCnt++){
            myRow[colCnt] = tableData.get(colCnt).get(rowNum);
        }
        
        return myRow;
    }
    
    public static boolean copyRow(ArrListTable fromTable, ArrListTable toTable, int rowNum){
        //if the tables don't have same number of columns return false
        if(fromTable.getNumAttributes() != toTable.getNumAttributes())
            return false;
        
        //copy row over
        for(int colCnt = 0; colCnt < fromTable.getNumAttributes(); colCnt++){
            toTable.tableData.get(colCnt).add(fromTable.tableData.get(colCnt).get(rowNum));
        }
        
        //also copy id value over
        toTable.idValues.add(fromTable.idValues.get(rowNum));
        
        return true;
    }

    //returns table with applied selection
    public TableStructure selectRows(boolean[] selectionList){
        //selectionList should be same length as tableData
        if(getNumEntries() != selectionList.length)
            return null;
        
        //create a new table for selection
    	ArrListTable selectionTable = new ArrListTable("", getRow(0));
    	
    	//copy rows only selected rows over
    	for(int rowCnt = 1; rowCnt <= getNumEntries(); rowCnt++)
    	    if(selectionList[rowCnt - 1])
    	        copyRow(this, selectionTable, rowCnt);
    	
        return selectionTable;
    }

    //returns true if update successful
    public boolean updateTable(boolean[] selectionList, String[][] nameValPair){
        //selectionList should be same length as tableData
        if(getNumEntries() != selectionList.length)
            return false;

        for(int attrCount = 0; attrCount < nameValPair[0].length; attrCount++){
            List<String> myCol = getColumn(nameValPair[0][attrCount]);
            
            for(int rowCnt = 1; rowCnt <= getNumEntries(); rowCnt++)
                if(selectionList[rowCnt - 1])
                    myCol.set(rowCnt, nameValPair[1][attrCount]);
        }
        
        return true;
    }

    public String printTable(){
        String outputString = "";
        for(int rowCnt = 0; rowCnt <= getNumEntries(); rowCnt++){
            for(int colCnt = 0; colCnt < getNumAttributes(); colCnt++){
                //remove quote marks from string literals
                String valToPrint = removeStrLitQuotes(tableData.get(colCnt).get(rowCnt));
                
                //do not display NULL values - instead display empty column
                if(valToPrint.equalsIgnoreCase("NULL"))
                    valToPrint = "";
                
                outputString += valToPrint;
                if(colCnt < getNumAttributes() - 1)
                    outputString += "\t".repeat(getColumnTabs(colCnt, rowCnt));
            }
            outputString += "\n";
        }
        return outputString;
    }

    public static String removeStrLitQuotes(String strIn){
        int strLen = strIn.length();
        if(strLen < 2)
            return strIn;
        else
            if(strIn.charAt(0) == '\'' && strIn.charAt(strLen - 1) == '\'')
                return strIn.substring(1, strLen - 1);
        return strIn;
    }
    
    //gets tab spacing for printing table with aligned columns
    public int getColumnTabs(int colNum, int rowNum){
        int tabWidth = 4;
        int maxColWidth = 0;
        int maxColTabWidth;
        int colWidth;
        int colTabWidth;

        for(int rowCnt = 0; rowCnt <= getNumEntries(); rowCnt++)
            if(tableData.get(colNum).get(rowCnt).length() > maxColWidth)
                maxColWidth = tableData.get(colNum).get(rowCnt).length();

        colWidth = tableData.get(colNum).get(rowNum).length();

        //number of tabs to "contain" maximum width entry
        maxColTabWidth = (int) Math.ceil(((double) maxColWidth + 1)/tabWidth);

        //number of tabs to "contain" given entry
        colTabWidth = (int) Math.ceil(((double) colWidth + 1)/tabWidth);

        return maxColTabWidth - colTabWidth + 1;
    }
    
    public ConditionResult evaluateCondition(String colName, String comparator, String condValue,
                                             int rowNum){
        //get the relevant table value
        String tabValue = getColumn(colName).get(rowNum);
        
        //find out type of the provided value
        double doubValue = 0;
        DataType typeOfValue = DataType.STRING;
        try{
            doubValue = Double.parseDouble(condValue);
            typeOfValue = DataType.DOUBLE;
        }
        catch(NumberFormatException notDouble){}

        //find out type of the table value
        double doubTabVal = 0;
        DataType typeOfTabVal = DataType.STRING;
        try{
            doubTabVal = Double.parseDouble(tabValue);
            typeOfTabVal = DataType.DOUBLE;
        }
        catch(NumberFormatException notDouble){}
        
        //cannot compare numeric and string types
        if( ((typeOfValue == DataType.STRING && typeOfTabVal != DataType.STRING)
            || (typeOfValue != DataType.STRING && typeOfTabVal == DataType.STRING))
            && !comparator.equalsIgnoreCase("LIKE"))
            return ConditionResult.INVALID;
        
        //determine type of comparison being performed
        DataType comparisonType = DataType.STRING; //default compares strings
        if(comparator.equalsIgnoreCase("LIKE"))
            comparisonType = DataType.STRING; //"LIKE" only works with strings
        else if(typeOfValue == DataType.DOUBLE && typeOfTabVal == DataType.DOUBLE)
            comparisonType = DataType.DOUBLE; //comparing doubles
        
        if(comparisonType == DataType.DOUBLE)
            return compareNumerics(doubTabVal, comparator, doubValue);
        else return compareStrings(tabValue, comparator, condValue);
    }
    
    public ConditionResult compareNumerics(double tabValue, String comparator, double condValue){
        double epsilon = 0.0001;
        if(comparator.equals("==") && Math.abs(tabValue - condValue) < epsilon){
            return ConditionResult.TRUE;
        }else if(comparator.equals(">") && tabValue > condValue){
            return ConditionResult.TRUE;
        }else if(comparator.equals("<") && tabValue < condValue){
            return ConditionResult.TRUE;
        }else if(comparator.equals(">=") && tabValue >= condValue){
            return ConditionResult.TRUE;
        }else if(comparator.equals("<=") && tabValue <= condValue){
            return ConditionResult.TRUE;
        }else if(comparator.equals("!=") && Math.abs(tabValue - condValue) > epsilon){
            return ConditionResult.TRUE;
        }else return ConditionResult.FALSE;
    }

    public ConditionResult compareStrings(String colName, String comparator, String condValue){
        if(comparator.equals("==") && colName.equalsIgnoreCase(condValue)){
            return ConditionResult.TRUE;
        }else if(comparator.equals(">") && colName.compareToIgnoreCase(condValue) > 0){
            return ConditionResult.TRUE;
        }else if(comparator.equals("<") && colName.compareToIgnoreCase(condValue) < 0){
            return ConditionResult.TRUE;
        }else if(comparator.equals(">=") && colName.compareToIgnoreCase(condValue) >= 0){
            return ConditionResult.TRUE;
        }else if(comparator.equals("<=") && colName.compareToIgnoreCase(condValue) <= 0){
            return ConditionResult.TRUE;
        }else if(comparator.equals("!=") && !colName.equalsIgnoreCase(condValue)){
            return ConditionResult.TRUE;
        }else if(comparator.equalsIgnoreCase("LIKE")){
            condValue = removeStrLitQuotes(condValue);
            if(colName.contains(condValue)) //"contains" is case sensitive
                return ConditionResult.TRUE;
        }
        return ConditionResult.FALSE;
    }
    
    public boolean removeRow(int rowNum){
        //cannot remove the "header" row
        if(rowNum == 0)
            return false;
        
        //remove the row
        for(int colCnt = 0; colCnt < getNumAttributes(); colCnt++)
            tableData.get(colCnt).remove(rowNum);
        
        //finally remove id value
        idValues.remove(rowNum);
        
        return true;
    }
    
    public String getTableName(){
        return tableName;
    }

    public List<Integer> getIDValues(){
        return idValues;
    }
}