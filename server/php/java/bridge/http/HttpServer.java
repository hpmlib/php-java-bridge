/*-*- mode: Java; tab-width:8 -*-*/

package php.java.bridge.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import php.java.bridge.ISocketFactory;
import php.java.bridge.Util;


/**
 * This class can be used to create a simple HTTP server. It can be used during development, when no HTTP server or Servlet engine is available.
 * @author jostb
 *
 */
public abstract class HttpServer implements Runnable {
    protected ISocketFactory socket;

    /**
     * Create a new server socket
     * @return The server socket.
     */
    public abstract ISocketFactory bind();

    /**
     * Create a new HTTP Server.
     * @see HttpServer#destroy()
     */
    public HttpServer() {
	socket = bind();
	Thread t = new Thread(this, "JavaBridgeHttpServer");
	t.setDaemon(true);
        t.start();
    }

    /**
     * Parse the header. After that <code>req</code> contains the body.
     * @param req The HttpRequest
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    protected void parseHeader(HttpRequest req) throws UnsupportedEncodingException, IOException {
	byte buf[] = new byte[Util.BUF_SIZE];
		
	InputStream natIn = req.getInputStream();
	String line = null;
	int i=0, n, s=0;
	boolean eoh=false;
	// the header and content
	while((n = natIn.read(buf, i, buf.length-i)) !=-1 ) {
	    int N = i + n;
	    // header
	    while(!eoh && i<N) {
		switch(buf[i++]) {
    			
		case '\n':
		    if(s+2==i && buf[s]=='\r') {
			eoh=true;
		    } else {
			req.addHeader(new String(buf, s, i-s-2, Util.ASCII));
			s=i;
		    }
		}
	    }
	    // body
	    if(eoh) {
		req.pushBack(buf, i, N-i);
		break;
	    }
	}

    }
		
    /**
     * accept, create a HTTP request and response, parse the header and body
     * @throws IOException
     */
    protected void doRun() throws IOException {
	while(true) {
	    Socket sock;
	    try {sock = socket.accept();} catch (IOException e) {return;} // socket closed
	    Util.logDebug("Socket connection accepted");
	    HttpRequest req = new HttpRequest(sock.getInputStream());
	    HttpResponse res = new HttpResponse(sock.getOutputStream());
	    parseHeader(req);
	    parseBody(req, res);
	}
    }

    /**
     * Sets the content length but leaves the rest of the body untouched.
     */
    protected void parseBody(HttpRequest req, HttpResponse res) {
	req.setContentLength(Integer.parseInt(req.getHeader("Content-Length")));
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
	try {
	    doRun();
	} catch (IOException e) {
	    Util.printStackTrace(e);
	}
    }

    /**
     * Stop the HTTP server.
     *
     */
    public void destroy() {
	try {
	    socket.close();
	} catch (IOException e) {
	    Util.printStackTrace(e);
	}
    }
}
