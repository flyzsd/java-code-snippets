/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

final class GuardWithTestHandle extends MethodHandle {

	final MethodHandle guard;
	final MethodHandle trueTarget;
	final MethodHandle falseTarget;

	protected GuardWithTestHandle(MethodHandle guard, MethodHandle trueTarget, MethodHandle falseTarget) {
		super(trueTarget.type(), GuardWithTestHandle.class, "GuardWithTestHandle", KIND_GUARDWITHTEST, 0, guard.type());  //$NON-NLS-1$
		this.guard       = guard;
		this.trueTarget  = trueTarget;
		this.falseTarget = falseTarget;
	}

	GuardWithTestHandle(GuardWithTestHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.guard = originalHandle.guard;
		this.trueTarget = originalHandle.trueTarget;
		this.falseTarget = originalHandle.falseTarget;
	}

	public static GuardWithTestHandle get(MethodHandle guard, MethodHandle trueTarget, MethodHandle falseTarget) {
		return new GuardWithTestHandle(guard, trueTarget, falseTarget);
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

 	protected final ThunkTuple computeThunks(Object guardType) {
 		// Different thunks accomodate guards with different numbers of parameters
 		return thunkTable().get(new ThunkKeyWithObject(ThunkKey.computeThunkableType(type()), ThunkKey.computeThunkableType((MethodType)guardType)));
 	}

 	private static native int numGuardArgs();

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(guard, trueTarget, falseTarget);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		if (ILGenMacros.invokeExact_Z(guard, ILGenMacros.firstN(numGuardArgs(), argPlaceholder))) {
			return ILGenMacros.invokeExact_X(trueTarget, argPlaceholder);
		} else {
			return ILGenMacros.invokeExact_X(falseTarget, argPlaceholder);
		}
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new GuardWithTestHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof GuardWithTestHandle) {
			((GuardWithTestHandle)right).compareWithGuardWithTest(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithGuardWithTest(GuardWithTestHandle left, Comparator c) {
		c.compareChildHandle(left.guard, this.guard);
		c.compareChildHandle(left.trueTarget, this.trueTarget);
		c.compareChildHandle(left.falseTarget, this.falseTarget);
	}
}

