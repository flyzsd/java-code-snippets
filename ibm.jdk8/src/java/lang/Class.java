package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

import java.io.InputStream;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.security.AllPermission;
import java.security.Permissions;
import java.lang.reflect.*;
import java.net.URL;
import java.lang.annotation.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedAction;
import java.lang.ref.*;

import sun.reflect.generics.repository.ClassRepository;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.scope.ClassScope;
import java.util.ArrayList;
import sun.reflect.annotation.AnnotationType;

import java.util.Arrays;

import com.ibm.jvm.packed.PackedObject;

import com.ibm.oti.vm.VM;

import java.lang.annotation.Repeatable;
import java.lang.invoke.*;

import com.ibm.oti.reflect.TypeAnnotationParser;

import java.security.PrivilegedActionException;

/**
 * An instance of class Class is the in-image representation
 * of a Java class. There are three basic types of Classes
 * <dl>
 * <dt><em>Classes representing object types (classes or interfaces)</em></dt>
 * <dd>These are Classes which represent the class of a
 *     simple instance as found in the class hierarchy.
 *     The name of one of these Classes is simply the
 *     fully qualified class name of the class or interface
 *     that it represents. Its <em>signature</em> is
 *     the letter "L", followed by its name, followed
 *     by a semi-colon (";").</dd>
 * <dt><em>Classes representing base types</em></dt>
 * <dd>These Classes represent the standard Java base types.
 *     Although it is not possible to create new instances
 *     of these Classes, they are still useful for providing
 *     reflection information, and as the component type
 *     of array classes. There is one of these Classes for
 *     each base type, and their signatures are:
 *     <ul>
 *     <li><code>B</code> representing the <code>byte</code> base type</li>
 *     <li><code>S</code> representing the <code>short</code> base type</li>
 *     <li><code>I</code> representing the <code>int</code> base type</li>
 *     <li><code>J</code> representing the <code>long</code> base type</li>
 *     <li><code>F</code> representing the <code>float</code> base type</li>
 *     <li><code>D</code> representing the <code>double</code> base type</li>
 *     <li><code>C</code> representing the <code>char</code> base type</li>
 *     <li><code>Z</code> representing the <code>boolean</code> base type</li>
 *     <li><code>V</code> representing void function return values</li>
 *     </ul>
 *     The name of a Class representing a base type
 *     is the keyword which is used to represent the
 *     type in Java source code (i.e. "int" for the
 *     <code>int</code> base type.</dd>
 * <dt><em>Classes representing array classes</em></dt>
 * <dd>These are Classes which represent the classes of
 *     Java arrays. There is one such Class for all array
 *     instances of a given arity (number of dimensions)
 *     and leaf component type. In this case, the name of the
 *     class is one or more left square brackets (one per
 *     dimension in the array) followed by the signature ofP
 *     the class representing the leaf component type, which
 *     can be either an object type or a base type. The
 *     signature of a Class representing an array type
 *     is the same as its name.</dd>
 * </dl>
 *
 * @author		OTI
 * @version		initial
 */
public final class Class<T> implements java.io.Serializable, GenericDeclaration, Type {
	private static final long serialVersionUID = 3206093459760846163L;
	private static ProtectionDomain AllPermissionsPD;
	private static final int SYNTHETIC = 0x1000;
	private static final int ANNOTATION = 0x2000;
	private static final int ENUM = 0x4000;
	private static final int MEMBER_INVALID_TYPE = -1;

	static final Class<?>[] EmptyParameters = new Class<?>[0];

	private long vmRef;
	private ClassLoader classLoader;

	private ProtectionDomain protectionDomain;
	private String classNameString;

	private static final class AnnotationVars {
		AnnotationVars() {}
		static long annotationTypeOffset = -1;
		static long valueMethodOffset = -1;

		volatile AnnotationType annotationType;
		MethodHandle valueMethod;
	}
	private transient AnnotationVars annotationVars;
	private static long annotationVarsOffset = -1;

	transient ClassValue.ClassValueMap classValueMap;

	private static final class EnumVars<T> {
		EnumVars() {}
		static long enumDirOffset = -1;
		static long enumConstantsOffset = -1;

		Map<String, T> cachedEnumConstantDirectory;
		T[] cachedEnumConstants;
	}
	private transient EnumVars<T> enumVars;
	private static long enumVarsOffset = -1;

	J9VMInternals.ClassInitializationLock initializationLock;

	private Object methodHandleCache;

	private transient ClassRepositoryHolder classRepoHolder;

	/* Helper class to hold the ClassRepository. We use a Class with a final
	 * field to ensure that we have both safe initialization and safe publication.
	 */
	private static final class ClassRepositoryHolder {
		static final ClassRepositoryHolder NullSingleton = new ClassRepositoryHolder(null);
		final ClassRepository classRepository;

		ClassRepositoryHolder(ClassRepository classRepo) {
			classRepository = classRepo;
		}
	}

	private static final class AnnotationCache {
		final Map<Class<? extends Annotation>, Annotation> directAnnotationMap;
		final Map<Class<? extends Annotation>, Annotation> annotationMap;
		AnnotationCache(
				Map<Class<? extends Annotation>, Annotation> directMap,
				Map<Class<? extends Annotation>, Annotation> annMap
		) {
			directAnnotationMap = directMap;
			annotationMap = annMap;
		}
	}
	private transient AnnotationCache annotationCache;
	private static long annotationCacheOffset = -1;
	private static boolean reflectCacheEnabled;
	private static boolean reflectCacheDebug;
	private static boolean reflectCacheAppOnly = true;

	static MethodHandles.Lookup implLookup;

	private static final sun.misc.Unsafe unsafe = sun.misc.Unsafe.getUnsafe();

	static sun.misc.Unsafe getUnsafe() {
		return unsafe;
	}

/**
 * Prevents this class from being instantiated. Instances
 * created by the virtual machine only.
 */
private Class() {}

/*
 * Ensure the caller has the requested type of access.
 *
 * @param		security			the current SecurityManager
 * @param		callerClassLoader	the ClassLoader of the caller of the original protected API
 * @param		type				type of access, PUBLIC, DECLARED or INVALID
 *
 */
void checkMemberAccess(SecurityManager security, ClassLoader callerClassLoader, int type) {
	if (callerClassLoader != ClassLoader.bootstrapClassLoader) {
		ClassLoader loader = getClassLoaderImpl();
		if (type == Member.DECLARED && callerClassLoader != loader) {
			security.checkPermission(RuntimePermission.permissionToAccessDeclaredMembers);
		}
		if (sun.reflect.misc.ReflectUtil.needsPackageAccessCheck(callerClassLoader, loader)) {
			if (Proxy.isProxyClass(this)) {
				sun.reflect.misc.ReflectUtil.checkProxyPackageAccess(callerClassLoader, this.getInterfaces());
			} else {
				String packageName = this.getPackageName();
				if (packageName != "") {	//$NON-NLS-1$
					security.checkPackageAccess(packageName);
				}
			}
		}
	}
}

/**
 * Ensure the caller has the requested type of access.
 *
 * This helper method is only called by getClasses, and skip security.checkPackageAccess()
 * when the class is a ProxyClass and the package name is sun.proxy.
 *
 * @param		type			type of access, PUBLIC or DECLARED
 *
 */
private void checkNonSunProxyMemberAccess(SecurityManager security, ClassLoader callerClassLoader, int type) {
	if (callerClassLoader != ClassLoader.bootstrapClassLoader) {
		ClassLoader loader = getClassLoaderImpl();
		if (type == Member.DECLARED && callerClassLoader != loader) {
			security.checkPermission(RuntimePermission.permissionToAccessDeclaredMembers);
		}
		String packageName = this.getPackageName();
		if (!(Proxy.isProxyClass(this) && packageName.equals(sun.reflect.misc.ReflectUtil.PROXY_PACKAGE)) &&
				packageName != "" && sun.reflect.misc.ReflectUtil.needsPackageAccessCheck(callerClassLoader, loader))	//$NON-NLS-1$
		{
			security.checkPackageAccess(packageName);
		}
	}
}

private static void forNameAccessCheck(final SecurityManager sm, final Class<?> callerClass, final Class<?> foundClass) {
	if (null != callerClass) {
		ProtectionDomain pd = callerClass.getPDImpl();
		if (null != pd) {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				@Override
				public Object run() {
					foundClass.checkMemberAccess(sm, callerClass.getClassLoaderImpl(), MEMBER_INVALID_TYPE);
					return null;
				}
			}, new AccessControlContext(new ProtectionDomain[]{pd}));
		}
	}
}

/**
 * Answers a Class object which represents the class
 * named by the argument. The name should be the name
 * of a class as described in the class definition of
 * java.lang.Class, however Classes representing base
 * types can not be found using this method.
 *
 * @param		className	The name of the non-base type class to find
 * @return		the named Class
 * @throws		ClassNotFoundException If the class could not be found
 *
 * @see			java.lang.Class
 */
@sun.reflect.CallerSensitive
public static Class<?> forName(String className) throws ClassNotFoundException {
	SecurityManager sm = null;
	/**
	 * Get the SecurityManager from System.  If the VM has not yet completed bootstrapping (i.e., J9VMInternals.initialized is still false)
	 * sm is kept as null without referencing System in order to avoid loading System earlier than necessary.
	 */
	if (J9VMInternals.initialized) {
		sm = System.getSecurityManager();
	}
	if (null == sm) {
		return forNameImpl(className, true, ClassLoader.callerClassLoader());
	}
	Class<?> caller = getStackClass(1);
	ClassLoader callerClassLoader = null;
	if (null != caller) {
		callerClassLoader = caller.getClassLoaderImpl();
	}
	Class<?> c = forNameImpl(className, false, callerClassLoader);
	forNameAccessCheck(sm, caller, c);
	J9VMInternals.initialize(c);
	return c;
}

AnnotationType getAnnotationType() {
	AnnotationVars localAnnotationVars = getAnnotationVars();
	return localAnnotationVars.annotationType;
}
void setAnnotationType(AnnotationType t) {
	AnnotationVars localAnnotationVars = getAnnotationVars();
	localAnnotationVars.annotationType = t;
}
/**
 * Compare-And-Swap the AnnotationType instance corresponding to this class.
 * (This method only applies to annotation types.)
 */
boolean casAnnotationType(AnnotationType oldType, AnnotationType newType) {
	AnnotationVars localAnnotationVars = getAnnotationVars();
	long localTypeOffset = AnnotationVars.annotationTypeOffset;
	if (-1 == localTypeOffset) {
		Field field = AccessController.doPrivileged(new PrivilegedAction<Field>() {
			public Field run() {
				try {
					return AnnotationVars.class.getDeclaredField("annotationType"); //$NON-NLS-1$
				} catch (Exception e) {
					throw newInternalError(e);
				}
			}
		});
		localTypeOffset = getUnsafe().objectFieldOffset(field);
		AnnotationVars.annotationTypeOffset = localTypeOffset;
	}
	return getUnsafe().compareAndSwapObject(localAnnotationVars, localTypeOffset, oldType, newType);
}

/**
 * Answers a Class object which represents the class
 * named by the argument. The name should be the name
 * of a class as described in the class definition of
 * java.lang.Class, however Classes representing base
 * types can not be found using this method.
 * Security rules will be obeyed.
 *
 * @param		className			The name of the non-base type class to find
 * @param		initializeBoolean	A boolean indicating whether the class should be
 *									initialized
 * @param		classLoader			The classloader to use to load the class
 * @return		the named class.
 * @throws		ClassNotFoundException If the class could not be found
 *
 * @see			java.lang.Class
 */
@sun.reflect.CallerSensitive
public static Class<?> forName(String className, boolean initializeBoolean, ClassLoader classLoader)
	throws ClassNotFoundException
{
	SecurityManager sm = null;
	if (J9VMInternals.initialized) {
		sm = System.getSecurityManager();
	}
	if (null == sm) {
		return forNameImpl(className, initializeBoolean, classLoader);
	}
	Class<?> caller = getStackClass(1);
	/* perform security checks */
	if (null == classLoader) {
		if (null != caller) {
			ClassLoader callerClassLoader = caller.getClassLoaderImpl();
			if (callerClassLoader != ClassLoader.bootstrapClassLoader) {
				/* only allowed if caller has RuntimePermission("getClassLoader") permission */
				sm.checkPermission(RuntimePermission.permissionToGetClassLoader);
			}
		}
	}
	Class<?> c = forNameImpl(className, false, classLoader);
	forNameAccessCheck(sm, caller, c);
	if (initializeBoolean) {
		J9VMInternals.initialize(c);
	}
	return c;
}

/**
 * Answers a Class object which represents the class
 * named by the argument. The name should be the name
 * of a class as described in the class definition of
 * java.lang.Class, however Classes representing base
 * types can not be found using this method.
 *
 * @param		className			The name of the non-base type class to find
 * @param		initializeBoolean	A boolean indicating whether the class should be
 *									initialized
 * @param		classLoader			The classloader to use to load the class
 * @return		the named class.
 * @throws		ClassNotFoundException If the class could not be found
 *
 * @see			java.lang.Class
 */
private static native Class<?> forNameImpl(String className,
                            boolean initializeBoolean,
                            ClassLoader classLoader)
	throws ClassNotFoundException;

/**
 * Answers an array containing all public class members
 * of the class which the receiver represents and its
 * superclasses and interfaces
 *
 * @return		the class' public class members
 * @throws		SecurityException If member access is not allowed
 *
 * @see			java.lang.Class
 */
@sun.reflect.CallerSensitive
public Class<?>[] getClasses() {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkNonSunProxyMemberAccess(security, callerClassLoader, Member.PUBLIC);
	}

	java.util.Vector<Class<?>> publicClasses = new java.util.Vector<>();
	Class<?> current = this;
	Class<?>[] classes;
	while(current != null) {
		classes = current.getDeclaredClassesImpl();
		for (int i = 0; i < classes.length; i++)
			if (Modifier.isPublic(classes[i].getModifiers()))
				publicClasses.addElement(classes[i]);
		current = current.getSuperclass();
	}
	classes = new Class<?>[publicClasses.size()];
	publicClasses.copyInto(classes);
	return classes;
}

/**
 * Answers the classloader which was used to load the
 * class represented by the receiver. Answer null if the
 * class was loaded by the system class loader
 *
 * @return		the receiver's class loader or nil
 *
 * @see			java.lang.ClassLoader
 */
@sun.reflect.CallerSensitive
public ClassLoader getClassLoader() {
	if (null != classLoader) {
		if (classLoader == ClassLoader.bootstrapClassLoader)	{
			return null;
		}
		SecurityManager security = System.getSecurityManager();
		if (null != security) {
			ClassLoader callersClassLoader = ClassLoader.callerClassLoader();
			if (ClassLoader.needsClassLoaderPermissionCheck(callersClassLoader, classLoader)) {
				security.checkPermission(RuntimePermission.permissionToGetClassLoader);
			}
		}
	}
	return classLoader;
}

