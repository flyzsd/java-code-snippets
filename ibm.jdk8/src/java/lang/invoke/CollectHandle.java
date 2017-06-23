/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

import com.ibm.oti.util.Msg;

/* CollectHandle is a MethodHandle subclass used to call another MethodHandle.
 * It accepts the incoming arguments and collects the requested number
 * of them into an array of type 'T'.
 * <p>
 * The vmSlot will hold 0 as there is no actual method for it.
 * <p>
 * Return types can NOT be adapted by this handle.
 * <p>
 * Can't pre-allocate the collect array as its not thread-safe - same handle
 * can be used in multiple threads or collected args array can be modified
 * down the call chain.
 */
final class CollectHandle extends MethodHandle {
	@VMCONSTANTPOOL_FIELD
	final MethodHandle next;
	@VMCONSTANTPOOL_FIELD
	final int collectArraySize; /* Size of the collect array */

	CollectHandle(MethodHandle next, int collectArraySize) {
		super(collectMethodType(next.type(), collectArraySize), null, null, KIND_COLLECT, next.rawModifiers, new Integer(collectArraySize));
		this.collectArraySize = collectArraySize;
		this.next = next;
		this.vmSlot = 0;
	}

	CollectHandle(CollectHandle original, MethodType newType) {
		super(original, newType);
		this.collectArraySize = original.collectArraySize;
		this.next = original.next;
		this.vmSlot = original.vmSlot;
	}

	private static final MethodType collectMethodType(MethodType type, int collectArraySize) {
		int parameterCount = type.parameterCount();
		if (parameterCount == 0) {
			// K05ca = last argument of MethodType must be an array class
			throw new IllegalArgumentException(Msg.getString("K05ca")); //$NON-NLS-1$
		}
		// Ensure the last class is an array
		Class<?> arrayComponent = type.lastParameterType().getComponentType();
		if (arrayComponent == null) {
			// K05ca = last argument of MethodType must be an array class
			throw new IllegalArgumentException(Msg.getString("K05ca")); //$NON-NLS-1$
		}
		// Change the T[] into a 'T'
		MethodType newType = type.changeParameterType(parameterCount - 1 , arrayComponent);

		// Add necessary additional 'T' to the type
		if (collectArraySize == 0) {
			newType = newType.dropParameterTypes(parameterCount - 1 , parameterCount);
		} else if (collectArraySize > 1){
			Class<?>[] classes = new Class[collectArraySize - 1];
			for (int j = 0; j < classes.length; j++) {
				classes[j] = arrayComponent;
			}
			newType = newType.insertParameterTypes(newType.parameterCount(), classes);
		}
		return newType;
	}

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new CollectHandle(this, newType);
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }
	protected final ThunkTuple computeThunks(Object arg) {
		int collectArraySize = (Integer)arg;
		return thunkTable().get(new ThunkKeyWithInt(ThunkKey.computeThunkableType(type()), collectArraySize));
	}

	private final Object allocateArray() {
		return java.lang.reflect.Array.newInstance(
			next.type().lastParameterType().getComponentType(),
			collectArraySize);
	}

	private static native int numArgsToPassThrough();
	private static native int numArgsToCollect();

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) throws Throwable {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(next);
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		ILGenMacros.populateArray(
			ILGenMacros.push(allocateArray()),
			ILGenMacros.lastN(numArgsToCollect(), argPlaceholder));
		return ILGenMacros.invokeExact_X(
			next,
			ILGenMacros.placeholder(
				ILGenMacros.firstN(numArgsToPassThrough(), argPlaceholder),
				ILGenMacros.pop_L()));
	}

	// }}} JIT support

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof CollectHandle) {
			((CollectHandle)right).compareWithCollect(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithCollect(CollectHandle left, Comparator c) {
		c.compareStructuralParameter(left.collectArraySize, this.collectArraySize);
		c.compareChildHandle(left.next, this.next);
	}
}

