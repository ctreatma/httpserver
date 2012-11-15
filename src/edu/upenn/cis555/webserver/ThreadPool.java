package edu.upenn.cis555.webserver;

import java.util.ArrayList;
import java.util.List;

public class ThreadPool {
    private List<Thread> threads;
    private int activeThreads = 0;

    public ThreadPool(int size) {
        threads = new ArrayList<Thread>(size);
        for (int i = 0; i < size; ++i) {
            threads.add(new RequestListener(this));
        }
    }

    public void start() {
    	for (Thread thread : threads) {
    		thread.start();
    	}
    }
    synchronized public void shutDown() {
        try {
            while (activeThreads > 0) {
                wait();
            }
        }
        catch (InterruptedException ex) {
            // Ignore.
        }
    }

    synchronized protected void startRequest() {
        activeThreads++;
        //System.out.println("Active threads: " + activeThreads);
        notifyAll();
    }

    synchronized protected void finishRequest() {
        activeThreads--;
        //System.out.println("Active threads: " + activeThreads);
        notifyAll();
    }
    
    public void status() {
        for (Thread thread : threads) {
            System.out.println(thread.getName() + ": " + thread.getState());
        }
    }
}