/**
 * Answers the ClassLoader which was used to load the
 * class represented by the receiver. Answer null if the
 * class was loaded by the system class loader
 *
 * @return		the receiver's class loader or nil
 *
 * @see			java.lang.ClassLoader
 */
ClassLoader getClassLoader0() {
	ClassLoader loader = getClassLoaderImpl();
	return loader;
}

/**
 * Return the ClassLoader for this Class without doing any security
 * checks. The bootstrap ClassLoader is returned, unlike getClassLoader()
 * which returns null in place of the bootstrap ClassLoader.
 *
 * @return the ClassLoader
 *
 * @see ClassLoader#isASystemClassLoader()
 */
ClassLoader getClassLoaderImpl()
{
	return classLoader;
}

/**
 * Answers a Class object which represents the receiver's
 * component type if the receiver represents an array type.
 * Otherwise answers nil. The component type of an array
 * type is the type of the elements of the array.
 *
 * @return		the component type of the receiver.
 *
 * @see			java.lang.Class
 */
public native Class<?> getComponentType();

private NoSuchMethodException newNoSuchMethodException(String name, Class<?>[] types) {
	StringBuilder error = new StringBuilder();
	error.append(getName()).append('.').append(name).append('(');
	for (int i = 0; i < types.length; ++i) {
		if (i != 0) error.append(", "); //$NON-NLS-1$
		error.append(types[i] == null ? null : types[i].getName());
	}
	error.append(')');
	return new NoSuchMethodException(error.toString());
}

/**
 * Answers a public Constructor object which represents the
 * constructor described by the arguments.
 *
 * @param		parameterTypes	the types of the arguments.
 * @return		the constructor described by the arguments.
 * @throws		NoSuchMethodException if the constructor could not be found.
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getConstructors
 */
@sun.reflect.CallerSensitive
public Constructor<T> getConstructor(Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.PUBLIC);
	}

	if (parameterTypes == null) parameterTypes = EmptyParameters;
	Constructor<T> cachedConstructor = lookupCachedConstructor(parameterTypes);
	if (cachedConstructor != null && Modifier.isPublic(cachedConstructor.getModifiers())) {
		return cachedConstructor;
	}

	if (VM.PACKED_SUPPORT_ENABLED) {
		if (PackedObject.isPackedArray(this)) {
			throw newNoSuchMethodException("<init>", parameterTypes); //$NON-NLS-1$
		}
	}

	J9VMInternals.prepare(this);

	Constructor<T> rc;
	// Handle the default constructor case upfront
	if (parameterTypes.length == 0) {
		rc = getConstructorImpl(parameterTypes, "()V"); //$NON-NLS-1$
	} else {
		parameterTypes = parameterTypes.clone();
		// Build a signature for the requested method.
		String signature = getParameterTypesSignature("<init>", parameterTypes, "V"); //$NON-NLS-1$ //$NON-NLS-2$
		rc = getConstructorImpl(parameterTypes, signature);
		if (rc != null)
			rc = checkParameterTypes(rc, parameterTypes);
	}
	if (rc == null) throw newNoSuchMethodException("<init>", parameterTypes); //$NON-NLS-1$
	return cacheConstructor(rc);
}

/**
 * Answers a public Constructor object which represents the
 * constructor described by the arguments.
 *
 * @param		parameterTypes	the types of the arguments.
 * @param		signature		the signature of the method.
 * @return		the constructor described by the arguments.
 *
 * @see			#getConstructors
 */
private native Constructor<T> getConstructorImpl(Class<?> parameterTypes[], String signature);

/**
 * Answers an array containing Constructor objects describing
 * all constructors which are visible from the current execution
 * context.
 *
 * @return		all visible constructors starting from the receiver.
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Constructor<?>[] getConstructors() throws SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.PUBLIC);
	}

	Constructor<T>[] cachedConstructors = lookupCachedConstructors(CacheKey.PublicConstructorsKey);
	if (cachedConstructors != null) {
		return cachedConstructors;
	}

	if (VM.PACKED_SUPPORT_ENABLED) {
		if (PackedObject.isPackedArray(this)) {
			return cacheConstructors(new Constructor[0], CacheKey.PublicConstructorsKey);
		}
	}

	J9VMInternals.prepare(this);

	Constructor<T>[] ctors = getConstructorsImpl();

	return cacheConstructors(ctors, CacheKey.PublicConstructorsKey);
}

/**
 * Answers an array containing Constructor objects describing
 * all constructors which are visible from the current execution
 * context.
 *
 * @return		all visible constructors starting from the receiver.
 *
 * @see			#getMethods
 */
private native Constructor<T>[] getConstructorsImpl();

/**
 * Answers an array containing all class members of the class
 * which the receiver represents. Note that some of the fields
 * which are returned may not be visible in the current
 * execution context.
 *
 * @return		the class' class members
 * @throws		SecurityException if member access is not allowed
 *
 * @see			java.lang.Class
 */
@sun.reflect.CallerSensitive
public Class<?>[] getDeclaredClasses() throws SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkNonSunProxyMemberAccess(security, callerClassLoader, Member.DECLARED);
	}

	return getDeclaredClassesImpl();
}

/**
 * Answers an array containing all class members of the class
 * which the receiver represents. Note that some of the fields
 * which are returned may not be visible in the current
 * execution context.
 *
 * @return		the class' class members
 *
 * @see			java.lang.Class
 */
private native Class<?>[] getDeclaredClassesImpl();

/**
 * Answers a Constructor object which represents the
 * constructor described by the arguments.
 *
 * @param		parameterTypes	the types of the arguments.
 * @return		the constructor described by the arguments.
 * @throws		NoSuchMethodException if the constructor could not be found.
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getConstructors
 */
@sun.reflect.CallerSensitive
public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.DECLARED);
	}

	if (parameterTypes == null) parameterTypes = EmptyParameters;
	Constructor<T> cachedConstructor = lookupCachedConstructor(parameterTypes);
	if (cachedConstructor != null) {
		return cachedConstructor;
	}

	if (VM.PACKED_SUPPORT_ENABLED) {
		if (PackedObject.isPackedArray(this)) {
			throw newNoSuchMethodException("<init>", parameterTypes); //$NON-NLS-1$
		}
	}

	J9VMInternals.prepare(this);

	Constructor<T> rc;
	// Handle the default constructor case upfront
	if (parameterTypes.length == 0) {
		rc = getDeclaredConstructorImpl(parameterTypes, "()V"); //$NON-NLS-1$
	} else {
		parameterTypes = parameterTypes.clone();
		// Build a signature for the requested method.
		String signature = getParameterTypesSignature("<init>", parameterTypes, "V"); //$NON-NLS-1$ //$NON-NLS-2$
		rc = getDeclaredConstructorImpl(parameterTypes, signature);
		if (rc != null)
			rc = checkParameterTypes(rc, parameterTypes);
	}
	if (rc == null) throw newNoSuchMethodException("<init>", parameterTypes); //$NON-NLS-1$
	return cacheConstructor(rc);
}

/**
 * Answers a Constructor object which represents the
 * constructor described by the arguments.
 *
 * @param		parameterTypes	the types of the arguments.
 * @param		signature		the signature of the method.
 * @return		the constructor described by the arguments.
 *
 * @see			#getConstructors
 */
private native Constructor<T> getDeclaredConstructorImpl(Class<?>[] parameterTypes, String signature);

/**
 * Answers an array containing Constructor objects describing
 * all constructor which are defined by the receiver. Note that
 * some of the fields which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's constructors.
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Constructor<?>[] getDeclaredConstructors() throws SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.DECLARED);
	}

	Constructor<T>[] cachedConstructors = lookupCachedConstructors(CacheKey.DeclaredConstructorsKey);
	if (cachedConstructors != null) {
		return cachedConstructors;
	}

	if (VM.PACKED_SUPPORT_ENABLED) {
		if (PackedObject.isPackedArray(this)) {
			return cacheConstructors(new Constructor[0], CacheKey.DeclaredConstructorsKey);
		}
	}

	J9VMInternals.prepare(this);

	Constructor<T>[] ctors = getDeclaredConstructorsImpl();

	return cacheConstructors(ctors, CacheKey.DeclaredConstructorsKey);
}

/**
 * Answers an array containing Constructor objects describing
 * all constructor which are defined by the receiver. Note that
 * some of the fields which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's constructors.
 *
 * @see			#getMethods
 */
private native Constructor<T>[] getDeclaredConstructorsImpl();

/**
 * Answers a Field object describing the field in the receiver
 * named by the argument. Note that the Constructor may not be
 * visible from the current execution context.
 *
 * @param		name		The name of the field to look for.
 * @return		the field in the receiver named by the argument.
 * @throws		NoSuchFieldException if the requested field could not be found
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getDeclaredFields
 */
@sun.reflect.CallerSensitive
public Field getDeclaredField(String name) throws NoSuchFieldException, SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.DECLARED);
	}

	Field cachedField = lookupCachedField(name);
	if (cachedField != null && cachedField.getDeclaringClass() == this) {
		return cachedField;
	}

	J9VMInternals.prepare(this);

	Field field = getDeclaredFieldImpl(name);

	if (VM.PACKED_SUPPORT_ENABLED) {
		if (fieldRequiresPacked(field)) {
			throw new NoSuchFieldException(name);
		}
	}

	Field[] fields = sun.reflect.Reflection.filterFields(this, new Field[] {field});
	if (0 == fields.length) {
		throw new NoSuchFieldException(name);
	}
	return cacheField(fields[0]);
}

/**
 * Answers a Field object describing the field in the receiver
 * named by the argument. Note that the Constructor may not be
 * visible from the current execution context.
 *
 * @param		name		The name of the field to look for.
 * @return		the field in the receiver named by the argument.
 * @throws		NoSuchFieldException If the given field does not exist
 *
 * @see			#getDeclaredFields
 */
private native Field getDeclaredFieldImpl(String name) throws NoSuchFieldException;

/**
 * Answers an array containing Field objects describing
 * all fields which are defined by the receiver. Note that
 * some of the fields which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's fields.
 * @throws		SecurityException If member access is not allowed
 *
 * @see			#getFields
 */
@sun.reflect.CallerSensitive
public Field[] getDeclaredFields() throws SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.DECLARED);
	}

	Field[] cachedFields = lookupCachedFields(CacheKey.DeclaredFieldsKey);
	if (cachedFields != null) {
		return cachedFields;
	}

	J9VMInternals.prepare(this);

	Field[] fields = getDeclaredFieldsImpl();

	if (VM.PACKED_SUPPORT_ENABLED) {
		fields = filterPackedFields(fields);
	}

	return cacheFields(sun.reflect.Reflection.filterFields(this, fields), CacheKey.DeclaredFieldsKey);
}

/**
 * Answers an array containing Field objects describing
 * all fields which are defined by the receiver. Note that
 * some of the fields which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's fields.
 *
 * @see			#getFields
 */
private native Field[] getDeclaredFieldsImpl();

/**
 * Answers a Method object which represents the method
 * described by the arguments. Note that the associated
 * method may not be visible from the current execution
 * context.
 *
 * @param		name			the name of the method
 * @param		parameterTypes	the types of the arguments.
 * @return		the method described by the arguments.
 * @throws		NoSuchMethodException if the method could not be found.
 * @throws		SecurityException If member access is not allowed
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Method getDeclaredMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.DECLARED);
	}

	Method result, bestCandidate;
	int maxDepth;
	String strSig;
	if (parameterTypes == null) parameterTypes = EmptyParameters;
	Method cachedMethod = lookupCachedMethod(name, parameterTypes);
	if (cachedMethod != null && cachedMethod.getDeclaringClass() == this) {
		return cachedMethod;
	}

	J9VMInternals.prepare(this);

	// Handle the no parameter case upfront
	if (name == null || parameterTypes.length == 0) {
		strSig = "()"; //$NON-NLS-1$
		parameterTypes = EmptyParameters;
	} else {
		parameterTypes = parameterTypes.clone();
		// Build a signature for the requested method.
		strSig = getParameterTypesSignature(name, parameterTypes, ""); //$NON-NLS-1$
	}

	result = getDeclaredMethodImpl(name, parameterTypes, strSig, null);
	if (result == null) {
		throw newNoSuchMethodException(name, parameterTypes);
	}
	if (0 == sun.reflect.Reflection.filterMethods(this, new Method[] {result}).length) {
		throw newNoSuchMethodException(name, parameterTypes);
	}

	if (VM.PACKED_SUPPORT_ENABLED) {
		/* We do this check after getting the method so that we can
		 * verify whether the method's return type requires packed.
		 */
		if (PackedObject.isPackedArray(this)) {
			throw newNoSuchMethodException(name, parameterTypes);
		}
	}

	if (parameterTypes.length > 0) {
		// the result.getDeclaringClass() is always the same as this class for getDeclaredMethod()
		ClassLoader loader = getClassLoaderImpl();
		for (int i = 0; i < parameterTypes.length; ++i) {
			Class<?> parameterType = parameterTypes[i];
			if (!parameterType.isPrimitive()) {
				try {
					if (Class.forName(parameterType.getName(), false, loader) != parameterType) {
						throw newNoSuchMethodException(name, parameterTypes);
					}
				} catch(ClassNotFoundException e) {
					throw newNoSuchMethodException(name, parameterTypes);
				}
			}
		}
	}

	/* [PR 113003] The native is called repeatedly until it returns null,
	 * as each call returns another match if one exists.
	 * If more than one match is found, the code below selects the
	 * candidate method whose return type has the largest depth. The latter
	 * case is expected to occur only in certain JCK tests, as most Java
	 * compilers will refuse to produce a class file with multiple methods
	 * of the same name differing only in return type.
	 *
	 * Selecting by largest depth is one possible algorithm that satisfies the
	 * spec.
	 */
	bestCandidate = result;
	maxDepth = result.getReturnType().getClassDepth();
	while( true ) {
		result = getDeclaredMethodImpl(name, parameterTypes, strSig, result);
		if( result == null ) {
			break;
		}
		int resultDepth = result.getReturnType().getClassDepth();
		if( resultDepth > maxDepth ) {
			bestCandidate = result;
			maxDepth = resultDepth;
		}
	}
	return cacheMethod(bestCandidate);
}

/**
 * This native iterates over methods matching the provided name and signature
 * in the receiver class. The startingPoint parameter is passed the last
 * method returned (or null on the first use), and the native returns the next
 * matching method or null if there are no more matches.
 * Note that the associated method may not be visible from the
 * current execution context.
 *
 * @param		name				the name of the method
 * @param		parameterTypes		the types of the arguments.
 * @param		partialSignature	the signature of the method, without return type.
 * @param		startingPoint		the method to start searching after, or null to start at the beginning
 * @return		the next Method described by the arguments
 *
 * @see			#getMethods
 */
private native Method getDeclaredMethodImpl(String name, Class<?>[] parameterTypes, String partialSignature, Method startingPoint);

