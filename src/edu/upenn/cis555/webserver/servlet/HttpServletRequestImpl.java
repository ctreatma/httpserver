package edu.upenn.cis555.webserver.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.upenn.cis555.webserver.support.HttpHandler;

public class HttpServletRequestImpl implements HttpServletRequest {     
    private Map<String,List<String>> params;
    private Map<String,Object> props;
    private Map<String,List<String>> headers;
    private List<Cookie> cookies = new ArrayList<Cookie>();
    private HttpSessionImpl session;
    private String encoding = "ISO-8859-1";
    private boolean secure = false;
    private String contentType;
    private Locale locale;
    private InputStream input;
    private BufferedReader reader;
    private String path;
    private HttpHandler handler;
    private ServletConfigImpl config;

    public HttpServletRequestImpl(InputStream input, HttpHandler handler, ServletConfigImpl config) {
    	headers = new HashMap<String, List<String>>();
    	params = new HashMap<String, List<String>>();
    	props = new HashMap<String,Object>();
    	
    	this.input = input;
        this.handler = handler;
        this.config = config;

    	setHeaders(handler.getHeaders());
    	setCookies(handler.getCookies());
    	loadSession();
    }
    
    public String getAuthType() {
        return "BASIC";
    }

    public String getContextPath() {
        return "";
    }

    public Cookie[] getCookies() {
        return cookies.toArray(new Cookie[cookies.size()]);
    }

    public long getDateHeader(String name) {
        name = name.toLowerCase();
        if (headers.containsKey(name)) {
            try {
            	return parseHeaderWithDateFormat(name, "EEE, d MMM yyyy HH:mm:ss z");
            } catch (ParseException ex) {}
        	try {
                return parseHeaderWithDateFormat(name, "EEEE, d-MMM-yy HH:mm:ss z");
        	} catch (ParseException ex) {}
        	try {
                return parseHeaderWithDateFormat(name, "EEE MMM d HH:mm:ss yyyy");
        	} catch (ParseException ex) {
                throw new IllegalArgumentException("Header " + name + " could not be converted to Date.");
            }
            
        }
        return -1;
    }

    @Override
    public String getHeader(String name) {
        name = name.toLowerCase();
        if (headers.containsKey(name)) {
            return headers.get(name).get(0);
        }
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String arg0) {
        arg0 = arg0.toLowerCase();
        return Collections.enumeration(headers.get(arg0));
    }

    @Override
    public int getIntHeader(String name) {
        name = name.toLowerCase();
        if (headers.containsKey(name)) {
            return Integer.parseInt(headers.get(name).get(0));
        }
        return -1;
    }

    @Override
    public String getMethod() {
        return handler.getMethod();
    }

    @Override
	public String getPathInfo() {
		String extra = null;
		if (path != null) {
			 extra = path.replace(getContextPath(), "");
			 if (extra.indexOf("/", 1) > -1) {
				 extra = extra.substring(extra.indexOf("/", 1));
			 } else {
				 extra = null;
			 }
		}
		return extra;
	}

    @Override
    public String getPathTranslated() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
	public String getQueryString() {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, List<String>> entry : params.entrySet()) {
			for (String value : entry.getValue()) {
				builder.append("&").append(entry.getKey()).append("=").append(value);
			}
		}
		return builder.toString().replaceFirst("&", "");
	}

    @Override
    public String getRemoteUser() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getRequestURI() {
        return this.getContextPath() + this.getServletPath() + this.getPathInfo();
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        url.append(this.getScheme());
        url.append("://");
        url.append(this.getServerName());
        url.append(":");
        url.append(this.getServerPort());
        url.append(this.getRequestURI());
        return url;
    }

    @Override
    public String getRequestedSessionId() {
        if (hasSession()) {
            return session.getId();
        }
        return null;
    }

    @Override
	public String getServletPath() {
		String servletPath = null;
		if (path != null) {
			servletPath = path.replace(getContextPath(), "");
			if (servletPath.indexOf("/", 1) > -1) {
				servletPath = servletPath.substring(0, servletPath.indexOf("/", 1));
			}
		}
		return servletPath;
	}

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create) {
       if (!hasSession()) {
    	   session = create ? new HttpSessionImpl(config) : null;
       }
       return session;
    }

    @Override
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return hasSession() && !session.isNew();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return hasSession();
    }

    @Override
    public boolean isUserInRole(String role) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object getAttribute(String attribute) {
        return props.get(attribute);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(props.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    @Override
    public int getContentLength() {
        return -1;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getLocalAddr() {
        return handler.getLocalHostAddress();
    }

    @Override
    public String getLocalName() {
        return handler.getLocalHostName();
    }

    @Override
    public int getLocalPort() {
        return handler.getLocalPort();
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getParameter(String key) {
        if (params.containsKey(key)) {
            return params.get(key).get(0);
        }
        return null;
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        return params;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(params.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        if (params.containsKey(name)) {
            List<String> values = params.get(name);
            return values.toArray(new String[values.size()]);
        }
        return null;
    }

    @Override
    public String getProtocol() {
        return handler.getProtocol();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(input, encoding));
            while (reader.readLine() != null && reader.readLine().trim().length() != 0) {
                // Skip over the headers to the request body
            }
        }
        return reader;
    }

    @Override
    public String getRealPath(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getRemoteAddr() {
        return handler.getRemoteHostAddress();
    }

    @Override
    public String getRemoteHost() {
        return handler.getRemoteHostName();
    }

    @Override
    public int getRemotePort() {
        return handler.getRemotePort();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return handler.getLocalHostName();
    }

    @Override
    public int getServerPort() {
        return handler.getLocalPort();
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public void removeAttribute(String name) {
        props.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        props.put(name, value);
    }

    @Override
    public void setCharacterEncoding(String encoding)
            throws UnsupportedEncodingException {
        this.encoding = encoding;
    }
    
    public void addParameter(String key, String value) {
        try {
            key = URLDecoder.decode(key, encoding);
            value = URLDecoder.decode(value, encoding);
        }
        catch (UnsupportedEncodingException ex) {}
        List<String> values = params.get(key);
        if (values == null) {
            values = new ArrayList<String>();
            params.put(key, values);
        }
        values.add(value);
    }
    
    boolean hasSession() {
        return ((session != null) && session.isValid());
    }

    private void setCookies(List<Cookie> cookies) {
    	this.cookies.addAll(cookies);
    }

    private void setHeaders(Map<String, List<String>> headers) {
    	this.headers.putAll(headers);
    }
    
    private void loadSession() {
    	for (Cookie cookie: cookies) {
    		if (cookie.getName().equalsIgnoreCase("httpSessionId")) {
    			session = config.getServletContext().getSession(cookie.getValue());
    		}
    	}
    }
    private long parseHeaderWithDateFormat(String name, String format)
    		throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String headerValue = headers.get(name).get(0);
        return dateFormat.parse(headerValue).getTime();
    }
}
