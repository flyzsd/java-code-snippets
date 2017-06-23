/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */

package java.lang.invoke;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

abstract class FieldHandle extends MethodHandle {
	final Class<?> fieldClass;
	final int final_modifiers;

	FieldHandle(MethodType type, Class<?> referenceClass, String fieldName, Class<?> fieldClass, int kind, Class<?> accessClass) throws IllegalAccessException, NoSuchFieldException {
		super(type, referenceClass, fieldName, kind, null);
		this.fieldClass = fieldClass;
		/* modifiers is set inside the native */
		this.defc = finishFieldInitialization(accessClass);
		final_modifiers = rawModifiers;
		assert(isVMSlotCorrectlyTagged());
	}

	FieldHandle(MethodType type, Field field, int kind, boolean isStatic) throws IllegalAccessException {
		super(type, field.getDeclaringClass(), field.getName(), kind, field.getModifiers(), null);
		this.fieldClass = field.getType();
		assert(isStatic == Modifier.isStatic(field.getModifiers()));

		boolean succeed = setVMSlotAndRawModifiersFromField(this, field);
		if (!succeed) {
			throw new IllegalAccessException();
		}
		final_modifiers = rawModifiers;
		assert(isVMSlotCorrectlyTagged());
	}

	FieldHandle(FieldHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.fieldClass = originalHandle.fieldClass;
		final_modifiers = rawModifiers;
		assert(isVMSlotCorrectlyTagged());
	}

	final Class<?> finishFieldInitialization(Class<?> accessClass) throws IllegalAccessException, NoSuchFieldException {
		String signature = MethodType.getBytecodeStringName(fieldClass);
		try {
			return lookupField(definingClass, name, signature, kind, accessClass);
		} catch (NoSuchFieldError e) {
			throw new NoSuchFieldException(e.getMessage());
		} catch (LinkageError e) {
			throw (IllegalAccessException) new IllegalAccessException(e.getMessage()).initCause(e);
		}
	}

	/* Ensure the vmSlot is low tagged if static */
	boolean isVMSlotCorrectlyTagged() {
		if ((KIND_PUTSTATICFIELD == this.kind) || (KIND_GETSTATICFIELD == this.kind)) {
			return (vmSlot & 1) == 1;
		}
		return (vmSlot & 1) == 0;
	}

	@Override
	boolean canRevealDirect() {
		return true;
	}

	final void compareWithField(FieldHandle left, Comparator c) {
		c.compareStructuralParameter(left.definingClass, this.definingClass);
		c.compareStructuralParameter(left.vmSlot, this.vmSlot);
	}

}