/**
 * Answers an array containing Method objects describing
 * all methods which are defined by the receiver. Note that
 * some of the methods which are returned may not be visible
 * in the current execution context.
 *
 * @throws		SecurityException	if member access is not allowed
 * @return		the receiver's methods.
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Method[] getDeclaredMethods() throws SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.DECLARED);
	}

	Method[] cachedMethods = lookupCachedMethods(CacheKey.DeclaredMethodsKey);
	if (cachedMethods != null) {
		return cachedMethods;
	}

	if (VM.PACKED_SUPPORT_ENABLED) {
		if (PackedObject.isPackedArray(this)) {
			return cacheMethods(new Method[0], CacheKey.DeclaredMethodsKey);
		}
	}

	J9VMInternals.prepare(this);

	return cacheMethods(sun.reflect.Reflection.filterMethods(this, getDeclaredMethodsImpl()), CacheKey.DeclaredMethodsKey);
}

/**
 * Answers an array containing Method objects describing
 * all methods which are defined by the receiver. Note that
 * some of the methods which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's methods.
 *
 * @see			#getMethods
 */
private native Method[] getDeclaredMethodsImpl();

/**
 * Answers the class which declared the class represented
 * by the receiver. This will return null if the receiver
 * is a member of another class.
 *
 * @return		the declaring class of the receiver.
 */
@sun.reflect.CallerSensitive
public Class<?> getDeclaringClass() {
	Class<?> declaringClass = getDeclaringClassImpl();
	if (declaringClass == null) {
		return declaringClass;
	}
	if (declaringClass.isClassADeclaredClass(this)) {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
			declaringClass.checkMemberAccess(security, callerClassLoader, MEMBER_INVALID_TYPE);
		}
		return declaringClass;
	}

	// K0555 = incompatible InnerClasses attribute between "{0}" and "{1}"
	throw new IncompatibleClassChangeError(
			com.ibm.oti.util.Msg.getString("K0555", this.getName(),	declaringClass.getName())); //$NON-NLS-1$
}
/**
 * Returns true if the class passed in to the method is a declared class of
 * this class.
 *
 * @param		aClass		The class to validate
 * @return		true if aClass a declared class of this class
 * 				false otherwise.
 *
 */
private native boolean isClassADeclaredClass(Class<?> aClass);

/**
 * Answers the class which declared the class represented
 * by the receiver. This will return null if the receiver
 * is a member of another class.
 *
 * @return		the declaring class of the receiver.
 */
private native Class<?> getDeclaringClassImpl();

/**
 * Answers a Field object describing the field in the receiver
 * named by the argument which must be visible from the current
 * execution context.
 *
 * @param		name		The name of the field to look for.
 * @return		the field in the receiver named by the argument.
 * @throws		NoSuchFieldException If the given field does not exist
 * @throws		SecurityException If access is denied
 *
 * @see			#getDeclaredFields
 */
@sun.reflect.CallerSensitive
public Field getField(String name) throws NoSuchFieldException, SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.PUBLIC);
	}

	Field cachedField = lookupCachedField(name);
	if (cachedField != null && Modifier.isPublic(cachedField.getModifiers())) {
		return cachedField;
	}

	J9VMInternals.prepare(this);

	Field field = getFieldImpl(name);

	if (VM.PACKED_SUPPORT_ENABLED) {
		if (fieldRequiresPacked(field)) {
			throw new NoSuchFieldException(name);
		}
	}
	if (0 == sun.reflect.Reflection.filterFields(this, new Field[] {field}).length) {
		throw new NoSuchFieldException(name);
	}

	return cacheField(field);
}

/**
 * Answers a Field object describing the field in the receiver
 * named by the argument which must be visible from the current
 * execution context.
 *
 * @param		name		The name of the field to look for.
 * @return		the field in the receiver named by the argument.
 * @throws		NoSuchFieldException If the given field does not exist
 *
 * @see			#getDeclaredFields
 */
private native Field getFieldImpl(String name) throws NoSuchFieldException;

/**
 * Answers an array containing Field objects describing
 * all fields which are visible from the current execution
 * context.
 *
 * @return		all visible fields starting from the receiver.
 * @throws		SecurityException If member access is not allowed
 *
 * @see			#getDeclaredFields
 */
@sun.reflect.CallerSensitive
public Field[] getFields() throws SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.PUBLIC);
	}

	Field[] cachedFields = lookupCachedFields(CacheKey.PublicFieldsKey);
	if (cachedFields != null) {
		return cachedFields;
	}

	J9VMInternals.prepare(this);

	Field[] fields = getFieldsImpl();

	if (VM.PACKED_SUPPORT_ENABLED) {
		fields = filterPackedFields(fields);
	}

	return cacheFields(sun.reflect.Reflection.filterFields(this, fields), CacheKey.PublicFieldsKey);
}

/**
 * Answers an array containing Field objects describing
 * all fields which are visible from the current execution
 * context.
 *
 * @return		all visible fields starting from the receiver.
 *
 * @see			#getDeclaredFields
 */
private native Field[] getFieldsImpl();

/**
 * Answers an array of Class objects which match the interfaces
 * specified in the receiver classes <code>implements</code>
 * declaration
 *
 * @return		Class<?>[]
 *					the interfaces the receiver claims to implement.
 */
public Class<?>[] getInterfaces()
{
	return J9VMInternals.getInterfaces(this);
}

/**
 * Answers a Method object which represents the method
 * described by the arguments.
 *
 * @param		name String
 *					the name of the method
 * @param		parameterTypes Class<?>[]
 *					the types of the arguments.
 * @return		Method
 *					the method described by the arguments.
 * @throws	NoSuchMethodException
 *					if the method could not be found.
 * @throws	SecurityException
 *					if member access is not allowed
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.PUBLIC);
	}

	Method result, bestCandidate;
	int maxDepth;
	String strSig;

	if (parameterTypes == null) parameterTypes = EmptyParameters;
	Method cachedMethod = lookupCachedMethod(name, parameterTypes);
	if (cachedMethod != null && Modifier.isPublic(cachedMethod.getModifiers())) {
		return cachedMethod;
	}

	J9VMInternals.prepare(this);

	// Handle the no parameter case upfront
	if (name == null || parameterTypes.length == 0) {
		strSig = "()"; //$NON-NLS-1$
		parameterTypes = EmptyParameters;
	} else {
		parameterTypes = parameterTypes.clone();
		// Build a signature for the requested method.
		strSig = getParameterTypesSignature(name, parameterTypes, ""); //$NON-NLS-1$
	}
	result = getMethodImpl(name, parameterTypes, strSig);
	if (result == null) {
		throw newNoSuchMethodException(name, parameterTypes);
	}
	if (0 == sun.reflect.Reflection.filterMethods(this, new Method[] {result}).length) {
		throw newNoSuchMethodException(name, parameterTypes);
	}

	if (VM.PACKED_SUPPORT_ENABLED) {
		/* We do this check after getting the method so that we can
		 * verify whether the method's return type requires packed.
		 */
		if (PackedObject.isPackedArray(this)) {
			throw newNoSuchMethodException(name, parameterTypes);
		}
	}

	if (parameterTypes.length > 0) {
		ClassLoader loader = result.getDeclaringClass().getClassLoaderImpl();
		for (int i = 0; i < parameterTypes.length; ++i) {
			Class<?> parameterType = parameterTypes[i];
			if (!parameterType.isPrimitive()) {
				try {
					if (Class.forName(parameterType.getName(), false, loader) != parameterType) {
						throw newNoSuchMethodException(name, parameterTypes);
					}
				} catch(ClassNotFoundException e) {
					throw newNoSuchMethodException(name, parameterTypes);
				}
			}
		}
	}

	/* [PR 113003] The native is called repeatedly until it returns null,
	 * as each call returns another match if one exists. The first call uses
	 * getMethodImpl which searches across superclasses and interfaces, but
	 * since the spec requires that we only weigh multiple matches against
	 * each other if they are in the same class, on subsequent calls we call
	 * getDeclaredMethodImpl on the declaring class of the first hit.
	 * If more than one match is found, the code below selects the
	 * candidate method whose return type has the largest depth. This case
	 * case is expected to occur only in certain JCK tests, as most Java
	 * compilers will refuse to produce a class file with multiple methods
	 * of the same name differing only in return type.
	 *
	 * Selecting by largest depth is one possible algorithm that satisfies the
	 * spec.
	 */
	bestCandidate = result;
	maxDepth = result.getReturnType().getClassDepth();
	Class<?> declaringClass = result.getDeclaringClass();
	while( true ) {
		result = declaringClass.getDeclaredMethodImpl(name, parameterTypes, strSig, result);
		if( result == null ) {
			break;
		}
		if( (result.getModifiers() & Modifier.PUBLIC) != 0 ) {
			int resultDepth = result.getReturnType().getClassDepth();
			if( resultDepth > maxDepth ) {
				bestCandidate = result;
				maxDepth = resultDepth;
			}
		}
	}

	return cacheMethod(bestCandidate);
}

/**
 * Answers a Method object which represents the first method found matching
 * the arguments.
 *
 * @param		name String
 *					the name of the method
 * @param		parameterTypes Class<?>[]
 *					the types of the arguments.
 * @param		partialSignature String
 *					the signature of the method, without return type.
 * @return		Object
 *					the first Method found matching the arguments
 *
 * @see			#getMethods
 */
private native Method getMethodImpl(String name, Class<?>[] parameterTypes, String partialSignature);

/**
 * Answers an array containing Method objects describing
 * all methods which are visible from the current execution
 * context.
 *
 * @return		Method[]
 *					all visible methods starting from the receiver.
 * @throws	SecurityException
 *					if member access is not allowed
 *
 * @see			#getDeclaredMethods
 */
@sun.reflect.CallerSensitive
public Method[] getMethods() throws SecurityException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkMemberAccess(security, callerClassLoader, Member.PUBLIC);
	}

	Method[] methods;

	methods = lookupCachedMethods(CacheKey.PublicMethodsKey);
	if (methods != null) {
		return methods;
	}

	if(isPrimitive()) return new Method[0];

	if (VM.PACKED_SUPPORT_ENABLED) {
		if (PackedObject.isPackedArray(this)) {
			return cacheMethods(new Method[0], CacheKey.PublicMethodsKey);
		}
	}

	J9VMInternals.prepare(this);

	if(isInterface())
	{
		return cacheMethods(sun.reflect.Reflection.filterMethods(this, getInterfaceMethodsImpl()), CacheKey.PublicMethodsKey);
	}
	else
	{
		int vCount = 0;
		int sCount = 0;

		do {
			vCount = getVirtualMethodCountImpl();
			sCount = getStaticMethodCountImpl();
			methods = (Method[])Method.class.allocateAndFillArray(vCount + sCount);
			if ((true == getVirtualMethodsImpl(methods, 0, vCount)) && (true == getStaticMethodsImpl(methods, vCount, sCount))) {
				break;
			}
		} while (true);

		for (int index = 0; index < vCount; index++) {
			if (methods[index] != null && methods[index].getDeclaringClass().isInterface()) {
				// there is an abstract methods
				Method[] interfaceMethods = getClassInterfaceMethodsImpl();
				int count = interfaceMethods.length;
				for (int i = 0; i < interfaceMethods.length; i++) {
					if (interfaceMethods[i] != null) {
						for (int j = 0; j < vCount; j++) {
							if (methodsEqual(interfaceMethods[i], methods[j])) {
								Class<?> declaringClass = methods[j].getDeclaringClass();
								if (declaringClass.isInterface()) {
									if (!declaringClass.equals(interfaceMethods[i].getDeclaringClass())) {
										//	this is an extra interface method not returned by getVirtualMethodsImpl()
										//	it will be added to methodResult
										continue;
									}
								}

								interfaceMethods[i] = null;
								count--;
								break;
							}
						}
					}
				}

				int methodsLength = methods.length;
				Method[] methodResult = new Method[methodsLength + count];
				System.arraycopy(methods, 0, methodResult, 0, methodsLength);
				int appendIndex = 0;
				for(int k = 0; k < interfaceMethods.length; k++) {
					if (interfaceMethods[k] != null) {
						methodResult[methodsLength + appendIndex] = interfaceMethods[k];
						appendIndex++;
					}
				}

				return cacheMethods(sun.reflect.Reflection.filterMethods(this, methodResult), CacheKey.PublicMethodsKey);
			}
		}

		return cacheMethods(sun.reflect.Reflection.filterMethods(this, methods), CacheKey.PublicMethodsKey);
	}
}

private boolean methodsEqual(Method m1, Method m2) {
	if(!m1.getName().equals(m2.getName())) {
		return false;
	}
	if(!m1.getReturnType().equals(m2.getReturnType())) {
		return false;
	}
	Class<?>[] m1Parms = m1.getParameterTypes();
	Class<?>[] m2Parms = m2.getParameterTypes();
	if(m1Parms.length != m2Parms.length) {
		return false;
	}
	for(int i = 0; i < m1Parms.length; i++) {
		if(m1Parms[i] != m2Parms[i]) {
			return false;
		}
	}
	return true;
}

private int getInterfaceMethodCountImpl()
{
	int count = getDeclaredMethods().length;
	Class<?>[] parents = getInterfaces();
	for(int i = 0; i < parents.length; i++) {
		count += parents[i].getInterfaceMethodCountImpl();
	}
	return count;
}

private Method[] getInterfaceMethodsImpl()
{
	Class<?>[] parents;
	/**
	 * %local% is a local methods array returned from getDeclaredMethods()
	 * %scratch% is a temporary array with a pessimal amount of space,
	 * 	abstract local methods, non-null interface methods inherited from super-interfaces are copied to scratch sequentially,
	 * 	and eventually all non-null methods entries in scratch are copied to unique array which has exactly number of these non-null entries.
	 * %unique% array is returned as result of this getInterfaceMethodsImpl()
	 */
	Method[] scratch, unique, local;
	/**
	 * %index% points to the entry after latest non-null method entry added to scratch array
	 * 	or the number of non-null method entries already added to scratch array
	 * %localIndex% points to the entry after last local abstract method entry copied to scratch array
	 * 	or the number of local abstract method entries added to scratch array
	 * %indexPrevBlock% points to the entry after last interface method entry added to scratch which was returned by a call to getInterfaceMethodsImpl(),
	 * 	and following interface methods to be added will be returned by another call to getInterfaceMethodsImpl() for other super interfaces.
	 */
	int index, localIndex, indexPrevBlock;

	/* Get a pessimal amount of scratch space */
	scratch = new Method[getInterfaceMethodCountImpl()];

	/* Obtain the local methods. These are guaranteed to be returned */
	local = getDeclaredMethods();
	index = 0;
	for(int i = 0; i < local.length; i++) {
		/* <clinit> is never abstract */
		if(Modifier.isPublic(local[i].getModifiers())) {
			scratch[index++] = local[i];
		}
	}
	localIndex = index;
	indexPrevBlock = index;

	/* Walk each superinterface */
	parents = getInterfaces();
	for(int i = 0; i < parents.length; i++) {
		/* Get the superinterface's (swept) methods */
		Method[] parentMethods = parents[i].getInterfaceMethodsImpl();
		for (int j = 0; j < parentMethods.length; j++) {
			/* [PR 65639] Added modifier check to prevent returning public static methods of superinterfaces
			 * because according to the spec, static methods in interfaces are not inherited*/
			if ((parentMethods[j] != null) && (!(Modifier.isStatic(parentMethods[j].getModifiers())))) {
				/* Sweep out any local overrides */
				boolean	redundant = false;
				for (int k = 0; k < localIndex; k++) {
					if(methodsEqual(scratch[k], parentMethods[j])) {
						//	found local override
						redundant = true;
						break;
					}
				}
				if (!redundant) {
					for (int k = localIndex; k < indexPrevBlock; k++) {
						if (scratch[k].equals(parentMethods[j])) {
							//	found a dup
							redundant = true;
							break;
						}
					}
				}
				if (!redundant) {
					//	non-null interface methods, no override, no dup
					scratch[index] = parentMethods[j];
					index++;
				}
			}
		}
		indexPrevBlock = index;
	}

	/* Copy into a correctly sized array and return */
	unique = new Method[index];
	System.arraycopy(scratch, 0, unique, 0, index);

	return unique;
}

