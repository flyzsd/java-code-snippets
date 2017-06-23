/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

final class Insert1Handle extends InsertHandle {
	final Object value;

	Insert1Handle(MethodType type, MethodHandle next, int insertionIndex, Object values[]) {
		super(type, next, insertionIndex, values);
		this.value = values[0];
	}

	Insert1Handle(Insert1Handle originalHandle, MethodType nextType) {
		super(originalHandle, nextType);
		this.value = originalHandle.value;
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
				value,
				ILGenMacros.lastN(numSuffixArgs(), argPlaceholder)));
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new Insert1Handle(this, newType);
	}
}

