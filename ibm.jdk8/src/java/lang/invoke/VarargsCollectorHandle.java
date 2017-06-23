/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2016  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandles.Lookup;

import com.ibm.oti.util.Msg;

/*
 * VarargsCollectorHandle is a MethodHandle subclass used to implement
 * MethodHandle.asVarargsCollector(Class<?> arrayType)
 * <p>
 * The vmSlot will hold 0 as there is no actual method for it.
 *
 * Type of VarargsCollectorHandle and its 'next' handle will always match
 */
final class VarargsCollectorHandle extends MethodHandle {
	final MethodHandle next;
	final Class<?> arrayType;

	VarargsCollectorHandle(MethodHandle next, Class<?> arrayType) {
		super(varargsCollectorType(next.type, arrayType), next.definingClass, null, KIND_VARARGSCOLLECT, next.rawModifiers & ~MethodHandles.Lookup.VARARGS, null);
		this.next = next;
		if (arrayType == null) {
			throw new IllegalArgumentException();
		}
		this.arrayType = arrayType;
		this.defc = next.defc;
	}

	VarargsCollectorHandle(VarargsCollectorHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.next = originalHandle.next;
		this.arrayType = originalHandle.arrayType;
	}

	static MethodType varargsCollectorType(MethodType nextType, Class<?> arrayType) {
		return nextType.changeParameterType(nextType.parameterCount() - 1, arrayType);
	}

	public Object invokeWithArguments(Object... args) throws Throwable, WrongMethodTypeException, ClassCastException {
		if (args.length < 253) {
			MethodHandle mh = IWAContainer.getMH(args.length);
			return mh.invokeExact((MethodHandle)this, args);
		}
		MethodHandle mh = this.asType(MethodType.genericMethodType(args.length));
		mh = mh.asSpreader(Object[].class, args.length);
		return mh.invokeExact(args);
	}

	private CollectHandle previousCollector = null;

	@Override
	public MethodHandle asType(MethodType newType) throws ClassCastException {
		if (type == newType)  {
			return this;
		}
		int parameterCount = type.parameterCount();
		int newTypeParameterCount = newType.parameterCount();
		if (parameterCount == newTypeParameterCount) {
			if (type.lastParameterType().isAssignableFrom(newType.lastParameterType())) {
				return next.asType(newType);
			}
		}
		int collectCount = newTypeParameterCount - parameterCount + 1;
		if (collectCount < 0) {
			throw new WrongMethodTypeException();
		}
		CollectHandle collector = previousCollector;
		if ((collector == null) || (collector.collectArraySize != collectCount)) {
			collector = (CollectHandle) next.asCollector(arrayType, collectCount);
			// update cached collector handle
			previousCollector = collector;
		}
		return collector.asType(newType);
	}

	@Override
	public MethodHandle	asVarargsCollector(Class<?> arrayParameter) throws IllegalArgumentException {
		if (arrayType == arrayParameter) {
			return this;
		}
		if (!arrayType.isAssignableFrom(arrayParameter)) {
			// K05cc = Cannot assign '{0}' to methodtype '{1}'
			throw new IllegalArgumentException(Msg.getString("K05cc", arrayParameter, type)); //$NON-NLS-1$
		}
		return next.asVarargsCollector(arrayParameter);
	}

	@Override
	public MethodHandle asFixedArity() {
		MethodHandle fixedArity = next;
		while (fixedArity.isVarargsCollector()) {
			// cover varargsCollector on a varargsCollector
			fixedArity = ((VarargsCollectorHandle)fixedArity).next;
		}
		// asType will return 'this' if type is the same
		return fixedArity.asType(type());
	}

	@Override
	boolean canRevealDirect() {
		// Check that the underlying member is a varargs method
		if ((next.kind >= MethodHandleInfo.REF_invokeVirtual) && ((next.rawModifiers & Lookup.VARARGS) != 0)) {
			return next.canRevealDirect();
		}
		return false;
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) throws Throwable {
		return ILGenMacros.invokeExact_X(next, argPlaceholder);
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new VarargsCollectorHandle(this, newType);
	}

	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof VarargsCollectorHandle) {
			((VarargsCollectorHandle)right).compareWithVarargsCollector(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithVarargsCollector(VarargsCollectorHandle left, Comparator c) {
		c.compareStructuralParameter(left.arrayType, this.arrayType);
		c.compareChildHandle(left.next, this.next);
	}
}

