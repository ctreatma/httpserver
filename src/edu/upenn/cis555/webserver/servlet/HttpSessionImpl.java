package edu.upenn.cis555.webserver.servlet;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import edu.upenn.cis555.webserver.Httpserver;

@SuppressWarnings("deprecation")
public class HttpSessionImpl implements HttpSession {
    private Map<String, Object> attributes;
    private UUID id;
    private Date creationTime, lastAccessedTime;
    private boolean isNew = true, isValid = true;
    private int maxInactiveInterval;
    private ServletConfigImpl config;

    public HttpSessionImpl(ServletConfigImpl config) {
    	attributes = new HashMap<String, Object>();
    	creationTime = new Date();
    	setLastAccessedTime(creationTime);
    	id = UUID.randomUUID();
    	
    	this.config = config;
        this.config.getServletContext().addSession(this);
        String ts = this.config.getInitParameter("session-timeout");
        int timeout = Integer.parseInt(ts);
        this.setMaxInactiveInterval(timeout);
    }
    
    @Override
    public Object getAttribute(String name) {
        if (isValid) {
            return attributes.get(name);
        }
        else {
            throw new IllegalStateException("The session " + id + " is invalid.");
        }
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        if (isValid) {
            return Collections.enumeration(attributes.keySet());
        }
        else {
            throw new IllegalStateException("The session " + id + " is invalid.");
        }
    }

    @Override
    public long getCreationTime() {
        if (isValid) {
            return creationTime.getTime();
        }
        else {
            throw new IllegalStateException("The session " + id + " is invalid.");
        }
    }

    @Override
    public String getId() {
        if (isValid) {
            return id.toString();
        }
        else {
            throw new IllegalStateException("The session " + id + " is invalid.");
        }
    }

    @Override
    public long getLastAccessedTime() {
        if (isValid) {
            return lastAccessedTime.getTime();
        }
        else {
            throw new IllegalStateException("The session " + id + " is invalid.");
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public ServletContext getServletContext() {
        return Httpserver.getEngine().getServletContext();
    }

	public HttpSessionContext getSessionContext() {
        return null;
    }

    public Object getValue(String name) {
        return getAttribute(name);
    }

    public String[] getValueNames() {
        if (isValid) {
            return attributes.keySet().toArray(new String[attributes.keySet().size()]);
        }
        else {
            throw new IllegalStateException("The session " + id + " is invalid.");
        }
    }

    public void invalidate() {
        config.getServletContext().removeSession(id.toString());
        isValid = false;
    }

    @Override
    public boolean isNew() {
        if (isValid) {
            return isNew;
        }
        else {
            throw new IllegalStateException("The session " + id + " is invalid.");
        }
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        if (isValid) {
            attributes.remove(name);
        }
        else {
            throw new IllegalStateException("The session " + id + " is invalid.");
        }
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (isValid) {
            attributes.put(name, value);
        }
        else {
            throw new IllegalStateException("The session " + id + " is invalid.");
        }
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    boolean isValid() {
        return isValid;
    }

    void setLastAccessedTime(Date lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }
    
    void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}
