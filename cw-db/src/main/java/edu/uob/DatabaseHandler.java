package edu.uob;

import java.io.File;
import java.util.*;

public class DatabaseHandler{
    String fileSep = File.separator;
    
    //folder within which all data is stored
    String parentFolderPath;
    
    //database file information
    String databaseName;
    String databasePath;
    File databaseFile;
    
    //table handler - deals with all table operations
    TableHandler tableHandler;
    
    public DatabaseHandler(String parentFolderPath){
        this.parentFolderPath = parentFolderPath;
        databaseName = databasePath = null;
        databaseFile = null;
        tableHandler = new TableHandler(parentFolderPath);
        tableHandler.setDatabaseName(null);
    }

    //updates databaseName, databasePath, databaseFile
    public void updateFilePath(String newDB){
        databaseName = newDB;
        databasePath = parentFolderPath + fileSep + databaseName;
        databaseFile = new File(databasePath);
        
        //also ensure the table handler is updated
        tableHandler.setDatabaseName(databaseName);
    }
    
    public File getFileFromDB(String selectedDB){
        String selectedPath = parentFolderPath + fileSep + selectedDB;
        return new File(selectedPath);
    }
    
    public boolean makeDatabaseDir(String newDB){
        updateFilePath(newDB);
        
        //only make directory if non-existent
        if(!databaseFile.exists())
            return databaseFile.mkdir();
        return false;
    }
    
    public boolean deleteDatabaseDir(String selectedDB){
        File fileOfDB = getFileFromDB(selectedDB);
        
        //check if database exists
        if(!fileOfDB.exists())
            return false;
        
        //delete any subfiles if present
        File [] subFiles = fileOfDB.listFiles();
        if(subFiles != null)
            for(File subFile : subFiles)
                subFile.delete();
        
        //delete directory
        fileOfDB.delete();
        
        //if you deleted the "current" database
        //then set all file references to null
        if(selectedDB.equals(databaseName)){
            databaseName = databasePath = null;
            databaseFile = null;
            tableHandler.setDatabaseName(null);
        }
        
        return true;
    }
    
    //returns false if errors encountered responding to command
    public String respondToCommand(String commandStr){
        //string to tokens
        List<Token> commandToks = Tokeniser.extractTokens(commandStr);

        //if token list too long ignore request
        if(commandToks.size() > 1000)
            return "[ERROR]\nCommand length exceeds limit.\n";
        
        //tokens to parse tree
        Node parseTree = SQLSimpGrammar.command.parseTokens(commandToks, 0);
        
        //parsing failure means return false
        if(parseTree == null)
            return "[ERROR]\nParsing failure. Please check command syntax.\n";

        //check reserved words are not being used
        if(!parseTree.checkNoReservedWordsUsed())
            return "[ERROR]\nCannot use SQL reserved words for attribute, table or database names.\n";

        if(parseTree.findByRuleName(RuleName.USE) != null)
            return useCommand(parseTree);
        
        else if(parseTree.findByRuleName(RuleName.CREATE_DB) != null)
            return createDatabaseCommand(parseTree);
        
        else if(parseTree.findByRuleName(RuleName.DROP_DB) != null)
            return dropDatabaseCommand(parseTree);
        
        //attempting non-database commands without database specified gives error
        else if(databaseName == null)
            return "[ERROR]\nPlease specify database.\n";
        
        String nameOfTable = parseTree.findTokenString(RuleName.TABLE_NAME).toLowerCase();
        boolean tableExists = tableHandler.checkTableExists(nameOfTable);
        
        if(parseTree.findByRuleName(RuleName.CREATE_TABLE) != null)
            return createTableCommand(parseTree, nameOfTable, tableExists);
        
        //all the commands below need tableExists to be true
        if(!tableExists)
            return "[ERROR]\nTable does not exist. Check database correctly set.\n";
        
        if(parseTree.findByRuleName(RuleName.DROP_TABLE) != null)
            return dropTableCommand(nameOfTable);
        
        //load the table for the subsequent commands
        tableHandler.setTableName(nameOfTable);
        tableHandler.loadTable();
        
        if(parseTree.findByRuleName(RuleName.ALTER) != null)
            return alterCommand(parseTree);

        else if(parseTree.findByRuleName(RuleName.JOIN) != null)
            return joinCommand(parseTree);

        //for all remaining commands the attributes must exist
        //check all attributes within the command exist
        if(!parseTree.checkAttributesExist(tableHandler.currTable))
            return "[ERROR]\nNot all specified attributes exist.\n";

        if(parseTree.findByRuleName(RuleName.INSERT) != null)
            return insertCommand(parseTree);
        
        else if(parseTree.findByRuleName(RuleName.SELECT) != null)
            return selectCommand(parseTree);
        
        else if(parseTree.findByRuleName(RuleName.UPDATE) != null)
            return updateCommand(parseTree);

        else if(parseTree.findByRuleName(RuleName.DELETE) != null)
            return deleteCommand(parseTree);

        else
            return "[ERROR]\nCommand Execution Failure\n";          
    }
    
