/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2014, 2016  All Rights Reserved
 */
package java.lang.invoke;

import static java.lang.invoke.MethodHandles.lookup;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;

final class MethodHandleInfoImpl implements MethodHandleInfo {
	private final MethodHandle mh;

	MethodHandleInfoImpl(MethodHandle methodHandle) {
		this.mh = methodHandle;
	}

	public Class<?> getDeclaringClass() {
		return mh.defc;
	}

	public String getName() {
		return mh.name;
	}

	public MethodType getMethodType() {
		if (requiresReceiver()) {
			return mh.type.dropParameterTypes(0, 1);
		} else if (isConstructor()) {
			return mh.type.changeReturnType(void.class);
		}
		return mh.type;
	}

	public int getModifiers() {
		if (isMethod() || isConstructor()) {
			return	(mh.rawModifiers & (
						Modifier.methodModifiers() | // JLS 8.4.3
						0x0080 | 0x0040 | 0x1000 // ACC_VARARGS | ACC_BRIDGE | ACC_SYNTHETIC
					));
		} else if (isField()) {
			return	(mh.rawModifiers & (
						Modifier.fieldModifiers() | // JLS 8.3.1
						0x1000 // ACC_SYNTHETIC
					));
		} else {
			// This case can't happen because all possible reference kinds are covered by the other cases. Returning -1 to indicate error.
			return -1;
		}
	}

	public int getReferenceKind() {
		if (mh.directHandleOriginatedInFindVirtual()
		|| (MethodHandle.KIND_INVOKEEXACT == mh.kind)
		|| (MethodHandle.KIND_INVOKEGENERIC == mh.kind)
		) {
			return REF_invokeVirtual;
		}
		return mh.kind;
	}

	public <T extends Member> T reflectAs(Class<T> expected, MethodHandles.Lookup lookup) throws NullPointerException, IllegalArgumentException, ClassCastException {
		// Error checking
		if ((null == expected) || (null == lookup)) {
			throw new NullPointerException();
		}
		try {
			lookup.checkAccess(mh);
		} catch (IllegalAccessException e) {
			// K0583 = The Member is not accessible to the Lookup object
			IllegalArgumentException x = new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0583")); //$NON-NLS-1$
			x.initCause(e);
			throw x;
		}

		if ((MethodHandle.KIND_INVOKEEXACT == mh.kind) || (MethodHandle.KIND_INVOKEGENERIC == mh.kind)) {
			// K0590 = Can't unreflect @SignaturePolymorphic method: {0}
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0590", this)); //$NON-NLS-1$
		}

		// Get the underlying Member
		Member member = null;
		final Class<?> defc = getDeclaringClass();
		final boolean isPublic = Modifier.isPublic(getModifiers());
		member = AccessController.doPrivileged(new PrivilegedAction<Member>() {
			public Member run() {
				Member result = null;
				try {
					// Field
					if (isField()) {
						if (isPublic) {
							result = defc.getField(getName());
						} else {
							result = defc.getDeclaredField(getName());
						}
					// Method
					} else if (isMethod()) {
						if (isPublic) {
							result = defc.getMethod(getName(), getMethodType().arguments);
						} else {
							result = defc.getDeclaredMethod(getName(), getMethodType().arguments);
						}
					// Constructor
					} else if (isConstructor()) {
						if (isPublic) {
							result = defc.getConstructor(getMethodType().arguments);
						} else {
							result = defc.getDeclaredConstructor(getMethodType().arguments);
						}
					}
				} catch (NoSuchFieldException | NoSuchMethodException e) {
					throw new IllegalArgumentException(e);
				}
				if (null == result) {
					// This should never happen
					throw new InternalError("Unable to get the underlying Member due to invalid reference kind"); //$NON-NLS-1$
				}
				return result;
			}
		});
		expected.cast(member);
		return (T)member;
	}

	/**
	 * @return whether the underlying member requires a receiver object when invoked
	 */
	private boolean requiresReceiver() {
		int kind = getReferenceKind();
		return (REF_getField == kind)
			|| (REF_putField == kind)
			|| (REF_invokeInterface == kind)
			|| (REF_invokeSpecial == kind)
			|| (REF_invokeVirtual == kind);
	}

	boolean isMethod() {
		return (getReferenceKind() >= REF_invokeVirtual) && !isConstructor();
	}

	boolean isField() {
		return getReferenceKind() <= REF_putStatic;
	}

	boolean isConstructor() {
		return getReferenceKind() == REF_newInvokeSpecial;
	}

	@Override
	public String toString() {
		return MethodHandleInfo.toString(getReferenceKind(), getDeclaringClass(), getName(), getMethodType());
	}

}

