package edu.upenn.cis555.webserver.support;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class HttpUtils {
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	public static final String DEFAULT_CONTENT_TYPE = "text/html";
	
	public static Map<Integer, String> STATUS_CODES;
	static {
		STATUS_CODES = new HashMap<Integer, String>();
		STATUS_CODES.put(HttpServletResponse.SC_CONTINUE, "Continue");
		STATUS_CODES.put(HttpServletResponse.SC_OK, "OK");
		STATUS_CODES.put(HttpServletResponse.SC_MOVED_TEMPORARILY, "Moved Temporarily");
		STATUS_CODES.put(HttpServletResponse.SC_NOT_MODIFIED, "Not Modified");
		STATUS_CODES.put(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
		STATUS_CODES.put(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
		STATUS_CODES.put(HttpServletResponse.SC_NOT_FOUND, "Not Found");
		STATUS_CODES.put(HttpServletResponse.SC_PRECONDITION_FAILED, "Precondition Failed");
		STATUS_CODES.put(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
		STATUS_CODES.put(HttpServletResponse.SC_NOT_IMPLEMENTED, "Not Implemented");
	}
	
	public static String getStatusLine(String version, int sc) {
		return version + " " + sc + " " + STATUS_CODES.get(sc) + LINE_SEPARATOR;
	}
	

	public static final String HTTP_1_0 = "HTTP/1.0";
	public static final String HTTP_1_1 = "HTTP/1.1";
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_HEAD = "HEAD";
	public static final String INITIAL_REQUEST_LINE_PATTERN = 
		"(" + METHOD_GET + "|" + METHOD_POST + "|" + METHOD_HEAD + ")\\s+[^\\s]+\\s+(HTTP/)\\d\\.\\d";
	public static final String HEADER_HOST_PATTERN = "(Host:)\\s+[^\\s]+";
	public static final String HEADER_IF_MODIFIED_SINCE_PATTERN = "(If\\-Modified\\-Since:)\\s+";
	public static final String HEADER_IF_UNMODIFIED_SINCE_PATTERN = "(If\\-Unmodified\\-Since:)\\s+";
	public static final String HEADER_PERSISTENT_CONNECTION_PATTERN = "(Connection:)\\s+(Keep\\-Alive)";
	public static final String HEADER_COOKIE_PATTERN = "(Cookie:)\\s+([^\\s]+=[^\\s]+;\\s)*([^\\s]+=[^\\s]+)";
	public static final String HEADER_CONTENT_LENGTH_PATTERN = "(Content-Length:)\\s+\\d+";
	public static final String HEADER_PATTERN = "[^\\s]+:\\s+.+";
	public static final String MESSAGE_BODY_PATTERN = "(([^\\s]+=[^\\&]*)&*)+";
	public static final int UNKNOWN_CONTENT_LENGTH = -1;
}
