/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2010, 2016  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.ConvertHandle.FilterHelpers;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ibm.jvm.packed.PackedObject;
import com.ibm.oti.util.Msg;
import com.ibm.oti.lang.ArgumentHelper;
import com.ibm.oti.vm.VM;
import com.ibm.oti.vm.VMLangAccess;

/**
 * Factory class for creating and adapting MethodHandles.
 *
 * @since 1.7
 */
public class MethodHandles {
	@sun.reflect.CallerSensitive
	static final native Class<?> getStackClass(int depth);

	MethodHandles() {
	}

	/**
	 * A factory for creating MethodHandles that require access-checking on creation.
	 * <p>
	 * Unlike Reflection, MethodHandle only requires access-checking when the MethodHandle
	 * is created, not when it is invoked.
	 * <p>
	 * This class provides the lookup authentication necessary when creating MethodHandles.  Any
	 * number of MethodHandles can be lookup using this token, and the token can be shared to provide
	 * others with the the "owner's" authentication level.
	 * <p>
	 * Sharing {@link Lookup} objects should be done with care, as they may allow access to private
	 * methods.
	 * <p>
	 * When using the lookup factory methods (find and unreflect methods), creating the MethodHandle
	 * may fail because the method's arity is too high.
	 *
	 */
	static final public class Lookup {

		/**
		 * Bit flag 0x1 representing <i>public</i> access.  See {@link #lookupModes()}.
		 */
		public static final int PUBLIC = Modifier.PUBLIC;

		/**
		 * Bit flag 0x2 representing <i>private</i> access.  See {@link #lookupModes()}.
		 */
		public static final int PRIVATE = Modifier.PRIVATE;

		/**
		 * Bit flag 0x4 representing <i>protected</i> access.  See {@link #lookupModes()}.
		 */
		public static final int PROTECTED = Modifier.PROTECTED;

		/**
		 * Bit flag 0x8 representing <i>package</i> access.  See {@link #lookupModes()}.
		 */
		public static final int PACKAGE = 0x8;

		static final int INTERNAL_PRIVILEGED = 0x10;

		private static final int FULL_ACCESS_MASK = PUBLIC | PRIVATE | PROTECTED | PACKAGE;
		private static final int NO_ACCESS = 0;

		private static final String INVOKE_EXACT = "invokeExact"; //$NON-NLS-1$
		private static final String INVOKE = "invoke"; //$NON-NLS-1$
		static final int VARARGS = 0x80;

		/* single cached value of public Lookup object */
		static Lookup publicLookup = new Lookup(Object.class, Lookup.PUBLIC);

		/* single cached internal privileged lookup */
		static Lookup internalPrivilegedLookup = new Lookup(MethodHandle.class, Lookup.INTERNAL_PRIVILEGED);
		static Lookup IMPL_LOOKUP = internalPrivilegedLookup; /* hack for b56 of lambda-dev */

		/* Access token used in lookups - Object for public lookup */
		final Class<?> accessClass;
		final int accessMode;
		private final  boolean performSecurityCheck;

		Lookup(Class<?> lookupClass, int lookupMode, boolean doCheck) {
			this.performSecurityCheck = doCheck;
			if (doCheck && (INTERNAL_PRIVILEGED != lookupMode)) {
				if ( lookupClass.getName().startsWith("java.lang.invoke.")) {  //$NON-NLS-1$
					// K0588 = Illegal Lookup object - originated from java.lang.invoke: {0}
					throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0588", lookupClass.getName())); //$NON-NLS-1$
				}
			}

			accessClass = lookupClass;
			accessMode = lookupMode;
		}

		Lookup(Class<?> lookupClass, int lookupMode) {
			this(lookupClass, lookupMode, true);
		}

		Lookup(Class<?> lookupClass) {
			this(lookupClass, FULL_ACCESS_MASK, true);
		}

		Lookup(Class<?> lookupClass, boolean performSecurityCheck) {
			 this(lookupClass, FULL_ACCESS_MASK, performSecurityCheck);
		}

		/**
		 * A query to determine the lookup capabilities held by this instance.
		 *
		 * @return the lookup mode bit mask for this Lookup object
		 */
		public int lookupModes() {
			return accessMode;
		}

		/*
		 * Is the varargs bit set?
		 */
		static boolean isVarargs(int modifiers) {
			return (modifiers & VARARGS) != 0;
		}

		/* If the varargs bit is set, wrap the MethodHandle with an
		 * asVarargsCollector adapter.
		 * Last class type will be Object[] if not otherwise appropriate.
		 */
		private static MethodHandle convertToVarargsIfRequired(MethodHandle handle) {
			if (isVarargs(handle.rawModifiers)) {
				Class<?> lastClass = handle.type.lastParameterType();
				return handle.asVarargsCollector(lastClass);
			}
			return handle;
		}

		/**
		 * Return an early-bound method handle to a non-static method.  The receiver must
		 * have a Class in its hierarchy that provides the virtual method named methodName.
		 * <p>
		 * The final MethodType of the MethodHandle will be identical to that of the method.
		 * The receiver will be inserted prior to the call and therefore does not need to be
		 * included in the MethodType.
		 *
		 * @param receiver - The Object to insert as a receiver.  Must implement the methodName
		 * @param methodName - The name of the method
		 * @param type - The MethodType describing the method
		 * @return a MethodHandle to the required method.
		 * @throws IllegalAccessException if access is denied
		 * @throws NoSuchMethodException if the method doesn't exist
		 */
		public MethodHandle bind(Object receiver, String methodName, MethodType type) throws IllegalAccessException, NoSuchMethodException {
			nullCheck(receiver, methodName, type);
			Class<?> receiverClass = receiver.getClass();
			MethodHandle handle = handleForMHInvokeMethods(receiverClass, methodName, type);
			if (handle == null) {
				// Use the priviledgedLookup to allow probe the findSpecial cache without restricting the receiver
				handle = internalPrivilegedLookup.findSpecialImpl(receiverClass, methodName, type, receiverClass);
				handle = handle.asFixedArity(); // remove unnecessary varargsCollector from the middle of the MH chain.
				handle = convertToVarargsIfRequired(handle.bindTo(receiver));
			}
			checkAccess(handle);
			checkSecurity(handle.defc, receiverClass, handle.rawModifiers);

			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle);

			return handle;
		}

		private static void nullCheck(Object a, Object b) {
			// use implicit null checks
			a.getClass();
			b.getClass();
		}

		private static void nullCheck(Object a, Object b, Object c) {
			// use implicit null checks
			a.getClass();
			b.getClass();
			c.getClass();
		}

		private static void nullCheck(Object a, Object b, Object c, Object d) {
			// use implicit null checks
			a.getClass();
			b.getClass();
			c.getClass();
			d.getClass();
		}

