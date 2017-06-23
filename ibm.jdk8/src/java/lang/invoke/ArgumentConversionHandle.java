/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

abstract class ArgumentConversionHandle extends ConvertHandle {

	ArgumentConversionHandle(MethodHandle handle, MethodType type, Class<?> definingClass, String name, int kind) {
		super(handle, type, definingClass, name, kind, handle.type());
		if (type == null) {
			throw new IllegalArgumentException();
		}
		checkConversion(handle.type, type);
		this.vmSlot = 0;
	}

	// {{{ JIT support
	protected final ThunkTuple computeThunks(Object nextHandleType) {
		// To get the type casts right, we must inspect the full signatures of
		// both the receiver and nextHandleType, but we can upcast the return
		// type becuse ArgumentConversionHandles are not used to filter return types.
		MethodType thunkableReceiverType = ThunkKey.computeThunkableType(type(), 0);
		MethodType thunkableNextType     = ThunkKey.computeThunkableType((MethodType)nextHandleType, 0);
		return thunkTable().get(new ThunkKeyWithObject(thunkableReceiverType, thunkableNextType));
	}
	// }}} JIT support
}

