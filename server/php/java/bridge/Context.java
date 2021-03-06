
package php.java.bridge;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;


public class Context implements Invocable {

    /**
     * The engine scope
     */
    public static final int ENGINE_SCOPE = 100;

    /**
     * The global scope
     */
    public static final int GLOBAL_SCOPE = 200;
	
    /** Map of the scope of level GLOBAL_SCOPE */
    protected Map globalScope;
    
    /** Map of the scope of level ENGINE_SCOPE */
    protected Map engineScope;

    private Object	kont;


	
    
    /**
     * Retrieves the value for getAttribute(String, int) for the 
     * lowest scope in which it returns a non-null value.
     * 
     * @param name the name of the attribute 
     * @return the value of the attribute
     */
    public Object getAttribute(String name) throws IllegalArgumentException{
      
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        
        if (engineScope.get(name) != null) {
            return engineScope.get(name);
        } else if (globalScope.get(name) != null) {
            return globalScope.get(name);
        } else {
            return null;            
        }
    }
    
    /**
     * Retrieves the value associated with specified name in the 
     * specified level of scope. Returns null if no value is 
     * associated with specified key in specified level of scope.
     *  
     * @param key   the name of the attribute
     * @param scope the level of scope
     * @return the value value associated with the specified name in
     *         specified level of scope
     */
    public Object getAttribute(String name, int scope) 
	throws IllegalArgumentException{
        
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        
        switch (scope) {
	case ENGINE_SCOPE:
	    return engineScope.get(name);
	case GLOBAL_SCOPE:
	    return globalScope.get(name);
	default:
	    throw new IllegalArgumentException("invalid scope");
        }
    }

    /**
     * Retrieves the lowest value of scopes for which the attribute 
     * is defined. If there is no associate scope with the given 
     * attribute (-1) is returned.
     * 
     * @param  name the name of attribute
     * @return the value of level of scope  
     */
    public int getAttributesScope(String name) {
        if (engineScope.containsKey(name)) {
            return ENGINE_SCOPE;
        } else if(globalScope.containsKey(name)) {
            return GLOBAL_SCOPE;
        }
        
        return -1;
    }
    
    /**
     * Retrieves the Map instance associated with the specified
     * level of scope.
     * 
     * @param scope the level of the scope
     * @return the Map associated with the specified level of 
     *         scope
     */
    public Map getMap(int scope) {
        
        switch (scope) {
	case ENGINE_SCOPE:
	    return engineScope;
	case GLOBAL_SCOPE:
	    return globalScope;
	default:
	    return null;
        }
    }
    
    /**
     * Retrieves an instance of java.io.Writer which can be used by 
     * scripts to display their output.
     * 
     * @return an instance of java.io.Writer
     */
    public Writer getWriter() throws IOException {
        
        // autoflush is ture so that I can see the output immediately
        return new PrintWriter(System.out, true); 
    }
    
    /**
     * Removes the specified attribute form the specified level of 
     * scope.
     * 
     * @param name the name of the attribute
     * @param scope the level of scope 
     * @return value which is removed
     * @throws
     */
    public Object removeAttribute(String name, int scope) 
	throws IllegalArgumentException{ 
        
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        
        switch (scope) {
	case ENGINE_SCOPE:
	    return engineScope.remove(name);
	case GLOBAL_SCOPE:
	    return globalScope.remove(name);
	default:
	    throw new IllegalArgumentException("invalid scope");
        }    
    }
    
    /**
     * Sets an attribute specified by the name in specified level of 
     * scope.
     *  
     * @param key   the name of the attribute
     * @param value the value of the attribute
     * @param scope the level of the scope
     * @return value the value associated with the specified key in 
     *         specified scope
     * @throws IllegalArguementException if the name is null scope is
     *         invlaid
     */
    public void setAttribute(String name, Object value, int scope) 
	throws IllegalArgumentException{
        
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        
        switch (scope) {
	case ENGINE_SCOPE:
	    engineScope.put(name, value);
	    break;
	case GLOBAL_SCOPE:
	    globalScope.put(name, value);
	    break;
	default:
	    throw new IllegalArgumentException("invalid scope");
        }
    }
	
    /**
     * Associates the specified Map with specified level of 
     * scope.
     * 
     * @param map the Map to be associated with specified
     *                  level of scope
     * @param scope     the level of scope 
     */	
    public void setMap(Map map, int scope) 
	throws IllegalArgumentException {
        
	switch (scope) {
	case ENGINE_SCOPE:
	    engineScope = map;
	    break;
	case GLOBAL_SCOPE:
	    globalScope = map;
	    break;
	default:
	    throw new IllegalArgumentException("invalid scope");
			
	}
    }

    /* (non-Javadoc)
     * @see php.java.bridge.Invocable#call(php.java.bridge.PhpProcedureProxy)
     */
    public boolean call(PhpProcedureProxy kont) {
	return false;
    }
	
}
