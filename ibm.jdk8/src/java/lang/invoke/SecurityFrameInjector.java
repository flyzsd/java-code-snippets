/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2012, 2016  All Rights Reserved
 */
package java.lang.invoke;

import static java.lang.invoke.MethodType.methodType;

import java.io.InputStream;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import sun.misc.IOUtils;
import sun.misc.Unsafe;

import com.ibm.oti.vm.VMLangAccess;

/*
 * A factory class that manages the relationship between ClassLoaders and the
 * injected SecurityFrame class, which acts as a trampoline to provide the
 * correct view to caller sensitive methods.
 * This class also provides an API to determine which methods are sensitive to
 * their callers.
 */
final class SecurityFrameInjector {

	/* Map from ClassLoader to injected java.lang.invoke.SecurityFrame.  Map is required as findClass() will
	 * search the hierarchy.
	 */
	static Map<ClassLoader, WeakReference<Class<?>>> LoaderToSecurityFrameClassMap = Collections.synchronizedMap(new WeakHashMap<ClassLoader, WeakReference<Class<?>>>());

	static byte[] securityFrameClassBytes = null;

	/* Private class to act as a lock as using 'new Object()' for a lock
	 * prevents lock word optimizations.
	 */
	static final class SecurityFrameInjectorLoaderLock { };
	/* Single shared lock to control attempts to define SecurityFrame in different loaders */
	static SecurityFrameInjectorLoaderLock loaderLock = new SecurityFrameInjectorLoaderLock();

