/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

final class PermuteHandle extends MethodHandle {
	@VMCONSTANTPOOL_FIELD
	private final MethodHandle next;

	@VMCONSTANTPOOL_FIELD
	private final int[] permute;

	PermuteHandle(MethodType type, MethodHandle next, int[] permute) {
		super(type, next.definingClass, "PermuteHandle", KIND_PERMUTE, permute); //$NON-NLS-1$
 		this.next    = next;
 		this.permute = permute;
	}

	PermuteHandle(PermuteHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.next = originalHandle.next;
		this.permute = originalHandle.permute;
	}

	/*
	 * Create a combined permute.  This removes a MH from the chain when
	 * we have a permute(permute(handle, ...) ...).
	 */
	@Override
	MethodHandle permuteArguments(MethodType permuteType, int... permute2) {
		int[] combinedPermute = new int[permute.length];
		for (int i = 0; i < permute.length; i++) {
			combinedPermute[i] = permute2[permute[i]];
		}
		return new PermuteHandle(permuteType, next, combinedPermute);
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	protected final ThunkTuple computeThunks(Object permutationArg) {
		// Jitted code depends on the permutation, so we can't share handles with different permutations.
		int[] permutation = (int[]) permutationArg;
		return thunkTable().get(new ThunkKeyWithIntArray(ThunkKey.computeThunkableType(type()), permutation));
	}

	private static native int permuteArgs(int argPlaceholder);

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(next);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		return ILGenMacros.invokeExact_X(next, permuteArgs(argPlaceholder));
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new PermuteHandle(this, newType);
	}

 	final void compareWith(MethodHandle right, Comparator c) {
 		if (right instanceof PermuteHandle) {
 			((PermuteHandle)right).compareWithPermute(this, c);
 		} else {
 			c.fail();
 		}
 	}

 	final void compareWithPermute(PermuteHandle left, Comparator c) {
 		c.compareStructuralParameter(left.permute.length, this.permute.length);
 		for (int i = 0; (i < left.permute.length) && (i < this.permute.length); i++) {
 			c.compareStructuralParameter(left.permute[i], this.permute[i]);
 		}
 		c.compareChildHandle(left.next, this.next);
 	}
}

