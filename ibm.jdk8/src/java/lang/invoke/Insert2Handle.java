/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

final class Insert2Handle extends InsertHandle {
	final Object value_1;
	final Object value_2;

	Insert2Handle(MethodType type, MethodHandle next, int insertionIndex, Object values[]) {
		super(type, next, insertionIndex, values);
		this.value_1 = values[0];
		this.value_2 = values[1];
	}

	Insert2Handle(Insert2Handle originalHandle, MethodType nextType) {
		super(originalHandle, nextType);
		this.value_1 = originalHandle.value_1;
		this.value_2 = originalHandle.value_2;
	}

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new Insert2Handle(this, newType);
	}

	// {{{ JIT support
	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	protected final ThunkTuple computeThunks(Object arg) {
		int[] info = (int[])arg;
		return thunkTable().get(new ThunkKeyWithIntArray(ThunkKey.computeThunkableType(type()), info));
	}

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(next);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		return ILGenMacros.invokeExact_X(next, ILGenMacros.placeholder(
				ILGenMacros.firstN(numPrefixArgs(), argPlaceholder),
				value_1,
				value_2,
				ILGenMacros.lastN(numSuffixArgs(), argPlaceholder)));
	}

	// }}} JIT support
}

