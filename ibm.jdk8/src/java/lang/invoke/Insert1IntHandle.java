/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

final class Insert1IntHandle extends InsertHandle {
	final int value;
	final MethodHandle nextNoUnbox;  // Next handle without the astype from Object for the inserted value

	Insert1IntHandle(MethodType type, MethodHandle next, int insertionIndex, Object values[], MethodHandle nextNoUnbox) {
		super(type, next, insertionIndex, values);
		/* Value must unbox and widen to int.  Only byte, short, char, or int are possible */
		if (values[0] instanceof Character) {
			this.value = ((Character)values[0]).charValue();
		} else {
			this.value = ((Number)values[0]).intValue();
		}
		this.nextNoUnbox = nextNoUnbox;
	}

	Insert1IntHandle(Insert1IntHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.value = originalHandle.value;
		this.nextNoUnbox = originalHandle.nextNoUnbox;
	}

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new Insert1IntHandle(this, newType);
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(nextNoUnbox);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		return ILGenMacros.invokeExact_X(nextNoUnbox, ILGenMacros.placeholder(
			ILGenMacros.firstN(numPrefixArgs(), argPlaceholder),
			value,
			ILGenMacros.lastN(numSuffixArgs(), argPlaceholder)));
	}

	// }}} JIT support
}

