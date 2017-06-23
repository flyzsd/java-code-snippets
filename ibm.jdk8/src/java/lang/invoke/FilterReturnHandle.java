/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.FrameIteratorSkip;

@VMCONSTANTPOOL_CLASS
final class FilterReturnHandle extends ConvertHandle {
	@VMCONSTANTPOOL_FIELD
	final MethodHandle filter;

	FilterReturnHandle(MethodHandle next, MethodHandle filter) {
		super(next, next.type.changeReturnType(filter.type.returnType), FilterReturnHandle.class, "FilterReturnHandle", KIND_FILTERRETURN, filter.type()); //$NON-NLS-1$
		this.filter = filter;
	}

	FilterReturnHandle(FilterReturnHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.filter = originalHandle.filter;
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	protected final ThunkTuple computeThunks(Object filterHandleType) {
		// We include the full type of filter in order to get the type cast right.
		return thunkTable().get(new ThunkKeyWithObject(ThunkKey.computeThunkableType(type()), filterHandleType));
	}

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) throws Throwable {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(filter, next);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		return ILGenMacros.invokeExact_X(filter, ILGenMacros.invokeExact(next, argPlaceholder));
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new FilterReturnHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof FilterReturnHandle) {
			((FilterReturnHandle)right).compareWithFilterReturn(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithFilterReturn(FilterReturnHandle left, Comparator c) {
		compareWithConvert(left, c);
		c.compareChildHandle(left.filter, this.filter);
	}
}

