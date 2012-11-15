package edu.upenn.cis555.webserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.upenn.cis555.webserver.servlet.ServletEngine;

public class Httpserver {
    private static final int poolSize = 10;
    private static boolean quit = false;
    private static int port;
    private static File rootDir;
    private static RequestQueue requests;
    private static ThreadPool requestHandlers;
    private static ServerDaemon server;
    private static ServletEngine engine;

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
        	System.err.println("httpserver <port> <server root> [<path to web.xml>]");
            System.exit(1);
        }
        else {
            try {
                port = Integer.parseInt(args[0]);
                String rootPath = args[1];
                rootDir = new File(rootPath);
                if (rootDir.isDirectory() && rootDir.isAbsolute()) {
                    requests = new RequestQueue();
                	
                    try {
                    	server = new ServerDaemon(port);
                    }
                    catch (IOException ex) {
                    	Logger.logError("The server could not listen on port " + port);
                    	System.exit(1);
                    }
                    
                    if (args.length == 3) {
                        engine = new ServletEngine(args[2]);
                    }

                    server.start();

                    requestHandlers = new ThreadPool(poolSize);
                    requestHandlers.start();

                    Runtime.getRuntime().addShutdownHook(new ShutdownThread());
                    
                    System.out.println();
                    System.out.println("-----------------------------------------------");
                    System.out.println("Welcome to Charles Treatman's CIS555 Web Server");
                    System.out.println("-----------------------------------------------");
                    System.out.println();

                    InputStreamReader converter = new InputStreamReader(System.in);
                    BufferedReader in = new BufferedReader(converter);
                    
                    while (!quit) {
                        System.out.println("Choose a menu option:");
                        if (engine != null) {
                        	if (engine.isRunning())
                                System.out.println("- 'stop' servlet engine");
                        	else
                        		System.out.println("- 'start' servlet engine");
                        }
                        System.out.println("- 'error' log");
                        System.out.println("- 'status' of request threads");
                        System.out.println("- 'quit' httpserver");

                        String option = "";
                        option = in.readLine();

                        if (engine != null) {
                            if (option.equals("start")) {
                            	engine.start();
                            }
                            else if (option.equals("stop")) {
                            	engine.stop();
                            }
                        }
                        
                        if (option.equalsIgnoreCase("error")) {
                            Logger.showErrorLog();
                        }
                        else if (option.equalsIgnoreCase("status")) {
                            requestHandlers.status();
                        }
                        else if (option.equalsIgnoreCase("quit")) {
                            quit = true;
                        }

                        System.out.println();
                        System.out.println();
                    }
                }
                else {
                    Logger.logError("Bad root path supplied: " + rootPath);
                }
            }
            catch (Exception ex) {
                Logger.logError(ex, null);
                System.exit(1);
            }
        }
        System.exit(0);
    }

    public static ThreadPool getThreadPool() {
    	return requestHandlers;
    }
    
    public static RequestQueue getRequestQueue() {
    	return requests;
    }
    
    public static ServerDaemon getServer() {
    	return server;
    }
    
	public static ServletEngine getEngine() {
		return engine;
	}

	public static File getDocumentRoot() {
		return rootDir;
	}
}
