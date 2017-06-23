/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2014, 2016  All Rights Reserved
 */

package java.lang.invoke;

import static java.lang.invoke.MethodType.methodType;

import com.ibm.oti.vm.VMLangAccess;

/**
 * MethodHandleCache is a per-class cache that used to cache:
 * * the Class.cast(Object) handle for the class &
 * * the Constant(null) handle for the class.
 * These handles are commonly created and this saves on creating duplicate handles.
 */
class MethodHandleCache {

	private Class<?> clazz;
	private MethodHandle classCastHandle;
	private MethodHandle nullConstantObjectHandle;

	private MethodHandleCache(Class<?> clazz) {
		this.clazz = clazz;
	}

	/**
	 * Get the MethodHandle cache for a given class.
	 * If one doesn't exist, it will be created.
	 *
	 * @param clazz the Class that MHs are being cached on
	 * @return the MethodHandleCache object for that class
	 */
	public static MethodHandleCache getCache(Class<?> clazz) {
		VMLangAccess vma = MethodHandles.Lookup.getVMLangAccess();
		MethodHandleCache cache = (MethodHandleCache)vma.getMethodHandleCache(clazz);
		if (null == cache) {
			cache = new MethodHandleCache(clazz);
			cache = (MethodHandleCache)vma.setMethodHandleCache(clazz, cache);
		}
		return cache;
	}

	/**
	 * @return a BoundMethodHandle that calls {@link Class#cast(Object)} on the passed in class
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 */
	public MethodHandle getClassCastHandle() throws IllegalAccessException, NoSuchMethodException {
		if (null == classCastHandle) {
			synchronized (this) {
				if (null == classCastHandle) {
					classCastHandle = MethodHandles.Lookup.internalPrivilegedLookup.bind(clazz, "cast", methodType(Object.class, Object.class)); //$NON-NLS-1$
				}
			}
		}
		return classCastHandle;
	}

	/**
	 * @return a MethodHandle that contains a null ConstantHandle for a specific class.
	 */
	public MethodHandle getNullConstantObjectHandle() {
		if (null == nullConstantObjectHandle) {
			nullConstantObjectHandle = new ConstantObjectHandle(methodType(clazz), null);
		}
		return nullConstantObjectHandle;
	}
}
