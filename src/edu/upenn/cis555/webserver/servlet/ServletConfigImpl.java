package edu.upenn.cis555.webserver.servlet;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletConfig;


public class ServletConfigImpl implements ServletConfig {
    private String name;
    private ServletContextImpl context;
    private Map<String, String> initParams;

    public ServletConfigImpl(String name, ServletContextImpl context) {
        this.name = name;
        this.context = context;
        initParams = new HashMap<String, String>();
    }

    public String getInitParameter(String name) {
        return initParams.get(name);
    }

    public Enumeration<String> getInitParameterNames() {
        Set<String> keys = initParams.keySet();
        Vector<String> atts = new Vector<String>(keys);
        return atts.elements();
    }

    public ServletContextImpl getServletContext() {
        return context;
    }

    public String getServletName() {
        return name;
    }

    public void setInitParam(String name, String value) {
        initParams.put(name, value);
    }
}
