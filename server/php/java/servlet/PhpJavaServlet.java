/*-*- mode: Java; tab-width:8 -*-*/

package php.java.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import php.java.bridge.DynamicJavaBridgeClassLoader;
import php.java.bridge.JavaBridge;
import php.java.bridge.JavaBridgeClassLoader;
import php.java.bridge.Request;
import php.java.bridge.ThreadPool;
import php.java.bridge.Util;
import php.java.bridge.http.ContextServer;

/**
 * Handles requests from PHP clients. When apache or IIS are not 
 * available this servlet can start  php as a CGI sub-process.
 * However, it is recommended to install php as an Apache module
 * and to use the mod_jk adapter to connect apache with the
 * servlet engine or context server.
 */
public class PhpJavaServlet extends FastCGIServlet {

    private static final long serialVersionUID = 3257854259629144372L;

    /**
     * The CGI default port
     */
    public static final String CGI_CHANNEL = "9567";
    static private final ContextServer socketRunner = new ContextServer(new ThreadPool("JavaBridgeContextRunner", Integer.parseInt(Util.THREAD_POOL_MAX_SIZE)));

    protected static class Logger extends Util.Logger {
	private ServletContext ctx;
	public Logger(ServletContext ctx) {
	    this.ctx = ctx;
	}
	public void log(String s) {
	    if(Util.logLevel>5) System.out.println(s);
	    ctx.log(s); 
	}
	public String now() { return ""; }
	public void printStackTrace(Throwable t) {
	    ctx.log(Util.EXTENSION_NAME + " Exception: ", t);
	    if(Util.logLevel>5) t.printStackTrace();
	}
    }
    
    private boolean override_hosts = true;
    
    public void init(ServletConfig config) throws ServletException {
	String value;
        try {
	    value = config.getInitParameter("servlet_log_level");
	    if(value!=null && value.trim().length()!=0) Util.logLevel=Integer.parseInt(value);
        } catch (Throwable t) {Util.printStackTrace(t);}      

    	try {
	    value = config.getInitParameter("override_hosts");
	    if(value!=null && value.trim().equalsIgnoreCase("off")) override_hosts=false;
    	} catch (Throwable t) {Util.printStackTrace(t);}      

    	super.init(config);
       
	Util.setLogger(new Logger(config.getServletContext()));
        DynamicJavaBridgeClassLoader.initClassLoader(Util.DEFAULT_EXTENSION_DIR);

	Util.TCP_SOCKETNAME = CGI_CHANNEL;
	    try {
	        Runtime.getRuntime().addShutdownHook(
						 (new Thread("JavaBridgeContextRunnerShutdown") {
						     public void run() {
							 if(socketRunner!=null) try {socketRunner.destroy();} catch(Throwable t) {/*ignore*/}
						     }
						 }));
	    } catch (Throwable t) {t.printStackTrace();};

    }

    public void destroy() {
    	super.destroy();
    }
    
    /**
     * Adjust the standard tomcat CGI env. CGI only.
     */
    protected class CGIEnvironment extends FastCGIServlet.CGIEnvironment {
    	protected ContextFactory sessionFactory;
    	
	protected CGIEnvironment(HttpServletRequest req, HttpServletResponse res, ServletContext context) {
	    super(req, res, context);
	}

