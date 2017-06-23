/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/*
 * MethodHandle subclass that is able to set the value of
 * an instance field.
 * <p>
 * vmSlot will hold the field offset in the instance.
 *
 */
final class FieldSetterHandle extends FieldHandle {

	FieldSetterHandle(Class<?> referenceClass, String fieldName, Class<?> fieldClass, Class<?> accessClass) throws IllegalAccessException, NoSuchFieldException {
		super(fieldMethodType(referenceClass, fieldClass), referenceClass, fieldName, fieldClass, KIND_PUTFIELD, accessClass);
	}

	FieldSetterHandle(Field field) throws IllegalAccessException {
		super(fieldMethodType(field.getDeclaringClass(), field.getType()), field, KIND_PUTFIELD, false);
	}

	FieldSetterHandle(FieldSetterHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/*
	 * Create the MethodType to be passed to the constructor
	 * MethodType of a field setter is (instanceType, fieldType)V.
	 */
	private final static MethodType fieldMethodType(Class<?> definingClass, Class<?> fieldClass) {
		return MethodType.methodType(void.class, definingClass, fieldClass);
	}

	// {{{ JIT support
	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(Object receiver, int    newValue, int argPlaceholder) {
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putIntVolatile(receiver, vmSlot+HEADER_SIZE, newValue);
		else
			getUnsafe().putInt        (receiver, vmSlot+HEADER_SIZE, newValue);
	}

	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(Object receiver, long   newValue, int argPlaceholder) {
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putLongVolatile(receiver, vmSlot+HEADER_SIZE, newValue);
		else
			getUnsafe().putLong        (receiver, vmSlot+HEADER_SIZE, newValue);
	}

	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(Object receiver, float  newValue, int argPlaceholder) {
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putFloatVolatile(receiver, vmSlot+HEADER_SIZE, newValue);
		else
			getUnsafe().putFloat        (receiver, vmSlot+HEADER_SIZE, newValue);
	}

	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(Object receiver, double newValue, int argPlaceholder) {
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putDoubleVolatile(receiver, vmSlot+HEADER_SIZE, newValue);
		else
			getUnsafe().putDouble        (receiver, vmSlot+HEADER_SIZE, newValue);
	}

	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(Object receiver, Object newValue, int argPlaceholder) {
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putObjectVolatile(receiver, vmSlot+HEADER_SIZE, newValue);
		else
			getUnsafe().putObject        (receiver, vmSlot+HEADER_SIZE, newValue);
	}

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new FieldSetterHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof FieldSetterHandle) {
			((FieldSetterHandle)right).compareWithFieldSetter(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithFieldSetter(FieldSetterHandle left, Comparator c) {
		compareWithField(left, c);
	}
}

