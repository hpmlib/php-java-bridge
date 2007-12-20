/*-*- mode: Java; tab-width:8 -*-*/

package php.java.bridge;


/*
 * Copyright (C) 2003-2007 Jost Boekemeier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER(S) OR AUTHOR(S) BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import php.java.bridge.http.IContext;

/**
 * Create PHP/Java Bridge instances.
 */
public interface IJavaBridgeFactory {

    /**
     * Return an instance of the JavaBridgeClassLoader. 
     * Return an instance of SimpleJavaBridgeClassLoader, or, if you want to support java_require(), an instance of the JavaBridgeClassLoader
     * with the current thread context class loader as a delegate.
     * @see php.java.bridge.Util#getContextClassLoader() 
     * @return The JavaBridgeClassLoader
     */
    public SimpleJavaBridgeClassLoader getJavaBridgeClassLoader();
    
    /**
     * Return a session for the JavaBridge
     * @param name The session name. If name is null, the name PHPSESSION will be used.
     * @param clientIsNew true if the client wants a new session
     * @param timeout timeout in seconds. If 0 the session does not expire.
     * @return The session
     * @see php.java.bridge.ISession
     */
    public ISession getSession(String name, boolean clientIsNew, int timeout);

    /**
     * Return the associated JSR223 context
     * @return The JSR223 context, if supported by the environment or null.
     * @see php.java.bridge.http.ContextFactory#getContext()
     */
    public IContext getContext();

    /**
     * Return the JavaBridge.
     * @return Returns the bridge.
     */
    public JavaBridge getBridge();

    /**
     * Recycle the factory for new reqests.
     */
    public void recycle();

    /**
     * Destroy the factory
     */
    public void destroy();
}