/* return the number of interface methods inherited from parent classes & interfaces */
private int getClassInterfaceMethodsCountImpl() {
	Class<?> parent;
	Class<?>[] interfaces;
	int count = 0;

	parent = this.getSuperclass();
	if (parent != null && !parent.equals(Object.class)) {
		count += parent.getClassInterfaceMethodsCountImpl();
	}

	interfaces = getInterfaces();
	for(int i = 0; i < interfaces.length; i++) {
		count += interfaces[i].getInterfaceMethodCountImpl();
	}
	return count;
}

/* return an array of interface methods inherited from parent classes & interfaces */
private Method[] getClassInterfaceMethodsImpl()
{
	Method[] unique, scratch, parentsMethods;
	Class<?> parent;
	Class<?>[] interfaces;
	int index = 0, indexPrevBlock = 0;

	scratch = new Method[getClassInterfaceMethodsCountImpl()];
	parent = this.getSuperclass();
	if (parent != null && !parent.equals(Object.class)) {
		parentsMethods = parent.getClassInterfaceMethodsImpl();
		System.arraycopy(parentsMethods, 0, scratch, 0, parentsMethods.length);
		index = parentsMethods.length;
		indexPrevBlock = index;
	}

	/* Walk each superinterface */
	interfaces = getInterfaces();
	for(int i = 0; i < interfaces.length; i++) {
		Method[] interfaceMethods = interfaces[i].getInterfaceMethodsImpl();

		if (index == 0) {
			System.arraycopy(interfaceMethods, 0, scratch, 0, interfaceMethods.length);
			index = interfaceMethods.length;
			indexPrevBlock = index;
		} else {
			/* 	Not check override here, interface methods overrided will be removed within getMethods()
			 *  when comparing interface methods returned from this methods with virtual methods returned
			 *  from getVirtualMethodsImpl()
			 */
			/* Sweep out any duplicates */
			for(int j = 0; j < interfaceMethods.length; j++) {
				//	compare with interface methods inherited from parent classes and
				//	other interfaces already added to scratch
				if (interfaceMethods[j] != null) {
					boolean redundant = false;
					for(int k = 0; k < indexPrevBlock; k++) {
						if((scratch[k] != null) && interfaceMethods[j].equals(scratch[k])) {
							//	found a dup
							redundant = true;
							break;
						}
					}
					if (!redundant) {
						// add to scratch
						scratch[index] = interfaceMethods[j];
						index++;
					}
				}
			}
			indexPrevBlock = index;
		}
	}

	unique = new Method[index];
	System.arraycopy(scratch, 0, unique, 0, index);

	return unique;
}

private native int getVirtualMethodCountImpl();
private native boolean getVirtualMethodsImpl(Method[] array, int start, int count);
private native int getStaticMethodCountImpl();
private native boolean getStaticMethodsImpl(Method[] array, int start, int count);
private native Object[] allocateAndFillArray(int size);

/**
 * Answers an integer which which is the receiver's modifiers.
 * Note that the constants which describe the bits which are
 * returned are implemented in class java.lang.reflect.Modifier
 * which may not be available on the target.
 *
 * @return		the receiver's modifiers
 */
public int getModifiers() {
	int rawModifiers = getModifiersImpl();
	if (isArray()) {
		rawModifiers &= Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED |
				Modifier.ABSTRACT | Modifier.FINAL;
	} else {
		rawModifiers &= Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED |
					Modifier.STATIC | Modifier.FINAL | Modifier.INTERFACE |
					Modifier.ABSTRACT | SYNTHETIC | ENUM | ANNOTATION;
	}
	return rawModifiers;
}

private native int getModifiersImpl();

/**
 * Answers the name of the class which the receiver represents.
 * For a description of the format which is used, see the class
 * definition of java.lang.Class.
 *
 * @return		the receiver's name.
 *
 * @see			java.lang.Class
 */
public String getName() {
	String name = classNameString;
	if (name != null){
		return name;
	}
	//must have been null to set it
	name = VM.getClassNameImpl(this).intern();
	classNameString = name;
	return name;
}

/**
 * Answers the ProtectionDomain of the receiver.
 * <p>
 * Note: In order to conserve space in embedded targets, we allow this
 * method to answer null for classes in the system protection domain
 * (i.e. for system classes). System classes are always given full
 * permissions (i.e. AllPermission). This is not changeable via the
 * java.security.Policy.
 *
 * @return		ProtectionDomain
 *					the receiver's ProtectionDomain.
 *
 * @see			java.lang.Class
 */
public ProtectionDomain getProtectionDomain() {
	SecurityManager security = System.getSecurityManager();
	if (security != null)
		security.checkPermission(
				RuntimePermission.permissionToGetProtectionDomain);

	ProtectionDomain result = getPDImpl();
	if (result != null) return result;

	if (AllPermissionsPD == null) {
		allocateAllPermissionsPD();
	}
	return AllPermissionsPD;
}

private void allocateAllPermissionsPD() {
	Permissions collection = new Permissions();
	collection.add(new AllPermission());
	AllPermissionsPD = new ProtectionDomain(null, collection);
}

/**
 * Answers the ProtectionDomain of the receiver.
 * <p>
 * This method is for internal use only.
 *
 * @return		ProtectionDomain
 *					the receiver's ProtectionDomain.
 *
 * @see			java.lang.Class
 */
ProtectionDomain getPDImpl() {
	return protectionDomain;
}

/**
 * Answers the name of the package to which the receiver belongs.
 * For example, Object.class.getPackageName() returns "java.lang".
 *
 * @return		the receiver's package name.
 *
 * @see			#getPackage
 */
String getPackageName() {
	String name = getName();
	int index = name.lastIndexOf('.');
	if (index >= 0) return name.substring(0, index);
	return ""; //$NON-NLS-1$
}

/**
 * Answers a read-only stream on the contents of the
 * resource specified by resName. The mapping between
 * the resource name and the stream is managed by the
 * class' class loader.
 *
 * @param		resName 	the name of the resource.
 * @return		a stream on the resource.
 *
 * @see			java.lang.ClassLoader
 */
public URL getResource(String resName) {
	ClassLoader loader = this.getClassLoaderImpl();
	if (loader == ClassLoader.bootstrapClassLoader)
		return ClassLoader.getSystemResource(this.toResourceName(resName));
	else
		return loader.getResource(this.toResourceName(resName));
}

/**
 * Answers a read-only stream on the contents of the
 * resource specified by resName. The mapping between
 * the resource name and the stream is managed by the
 * class' class loader.
 *
 * @param		resName		the name of the resource.
 * @return		a stream on the resource.
 *
 * @see			java.lang.ClassLoader
 */
public InputStream getResourceAsStream(String resName) {
	ClassLoader loader = this.getClassLoaderImpl();
	if (loader == ClassLoader.bootstrapClassLoader)
		return ClassLoader.getSystemResourceAsStream(this.toResourceName(resName));
	else
		return loader.getResourceAsStream(this.toResourceName(resName));
}

/**
 * Answers a String object which represents the class's
 * signature, as described in the class definition of
 * java.lang.Class.
 *
 * @return		the signature of the class.
 *
 * @see			java.lang.Class
 */
private String getSignature() {
	if(isArray()) return getName(); // Array classes are named with their signature
	if(isPrimitive()) {
		// Special cases for each base type.
		if(this == void.class) return "V"; //$NON-NLS-1$
		if(this == boolean.class) return "Z"; //$NON-NLS-1$
		if(this == byte.class) return "B"; //$NON-NLS-1$
		if(this == char.class) return "C"; //$NON-NLS-1$
		if(this == short.class) return "S"; //$NON-NLS-1$
		if(this == int.class) return "I"; //$NON-NLS-1$
		if(this == long.class) return "J"; //$NON-NLS-1$
		if(this == float.class) return "F"; //$NON-NLS-1$
		if(this == double.class) return "D"; //$NON-NLS-1$
	}

	// General case.
	// Create a buffer of the correct size
	String name = getName();
	return new StringBuilder(name.length() + 2).
		append('L').append(name).append(';').toString();
}

/**
 * Answers the signers for the class represented by the
 * receiver, or null if there are no signers.
 *
 * @return		the signers of the receiver.
 *
 * @see			#getMethods
 */
public Object[] getSigners() {
	 return getClassLoaderImpl().getSigners(this);
}

/**
 * Answers the Class which represents the receiver's
 * superclass. For Classes which represent base types,
 * interfaces, and for java.lang.Object the method
 * answers null.
 *
 * @return		the receiver's superclass.
 */
public Class<? super T> getSuperclass()
{
	return J9VMInternals.getSuperclass(this);
}

/**
 * Answers true if the receiver represents an array class.
 *
 * @return		<code>true</code>
 *					if the receiver represents an array class
 *              <code>false</code>
 *                  if it does not represent an array class
 */
public native boolean isArray();

/**
 * Answers true if the type represented by the argument
 * can be converted via an identity conversion or a widening
 * reference conversion (i.e. if either the receiver or the
 * argument represent primitive types, only the identity
 * conversion applies).
 *
 * @return		<code>true</code>
 *					the argument can be assigned into the receiver
 *              <code>false</code>
 *					the argument cannot be assigned into the receiver
 * @param		cls	Class
 *					the class to test
 * @throws	NullPointerException
 *					if the parameter is null
 *
 */
public native boolean isAssignableFrom(Class<?> cls);

/**
 * Answers true if the argument is non-null and can be
 * cast to the type of the receiver. This is the runtime
 * version of the <code>instanceof</code> operator.
 *
 * @return		<code>true</code>
 *					the argument can be cast to the type of the receiver
 *              <code>false</code>
 *					the argument is null or cannot be cast to the
 *					type of the receiver
 *
 * @param		object Object
 *					the object to test
 */
public native boolean isInstance(Object object);

/**
 * Answers true if the receiver represents an interface.
 *
 * @return		<code>true</code>
 *					if the receiver represents an interface
 *              <code>false</code>
 *                  if it does not represent an interface
 */
public boolean isInterface() {
	// This code has been inlined in toGenericString. toGenericString
	// must be modified to reflect any changes to this implementation.
	return !isArray() && (getModifiersImpl() & 512 /* AccInterface */) != 0;
}

/**
 * Answers true if the receiver represents a base type.
 *
 * @return		<code>true</code>
 *					if the receiver represents a base type
 *              <code>false</code>
 *                  if it does not represent a base type
 */
public native boolean isPrimitive();

/**
 * Answers a new instance of the class represented by the
 * receiver, created by invoking the default (i.e. zero-argument)
 * constructor. If there is no such constructor, or if the
 * creation fails (either because of a lack of available memory or
 * because an exception is thrown by the constructor), an
 * InstantiationException is thrown. If the default constructor
 * exists, but is not accessible from the context where this
 * message is sent, an IllegalAccessException is thrown.
 *
 * @return		a new instance of the class represented by the receiver.
 * @throws		IllegalAccessException if the constructor is not visible to the sender.
 * @throws		InstantiationException if the instance could not be created.
 */
@sun.reflect.CallerSensitive
public T newInstance() throws IllegalAccessException, InstantiationException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
		checkNonSunProxyMemberAccess(security, callerClassLoader, Member.PUBLIC);
	}

	return (T)J9VMInternals.newInstanceImpl(this);
}

/**
 * Used as a prototype for the jit.
 *
 * @param 		callerClass
 * @return		the object
 * @throws 		InstantiationException
 */
private Object newInstancePrototype(Class<?> callerClass) throws InstantiationException {
	throw new InstantiationException(this);
}

/**
 * Answers a string describing a path to the receiver's appropriate
 * package specific subdirectory, with the argument appended if the
 * argument did not begin with a slash. If it did, answer just the
 * argument with the leading slash removed.
 *
 * @return		String
 *					the path to the resource.
 * @param		resName	String
 *					the name of the resource.
 *
 * @see			#getResource
 * @see			#getResourceAsStream
 */
private String toResourceName(String resName) {
	// Turn package name into a directory path
	if (resName.length() > 0 && resName.charAt(0) == '/')
		return resName.substring(1);

	String qualifiedClassName = getName();
	int classIndex = qualifiedClassName.lastIndexOf('.');
	if (classIndex == -1) return resName; // from a default package
	return qualifiedClassName.substring(0, classIndex + 1).replace('.', '/') + resName;
}

/**
 * Answers a string containing a concise, human-readable
 * description of the receiver.
 *
 * @return		a printable representation for the receiver.
 */
@Override
public String toString() {
	// Note change from 1.1.7 to 1.2: For primitive types,
	// return just the type name.
	if (isPrimitive()) return getName();
	return (isInterface() ? "interface " : "class ") + getName(); //$NON-NLS-1$ //$NON-NLS-2$
}

/**
 * Returns a formatted string describing this Class. The string has
 * the following format:
 * modifier1 modifier2 ... kind name&#60;typeparam1, typeparam2, ...&#62;.
 * kind is one of "class", "enum", "interface", "&#64;interface", or
 * empty string for primitive types. The type parameter list is
 * omitted if there are no type parameters.
 *
 * @return		a formatted string describing this class.
 * @since 1.8
 */
public String toGenericString() {
	if (isPrimitive()) return getName();

	StringBuilder result = new StringBuilder();
	int modifiers = getModifiers();

	// Checks for isInterface, isAnnotation and isEnum have been inlined
	// in order to avoid multiple calls to isArray and getModifiers
	boolean isArray = isArray();
	boolean isInterface = !isArray && (0 != (modifiers & Modifier.INTERFACE));

	// Get kind of type before modifying the modifiers
	String kindOfType;
	if ((!isArray) && ((modifiers & ANNOTATION) != 0)) {
		kindOfType = "@interface "; //$NON-NLS-1$
	} else if (isInterface) {
		kindOfType = "interface "; //$NON-NLS-1$
	} else if ((!isArray) && ((modifiers & ENUM) != 0) && (getSuperclass() == Enum.class)) {
		kindOfType = "enum "; //$NON-NLS-1$
	} else {
		kindOfType = "class "; //$NON-NLS-1$
	}

	// Remove "interface" from modifiers (is included as kind of type)
	if (isInterface) {
		modifiers -= Modifier.INTERFACE;
	}

	// Build generic string
	result.append(Modifier.toString(modifiers));
	if (result.length() > 0) {
		result.append(' ');
	}
	result.append(kindOfType);
	result.append(getName());

	// Add type parameters if present
	TypeVariable<?>[] typeVariables = getTypeParameters();
	if (0 != typeVariables.length) {
		result.append('<');
		boolean comma = false;
		for (TypeVariable<?> t : typeVariables) {
			if (comma) result.append(',');
			result.append(t);
			comma = true;
		}
		result.append('>');
	}

	return result.toString();
}

