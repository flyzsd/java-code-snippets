/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.reflect.Modifier;

/* InvokeExactHandle is a MethodHandle subclass used to MethodHande.invokeExact
 * with a specific signature on a MethodHandle.
 * <p>
 * The vmSlot will hold 0 as there is no actual method for it.
 * <p>
 * Can be thought of as a special case of VirtualHandle.
 */
final class InvokeExactHandle extends MethodHandle {
	/* MethodType that the first argument MethodHandle must match */
	final MethodType nextType;

	InvokeExactHandle(MethodType type) {
		super(invokeExactMethodType(type), MethodHandle.class, "invokeExact", KIND_INVOKEEXACT, PUBLIC_FINAL_NATIVE, null); //$NON-NLS-1$
		nextType = type;
		this.vmSlot = 0;
		this.defc = MethodHandle.class;
	}

	InvokeExactHandle(InvokeExactHandle originalHandle, MethodType newType) {
			super(originalHandle, newType);
			this.nextType = originalHandle.nextType;
	}

	/*
	 * Insert MethodHandle as first argument to existing type.
	 * (LMethodHandle;otherargs)returntype
	 */
	private static final MethodType invokeExactMethodType(MethodType type){
		if (type == null) {
			throw new IllegalArgumentException();
		}
		return type.insertParameterTypes(0, MethodHandle.class);
	}

	@Override
	boolean canRevealDirect() {
		/* This is invokevirtual of MethodHandle.invokeExact() */
		return true;
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	protected ThunkTuple computeThunks(Object arg) {
		// The first argument is always a MethodHandle.
		// We don't upcast that to Object to avoid a downcast in the thunks.
		//
		return thunkTable().get(new ThunkKey(ThunkKey.computeThunkableType(type(), 0, 1)));
	}

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(MethodHandle next, int argPlaceholder) throws Throwable {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(next);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		ILGenMacros.typeCheck(next, nextType);
		return ILGenMacros.invokeExact_X(next, argPlaceholder);
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new InvokeExactHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof InvokeExactHandle) {
			((InvokeExactHandle)right).compareWithInvokeExact(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithInvokeExact(InvokeExactHandle left, Comparator c) {
		// Nothing distinguishes InvokeExactHandles except their type, which Comparator already deals with
	}
}

