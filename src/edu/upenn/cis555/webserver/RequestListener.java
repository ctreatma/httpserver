package edu.upenn.cis555.webserver;

import java.io.*;
import java.net.Socket;
import edu.upenn.cis555.webserver.support.HttpHandler;

public class RequestListener extends Thread {
    private ThreadPool owner;

    public RequestListener(ThreadPool owner) {
        this.owner = owner;
    }

    public void run() {
        while (true) {
            Socket socket = Httpserver.getRequestQueue().getRequest();
            openSocket(socket);
        }
    }

    private void openSocket(Socket socket) {
        owner.startRequest();
        try {
            HttpHandler handler = new HttpHandler(socket);
            handler.handleRequest();
        }
        catch (IOException ex) {
            Logger.logError(ex, "Thread '" + this.getName() + "' had an error opening the socket streams.");
        }
        finally {
            try {
                socket.close();
            }
            catch (IOException ex) {
                Logger.logError(ex, "Thread '" + this.getName() + "' had an error closing the socket.");
            }
            owner.finishRequest();
        }
    }
}
