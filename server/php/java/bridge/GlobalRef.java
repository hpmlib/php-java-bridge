/*-*- mode: Java; tab-width:8 -*-*/

package php.java.bridge;

public class GlobalRef {
    static final int DEFAULT_SIZE=1024;
	
    Object[] globalRef;
    int id;
	
    private JavaBridge bridge;
    public GlobalRef(JavaBridge bridge) {
	this.bridge=bridge;
	globalRef=new Object[DEFAULT_SIZE];
    }
	
    public Object get(int id) {
	return globalRef[--id];
    }
	
    public void remove(int id) {
	globalRef[--id]=null;
    }
	
    public int append(Object object) {
	try {
	    globalRef[id]=object;
	} catch (ArrayIndexOutOfBoundsException e) {
	    Object o[] = new Object[globalRef.length<<1];
	    System.arraycopy(globalRef, 0, o, 0, globalRef.length);
	    globalRef=o;
	    globalRef[id]=object;
	}
	return ++id; // 0 is interpreted as null
    }
}