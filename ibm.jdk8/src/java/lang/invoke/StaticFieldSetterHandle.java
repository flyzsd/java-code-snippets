/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/*
 * MethodHandle subclass that is able to set the value of
 * a static field.
 * <p>
 * vmSlot will hold the Unsafe field offset  + low tag.
 *
 */
final class StaticFieldSetterHandle extends FieldHandle {

	StaticFieldSetterHandle(Class<?> referenceClass, String fieldName, Class<?> fieldClass, Class<?> accessClass) throws IllegalAccessException, NoSuchFieldException {
		super(fieldMethodType(fieldClass), referenceClass, fieldName, fieldClass, KIND_PUTSTATICFIELD, accessClass);
	}

	StaticFieldSetterHandle(Field field) throws IllegalAccessException {
		super(fieldMethodType(field.getType()), field, KIND_PUTSTATICFIELD, true);
	}

	StaticFieldSetterHandle(StaticFieldSetterHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/* Create the MethodType to be passed to the constructor */
	private final static MethodType fieldMethodType(Class<?> fieldClass) {
		return MethodType.methodType(void.class, fieldClass);
	}

	// {{{ JIT support
	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(int    newValue, int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putIntVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putInt        (defc, vmSlot, newValue);
	}

	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(long   newValue, int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putLongVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putLong        (defc, vmSlot, newValue);
	}

	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(float  newValue, int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putFloatVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putFloat        (defc, vmSlot, newValue);
	}

	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(double newValue, int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putDoubleVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putDouble        (defc, vmSlot, newValue);
	}

	@FrameIteratorSkip
	private final void invokeExact_thunkArchetype_V(Object newValue, int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			getUnsafe().putObjectVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putObject        (defc, vmSlot, newValue);
	}

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new StaticFieldSetterHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof StaticFieldSetterHandle) {
			((StaticFieldSetterHandle)right).compareWithStaticFieldSetter(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithStaticFieldSetter(StaticFieldSetterHandle left, Comparator c) {
		compareWithField(left, c);
	}
}