/**
 * Returns the Package of which this class is a member.
 * A class has a Package iff it was loaded from a SecureClassLoader
 *
 * @return		Package the Package of which this class is a member or null
 *
 */
public Package getPackage() {
	return getClassLoaderImpl().getPackage(getPackageName());
}

static Class<?> getPrimitiveClass(String name) {
	if (name.equals("float")) //$NON-NLS-1$
		return new float[0].getClass().getComponentType();
	if (name.equals("double")) //$NON-NLS-1$
		return new double[0].getClass().getComponentType();
	if (name.equals("int")) //$NON-NLS-1$
		return new int[0].getClass().getComponentType();
	if (name.equals("long")) //$NON-NLS-1$
		return new long[0].getClass().getComponentType();
	if (name.equals("char")) //$NON-NLS-1$
		return new char[0].getClass().getComponentType();
	if (name.equals("byte")) //$NON-NLS-1$
		return new byte[0].getClass().getComponentType();
	if (name.equals("boolean")) //$NON-NLS-1$
		return new boolean[0].getClass().getComponentType();
	if (name.equals("short")) //$NON-NLS-1$
		return new short[0].getClass().getComponentType();
	if (name.equals("void")) { //$NON-NLS-1$
		try {
			java.lang.reflect.Method method = Runnable.class.getMethod("run", EmptyParameters); //$NON-NLS-1$
			return method.getReturnType();
		} catch (Exception e) {
			com.ibm.oti.vm.VM.dumpString("Cannot initialize Void.TYPE\n"); //$NON-NLS-1$
		}
	}
	throw new Error("Unknown primitive type: " + name); //$NON-NLS-1$
}

/**
 * Returns the assertion status for this class.
 * Assertion is enabled/disabled based on
 * classloader default, package or class default at runtime
 *
 * @since 1.4
 *
 * @return		the assertion status for this class
 */
public boolean desiredAssertionStatus() {
	ClassLoader cldr = getClassLoaderImpl();
	if (cldr != null) {
		return cldr.getClassAssertionStatus(getName());
	}
	return false;
}

/**
 * Answer the class at depth.
 *
 * Notes:
 * 	 1) This method operates on the defining classes of methods on stack.
 *		NOT the classes of receivers.
 *
 *	 2) The item at index zero describes the caller of this method.
 *
 * @param 		depth
 * @return		the class at the given depth
 */
@sun.reflect.CallerSensitive
static final native Class<?> getStackClass(int depth);

/**
 * Walk the stack and answer an array containing the maxDepth
 * most recent classes on the stack of the calling thread.
 *
 * Starting with the caller of the caller of getStackClasses(), return an
 * array of not more than maxDepth Classes representing the classes of
 * running methods on the stack (including native methods).  Frames
 * representing the VM implementation of java.lang.reflect are not included
 * in the list.  If stopAtPrivileged is true, the walk will terminate at any
 * frame running one of the following methods:
 *
 * <code><ul>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedAction;)Ljava/lang/Object;</li>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedExceptionAction;)Ljava/lang/Object;</li>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;</li>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedExceptionAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;</li>
 * </ul></code>
 *
 * If one of the doPrivileged methods is found, the walk terminate and that frame is NOT included in the returned array.
 *
 * Notes: <ul>
 * 	 <li> This method operates on the defining classes of methods on stack.
 *		NOT the classes of receivers. </li>
 *
 *	 <li> The item at index zero in the result array describes the caller of
 *		the caller of this method. </li>
 *</ul>
 *
 * @param 		maxDepth			maximum depth to walk the stack, -1 for the entire stack
 * @param 		stopAtPrivileged	stop at priviledged classes
 * @return		the array of the most recent classes on the stack
 */
@sun.reflect.CallerSensitive
static final native Class<?>[] getStackClasses(int maxDepth, boolean stopAtPrivileged);

/**
 * Called from JVM_ClassDepth.
 * Answers the index in the stack of the first method which
 * is contained in a class called <code>name</code>. If no
 * methods from this class are in the stack, return -1.
 *
 * @param		name String
 *					the name of the class to look for.
 * @return		int
 *					the depth in the stack of a the first
 *					method found.
 */
@sun.reflect.CallerSensitive
static int classDepth (String name) {
	Class<?>[] classes = getStackClasses(-1, false);
	for (int i=1; i<classes.length; i++)
		if (classes[i].getName().equals(name))
			return i - 1;
	return -1;
}

/**
 * Called from JVM_ClassLoaderDepth.
 * Answers the index in the stack of thee first class
 * whose class loader is not a system class loader.
 *
 * @return		the frame index of the first method whose class was loaded by a non-system class loader.
 */
@sun.reflect.CallerSensitive
static int classLoaderDepth() {
	// Now, check if there are any non-system class loaders in
	// the stack up to the first privileged method (or the end
	// of the stack.
	Class<?>[] classes = getStackClasses(-1, true);
	for (int i=1; i<classes.length; i++) {
		ClassLoader cl = classes[i].getClassLoaderImpl();
		if (!cl.isASystemClassLoader()) return i - 1;
	}
	return -1;
}

/**
 * Called from JVM_CurrentClassLoader.
 * Answers the class loader of the first class in the stack
 * whose class loader is not a system class loader.
 *
 * @return		the most recent non-system class loader.
 */
@sun.reflect.CallerSensitive
static ClassLoader currentClassLoader() {
	// Now, check if there are any non-system class loaders in
	// the stack up to the first privileged method (or the end
	// of the stack.
	Class<?>[] classes = getStackClasses(-1, true);
	for (int i=1; i<classes.length; i++) {
		ClassLoader cl = classes[i].getClassLoaderImpl();
		if (!cl.isASystemClassLoader()) return cl;
	}
	return null;
}

/**
 * Called from JVM_CurrentLoadedClass.
 * Answers the first class in the stack which was loaded
 * by a class loader which is not a system class loader.
 *
 * @return		the most recent class loaded by a non-system class loader.
 */
@sun.reflect.CallerSensitive
static Class<?> currentLoadedClass() {
	// Now, check if there are any non-system class loaders in
	// the stack up to the first privileged method (or the end
	// of the stack.
	Class<?>[] classes = getStackClasses(-1, true);
	for (int i=1; i<classes.length; i++) {
		ClassLoader cl = classes[i].getClassLoaderImpl();
		if (!cl.isASystemClassLoader()) return classes[i];
	}
	return null;
}

/**
 * Return the specified Annotation for this Class. Inherited Annotations
 * are searched.
 *
 * @param annotation the Annotation type
 * @return the specified Annotation or null
 *
 * @since 1.5
 */
public <A extends Annotation> A getAnnotation(Class<A> annotation) {
	if (annotation == null) throw new NullPointerException();
	return (A)getAnnotationCache().annotationMap.get(annotation);
}

/**
 * Return the directly declared Annotations for this Class, including the Annotations
 * inherited from superclasses.
 * If an annotation type has been included before, then next occurrences will not be included.
 *
 * Repeated annotations are not included since they will be stored in their container annotation.
 * But container annotations are included. (If a container annotation is repeatable and it is repeated,
 * then these container annotations' container annotation is included. )
 * @return an array of Annotation
 *
 * @since 1.5
 */
public Annotation[] getAnnotations() {
	Collection<Annotation> annotations = getAnnotationCache().annotationMap.values();
	return annotations.toArray(new Annotation[annotations.size()]);
}

/**
 * Looks through directly declared annotations for this class, not including Annotations inherited from superclasses.
 *
 * @param annotation the Annotation to search for
 * @return directly declared annotation of specified annotation type.
 *
 * @since 1.8
 */
public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotation) {
	if (annotation == null) throw new NullPointerException();
	return (A)getAnnotationCache().directAnnotationMap.get(annotation);
}

/**
 * Return the annotated types for the implemented interfaces.
 * @return array, possibly empty, of AnnotatedTypes
 */
public AnnotatedType[] getAnnotatedInterfaces() {
	return TypeAnnotationParser.buildAnnotatedInterfaces(this);
}

/**
 * Return the annotated superclass of this class.
 * @return null if this class is Object, an interface, a primitive type, or an array type.  Otherwise return (possibly empty) AnnotatedType.
 */
public AnnotatedType getAnnotatedSuperclass() {
	if (this.equals(Object.class) || this.isInterface() || this.isPrimitive() || this.isArray()) {
		return null;
	}
	return TypeAnnotationParser.buildAnnotatedSupertype(this);
}

/**
 * Answers the type name of the class which the receiver represents.
 *
 * @return the fully qualified type name, with brackets if an array class
 *
 * @since 1.8
 */
@Override
public String getTypeName() {
	if (isArray()) {
		StringBuilder nameBuffer = new StringBuilder("[]"); //$NON-NLS-1$
		Class<?> componentType = getComponentType();
		while (componentType.isArray()) {
			nameBuffer.append("[]"); //$NON-NLS-1$
			componentType = componentType.getComponentType();
		}
		nameBuffer.insert(0, componentType.getName());
		return nameBuffer.toString();
	} else {
		return getName();
	}
}

/**
 * Returns the annotations only for this Class, not including Annotations inherited from superclasses.
 * It includes all the directly declared annotations.
 * Repeated annotations are not included but their container annotation does.
 *
 * @return an array of declared annotations
 *
 *
 * @since 1.5
 */
public Annotation[] getDeclaredAnnotations() {
	Collection<Annotation> directAnnotations = getAnnotationCache().directAnnotationMap.values();
	return directAnnotations.toArray(new Annotation[directAnnotations.size()]);
}

/**
 *
 * Terms used for annotations :
 * Repeatable Annotation :
 * 		An annotation which can be used more than once for the same class declaration.
 * 		Repeatable annotations are annotated with Repeatable annotation which tells the
 * 		container annotation for this repeatable annotation.
 * 		Example =
 *
 * 		@interface ContainerAnnotation {RepeatableAnn[] value();}
 * 		@Repeatable(ContainerAnnotation.class)
 * Container Annotation:
 * 		Container annotation stores the repeated annotations in its array-valued element.
 * 		Using repeatable annotations more than once makes them stored in their container annotation.
 * 		In this case, container annotation is visible directly on class declaration, but not the repeated annotations.
 * Repeated Annotation:
 * 		A repeatable annotation which is used more than once for the same class.
 * Directly Declared Annotation :
 * 		All non repeatable annotations are directly declared annotations.
 * 		As for repeatable annotations, they can be directly declared annotation if and only if they are used once.
 * 		Repeated annotations are not directly declared in class declaration, but their container annotation does.
 *
 * -------------------------------------------------------------------------------------------------------
 *
 * Gets the specified type annotations of this class.
 * If the specified type is not repeatable annotation, then returned array size will be 0 or 1.
 * If specified type is repeatable annotation, then all the annotations of that type will be returned. Array size might be 0, 1 or more.
 *
 * It does not search through super classes.
 *
 * @param annotationClass the annotation type to search for
 * @return array of declared annotations in the specified annotation type
 *
 * @since 1.8
 */
public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
	ArrayList<A> annotationsList = internalGetDeclaredAnnotationsByType(annotationClass);
	return annotationsList.toArray((A[])Array.newInstance(annotationClass, annotationsList.size()));
}

private <A extends Annotation> ArrayList<A> internalGetDeclaredAnnotationsByType(Class<A> annotationClass) {
	AnnotationCache currentAnnotationCache = getAnnotationCache();
	ArrayList<A> annotationsList = new ArrayList<>();

	Repeatable repeatable = annotationClass.getDeclaredAnnotation(Repeatable.class);
	if (repeatable == null) {
		A annotation = (A)currentAnnotationCache.directAnnotationMap.get(annotationClass);
		if (annotation != null) {
			annotationsList.add(annotation);
		}
	} else {
		Class<? extends Annotation> containerType = repeatable.value();
		// if the annotation and its container are both present, the order must be maintained
		for (Map.Entry<Class<? extends Annotation>, Annotation> entry : currentAnnotationCache.directAnnotationMap.entrySet()) {
			Class<? extends Annotation> annotationType = entry.getKey();
			if (annotationType == annotationClass) {
				annotationsList.add((A)entry.getValue());
			} else if (annotationType == containerType) {
				A[] containedAnnotations = (A[])getAnnotationsArrayFromValue(entry.getValue(), containerType, annotationClass);
				if (containedAnnotations != null) {
					annotationsList.addAll(Arrays.asList(containedAnnotations));
				}
			}
		}
	}

	return annotationsList;
}

/**
 * Gets the specified type annotations of this class.
 * If the specified type is not repeatable annotation, then returned array size will be 0 or 1.
 * If specified type is repeatable annotation, then all the annotations of that type will be returned. Array size might be 0, 1 or more.
 *
 * It searches through superclasses until it finds the inherited specified annotationClass.
 *
 * @param annotationClass the annotation type to search for
 * @return array of declared annotations in the specified annotation type
 *
 * @since 1.8
 */
public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass)
{
	ArrayList<A> annotationsList = internalGetDeclaredAnnotationsByType(annotationClass);

	if (annotationClass.isInheritedAnnotationType()) {
		Class<?> sc = this;
		while (0 == annotationsList.size()) {
			sc = sc.getSuperclass();
			if (null == sc) break;
			ArrayList<A> superAnnotations = sc.internalGetDeclaredAnnotationsByType(annotationClass);
			if (superAnnotations != null) {
				annotationsList.addAll(superAnnotations);
			}
		}
	}
	return annotationsList.toArray((A[])Array.newInstance(annotationClass, annotationsList.size()));
}

AnnotationVars getAnnotationVars() {
	AnnotationVars tempAnnotationVars = annotationVars;
	if (tempAnnotationVars == null) {
		if (annotationVarsOffset == -1) {
			try {
				Field annotationVarsField = Class.class.getDeclaredField("annotationVars"); //$NON-NLS-1$
				annotationVarsOffset = getUnsafe().objectFieldOffset(annotationVarsField);
			} catch (NoSuchFieldException e) {
				throw newInternalError(e);
			}
		}
		tempAnnotationVars = new AnnotationVars();
		synchronized (this) {
			if (annotationVars == null) {
				// Lazy initialization of a non-volatile field. Ensure the Object is initialized
				// and flushed to memory before assigning to the annotationVars field.
				getUnsafe().putOrderedObject(this, annotationVarsOffset, tempAnnotationVars);
			} else {
				tempAnnotationVars = annotationVars;
			}
		}
	}
	return tempAnnotationVars;
}

