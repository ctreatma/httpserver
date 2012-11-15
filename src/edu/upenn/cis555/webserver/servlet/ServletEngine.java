package edu.upenn.cis555.webserver.servlet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import edu.upenn.cis555.webserver.Logger;
import edu.upenn.cis555.webserver.support.Handler;

public class ServletEngine {
	private ServletConfigImpl config;
	private ServletContextImpl context;
	private Map<String, HttpServlet> servlets;
    private SessionCleanupThread cleanup;
    private boolean running;
    private File directory;
    private Handler handler;
	
    public ServletEngine(String webDotXml) throws Exception {
    	parseWebDotXml(webDotXml);
    }
    
    public void start() throws Exception {
        cleanup = new SessionCleanupThread(this);
        cleanup.setDaemon(true);
        cleanup.start();
        
        createContext(handler, directory);
        createServlets(handler);
        running = true;
    }
    
    public void stop() {
    	if (servlets != null) {
            for(HttpServlet servlet : servlets.values()) {
                servlet.destroy();
            }
            servlets = null;
        }
    	running = false;
    }
    
    public boolean isRunning() {
    	return running;
    }
    
    public void parseWebDotXml(String webDotXml) throws Exception {
        handler = new Handler();
        File file = new File(webDotXml);
        if (file.exists() == false) {
            Logger.logError("error: cannot find '" + file.getPath() + "'");
            return;
        }
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(file, handler);

        try {
            // I'm making the assumption here that the webapp follows the usual conventions
            // for layout, so web.xml is located in {webappDir}/WEB-INF/web.xml
            directory = file.getParentFile().getParentFile();
        }
        catch (Exception ex) {
            Logger.logError(ex, null);
        }
    }

    
    private void createServlets(Handler h) throws Exception {
    	if (servlets == null) {
    		servlets = new HashMap<String,HttpServlet>();
    		for (String name : h.getServlets().keySet()) {
    			config = new ServletConfigImpl(name, context);
    			config.setInitParam("session-timeout", Integer.toString(h.getMaxInactive()));
    			String className = h.getServlets().get(name);
    			@SuppressWarnings("rawtypes")
				Class servletClass = Class.forName(className);
    			HttpServlet servlet = (HttpServlet) servletClass.newInstance();
    			Map<String,String> servletParams = h.getServletParams().get(name);
    			if (servletParams != null) {
    				for (String param : servletParams.keySet()) {
    					config.setInitParam(param, servletParams.get(param));
    				}
    			}
    			servlet.init(config);
    			servlets.put(name, servlet);
    		}
    	}
    }
    
    private void createContext(Handler h, File webappDir) {
        context = new ServletContextImpl(h.getDisplayName(), webappDir);
        context.setAttribute("ServletContext", h.getDisplayName());
        for (String param : h.getContextParams().keySet()) {
            context.setInitParam(param, h.getContextParams().get(param));
        }
    }

	public void removeInvalidSessions() {
		context.removeInvalidSessions();
	}

	public Map<String, String> getUrlPatterns() {
		return handler.getUrlPatterns();
	}

	public Map<String, HttpServlet> getServlets() {
		return servlets;
	}

	public HttpServlet getServlet(String path) {
		HttpServlet servlet = null;
        for (Map.Entry<String, String> servletLookup : handler.getUrlPatterns().entrySet()) {
            String urlRegexp = "(" + servletLookup.getKey() + ")(/.*)?";
            Pattern pattern = Pattern.compile(urlRegexp);
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                servlet = servlets.get(servletLookup.getValue());
            }
        }
        return servlet;
	}
	
	public ServletContextImpl getServletContext() {
		return context;
	}
}
