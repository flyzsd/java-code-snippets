/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

final class CatchHandle extends PassThroughHandle {
	private final MethodHandle tryTarget;
	private final Class<? extends Throwable> exceptionClass;
	private final MethodHandle catchTarget;

	protected CatchHandle(MethodHandle tryTarget, Class<? extends Throwable> exceptionClass, MethodHandle catchTarget, MethodHandle equivalent) {
		super(equivalent, "CatchHandle", infoAffectingThunks(catchTarget.type().parameterCount())); //$NON-NLS-1$
		this.tryTarget = tryTarget;
		this.exceptionClass = exceptionClass;
		this.catchTarget = catchTarget;
	}

	CatchHandle(CatchHandle catchHandle, MethodType newType) {
		super(catchHandle, newType);
		this.tryTarget = catchHandle.tryTarget;
		this.exceptionClass = catchHandle.exceptionClass;
		this.catchTarget = catchHandle.catchTarget;
	}

	MethodHandle cloneWithNewType(MethodType newType) {
		return new CatchHandle(this, newType);
	}

	public static CatchHandle get(MethodHandle tryTarget, Class<? extends Throwable> exceptionClass, MethodHandle catchTarget, MethodHandle equivalent) {
		return new CatchHandle(tryTarget, exceptionClass, catchTarget, equivalent);
	}

	// {{{ JIT support
	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	private static native int numCatchTargetArgsToPassThrough();

	private static Object infoAffectingThunks(int numCatchTargetArgs) {
		// The number of arguments passed to the catch target affects the code generated in the thunks
		return numCatchTargetArgs;
	}

	protected final ThunkTuple computeThunks(Object arg) {
		int numCatchTargetArgs = (Integer)arg;
		return thunkTable().get(new ThunkKeyWithInt(ThunkKey.computeThunkableType(type()), numCatchTargetArgs));
	}

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(tryTarget, catchTarget);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		try {
			return ILGenMacros.invokeExact_X(tryTarget, argPlaceholder);
		} catch (Throwable t) {
			if (exceptionClass.isInstance(t)) {
				return ILGenMacros.invokeExact_X(catchTarget, ILGenMacros.placeholder(t, ILGenMacros.firstN(numCatchTargetArgsToPassThrough(), argPlaceholder)));
			} else {
				throw t;
			}
		}
	}

	// }}} JIT support

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof CatchHandle) {
			((CatchHandle)right).compareWithCatch(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithCatch(CatchHandle left, Comparator c) {
		c.compareStructuralParameter(left.exceptionClass, this.exceptionClass);
		c.compareChildHandle(left.tryTarget, this.tryTarget);
		c.compareChildHandle(left.catchTarget, this.catchTarget);
	}
}

