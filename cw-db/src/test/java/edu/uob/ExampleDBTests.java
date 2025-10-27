package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.io.File;
import java.nio.file.Paths;

public class ExampleDBTests {

    private DBServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    // Random name generator - useful for testing "bare earth" queries (i.e. where tables don't previously exist)
    private String generateRandomName() {
        String randomName = "";
        for(int i=0; i<10 ;i++) randomName += (char)( 97 + (Math.random() * 25.0));
        return randomName;
    }

    private String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
        "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // A basic test that creates a database, creates a table, inserts some test data, then queries it.
    // It then checks the response to see that a couple of the entries in the table are returned as expected
    @Test
    public void testBasicCreateAndQuery() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("[OK]"), "A valid query was made, however an [OK] tag was not returned");
        assertFalse(response.contains("[ERROR]"), "A valid query was made, however an [ERROR] tag was returned");
        assertTrue(response.contains("Simon"), "An attempt was made to add Simon to the table, but they were not returned by SELECT *");
        assertTrue(response.contains("Chris"), "An attempt was made to add Chris to the table, but they were not returned by SELECT *");
    }

    // A test to make sure that querying returns a valid ID (this test also implicitly checks the "==" condition)
    // (these IDs are used to create relations between tables, so it is essential that suitable IDs are being generated and returned !)
    @Test
    public void testQueryID() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT id FROM marks WHERE name == 'Simon';");
        // Convert multi-lined responses into just a single line
        String singleLine = response.replace("\n"," ").trim();
        // Split the line on the space character
        String[] tokens = singleLine.split(" ");
        // Check that the very last token is a number (which should be the ID of the entry)
        String lastToken = tokens[tokens.length-1];
        try {
            Integer.parseInt(lastToken);
        } catch (NumberFormatException nfe) {
            fail("The last token returned by `SELECT id FROM marks WHERE name == 'Simon';` should have been an integer ID, but was " + lastToken);
        }
    }

    // A test to make sure that databases can be reopened after server restart
    @Test
    public void testTablePersistsAfterRestart() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        // Create a new server object
        server = new DBServer();
        sendCommandToServer("USE " + randomName + ";");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("Simon"), "Simon was added to a table and the server restarted - but Simon was not returned by SELECT *");
    }

    // Test to make sure that the [ERROR] tag is returned in the case of an error (and NOT the [OK] tag)
    @Test
    public void testForErrorTag() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT * FROM libraryfines;");
        assertTrue(response.contains("[ERROR]"), "An attempt was made to access a non-existent table, however an [ERROR] tag was not returned");
        assertFalse(response.contains("[OK]"), "An attempt was made to access a non-existent table, however an [OK] tag was returned");
    }

    @Test
    public void testToken(){
        //TESTS FOR "SPLIT BY SPACES" METHOD
        List<String> tokStrings;

        //empty string
        tokStrings = Tokeniser.splitBySpaces("");
        assertTrue(tokStrings.size() == 0);

        //string with only spaces
        tokStrings = Tokeniser.splitBySpaces("    ");
        assertTrue(tokStrings.size() == 0);

        //string with leading and trailing spaces
        tokStrings = Tokeniser.splitBySpaces(" string with leading and trailing spaces  ");
        assertTrue(tokStrings.size() == 6);

        //string with variable internal spacing
        tokStrings = Tokeniser.splitBySpaces("string with  varied    spaces");
        assertTrue(tokStrings.size() == 4);

        //string with string literal with spaces
        tokStrings = Tokeniser.splitBySpaces("string with 'string literal $$'");
        assertTrue(tokStrings.size() == 3);

        //string with irregular arrangement of single quotes
        tokStrings = Tokeniser.splitBySpaces("odd string: ''''");
        assertTrue(tokStrings.size() == 3);

        //string with single quote character
        tokStrings = Tokeniser.splitBySpaces("'");
        assertTrue(tokStrings.size() == 1);

        //TESTS FOR "EXTRACT TOKENS" METHOD
        List<Token> tokensRegularSp;
        List<Token> tokensForTest;

        //check spaces have no effect
        tokensRegularSp = strToTokens("Here is a list of tokens");
        tokensForTest = Tokeniser.extractTokens("Here is a list of tokens");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));
        tokensForTest = Tokeniser.extractTokens("   Here is a list of tokens   ");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));
        tokensForTest = Tokeniser.extractTokens("Here    is     a      list of tokens");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));

        //check for empty string
        tokensForTest = Tokeniser.extractTokens("");
        assertTrue(tokensForTest.size() == 0);

        //check for series of spaces
        tokensForTest = Tokeniser.extractTokens("    ");
        assertTrue(tokensForTest.size() == 0);

        //test working with brackets and punctuator at end
        tokensRegularSp = strToTokens("Here is a ( bracketed ) word .");
        tokensForTest = Tokeniser.extractTokens("Here is a (bracketed) word.");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));

        //check consecutive symbolic characters are separated
        tokensRegularSp = strToTokens("Here is a ( ( double bracketed ) ) word .");
        tokensForTest = Tokeniser.extractTokens("Here is a ((double bracketed)) word.");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));

        //check string literal with spaces correctly tokenised
        tokensForTest = Tokeniser.extractTokens("Here is a 'string literal' term;");
        assertTrue(tokensForTest.size() == 6);

        //check string literal with spaces and symbols correctly tokenised
        tokensForTest = Tokeniser.extractTokens("Here is a weird literal: '!$% &*() []{}';");
        assertTrue(tokensForTest.size() == 8);

        //check string literal with spaces and symbols correctly tokenised
        tokensForTest = Tokeniser.extractTokens("Here is a weird literal: '!$% &*() []{}';");
        assertTrue(tokensForTest.size() == 8);

        //check parsing of numerics and comparator symbols
        tokensRegularSp = strToTokens("xyz >= abc == 2.66 + -5 != -12.12 > 16 < 88.76 or 2 ;");
        tokensForTest = Tokeniser.extractTokens("xyz >= abc == +2.66 + -5 != -12.12 > 16 < 88.76 or +2;");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));

        //string literal at end of string
        tokensRegularSp = strToTokens("trying to cause a 'crash'");
        tokensForTest = Tokeniser.extractTokens("trying to cause a 'crash'");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));

        //string literal at end without closing quote mark
        tokensRegularSp = strToTokens("trying to cause a 'crash");
        tokensForTest = Tokeniser.extractTokens("trying to cause a 'crash");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));

        //more incomplete entities
        tokensRegularSp = strToTokens("incomplete decimal : 17 .");
        tokensForTest = Tokeniser.extractTokens("incomplete decimal: +17.");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));
        tokensRegularSp = strToTokens("incomplete operator : <");
        tokensForTest = Tokeniser.extractTokens("incomplete operator:<");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));
        tokensRegularSp = strToTokens("incomplete number : -");
        tokensForTest = Tokeniser.extractTokens("incomplete number: -");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));
        tokensRegularSp = strToTokens("incomplete number : . 125");
        tokensForTest = Tokeniser.extractTokens("incomplete number: .125");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));
        tokensRegularSp = strToTokens("incomplete number . 125");
        tokensForTest = Tokeniser.extractTokens("incomplete number.125");
        assertTrue(compareTokens(tokensForTest, tokensRegularSp));
    }

    @Test
    public void testParser(){
        String command;

        //VALID "USE" COMMANDS
        command = "use cars;";
        assertTrue(parseTreeNuLeavesCorrect(command, 3));

        //VALID "CREATE" COMMANDS
        command = "create database students;";
        assertTrue(parseTreeNuLeavesCorrect(command, 4));
        command = "CREATE TABLE beverages;";
        assertTrue(parseTreeNuLeavesCorrect(command, 4));
        command = "create table pets (name);";
        assertTrue(parseTreeNuLeavesCorrect(command, 7));
        command = "create table pets (name, species);";
        assertTrue(parseTreeNuLeavesCorrect(command, 9));
        command = "create table pets (name, species, gender, birthday);";
        assertTrue(parseTreeNuLeavesCorrect(command, 13));

        //VALID "DROP" COMMANDS
        command = "DROP DATABASE students;";
        assertTrue(parseTreeNuLeavesCorrect(command, 4));
        command = "drop table pets;";
        assertTrue(parseTreeNuLeavesCorrect(command, 4));

        //VALID "ALTER" COMMANDS
        command = "ALTER TABLE students ADD major;";
        assertTrue(parseTreeNuLeavesCorrect(command, 6));
        command = "alter table students drop favouriteFood;";
        assertTrue(parseTreeNuLeavesCorrect(command, 6));

        //VALID "INSERT" COMMANDS
        command = "INSERT INTO birds VALUES ('Golden Eagle', 'Raptor', 2.15, 15);";
        assertTrue(parseTreeNuLeavesCorrect(command, 14));
        command = "INSERT INTO birds VALUES ('Bullfinch', 'Finch', NULL, 10);";
        assertTrue(parseTreeNuLeavesCorrect(command, 14));
        command = "insert into cars values (98850, NULL, TRUE, 3.6);";
        assertTrue(parseTreeNuLeavesCorrect(command, 14));
        command = "insert into coinTosses values (false);";
        assertTrue(parseTreeNuLeavesCorrect(command, 8));

        //VALID "SELECT" COMMANDS
        command = "select * from accounts;";
        assertTrue(parseTreeNuLeavesCorrect(command, 5));
        command = "select price, tastiness, calories, smell from food;";
        assertTrue(parseTreeNuLeavesCorrect(command, 11));
        command = "sELECT result fROM coinTosses;";
        assertTrue(parseTreeNuLeavesCorrect(command, 5));
        command = "SELECT * FROM cars WHERE miles < 20000;";
        assertTrue(parseTreeNuLeavesCorrect(command, 9));
        command = "SELECT * FROM cars WHERE (miles<20000)or((age<2) and model == 'Land Rover');";
        assertTrue(parseTreeNuLeavesCorrect(command, 23));
        command = "SELECT * FROM cars WHERE (miles<20000)or(((age<2) and make == 'Land Rover')and(make LIKE 'cruiser'));";
        assertTrue(parseTreeNuLeavesCorrect(command, 31));
        command = "select * from maths where (((x>1.63)and y!='E') or(z != True and p <= 42) and d LIKE 'Dee' or p==null and f<0.68);";
        assertTrue(parseTreeNuLeavesCorrect(command, 41));

        //VALID "UPDATE" COMMANDS
        command = "Update groceries Set bags = 5, price = 65.43, supermarket = 'Tesco Local' Where location == 'Bristol';";
        assertTrue(parseTreeNuLeavesCorrect(command, 19));
        command = "Update shopping Set cost = '$15.99', supplier = 'J&J Clothing Co.', line = '#mod_box' Where location == 'London';";
        assertTrue(parseTreeNuLeavesCorrect(command, 19));

        //VALID "DELETE" COMMANDS
        command = "delete from cars where colour == 'yellow' or miles > 10000 or price > 15000 or make == 'Vauxhall';";
        assertTrue(parseTreeNuLeavesCorrect(command, 20));
        command = "delete from stocks where strike == 150.00 and maturity == 2.0;";
        assertTrue(parseTreeNuLeavesCorrect(command, 12));

        //VALID "JOIN" COMMANDS
        command = "JOIN MUSIC AND PARTIES ON PEOPLE AND TASTES;";
        assertTrue(parseTreeNuLeavesCorrect(command, 9));
        command = "JOIN 66MUSIC AND PARTIES101 ON PEOPL3 AND TA5TES;";
        assertTrue(parseTreeNuLeavesCorrect(command, 9));

        //VALID COMMANDS WITH VARYING SPACES
        command = "     JOIN MUSIC AND PARTIES ON PEOPLE AND TASTES     ;";
        assertTrue(parseTreeNuLeavesCorrect(command, 9));
        command = "select    price, tastiness   , calories, smell     from food ; ";
        assertTrue(parseTreeNuLeavesCorrect(command, 11));

        //VALID SYMBOLIC CHARACTERS WITHIN STRING LITERALS
        command = "SELECT * FROM language WHERE symbols == '!#$%&()*+,-./:;>=<?@[\\]^_`{}~';";
        assertTrue(parseTreeNuLeavesCorrect(command, 9));

        //EMPTY STRING
        command = "SELECT * FROM riddle WHERE clue == '';";
        assertTrue(parseTreeNuLeavesCorrect(command, 9));

        //INVALID COMMANDS

        //commands without semicolon
        command = "use cars";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "create table pets (name, species, gender, birthday)";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "SELECT * FROM cars WHERE miles < 20000";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));

        //commands missing words
        command = "select price, tastiness, calories, smell food;";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "MUSIC AND PARTIES ON PEOPLE AND TASTES;";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "insert coinTosses values (false);";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));

        //commands with bracket errors
        command = "create table pets name;";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "INSERT INTO birds VALUES 'Golden Eagle', 'Raptor', 2.15, 15;";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "select (price, tastiness, calories, smell) from food;";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));

        //plaintext errors
        command = "create database finance_data;";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "ALTER TABLE censors**p ADD major;";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "alter table students drop food$$Money;";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));

        //number formatting errors
        command = "INSERT INTO birds VALUES 'Golden Eagle', 'Raptor', 2., 15;";
        assertTrue(parseTreeNuLeavesCorrect(command,0));
        command = "INSERT INTO birds VALUES 'Golden Eagle', 'Raptor', .99, 15;";
        assertTrue(parseTreeNuLeavesCorrect(command,0));

        //invalid symbols
        command = "SELECT * FROM language WHERE string == '£8.99';";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "SELECT * FROM language WHERE symbols == 'speech mark \"';";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
        command = "SELECT * FROM language WHERE symbols == 'symbol ¬';";
        assertTrue(parseTreeNuLeavesCorrect(command, 0));
    }

    @Test
    public void testDatabaseHandler(){
        String storageFolderPath;
        String fileSep = File.separator;

        //create the directory - repeat of code from DBServer
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }

        DatabaseHandler databaseHandler = new DatabaseHandler(storageFolderPath);
        String command, response;
        String tagOK = "[OK]";
        String tagError = "[ERROR]";

        //CREATE DATABASE TEST
        String databaseName = "testdatabase";
        String databasePath = storageFolderPath + fileSep + databaseName;
        File databaseFile = new File(databasePath);
        //delete database before doing create test
        databaseHandler.deleteDatabaseDir(databaseName);
        command = "create database testdatabase;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(databaseFile.exists());

        //CREATE TEST - PRE-EXISTING DATABASE
        command = "create database testdatabase;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("already exists"));

        //USE TEST - NON-EXISTENT DATABASE
        command = "use missingdatabase;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("not exist"));

        //USE TEST
        command = "use testdatabase;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        //DROP DATABASE - NON-EXISTENT DATABASE
        command = "drop database missingdatabase;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("check database exists"));

        //DROP DATABASE TEST
        command = "drop database testdatabase;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(!databaseFile.exists());

        //CREATE, USE AND DROP TEST WITH MIXED CASE
        command = "create database TESTdataBASE;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(databaseFile.exists());
        command = "use TeStDaTaBaSe;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        command = "drop database testDATAbase;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(!databaseFile.exists());

        command = "create database testdatabase;";
        databaseHandler.respondToCommand(command);
        command = "use testdatabase;";
        databaseHandler.respondToCommand(command);

        //CREATE TABLE TEST
        command = "create table cars;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        File carsFile = new File(databasePath + fileSep + "cars.tab");
        assert(carsFile.exists());

        //CREATE TABLE - ALREADY EXISTS TEST
        command = "create table CARS;"; //also works with uppercase
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("already exists"));

        //DROP TABLE TEST - DOESN'T EXIST
        command = "drop table birds;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("not exist"));

        //DROP TABLE TEST
        command = "drop table cars;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        //CREATE AND DROP TABLE - CASE INSENSITIVE
        command = "create table CaRs;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        command = "drop table cArS;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        command = "create table cars;";
        databaseHandler.respondToCommand(command);

        //ADDING COLUMNS TO EMPTY DATABASE
        command = "alter table cars add model;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        command = "select * from CARS;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.split("\\s+").length == 3);
        command = "alter table cars add topSpeed;";
        databaseHandler.respondToCommand(command);
        command = "select * from CARS;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.split("\\s+").length == 4);

        //CHECKING PRESERVATION OF CASE IN COLUMNS
        assertTrue(response.split("\\s+")[3].equals("topSpeed"));

        //CHECKING CASE INSENSITIVE SELECT
        command = "select TOPSPEED from CARS;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.split("\\s+")[1].equalsIgnoreCase("topSpeed"));

        //INSERTING VALUES
        command = "alter table cars add 0to60;";
        databaseHandler.respondToCommand(command);
        command = "alter table cars add colour;";
        databaseHandler.respondToCommand(command);
        command = "alter table cars add serialCode;";
        databaseHandler.respondToCommand(command);
        command = "insert into cars values ('Ferrari Enzo', 211, 3.1, 'Fire Red', '$%^ ***');";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        command = "insert into cars values ('Pagani Zonda', 230, 2.2, 'Silver', '$$* (!)');";
        databaseHandler.respondToCommand(command);
        command = "insert into cars values ('Lamborghini Diablo', 197, 3.4, 'Yellow', '<>; {[}');";
        databaseHandler.respondToCommand(command);
        command = "insert into cars values ('Porsche 911', 181, 3.9, 'Black', '##- \\?/');";
        databaseHandler.respondToCommand(command);
        command = "select * from CARS;";
        response = databaseHandler.respondToCommand(command);
        //System.out.println(response);
        assertTrue(response.split("\\s+").length == 40);

        //ADD COLUMN TO NON-EMPTY DATABASE
        command = "alter table cars add owner;";
        databaseHandler.respondToCommand(command);
        command = "select * from CARS;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.split("\\s+").length == 41);

        //CHECKING NULL VALUES
        command = "select model from CARS where owner == null;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.split("\\s+").length == 10);

        //CHECKING SELECT PRINTS IN CORRECT ORDER
        command = "select COLOUR, TOPSPEED, MODEL from CARS;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.split("\\s+")[1].equals("colour"));
        assertTrue(response.split("\\s+")[2].equals("topSpeed"));
        assertTrue(response.split("\\s+")[3].equals("model"));

        //CHECKING SELECT COMMANDS
        command = "select * from CARS where topspeed > 200;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.split("\\s+").length == 25);

        //AMBIGUOUS SELECT COMMAND - CHECK DOES NOT CRASH
        command = "select * from CARS where topspeed > 200 and colour == 'Silver' or colour == 'Yellow';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        //COMPOUND CONDITION FOR SELECT
        command = "select model, topspeed from CARS where (topspeed > 200 and colour == 'Silver') or colour == 'Yellow';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains("Zonda"));
        assertTrue(response.contains("Diablo"));

        //MORE COMPLICATED SELECT STATEMENT
        command = "select * from CARS where 0to60<3 or(model like 'Porsche' and colour=='Black')or(topspeed>200 and 0to60<3.5);";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains("Enzo"));
        assertTrue(response.contains("Zonda"));
        assertTrue(response.contains("911"));

        //CHECK RESERVED WORD USAGE
        command = "alter table cars add select;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("reserved"));

        //NONSENSICAL COMPARISON RETURNS NO DATA
        command = "select colour, model from cars where colour > 9000;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(response.split("\\s+").length == 3);

        //TRYING TO DROP NON-EXISTENT COLUMN
        command = "alter table cars drop engineSize;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("does not exist"));

        //TRYING TO DROP ID
        command = "alter table cars drop id;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("Cannot delete ID"));

        //TRYING TO ADD EXISTING COLUMN
        command = "alter table cars add TOPSPEED;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("Column already exists"));

        //TRYING TO INSERT WITH TOO MANY VALUES
        command = "insert into cars values (123, 179, 4.4, 'Green', '123 ###', 'Bob James', 'extraValue');";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("Check number of columns"));

        //TRYING TO INSERT WITH TOO FEW VALUES
        command = "insert into cars values (123, 179, 4.4, 'Green', '123 ###');";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("Check number of columns"));

        //PERFORMING SELECT WITH NON-EXISTENT ATTRIBUTE
        command = "select interiorTrim from cars;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("Not all specified attributes exist"));

        //PERFORMING UPDATE WITH NON-EXISTENT ATTRIBUTE
        command = "update cars set topSpeed = 250 where engineSize == '5000cc';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("Not all specified attributes exist"));

        //PERFORMING DELETE WITH NON-EXISTENT ATTRIBUTE
        command = "delete from cars where weight > 2000;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("Not all specified attributes exist"));

        //TRYING TO CHANGE ID WITH "UPDATE"
        command = "update cars set id = 1, owner = 'Bill' where topSpeed > 150;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("ID column cannot be changed"));

        //SELECT REPEATED SELECTION
        command = "SELECT topSpeed, topSpeed, topSpeed from cars;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        //UPDATE COMMAND BASIC USAGE
        command = "update cars set owner = 'Andy' where model == 'Ferrari Enzo';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        command = "update cars set owner = 'Brad' where model == 'Pagani Zonda';";
        databaseHandler.respondToCommand(command);
        command = "update cars set owner = 'Brad' where model like 'ini';";
        databaseHandler.respondToCommand(command);
        command = "update cars set owner = 'Charles' where model like '911';";
        databaseHandler.respondToCommand(command);

        //UPDATE COMMAND WITH LARGER NAME VALUE LIST
        command = "update cars set owner = 'Cliff', topSpeed = 160, colour = 'Pink', 0TO60 = 5 where model like '911';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        //UPDATE WITH REPETITION IN NAME VALUE LIST
        command = "update cars set owner = 'Amy', owner = 'Amy', owner = 'Amy' where model like 'Ferrari';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        //VALID QUERY BUT WHICH GENERATES NO DATA
        command = "select * from cars where topspeed > 1000;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        //VALID COMPARISON WITH STRINGS
        command = "select * from cars where owner > 'Arun';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(!response.contains("Amy"));

        //CHECKING STRING COMPARISON CASE INSENSITIVE
        command = "select * from cars where owner > 'arun';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(!response.contains("Amy"));
        command = "select * from cars where owner == 'amy';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(response.contains("Amy"));

        //CHECK "LIKE" IS CASE SENSITIVE
        command = "select * from cars where model like 'porsche';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(!response.contains("Porsche"));

        //JOIN TEST
        command = "create table owners (owner, nationality, age, occupation);";
        databaseHandler.respondToCommand(command);
        command = "insert into owners values ('Amy', 'UK', 25, 'doctor');";
        databaseHandler.respondToCommand(command);
        command = "insert into owners values ('Brad', 'US', 35, 'engineer');";
        databaseHandler.respondToCommand(command);
        command = "insert into owners values ('Cliff', 'Germany', 50, 'journalist');";
        databaseHandler.respondToCommand(command);
        command = "join owners and cars on owner and owner;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
        assertTrue(response.split("\\s+").length == 55);

        //JOIN WITH MISSING TABLE
        command = "join owners and houses on owner and owner;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("Check both tables exist"));

        //JOIN WITH MISSING ATTRIBUTE
        command = "join owners and cars on city and owner;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("Non-existent attribute"));

        //JOIN ON TWO EMPTY TABLES
        command = "create table birds (name, weight, food);";
        databaseHandler.respondToCommand(command);
        command = "create table mammals (name, weight, food);";
        databaseHandler.respondToCommand(command);
        command = "join birds and mammals on food and food;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        //DELETE FROM TEST
        command = "delete from owners where owner == 'cliff';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));

        //ID UPDATE TEST
        //force table "owners" to be re-loaded from file
        command = "select * from cars;";
        databaseHandler.respondToCommand(command);
        command = "insert into owners values ('Dave', 'France', 65, 'accountant');";
        databaseHandler.respondToCommand(command);
        command = "select id from owners where owner == 'Dave';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains("4"));

        //CHECK MAXIMUM NUMBER OF TOKENS IN QUERY
        command = "select * from cars where ";
        for(int count = 0; count < 500; count++)
            command += "owner == 'Amy' and ";
        command += "owner == 'Amy';";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("exceeds limit"));

        //CHECK MAXIMUM NUMBER OF ENTRIES IN TABLE
        command = "insert into owners values ('Dave', 'France', 65, 'accountant');";
        for(int count = 0; count < 1000; count++)
            databaseHandler.respondToCommand(command);
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("entry limit exceeded"));

        //CHECK MAXIMUM NUMBER OF ATTRIBUTES IN TABLE
        for(int count = 0; count < 110; count++){
            command = "alter table cars add stuff" + Integer.toString(count) + ";";
            databaseHandler.respondToCommand(command);
        }
        command = "alter table cars add stuff" + Integer.toString(110) + ";";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagError));
        assertTrue(response.contains("attribute limit exceeded"));

        //FINALLY - REMOVE DATABASEs
        command = "drop database testdatabase;";
        response = databaseHandler.respondToCommand(command);
        assertTrue(response.contains(tagOK));
    }

    public static boolean parseTreeNuLeavesCorrect(String command, int correctNuLeaves){
        //get the tokens and parse tree
        List<Token> tokensForTest = Tokeniser.extractTokens(command);
        Node parseTree = SQLSimpGrammar.command.parseTokens(tokensForTest, 0);

        //assuming a valid parse tree generated, check number of leaf nodes
        if(parseTree != null)
            return Node.getNumberOfLeaves(parseTree) == correctNuLeaves;
        //otherwise a failed parse corresponds to the value 0
        else
            return correctNuLeaves == 0;
    }

    public static List<Token> strToTokens(String commandStr){
        List<String> tokenStrings;
        tokenStrings = new ArrayList<String>(Arrays.asList(commandStr.split(" ")));
        List<Token> tokens = new ArrayList<Token>();
        for(String tokStr : tokenStrings)
            tokens.add(new Token(tokStr));
        return tokens;
    }

    public static boolean compareTokens(List<Token> tokens1, List<Token> tokens2){
        //same lengths
        if(tokens1.size() != tokens2.size())
            return false;

        //same tokens
        for(int tokCnt = 0; tokCnt < tokens1.size(); tokCnt++)
            if(!tokens1.get(tokCnt).tokenText.equalsIgnoreCase(tokens2.get(tokCnt).tokenText))
                return false;

        return true;
    }
}