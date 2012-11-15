package edu.upenn.cis555.webserver.support;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class Handler extends DefaultHandler {
	private int state;
	private String servletName;
	private String paramName;
	private String displayName;
	private int maxInactive;
	private Map<String, String> urlPatterns;
	private Map<String, String> servlets;
	private Map<String, String> contextParams;
	private Map<String, Map<String, String>> servletParams;

	public Handler() {
		urlPatterns = new HashMap<String, String>();
		servlets = new HashMap<String, String>();
		contextParams = new HashMap<String, String>();
		servletParams = new HashMap<String, Map<String, String>>();
	}
	
	public int getMaxInactive() {
		return maxInactive;
	}

	public String getDisplayName() {
		return displayName;
	}
	
	public Map<String, String> getServlets() {
		return servlets;
	}
	public Map<String, String> getContextParams() {
		return contextParams;
	}
	
	public Map<String, Map<String, String>> getServletParams() {
		return servletParams;
	}

	public Map<String, String> getUrlPatterns() {
		return urlPatterns;
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (qName.equals("servlet-name")) {
			state = 1;
		} else if (qName.equals("servlet-class")) {
			state = 2;
		} else if (qName.equals("context-param")) {
			state = 3;
		} else if (qName.equals("init-param")) {
			state = 4;
		} else if (qName.equals("param-name")) {
			state = (state == 3) ? 10 : 20;
		} else if (qName.equals("param-value")) {
			state = (state == 10) ? 11 : 21;
		} else if (qName.equals("url-pattern")) {
			state = 5;
		} else if (qName.equals("display-name")) {
			state = 6;
		} else if (qName.equals("session-timeout")) {
			state = 7;
		} else if (qName.equals("description")) {
			state = 8;
		} else {
			state = 9;
		}
	}
	public void characters(char[] ch, int start, int length) {
		String value = new String(ch, start, length);
		if (state == 1) {
			servletName = value;
			state = 0;
		} else if (state == 2) {
			servlets.put(servletName, value);
			state = 0;
		} else if (state == 10 || state == 20) {
			paramName = value;
		} else if (state == 11) {
			if (paramName == null) {
				System.err.println("Context parameter value '" + value + "' without name");
				System.exit(-1);
			}
			contextParams.put(paramName, value);
			paramName = null;
			state = 0;
		} else if (state == 21) {
			if (paramName == null) {
				System.err.println("Servlet parameter value '" + value + "' without name");
				System.exit(-1);
			}
			Map<String,String> p = servletParams.get(servletName);
			if (p == null) {
				p = new HashMap<String,String>();
				servletParams.put(servletName, p);
			}
			p.put(paramName, value);
			paramName = null;
			state = 0;
		} else if (state == 5) {
			urlPatterns.put(value, servletName);
		} else if (state == 6) {
			displayName = value;
		} else if (state == 7) {
			maxInactive = Integer.parseInt(value) * 60;
		}
	}}