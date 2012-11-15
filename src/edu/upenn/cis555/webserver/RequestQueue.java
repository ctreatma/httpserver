package edu.upenn.cis555.webserver;

import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class RequestQueue {
    private static Queue<Socket> requests;

    public RequestQueue() {
        requests = new LinkedList<Socket>();
    }

    synchronized public Socket getRequest() {
        while (requests.isEmpty()) {
            try {
                wait();
            }
            catch (InterruptedException ex) {
                //Ignore.
            }
        }
        return requests.remove();
    }


    synchronized public void addRequest(Socket request) {
        requests.add(request);
        notifyAll();
    }
    
    synchronized public boolean hasMoreRequests() {
        return !requests.isEmpty();
    }
}