	protected String getCommand() {
	    return php;
	}
	protected boolean setCGIEnvironment(HttpServletRequest req, HttpServletResponse res) {
	    boolean ret = super.setCGIEnvironment(req, res);
	    if(ret) {
	    	/* Inform the client that we are a cgi servlet and send the re-direct port */
		if(override_hosts) { 
		    StringBuffer buf = new StringBuffer("127.0.0.1:");
		    buf.append(this.env.get("SERVER_PORT"));
		    buf.append("/");
		    buf.append(req.getRequestURI());
		    this.env.put("X_JAVABRIDGE_OVERRIDE_HOSTS", buf.toString());
		}
		else
		    this.env.put("X_JAVABRIDGE_OVERRIDE_HOSTS", "");
		this.env.put("REDIRECT_STATUS", "1");
		this.env.put("SCRIPT_FILENAME", this.env.get("PATH_TRANSLATED"));
	        this.env.put("SERVER_SOFTWARE", Util.EXTENSION_NAME);
	        this.env.put("HTTP_HOST", this.env.get("SERVER_NAME")+":"+this.env.get("SERVER_PORT"));
	        
		/* send the session context now, otherwise the client has to 
		 * call handleRedirectConnection */
	    	String id = req.getHeader("X_JAVABRIDGE_CONTEXT");
	    	if(id==null) id = ContextFactory.addNew(PhpJavaServlet.this.getServletContext(), req, res).getId();
		this.env.put("X_JAVABRIDGE_CONTEXT", id);
	        
	        /* For the request: http://localhost:8080/JavaBridge/test.php the
	         * req.getPathInfo() returns cgi/test.php. But PHP shouldn't know
	         * about this detail.
	         */
	        this.env.remove("PATH_INFO"); 
	    }
	    return ret;
	        	
	}
	protected String[] findCGI(String pathInfo, String webAppRootDir,
				   String contextPath, String servletPath,
				   String cgiPathPrefix) {
	    String[] retval;
	    if((retval=super.findCGI(pathInfo, webAppRootDir, contextPath, servletPath, cgiPathPrefix))!=null) return retval;
	    cgiRunnerFactory = new CGIRunnerFactory();
		
	    StringBuffer cgiDir = new StringBuffer(webAppRootDir);
	    if(!webAppRootDir.endsWith(File.separator)) cgiDir.append(File.separatorChar);
	    cgiDir.append(cgiPathPrefix);

	    // incorrect but reasonable values for display only.
	    String display_cgi="php";
	    this.pathInfo = "/"+display_cgi+servletPath;
	    
	    cgiDir.append(File.separatorChar);
	    cgiDir.append(display_cgi);
	    return new String[] {
		cgiDir.toString(),
		contextPath+servletPath, 
		File.separator+display_cgi, 
		display_cgi};
	}
    }
    
    /**
     * Create a cgi environment. Used by cgi only.
     * @param req The request
     * @param servletContext The servlet context
     * @return The new cgi environment.
     */
    protected CGIServlet.CGIEnvironment createCGIEnvironment(HttpServletRequest req, HttpServletResponse res, ServletContext servletContext) {
	CGIEnvironment env = new CGIEnvironment(req, res, servletContext);
	env.init(req, res);
	return env;
    }

    protected class CGIRunnerFactory extends CGIServlet.CGIRunnerFactory {
        protected CGIServlet.CGIRunner createCGIRunner(CGIServlet.CGIEnvironment cgiEnv) {
            return new CGIRunner(cgiEnv);
	}
    }

    protected static class HeaderParser extends Util.HeaderParser {
    	private CGIRunner runner;
	protected HeaderParser(CGIRunner runner) {
	    this.runner = runner;
    	}
    	protected void parseHeader(String header) {
	    runner.addHeader(header);
    	}
    }
    protected class CGIRunner extends CGIServlet.CGIRunner {
	
	protected CGIRunner(CGIServlet.CGIEnvironment env) {
	    super(env);
	}


