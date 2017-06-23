/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.FrameIteratorSkip;

/* ExplicitCastHandle is a MethodHandle subclass used to implement
 * MethodHandles.explicitCastArgument()
 * <p>
 * The vmSlot will hold 0 as there is no actual method for it.
 *
 */
final class ExplicitCastHandle extends ArgumentConversionHandle {

	ExplicitCastHandle(MethodHandle handle, MethodType type) {
		super(handle, type, null, null, KIND_EXPLICITCAST);
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	protected static native int convertArgs(int argPlaceholder);

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) throws Throwable {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(next);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		return ILGenMacros.invokeExact_X(next, convertArgs(argPlaceholder));
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new ExplicitCastHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof ExplicitCastHandle) {
			((ExplicitCastHandle)right).compareWithExplicitCast(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithExplicitCast(ExplicitCastHandle left, Comparator c) {
		compareWithConvert(left, c);
	}
}