private MethodHandle getValueMethod(final Class<? extends Annotation> containedType) {
	final AnnotationVars localAnnotationVars = getAnnotationVars();
	MethodHandle valueMethod = localAnnotationVars.valueMethod;
	if (valueMethod == null) {
		final MethodType methodType = MethodType.methodType(Array.newInstance(containedType, 0).getClass());
		valueMethod = AccessController.doPrivileged(new PrivilegedAction<MethodHandle>() {
		    @Override
		    public MethodHandle run() {
		    	try {
		    		MethodHandles.Lookup localImplLookup = implLookup;
		    		if (localImplLookup == null) {
		    			Field privilegedLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP"); //$NON-NLS-1$
		    			privilegedLookupField.setAccessible(true);
		    			localImplLookup = (MethodHandles.Lookup)privilegedLookupField.get(MethodHandles.Lookup.class);
		    			Field implLookupField = Class.class.getDeclaredField("implLookup"); //$NON-NLS-1$
		    			long implLookupOffset = getUnsafe().staticFieldOffset(implLookupField);
			    		// Lazy initialization of a non-volatile field. Ensure the Object is initialized
			    		// and flushed to memory before assigning to the implLookup field.
		    			getUnsafe().putOrderedObject(Class.class, implLookupOffset, localImplLookup);
		    		}
		    		MethodHandle handle = localImplLookup.findVirtual(Class.this, "value", methodType); //$NON-NLS-1$
		    		if (AnnotationVars.valueMethodOffset == -1) {
		    			Field valueMethodField = AnnotationVars.class.getDeclaredField("valueMethod"); //$NON-NLS-1$
		    			AnnotationVars.valueMethodOffset = getUnsafe().objectFieldOffset(valueMethodField);
		    		}
		    		// Lazy initialization of a non-volatile field. Ensure the Object is initialized
		    		// and flushed to memory before assigning to the valueMethod field.
		    		getUnsafe().putOrderedObject(localAnnotationVars, AnnotationVars.valueMethodOffset, handle);
		    		return handle;
		    	} catch (NoSuchMethodException e) {
		    		return null;
		    	} catch (IllegalAccessException | NoSuchFieldException e) {
		    		throw newInternalError(e);
				}
		    }
		});
	}
	return valueMethod;
}

/**
 * Gets the array of containedType from the value() method.
 *
 * @param container the annotation which is the container of the repeated annotation
 * @param containerType the annotationType() of the container. This implements the value() method.
 * @param containedType the annotationType() stored in the container
 * @return Annotation array if the given annotation has a value() method which returns an array of the containedType. Otherwise, return null.
 */
private Annotation[] getAnnotationsArrayFromValue(Annotation container, Class<? extends Annotation> containerType, Class<? extends Annotation> containedType) {
	try {
		MethodHandle valueMethod = containerType.getValueMethod(containedType);
		if (valueMethod != null) {
			Object children = valueMethod.invoke(container);
			/*
			 * Check whether value is Annotation array or not
			 */
			if (children instanceof Annotation[]) {
				return (Annotation[])children;
			}
		}
		return null;
	} catch (Error | RuntimeException e) {
		throw e;
	} catch (Throwable t) {
		throw new RuntimeException(t);
	}
}

private boolean isInheritedAnnotationType() {
	return getAnnotationCache().directAnnotationMap.get(Inherited.class) != null;
}

private Map<Class<? extends Annotation>, Annotation> buildAnnotations(Map<Class<? extends Annotation>, Annotation> directAnnotationsMap) {
	Class<?> superClass = getSuperclass();
	if (superClass == null) {
		return directAnnotationsMap;
	}
	Map<Class<? extends Annotation>, Annotation> superAnnotations = superClass.getAnnotationCache().annotationMap;
	Map<Class<? extends Annotation>, Annotation> annotationsMap = null;
	for (Map.Entry<Class<? extends Annotation>, Annotation> entry : superAnnotations.entrySet()) {
		Class<? extends Annotation> annotationType = entry.getKey();
		// if the annotation is Inherited store the annotation
		if (annotationType.isInheritedAnnotationType()) {
			if (annotationsMap == null) {
				annotationsMap = new LinkedHashMap<>((superAnnotations.size() + directAnnotationsMap.size()) * 4 / 3);
			}
			annotationsMap.put(annotationType, entry.getValue());
		}
	}
	if (annotationsMap == null) {
		return directAnnotationsMap;
	}
	annotationsMap.putAll(directAnnotationsMap);
	return annotationsMap;
}

/**
 * Gets all the direct annotations.
 * It does not include repeated annotations for this class, it includes their container annotation(s).
 *
 * @return array of all the direct annotations.
 */
private AnnotationCache getAnnotationCache() {
	AnnotationCache annotationCacheResult = annotationCache;

	if (annotationCacheResult == null) {
		byte[] annotationsData = getDeclaredAnnotationsData();
		if (annotationsData == null) {
			Map<Class<? extends Annotation>, Annotation> emptyMap = Collections.emptyMap();
			annotationCacheResult = new AnnotationCache(emptyMap, buildAnnotations(emptyMap));
		} else {
			Annotation[] directAnnotations = sun.reflect.annotation.AnnotationParser.toArray(
						sun.reflect.annotation.AnnotationParser.parseAnnotations(
								annotationsData,
								new Access().getConstantPool(this),
								this));

			Map<Class<? extends Annotation>, Annotation> directAnnotationsMap = new LinkedHashMap<>(directAnnotations.length * 4 / 3);
			for (Annotation annotation : directAnnotations) {
				Class<? extends Annotation> annotationType = annotation.annotationType();
				directAnnotationsMap.put(annotationType, annotation);
			}
			annotationCacheResult = new AnnotationCache(directAnnotationsMap, buildAnnotations(directAnnotationsMap));
		}

		// Don't bother with synchronization. Since it is just a cache, it doesn't matter if it gets overwritten
		// because multiple threads create the cache at the same time
		long localAnnotationCacheOffset = annotationCacheOffset;
		if (localAnnotationCacheOffset == -1) {
			try {
				Field annotationCacheField = Class.class.getDeclaredField("annotationCache"); //$NON-NLS-1$
				localAnnotationCacheOffset = getUnsafe().objectFieldOffset(annotationCacheField);
				annotationCacheOffset = localAnnotationCacheOffset;
			} catch (NoSuchFieldException e) {
				throw newInternalError(e);
			}
		}
		// Lazy initialization of a non-volatile field. Ensure the Object is initialized
		// and flushed to memory before assigning to the annotationCache field.
		getUnsafe().putOrderedObject(this, localAnnotationCacheOffset, annotationCacheResult);
	}
	return annotationCacheResult;
}

private native byte[] getDeclaredAnnotationsData();

/**
 * Answer if this class is an Annotation.
 *
 * @return true if this class is an Annotation
 *
 * @since 1.5
 */
public boolean isAnnotation() {
	// This code has been inlined in toGenericString. toGenericString
	// must be modified to reflect any changes to this implementation.
	return !isArray() && (getModifiersImpl() & ANNOTATION) != 0;
}

/**
 * Answer if the specified Annotation exists for this Class. Inherited
 * Annotations are searched.
 *
 * @param annotation the Annotation type
 * @return true if the specified Annotation exists
 *
 * @since 1.5
 */
public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
	if (annotation == null) throw new NullPointerException();
	return getAnnotation(annotation) != null;
}

/**
 * Cast this Class to a subclass of the specified Class.
 *
 * @param cls the Class to cast to
 * @return this Class, cast to a subclass of the specified Class
 *
 * @throws ClassCastException if this Class is not the same or a subclass
 *		of the specified Class
 *
 * @since 1.5
 */
public <U> Class<? extends U> asSubclass(Class<U> cls) {
	if (!cls.isAssignableFrom(this))
		throw new ClassCastException(this.toString());
	return (Class<? extends U>)this;
}

/**
 * Cast the specified object to this Class.
 *
 * @param object the object to cast
 *
 * @return the specified object, cast to this Class
 *
 * @throws ClassCastException if the specified object cannot be cast
 *		to this Class
 *
 * @since 1.5
 */
public T cast(Object object) {
	if (object != null && !this.isInstance(object))
		// K0336 = Cannot cast {0} to {1}
		throw new ClassCastException(com.ibm.oti.util.Msg.getString("K0336", object.getClass(), this)); //$NON-NLS-1$
	return (T)object;
}

/**
 * Answer if this Class is an enum.
 *
 * @return true if this Class is an enum
 *
 * @since 1.5
 */
public boolean isEnum() {
	// This code has been inlined in toGenericString. toGenericString
	// must be modified to reflect any changes to this implementation.
	return !isArray() && (getModifiersImpl() & ENUM) != 0 &&
		getSuperclass() == Enum.class;
}

private EnumVars<T> getEnumVars() {
	EnumVars<T> tempEnumVars = enumVars;
	if (tempEnumVars == null) {
		long localEnumVarsOffset = enumVarsOffset;
		if (localEnumVarsOffset == -1) {
			Field enumVarsField;
			try {
				enumVarsField = Class.class.getDeclaredField("enumVars"); //$NON-NLS-1$
				localEnumVarsOffset = getUnsafe().objectFieldOffset(enumVarsField);
				enumVarsOffset = localEnumVarsOffset;
			} catch (NoSuchFieldException e) {
				throw newInternalError(e);
			}
		}
		// Don't bother with synchronization to determine if the field is already assigned. Since it is just a cache,
		// it doesn't matter if it gets overwritten because multiple threads create the cache at the same time
		tempEnumVars = new EnumVars<>();
		// Lazy initialization of a non-volatile field. Ensure the Object is initialized
		// and flushed to memory before assigning to the enumVars field.
		getUnsafe().putOrderedObject(this, localEnumVarsOffset, tempEnumVars);
	}
	return tempEnumVars;
}

/**
 *
 * @return Map keyed by enum name, of uncloned and cached enum constants in this class
 */
Map<String, T> enumConstantDirectory() {
	EnumVars<T> localEnumVars = getEnumVars();
	Map<String, T> map = localEnumVars.cachedEnumConstantDirectory;
	if (null == map) {
		T[] enums = getEnumConstantsShared();
		if (enums == null) {
			/*
			 * Class#valueOf() is the caller of this method,
			 * according to the spec it throws IllegalArgumentException if the class is not an Enum.
			 */
			// K0564 = {0} is not an Enum
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0564", getName())); //$NON-NLS-1$
		}
		map = new HashMap<>(enums.length * 4 / 3);
		for (int i = 0; i < enums.length; i++) {
			map.put(((Enum<?>) enums[i]).name(), enums[i]);
		}

		if (EnumVars.enumDirOffset == -1) {
			try {
				Field enumDirField = EnumVars.class.getDeclaredField("cachedEnumConstantDirectory"); //$NON-NLS-1$
				EnumVars.enumDirOffset = getUnsafe().objectFieldOffset(enumDirField);
			} catch (NoSuchFieldException e) {
				throw newInternalError(e);
			}
		}
		// Lazy initialization of a non-volatile field. Ensure the Object is initialized
		// and flushed to memory before assigning to the cachedEnumConstantDirectory field.
		getUnsafe().putOrderedObject(localEnumVars, EnumVars.enumDirOffset, map);
	}
	return map;
}

/**
 * Answer the shared uncloned array of enum constants for this Class. Returns null if
 * this class is not an enum.
 *
 * @return the array of enum constants, or null
 *
 * @since 1.5
 */
T[] getEnumConstantsShared() {
	EnumVars<T> localEnumVars = getEnumVars();
	T[] enums = localEnumVars.cachedEnumConstants;
	if (null == enums && isEnum()) {
		try {
			final PrivilegedExceptionAction<Method> privilegedAction = new PrivilegedExceptionAction<Method>() {
				@Override
				public Method run() throws Exception {
					Method method = getMethod("values"); //$NON-NLS-1$
					// the enum class may not be visible
					method.setAccessible(true);
					return method;
				}
			};

			Method values = AccessController.doPrivileged(privilegedAction);
			enums = (T[])values.invoke(this);

			long localEnumConstantsOffset = EnumVars.enumConstantsOffset;
			if (localEnumConstantsOffset == -1) {
				try {
					Field enumConstantsField = EnumVars.class.getDeclaredField("cachedEnumConstants"); //$NON-NLS-1$
					localEnumConstantsOffset = getUnsafe().objectFieldOffset(enumConstantsField);
					EnumVars.enumConstantsOffset = localEnumConstantsOffset;
				} catch (NoSuchFieldException e) {
					throw newInternalError(e);
				}
			}
			// Lazy initialization of a non-volatile field. Ensure the Object is initialized
			// and flushed to memory before assigning to the cachedEnumConstants field.
			getUnsafe().putOrderedObject(localEnumVars, localEnumConstantsOffset, enums);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | PrivilegedActionException e) {
			enums = null;
		}
	}

	return enums;
}

/**
 * Answer the array of enum constants for this Class. Returns null if
 * this class is not an enum.
 *
 * @return the array of enum constants, or null
 *
 * @since 1.5
 */
public T[] getEnumConstants() {
	T[] enumConstants = getEnumConstantsShared();
	if (null != enumConstants) {
		return enumConstants.clone();
	} else {
		return null;
	}
}

/**
 * Answer if this Class is synthetic. A synthetic Class is created by
 * the compiler.
 *
 * @return true if this Class is synthetic.
 *
 * @since 1.5
 */
public boolean isSynthetic() {
	return !isArray() && (getModifiersImpl() & SYNTHETIC) != 0;
}

private native String getGenericSignature();

private CoreReflectionFactory getFactory() {
	return CoreReflectionFactory.make(this, ClassScope.make(this));
}

private ClassRepositoryHolder getClassRepositoryHolder() {
	ClassRepositoryHolder localClassRepositoryHolder = classRepoHolder;
	if (localClassRepositoryHolder == null) {
		synchronized(this) {
			localClassRepositoryHolder = classRepoHolder;
			if (localClassRepositoryHolder == null) {
				String signature = getGenericSignature();
				if (signature == null) {
					localClassRepositoryHolder = ClassRepositoryHolder.NullSingleton;
				} else {
					ClassRepository classRepo = ClassRepository.make(signature, getFactory());
					localClassRepositoryHolder = new ClassRepositoryHolder(classRepo);
				}
				classRepoHolder = localClassRepositoryHolder;
			}
		}
	}
	return localClassRepositoryHolder;
}

/**
 * Answers an array of TypeVariable for the generic parameters declared
 * on this Class.
 *
 * @return		the TypeVariable[] for the generic parameters
 *
 * @since 1.5
 */
@SuppressWarnings("unchecked")
public TypeVariable<Class<T>>[] getTypeParameters() {
	ClassRepositoryHolder holder = getClassRepositoryHolder();
	ClassRepository repository = holder.classRepository;
	if (repository == null) return new TypeVariable[0];
	return (TypeVariable<Class<T>>[])repository.getTypeParameters();
}

