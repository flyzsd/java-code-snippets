/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

import static java.lang.invoke.MethodType.*;

/**
 * CallSite is used by the invokedynamic bytecode to hold a reference to the MethodHandle target of the instruction.
 * <p>
 * Although CallSite is an abstract class, it cannot be directly sub-classed.  Instead, it is necessary to sub-class
 * one of the three implementation classes:
 * <ul>
 * <li>ConstantCallSite - if the target will never change</li>
 * <li>VolatileCallSite - if the target is expected to frequently change.  Changes will be immediately visible in all threads.</li>
 * <li>MutableCallSite - if the target is expected to rarely change and threads may see previous values of the target for some time.</li>
 * </ul>
 *
 * <p>
 * CallSites are created with a MethodType and permanently bound to that type.  Any changes to the target
 * MethodHandle must be of the identical MethodType or a WrongMethodTypeException will be thrown.
 *
 * @since 1.7
 */
public abstract class CallSite {
	private final MethodType type;
	private static MethodHandle initialTargetHandleCache;

	CallSite(MethodType type){
		type.getClass(); // Throw NPE if null
		this.type = type;
	}

	/**
	 * Report the type of CallSite's target MethodHandle.
	 * A CallSite cannot change its type.
	 * @return The permanent MethodType of this CallSite.
	 */
	public MethodType type() {
		return type;
	}

	/**
	 * Return the target MethodHandle of the CallSite.
	 *
	 * @return the current target MethodHandle
	 */
	public abstract MethodHandle getTarget();

	/**
	 * Set the CallSite's target to be <i>nextTarget</i>.
	 * The <i>nextTarget</i> MethodHandle must have the same type as the CallSite.
	 *
	 * @param nextTarget - the new target value for the CallSite
	 * @throws WrongMethodTypeException - if the type of <i>nextTarget</i> differs from that of the CallSite.
	 * @throws NullPointerException - if <i>nextTarget</i> is null.
	 */
	public abstract void setTarget(MethodHandle nextTarget) throws WrongMethodTypeException, NullPointerException;

	/**
	 * Return a MethodHandle equivalent to the invokedynamic instruction on this CallSite.
	 * The MethodHandle is equivalent to getTarget().invokeExact(args).
	 *
	 * @return a MethodHandle that is equivalent to an invokedynamic instruction on this CallSite.
	 */
	public abstract MethodHandle dynamicInvoker();

	/* Defer the creation of the Exception until called in the IllegalState */
	static void throwIllegalStateException() throws IllegalStateException {
		throw new IllegalStateException();
	}

	/* Return the initial target for the CallSite - it will throw an IllegalStateException.  */
	static MethodHandle initialTarget(MethodType type) {
		MethodHandle initialTargetHandle = initialTargetHandleCache;
		if (null == initialTargetHandle) {
			initialTargetHandle = lookupInitialTarget();
		}
		initialTargetHandle = initialTargetHandle.asType(methodType(type.returnType));
		/* Adapt the initial target to be compliant with what the caller expects */
		return MethodHandles.dropArguments(initialTargetHandle, 0, type.arguments);
	}

	/* Initialize the cached MethodHandle for initialTarget */
	private static MethodHandle lookupInitialTarget() {
		try {
			initialTargetHandleCache = MethodHandles.Lookup.internalPrivilegedLookup.findStatic(CallSite.class, "throwIllegalStateException", methodType(void.class)); //$NON-NLS-1$
		} catch (IllegalAccessException | NoSuchMethodException e) {
			throw new InternalError("Unable to lookup CallSite initial target"); //$NON-NLS-1$
		}
		return initialTargetHandleCache;
	}
}

