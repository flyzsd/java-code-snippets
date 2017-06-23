/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

abstract class FoldHandle extends MethodHandle {
	protected final MethodHandle next;
	protected final MethodHandle combiner;

	protected FoldHandle(MethodHandle next, MethodHandle combiner, MethodType type, Class<?> definingClass, String name) {
		super(type, definingClass, name, KIND_FOLDHANDLE, 0, combiner.type());
		this.next     = next;
		this.combiner = combiner;
	}

	FoldHandle(FoldHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.next = originalHandle.next;
		this.combiner = originalHandle.combiner;
	}

	public static FoldHandle get(MethodHandle next, MethodHandle combiner, MethodType type) {
		if (combiner.type().returnType() == void.class) {
			return new FoldVoidHandle(next, combiner, type);
		} else {
			return new FoldNonvoidHandle(next, combiner, type);
		}
	}

 	protected final ThunkTuple computeThunks(Object combinerType) {
 		// Different thunks accomodate combiners with different numbers of parameters
 		return thunkTable().get(new ThunkKeyWithObject(ThunkKey.computeThunkableType(type()), ThunkKey.computeThunkableType((MethodType)combinerType)));
 	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof FoldHandle) {
			((FoldHandle)right).compareWithFold(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithFold(FoldHandle left, Comparator c) {
		c.compareChildHandle(left.next, this.next);
		c.compareChildHandle(left.combiner, this.combiner);
	}
}

final class FoldNonvoidHandle extends FoldHandle {

	FoldNonvoidHandle(MethodHandle next, MethodHandle combiner, MethodType type) {
		super(next, combiner, type, FoldNonvoidHandle.class, "FoldNonvoidHandle"); //$NON-NLS-1$
	}

	// {{{ JIT support

	FoldNonvoidHandle(FoldNonvoidHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected ThunkTable thunkTable(){ return _thunkTable; }

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(combiner, next);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		return ILGenMacros.invokeExact_X(next, ILGenMacros.placeholder(
			ILGenMacros.invokeExact(combiner, ILGenMacros.firstN(ILGenMacros.parameterCount(combiner), argPlaceholder)),
			argPlaceholder));
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new FoldNonvoidHandle(this, newType);
	}
}

final class FoldVoidHandle extends FoldHandle {

	FoldVoidHandle(MethodHandle next, MethodHandle combiner, MethodType type) {
		super(next, combiner, type, FoldVoidHandle.class, "FoldVoidHandle"); //$NON-NLS-1$
	}

	FoldVoidHandle(FoldVoidHandle foldVoidHandle, MethodType newType) {
		super(foldVoidHandle, newType);
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected ThunkTable thunkTable(){ return _thunkTable; }

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(combiner, next);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		ILGenMacros.invokeExact(combiner, ILGenMacros.firstN(ILGenMacros.parameterCount(combiner), argPlaceholder));
		return ILGenMacros.invokeExact_X(next, argPlaceholder);
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new FoldVoidHandle(this,  newType);
	}
}

