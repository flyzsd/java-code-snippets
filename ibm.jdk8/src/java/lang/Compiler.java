package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2016  All Rights Reserved
 */

/**
 * This class is a placeholder for environments which
 * explicitely manage the action of a "Just In Time"
 * compiler.
 *
 * @author		OTI
 * @version		initial
 *
 * @see			Cloneable
 */
public final class Compiler {

private Compiler() {}

/**
 * Low level interface to the JIT compiler. Can return
 * any object, or null if no JIT compiler is available.
 *
 * @author		OTI
 * @version		initial
 *
 * @return		Object
 *					result of executing command
 * @param		cmd Object
 *					a command for the JIT compiler
 */
public static Object command(Object cmd) {
	if (cmd == null) {
		throw new NullPointerException();
	}
	return commandImpl(cmd);
}

private static native Object commandImpl(Object cmd);

/**
 * Compiles the class using the JIT compiler. Answers
 * true if the compilation was successful, or false if
 * it failed or there was no JIT compiler available.
 *
 * @return		boolean
 *					indicating compilation success
 * @param		classToCompile java.lang.Class
 *					the class to JIT compile
 */
public static boolean compileClass(Class<?> classToCompile) {
	if (classToCompile == null) {
		throw new NullPointerException();
	}
	return compileClassImpl(classToCompile);
}

private static native boolean compileClassImpl(Class classToCompile);

/**
 * Compiles all classes whose name matches the argument
 * using the JIT compiler. Answers true if the compilation
 * was successful, or false if it failed or there was no
 * JIT compiler available.
 *
 * @return		boolean
 *					indicating compilation success
 * @param		nameRoot String
 *					the string to match against class names
 */
public static boolean compileClasses(String nameRoot) {
	if (nameRoot == null) {
		throw new NullPointerException();
	}
	return compileClassesImpl(nameRoot);
}

private static native boolean compileClassesImpl(String nameRoot);

/**
 * Disable the JIT compiler
 */
public static native void disable();

/**
 * Enable the JIT compiler
 */
public static native void enable();
}