    public String useCommand(Node parseTree){
        String nameOfDB = parseTree.findTokenString(RuleName.DB_NAME).toLowerCase();

        //get a file object to check if database exists
        File fileOfDB = new File(parentFolderPath + fileSep + nameOfDB);

        //return error if does not exist
        if(!fileOfDB.exists())
            return "[ERROR]\nDatabase does not exist.\n";
        else{
            updateFilePath(nameOfDB);
            return "[OK]\n";
        }
    }
    
    public String createDatabaseCommand(Node parseTree){
        String nameOfDB = parseTree.findTokenString(RuleName.DB_NAME).toLowerCase();

        //get a file object to check if database exists
        File fileOfDB = new File(parentFolderPath + fileSep + nameOfDB);

        //return error if already exists
        if(fileOfDB.exists())
            return "[ERROR]\nDatabase already exists.\n";
        else{
            makeDatabaseDir(nameOfDB);
            return "[OK]\n";
        }
    }
    
    public String dropDatabaseCommand(Node parseTree){
        String nameOfDB = parseTree.findTokenString(RuleName.DB_NAME).toLowerCase();
        if(!deleteDatabaseDir(nameOfDB))
            return "[ERROR]\nPlease check database exists.\n";
        return "[OK]\n";
    }
    
    public String createTableCommand(Node parseTree, String nameOfTable, boolean tableExists){
        //check whether table already exists
        if(tableExists)
            return "[ERROR]\nTable already exists.\n";
        
        //get all the attributes from the command
        List<String> tableAttributes = parseTree.getAttributesOrVals(RuleName.ATTRIBUTE);
        String[] attributeArray = getStringArray(tableAttributes);
        
        //then set the table name in table handler
        tableHandler.setTableName(nameOfTable);
        
        //create table with attributes if that is the command
        if(parseTree.findByRuleName(RuleName.CR_TABLE_ATTR) != null)
            tableHandler.createTable(attributeArray);
        else
            tableHandler.createTable();
        return "[OK]\n";
    }
    
    public String dropTableCommand(String nameOfTable){
        //if deleted currently referenced table
        //then remove all references within tableHandler
        if(nameOfTable.equals(tableHandler.getTableName()))
            tableHandler.setDatabaseName(databaseName);
        
        if(tableHandler.deleteTable(nameOfTable))
            return "[OK]\n";
        else return "[ERROR]\nCould not delete table.\n";
    }
    
    public String alterCommand(Node parseTree){
        String alterationType = parseTree.findTokenString(RuleName.ALTERATION_TYPE);
        String colName = parseTree.findTokenString(RuleName.ATTRIBUTE);
        
        if(alterationType.equalsIgnoreCase("ADD")){
            if(tableHandler.currTable.checkAttributeExists(colName)){
                return "[ERROR]\nColumn already exists.\n";
            }else if(tableHandler.currTable.getNumAttributes() >= 100)
                return "[ERROR]\nTable attribute limit exceeded.\n";
            else{
                tableHandler.currTable.addColumn(colName);
                tableHandler.saveTable();
                return "[OK]\n";
            }    
        }
        
        //otherwise it must be a "DROP" command
        else{
            if(!tableHandler.currTable.checkAttributeExists(colName)){
                return "[ERROR]\nColumn does not exist.\n";
            }else if(colName.equalsIgnoreCase("ID")){
                return "[ERROR]\nCannot delete ID column.\n";
            }else{
                tableHandler.currTable.removeColumn(colName);
                tableHandler.saveTable();
                return "[OK]\n";
            } 
        }
    }
    
