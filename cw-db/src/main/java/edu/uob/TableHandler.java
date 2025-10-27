package edu.uob;

import java.io.*;

public class TableHandler{
    String fileSep = File.separator;
    
    //folder within which all data is stored
    String parentFolderPath;
    
    //database file information
    String databaseName;
    
    //table file information
    String tableName;
    String tablePath;
    File tableFile;
    
    //database currently being worked on
    ArrListTable currTable;

    public TableHandler(String parentFolderPath){
        this.parentFolderPath = parentFolderPath;
        tableName = databaseName = tablePath = null;
        tableFile = null;
    }
    
    public TableHandler(String databaseName, String tableName, String parentFolderPath){
        this.parentFolderPath = parentFolderPath;
        this.databaseName = databaseName;
        setTableName(tableName);
    }

    public boolean createTable(){
        currTable = new ArrListTable(tableName);
        return saveTable();
    }
    
    public boolean createTable(String[] attributes){
        currTable = new ArrListTable(tableName, attributes);
        return saveTable();
    }

    //loads a given table
    //returns false if load fails
    public boolean loadTable(){
        FileReader reader;
        String nextLine;
        boolean firstLine = true;

        try{ reader = new FileReader(tableFile); }
        catch(FileNotFoundException fileExcep){ return false; }

        BufferedReader buffReader = new BufferedReader(reader);

        //read the first line
        try{ nextLine = buffReader.readLine(); }
        catch(IOException readerExc){ return false; }

        while(nextLine != null){

            //split the row into individual values
            String[] rowValues = nextLine.split("\t");

            //for the first line, which contains column names, create table
            if(firstLine){
                currTable = new ArrListTable(tableName, rowValues);
                firstLine = false;
            }else{
                currTable.addRow(rowValues, true);
            }

            //keep reading lines
            try{ nextLine = buffReader.readLine(); }
            catch(IOException readExc){ return false; }
        }
        
        //load last assigned ID
        if(!loadID())
            return false;
        
        try{ buffReader.close(); }
        catch(IOException closeExc){ return false; }

        return true;
    }

    //saves a given table
    //returns false if save fails
    public boolean saveTable(){
        FileWriter writer;
        String nextLine;

        //delete file if already exists
        tableFile.delete();

        //create file
        try{ tableFile.createNewFile(); }
        catch(IOException fileCrExc){ return false; }

        try{ writer = new FileWriter(tableFile); }
        catch(IOException writerExc){ return false; }

        BufferedWriter buffWriter = new BufferedWriter(writer);

        for(int rowCnt = 0; rowCnt <= currTable.getNumEntries(); rowCnt++){
            nextLine = "";
            for(int colCnt = 0; colCnt < currTable.getNumAttributes(); colCnt++){
                nextLine += currTable.tableData.get(colCnt).get(rowCnt);
                if(colCnt < currTable.getNumAttributes() - 1)
                    nextLine += "\t";
            }
            if(rowCnt < currTable.getNumEntries())
                nextLine += "\n";
            try{ buffWriter.write(nextLine); }
            catch(IOException writeExc){ return false; }
        }

        try{ buffWriter.close(); }
        catch(IOException closeExc){ return false; }

        //persistent storage of ID
        if(!saveID())
            return false;

        return true;
    }
    
    //checks whether table exists within current database
    //returns true if does exist
    public boolean checkTableExists(String nameToCheck){
        File fileToCheck = getFileFromTable(nameToCheck);
        return fileToCheck.exists();
    }
    
    public File getFileFromTable(String selectedTable){
        String selectedPath = parentFolderPath + fileSep + databaseName 
            + fileSep + selectedTable + ".tab";
        return new File(selectedPath);
    }
    
    //returns true if table file is successfully deleted
    public boolean deleteTable(String selectedTable){
        File fileToDelete = getFileFromTable(selectedTable);
        return fileToDelete.delete();
    }

    public String getTableName(){
        return tableName;
    }

    public void setTableName(String tableName){
        this.tableName = tableName;
        tablePath = parentFolderPath + fileSep + databaseName 
            + fileSep + tableName + ".tab";
        tableFile = new File(tablePath);
    }

    public void setDatabaseName(String databaseName){
        this.databaseName = databaseName;
        
        //"deselect" pointing to any table
        tableName = null;
        tableFile = null;
        tablePath = null;
        currTable = null;
    }

    public boolean saveID(){
        String fileNameForID = tableName + "_ID";
        File fileForID = getFileFromTable(fileNameForID);
        FileWriter writerForID;

        //delete file if already exists
        fileForID.delete();

        //create file
        try{ fileForID.createNewFile(); }
        catch(IOException fileCrExc){ return false; }

        try{ writerForID = new FileWriter(fileForID); }
        catch(IOException writerExc){ return false; }

        BufferedWriter buffWriter = new BufferedWriter(writerForID);
        try{ buffWriter.write(Integer.toString(currTable.lastAssignedID)); }
        catch(IOException writeExc){ return false; }

        try{ buffWriter.close(); }
        catch(IOException closeExc){ return false; }
        return true;
    }

    public boolean loadID(){
        String fileNameForID = tableName + "_ID";
        File fileForID = getFileFromTable(fileNameForID);
        FileReader readerForID;
        String inputStr;

        try{ readerForID = new FileReader(fileForID); }
        catch(FileNotFoundException fileExcep){ return false; }

        BufferedReader buffReader = new BufferedReader(readerForID);

        //read the first line
        try{ inputStr = buffReader.readLine(); }
        catch(IOException readerExc){ return false; }

        //convert ID string to number
        currTable.lastAssignedID = Integer.parseInt(inputStr);

        try{ buffReader.close(); }
        catch(IOException closeExc){ return false; }
        return true;
    }
}
