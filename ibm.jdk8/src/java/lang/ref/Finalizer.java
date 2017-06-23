package java.lang.ref;

/*******************************************************************************
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 *
 *     Copyright IBM Corp. 2002, 2016 All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/

final class Finalizer {

// called by java.lang.Runtime.runFinalization0
static void runFinalization() {
	runFinalizationImpl();
}

private static native void runFinalizationImpl();

// called by java.lang.Shutdown.runAllFinalizers native
// invoked when Runtime.runFinalizersOnExit() was called with true
static void runAllFinalizers() {
	runAllFinalizersImpl();
}

private static native void runAllFinalizersImpl();
}
