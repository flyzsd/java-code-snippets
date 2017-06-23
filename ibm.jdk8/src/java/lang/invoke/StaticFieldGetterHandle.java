/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/*
 * MethodHandle subclass that is able to return the value of
 * a static field.
 * <p>
 * vmSlot will hold the Unsafe field offset + low tag.
 *
 */
final class StaticFieldGetterHandle extends FieldHandle {

	StaticFieldGetterHandle(Class<?> referenceClass, String fieldName, Class<?> fieldClass, Class<?> accessClass) throws IllegalAccessException, NoSuchFieldException {
		super(fieldMethodType(fieldClass), referenceClass, fieldName, fieldClass, KIND_GETSTATICFIELD, accessClass);
	}

	StaticFieldGetterHandle(Field field) throws IllegalAccessException {
		super(fieldMethodType(field.getType()), field, KIND_GETSTATICFIELD, true);
	}

	StaticFieldGetterHandle(StaticFieldGetterHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/* Create the MethodType to be passed to the constructor */
	private final static MethodType fieldMethodType(Class<?> fieldClass) {
		return MethodType.methodType(fieldClass);
	}

	// {{{ JIT support
	@FrameIteratorSkip
	private final int    invokeExact_thunkArchetype_I(int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			return getUnsafe().getIntVolatile(defc, vmSlot);
		else
			return getUnsafe().getInt        (defc, vmSlot);
	}

	@FrameIteratorSkip
	private final long   invokeExact_thunkArchetype_J(int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			return getUnsafe().getLongVolatile(defc, vmSlot);
		else
			return getUnsafe().getLong        (defc, vmSlot);
	}

	@FrameIteratorSkip
	private final float  invokeExact_thunkArchetype_F(int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			return getUnsafe().getFloatVolatile(defc, vmSlot);
		else
			return getUnsafe().getFloat        (defc, vmSlot);
	}

	@FrameIteratorSkip
	private final double invokeExact_thunkArchetype_D(int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			return getUnsafe().getDoubleVolatile(defc, vmSlot);
		else
			return getUnsafe().getDouble        (defc, vmSlot);
	}

	@FrameIteratorSkip
	private final Object invokeExact_thunkArchetype_L(int argPlaceholder) {
		initializeClassIfRequired();
		if (Modifier.isVolatile(final_modifiers))
			return getUnsafe().getObjectVolatile(defc, vmSlot);
		else
			return getUnsafe().getObject        (defc, vmSlot);
	}

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new StaticFieldGetterHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof StaticFieldGetterHandle) {
			((StaticFieldGetterHandle)right).compareWithStaticFieldGetter(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithStaticFieldGetter(StaticFieldGetterHandle left, Comparator c) {
		compareWithField(left, c);
	}
}

