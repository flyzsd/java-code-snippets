/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.reflect.Constructor;

/* ConstructorHandle is a MethodHandle subclass used to call <init> methods.  This
 * class is similar to DirectHandle in that the method to call has already
 * been resolved down to an exact method address.
 * <p>
 * The constructor must be called with the type of the <init> method. This means
 * it must have a void return type.
 * <p>
 * This is the equivalent of calling newInstance except with a known constructor.
 * <p>
 * The vmSlot will hold a J9Method address of the <init> method.
 */
final class ConstructorHandle extends MethodHandle {

	static {
		//Making sure DirectHandle is loaded before the ConstructorHandle is loaded. Therefore, to secure a correct thunk.
		DirectHandle.load();
	}

	public ConstructorHandle(Class<?> definingClass, MethodType type) throws NoSuchMethodException, IllegalAccessException {
		super(type, definingClass, "<init>", KIND_CONSTRUCTOR, null); //$NON-NLS-1$
		/* Pass definingClass as SpecialToken as KIND_SPECIAL & KIND_CONSTRUCTOR share lookup code */
		this.defc = finishMethodInitialization(definingClass, type);
	}

	public ConstructorHandle(Constructor<?> ctor) throws IllegalAccessException {
		super(MethodType.methodType(ctor.getDeclaringClass(), ctor.getParameterTypes()), ctor.getDeclaringClass(), "<init>", KIND_CONSTRUCTOR, ctor.getModifiers(), ctor.getDeclaringClass()); //$NON-NLS-1$

		boolean succeed = setVMSlotAndRawModifiersFromConstructor(this, ctor);
		if (!succeed) {
			throw new IllegalAccessException();
		}
	}

	ConstructorHandle(ConstructorHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/*
	 * Constructors have type (args of passed in type)definingClass.
	 */
	MethodType computeHandleType(MethodType type) {
		return type.changeReturnType(definingClass);
	}

	@Override
	boolean canRevealDirect() {
		return true;
	}

	// {{{ JIT support
	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	@FrameIteratorSkip
	private final Object invokeExact_thunkArchetype_L(int argPlaceholder) {
		if (ILGenMacros.isCustomThunk()) {
			DirectHandle.directCall_V(ILGenMacros.push(ILGenMacros.rawNew(definingClass)), argPlaceholder);
		} else if (DirectHandle.isAlreadyCompiled(vmSlot)) {
			ComputedCalls.dispatchDirect_V(DirectHandle.compiledEntryPoint(vmSlot), ILGenMacros.push(ILGenMacros.rawNew(definingClass)), argPlaceholder);
		} else {
			// Calling rawNew on definingClass will cause <clinit> to be called, so we don't need an explicit call to initializeClassIfRequired.
			ComputedCalls.dispatchJ9Method_V(vmSlot, ILGenMacros.push(ILGenMacros.rawNew(definingClass)), argPlaceholder);
		}
		return ILGenMacros.pop_L();
	}
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new ConstructorHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof ConstructorHandle) {
			((ConstructorHandle)right).compareWithConstructor(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithConstructor(ConstructorHandle left, Comparator c) {
		c.compareStructuralParameter(left.definingClass, this.definingClass);
	}
}