	/*
	 * Helper method required by MethodHandleUtils.isCallerSensitive() to determine
	 * if a methodhandle can be called virtually from the class that defines the
	 * caller-sensitive method.
	 */
	static boolean virtualCallAllowed(MethodHandle handle, Class<?> sensitiveMethodDefiningClass) {
		if (handle.defc == sensitiveMethodDefiningClass) {
			return true;
		}
		int modifiers = handle.rawModifiers;
		if (Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers)) {
			return false;
		}
		if (sensitiveMethodDefiningClass.isAssignableFrom(handle.defc) || handle.defc.isInterface()) {
			return true;
		}
		return false;
	}

	/*
	 * Reads and caches the classfile bytes for java.lang.invoke.SecurityFrame
	 */
	static byte[] initializeSecurityFrameClassBytes() {
		if (securityFrameClassBytes == null) {
			synchronized(SecurityFrameInjector.class) {
				if (securityFrameClassBytes == null) {
					securityFrameClassBytes = AccessController.doPrivileged(new PrivilegedAction<byte[]>() {
						public byte[] run() {
							try {
								InputStream is = Lookup.class.getResourceAsStream("/java/lang/invoke/SecurityFrame.class"); //$NON-NLS-1$
								return IOUtils.readFully(is, -1, true);
							} catch(java.io.IOException e) {
								// K056A = Unable to read java.lang.invoke.SecurityFrame.class bytes
								throw new Error(com.ibm.oti.util.Msg.getString("K056A"), e); //$NON-NLS-1$
							}
						}
					});
				}
			}
		}
		return securityFrameClassBytes;
	}

	static Class<?> probeLoaderToSecurityFrameMap(ClassLoader loader) {
		WeakReference<Class<?>> weakRef = LoaderToSecurityFrameClassMap.get(loader);
		if (weakRef != null) {
			return weakRef.get();
		}
		return null;
	}

	/*
	 * Wraps the passed in MethodHandle with a trampoline MethodHandle.  The trampoline handle
	 * is from the same classloader & protection domain as the context class argument.
	 *
	 * Ie: Class A does lookup().in(Object.class).find.... The trampoline will be built against
	 * class A, not Object.
	 */
	static MethodHandle wrapHandleWithInjectedSecurityFrame(MethodHandle handle, final Class<?> context) {
		initializeSecurityFrameClassBytes();

		/* preserve varargs status so that the wrapper handle is also correctly varargs/notvarargs */
		boolean isVarargs = Lookup.isVarargs(handle.rawModifiers) || handle.isVarargsCollector();
		final MethodHandle tempFinalHandle = handle;
		MethodType originalType = handle.type;

		try {
			Object o = AccessController.doPrivileged(new PrivilegedAction<Object>() {

				/* Helper method to Unsafe.defineClass the securityFrame into the
				 * provided ClassLoader and ProtectionDomain.
				 */
				private Class<?> injectSecurityFrameIntoLoader(ClassLoader loader, ProtectionDomain pd) {
					return Unsafe.getUnsafe().defineClass(
							"java.lang.invoke.SecurityFrame", //$NON-NLS-1$
							securityFrameClassBytes,
							0,
							securityFrameClassBytes.length,
							loader,
							pd);
				}

				public Object run() {
					Class<?> injectedSecurityFrameClass;
					VMLangAccess vma = Lookup.getVMLangAccess();
					ClassLoader rawLoader = vma.getClassloader(context);
					/* Probe map to determine if java.lang.invoke.SecurityFrame has already been injected
					 * into this ClassLoader.  If it hasn't, use Unsafe to define it.  We can't use
					 * Class.forName() here as it will crawl the class heirarchy and SecurityFrame must
					 * be included in each loader / protection domain.
					 */
					injectedSecurityFrameClass = probeLoaderToSecurityFrameMap(rawLoader);
					if (injectedSecurityFrameClass == null) {
						synchronized(loaderLock) {
							injectedSecurityFrameClass = probeLoaderToSecurityFrameMap(rawLoader);
							if (injectedSecurityFrameClass == null) {
								injectedSecurityFrameClass = injectSecurityFrameIntoLoader(rawLoader, context.getProtectionDomain());
								LoaderToSecurityFrameClassMap.put(rawLoader, new WeakReference<Class<?>>(injectedSecurityFrameClass));
							}
						}
					}

					try {
						Constructor<?> constructor = injectedSecurityFrameClass.getConstructor(MethodHandle.class, Class.class);
						constructor.setAccessible(true);
						return constructor.newInstance(tempFinalHandle, context);
					} catch (SecurityException | ReflectiveOperationException e) {
						throw new Error(e);
					}
				}
			});
			handle = Lookup.internalPrivilegedLookup.bind(o, "invoke", methodType(Object.class, Object[].class)); //$NON-NLS-1$
			handle = handle.asType(originalType);
			if (isVarargs) {
				handle = handle.asVarargsCollector(originalType.lastParameterType());
			}
		} catch (IllegalAccessException | NoSuchMethodException e) {
			throw new Error(e);
		}
		return handle;
	}

	/*
	 * Helper method to wrap a MethodHandle with an injected SecurityFrame if required.  The resulting MH chain will be:
	 * AsType(type) -> ReceiverBoundHandle(Object[]) --invokeWArgs-> original handle
	 * VarargsCollector nature will be preserved.
	 */
	static MethodHandle wrapHandleWithInjectedSecurityFrameIfRequired(Lookup lookup, MethodHandle handle) throws IllegalAccessException {
		if (isCallerSensitive(handle)) {
			if (lookup.isWeakenedLookup()) {
				// K0589 = Caller-sensitive method cannot be looked up using a restricted lookup object
				throw new IllegalAccessException(com.ibm.oti.util.Msg.getString("K0589")); //$NON-NLS-1$
			}
			handle = wrapHandleWithInjectedSecurityFrame(handle, lookup.accessClass);
		}
		return handle;
	}

	private static final int CALLER_SENSITIVE_BIT = 0x100000;
	/*
	 * Determine if a method is caller-sensitive based on whether it has the
	 * sun.reflect.CallerSensitive annotation.
	 */
	static boolean isCallerSensitive(MethodHandle mh) {
		return CALLER_SENSITIVE_BIT == (mh.rawModifiers & CALLER_SENSITIVE_BIT);
	}

	/*
	 * Attempt to unwrap a MethodHandle that has been wrapped in a SecurityFrame.
	 *
	 * @param potentialInjectFrame The wrapped MethodHandle
	 * @param lookup A Lookup object with the same lookup class as the Lookup object used to create the MethodHandle
	 *
	 * @return The unwrapped MethodHandle if successfully unwrapped, null if not.
	 */
	static MethodHandle penetrateSecurityFrame(MethodHandle potentialInjectFrame, Lookup lookup) {
		final MethodType originalMT = potentialInjectFrame.type;

		// SecurityFrames are always represented as:
		// 1) VaragsCollectHandle -> AsTypeHandle -> RBH with bound value being an instance of SecurityFrame
		// 2) AsTypeHandle -> RBH with bound value being an instance of SecurityFrame
		// 3) RBH with bound value being an instance of SecurityFrame if signature is (Object[])Object
		boolean mustBeVarags = false;
		if (potentialInjectFrame.kind == MethodHandle.KIND_VARARGSCOLLECT) {
			mustBeVarags = true;
			potentialInjectFrame = ((VarargsCollectorHandle)potentialInjectFrame).next;
		}
		if (potentialInjectFrame.kind == MethodHandle.KIND_ASTYPE) {
			potentialInjectFrame = ((AsTypeHandle)potentialInjectFrame).next;
		}
		if (potentialInjectFrame.kind == MethodHandle.KIND_FILTERRETURN) {
			potentialInjectFrame = ((FilterReturnHandle)potentialInjectFrame).next;
		}
		if (potentialInjectFrame.kind == MethodHandle.KIND_COLLECT) {
			potentialInjectFrame = ((CollectHandle)potentialInjectFrame).next;
		}
		if (potentialInjectFrame.kind == MethodHandle.KIND_ASTYPE) {
			potentialInjectFrame = ((AsTypeHandle)potentialInjectFrame).next;
		}
		if (potentialInjectFrame.kind == MethodHandle.KIND_FILTERRETURN) {
			potentialInjectFrame = ((FilterReturnHandle)potentialInjectFrame).next;
		}

		if (potentialInjectFrame.kind == MethodHandle.KIND_BOUND) {
			ReceiverBoundHandle rbh = (ReceiverBoundHandle)potentialInjectFrame;
			final Object receiver = rbh.receiver;

			VMLangAccess vma = Lookup.getVMLangAccess();
			ClassLoader rawLoader = vma.getClassloader(receiver.getClass());
			Class<?> injectedSecurityFrame = null;
			synchronized (loaderLock) {
				injectedSecurityFrame = probeLoaderToSecurityFrameMap(rawLoader);
			}
			if ((injectedSecurityFrame == null) || !injectedSecurityFrame.isInstance(receiver)) {
				/* receiver object cannot be an instance of SecurityFrame as its classloader
				 * doesn't have an injected security frame class
				 */
				return null;
			}
			final Class<?> finalInjectedSecurityFrame = injectedSecurityFrame;
			MethodHandle target = AccessController.doPrivileged(new PrivilegedAction<MethodHandle>() {
				public MethodHandle run() {
					try {
						Field targetField = finalInjectedSecurityFrame.getDeclaredField("target"); //$NON-NLS-1$
						targetField.setAccessible(true);
						return (MethodHandle)targetField.get(receiver);
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
						throw (InternalError)new InternalError().initCause(e);
					}
				}
			});
			Class<?> targetAccessClass = AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
				public Class<?> run() {
					try {
						Field targetField = finalInjectedSecurityFrame.getDeclaredField("accessClass"); //$NON-NLS-1$
						targetField.setAccessible(true);
						return (Class<?>)targetField.get(receiver);
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
						throw (InternalError)new InternalError().initCause(e);
					}
				}
			});
			if (lookup.accessMode != MethodHandles.Lookup.INTERNAL_PRIVILEGED) {
				if (targetAccessClass != lookup.accessClass) {
					return null;
				}
			}
			if ((target.type == originalMT) && (Lookup.isVarargs(target.rawModifiers) == mustBeVarags)) {
				return target;
			}
		}
		// Not a directHandle - someone added wrapping to it.
		return null;
	}

}

