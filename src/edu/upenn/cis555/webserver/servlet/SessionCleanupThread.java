package edu.upenn.cis555.webserver.servlet;

import edu.upenn.cis555.webserver.Logger;

public class SessionCleanupThread extends Thread {
	ServletEngine engine;
	
    public SessionCleanupThread(ServletEngine engine) {
    	this.engine = engine;
    }
    
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000); // TODO: Figure out how to get at the maxInactive setting.
                engine.removeInvalidSessions();
            }
            catch (Exception ex) {
                Logger.logError(ex, null);
                System.exit(1);
            }
        }
    }
}
