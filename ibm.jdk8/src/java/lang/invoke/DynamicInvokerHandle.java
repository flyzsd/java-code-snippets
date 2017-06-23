/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

@VMCONSTANTPOOL_CLASS
class DynamicInvokerHandle extends MethodHandle {
	@VMCONSTANTPOOL_FIELD
	final CallSite site;

	DynamicInvokerHandle(CallSite site) {
		super(site.type(), site.getClass(), "dynamicInvoker", MethodHandle.KIND_DYNAMICINVOKER, null); //$NON-NLS-1$
		this.site = site;
		this.vmSlot = 0;
	}

	DynamicInvokerHandle(DynamicInvokerHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.site = originalHandle.site;
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected ThunkTable thunkTable(){ return _thunkTable; }

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) throws Throwable {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(site.getTarget());
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}
		return ILGenMacros.invokeExact_X(site.getTarget(), argPlaceholder);
	}

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new DynamicInvokerHandle(this, newType);
	}

	// Final because any pair of invokers are equivalent if the point at the same site
	final void compareWith(MethodHandle right, Comparator c) {
		if (right instanceof DynamicInvokerHandle) {
			((DynamicInvokerHandle)right).compareWithDynamicInvoker(this, c);
		} else {
			c.fail();
		}
	}

	final void compareWithDynamicInvoker(DynamicInvokerHandle left, Comparator c) {
		c.compareStructuralParameter(left.site, this.site);
	}
}

