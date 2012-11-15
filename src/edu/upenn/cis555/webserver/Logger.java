package edu.upenn.cis555.webserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

public class Logger {
    private static final String logFileName = "error.log";
    private static File logFile;
    private static PrintStream logFileWriter;
    
    public static void logError(String msg) {
        try {
            checkLogFileExists();
            logFileWriter.println("<<<<<<<<<<<");
            logFileWriter.println(new Date());
            logFileWriter.println(msg);
            logFileWriter.println(">>>>>>>>>>>");
            logFileWriter.println();
            logFileWriter.flush();
        }
        catch (IOException e) {
            // Can't log this error, so dump stack trace to stderr?
            e.printStackTrace();
        }
        
    }
    
    public static void logError(Exception ex, String msg) {
        try {
            checkLogFileExists();
            logFileWriter.println("<<<<<<<<<<<");
            logFileWriter.println(new Date());
            if (msg != null) {
                logFileWriter.println(msg);
            }
            ex.printStackTrace(logFileWriter);
            logFileWriter.println(">>>>>>>>>>>");
            logFileWriter.println();
            logFileWriter.flush();
        }
        catch (IOException e) {
            // Can't log this error, so dump stack trace to stderr?
            e.printStackTrace();
            System.err.println("Encountered while logging: ");
            ex.printStackTrace();
        }
        
    }
    
    public static void showErrorLog() {
        try {
            checkLogFileExists();
            BufferedReader logReader = new BufferedReader(new FileReader(logFile));

            String line;
            System.out.println();
            while ((line = logReader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println();
        	logReader.close();
        }
        catch (Exception ex) {
            // Can't log this error, so dump stack trace to stderr?
            ex.printStackTrace();
        }
        finally {
        }
    }
    
    private static void checkLogFileExists() throws IOException {
        if (logFile == null) {
            logFile = new File(logFileName);
        }
        if (logFile.createNewFile() || logFileWriter == null) {
            logFileWriter = new PrintStream(logFile);            
        }
        
    }
}
