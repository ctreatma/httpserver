package edu.upenn.cis555.webserver.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;


public class ResponseWriter extends PrintWriter {
    private HttpServletResponseImpl response;
    private int contentLength;
    
    public ResponseWriter(HttpServletResponseImpl response,
            OutputStreamWriter output) {
        super(output);
        this.response = response;
        this.contentLength = 0;
    }
    
    @Override
    public void flush() {
        try {
        if (!response.isCommitted()) {
            response.commit();
        }
        super.flush();
        }
        catch (IOException ex) {
            // Can't throw it here, so log it?
            ex.printStackTrace();
        }
    }

    @Override
    public void write(char[] buf) {
        write(buf, 0, buf.length);
    }
    
    @Override
    public void write(char[] buf, int off, int len) {
        super.write(buf, off, len);
        contentLength += len;
    }
    
    @Override
    public void write(int c) {
        super.write(c);
        contentLength += 1;
    }
    
    @Override
    public void write(String s) {
        write(s, 0, s.length());
    }
    
    @Override
    public void write(String s, int off, int len) {
        super.write(s, off, len);
        contentLength += len;
    }

	public int getContentLength() {
		return contentLength;
	}
}