/**
 * Answers an array of Type for the Class objects which match the
 * interfaces specified in the receiver classes <code>implements</code>
 * declaration.
 *
 * @return		Type[]
 *					the interfaces the receiver claims to implement.
 *
 * @since 1.5
 */
public Type[] getGenericInterfaces() {
	ClassRepositoryHolder holder = getClassRepositoryHolder();
	ClassRepository repository = holder.classRepository;
	if (repository == null) return getInterfaces();
	return repository.getSuperInterfaces();
}

/**
 * Answers the Type for the Class which represents the receiver's
 * superclass. For classes which represent base types,
 * interfaces, and for java.lang.Object the method
 * answers null.
 *
 * @return		the Type for the receiver's superclass.
 *
 * @since 1.5
 */
public Type getGenericSuperclass() {
	ClassRepositoryHolder holder = getClassRepositoryHolder();
	ClassRepository repository = holder.classRepository;
	if (repository == null) return getSuperclass();
	if (isInterface()) return null;
	return repository.getSuperclass();
}

private native Object getEnclosingObject();

/**
 * If this Class is defined inside a constructor, return the Constructor.
 *
 * @return the enclosing Constructor or null
 * @throws SecurityException if declared member access or package access is not allowed
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 * @see #isLocalClass()
 */
@sun.reflect.CallerSensitive
public Constructor<?> getEnclosingConstructor() throws SecurityException {
	Constructor<?> constructor = null;
	Object enclosing = getEnclosingObject();
	if (enclosing instanceof Constructor<?>) {
		constructor = (Constructor<?>) enclosing;
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
			constructor.getDeclaringClass().checkMemberAccess(security, callerClassLoader, Member.DECLARED);
		}
	}
	return constructor;
}

/**
 * If this Class is defined inside a method, return the Method.
 *
 * @return the enclosing Method or null
 * @throws SecurityException if declared member access or package access is not allowed
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 * @see #isLocalClass()
 */
@sun.reflect.CallerSensitive
public Method getEnclosingMethod() throws SecurityException {
	Method method = null;
	Object enclosing = getEnclosingObject();
	if (enclosing instanceof Method) {
		method = (Method)enclosing;
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
			method.getDeclaringClass().checkMemberAccess(security, callerClassLoader, Member.DECLARED);
		}
	}
	return method;
}

private native Class<?> getEnclosingObjectClass();

/**
 * Return the enclosing Class of this Class. Unlike getDeclaringClass(),
 * this method works on any nested Class, not just classes nested directly
 * in other classes.
 *
 * @return the enclosing Class or null
 * @throws SecurityException if package access is not allowed
 *
 * @since 1.5
 *
 * @see #getDeclaringClass()
 * @see #isAnonymousClass()
 * @see #isLocalClass()
 * @see #isMemberClass()
 */
@sun.reflect.CallerSensitive
public Class<?> getEnclosingClass() throws SecurityException {
	Class<?> enclosingClass = getDeclaringClass();
	if (enclosingClass == null) {
		enclosingClass = getEnclosingObjectClass();
	}
	if (enclosingClass != null) {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(1);
			enclosingClass.checkMemberAccess(security, callerClassLoader, MEMBER_INVALID_TYPE);
		}
	}

	return enclosingClass;
}

private native String getSimpleNameImpl();

/**
 * Return the simple name of this Class. The simple name does not include
 * the package or the name of the enclosing class. The simple name of an
 * anonymous class is "".
 *
 * @return the simple name
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 */
public String getSimpleName() {
	int arrayCount = 0;
	Class<?> baseType = this;
	if (isArray()) {
		arrayCount = 1;
		while ((baseType = baseType.getComponentType()).isArray()) {
			arrayCount++;
		}
	}
	String simpleName = baseType.getSimpleNameImpl();
	if (simpleName == null) {
		// either a base class, or anonymous class
		if (baseType.getEnclosingObjectClass() != null) {
			simpleName = ""; //$NON-NLS-1$
		} else {
			// remove the package name
			simpleName = baseType.getName();
			int index = simpleName.lastIndexOf('.');
			if (index != -1) {
				simpleName = simpleName.substring(index+1);
			}
		}
	}
	if (arrayCount > 0) {
		StringBuilder result = new StringBuilder(simpleName);
		for (int i=0; i<arrayCount; i++) {
			result.append("[]"); //$NON-NLS-1$
		}
		return result.toString();
	}
	return simpleName;
}

/**
 * Return the canonical name of this Class. The canonical name is null
 * for a local or anonymous class. The canonical name includes the package
 * and the name of the enclosing class.
 *
 * @return the canonical name or null
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 * @see #isLocalClass()
 */
public String getCanonicalName() {
	int arrayCount = 0;
	Class<?> baseType = this;
	if (isArray()) {
		arrayCount = 1;
		while ((baseType = baseType.getComponentType()).isArray()) {
			arrayCount++;
		}
	}
	if (baseType.getEnclosingObjectClass() != null) {
		// local or anonymous class
		return null;
	}
	String canonicalName;
	Class<?> declaringClass = baseType.getDeclaringClass();
	if (declaringClass == null) {
		canonicalName = baseType.getName();
	} else {
		String declaringClassCanonicalName = declaringClass.getCanonicalName();
		if (declaringClassCanonicalName == null) return null;
		// remove the enclosingClass from the name, including the $
		String simpleName = baseType.getName().substring(declaringClass.getName().length() + 1);
		canonicalName = declaringClassCanonicalName + '.' + simpleName;
	}

	if (arrayCount > 0) {
		StringBuilder result = new StringBuilder(canonicalName);
		for (int i=0; i<arrayCount; i++) {
			result.append("[]"); //$NON-NLS-1$
		}
		return result.toString();
	}
	return canonicalName;
}

/**
 * Answer if this Class is anonymous. An unnamed Class defined
 * inside a method.
 *
 * @return true if this Class is anonymous.
 *
 * @since 1.5
 *
 * @see #isLocalClass()
 */
public boolean isAnonymousClass() {
	return getSimpleNameImpl() == null && getEnclosingObjectClass() != null;
}

/**
 * Answer if this Class is local. A named Class defined inside
 * a method.
 *
 * @return true if this Class is local.
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 */
public boolean isLocalClass() {
	return getEnclosingObjectClass() != null && getSimpleNameImpl() != null;
}

/**
 * Answer if this Class is a member Class. A Class defined inside another
 * Class.
 *
 * @return true if this Class is local.
 *
 * @since 1.5
 *
 * @see #isLocalClass()
 */
public boolean isMemberClass() {
	return getEnclosingObjectClass() == null && getDeclaringClass() != null;
}

/**
 * Return the depth in the class hierarchy of the receiver.
 * Base type classes and Object return 0.
 *
 * @return receiver's class depth
 *
 * @see #getDeclaredMethod
 * @see #getMethod
 */
private native int getClassDepth();

/**
 * Compute the signature for get*Method()
 *
 * @param		name			the name of the method
 * @param		parameterTypes	the types of the arguments
 * @return 		the signature string
 * @throws		NoSuchMethodException if one of the parameter types cannot be found in the local class loader
 *
 * @see #getDeclaredMethod
 * @see #getMethod
 */
private String getParameterTypesSignature(String name, Class<?>[] parameterTypes, String returnTypeSignature) throws NoSuchMethodException {
	int total = 2;
	String[] sigs = new String[parameterTypes.length];
	for(int i = 0; i < parameterTypes.length; i++) {
		Class<?> parameterType = parameterTypes[i];
		if (parameterType != null) {
			sigs[i] = parameterType.getSignature();
			total += sigs[i].length();
		} else throw newNoSuchMethodException(name, parameterTypes);
	}
	total += returnTypeSignature.length();
	StringBuilder signature = new StringBuilder(total);
	signature.append('(');
	for(int i = 0; i < parameterTypes.length; i++)
		signature.append(sigs[i]);
	signature.append(')').append(returnTypeSignature);
	return signature.toString();
}

private static Method copyMethod, copyField, copyConstructor;
private static Field methodParameterTypesField;
private static Field constructorParameterTypesField;
private static final Object[] NoArgs = new Object[0];

static void initCacheIds(boolean cacheEnabled, boolean cacheDebug) {
	reflectCacheEnabled = cacheEnabled;
	reflectCacheDebug = cacheDebug;
	AccessController.doPrivileged(new PrivilegedAction<Void>() {
		@Override
		public Void run() {
			doInitCacheIds();
			return null;
		}
	});
}
static void setReflectCacheAppOnly(boolean cacheAppOnly) {
	reflectCacheAppOnly = cacheAppOnly;
}
@SuppressWarnings("nls")
static void doInitCacheIds() {
	constructorParameterTypesField = getAccessibleField(Constructor.class, "parameterTypes");
	methodParameterTypesField = getAccessibleField(Method.class, "parameterTypes");
	if (reflectCacheEnabled) {
		copyConstructor = getAccessibleMethod(Constructor.class, "copy");
		copyMethod = getAccessibleMethod(Method.class, "copy");
		copyField = getAccessibleMethod(Field.class, "copy");
	}
}
private static Field getAccessibleField(Class<?> cls, String name) {
	try {
		Field field = cls.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	} catch (NoSuchFieldException e) {
		throw newInternalError(e);
	}
}
private static Method getAccessibleMethod(Class<?> cls, String name) {
	try {
		Method method = cls.getDeclaredMethod(name, EmptyParameters);
		method.setAccessible(true);
		return method;
	} catch (NoSuchMethodException e) {
		throw newInternalError(e);
	}
}

/**
 * represents all methods of a given name and signature visible from a given class or interface.
 *
 */

private static final class ReflectRef extends SoftReference<Object> implements Runnable {
	private static final ReferenceQueue<Object> queue = new ReferenceQueue<>();
	private final ReflectCache cache;
	final CacheKey key;
	ReflectRef(ReflectCache cache, CacheKey key, Object value) {
		super(value, queue);
		this.cache = cache;
		this.key = key;
	}
	@Override
	public void run() {
		cache.handleCleared(this);
	}
}

private static final class CacheKey {
	private static final int PRIME = 31;
	private static int hashCombine(int partial, int itemHash) {
		return partial * PRIME + itemHash;
	}
	private static int hashCombine(int partial, Object item) {
		return hashCombine(partial, item == null ? 0 : item.hashCode());
	}

	static CacheKey newConstructorKey(Class<?>[] parameterTypes) {
		return new CacheKey("", parameterTypes, null); //$NON-NLS-1$
	}
	static CacheKey newFieldKey(String fieldName, Class<?> type) {
		return new CacheKey(fieldName, null, type);
	}
	static CacheKey newMethodKey(String methodName, Class<?>[] parameterTypes, Class<?> returnType) {
		return new CacheKey(methodName, parameterTypes, returnType);
	}

	static final CacheKey PublicConstructorsKey = new CacheKey("/c", EmptyParameters, null); //$NON-NLS-1$
	static final CacheKey PublicFieldsKey = newFieldKey("/f", null); //$NON-NLS-1$
	static final CacheKey PublicMethodsKey = new CacheKey("/m", EmptyParameters, null); //$NON-NLS-1$

	static final CacheKey DeclaredConstructorsKey = new CacheKey(".c", EmptyParameters, null); //$NON-NLS-1$
	static final CacheKey DeclaredFieldsKey = newFieldKey(".f", null); //$NON-NLS-1$
	static final CacheKey DeclaredMethodsKey = new CacheKey(".m", EmptyParameters, null); //$NON-NLS-1$

	private final String name;
	private final Class<?>[] parameterTypes;
	private final Class<?> returnType;
	private final int hashCode;
	private CacheKey(String name, Class<?>[] parameterTypes, Class<?> returnType) {
		super();
		int hash = hashCombine(name.hashCode(), returnType);
		if (parameterTypes != null) {
			for (Class<?> parameterType : parameterTypes) {
				hash = hashCombine(hash, parameterType);
			}
		}
		this.name = name;
		this.parameterTypes = parameterTypes;
		this.returnType = returnType;
		this.hashCode = hash;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		CacheKey that = (CacheKey) obj;
		if (this.returnType == that.returnType
				&& sameTypes(this.parameterTypes, that.parameterTypes)) {
			return this.name.equals(that.name);
		}
		return false;
	}
	@Override
	public int hashCode() {
		return hashCode;
	}
}

private static Class<?>[] getParameterTypes(Constructor<?> constructor) {
	try {
		if (null != constructorParameterTypesField)	{
			return (Class<?>[]) constructorParameterTypesField.get(constructor);
		} else {
			return constructor.getParameterTypes();
		}
	} catch (IllegalAccessException | IllegalArgumentException e) {
		throw newInternalError(e);
	}
}

static Class<?>[] getParameterTypes(Method method) {
	try {
		if (null != methodParameterTypesField)	{
			return (Class<?>[]) methodParameterTypesField.get(method);
		} else {
			return method.getParameterTypes();
		}
	} catch (IllegalAccessException | IllegalArgumentException e) {
		throw newInternalError(e);
	}
}

private static final class ReflectCache extends ConcurrentHashMap<CacheKey, ReflectRef> {
	private static final long serialVersionUID = 6551549321039776630L;

	private final Class<?> owner;
	private final AtomicInteger useCount;

	ReflectCache(Class<?> owner) {
		super();
		this.owner = owner;
		this.useCount = new AtomicInteger();
	}

	ReflectCache acquire() {
		useCount.incrementAndGet();
		return this;
	}