    public String insertCommand(Node parseTree){
        //get all the values from the command
        List<String> tableVals = parseTree.getAttributesOrVals(RuleName.VALUE);
        String[] valueArray = getStringArray(tableVals);

        //cannot insert if row limit exceeded
        if(tableHandler.currTable.getNumEntries() >= 1000)
            return "[ERROR]\nTable entry limit exceeded.\n";
        
        if(tableHandler.currTable.addRow(valueArray, false)){
            tableHandler.saveTable();
            return "[OK]\n";
        }else{
            return "[ERROR]\nFailed to add values. Check number of columns correct.\n";
        }
    }
    
    public String selectCommand(Node parseTree){
        //create an array of all true values
        boolean selectAll[] = new boolean[tableHandler.currTable.getNumEntries()];
        Arrays.fill(selectAll, true);
        
        //create a copy of the table
        TableStructure outputTable = tableHandler.currTable.selectRows(selectAll);
        
        if(parseTree.findByRuleName(RuleName.SELECT_COND) != null){
            //evaluate the condition for each row
            boolean condSelection[] = generateSelection(parseTree);

            //perform the selection
            outputTable = outputTable.selectRows(condSelection);
        }
        
        //get the attributes for filtering the columns
        Node wildAttrList = parseTree.findByRuleName(RuleName.WILD_ATTR_LIST);
        List<String> attributesList = wildAttrList.getAttributesOrVals(RuleName.ATTRIBUTE);
        
        //filter table by columns specified if no asterisk in command
        if(parseTree.findByRuleName(RuleName.ASTERISK_LITERAL) == null){
            ArrListTable attrTable = new ArrListTable("");
            
            //remove "id" column added by default in constructor
            attrTable.tableData.remove(0);
            
            //add all selected columns in order
            for(String colName : attributesList)
                attrTable.tableData.add(outputTable.getColumn(colName));
            
            //finally - add the idValues list and update reference
            attrTable.idValues = outputTable.getIDValues();
            outputTable = attrTable;
        }
                
        //print table
        return "[OK]\n" + outputTable.printTable();
    }
    
    public String updateCommand(Node parseTree){
        //get the name value list node
        Node nameValPair = parseTree.findByRuleName(RuleName.NAME_VALUE_LIST);
        
        //get the attributes within name value list
        List<String> attributesList = nameValPair.getAttributesOrVals(RuleName.ATTRIBUTE);
        String[] attrArray = getStringArray(attributesList);

        //prevent "ID" from being changed
        for(String attr : attributesList)
            if(attr.equalsIgnoreCase("ID"))
                return "[ERROR]\nThe ID column cannot be changed.\n";

        //get the values within name value list
        List<String> valuesList = nameValPair.getAttributesOrVals(RuleName.VALUE);
        String[] valueArray = getStringArray(valuesList);
        
        //get 2d array of attributes and values
        String[][] nameValPairs = {attrArray, valueArray};
        
        //perform conditional selection
        boolean[] condSelection = generateSelection(parseTree);

        //finally - update the values
        tableHandler.currTable.updateTable(condSelection, nameValPairs);
        tableHandler.saveTable();
        return "[OK]\n";
    }
    
    public String deleteCommand(Node parseTree){
        //evaluate the condition for each row
        boolean condSelection[] = generateSelection(parseTree);

        //delete the matching rows
        //go in reverse order to avoid messing up the progression with rowCnt
        for(int rowCnt = tableHandler.currTable.getNumEntries(); rowCnt >= 1; rowCnt--)
            if(condSelection[rowCnt - 1])
                tableHandler.currTable.removeRow(rowCnt);

        tableHandler.saveTable();
        
        return "[OK]\n";
    }
    
