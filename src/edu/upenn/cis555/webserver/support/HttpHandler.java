package edu.upenn.cis555.webserver.support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;

import edu.upenn.cis555.webserver.Httpserver;
import edu.upenn.cis555.webserver.Logger;
import edu.upenn.cis555.webserver.servlet.HttpServletRequestImpl;
import edu.upenn.cis555.webserver.servlet.HttpServletResponseImpl;
import edu.upenn.cis555.webserver.servlet.ServletConfigImpl;
import edu.upenn.cis555.webserver.servlet.ServletEngine;

public class HttpHandler {
	private InputStream input;
	private BufferedReader reader;
	private OutputStream output;
	private BufferedWriter writer;
	private Socket socket;
	
	// Request-specific values
	private List<Cookie> cookies;
	private Date modifiedSince, unmodifiedSince;
	private Map<String, List<String>> headers;
	private boolean includesHost = false, validRequest = false;
	private String method, params, path, protocol;
	
	private static SimpleDateFormat format, format2, format3;

	static {
		format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
	
		format2 = new SimpleDateFormat("EEEE, d-MMM-yy HH:mm:ss z");
		format2.setTimeZone(TimeZone.getTimeZone("GMT"));
	
		format3 = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
		format3.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public HttpHandler(Socket socket) throws IOException {
		this.socket = socket;
		this.input = socket.getInputStream();
		this.output = socket.getOutputStream();
		this.reader = new BufferedReader(new InputStreamReader(this.input));
		this.writer = new BufferedWriter(new OutputStreamWriter(this.output));
	}

	private void processInitialLine(String line) throws IOException {
		String[] requestParts = line.split("\\s");
		method = requestParts[0];
		path = requestParts[1];
		protocol = requestParts[2];
		
		if (!method.equals(HttpUtils.METHOD_GET) && !method.equals(HttpUtils.METHOD_HEAD) &&
				!method.equals(HttpUtils.METHOD_POST)) {
			handleNotImplemented();
		}
		else {
			validRequest = true;
			if (protocol.equals(HttpUtils.HTTP_1_1)) {
				writer.write(protocol + " 100 Continue\r\n\r\n");
				writer.flush();
			}
		}
	}
	
	private void processHeaderLine(String line) {
		String headerName = line.substring(0, line.indexOf(":")).trim();
		String headerValue = line.substring(line.indexOf(":") + 1).trim();
		if (protocol.equals(HttpUtils.HTTP_1_1)) {
			if (Pattern.matches(HttpUtils.HEADER_HOST_PATTERN, line)) {
				includesHost = true;
			}
			else if (Pattern.matches(HttpUtils.HEADER_IF_MODIFIED_SINCE_PATTERN, line)) {
				modifiedSince = parseDateHeader(headerValue);
			}
			else if (Pattern.matches(HttpUtils.HEADER_IF_UNMODIFIED_SINCE_PATTERN, line)) {
				unmodifiedSince = parseDateHeader(headerValue);
			}
			else if (Pattern.matches(HttpUtils.HEADER_COOKIE_PATTERN, line)) {
				String[] cookieStrings = headerValue.split("[;,]");
				int version = 0;
				Cookie current = null;
				for (String cookie : cookieStrings) {
					String[] nameValuePair = cookie.split("=");
					if (nameValuePair[0].equalsIgnoreCase("$Domain")) {
						current.setDomain(nameValuePair[1]);
					}
					if (nameValuePair[0].equalsIgnoreCase("$Path")) {
						current.setPath(nameValuePair[1]);
					}
					if (nameValuePair[0].equalsIgnoreCase("$Version")) {
						version = Integer.parseInt(nameValuePair[1]);
					}
					else {
						current = new Cookie(nameValuePair[0].trim(), nameValuePair[1].trim());
						current.setVersion(version);
						cookies.add(current);
					}
				}
			}
		}
		List<String> values = getHeaderValues(headerName);
		values.add(headerValue);
	}
	
	private List<String> getHeaderValues(String name) {
		List<String> values = headers.get(name);
		if (values == null) {
			values = new ArrayList<String>();
			headers.put(name, values);
		}
		return values;
	}
	
	private Date parseDateHeader(String value) {
		Date parsedDate = null;
		try {
			parsedDate = format.parse(value); 
		}
		catch(ParseException ex) {
			try {
				parsedDate = format2.parse(value); 
			}
			catch(ParseException ex2) {
				try {
					parsedDate = format3.parse(value); 
				}
				catch(ParseException ex3) {
					// Couldn't parse date...so just ignore the header?
					Logger.logError(ex3, null);
				}
			}
		}
		return parsedDate;
	}
	
	public void handleRequest() throws IOException {
		cookies = new ArrayList<Cookie>();
		headers = new HashMap<String, List<String>>();
		
		// Make sure we got the correct initial request line.
		String line;
		if ((line = reader.readLine()) != null && Pattern.matches(HttpUtils.INITIAL_REQUEST_LINE_PATTERN, line)) {
			processInitialLine(line);

			if (validRequest) {
				// Read in header lines
				while ((line = reader.readLine()) != null && line.trim().length() != 0) {
					processHeaderLine(line);
				}
				handleResponse();
			}
		}
		else {
			handleBadRequest();
		}
		
		writer.flush();
	}
	
	private void handleResponse() throws IOException {
		if (protocol.equals(HttpUtils.HTTP_1_1) && !includesHost) {
			renderNoHostProvided();
		}
		else {
			if (method.equals(HttpUtils.METHOD_POST)) {
				int contentLength = 0;
				if (headers.containsKey("Content-Length")) {
					contentLength = Integer.valueOf(headers.get("Content-Length").get(0));
				}
				char[] contentChars = new char[contentLength];
				reader.read(contentChars, 0, contentLength);
				params = new String(contentChars);
			}
			Pattern pattern = Pattern.compile("https?://[^/]*(/[^\\?]*)(\\?(.*))?");
			Matcher matcher = pattern.matcher(path);
			if (matcher.matches()) {
				path = URLDecoder.decode(matcher.group(1), "UTF-8");
				if (method.equals(HttpUtils.METHOD_GET)) {
					params = matcher.group(3);
				}
			}

			boolean isServletRequest = false;
			HttpServlet servlet = null;
			for (Map.Entry<String, String> servletLookup : Httpserver.getEngine().getUrlPatterns().entrySet()) {
				String urlPattern = servletLookup.getKey();
				String servletName = servletLookup.getValue();
				if (urlPattern.endsWith("*")) {
					String prefix = urlPattern.substring(0, urlPattern.indexOf("*"));
					pattern = Pattern.compile("(" + prefix + ")" + "(/.*)");
				}
				else {
					pattern = Pattern.compile("(" + urlPattern + ")$");
				}
				matcher = pattern.matcher(path);
				if (matcher.matches()) {
					isServletRequest = true;
					ServletEngine engine = Httpserver.getEngine();
					if (engine != null) {
						Map<String, HttpServlet> servlets = engine.getServlets();
						if (servlets != null)
							servlet = servlets.get(servletName);
					}
					break;
				}
			}
			if (isServletRequest) {
				if (servlet != null) {
					HttpServletRequestImpl servletRequest = new HttpServletRequestImpl(input, this, (ServletConfigImpl) servlet.getServletConfig());
					HttpServletResponseImpl servletResponse = new HttpServletResponseImpl(output, servletRequest);

					if (params != null && params.trim().length() > 0) {
						String[] paramsArray = params.split("&");
						for (String param : paramsArray) {
							String[] paramParts = param.split("=");
							servletRequest.addParameter(paramParts[0], paramParts[1]);
						}
					}

					try {
						servlet.service(servletRequest, servletResponse);
						servletResponse.commit();
						servletResponse.flushBuffer();
					}
					catch (Exception ex) {
						handleError(ex);
					}
				}
				else {
					handleUnavailable();
				}
			}
			else {
				File requestFile = new File(Httpserver.getDocumentRoot().getCanonicalPath() + path);
				if (requestFile.exists()) {
					if (requestFile.getCanonicalPath().startsWith(Httpserver.getDocumentRoot().getCanonicalPath()) && !requestFile.isHidden()) {
						Date lastModified = new Date(requestFile.lastModified());

						if (modifiedSince != null && lastModified.before(modifiedSince) && method.equals(HttpUtils.METHOD_GET)) {
							handleNotModified();
						}
						else if (unmodifiedSince != null && lastModified.after(unmodifiedSince)) {
							handlePreconditionFailed();
						}
						else {
							// Get mime type
							String mimeType;
							if (requestFile.isDirectory()) {
								if (!path.endsWith("/")) {
									path = path + "/";
								}
								mimeType = "text/html";
								StringBuffer dirListing = new StringBuffer();
								dirListing.append("<html><head><title>Index of " + path + "</title></head>");
								dirListing.append("<body><h1>Index of " + path + "</h1>");
								File[] files = requestFile.listFiles();
								for(int i = 0; i < files.length; ++i) {
									dirListing.append("<p><a href='" + path + URLEncoder.encode(files[i].getName(), "UTF-8") + "'>" + files[i].getName() + "</a></p>");
								}
								dirListing.append("</body></html>");
								writer.write(protocol + " 200 OK\r\n");
								writer.write("Date: " + format.format(new Date()) + "\r\n");
								writer.write("Content-Type: " + mimeType + "\r\n");
								writer.write("Content-Length: " + dirListing.toString().getBytes().length + "\r\n");
								writer.write("Connection: close\r\n\r\n");
								if (method.equals(HttpUtils.METHOD_GET)) {
									writer.write(dirListing.toString());
								}
							}
							else {
								if (requestFile.getName().endsWith(".css")) {
									// For some reason, built in mime type map doesn't
									// include mappings for css files
									mimeType = "text/css";
								}
								else {
									mimeType = new MimetypesFileTypeMap().getContentType(requestFile);
								}

								writer.write(protocol + " 200 OK\r\n");
								writer.write("Date: " + format.format(new Date()) + "\r\n");
								writer.write("Content-Type: " + mimeType + "\r\n");
								writer.write("Content-Length: " + requestFile.length() + "\r\n");
								writer.write("Connection: close\r\n\r\n");
								if (method.equals(HttpUtils.METHOD_GET)) {
									FileInputStream fis = new FileInputStream(requestFile);
									int readValue;
									while ((readValue = fis.read()) != -1) {
										writer.write(readValue);
									}
									fis.close();
								}
							}
						}
					}
					else {
						handleForbidden();
					}
				}
				else {
					handleNotFound();
				}
			}
		}
	}

	private void renderNoHostProvided() throws IOException {
		String responseContent = "<html><body><h2>400 Bad Request</h2><p>No Host: header received</p><p>HTTP 1.1 requests must include the Host: header.</p></body></html>";

		writer.write(protocol + " 400 Bad Request\r\n");
		writer.write("Date: " + format.format(new Date()) + "\r\n");
		writer.write("Content-Type: text/html\r\n");
		writer.write("Content-Length: " + responseContent.getBytes().length + "\r\n");
		writer.write("Connection: close\r\n\r\n");
		writer.write(responseContent);
	}

	private void handlePreconditionFailed() throws IOException {
		writer.write("HTTP/1.1 412 Precondition Failed\r\n\r\n");
	}

	private void handleNotModified() throws IOException {
		writer.write("HTTP/1.1 304 Not Modified\r\n");
		writer.write("Date: " + format.format(new Date()) + "\r\n\r\n");
	}

	private void handleBadRequest() throws IOException {
		String responseContent;
		responseContent = "<html><body><h2>400 Bad Request</h2><p>I have no idea what you're trying to do.</p></body></html>";

		writer.write("HTTP/1.1 400 Bad Request\r\n");
		writer.write("Content-Type: text/html\r\n");
		writer.write("Content-Length: " + responseContent.getBytes().length + "\r\n");
		writer.write("Connection: close\r\n\r\n");
		writer.write(responseContent);
	}

	private void handleNotImplemented() throws IOException {
		String responseContent;
		responseContent = "<html><body><h2>501 Not Implemented</h2><p>The requested method is not implemented on this server.</p></body></html>";

		writer.write(protocol + " 501 Not Implemented\r\n");
		writer.write("Date: " + format.format(new Date()) + "\r\n");
		writer.write("Content-Type: text/html\r\n");
		writer.write("Content-Length: " + responseContent.getBytes().length + "\r\n");
		writer.write("Connection: close\r\n\r\n");
		writer.write(responseContent);
	}

	private void handleNotFound() throws IOException {
		String responseContent;
		responseContent = "<html><body><h2>404 Not Found</h2><p>The requested path was not found.</p></body></html>";

		writer.write(protocol + " 404 Not Found\r\n");
		writer.write("Date: " + format.format(new Date()) + "\r\n");
		writer.write("Content-Type: text/html\r\n");
		writer.write("Content-Length: " + responseContent.getBytes().length + "\r\n");
		writer.write("Connection: close\r\n\r\n");
		writer.write(responseContent);
	}

	private void handleForbidden() throws IOException {
		String responseContent;
		responseContent = "<html><body><h2>403 Forbidden</h2><p>Authorization required.</p></body></html>";

		writer.write(protocol + " 403 Forbidden\r\n");
		writer.write("Date: " + format.format(new Date()) + "\r\n");
		writer.write("Content-Type: text/html\r\n");
		writer.write("Content-Length: " + responseContent.getBytes().length + "\r\n");
		writer.write("Connection: close\r\n\r\n");
		writer.write(responseContent);
	}

	private void handleUnavailable() throws IOException {
		String responseContent;
		responseContent = "<html><body><h2>503 Service Unavailable</h2><p>The requested service is not running.  Please try again later.</p></body></html>";

		writer.write(protocol + " 503 Service Unavailable\r\n");
		writer.write("Date: " + format.format(new Date()) + "\r\n");
		writer.write("Content-Type: text/html\r\n");
		writer.write("Content-Length: " + responseContent.getBytes().length + "\r\n");
		writer.write("Connection: close\r\n\r\n");
		writer.write(responseContent);
	}

	private void handleError(Exception ex) throws IOException {
		String responseContent;
		responseContent = "<html><body><h2>500 Internal Server Error</h2><p>An unexpected error occurred.</p></body></html>";

		writer.write(protocol + " 500 Internal Server Error\r\n");
		writer.write("Date: " + format.format(new Date()) + "\r\n");
		writer.write("Content-Type: text/html\r\n");
		writer.write("Content-Length: " + responseContent.getBytes().length + "\r\n");
		writer.write("Connection: close\r\n\r\n");
		writer.write(responseContent);
		Logger.logError(ex, null);
	}

	public List<Cookie> getCookies() {
		return cookies;
	}
	
	public Map<String, List<String>> getHeaders() {
		return headers;
	}
	
	public String getMethod() {
		return method;
	}

	public String getProtocol() {
		return protocol;
	}
	
	public InputStream getInputStream() {
		return input;
	}
	
	public String getRemoteHostAddress() {
		return socket.getInetAddress().getHostAddress();
	}

	public String getRemoteHostName() {
		return socket.getInetAddress().getHostName();
	}
	
	public int getRemotePort() {
		return socket.getPort();
	}

	public String getLocalHostAddress() {
		return socket.getLocalAddress().getHostAddress();
	}
	
	public String getLocalHostName() {
		return socket.getLocalAddress().getHostName();
	}
	
	public int getLocalPort() {
		return socket.getLocalPort();
	}
}
