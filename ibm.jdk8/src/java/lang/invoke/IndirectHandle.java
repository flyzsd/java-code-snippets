/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */

package java.lang.invoke;

abstract class IndirectHandle extends MethodHandle {
	IndirectHandle(MethodType type, Class<?> definingClass, String name, int kind, int modifiers) {
		super(type, definingClass, name, kind, modifiers, null);
	}

	IndirectHandle(MethodType type, Class<?> definingClass, String name, int kind) {
		super(type, definingClass, name, kind, null);
	}

	IndirectHandle(IndirectHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	// {{{ JIT support
	protected abstract long vtableOffset(Object receiver);
	protected final long vtableIndexArgument(Object receiver){ return - vtableOffset(receiver); }

	protected final long jittedMethodAddress(Object receiver) {
		long receiverClass = getJ9ClassFromClass(receiver.getClass());
		long result;
		if (VTABLE_ENTRY_SIZE == 4) {
			result = getUnsafe().getInt(receiverClass - vtableOffset(receiver));
		} else {
			result = getUnsafe().getLong(receiverClass - vtableOffset(receiver));
		}
		return result;
	}

	@Override
	boolean canRevealDirect() {
		return true;
	}

	// }}} JIT support

	final void compareWithIndirect(IndirectHandle left, Comparator c) {
		c.compareStructuralParameter(left.definingClass, this.definingClass);
		c.compareStructuralParameter(left.vmSlot, this.vmSlot);
	}
}

