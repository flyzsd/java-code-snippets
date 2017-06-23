package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2007, 2016  All Rights Reserved
 */

import java.security.AccessControlContext;
import java.lang.annotation.Annotation;

import com.ibm.oti.reflect.TypeAnnotationParser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Map;
import com.ibm.oti.reflect.AnnotationParser;

import sun.misc.JavaLangAccess;
import sun.nio.ch.Interruptible;
import sun.reflect.ConstantPool;
import sun.reflect.annotation.AnnotationType;

/**
 * Helper class used by the Sun JDK to allow privileged access to classes
 * from outside the java.lang package.  The sun.misc.SharedSecrets class
 * uses an instance of this class to access private java.lang members.
 */

final class Access implements JavaLangAccess {

	/** Set thread's blocker field. */
	public void blockedOn(java.lang.Thread thread, Interruptible interruptable) {
		thread.blockedOn(interruptable);
	}

    /**
     * Get the AnnotationType instance corresponding to this class.
     * (This method only applies to annotation types.)
     */
	public AnnotationType getAnnotationType(java.lang.Class arg0) {
		return arg0.getAnnotationType();
	}

	/** Return the constant pool for a class. */
	public native ConstantPool getConstantPool(java.lang.Class arg0);

    /**
     * Returns the elements of an enum class or null if the
     * Class object does not represent an enum type;
     * the result is uncloned, cached, and shared by all callers.
     */
	public <E extends Enum<E>> E[] getEnumConstantsShared(java.lang.Class<E> arg0) {
		return arg0.getEnumConstantsShared();
	}

	public void registerShutdownHook(int arg0, boolean arg1, Runnable arg2) {
		Shutdown.add(arg0, arg1, arg2);
	}

    /**
     * Set the AnnotationType instance corresponding to this class.
     * (This method only applies to annotation types.)
     */
	public void setAnnotationType(java.lang.Class<?> arg0, AnnotationType arg1) {
		arg0.setAnnotationType(arg1);
	}

	/**
	 * Compare-And-Swap the AnnotationType instance corresponding to this class.
	 * (This method only applies to annotation types.)
	 */
	public boolean casAnnotationType(final Class<?> clazz, AnnotationType oldType, AnnotationType newType) {
		return clazz.casAnnotationType(oldType, newType);
	}

	/**
	 * @param clazz Class object
	 * @return the array of bytes for the Annotations for clazz, or null if clazz is null
	 */
	public byte[] getRawClassAnnotations(Class<?> clazz) {

		return AnnotationParser.getAnnotationsData(clazz);
	}

	public int getStackTraceDepth(java.lang.Throwable arg0) {
		return arg0.getInternalStackTrace().length;
	}

	public java.lang.StackTraceElement getStackTraceElement(java.lang.Throwable arg0, int arg1) {
		return arg0.getInternalStackTrace()[arg1];
	}

	public Thread newThreadWithAcc(Runnable runnable, AccessControlContext acc) {
		return new Thread(runnable, acc);
	}

	/**
     * Returns a directly present annotation instance of annotationClass type from clazz.
     *
     *  @param clazz Class that will be searched for given annotationClass
     *  @param annotationClass annotation class that is being searched on the clazz declaration
     *
     *  @return declared annotation of annotationClass type for clazz, otherwise return null
     *
     */
    public  <A extends Annotation> A getDirectDeclaredAnnotation(Class<?> clazz, Class<A> annotationClass)
    {
    	return clazz.getDeclaredAnnotation(annotationClass);
    }

	@Override
	public Map<java.lang.Class<? extends Annotation>, Annotation> getDeclaredAnnotationMap(
			java.lang.Class<?> arg0) {
		throw new Error("getDeclaredAnnotationMap unimplemented"); //$NON-NLS-1$
	}

	@Override
	public byte[] getRawClassTypeAnnotations(java.lang.Class<?> clazz) {
		return TypeAnnotationParser.getTypeAnnotationsData(clazz);
	}

	@Override
	public byte[] getRawExecutableTypeAnnotations(Executable exec) {
		byte[] result = null;
		if (Method.class.isInstance(exec)) {
			Method jlrMethod = (Method) exec;
			result = TypeAnnotationParser.getTypeAnnotationsData(jlrMethod);
		} else if (Constructor.class.isInstance(exec)) {
			Constructor<?> jlrConstructor = (Constructor<?>) exec;
			result = TypeAnnotationParser.getTypeAnnotationsData(jlrConstructor);
		} else {
			throw new Error("getRawExecutableTypeAnnotations not defined for "+exec.getClass().getName()); //$NON-NLS-1$
		}
		return result;
	}

	/**
	 * Return a newly created String that uses the passed in char[]
	 * without copying.  The array must not be modified after creating
	 * the String.
	 *
	 * @param data The char[] for the String
	 * @return a new String using the char[].
	 */
	@Override
	public java.lang.String newStringUnsafe(char[] data) {
		return new String(data, true /*ignored*/);
	}

	@Override
	public void invokeFinalize(java.lang.Object arg0)
			throws java.lang.Throwable {
		/*
		 * Needed only by Oracle: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8027351
		 *  Not required for J9.
		 */
		throw new Error("invokeFinalize unimplemented"); //$NON-NLS-1$
	}
}
