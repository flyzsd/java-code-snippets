/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

import com.ibm.oti.util.Msg;

final class SpreadHandle extends MethodHandle {
	@VMCONSTANTPOOL_FIELD
 	private final MethodHandle next;
	@VMCONSTANTPOOL_FIELD
	private final Class<?> arrayClass;
	@VMCONSTANTPOOL_FIELD
	private final int spreadCount;

	protected SpreadHandle(MethodHandle next, MethodType collectType, Class<?> arrayClass, int spreadCount) {
		super(collectType, next.definingClass, null, KIND_SPREAD, infoAffectingThunks(arrayClass, spreadCount));
		this.arrayClass = arrayClass;
		this.spreadCount = spreadCount;
		this.next = next;
	}

	SpreadHandle(SpreadHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.next = originalHandle.next;
		this.arrayClass = originalHandle.arrayClass;
		this.spreadCount = originalHandle.spreadCount;
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	private static Object[] infoAffectingThunks(Class<?> arrayClass, int spreadCount) {
		// The number and types of values to spread affect jitted thunks
		Object[] result = { arrayClass, spreadCount };
		return result;
	}

	protected final ThunkTuple computeThunks(Object info) {
		return thunkTable().get(new ThunkKeyWithObjectArray(ThunkKey.computeThunkableType(type()), (Object[])info));
	}

	private static native int numArgsToPassThrough();
	private static native int numArgsToSpread();
	private static native Object arrayArg(int argPlaceholder);

	// Can't expand placeholders in calls to virtual methods, so here's a static method that calls Class.cast.
	private static void checkCast(Class<?> c, Object q) {
		c.cast(q);
	}

	private static void checkArray(Object spreadArg, int spreadCount) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
		if (spreadArg == null) {
			if (spreadCount != 0) {
				// K05d1 = cannot have null spread argument unless spreadCount is 0
				throw new IllegalArgumentException(Msg.getString("K05d1")); //$NON-NLS-1$
			}
		} else if (spreadCount != java.lang.reflect.Array.getLength(spreadArg)) {
			// K05d2 = expected '{0}' sized array; encountered '{1}' sized array
			throw new IllegalArgumentException(Msg.getString("K05d2", spreadCount, java.lang.reflect.Array.getLength(spreadArg))); //$NON-NLS-1$
		}
	}

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(next);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		checkCast(arrayClass, arrayArg(argPlaceholder));
		checkArray(arrayArg(argPlaceholder), spreadCount);
		return ILGenMacros.invokeExact_X(next, ILGenMacros.placeholder(
			ILGenMacros.firstN(numArgsToPassThrough(), argPlaceholder),
			ILGenMacros.arrayElements(arrayArg(argPlaceholder), 0, numArgsToSpread())));
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new SpreadHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof SpreadHandle) {
			((SpreadHandle)right).compareWithSpread(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithSpread(SpreadHandle left, Comparator c) {
		c.compareStructuralParameter(left.arrayClass, this.arrayClass);
		c.compareStructuralParameter(left.spreadCount, this.spreadCount);
		c.compareChildHandle(left.next, this.next);
	}

}