        protected void run() throws IOException, ServletException {
	    Process proc = null;
	    InputStream natIn = null;
	    OutputStream natOut = null;
	    InputStream in = null;
	    OutputStream out = null;
    	    try {
        	proc = Util.startProcess(new String[]{command}, wd, env);
        	Util.startProcessErrorReader(proc);

        	byte[] buf = new byte[BUF_SIZE];// headers cannot be larger than this value!

        	// the post variables
        	in = stdin;
    		natOut = proc.getOutputStream();
        	if(in!=null) {
		    int n;
    		    while((n=in.read(buf))!=-1) {
    			natOut.write(buf, 0, n);
    		    }
    		}

        	// header and body
         	natIn = proc.getInputStream();
    		out = response.getOutputStream();
    		Util.parseBody(buf, natIn, out, new Util.HeaderParser() {protected void parseHeader(String header) {addHeader(header);}});
    		
    		proc=null;
    	    } catch(IOException t) {throw t;} catch (Throwable t) { throw new ServletException(t); } finally {
    		if(out!=null) try {out.close();} catch (IOException e) {}
    		if(in!=null) try {in.close();} catch (IOException e) {}
    		if(natIn!=null) try {natIn.close();} catch (IOException e) {}
    		if(natOut!=null) try {natOut.close();} catch (IOException e) {}

    		if(proc!=null) try {proc.destroy(); } catch (Exception e) {}
    	    }
        }
    } //class CGIRunner
    
    
    private ContextFactory getContextManager(HttpServletRequest req, HttpServletResponse res) {
    	ContextFactory ctx = null;
    	String id = req.getHeader("X_JAVABRIDGE_CONTEXT");
    	if(id!=null) ctx = (ContextFactory)ContextFactory.get(id);
    	if(ctx==null) ctx = ContextFactory.addNew(getServletContext(), null, res); // no session sharing
    	if(ctx.getBridge()==null) {
	    JavaBridge bridge;
   	    ctx.setBridge(bridge = new JavaBridge(null, null));
    	    bridge.setClassLoader(new JavaBridgeClassLoader(ctx.getBridge(), DynamicJavaBridgeClassLoader.newInstance(Util.getContextClassLoader())));
    	    bridge.setSessionFactory(ctx);
    	    JavaBridge.load++;
	    ctx.getBridge().logDebug("first request (session is new).");
    	} else {
    	    ctx.getBridge().logDebug("cont. session");
    	}
    	res.setHeader("X_JAVABRIDGE_CONTEXT", ctx.getId());
    	return ctx;
    }
    
