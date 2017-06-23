package sun.reflect;

/*******************************************************************************
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 *
 *     Copyright IBM Corp. 2007, 2016 All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/

final class DelegatingClassLoader extends ClassLoader {
	private ClassLoader parent;
	private Object rootObject;

	static {
		initMethodIds();
	}

private static native void initMethodIds();

DelegatingClassLoader(ClassLoader parent) {
	super(parent);
	this.parent = parent;
}

/*
 * Keep a related object (held in a SoftReference) alive until the
 * DelegatingClassLoader is unloaded.
 */
DelegatingClassLoader(ClassLoader parent, Object rootObject) {
	this(parent);
	this.rootObject = rootObject;
}

private native Class parentFindLoadedClassImpl(ClassLoader delegate, String className);

public Class<?> loadClass(String className) throws ClassNotFoundException {
	Class loadedClass = findLoadedClass(className);
	if (loadedClass != null) {
		return loadedClass;
	}

	if (parent != null) {
		// first call findLoadedClass() on the parent
		// some parents throw ClassNotFoundException from loadClass()
		// without first calling findLoadedClass()
		loadedClass = parentFindLoadedClassImpl(parent, className);
		if (loadedClass != null) {
			return loadedClass;
		}
	}
	// must call super.loadClass() because the parent may be null
	// calling super.loadClass() will ensure loadClass is called on the systemClassLoader
	return super.loadClass(className);
}

}
