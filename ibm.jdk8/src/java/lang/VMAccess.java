package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2012, 2016  All Rights Reserved
 */

import com.ibm.oti.vm.*;

/**
 * Helper class to allow privileged access to classes
 * from outside the java.lang package. Based on sun.misc.SharedSecrets
 * implementation.
 */
final class VMAccess implements VMLangAccess {

	private static ClassLoader extClassLoader;

	/**
	 * Set the extension class loader. It can only be set once.
	 *
	 * @param loader the extension class loader
	 */
	static void setExtClassLoader(ClassLoader loader) {
		extClassLoader = loader;
	}

	/**
	 * Answer the extension class loader.
	 */
	public ClassLoader getExtClassLoader() {
		return extClassLoader;
	}

	/**
	 * Returns true if parent is the ancestor of child.
	 * Parent and child must not be null.
	 */
	public boolean isAncestor(java.lang.ClassLoader parent, java.lang.ClassLoader child) {
		return parent.isAncestorOf(child);
	}

	/**
	 * Returns the ClassLoader off clazz.
	 */
	public java.lang.ClassLoader getClassloader(java.lang.Class clazz) {
		return clazz.getClassLoaderImpl();
	}

	/**
	 * Returns the package name for a given class.
	 * clazz must not be null.
	 */
	public java.lang.String getPackageName(java.lang.Class clazz) {
		return clazz.getPackageName();
	}

	/**
	 * Returns a MethodHandle cache for a given class.
	 */
	public java.lang.Object getMethodHandleCache(java.lang.Class<?> clazz) {
		return clazz.getMethodHandleCache();
	}

	/**
	 * Set a MethodHandle cache to a given class.
	 */
	public java.lang.Object setMethodHandleCache(java.lang.Class<?> clazz, java.lang.Object object) {
		return clazz.setMethodHandleCache(object);
	}

	/**
	 * Returns a {@code java.util.Map} from method descriptor string to the equivalent {@code MethodType} as generated by {@code MethodType.fromMethodDescriptorString}.
	 * @param loader The {@code ClassLoader} used to get the MethodType.
	 * @return A {@code java.util.Map} from method descriptor string to the equivalent {@code MethodType}.
	 */
	public java.util.Map<String, java.lang.invoke.MethodType> getMethodTypeCache(ClassLoader loader) {
		return loader.getMethodTypeCache();
	}
}