    /**
     * Handle the override re-direct for "java_get_session()" when php runs within apache. 
     * Instead of connecting back to apache, we execute one statement and return the
     * result and the allocated session.  Used by Apache only.
     * 
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    protected void handleRedirectConnection(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
	InputStream in; ByteArrayOutputStream out; OutputStream resOut;
	ContextFactory ctx = getContextManager(req, res);
	JavaBridge bridge = ctx.getBridge();
	ctx.setSession(req);
	if(bridge.logLevel>3) bridge.logDebug("override redirect starts for " + ctx.getId());		
	// save old state
	InputStream bin = bridge.in;
	OutputStream bout = bridge.out;
	Request br  = bridge.request;
	
	bridge.in = in = req.getInputStream();
	bridge.out = out = new ByteArrayOutputStream();
	Request r = bridge.request = new Request(bridge);
	try {
	    if(r.init(in, out)) {
		r.handleOneRequest();
	    }
	} catch (Throwable e) {
	    Util.printStackTrace(e);
	}
	// restore state
	bridge.in = bin; bridge.out = bout; bridge.request = br;
	
	res.setContentLength(out.size());
	resOut = res.getOutputStream();
	out.writeTo(resOut);
	in.close();
	resOut.close();
	if(bridge.logLevel>3) bridge.logDebug("override redirect finished for " + ctx.getId());
    }
    
    /**
     * Handle a standard HTTP tunnel connection. Used when the local channel is not available
     * (security restrictions). Used by Apache and cgi.
     * 
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    protected void handleHttpConnection (HttpServletRequest req, HttpServletResponse res, boolean session)
	throws ServletException, IOException {
	InputStream in; ByteArrayOutputStream out;
	ContextFactory ctx = getContextManager(req, res);
	JavaBridge bridge = ctx.getBridge();
	if(session) ctx.setSession(req);

	if(req.getContentLength()==0) {
	    if(req.getHeader("Connection").equals("Close")) {
		JavaBridge.load--;
		bridge.logDebug("session closed.");
		ctx.remove();
	    }
	    return;
	}
	bridge.in = in = req.getInputStream();
	bridge.out = out = new ByteArrayOutputStream();
	Request r = bridge.request = new Request(bridge);
	try {
	    if(r.init(in, out)) {
		r.handleRequests();
	    }
	} catch (Throwable e) {
	    Util.printStackTrace(e);
	}
	res.setContentLength(out.size());
	out.writeTo(res.getOutputStream());
	in.close();
	out.close();
    }
    
    /**
     * Handle a redirected connection. The local channel is more than 50 
     * times faster than the HTTP tunnel. Used by Apache and cgi.
     * 
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    protected void handleSocketConnection (HttpServletRequest req, HttpServletResponse res, boolean session)
	throws ServletException, IOException {
	InputStream sin=null; ByteArrayOutputStream sout; OutputStream resOut = null;
	ContextFactory ctx = getContextManager(req, res);
	JavaBridge bridge = ctx.getBridge();
	if(session) ctx.setSession(req);

	bridge.in = sin=req.getInputStream();
	bridge.out = sout = new ByteArrayOutputStream();
	Request r = bridge.request = new Request(bridge);

	try {
	    if(r.init(sin, sout)) {
		res.setHeader("X_JAVABRIDGE_REDIRECT", socketRunner.getSocket().getSocketName());
	    	r.handleOneRequest();

		// redirect and re-open
	    	socketRunner.schedule();
		res.setContentLength(sout.size());
		resOut = res.getOutputStream();
		sout.writeTo(resOut);
		if(bridge.logLevel>3) bridge.logDebug("re-directing to port# "+ socketRunner.getSocket().getSocketName());
	    	sin.close();
	    	resOut.flush();
	    	if(bridge.logLevel>3) bridge.logDebug("waiting for context: " +ctx.getId());
	    	ctx.waitFor();
	    	resOut.close();
	    	if(bridge.logLevel>3) bridge.logDebug("context finished: " +ctx.getId());
	    }
	    else {
	        sin.close();
	        ctx.remove();
	    }
	} catch (Exception e) {
	    Util.printStackTrace(e);
	    try {if(sin!=null) sin.close();} catch (IOException e1) {}
	    try {if(resOut!=null) resOut.close();} catch (IOException e2) {}
	}
    }

    /**
     * Dispatcher for the "http tunnel", "local channel" or "override redirect".
     */
    protected void doPut (HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
    	short redirect = (short) req.getIntHeader("X_JAVABRIDGE_REDIRECT");
	try {
	    if(redirect==1) 
		handleRedirectConnection(req, res); /* override re-direct */
	    else if(socketRunner.isAvailable()) 
		handleSocketConnection(req, res, redirect==2); /* re-direct */
	    else
		handleHttpConnection(req, res, redirect==2); /* standard http tunnel */

	} catch (Throwable t) {
	    Util.printStackTrace(t);
	    try {res.getOutputStream().close();} catch (IOException x1) {}
	    try {req.getInputStream().close();} catch (IOException x2) {}
	}
    }

    /**
     * Used when running as a cgi binary only.
     * 
     *  (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {

    	try {
	    super.doGet(req, res);
    	} catch (IOException e) {
	    ServletException ex = new ServletException("An IO exception occured. Probably php was not installed as \"/usr/bin/php\" or \"c:/php5/php-cgi.exe\".\nPlease copy your PHP binary (\""+php+"\", see JavaBridge/WEB-INF/web.xml) into the JavaBridge/WEB-INF/cgi directory.\nSee webapps/JavaBridge/WEB-INF/cgi/README for details.", e);
	    php=null;
	    checkCgiBinary(getServletConfig());
	    throw ex;
    	} catch (Throwable t) {
	    Util.printStackTrace(t);
    	}
    }
}
