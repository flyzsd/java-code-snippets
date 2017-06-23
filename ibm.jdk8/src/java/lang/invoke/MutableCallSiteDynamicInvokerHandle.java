/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved
 */
package java.lang.invoke;

@VMCONSTANTPOOL_CLASS
final class MutableCallSiteDynamicInvokerHandle extends DynamicInvokerHandle {
	/* mutableSite and the parent's site fields will always be sync.  This is
	 * a redefinition as a MCS to enable the thunkArchetype to inline without
	 * a guard
	 */
	final MutableCallSite mutableSite;

	MutableCallSiteDynamicInvokerHandle(MutableCallSite site) {
		super(site);
		this.mutableSite = site;
	}

	MutableCallSiteDynamicInvokerHandle(MutableCallSiteDynamicInvokerHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.mutableSite = originalHandle.mutableSite;
	}

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new MutableCallSiteDynamicInvokerHandle(this, newType);
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	@FrameIteratorSkip
	private final int invokeExact_thunkArchetype_X(int argPlaceholder) throws Throwable {
		if (ILGenMacros.isShareableThunk()) {
			undoCustomizationLogic(mutableSite.getTarget());
		}
		if (!ILGenMacros.isCustomThunk()) {
			doCustomizationLogic();
		}

		// MutableCallSite.getTarget is final, so using mutableSite here allows
		// us to inline getTarget without a guard.
		//
		return ILGenMacros.invokeExact_X(mutableSite.getTarget(), argPlaceholder);
	}

	// }}} JIT support
}