    public String joinCommand(Node parseTree){
        //get the table names
        List <String> tableNames = parseTree.getAttributesOrVals(RuleName.TABLE_NAME);

        //convert table names to lowercase
        tableNames.set(0, tableNames.get(0).toLowerCase());
        tableNames.set(1, tableNames.get(1).toLowerCase());

        //check tables exist
        if(!tableHandler.checkTableExists(tableNames.get(0))
            || !tableHandler.checkTableExists(tableNames.get(1)))
            return "[ERROR]\nCheck both tables exist within database.\n";

        //load both tables
        TableHandler handler1 = new TableHandler(databaseName, tableNames.get(0), parentFolderPath);
        TableHandler handler2 = new TableHandler(databaseName, tableNames.get(1), parentFolderPath);
        handler1.loadTable();
        handler2.loadTable();
        
        //get the attribute names
        List <String> attrNames = parseTree.getAttributesOrVals(RuleName.ATTRIBUTE);
        String attr1 = attrNames.get(0);
        String attr2 = attrNames.get(1);
        
        //check attributes exist
        if(!handler1.currTable.checkAttributeExists(attr1)
            || !handler2.currTable.checkAttributeExists(attr2))
            return "[ERROR]\nNon-existent attribute(s) within table(s).\n";

        //create table for join result
        String[] attrArray = getJoinedRow(handler1.currTable, handler2.currTable,
                0, 0, true);
        TableStructure joinTable = new ArrListTable("", attrArray);
        
        //go over all the rows from both tables an perform join
        for(int rowCnt1 = 1; rowCnt1 <= handler1.currTable.getNumEntries(); rowCnt1++)
            for(int rowCnt2 = 1; rowCnt2 <= handler2.currTable.getNumEntries(); rowCnt2++){
                String valueTable1 = handler1.currTable.getColumn(attr1).get(rowCnt1);
                String valueTable2 = handler2.currTable.getColumn(attr2).get(rowCnt2);
                //perform simple string comparison as discussed in Teams
                if(valueTable1.equals(valueTable2))
                    joinTable.addRow(getJoinedRow(handler1.currTable, handler2.currTable,
                            rowCnt1, rowCnt2, false), false);
            }
        
        //finally - remove the attributes used to perform the join
        //but only remove them if they are not "id"
        if(!attr1.equalsIgnoreCase("ID"))
            joinTable.removeColumn(handler1.getTableName() + "." + attr1);
        if(!attr2.equalsIgnoreCase("ID"))
            joinTable.removeColumn(handler2.getTableName() + "." + attr2);
        
        return "[OK]\n" + joinTable.printTable();        
    }
    
    //note this excludes id columns from the join
    public String[] getJoinedRow(TableStructure table1, TableStructure table2, int rowCnt1,
                                 int rowCnt2, boolean prependName){
        String[] row1 = table1.getRow(rowCnt1);
        String[] row2 = table2.getRow(rowCnt2);
        String tableName1 = table1.getTableName();
        String tableName2 = table2.getTableName();
        
        //prepend all table attributes with respective table names
        if(prependName){
            for(int attCnt = 0; attCnt < row1.length; attCnt++)
                row1[attCnt] = tableName1 + "." + row1[attCnt];
            for(int attCnt = 0; attCnt < row2.length; attCnt++)
                row2[attCnt] = tableName2 + "." + row2[attCnt];
        }
        
        String[] joinedRow = new String[row1.length + row2.length - 2];
        System.arraycopy(row1, 1, joinedRow, 0, row1.length - 1);
        System.arraycopy(row2, 1, joinedRow, row1.length - 1, row2.length - 1);
        return joinedRow;
    }
    
    public boolean[] generateSelection(Node parseTree){
        boolean condSelection[] = new boolean[tableHandler.currTable.getNumEntries()];
        
        //evaluate the condition for each row
        for(int rowCnt = 1; rowCnt <= tableHandler.currTable.getNumEntries(); rowCnt++){
            ConditionResult rowResult = parseTree.checkConditionsTrue(tableHandler.currTable, rowCnt);
            if(rowResult == ConditionResult.TRUE)
                condSelection[rowCnt - 1] = true;
            else if(rowResult == ConditionResult.FALSE || rowResult == ConditionResult.INVALID)
                condSelection[rowCnt - 1] = false;
        }
        
        return condSelection;
    }
    
    //utility function to convert list to array
    String [] getStringArray(List<String> listVals){
        int numVals = listVals.size();
        return listVals.toArray(new String[numVals]);
    }
}
