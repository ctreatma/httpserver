package edu.upenn.cis555.webserver;

public class ShutdownThread extends Thread {
    
    public ShutdownThread() {
    }
    
    public void run() {
        System.out.println("Shutting down.");
        Httpserver.getServer().stopServer();
        Httpserver.getThreadPool().shutDown();
        if (Httpserver.getEngine() != null) {
            Httpserver.getEngine().stop();
        }
        System.out.println("Done.");
    }
}
