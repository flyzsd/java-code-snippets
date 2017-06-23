/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2012, 2016  All Rights Reserved
 */
package java.lang.invoke;

/*
 * A simple class that will be injected in each class loader, as
 * required, to act as a "trampoline" to ensure that methods which
 * are sensitive to their caller (ie: use getCallerClass())
 * can find a class in the correct ClassLoader and ProtectionDomain
 * when invoked by MethodHandle invocation.
 */
final class SecurityFrame {

	private final MethodHandle target;
	/* Required for revealDirect() access checking */
	@SuppressWarnings("unused")
	private final Class<?> accessClass;

	public SecurityFrame(MethodHandle target, Class<?> accessClass) {
		this.target = target.asFixedArity();
		this.accessClass = accessClass;
	}

	public Object invoke(Object... args) throws Throwable {
		return target.invokeWithArguments(args);
	}
}