	void handleCleared(ReflectRef ref) {
		boolean removed = false;
		if (remove(ref.key, ref) && isEmpty()) {
			if (useCount.get() == 0) {
				owner.setReflectCache(null);
				removed = true;
			}
		}
		if (reflectCacheDebug) {
			if (removed) {
				System.err.println("Removed reflect cache for: " + this); //$NON-NLS-1$
			} else {
				System.err.println("Retained reflect cache for: " + this + ", size: " + size()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	Object find(CacheKey key) {
		ReflectRef ref = get(key);
		return ref != null ? ref.get() : null;
	}

	void insert(CacheKey key, Object value) {
		put(key, new ReflectRef(this, key, value));
	}

	<T> T insertIfAbsent(CacheKey key, T value) {
		ReflectRef newRef = new ReflectRef(this, key, value);
		for (;;) {
			ReflectRef oldRef = putIfAbsent(key, newRef);
			if (oldRef == null) {
				return value;
			}
			T oldValue = (T) oldRef.get();
			if (oldValue != null) {
				return oldValue;
			}
			// The entry addressed by key has been cleared, but not yet removed from this map.
			// One thread will successfully replace the entry; the value stored will be shared.
			if (replace(key, oldRef, newRef)) {
				return value;
			}
		}
	}

	void release() {
		useCount.decrementAndGet();
	}

}

private transient ReflectCache reflectCache;
private static long reflectCacheOffset = -1;

private ReflectCache acquireReflectCache() {
	ReflectCache cache = reflectCache;
	if (cache == null) {
		sun.misc.Unsafe theUnsafe = getUnsafe();
		long cacheOffset = getReflectCacheOffset();
		ReflectCache newCache = new ReflectCache(this);
		do {
			// Some thread will insert this new cache making it available to all.
			if (theUnsafe.compareAndSwapObject(this, cacheOffset, null, newCache)) {
				cache = newCache;
				break;
			}
			cache = (ReflectCache) theUnsafe.getObject(this, cacheOffset);
		} while (cache == null);
	}
	return cache.acquire();
}
private static long getReflectCacheOffset() {
	long cacheOffset = reflectCacheOffset;
	if (cacheOffset < 0) {
		try {
			// Bypass the reflection cache to avoid infinite recursion.
			Field reflectCacheField = Class.class.getDeclaredFieldImpl("reflectCache"); //$NON-NLS-1$
			cacheOffset = getUnsafe().objectFieldOffset(reflectCacheField);
			reflectCacheOffset = cacheOffset;
		} catch (NoSuchFieldException e) {
			throw newInternalError(e);
		}
	}
	return cacheOffset;
}
void setReflectCache(ReflectCache cache) {
	// Lazy initialization of a non-volatile field. Ensure the Object is initialized
	// and flushed to memory before assigning to the annotationCache field.
	getUnsafe().putOrderedObject(this, getReflectCacheOffset(), cache);
}

private ReflectCache peekReflectCache() {
	return reflectCache;
}

static InternalError newInternalError(Exception cause) {
	InternalError err = new InternalError(cause.toString());
	err.setCause(cause);
	return err;
}

private Method lookupCachedMethod(String methodName, Class<?>[] parameters) {
	if (!reflectCacheEnabled) return null;

	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("lookup Method: "); //$NON-NLS-1$
		output.append(getName());
		output.append('.');
		output.append(methodName);
		System.err.println(output);
	}
	ReflectCache cache = peekReflectCache();
	if (cache != null) {
		// use a null returnType to find the Method with the largest depth
		Method method = (Method) cache.find(CacheKey.newMethodKey(methodName, parameters, null));
		if (method != null) {
			try {
				Class<?>[] orgParams = getParameterTypes(method);
				// ensure the parameter classes are identical
				if (sameTypes(parameters, orgParams)) {
					return (Method) copyMethod.invoke(method, NoArgs);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw newInternalError(e);
			}
		}
	}
	return null;
}

@sun.reflect.CallerSensitive
private Method cacheMethod(Method method) {
	if (!reflectCacheEnabled) return method;
	if (reflectCacheAppOnly && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return method;
	}
	if (copyMethod == null) return method;
	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("cache Method: "); //$NON-NLS-1$
		output.append(getName());
		output.append('.');
		output.append(method.getName());
		System.err.println(output);
	}
	try {
		Class<?>[] parameterTypes = getParameterTypes(method);
		CacheKey key = CacheKey.newMethodKey(method.getName(), parameterTypes, method.getReturnType());
		Class<?> declaringClass = method.getDeclaringClass();
		ReflectCache cache = declaringClass.acquireReflectCache();
		try {
			method = cache.insertIfAbsent(key, method);
		} finally {
			if (declaringClass != this) {
				cache.release();
				cache = acquireReflectCache();
			}
		}
		try {
			// cache the Method with the largest depth with a null returnType
			CacheKey lookupKey = CacheKey.newMethodKey(method.getName(), parameterTypes, null);
			cache.insert(lookupKey, method);
		} finally {
			cache.release();
		}
		return (Method)copyMethod.invoke(method, NoArgs);
	} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
		throw newInternalError(e);
	}
}

private Field lookupCachedField(String fieldName) {
	if (!reflectCacheEnabled) return null;

	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("lookup Field: "); //$NON-NLS-1$
		output.append(getName());
		output.append('.');
		output.append(fieldName);
		System.err.println(output);
	}
	ReflectCache cache = peekReflectCache();
	if (cache != null) {
		Field field = (Field) cache.find(CacheKey.newFieldKey(fieldName, null));
		if (field != null) {
			try {
				return (Field)copyField.invoke(field, NoArgs);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw newInternalError(e);
			}
		}
	}
	return null;
}

@sun.reflect.CallerSensitive
private Field cacheField(Field field) {
	if (!reflectCacheEnabled) return field;
	if (reflectCacheAppOnly && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return field;
	}
	if (copyField == null) return field;
	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("cache Field: "); //$NON-NLS-1$
		output.append(getName());
		output.append('.');
		output.append(field.getName());
		System.err.println(output);
	}
	CacheKey typedKey = CacheKey.newFieldKey(field.getName(), field.getType());
	Class<?> declaringClass = field.getDeclaringClass();
	ReflectCache cache = declaringClass.acquireReflectCache();
	try {
		field = cache.insertIfAbsent(typedKey, field);
		if (declaringClass == this) {
			// cache the Field returned from getField() with a null returnType
			CacheKey lookupKey = CacheKey.newFieldKey(field.getName(), null);
			cache.insert(lookupKey, field);
		}
	} finally {
		cache.release();
	}
	try {
		return (Field) copyField.invoke(field, NoArgs);
	} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
		throw newInternalError(e);
	}
}

private Constructor<T> lookupCachedConstructor(Class<?>[] parameters) {
	if (!reflectCacheEnabled) return null;

	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("lookup Constructor: "); //$NON-NLS-1$
		output.append(getName()).append('(');
		for (int i=0; i<parameters.length; i++) {
			if (i != 0) output.append(", "); //$NON-NLS-1$
			output.append(parameters[i].getName());
		}
		output.append(')');
		System.err.println(output);
	}
	ReflectCache cache = peekReflectCache();
	if (cache != null) {
		Constructor<?> constructor = (Constructor<?>) cache.find(CacheKey.newConstructorKey(parameters));
		if (constructor != null) {
			Class<?>[] orgParams = getParameterTypes(constructor);
			try {
				// ensure the parameter classes are identical
				if (sameTypes(orgParams, parameters)) {
					return (Constructor<T>) copyConstructor.invoke(constructor, NoArgs);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw newInternalError(e);
			}
		}
	}
	return null;
}

@sun.reflect.CallerSensitive
private Constructor<T> cacheConstructor(Constructor<T> constructor) {
	if (!reflectCacheEnabled) return constructor;
	if (reflectCacheAppOnly && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return constructor;
	}
	if (copyConstructor == null) return constructor;
	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("cache Constructor: "); //$NON-NLS-1$
		output.append(getName()).append('(');
		Class<?>[] params = constructor.getParameterTypes();
		for (int i=0; i<params.length; i++) {
			if (i != 0) output.append(", "); //$NON-NLS-1$
			output.append(params[i].getName());
		}
		output.append(')');
		System.err.println(output);
	}
	ReflectCache cache = acquireReflectCache();
	try {
		CacheKey key = CacheKey.newConstructorKey(getParameterTypes(constructor));
		cache.insert(key, constructor);
	} finally {
		cache.release();
	}
	try {
		return (Constructor<T>) copyConstructor.invoke(constructor, NoArgs);
	} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
		throw newInternalError(e);
	}
}

private static Method[] copyMethods(Method[] methods) {
	Method[] result = new Method[methods.length];
	try {
		for (int i=0; i<methods.length; i++) {
			result[i] = (Method)copyMethod.invoke(methods[i], NoArgs);
		}
		return result;
	} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
		throw newInternalError(e);
	}
}

private Method[] lookupCachedMethods(CacheKey cacheKey) {
	if (!reflectCacheEnabled) return null;

	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("lookup Methods in: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	ReflectCache cache = peekReflectCache();
	if (cache != null) {
		Method[] methods = (Method[]) cache.find(cacheKey);
		if (methods != null) {
			return copyMethods(methods);
		}
	}
	return null;
}

@sun.reflect.CallerSensitive
private Method[] cacheMethods(Method[] methods, CacheKey cacheKey) {
	if (!reflectCacheEnabled) return methods;
	if (reflectCacheAppOnly && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return methods;
	}
	if (copyMethod == null) return methods;
	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("cache Methods: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	ReflectCache cache = null;
	Class<?> cacheOwner = null;
	try {
		for (int i = 0; i < methods.length; ++i) {
			Method method = methods[i];
			CacheKey key = CacheKey.newMethodKey(method.getName(), getParameterTypes(method), method.getReturnType());
			Class<?> declaringClass = method.getDeclaringClass();
			if (cacheOwner != declaringClass || cache == null) {
				if (cache != null) {
					cache.release();
					cache = null;
				}
				cache = declaringClass.acquireReflectCache();
				cacheOwner = declaringClass;
			}
			methods[i] = cache.insertIfAbsent(key, method);
		}
		if (cache != null && cacheOwner != this) {
			cache.release();
			cache = null;
		}
		if (cache == null) {
			cache = acquireReflectCache();
		}
		cache.insert(cacheKey, methods);
	} finally {
		if (cache != null) {
			cache.release();
		}
	}
	return copyMethods(methods);
}

private static Field[] copyFields(Field[] fields) {
	Field[] result = new Field[fields.length];
	try {
		for (int i=0; i<fields.length; i++) {
			result[i] = (Field)copyField.invoke(fields[i], NoArgs);
		}
		return result;
	} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
		throw newInternalError(e);
	}
}

private Field[] lookupCachedFields(CacheKey cacheKey) {
	if (!reflectCacheEnabled) return null;

	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("lookup Fields in: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	ReflectCache cache = peekReflectCache();
	if (cache != null) {
		Field[] fields = (Field[]) cache.find(cacheKey);
		if (fields != null) {
			return copyFields(fields);
		}
	}
	return null;
}

@sun.reflect.CallerSensitive
private Field[] cacheFields(Field[] fields, CacheKey cacheKey) {
	if (!reflectCacheEnabled) return fields;
	if (reflectCacheAppOnly && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return fields;
	}
	if (copyField == null) return fields;
	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("cache Fields: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	ReflectCache cache = null;
	Class<?> cacheOwner = null;
	try {
		for (int i = 0; i < fields.length; ++i) {
			Field field = fields[i];
			Class<?> declaringClass = field.getDeclaringClass();
			if (cacheOwner != declaringClass || cache == null) {
				if (cache != null) {
					cache.release();
					cache = null;
				}
				cache = declaringClass.acquireReflectCache();
				cacheOwner = declaringClass;
			}
			fields[i] = cache.insertIfAbsent(CacheKey.newFieldKey(field.getName(), field.getType()), field);
		}
		if (cache != null && cacheOwner != this) {
			cache.release();
			cache = null;
		}
		if (cache == null) {
			cache = acquireReflectCache();
		}
		cache.insert(cacheKey, fields);
	} finally {
		if (cache != null) {
			cache.release();
		}
	}
	return copyFields(fields);
}

private static <T> Constructor<T>[] copyConstructors(Constructor<T>[] constructors) {
	Constructor<T>[] result = new Constructor[constructors.length];
	try {
		for (int i=0; i<constructors.length; i++) {
			result[i] = (Constructor<T>) copyConstructor.invoke(constructors[i], NoArgs);
		}
		return result;
	} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
		throw newInternalError(e);
	}
}

private Constructor<T>[] lookupCachedConstructors(CacheKey cacheKey) {
	if (!reflectCacheEnabled) return null;

	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("lookup Constructors in: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	ReflectCache cache = peekReflectCache();
	if (cache != null) {
		Constructor<T>[] constructors = (Constructor<T>[]) cache.find(cacheKey);
		if (constructors != null) {
			return copyConstructors(constructors);
		}
	}
	return null;
}

@sun.reflect.CallerSensitive
private Constructor<T>[] cacheConstructors(Constructor<T>[] constructors, CacheKey cacheKey) {
	if (!reflectCacheEnabled) return constructors;
	if (reflectCacheAppOnly && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return constructors;
	}
	if (copyConstructor == null) return constructors;
	if (reflectCacheDebug) {
		StringBuilder output = new StringBuilder(200);
		output.append("cache Constructors: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	ReflectCache cache = acquireReflectCache();
	try {
		for (int i=0; i<constructors.length; i++) {
			CacheKey key = CacheKey.newConstructorKey(getParameterTypes(constructors[i]));
			constructors[i] = cache.insertIfAbsent(key, constructors[i]);
		}
		cache.insert(cacheKey, constructors);
	} finally {
		cache.release();
	}
	return copyConstructors(constructors);
}

private static <T> Constructor<T> checkParameterTypes(Constructor<T> constructor, Class<?>[] parameterTypes) {
	Class<?>[] constructorParameterTypes = getParameterTypes(constructor);
	return sameTypes(constructorParameterTypes, parameterTypes) ? constructor : null;
}

static boolean sameTypes(Class<?>[] aTypes, Class<?>[] bTypes) {
	if (aTypes == null) {
		if (bTypes == null) {
			return true;
		}
	} else if (bTypes != null) {
		int length = aTypes.length;
		if (length == bTypes.length) {
			for (int i = 0; i < length; ++i) {
				if (aTypes[i] != bTypes[i]) {
					return false;
				}
			}
			return true;
		}
	}
	return false;
}

Object getMethodHandleCache() {
	return methodHandleCache;
}

Object setMethodHandleCache(Object cache) {
	Object result = methodHandleCache;
	if (null == result) {
		synchronized (this) {
			result = methodHandleCache;
			if (null == result) {
				methodHandleCache = cache;
				result = cache;
			}
		}
	}
	return result;
}

/**
 * Check if a field can be reflected by Field.
 *
 * PRE: Assumes @Packed support is runtime-enabled.
 *
 * @param field The field to test.
 * @return true if the field is non-null, and:
 * (1) its type is in the PackedObject class hierarchy; or
 * (2) it is an instance field of a PackedObject class.
 */
private final boolean fieldRequiresPacked(Field field)
{
	if (null != field) {
		if (PackedObject.isPackedArray(this)
			|| (PackedObject.class.isAssignableFrom(this) && (false == Modifier.isStatic(field.getModifiers())))) {
			return true;
		}
	}
	return false;
}

/**
 * Filter out fields that cannot be reflected by Field.
 *
 * PRE: Assumes @Packed support is runtime-enabled.
 *
 * @param fields A non-null array of fields. The array may be modified.
 * @return A filtered array of fields. If no fields were filtered out, the returned array
 * may be the same as the input fields array.
 */
private final Field[] filterPackedFields(Field[] fields)
{
	final boolean classIsPacked = PackedObject.class.isAssignableFrom(this);
	int keepIdx = 0;

	if (PackedObject.isPackedArray(this)) {
		return new Field[0];
	}

	/* compact the fields array, removing ineligible fields */
	for (int lookIdx = 0; lookIdx < fields.length; lookIdx++) {
		if (classIsPacked && (false == Modifier.isStatic(fields[lookIdx].getModifiers()))) {
			/* don't keep field */
		} else {
			fields[keepIdx] = fields[lookIdx];
			keepIdx++;
		}
	}

	/* if fields were removed, make a shorter copy of the array */
	if (keepIdx < fields.length) {
		fields = Arrays.copyOf(fields, keepIdx);
	}

	return fields;
}

}