		private static void initCheck(String methodName) throws NoSuchMethodException {
			if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) { //$NON-NLS-1$ //$NON-NLS-2$
				// K05ce = Invalid method name: {0}
				throw new NoSuchMethodException(Msg.getString("K05ce", methodName)); //$NON-NLS-1$
			}
		}

		/* We use this because VM.javalangVMaccess is not set when this class is loaded.
		 * Delay grabbing it until we need it, which by then the value will be set. */
		private static final class VMLangAccessGetter {
			public static final VMLangAccess vma = VM.getVMLangAccess();
		}

		static final VMLangAccess getVMLangAccess() {
			return VMLangAccessGetter.vma;
		}

		/* Verify two classes share the same package in a way to avoid Class.getPackage()
		 * and the security checks that go with it.
		 */
		private static boolean isSamePackage(Class<?> a, Class<?> b){
			// Two of the same class share a package
			if (a == b){
				return true;
			}

			VMLangAccess vma = getVMLangAccess();

			// If the string value is different, they're definitely not related
			if(!vma.getPackageName(a).equals(vma.getPackageName(b))) {
				return false;
			}

			ClassLoader cla = vma.getClassloader(a);
			ClassLoader clb = vma.getClassloader(b);

			// If both share the same classloader, then they are the same package
			if (cla == clb) {
				return true;
			}

			// If one is an ancestor of the other, they are also the same package
			if (vma.isAncestor(cla, clb) || vma.isAncestor(clb, cla)) {
				return true;
			}

			return false;
		}

		/* Equivalent of visible.c checkVisibility(); */
		void checkAccess(MethodHandle handle) throws IllegalAccessException {
			if (accessMode == INTERNAL_PRIVILEGED) {
				// Full access for use by MH implementation.
				return;
			}

			int handleModifiers = handle.rawModifiers;
			if (accessMode == NO_ACCESS) {
				throw new IllegalAccessException(this.toString());
			}

			if (Modifier.isPublic(handleModifiers)) {
				/* public method - class is either public or package
				 * if public, just need public access to call it
				 * if package, need to be in same package with package access
				 *
				 * Protected classes are public, and private classes are package.
				 */
				int handleDefcModifiers = handle.defc.getModifiers();
				if (Modifier.isPublic(handleDefcModifiers) || Modifier.isProtected(handleDefcModifiers)) {
					/* Class is public or protected access */
					if (Modifier.isPublic(accessMode) || Modifier.isProtected(accessMode)) {
						return;
					}
				} else {
					/* Class is package or private access */
					if ((((accessMode & PACKAGE) == PACKAGE) || Modifier.isPrivate(accessMode)) && isSamePackage(accessClass, handle.defc)) {
						/* 1) in same package with package access */
						return;
					}
				}
			} else if (Modifier.isPrivate(handleModifiers)) {
				/* Private */
				if (handle.defc == accessClass && Modifier.isPrivate(accessMode)) {
					return;
				}
			} else if (Modifier.isProtected(handleModifiers)) {
				/* Protected */
				if (accessMode != PUBLIC) {
					if (handle.definingClass.isArray()) {
						/* The only methods array classes have are defined on Object and thus accessible */
						return;
					}
					if (isSamePackage(accessClass, handle.defc)) {
						/* isSamePackage handles accessClass == handle.defc */
						return;
					}
					if (!accessClass.isInterface()) {
						if (Modifier.isProtected(accessMode) && handle.defc.isAssignableFrom(accessClass)) {
							if ((handle.kind == MethodHandle.KIND_CONSTRUCTOR)
							|| ((handle.kind == MethodHandle.KIND_SPECIAL) && !handle.directHandleOriginatedInFindVirtual())
							) {
								Class<?> targetClass = handle.definingClass;
								if (handle.kind == MethodHandle.KIND_SPECIAL) {
									targetClass = handle.specialCaller;
								}
								if (accessClass.isAssignableFrom(targetClass)) {
									/* success */
									return;
								}
							} else {
								/* MethodHandle.KIND_GETFIELD, MethodHandle.KIND_PUTFIELD & MethodHandle.KIND_VIRTUAL
								 * restrict the receiver to be a subclass under the current class and thus don't need
								 * additional access checks.
								 */
								return;
							}
						}
					}
				}
			} else {
				/* default (package access) */
				if (((accessMode & PACKAGE) == PACKAGE) && isSamePackage(accessClass, handle.defc)){
					return;
				}
			}
			// K0587 = '{0}' no access to: '{1}'
			String message = com.ibm.oti.util.Msg.getString("K0587", this.toString(), handle.defc.getName() + "." + handle.name + ":" + handle.type + "/" + handle.mapKindToBytecode());  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			throw new IllegalAccessException(message);
		}

		private void checkSpecialAccess(Class<?> callerClass) throws IllegalAccessException {
			if (isWeakenedLookup() || accessClass != callerClass) {
				// K0585 = {0} could not access {1} - private access required
				throw new IllegalAccessException(com.ibm.oti.util.Msg.getString("K0585", accessClass.getName(), callerClass.getName())); //$NON-NLS-1$
			}
		}

		/**
		 * Return a MethodHandle bound to a specific-implementation of a virtual method, as if created by an invokespecial bytecode
		 * using the class specialToken.  The method and all Classes in its MethodType must be accessible to
		 * the caller.
		 * <p>
		 * The receiver class will be added to the MethodType of the handle, but dispatch will not occur based
		 * on the receiver.
		 *
		 * @param clazz - the class or interface from which defines the method
		 * @param methodName - the method name
		 * @param type - the MethodType of the method
		 * @param specialToken - the calling class for the invokespecial
		 * @return a MethodHandle
		 * @throws IllegalAccessException - if the method is static or access checking fails
		 * @throws NullPointerException - if clazz, methodName, type or specialToken is null
		 * @throws NoSuchMethodException - if clazz has no instance method named methodName with signature matching type
		 * @throws SecurityException - if any installed SecurityManager denies access to the method
		 */
		public MethodHandle findSpecial(Class<?> clazz, String methodName, MethodType type, Class<?> specialToken) throws IllegalAccessException, NoSuchMethodException, SecurityException, NullPointerException {
			nullCheck(clazz, methodName, type, specialToken);
			checkSpecialAccess(specialToken);	/* Must happen before method resolution */
			MethodHandle handle = null;
			try {
				handle = findSpecialImpl(clazz, methodName, type, specialToken);
				if ((handle.defc != accessClass) && !handle.defc.isAssignableFrom(accessClass)) {
					// K0586 = Lookup class ({0}) must be the same as or subclass of the current class ({1})
					throw new IllegalAccessException(com.ibm.oti.util.Msg.getString("K0586", accessClass, handle.defc)); //$NON-NLS-1$
				}
				checkAccess(handle);
				checkSecurity(handle.defc, clazz, handle.rawModifiers);
				handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle);
			} catch (DefaultMethodConflictException e) {
				/* Method resolution revealed conflicting default method definitions. Return a MethodHandle that throws an
				 * IncompatibleClassChangeError when invoked, and who's type matches the requested type.
				 */
				handle = throwExceptionWithMethodTypeAndMessage(
						IncompatibleClassChangeError.class,
						type.insertParameterTypes(0, specialToken),
						e.getCause().getMessage());
			}
			return handle;
		}

		/*
		 * Lookup the findSpecial handle either from the special handle cache, or create a new handle and install it in the cache.
		 */
		private MethodHandle findSpecialImpl(Class<?> clazz, String methodName, MethodType type, Class<?> specialToken) throws IllegalAccessException, NoSuchMethodException, SecurityException, NullPointerException {
			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getSpecialCache(clazz);
			MethodHandle handle = HandleCache.getMethodWithSpecialCallerFromPerClassCache(cache, methodName, type, specialToken);
			if (handle == null) {
				initCheck(methodName);
				handle = new DirectHandle(clazz, methodName, type, MethodHandle.KIND_SPECIAL, specialToken);
				if (internalPrivilegedLookup != this) {
					handle = restrictReceiver(handle);
				}
				handle = convertToVarargsIfRequired(handle);
				HandleCache.putMethodWithSpecialCallerInPerClassCache(cache, methodName, type, handle, specialToken);
			}
			return handle;
		}

		/**
		 * Return a MethodHandle to a static method.  The MethodHandle will have the same type as the
		 * method.  The method and all classes in its type must be accessible to the caller.
		 *
		 * @param clazz - the class defining the method
		 * @param methodName - the name of the method
		 * @param type - the MethodType of the method
		 * @return A MethodHandle able to invoke the requested static method
		 * @throws IllegalAccessException - if the method is not static or access checking fails
		 * @throws NullPointerException - if clazz, methodName or type is null
		 * @throws NoSuchMethodException - if clazz has no static method named methodName with signature matching type
		 * @throws SecurityException - if any installed SecurityManager denies access to the method
		 */
		public MethodHandle findStatic(Class<?> clazz, String methodName, MethodType type) throws IllegalAccessException, NoSuchMethodException {
			nullCheck(clazz, methodName, type);
			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getStaticCache(clazz);
			MethodHandle handle = HandleCache.getMethodFromPerClassCache(cache, methodName, type);
			if (handle == null) {
				initCheck(methodName);
				handle = new DirectHandle(clazz, methodName, type, MethodHandle.KIND_STATIC, null);
				handle = HandleCache.putMethodInPerClassCache(cache, methodName, type, convertToVarargsIfRequired(handle));
			}
			checkAccess(handle);
			checkSecurity(handle.defc, clazz, handle.rawModifiers);
			/* Static check is performed by native code */
			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle);
			return handle;
		}

		/**
		 * Return a MethodHandle to a virtual method.  The method will be looked up in the first argument
		 * (aka receiver) prior to dispatch.  The type of the MethodHandle will be that of the method
		 * with the receiver type prepended.
		 *
		 * @param clazz - the class defining the method
		 * @param methodName - the name of the method
		 * @param type - the type of the method
		 * @return a MethodHandle that will do virtual dispatch on the first argument
		 * @throws IllegalAccessException - if method is static or access is refused
		 * @throws NullPointerException - if clazz, methodName or type is null
		 * @throws NoSuchMethodException - if clazz has no virtual method named methodName with signature matching type
		 * @throws SecurityException - if any installed SecurityManager denies access to the method
		 */
		public MethodHandle findVirtual(Class<?> clazz, String methodName, MethodType type) throws IllegalAccessException, NoSuchMethodException {
			nullCheck(clazz, methodName, type);

			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getVirtualCache(clazz);
			MethodHandle handle = HandleCache.getMethodFromPerClassCache(cache, methodName, type);
			if (handle == null) {
				handle = handleForMHInvokeMethods(clazz, methodName, type);
			}
			if (handle == null) {
				initCheck(methodName);

				if (clazz.isInterface()) {
					handle = new InterfaceHandle(clazz, methodName, type);
					if (Modifier.isStatic(handle.rawModifiers)) {
						throw new IllegalAccessException();
					}
					handle = adaptInterfaceLookupsOfObjectMethodsIfRequired(handle, clazz, methodName, type);
				} else {
					handle = new DirectHandle(clazz, methodName, type, MethodHandle.KIND_VIRTUAL, clazz, true);
					/* Static check is performed by native code */
					if (!Modifier.isPrivate(handle.rawModifiers) && !Modifier.isFinal(handle.rawModifiers)) {
						handle = new VirtualHandle((DirectHandle) handle);
					}
				}
				handle = convertToVarargsIfRequired(handle);
				HandleCache.putMethodInPerClassCache(cache, methodName, type, handle);
			}
			handle = restrictReceiver(handle);
			checkAccess(handle);
			checkSecurity(handle.defc, clazz, handle.rawModifiers);
			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle);
			return handle;
		}

		/**
		 * Restrict the receiver as indicated in the JVMS for invokespecial and invokevirtual.
		 * <blockquote>
		 * Finally, if the resolved method is protected (4.6), and it is a member of a superclass of the current class, and
		 * the method is not declared in the same runtime package (5.3) as the current class, then the class of objectref
		 * must be either the current class or a subclass of the current class.
		 * </blockquote>
		 */
		private MethodHandle restrictReceiver(MethodHandle handle) {
			if (!Modifier.isStatic(handle.rawModifiers)
			&& Modifier.isProtected(handle.rawModifiers)
			&& (handle.defc != accessClass)
			&& (handle.defc.isAssignableFrom(accessClass))
			&& (!isSamePackage(handle.defc, accessClass))
			) {
				handle = handle.cloneWithNewType(handle.type.changeParameterType(0, accessClass));
			}
			return handle;
		}

		/**
		 * Adapt InterfaceHandles on public Object methods if the method is not redeclared in the interface class.
		 * Public methods of Object are implicitly members of interfaces and do not receive iTable indexes.
		 * If the declaring class is Object, create a VirtualHandle and asType it to the interface class.
		 * @param handle An InterfaceHandle
		 * @param clazz The lookup class
		 * @param methodName The lookup name
		 * @param type The lookup type
		 * @return Either the original handle or an adapted one for Object methods.
		 * @throws NoSuchMethodException
		 * @throws IllegalAccessException
		 */
		static MethodHandle adaptInterfaceLookupsOfObjectMethodsIfRequired(MethodHandle handle, Class<?> clazz, String methodName, MethodType type) throws NoSuchMethodException, IllegalAccessException {
			assert handle instanceof InterfaceHandle;
			/* Object methods need to be treated specially if the interface hasn't declared them itself */
			if (Object.class == handle.defc) {
				if (!Modifier.isPublic(handle.rawModifiers)) {
					/* Interfaces only inherit *public* methods from Object */
					throw new NoSuchMethodException(clazz + "." + methodName + type); //$NON-NLS-1$
				}
				handle = new VirtualHandle(new DirectHandle(Object.class, methodName, type, MethodHandle.KIND_SPECIAL, Object.class));
				handle = handle.cloneWithNewType(handle.type.changeParameterType(0, clazz));
			}
			return handle;
		}

		/*
		 * Check for methods MethodHandle.invokeExact or MethodHandle.invoke.
		 * Access checks are not required as these methods are public and therefore
		 * accessible to everyone.
		 */
		static MethodHandle handleForMHInvokeMethods(Class<?> clazz, String methodName, MethodType type) {
			if (MethodHandle.class.isAssignableFrom(clazz)) {
				if (INVOKE_EXACT.equals(methodName)) {
					return type.getInvokeExactHandle();
				} else if (INVOKE.equals(methodName))  {
					return new InvokeGenericHandle(type);
				}
			}
			return null;
		}

		/**
		 * Return a MethodHandle that provides read access to a field.
		 * The MethodHandle will have a MethodType taking a single
		 * argument with type <code>clazz</code> and returning something of
		 * type <code>fieldType</code>.
		 *
		 * @param clazz - the class defining the field
		 * @param fieldName - the name of the field
		 * @param fieldType - the type of the field
		 * @return a MethodHandle able to return the value of the field
		 * @throws IllegalAccessException if access is denied or the field is static
		 * @throws NoSuchFieldException if no field is found with given name and type in clazz
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		public MethodHandle findGetter(Class<?> clazz, String fieldName, Class<?> fieldType) throws IllegalAccessException, NoSuchFieldException, SecurityException, NullPointerException {
			nullCheck(clazz, fieldName, fieldType);
			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getFieldGetterCache(clazz);
			MethodHandle handle = HandleCache.getFieldFromPerClassCache(cache, fieldName, fieldType);
			if (handle == null) {
				if (VM.PACKED_SUPPORT_ENABLED) {
					if (PackedObject.isPackedClass(clazz)) {
						// PACKED8 = This operation is not supported for @Packed classes.
						throw new UnsupportedOperationException(Msg.getString("PACKED8")); //$NON-NLS-1$
					}
				}
				handle = new FieldGetterHandle(clazz, fieldName, fieldType, accessClass);
				HandleCache.putFieldInPerClassCache(cache, fieldName, fieldType, handle);
			}
			checkAccess(handle);
			checkSecurity(handle.defc, clazz, handle.rawModifiers);
			return handle;
		}

		/**
		 * Return a MethodHandle that provides read access to a field.
		 * The MethodHandle will have a MethodType taking no arguments
		 * and returning something of type <code>fieldType</code>.
		 *
		 * @param clazz - the class defining the field
		 * @param fieldName - the name of the field
		 * @param fieldType - the type of the field
		 * @return a MethodHandle able to return the value of the field
		 * @throws IllegalAccessException if access is denied or the field is not static
		 * @throws NoSuchFieldException if no field is found with given name and type in clazz
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		public MethodHandle findStaticGetter(Class<?> clazz, String fieldName, Class<?> fieldType) throws IllegalAccessException, NoSuchFieldException, SecurityException, NullPointerException {
			nullCheck(clazz, fieldName, fieldType);
			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getStaticFieldGetterCache(clazz);
			MethodHandle handle = HandleCache.getFieldFromPerClassCache(cache, fieldName, fieldType);
			if (handle == null) {
				handle = new StaticFieldGetterHandle(clazz, fieldName, fieldType, accessClass);
				HandleCache.putFieldInPerClassCache(cache, fieldName, fieldType, handle);
			}
			checkAccess(handle);
			checkSecurity(handle.defc, clazz, handle.rawModifiers);
			return handle;
		}

		/**
		 * Return a MethodHandle that provides write access to a field.
		 * The MethodHandle will have a MethodType taking a two
		 * arguments, the first with type <code>clazz</code> and the second with
		 * type <code>fieldType</code>, and returning void.
		 *
		 * @param clazz - the class defining the field
		 * @param fieldName - the name of the field
		 * @param fieldType - the type of the field
		 * @return a MethodHandle able to set the value of the field
		 * @throws IllegalAccessException if access is denied
		 * @throws NoSuchFieldException if no field is found with given name and type in clazz
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		public MethodHandle findSetter(Class<?> clazz, String fieldName, Class<?> fieldType) throws IllegalAccessException, NoSuchFieldException, SecurityException, NullPointerException {
			nullCheck(clazz, fieldName, fieldType);
			if (fieldType == void.class) {
				throw new NoSuchFieldException();
			}
			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getFieldSetterCache(clazz);
			MethodHandle handle = HandleCache.getFieldFromPerClassCache(cache, fieldName, fieldType);
			if (handle == null) {
				if (VM.PACKED_SUPPORT_ENABLED) {
					if (PackedObject.isPackedClass(clazz)) {
						// PACKED8 = This operation is not supported for @Packed classes.
						throw new UnsupportedOperationException(Msg.getString("PACKED8")); //$NON-NLS-1$
					}
				}
				handle = new FieldSetterHandle(clazz, fieldName, fieldType, accessClass);
				HandleCache.putFieldInPerClassCache(cache, fieldName, fieldType, handle);
			}
			if (Modifier.isFinal(handle.rawModifiers)) {
				// K05cf = illegal setter on final field
				throw new IllegalAccessException(Msg.getString("K05cf")); //$NON-NLS-1$
			}
			checkSecurity(handle.defc, clazz, handle.rawModifiers);
			checkAccess(handle);
			return handle;
		}

		/**
		 * Return a MethodHandle that provides write access to a field.
		 * The MethodHandle will have a MethodType taking one argument
		 * of type <code>fieldType</code> and returning void.
		 *
		 * @param clazz - the class defining the field
		 * @param fieldName - the name of the field
		 * @param fieldType - the type of the field
		 * @return a MethodHandle able to set the value of the field
		 * @throws IllegalAccessException if access is denied
		 * @throws NoSuchFieldException if no field is found with given name and type in clazz
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		public MethodHandle findStaticSetter(Class<?> clazz, String fieldName, Class<?> fieldType) throws IllegalAccessException, NoSuchFieldException, SecurityException, NullPointerException {
			nullCheck(clazz, fieldName, fieldType);
			if (fieldType == void.class) {
				throw new NoSuchFieldException();
			}
			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getStaticFieldSetterCache(clazz);
			MethodHandle handle = HandleCache.getFieldFromPerClassCache(cache, fieldName, fieldType);
			if (handle == null) {
				handle = new StaticFieldSetterHandle(clazz, fieldName, fieldType, accessClass);
				HandleCache.putFieldInPerClassCache(cache, fieldName, fieldType, handle);
			}

			if (Modifier.isFinal(handle.rawModifiers)) {
				// K05cf = illegal setter on final field
				throw new IllegalAccessException(Msg.getString("K05cf")); //$NON-NLS-1$
			}
			checkAccess(handle);
			checkSecurity(handle.defc, clazz, handle.rawModifiers);
			return handle;
		}

		/**
		 * Create a lookup on the request class.  The resulting lookup will have no more
		 * access privileges than the original.
		 *
		 * @param lookupClass - the class to create the lookup on
		 * @return a new MethodHandles.Lookup object
		 */
		public MethodHandles.Lookup in(Class<?> lookupClass){
			lookupClass.getClass();	// implicit null check

			// If it's the same class as ourselves, return this
			if (lookupClass == accessClass) {
				return this;
			}

			int newAccessMode = accessMode & ~PROTECTED;

			if (!isSamePackage(accessClass,lookupClass)) {
				newAccessMode &= ~(PACKAGE | PROTECTED);
			}

			if ((newAccessMode & PRIVATE) == PRIVATE){
				Class<?> a = getUltimateEnclosingClassOrSelf(accessClass);
				Class<?> l = getUltimateEnclosingClassOrSelf(lookupClass);
				if (a != l) {
					newAccessMode &= ~PRIVATE;
				}
			}

			if(!Modifier.isPublic(lookupClass.getModifiers())){
				if(isSamePackage(accessClass, lookupClass)) {
					if ((accessMode & PACKAGE) == 0) {
						newAccessMode = NO_ACCESS;
					}
				} else {
					newAccessMode = NO_ACCESS;
				}
			} else {
				VMLangAccess vma = getVMLangAccess();
				if (vma.getClassloader(accessClass) != vma.getClassloader(lookupClass)) {
					newAccessMode &= ~(PACKAGE | PRIVATE | PROTECTED);
				}
			}

			return new Lookup(lookupClass, newAccessMode);
		}

		/*
		 * Get the top level class for a given class or return itself.
		 */
		private static Class<?> getUltimateEnclosingClassOrSelf(Class<?> c) {
			Class<?> enclosing = c.getEnclosingClass();
			Class<?> previous = c;

			while (enclosing != null) {
				previous = enclosing;
				enclosing = enclosing.getEnclosingClass();
			}
			return previous;
		}

		/*
		 * Determine if 'currentClassLoader' is the same or a child of the requestedLoader.  Necessary
		 * for access checking.
		 */
		private static boolean doesClassLoaderDescendFrom(ClassLoader currentLoader, ClassLoader requestedLoader) {
			if (requestedLoader == null) {
				/* Bootstrap loader is parent of everyone */
				return true;
			}
			if (currentLoader != requestedLoader) {
				while (currentLoader != null) {
					if (currentLoader == requestedLoader) {
						return true;
					}
					currentLoader = currentLoader.getParent();
				}
				return false;
			}
			return true;
		}

		/**
		 * The class being used for visibility checks and access permissions.
		 *
		 * @return The class used in by this Lookup object for access checking
		 */
		public Class<?> lookupClass() {
			return accessClass;
		}

		/**
		 * Make a MethodHandle to the Reflect method.  If the method is non-static, the receiver argument
		 * is treated as the intial argument in the MethodType.
		 * <p>
		 * If m is a virtual method, normal virtual dispatch is used on each invocation.
		 * <p>
		 * If the <code>accessible</code> flag is not set on the Reflect method, then access checking
		 * is performed using the lookup class.
		 *
		 * @param method - the reflect method
		 * @return A MethodHandle able to invoke the reflect method
		 * @throws IllegalAccessException - if access is denied
		 */
		public MethodHandle unreflect(Method method) throws IllegalAccessException{
			int methodModifiers = method.getModifiers();
			Class<?> declaringClass = method.getDeclaringClass();
			Map<CacheKey, WeakReference<MethodHandle>> cache;

			/* Determine which cache (static or virtual to use) */
			if (Modifier.isStatic(methodModifiers)) {
				cache = HandleCache.getStaticCache(declaringClass);
			} else {
				cache = HandleCache.getVirtualCache(declaringClass);
			}

			String methodName = method.getName();
			MethodType type = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
			MethodHandle handle = HandleCache.getMethodFromPerClassCache(cache, methodName, type);
			if (handle == null) {
				if (Modifier.isStatic(methodModifiers)) {
					handle = new DirectHandle(method, MethodHandle.KIND_STATIC, null);
				} else if (declaringClass.isInterface()) {
					if ((Modifier.isPrivate(methodModifiers)) && !Modifier.isStatic(methodModifiers)) {
						return throwAbstractMethodErrorForUnreflectPrivateInterfaceMethod(method, type);
					} else {
						handle = new InterfaceHandle(method);
					}
					/* Note, it is not required to call adaptInterfaceLookupsOfObjectMethodsIfRequired() here
					 * as Reflection will not return a j.l.r.Method for a public Object method with an interface
					 * as the declaringClass *unless* that the method is defined in the interface or superinterface.
					 */
				} else {

					/* Static check is performed by native code */
					if (!Modifier.isPrivate(methodModifiers) && !Modifier.isFinal(methodModifiers)) {
						handle = new VirtualHandle(method);
					} else {
						handle = new DirectHandle(method, MethodHandle.KIND_SPECIAL, declaringClass, true);
					}
				}
				handle = convertToVarargsIfRequired(handle);
				HandleCache.putMethodInPerClassCache(cache, methodName, type, handle);
			}

			if (!method.isAccessible()) {
				handle = restrictReceiver(handle);
				checkAccess(handle);
			}

			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle);

			return handle;
		}

		private MethodHandle throwAbstractMethodErrorForUnreflectPrivateInterfaceMethod(Method method, MethodType type) throws IllegalAccessException {
			Class<?> declaringClass = method.getDeclaringClass();
			int modifiers = method.getModifiers();

			if (!declaringClass.isInterface() || !Modifier.isPrivate(modifiers)) {
				throw new InternalError("Only applicable to private interface methods"); //$NON-NLS-1$
			}
			/* Inlined version of access checking.  This needs to cover the NO_ACCESS and private_access case
			 * as this method should only be called on a private interface method.
			 */
			if (!method.isAccessible()) {
				if (accessMode == NO_ACCESS) {
					throw new IllegalAccessException(this.toString());
				}
				if (declaringClass != accessClass || !Modifier.isPrivate(accessMode)) {
					// K0587 = '{0}' no access to: '{1}'
					String message = com.ibm.oti.util.Msg.getString("K0587", this.toString(), declaringClass + "." + method.getName() + ":" + MethodHandle.KIND_INTERFACE + "/invokeinterface");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					throw new IllegalAccessException(message);
				}
			}

			/* insert the 'receiver' into the MethodType just as is done by InterfaceHandle */
			type = type.insertParameterTypes(0, declaringClass);

			MethodHandle thrower = throwException(type.returnType(), AbstractMethodError.class);
			MethodHandle constructor;
			try {
				constructor = IMPL_LOOKUP.findConstructor(AbstractMethodError.class, MethodType.methodType(void.class));
			} catch (IllegalAccessException | NoSuchMethodException e) {
				throw new InternalError("Unable to find AbstractMethodError.<init>()");  //$NON-NLS-1$
			}
			MethodHandle handle = foldArguments(thrower, constructor);
			handle = dropArguments(handle, 0, type.parameterList());

			if (isVarargs(modifiers)) {
				Class<?> lastClass = handle.type.lastParameterType();
				handle = handle.asVarargsCollector(lastClass);
			}

			return handle;
		}

		/**
		 * Return a MethodHandle for the reflect constructor. The MethodType has a return type
		 * of the declared class, and the arguments of the constructor.  The MehtodHnadle
		 * creates a new object as through by newInstance.
		 * <p>
		 * If the <code>accessible</code> flag is not set, then access checking
		 * is performed using the lookup class.
		 *
		 * @param method - the Reflect constructor
		 * @return a Methodhandle that creates new instances using the requested constructor
		 * @throws IllegalAccessException - if access is denied
		 */
		public MethodHandle unreflectConstructor(Constructor<?> method) throws IllegalAccessException {
			String methodName = method.getName();
			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getConstructorCache(method.getDeclaringClass());
			MethodType type = MethodType.methodType(void.class, method.getParameterTypes());
			MethodHandle handle = HandleCache.getMethodFromPerClassCache(cache, methodName, type);
			if (handle == null) {
				handle = new ConstructorHandle(method);
				handle = convertToVarargsIfRequired(handle);
				HandleCache.putMethodInPerClassCache(cache, methodName, type, handle);
			}

			if (!method.isAccessible()) {
				checkAccess(handle);
			}

			return handle;
		}

		/**
		 * Return a MethodHandle that will create an object of the required class and initialize it using
		 * the constructor method with signature <i>type</i>.  The MethodHandle will have a MethodType
		 * with the same parameters as the constructor method, but will have a return type of the
		 * <i>declaringClass</i> instead of void.
		 *
		 * @param declaringClass - Class that declares the constructor
		 * @param type - the MethodType of the constructor.  Return type must be void.
		 * @return a MethodHandle able to construct and intialize the required class
		 * @throws IllegalAccessException if access is denied
		 * @throws NoSuchMethodException if the method doesn't exist
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		public MethodHandle findConstructor(Class<?> declaringClass, MethodType type) throws IllegalAccessException, NoSuchMethodException {
			nullCheck(declaringClass, type);
			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getConstructorCache(declaringClass);
			MethodHandle handle = HandleCache.getMethodFromPerClassCache(cache, "<init>", type); //$NON-NLS-1$
			if (handle == null) {
				if (VM.PACKED_SUPPORT_ENABLED) {
					if (PackedObject.isPackedArray(declaringClass)) {
						// PACKED9 = This operation is not supported for @Packed array classes.
						throw new UnsupportedOperationException(Msg.getString("PACKED9")); //$NON-NLS-1$
					}
				}
				handle = new ConstructorHandle(declaringClass, type);
				if (type.returnType() != void.class) {
					throw new NoSuchMethodException();
				}
				handle = HandleCache.putMethodInPerClassCache(cache, "<init>", type, convertToVarargsIfRequired(handle)); //$NON-NLS-1$
			}
			checkAccess(handle);
			checkSecurity(handle.defc, declaringClass, handle.rawModifiers);
			return handle;
		}

		/**
		 * Return a MethodHandle for the Reflect method, that will directly call the requested method
		 * as through from the class <code>specialToken</code>.  The MethodType of the method handle
		 * will be that of the method with the receiver argument prepended.
		 *
		 * @param method - a Reflect method
		 * @param specialToken - the class the call is from
		 * @return a MethodHandle that directly dispatches the requested method
		 * @throws IllegalAccessException - if access is denied
		 */
		public MethodHandle unreflectSpecial(Method method, Class<?> specialToken) throws IllegalAccessException {
			nullCheck(method, specialToken);
			checkSpecialAccess(specialToken);	/* Must happen before method resolution */
			String methodName = method.getName();
			Map<CacheKey, WeakReference<MethodHandle>> cache = HandleCache.getSpecialCache(method.getDeclaringClass());
			MethodType type = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
			MethodHandle handle = HandleCache.getMethodWithSpecialCallerFromPerClassCache(cache, methodName, type, specialToken);
			if (handle == null) {
				if (Modifier.isStatic(method.getModifiers())) {
					throw new IllegalAccessException();
				}
				/* Does not require 'restrictReceiver()'as DirectHandle(KIND_SPECIAL) sets receiver type to specialToken */
				handle = convertToVarargsIfRequired(new DirectHandle(method, MethodHandle.KIND_SPECIAL, specialToken));
				HandleCache.putMethodWithSpecialCallerInPerClassCache(cache, methodName, type, handle, specialToken);
			}

			if (!method.isAccessible()) {
				checkAccess(handle);
			}

			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle);

			return handle;
		}

		/**
		 * Create a MethodHandle that returns the value of the Reflect field.  There are two cases:
		 * <ol>
		 * <li>a static field - which has the MethodType with only a return type of the field</li>
		 * <li>an instance field - which has the MethodType with a return type of the field and a
		 * single argument of the object that contains the field</li>
		 * </ol>
		 * <p>
		 * If the <code>accessible</code> flag is not set, then access checking
		 * is performed using the lookup class.
		 *
		 * @param field - a Reflect field
		 * @return a MethodHandle able to return the field value
		 * @throws IllegalAccessException - if access is denied
		 */
		public MethodHandle unreflectGetter(Field field) throws IllegalAccessException {
			int modifiers = field.getModifiers();
			String fieldName = field.getName();
			Class<?> declaringClass = field.getDeclaringClass();
			Class<?> fieldType = field.getType();
			Map<CacheKey, WeakReference<MethodHandle>> cache;
			if (Modifier.isStatic(modifiers)) {
				cache = HandleCache.getStaticFieldGetterCache(declaringClass);
			} else {
				cache = HandleCache.getFieldGetterCache(declaringClass);
			}

			MethodHandle handle = HandleCache.getFieldFromPerClassCache(cache, fieldName, fieldType);
			if (handle == null) {
				if (Modifier.isStatic(modifiers)) {
					handle = new StaticFieldGetterHandle(field);
				} else {
					handle = new FieldGetterHandle(field);
				}
				HandleCache.putFieldInPerClassCache(cache, fieldName, fieldType, handle);
			}
			if (!field.isAccessible()) {
				checkAccess(handle);
			}
			return handle;
		}

		/**
		 * Create a MethodHandle that sets the value of the Reflect field.  All MethodHandles created
		 * here have a return type of void.  For the arguments, there are two cases:
		 * <ol>
		 * <li>a static field - which takes a single argument the same as the field</li>
		 * <li>an instance field - which takes two arguments, the object that contains the field, and the type of the field</li>
		 * </ol>
		 * <p>
		 * If the <code>accessible</code> flag is not set, then access checking
		 * is performed using the lookup class.
		 *
		 * @param field - a Reflect field
		 * @return a MethodHandle able to set the field value
		 * @throws IllegalAccessException - if access is denied
		 */
		public MethodHandle unreflectSetter(Field field) throws IllegalAccessException {
			MethodHandle handle;
			int modifiers = field.getModifiers();
			Map<CacheKey, WeakReference<MethodHandle>> cache;
			Class<?> declaringClass = field.getDeclaringClass();
			Class<?> fieldType = field.getType();
			String fieldName = field.getName();

			if (Modifier.isFinal(modifiers)) {
				// K05cf = illegal setter on final field
				throw new IllegalAccessException(Msg.getString("K05cf")); //$NON-NLS-1$
			}

			if (Modifier.isStatic(modifiers)) {
				cache = HandleCache.getStaticFieldSetterCache(declaringClass);
			} else {
				cache = HandleCache.getFieldSetterCache(declaringClass);
			}

			handle = HandleCache.getFieldFromPerClassCache(cache, fieldName, fieldType);
			if (handle == null) {
				if (Modifier.isStatic(modifiers)) {
					handle = new StaticFieldSetterHandle(field);
				} else {
					handle = new FieldSetterHandle(field);
				}

				HandleCache.putFieldInPerClassCache(cache, fieldName, fieldType, handle);
			}

			if (!field.isAccessible()) {
				checkAccess(handle);
			}
			return handle;
		}

		/**
		 * Cracks a MethodHandle, which allows access to its symbolic parts.
		 * The MethodHandle must have been created by this Lookup object or one that is able to recreate the MethodHandle.
		 * If the Lookup object is not able to recreate the MethodHandle, the cracking may fail.
		 *
		 * @param target The MethodHandle to be cracked
		 * @return a MethodHandleInfo which provides access to the target's symbolic parts
		 * @throws IllegalArgumentException if the target is not a direct handle, or if the access check fails
		 * @throws NullPointerException if the target is null
		 * @throws SecurityException if a SecurityManager denies access
		 */
		public MethodHandleInfo revealDirect(MethodHandle target) throws IllegalArgumentException, NullPointerException, SecurityException {
			if (!target.canRevealDirect()) { // Implicit null check
				target = SecurityFrameInjector.penetrateSecurityFrame(target, this);
				if ((target == null) || !target.canRevealDirect()) {
					// K0584 = The target is not a direct handle
					throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0584")); //$NON-NLS-1$
				}
			}
			try {
				checkAccess(target);
				checkSecurity(target.defc, target.definingClass, target.rawModifiers);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
			if (target instanceof VarargsCollectorHandle) {
				target = ((VarargsCollectorHandle)target).next;
			}
			return new MethodHandleInfoImpl(target);
		}

		@Override
		public String toString() {
			String toString = accessClass.getName();
			switch(accessMode) {
			case NO_ACCESS:
				toString += "/noaccess"; //$NON-NLS-1$
				break;
			case PUBLIC:
				toString += "/public"; //$NON-NLS-1$
				break;
			case PUBLIC | PACKAGE:
				toString += "/package"; //$NON-NLS-1$
				break;
			case PUBLIC | PACKAGE | PRIVATE:
				toString += "/private"; //$NON-NLS-1$
				break;
			}
			return toString;
		}

		/*
		 * Determine if this lookup has been weakened.
		 * A lookup is weakened when it doesn't have private access.
		 *
		 * return true if the lookup has been weakened.
		 */
		boolean isWeakenedLookup() {
			return PRIVATE != (accessMode & PRIVATE);
		}

		/*
		 * If there is a security manager, this performs 1 to 3 different security checks:
		 *
		 * 1) secmgr.checkPackageAccess(refcPkg), if classloader of access class is not same or ancestor of classloader of reference class
		 * 2) secmgr.checkPermission(new RuntimePermission("accessDeclaredMembers")), if retrieved member is not public
		 * 3) secmgr.checkPackageAccess(defcPkg), if retrieved member is not public, and if defining class and reference class are in different classloaders,
		 *    and if classloader of access class is not same or ancestor of classloader of defining class
		 */
		void checkSecurity(Class<?> definingClass, Class<?> referenceClass, int modifiers) throws IllegalAccessException {
			if (accessMode == INTERNAL_PRIVILEGED) {
				// Full access for use by MH implementation.
				return;
			}
			if (null == definingClass) {
				throw new IllegalAccessException();
			}

			if (performSecurityCheck) {
				SecurityManager secmgr = System.getSecurityManager();
				if (secmgr != null) {
					/* Use leaf-class in the case of arrays for security check */
					while (definingClass.isArray()) {
						definingClass = definingClass.getComponentType();
					}
					while (referenceClass.isArray()) {
						referenceClass = referenceClass.getComponentType();
					}

					VMLangAccess vma = getVMLangAccess();

					ClassLoader referenceClassLoader = referenceClass.getClassLoader();
					/* first check */
					if (isWeakenedLookup() || !doesClassLoaderDescendFrom(referenceClassLoader, accessClass.getClassLoader())) {
						String packageName = vma.getPackageName(referenceClass);
						secmgr.checkPackageAccess(packageName);
					}

					/* first check */
					if (!Modifier.isPublic(modifiers)) {
						if (vma.getClassloader(definingClass) != vma.getClassloader(accessClass)) {
							secmgr.checkPermission(new RuntimePermission("accessDeclaredMembers")); //$NON-NLS-1$
						}

						/* third check */
						if (definingClass.getClassLoader() != referenceClassLoader) {
							if (!doesClassLoaderDescendFrom(definingClass.getClassLoader(), accessClass.getClassLoader())) {
								secmgr.checkPackageAccess(vma.getPackageName(definingClass));
							}
						}
					}
				}
			}
		}

		/*
		 * Return a MethodHandle that will throw the passed in Exception object with the passed in message.
		 * The MethodType is of the returned MethodHandle is set to the passed in type.
		 *
		 * @param exceptionClass The type of exception to throw
		 * @param type The MethodType of the generated MethodHandle
		 * @param msg The message to be used in the exception
		 * @return A MethodHandle with the requested MethodType that throws the passed in Exception
		 * @throws IllegalAccessException If MethodHandle creation fails
		 * @throws NoSuchMethodException If MethodHandle creation fails
		 */
		private static MethodHandle throwExceptionWithMethodTypeAndMessage(Class<? extends Throwable> exceptionClass, MethodType type, String msg)  throws IllegalAccessException, NoSuchMethodException {
			MethodHandle thrower = throwException(type.returnType(), exceptionClass);
			MethodHandle constructor = IMPL_LOOKUP.findConstructor(exceptionClass, MethodType.methodType(void.class, String.class));
			MethodHandle result = foldArguments(thrower, constructor.bindTo(msg));

			/* Change result MethodType to the requested type */
			result = result.asType(MethodType.methodType(type.returnType));
			result = dropArguments(result, 0, type.parameterList());
			return result;
		}
	}

	/**
	 * Return a MethodHandles.Lookup object for the caller.
	 *
	 * @return a MethodHandles.Lookup object
	 */
	@sun.reflect.CallerSensitive
	public static MethodHandles.Lookup lookup() {
		Class<?> c = getStackClass(1);
		return new Lookup(c);
	}

	/**
	 * Return a MethodHandles.Lookup object that is only able to access <code>public</code> members.
	 *
	 * @return a MethodHandles.Lookup object
	 */
	public static MethodHandles.Lookup publicLookup() {
		return Lookup.publicLookup;
	}

	/**
	 * Gets the underlying Member of the provided <code>target</code> MethodHandle. This is done through an unchecked crack of the MethodHandle.
	 * Calling this method is equivalent to obtaining a lookup object capable of cracking the <code>target</code> MethodHandle, calling
	 * <code>Lookup.revealDirect</code> on the <code>target</code> MethodHandle and then calling <code>MethodHandleInfo.reflectAs</code>.
	 *
	 * If a SecurityManager is present, this method requires <code>ReflectPermission("suppressAccessChecks")</code>.
	 *
	 * @param expected the expected type of the underlying member
	 * @param target the direct MethodHandle to be cracked
	 * @return the underlying member of the <code>target</code> MethodHandle
	 * @throws SecurityException if the caller does not have the required permission (<code>ReflectPermission("suppressAccessChecks")</code>)
	 * @throws NullPointerException if either of the arguments are <code>null</code>
	 * @throws IllegalArgumentException if the <code>target</code> MethodHandle is not a direct MethodHandle
	 * @throws ClassCastException if the underlying member is not of the <code>expected</code> type
	 */
	public static <T extends Member> T reflectAs(Class<T> expected, MethodHandle target) throws SecurityException, NullPointerException, IllegalArgumentException, ClassCastException {
		if ((null == expected) || (null == target)) {
			throw new NullPointerException();
		}
		SecurityManager secmgr = System.getSecurityManager();
		if (null != secmgr) {
			secmgr.checkPermission(new ReflectPermission("suppressAccessChecks")); //$NON-NLS-1$
		}
		MethodHandleInfo mhi = Lookup.IMPL_LOOKUP.revealDirect(target);
		T result = mhi.reflectAs(expected, Lookup.IMPL_LOOKUP);
		return result;
	}

	/**
	 * Return a MethodHandle that is the equivalent of calling
	 * MethodHandles.lookup().findVirtual(MethodHandle.class, "invokeExact", type).
	 * <p>
	 * The MethodHandle has a method type that is the same as type except that an additional
	 * argument of MethodHandle will be added as the first parameter.
	 * <p>
	 * This method is not subject to the same security checks as a findVirtual call.
	 *
	 * @param type - the type of the invokeExact call to lookup
	 * @return a MethodHandle equivalent to calling invokeExact on the first argument.
	 * @throws IllegalArgumentException if the resulting MethodHandle would take too many parameters.
	 */
	public static MethodHandle exactInvoker(MethodType type) throws IllegalArgumentException {
		return type.getInvokeExactHandle();
	}

	/**
	 * Return a MethodHandle that is the equivalent of calling
	 * MethodHandles.lookup().findVirtual(MethodHandle.class, "invoke", type).
	 * <p>
	 * The MethodHandle has a method type that is the same as type except that an additional
	 * argument of MethodHandle will be added as the first parameter.
	 * <p>
	 * This method is not subject to the same security checks as a findVirtual call.
	 *
	 * @param type - the type of the invoke call to lookup
	 * @return a MethodHandle equivalent to calling invoke on the first argument.
	 * @throws IllegalArgumentException if the resulting MethodHandle would take too many parameters.
	 */
	public static MethodHandle invoker(MethodType type) throws IllegalArgumentException {
		type.getClass(); // implicit nullcheck
		return new InvokeGenericHandle(type);
	}

	/**
	 * Return a MethodHandle that is able to invoke a MethodHandle of <i>type</i> as though by
	 * invoke after spreading the final Object[] parameter.
	 * <p>
	 * When the <code>MethodHandle</code> is invoked, the argument array must contain exactly <i>spreadCount</i> arguments
	 * to be passed to the original <code>MethodHandle</code>.  The array may be null in the case when <i>spreadCount</i> is zero.
	 * Incorrect argument array size will cause the method to throw an <code>IllegalArgumentException</code> instead of invoking the target.
	 * </p>
	 * @param type - the type of the invoke method to look up
	 * @param fixedArgCount - the number of fixed arguments in the methodtype
	 * @return a MethodHandle that invokes its first argument after spreading the Object array
	 * @throws IllegalArgumentException if the fixedArgCount is less than 0 or greater than type.ParameterCount(), or if the resulting MethodHandle would take too many parameters.
	 * @throws NullPointerException if the type is null
	 */
	public static MethodHandle spreadInvoker(MethodType type, int fixedArgCount) throws IllegalArgumentException, NullPointerException {
		int typeParameterCount = type.parameterCount();
		if ((fixedArgCount < 0) || (fixedArgCount > typeParameterCount)) {
			// K039c = Invalid parameters
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K039c")); //$NON-NLS-1$
		}
		MethodHandle invoker = invoker(type);
		int spreadArgCount = typeParameterCount - fixedArgCount;
		return invoker.asSpreader(Object[].class, spreadArgCount);
	}

	/**
	 * Produce a MethodHandle that implements an if-else block.
	 *
	 * This MethodHandle is composed from three handles:
	 * <ul>
	 * <li>guard - a boolean returning handle that takes a subset of the arguments passed to the true and false targets</li>
	 * <li>trueTarget - the handle to call if the guard returns true</li>
	 * <li>falseTarget - the handle to call if the guard returns false</li>
	 * </ul>
	 *
	 * @param guard - method handle returning boolean to determine which target to call
	 * @param trueTarget - target method handle to call if guard is true
	 * @param falseTarget - target method handle to call if guard is false
	 * @return A MethodHandle that implements an if-else block.
	 *
	 * @throws NullPointerException - if any of the three method handles are null
	 * @throws IllegalArgumentException - if any of the following conditions are true:
	 * 				1) trueTarget and falseTarget have different MethodTypes
	 * 				2) the guard handle doesn't have a boolean return value
	 * 				3) the guard handle doesn't take a subset of the target handle's arguments
	 */
	public static MethodHandle guardWithTest(MethodHandle guard, MethodHandle trueTarget, MethodHandle falseTarget) throws NullPointerException, IllegalArgumentException{
		MethodType guardType = guard.type;
		MethodType trueType = trueTarget.type;
		MethodType falseType = falseTarget.type;
		if (trueType != falseType) {
			throw new IllegalArgumentException();
		}
		int testArgCount = guardType.parameterCount();
		if ((guardType.returnType != boolean.class) || (testArgCount > trueType.parameterCount())) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < testArgCount; i++) {
			if (guardType.arguments[i] != trueType.arguments[i]) {
				throw new IllegalArgumentException();
			}
		}

		MethodHandle result = GuardWithTestHandle.get(guard, trueTarget, falseTarget);
		return result;
	}

	/**
	 * Produce a MethodHandle that implements a try-catch block.
	 *
	 * This adapter acts as though the <i>tryHandle</i> where run inside a try block.  If <i>tryHandle</i>
	 * throws an exception of type <i>throwableClass</i>, the <i>catchHandle</i> is invoked with the
	 * exception instance and the original arguments.
	 * <p>
	 * The catchHandle may take a subset of the original arguments rather than the full set.  Its first
	 * argument will be the exception instance.
	 * <p>
	 * Both the catchHandle and the tryHandle must have the same return type.
	 *
	 * @param tryHandle - the method handle to wrap with the try block
	 * @param throwableClass - the class of exception to be caught and handled by catchHandle
	 * @param catchHandle - the method handle to call if an exception of type throwableClass is thrown by tryHandle
	 * @return a method handle that will call catchHandle if tryHandle throws an exception of type throwableClass
	 *
	 * @throws NullPointerException - if any of the parameters are null
	 * @throws IllegalArgumentException - if tryHandle and catchHandle have different return types,
	 * or the catchHandle doesn't take a throwableClass as its first argument,
	 * of if catchHandle arguments[1-N] differ from tryHandle arguments[0-(N-1)]
	 */
	public static MethodHandle catchException(MethodHandle tryHandle, Class<? extends Throwable> throwableClass, MethodHandle catchHandle) throws NullPointerException, IllegalArgumentException{
		if ((tryHandle == null) || (throwableClass == null) || (catchHandle == null)) {
			throw new NullPointerException();
		}
		MethodType tryType = tryHandle.type;
		MethodType catchType = catchHandle.type;
		if (tryType.returnType != catchType.returnType) {
			throw new IllegalArgumentException();
		}
		if (catchType.parameterType(0) != throwableClass) {
			throw new IllegalArgumentException();
		}
		int catchArgCount =  catchType.parameterCount();
		if ((catchArgCount - 1) > tryType.parameterCount()) {
			throw new IllegalArgumentException();
		}
		Class<?>[] tryParams = tryType.arguments;
		Class<?>[] catchParams = catchType.arguments;
		for (int i = 1; i < catchArgCount; i++) {
			if (catchParams[i] != tryParams[i - 1]) {
				throw new IllegalArgumentException();
			}
		}

		MethodHandle result = buildTransformHandle(new CatchHelper(tryHandle, catchHandle, throwableClass), tryType);
		if (true) {
			MethodHandle thunkable = CatchHandle.get(tryHandle, throwableClass, catchHandle, result);
			assert(thunkable.type() == result.type());
			result = thunkable;
		}
		return result;
	}

	/**
	 * Produce a MethodHandle that acts as an identity function.  It will accept a single
	 * argument and return it.
	 *
	 * @param classType - the type to use for the return and parameter types
	 * @return an identity MethodHandle that returns its argument
	 * @throws NullPointerException - if the classType is null
	 * @throws IllegalArgumentException - if the the classType is void.
	 */
	public static MethodHandle identity(Class<?> classType) throws NullPointerException, IllegalArgumentException {
		if (classType == void.class) {
			throw new IllegalArgumentException();
		}
		try {
			MethodType methodType = MethodType.methodType(classType, classType);
			if (classType.isPrimitive()){
				return Lookup.internalPrivilegedLookup.findStatic(MethodHandles.class, "identity", methodType); //$NON-NLS-1$
			}

			MethodHandle handle = Lookup.internalPrivilegedLookup.findStatic(MethodHandles.class, "identity", MethodType.methodType(Object.class, Object.class)); //$NON-NLS-1$
			return handle.cloneWithNewType(methodType);
		} catch(IllegalAccessException | NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	@SuppressWarnings("unused")
	private static boolean identity(boolean x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static byte identity(byte x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static short identity(short x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static char identity(char x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static int identity(int x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static float identity(float x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static double identity(double x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static long identity(long x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static Object identity(Object x) {
		return x;
	}

	/**
	 * Create a MethodHandle that returns the <i>constantValue</i> on each invocation.
	 * <p>
	 * Conversions of the <i>constantValue</i> to the <i>returnType</i> occur if possible, otherwise
	 * a ClassCastException is thrown.  For primitive <i>returnType</i>, widening primitive conversions are
	 * attempted.  Otherwise, reference conversions are attempted.
	 *
	 * @param returnType - the return type of the MethodHandle.
	 * @param constantValue - the value to return from the MethodHandle when invoked
	 * @return a MethodHandle that always returns the <i>constantValue</i>
	 * @throws NullPointerException - if the returnType is null
	 * @throws ClassCastException - if the constantValue cannot be converted to returnType
	 * @throws IllegalArgumentException - if the returnType is void
	 */
	public static MethodHandle constant(Class<?> returnType, Object constantValue) throws NullPointerException, ClassCastException, IllegalArgumentException {
		returnType.getClass();	// implicit null check
		if (returnType == void.class) {
			throw new IllegalArgumentException();
		}
		if (returnType.isPrimitive()) {
			if (constantValue == null) {
				throw new IllegalArgumentException();
			}
			Class<?> unwrapped = MethodType.unwrapPrimitive(constantValue.getClass());
			if ((returnType != unwrapped) && !FilterHelpers.checkIfWideningPrimitiveConversion(unwrapped, returnType)) {
				throw new ClassCastException();
			}
		} else {
			returnType.cast(constantValue);
		}
		return ConstantHandle.get(returnType, constantValue);
	}

	/**
	 * Return a MethodHandle able to read from the array.  The MethodHandle's return type will be the same as
	 * the elements of the array.  The MethodHandle will also accept two arguments - the first being the array, typed correctly,
	 * and the second will be the the <code>int</code> index into the array.
	 *
	 * @param arrayType - the type of the array
	 * @return a MethodHandle able to return values from the array
	 * @throws IllegalArgumentException - if arrayType is not actually an array
	 */
	public static MethodHandle arrayElementGetter(Class<?> arrayType) throws IllegalArgumentException {
		if (!arrayType.isArray()) {
			throw new IllegalArgumentException();
		}

		try {
			Class<?> componentType = arrayType.getComponentType();
			if (componentType.isPrimitive()) {
				// Directly lookup the appropriate helper method
				String name = componentType.getCanonicalName();
				MethodType type = MethodType.methodType(componentType, arrayType, int.class);
				return Lookup.internalPrivilegedLookup.findStatic(MethodHandles.class, name + "ArrayGetter", type); //$NON-NLS-1$
			}

			// Lookup the "Object[]" case and use some asTypes() to get the right MT and return type.
			MethodType type = MethodType.methodType(Object.class, Object[].class, int.class);
			MethodHandle mh = Lookup.internalPrivilegedLookup.findStatic(MethodHandles.class, "objectArrayGetter", type); //$NON-NLS-1$
			MethodType realType = MethodType.methodType(componentType, arrayType, int.class);
			return mh.cloneWithNewType(realType);
		} catch(IllegalAccessException | NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	/**
	 * Return a MethodHandle able to write to the array.  The MethodHandle will have a void return type and take three
	 * arguments: the first being the array, typed correctly, the second will be the the <code>int</code> index into the array,
	 * and the third will be the item to write into the array
	 *
	 * @param arrayType - the type of the array
	 * @return a MehtodHandle able to write into the array
	 * @throws IllegalArgumentException - if arrayType is not actually an array
	 */
	public static MethodHandle arrayElementSetter(Class<?> arrayType) throws IllegalArgumentException {
		if (!arrayType.isArray()) {
			throw new IllegalArgumentException();
		}

		try {
			Class<?> componentType = arrayType.getComponentType();
			if (componentType.isPrimitive()) {
				// Directly lookup the appropriate helper method
				String name = componentType.getCanonicalName();
				MethodType type = MethodType.methodType(void.class, arrayType, int.class, componentType);
				return Lookup.internalPrivilegedLookup.findStatic(MethodHandles.class, name + "ArraySetter", type); //$NON-NLS-1$
			}

			// Lookup the "Object[]" case and use some asTypes() to get the right MT and return type.
			MethodType type = MethodType.methodType(void.class, Object[].class, int.class, Object.class);
			MethodHandle mh = Lookup.internalPrivilegedLookup.findStatic(MethodHandles.class, "objectArraySetter", type); //$NON-NLS-1$
			MethodType realType = MethodType.methodType(void.class, arrayType, int.class, componentType);
			return mh.cloneWithNewType(realType);
		} catch(IllegalAccessException | NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	/**
	 * Return a MethodHandle that will throw the passed in Exception object.  The return type is largely
	 * irrelevant as the method never completes normally.  Any return type that is convenient can be
	 * used.
	 *
	 * @param returnType - The return type for the method
	 * @param exception - the type of Throwable to accept as an argument
	 * @return a MethodHandle that throws the passed in exception object
	 */
	public static MethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exception) {
		MethodType realType = MethodType.methodType(returnType, exception);
		MethodHandle handle;

		try {
			if (returnType.isPrimitive() || returnType.equals(void.class)) {
				// Directly lookup the appropriate helper method
				MethodType type = MethodType.methodType(returnType, Throwable.class);
				String name = returnType.getCanonicalName();
				handle = Lookup.internalPrivilegedLookup.findStatic(MethodHandles.class, name + "ExceptionThrower", type); //$NON-NLS-1$
			} else {
				MethodType type = MethodType.methodType(Object.class, Throwable.class);
				handle = Lookup.internalPrivilegedLookup.findStatic(MethodHandles.class, "objectExceptionThrower", type); //$NON-NLS-1$
			}
			return handle.cloneWithNewType(realType);
		} catch(IllegalAccessException | NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	/**
	 * Return a MethodHandle that will adapt the return value of <i>handle</i> by running the <i>filter</i>
	 * on it and returning the result of the filter.
	 * <p>
	 * If <i>handle</i> has a void return, <i>filter</i> must not take any parameters.
	 *
	 * @param handle - the MethodHandle that will have its return value adapted
	 * @param filter - the MethodHandle that will do the return adaption.
	 * @return a MethodHandle that will run the filter handle on the result of handle.
	 * @throws NullPointerException - if handle or filter is null
	 * @throws IllegalArgumentException - if the return type of <i>handle</i> differs from the type of the only argument to <i>filter</i>
	 */
	public static MethodHandle filterReturnValue(MethodHandle handle, MethodHandle filter) throws NullPointerException, IllegalArgumentException {
		MethodType filterType = filter.type;
		int filterArgCount = filterType.parameterCount();
		Class<?> handleReturnType = handle.type.returnType;

		if ((handleReturnType == void.class) && (filterArgCount == 0)) {
			// filter handle must not take any parameters as handle doesn't return anything
			return new FilterReturnHandle(handle, filter);
		}
		if ((filterArgCount == 1) && (filterType.parameterType(0) == handle.type.returnType)) {
			// filter handle must accept single parameter of handle's returnType
			return new FilterReturnHandle(handle, filter);
		}
		throw new IllegalArgumentException();
	}

	@SuppressWarnings("unused")
	private static void voidExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static int intExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static char charExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static byte byteExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static boolean booleanExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static short shortExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static long longExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static double doubleExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static float floatExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static Object objectExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static  int intArrayGetter(int[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static char charArrayGetter(char[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static short shortArrayGetter(short[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static byte byteArrayGetter(byte[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static long longArrayGetter(long[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static double doubleArrayGetter(double[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static float floatArrayGetter(float[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static boolean booleanArrayGetter(boolean[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static Object objectArrayGetter(Object[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static void intArraySetter(int[] array, int index, int value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void charArraySetter(char[] array, int index, char value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void shortArraySetter(short[] array, int index, short value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void byteArraySetter(byte[] array, int index, byte value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void longArraySetter(long[] array, int index, long value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void doubleArraySetter(double[] array, int index, double value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void floatArraySetter(float[] array, int index, float value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void booleanArraySetter(boolean[] array, int index, boolean value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void objectArraySetter(Object[] array, int index, Object value) {
		array[index] = value;
	}

	/**
	 * Produce a MethodHandle that adapts its arguments using the filter methodhandles before calling the underlying handle.
	 * <p>
	 * The type of the adapter is the type of the original handle with the filter argument types replacing their corresponding
	 * arguments.  Each of the adapters must return the correct type for their corresponding argument.
	 * <p>
	 * If the filters array is empty or contains only null filters, the original handle will be returned.
	 *
	 * @param handle - the underlying methodhandle to call with the filtered arguments
	 * @param startPosition - the position to start applying the filters at
	 * @param filters - the array of adapter handles to apply to the arguments
	 * @return a MethodHandle that modifies the arguments by applying the filters before calling the underlying handle
	 * @throws NullPointerException - if handle or filters is null
	 * @throws IllegalArgumentException - if one of the filters is not applicable to the corresponding handle argument
	 * 				or there are more filters then arguments when starting at startPosition
	 * 				or startPosition is invalid
	 * 				or if the resulting MethodHandle would take too many parameters
	 *
	 */
	public static MethodHandle filterArguments(MethodHandle handle, int startPosition, MethodHandle... filters) throws NullPointerException, IllegalArgumentException {
		filters.getClass();	// implicit null check
		MethodType handleType = handle.type;	// implicit null check
		if ((startPosition < 0) || ((startPosition + filters.length) > handleType.parameterCount())) {
			throw new IllegalArgumentException();
		}
		if (filters.length == 0) {
			return handle;
		}
		// clone the filter array so it can't be modified after the filters have been validated
		filters = filters.clone();

		// clone the parameter array
		Class<?>[] newArgTypes = handleType.parameterArray();
		boolean containsNonNullFilters = false;
		for (int i = 0; i < filters.length; i++) {
			MethodHandle filter = filters[i];
			if (filter != null) {
				containsNonNullFilters = true;
				MethodType filterType = filter.type;
				if (newArgTypes[startPosition + i] != filterType.returnType) {
					throw new IllegalArgumentException();
				}
				if (filterType.parameterCount() != 1) {
					throw new IllegalArgumentException();
				}
				newArgTypes[startPosition + i] = filterType.arguments[0];
			}
		}
		if (!containsNonNullFilters) {
			return handle;
		}

		// Remove any leading null filters
		for (int i = 0; i < filters.length; i++) {
			MethodHandle filter = filters[i];
			if (filter != null) {
				if (i != 0) {
					filters = Arrays.copyOfRange(filters, i, filters.length);
				}
				break;
			}
			startPosition += 1;
		}

		MethodType newType = MethodType.methodType(handleType.returnType, newArgTypes);
		MethodHandle result = FilterArgumentsHandle.get(handle, startPosition, filters, newType);
		return result;
	}

	/**
	 * Produce a MethodHandle that preprocesses some of the arguments by calling the preprocessor handle.
	 *
	 * If the preprocessor handle has a return type, it must be the same as the first argument type of the <i>handle</i>.
	 * If the preprocessor returns void, it does not contribute the first argument to the <i>handle</i>.
	 * In all cases, the preprocessor handle accepts a subset of the arguments for the handle.
	 *
	 * @param handle - the handle to call after preprocessing
	 * @param preprocessor - a methodhandle that preprocesses some of the incoming arguments
	 * @return a MethodHandle that preprocesses some of the arguments to the handle before calling the next handle, possibly with an additional first argument
	 * @throws NullPointerException - if any of the arguments are null
	 * @throws IllegalArgumentException - if the preprocessor's return type is not void and it differs from the first argument type of the handle,
	 * 			or if the arguments taken by the preprocessor isn't a subset of the arguments to the handle
	 */
	public static MethodHandle foldArguments(MethodHandle handle, MethodHandle preprocessor) throws NullPointerException, IllegalArgumentException {
		MethodType handleType = handle.type; // implicit nullcheck
		MethodType preprocessorType = preprocessor.type; // implicit nullcheck
		Class<?> preprocessorReturnClass = preprocessorType.returnType;

		if (preprocessorReturnClass == void.class) {
			// special case: a preprocessor handle that returns void doesn't provide an argument to the underlying handle
			if (handleType.parameterCount() < preprocessorType.parameterCount()) {
				throw new IllegalArgumentException();
			}
			if (preprocessorType.parameterCount() > 0) {
				for (int i = 0; i < preprocessorType.parameterCount(); i++) {
					if (preprocessorType.arguments[i]  != handleType.arguments[i]) {
						throw new IllegalArgumentException();
					}
				}
			}

			MethodHandle result = FoldHandle.get(handle, preprocessor, handleType);
			return result;
		}

		if (handleType.parameterCount() <= preprocessorType.parameterCount()) {
			throw new IllegalArgumentException();
		}
		if (preprocessorReturnClass != handleType.arguments[0]) {
			throw new IllegalArgumentException();
		}
		if (preprocessorType.parameterCount() > 0) {
			for (int i = 0; i < preprocessorType.parameterCount(); i++) {
				if (preprocessorType.arguments[i]  != handleType.arguments[i + 1]) {
					// K05d0 = Can't apply fold of type: {0} to handle of type: {1}
					throw new IllegalArgumentException(Msg.getString("K05d0", preprocessorType.toString(), handleType.toString())); //$NON-NLS-1$
				}
			}
		}

		MethodType newType = handleType.dropFirstParameterType();
		MethodHandle result = FoldHandle.get(handle, preprocessor, newType);
		return result;
	}

	/**
	 * Produce a MethodHandle that will permute the incoming arguments according to the
	 * permute array.  The new handle will have a type of permuteType.
	 * <p>
	 * The permutations can include duplicating or rearranging the arguments.  The permute
	 * array must have the same number of items as there are parameters in the
	 * handle's type.
	 * <p>
	 * Each argument type must exactly match - no conversions are applied.
	 *
	 * @param handle - the original handle to call after permuting the arguments
	 * @param permuteType - the new type of the adapter handle
	 * @param permute - the reordering from the permuteType to the handle type
	 * @return a MethodHandle that rearranges the arguments before calling the original handle
	 * @throws NullPointerException - if any of the arguments are null
	 * @throws IllegalArgumentException - if permute array is not the same length as handle.type().parameterCount() or
	 * 			if handle.type() and permuteType have different return types, or
	 * 			if the permute arguments don't match the handle.type()
	 */
	public static MethodHandle permuteArguments(MethodHandle handle, MethodType permuteType, int... permute) throws NullPointerException, IllegalArgumentException {
		// TODO: If permute is the identity permute, return this
		MethodType handleType = handle.type;	// implicit null check
		Class<?> permuteReturnType = permuteType.returnType; // implicit null check
		if (permute.length != handleType.parameterCount()) { // implicit null check on permute
			throw new IllegalArgumentException();
		}
		if (permuteReturnType != handleType.returnType) {
			throw new IllegalArgumentException();
		}
		permute = permute.clone();	// ensure the permute[] can't be modified during/after validation
		// validate permuted args are an exact match
		validatePermutationArray(permuteType, handleType, permute);
		return handle.permuteArguments(permuteType, permute);
	}

	/**
	 * Produce a MethodHandle that preprocesses some of the arguments by calling the filter handle.
	 *
	 * If the <i>filter</i> handle has a return type, it must be the same as the argument type at pos in the <i>target</i> arguments.
	 * If the <i>filter</i> returns void, it does not contribute an argument to the <i>target</i> arguments at pos.
	 * The <i>filter</i> handle consumes a subset (size equal to the <i>filter's</i> arity) of the returned handle's arguments, starting at pos.
	 *
	 * @param target - the target to call after preprocessing the arguments
	 * @param pos - argument index in handle arguments where the filter will collect its arguments and/or insert its return value as an argument to the target
	 * @param filter - a MethodHandle that preprocesses some of the incoming arguments
	 * @return a MethodHandle that preprocesses some of the arguments to the handle before calling the target with the new arguments
	 * @throws NullPointerException - if either target or filter are null
	 * @throws IllegalArgumentException - if the preprocessor's return type is not void and it differs from the target argument type at pos,
	 * 			if pos is not between 0 and the target's arity (exclusive for non-void filter, inclusive for void filter), or if the generated handle would
	 * 			have too many parameters
	 */
	public static MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) throws NullPointerException, IllegalArgumentException {
		MethodType targetType = target.type; // implicit nullcheck
		MethodType filterType = filter.type; // implicit nullcheck
		Class<?> filterReturnClass = filterType.returnType;

		if (filterReturnClass == void.class) {
			// special case: a filter handle that returns void doesn't provide an argument to the target handle
			if ((pos < 0) || (pos > targetType.argSlots)) {
				// K0580 = Filter argument index (pos) is not between 0 and target arity ("{0}")
				throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0580", targetType.argSlots)); //$NON-NLS-1$
			}
			MethodType resultType = targetType.insertParameterTypes(pos, filterType.arguments);
			MethodHandle result = buildTransformHandle(new VoidCollectHelper(target, pos, filter), resultType);
			return result;
		}

		if ((pos < 0) || (pos >= targetType.argSlots)) {
			// K0580 = Filter argument index (pos) is not between 0 and target arity ("{0}")
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0580", targetType.argSlots)); //$NON-NLS-1$
		}

		if (filterReturnClass != targetType.arguments[pos]) {
			// K0581 = Filter return type does not match target argument at position "{0}"
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0581", pos)); //$NON-NLS-1$
		}
		MethodType resultType = targetType.dropParameterTypes(pos, pos + 1).insertParameterTypes(pos, filterType.arguments);
		MethodHandle result = buildTransformHandle(new CollectHelper(target, pos, filter), resultType);
		return result;
	}

	/*
	 * Helper method to validate that the permute[] specifies a valid permutation from permuteType
	 * to handleType.
	 *
	 * This method throws IllegalArgumentException on failure and returns true on success.  This
	 * disjointness allows it to be used in asserts.
	 */
	private static boolean validatePermutationArray(MethodType permuteType, MethodType handleType, int[] permute) throws IllegalArgumentException {
		Class<?>[] permuteArgs = permuteType.arguments;
		Class<?>[] handleArgs = handleType.arguments;
		for (int i = 0; i < permute.length; i++) {
			int permuteIndex = permute[i];
			if ((permuteIndex < 0) || (permuteIndex >= permuteArgs.length)){
				throw new IllegalArgumentException();
			}
			if (handleArgs[i] != permuteArgs[permuteIndex]) {
				throw new IllegalArgumentException();
			}
		}
		return true;
	}

	/*
	 * Helper method to dropArguments(). This method must be called with cloned array Class<?>... valueType.
	 */
	private static MethodHandle dropArgumentsUnsafe(MethodHandle originalHandle, int location, Class<?>... valueTypes) {
		int valueTypeLength = valueTypes.length; // implicit null check
		MethodType originalType = originalHandle.type;	// implicit null check
		if ((location < 0) || (location > originalType.parameterCount())) {
			// K039c = Invalid parameters
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K039c")); //$NON-NLS-1$
		}

		MethodType permuteType = originalType.insertParameterTypes(location, valueTypes);
		/* Build equivalent permute array */
		int[] permute = new int[originalType.parameterCount()];
		int originalIndex = 0;
		for (int i = 0; i < permute.length; i++) {
			if (originalIndex == location) {
				originalIndex += valueTypeLength;
			}
			permute[i] = originalIndex++;
		}
		assert(validatePermutationArray(permuteType, originalType, permute));
		return originalHandle.permuteArguments(permuteType, permute);
	}

	/**
	 * This method returns a method handle that delegates to the original method handle,
	 * ignoring a particular range of arguments (starting at a given location and
	 * with given types).  The type of the returned method handle is the type of the original handle
	 * with the given types inserted in the parameter type list at the given location.
	 *
	 * @param originalHandle - the original method handle to be transformed
	 * @param location -  the location of the first argument to be removed
	 * @param valueTypes - an array of the argument types to be removed
	 * @return a MethodHandle - representing a transformed handle as described above
	 */
	public static MethodHandle dropArguments(MethodHandle originalHandle, int location, Class<?>... valueTypes) {
		valueTypes = valueTypes.clone();
		return dropArgumentsUnsafe(originalHandle, location, valueTypes);
	}

	/**
	 * This method returns a method handle that delegates to the original method handle,
	 * ignoring a particular range of arguments (starting at a given location and
	 * with given types).  The type of the returned method handle is the type of the original handle
	 * with the given types inserted in the parameter type list at the given location.
	 *
	 * @param originalHandle - the original method handle to be transformed
	 * @param location -  the location of the first argument to be removed
	 * @param valueTypes - a List of the argument types to be removed
	 * @return a MethodHandle - representing a transformed handle as described above
	 */
	public static MethodHandle dropArguments(MethodHandle originalHandle, int location, List<Class<?>> valueTypes) {
		valueTypes.getClass();	// implicit null check
		Class<?>[] valueTypesCopy = new Class<?>[valueTypes.size()];
		for(int i = 0; i < valueTypes.size(); i++) {
			valueTypesCopy[i] = valueTypes.get(i);
		}
		return dropArgumentsUnsafe(originalHandle, location, valueTypesCopy);
	}

	/* A helper method to invoke argument transformation helpers */
	private static MethodHandle buildTransformHandle(ArgumentHelper helper, MethodType mtype){
		MethodType helperType = MethodType.methodType(Object.class, Object[].class);
		try {
			return Lookup.internalPrivilegedLookup.bind(helper, "helper", helperType).asCollector(Object[].class, mtype.parameterCount()).asType(mtype); //$NON-NLS-1$
		} catch(IllegalAccessException | NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	/**
	 * Produce an adapter that converts the incoming arguments from <i>type</i> to the underlying MethodHandle's type
	 * and converts the return value as required.
	 * <p>
	 * The following conversions, beyond those allowed by {@link MethodHandle#asType(MethodType)} are also allowed:
	 * <ul>
	 * <li>A conversion to an interface is done without a cast</li>
	 * <li>A boolean is treated as a single bit unsigned integer and may be converted to other primitive types</li>
	 * <li>A primitive can also be cast using Java casting conversion if asType would have allowed Java method invocation conversion</li>
	 * <li>An unboxing conversion, possibly followed by a widening primitive conversion</li>
	 * </ul>
	 * These additional rules match Java casting conversion and those of the bytecode verifier.
	 *
	 * @param handle - the MethodHandle to invoke after converting the arguments to its type
	 * @param type - the type to convert from
	 * @return a MethodHandle which does the required argument and return conversions, if possible
	 * @throws NullPointerException - if either of the arguments are null
	 * @throws WrongMethodTypeException - if an illegal conversion is requested
	 */
	public static MethodHandle explicitCastArguments(MethodHandle handle, MethodType type) throws NullPointerException, WrongMethodTypeException {
		MethodType handleType = handle.type;	// implicit null check
		Class<?> newReturnType = type.returnType; // implicit null check

		if (handleType == type) {
			return handle;
		}
		MethodHandle mh = handle;
		if (handleType.returnType != newReturnType) {
			MethodHandle filter = FilterHelpers.getReturnFilter(handleType.returnType, newReturnType, true);
			mh = new FilterReturnHandle(handle, filter);
			/* Exit early if only return types differ */
			if (mh.type == type) {
				return mh;
			}
		}
		return new ExplicitCastHandle(mh, type);
	}

	/**
	 * This method returns a method handle that delegates to the original method handle,
	 * adding a particular range of arguments (starting at a given location and
	 * with given types).  The type of the returned method handle is the type of the original handle
	 * with the given types dropped from the parameter type list at the given location.
	 *
	 * @param originalHandle - the original method handle to be transformed
	 * @param location -  the location of the first argument to be inserted
	 * @param values - an array of the argument types to be inserted
	 * @return a MethodHandle - representing a transformed handle as described above
	 */
	public static MethodHandle insertArguments(MethodHandle originalHandle, int location, Object... values) {
		MethodType originalType = originalHandle.type; // expected NPE if originalHandle is null
		Class<?>[] arguments = originalType.parameterArray();

		boolean noValuesToInsert = values.length == 0;  // expected NPE.  Must be null checked before location is checked.

		if ((location < 0) || (location >= originalType.parameterCount())) {
			throw new IllegalArgumentException();
		}

		if (noValuesToInsert) {
			// No values to insert
			return originalHandle;
		}

		// clone the values[] so it can't be modified after validation occurs
		values = values.clone();

		/* This loop does two things:
		 * 1) Validates that the 'values' can be legitimately inserted in originalHandle
		 * 2) Builds up the parameter array for the asType operation
		 */
		for (int i = 0; i < values.length; i++) { // expected NPE if values is null
			Class<?> clazz = arguments[location + i];
			Object value = values[i];
			Class<?> valueClazz = clazz;
			if (value != null) {
				valueClazz = value.getClass();
			}
			if (clazz.isPrimitive()) {
				Objects.requireNonNull(value);
				Class<?> unwrapped = MethodType.unwrapPrimitive(valueClazz);
				if ((clazz != unwrapped) && !FilterHelpers.checkIfWideningPrimitiveConversion(unwrapped, clazz)) {
					clazz.cast(value);	// guaranteed to throw ClassCastException
				}
			} else {
				clazz.cast(value);
			}
			// overwrite the original argument with the new class from the values[]
			arguments[location + i] = valueClazz;
		}
		MethodHandle asTypedOriginalHandle = originalHandle.asType(MethodType.methodType(originalType.returnType, arguments));

		MethodType mtype = originalType.dropParameterTypes(location, location + values.length);
		MethodHandle insertHandle;
		switch(values.length) {
		case 1: {
			if (originalType.parameterType(location) == int.class) {
				insertHandle = new Insert1IntHandle(mtype, asTypedOriginalHandle, location, values, originalHandle);
			} else {
				insertHandle = new Insert1Handle(mtype, asTypedOriginalHandle, location, values);
			}
			break;
		}
		case 2:
			insertHandle = new Insert2Handle(mtype, asTypedOriginalHandle, location, values);
			break;
		case 3:
			insertHandle = new Insert3Handle(mtype, asTypedOriginalHandle, location, values);
			break;
		default:
			insertHandle = new InsertHandle(mtype, asTypedOriginalHandle, location, values);
		}

		// Now give originalHandle a chance to supply a handle with a better thunk archetype
		return originalHandle.insertArguments(insertHandle, asTypedOriginalHandle, location, values);
	}

	private static final class CatchHelper implements ArgumentHelper {
		private final MethodHandle tryTarget;
		private final MethodHandle catchTarget;
		private final Class<? extends Throwable> exceptionClass;

		CatchHelper(MethodHandle tryTarget, MethodHandle catchTarget, Class<? extends Throwable> exceptionClass) {
			this.tryTarget = tryTarget;
			this.catchTarget = catchTarget;
			this.exceptionClass = exceptionClass;
		}

		public Object helper(Object[] arguments) throws Throwable {
			try {
				return tryTarget.invokeWithArguments(arguments);
			} catch(Throwable t) {
				if (exceptionClass.isInstance(t)) {
					int catchArgCount = catchTarget.type.parameterCount();
					Object[] amendedArgs = new Object[catchArgCount];
					amendedArgs[0] = t;
					System.arraycopy(arguments, 0, amendedArgs, 1, catchArgCount - 1);
					return catchTarget.invokeWithArguments(amendedArgs);
				}
				throw t;
			}
		}
	}

	/**
	 * Helper class used by collectArguments.
	 */
	private static final class CollectHelper implements ArgumentHelper {
		private final MethodHandle target;
		private final MethodHandle filter;
		private final int pos;

		CollectHelper(MethodHandle target, int pos, MethodHandle filter) {
			this.target = target;
			this.filter = filter;
			this.pos = pos;
		}

		public Object helper(Object[] arguments) throws Throwable {
			// Invoke filter
			int filterArity = filter.type.parameterCount();
			Object filterReturn = filter.invokeWithArguments(Arrays.copyOfRange(arguments, pos, pos + filterArity));

			// Construct target arguments
			Object[] newArguments = new Object[(arguments.length - filterArity) + 1];
			System.arraycopy(arguments, 0, newArguments, 0, pos);
			int resumeTargetArgs = pos + filterArity;
			newArguments[pos] = filterReturn;
			System.arraycopy(arguments, resumeTargetArgs, newArguments, (pos + 1), arguments.length - (resumeTargetArgs));

			// Invoke target
			return target.invokeWithArguments(newArguments);
		}
	}

	/**
	 * Helper class used by collectArguments when the filter handle returns void.
	 */
	private static final class VoidCollectHelper implements ArgumentHelper {
		private final MethodHandle target;
		private final MethodHandle filter;
		private final int pos;

		VoidCollectHelper(MethodHandle target, int pos, MethodHandle filter) {
			this.target = target;
			this.filter = filter;
			this.pos = pos;
		}

		public Object helper(Object[] arguments) throws Throwable {
			// Invoke filter
			int filterArity = filter.type.parameterCount();
			filter.invokeWithArguments(Arrays.copyOfRange(arguments, pos, pos + filterArity));

			// Construct target arguments
			Object[] newArguments = new Object[arguments.length - filterArity];
			System.arraycopy(
					arguments, 0,
					newArguments, 0,
					pos);
			int resumeTargetArgs = pos + filterArity;
			System.arraycopy(
					arguments, resumeTargetArgs,
					newArguments, pos,
					arguments.length - (resumeTargetArgs));

			// Invoke target
			return target.invokeWithArguments(newArguments);
		}
	}
}

