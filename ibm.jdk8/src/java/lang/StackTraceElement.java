package java.lang;

import java.security.ProtectionDomain;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2002, 2016  All Rights Reserved
 */

/**
 * StackTraceElement represents a stack frame.
 *
 * @see Throwable#getStackTrace()
 */
public final class StackTraceElement implements java.io.Serializable {
	private static final long serialVersionUID = 6992337162326171013L;
	String declaringClass, methodName, fileName;
	int lineNumber;
	transient Object source;

/**
 * Create a StackTraceElement from the parameters.
 *
 * @param cls The class name
 * @param method The method name
 * @param file The file name
 * @param line The line number
 */
public StackTraceElement(String cls, String method, String file, int line) {
	if (cls == null || method == null) throw new NullPointerException();
	declaringClass = cls;
	methodName = method;
	fileName = file;
	lineNumber = line;
}

@SuppressWarnings("unused")
private StackTraceElement() {} // prevent instantiation from java code - only the VM creates these

/**
 * Returns true if the specified object is another StackTraceElement instance
 * representing the same execution point as this instance.
 *
 * @param obj the object to compare to
 *
 */
public boolean equals(Object obj) {
	if (!(obj instanceof StackTraceElement)) return false;
	StackTraceElement castObj = (StackTraceElement) obj;

	// Unknown methods are never equal to anything (not strictly to spec, but spec does not allow null method/class names)
	if ((methodName == null) || (castObj.methodName == null)) return false;

	if (!getMethodName().equals(castObj.getMethodName())) return false;
	if (!getClassName().equals(castObj.getClassName())) return false;
	String localFileName = getFileName();
	if (localFileName == null) {
		if (castObj.getFileName() != null) return false;
	} else {
		if (!localFileName.equals(castObj.getFileName())) return false;
	}
	if (getLineNumber() != castObj.getLineNumber()) return false;

	return true;
}

/**
 * Returns the full name (i.e. including package) of the class where this
 * stack trace element is executing.
 *
 * @return the name of the class where this stack trace element is
 *         executing.
 */
public String getClassName() {
	return (declaringClass == null) ? "<unknown class>" : declaringClass; //$NON-NLS-1$
}

/**
 * If available, returns the name of the file containing the Java code
 * source which was compiled into the class where this stack trace element
 * is executing.
 *
 * @return the name of the Java code source file which was compiled into the
 *         class where this stack trace element is executing. If not
 *         available, a <code>null</code> value is returned.
 */
public String getFileName() {
	return fileName;
}

/**
 * Returns the source file line number for the class where this stack trace
 * element is executing.
 *
 * @return the line number in the source file corresponding to where this
 *         stack trace element is executing.
 */
public int getLineNumber() {
	return lineNumber;
}

/**
 * Returns the name of the method where this stack trace element is
 * executing.
 *
 * @return the method in which this stack trace element is executing.
 *         Returns &lt;<code>unknown method</code>&gt; if the name of the
 *         method cannot be determined.
 */
public String getMethodName() {
	return (methodName == null) ? "<unknown method>" : methodName; //$NON-NLS-1$
}

/**
 * Returns a hash code value for this stack trace element.
 */
public int hashCode() {
	// either both methodName and declaringClass are null, or neither are null
	if (methodName == null) return 0;	// all unknown methods hash the same
	return methodName.hashCode() ^ declaringClass.hashCode();	// declaringClass never null if methodName is non-null
}

/**
 * Returns <code>true</code> if the method name returned by
 * {@link #getMethodName()} is implemented as a native method.
 *
 * @return true if the method is a native method
 */
public boolean isNativeMethod() {
	return lineNumber == -2;
}

/**
 * Returns a string representation of this stack trace element.
 */
public String toString() {
	StringBuilder buf = new StringBuilder(80);

	appendTo(buf);
	return buf.toString();
}

/**
 * Helper method for toString and for Throwable.print output with PrintStream and PrintWriter
 */
void appendTo(Appendable buf) {
	appendTo(buf, getClassName());
	appendTo(buf, "."); //$NON-NLS-1$
	appendTo(buf, getMethodName());

	if (isNativeMethod()) {
		appendTo(buf, "(Native Method)"); //$NON-NLS-1$
	} else {
		String fileName = getFileName();

		if (fileName == null) {
			appendTo(buf, "(Unknown Source)"); //$NON-NLS-1$
		} else {
			int lineNumber = getLineNumber();

			appendTo(buf, "("); //$NON-NLS-1$
			appendTo(buf, fileName);
			if (lineNumber >= 0) {
				appendTo(buf, ":"); //$NON-NLS-1$
				appendTo(buf, lineNumber);
			}
			appendTo(buf, ")"); //$NON-NLS-1$
		}
	}

	/* Support for -verbose:stacktrace */
	if (source != null) {
		appendTo(buf, " from "); //$NON-NLS-1$
		if (source instanceof String) {
			appendTo(buf, (String)source);
		} else if (source instanceof ProtectionDomain) {
			ProtectionDomain pd = (ProtectionDomain)source;
			appendTo(buf, pd.getClassLoader().toString());
			appendTo(buf, "(");			 //$NON-NLS-1$
			appendTo(buf, pd.getCodeSource().getLocation().toString());
			appendTo(buf, ")");			 //$NON-NLS-1$
		}
	}
}

/*
 * CMVC 97756 provide a way of printing this stack trace element without
 *            allocating memory, in particular without String concatenation.
 *            Used when printing a stack trace for an OutOfMemoryError.
 */

/**
 * Helper method for output with PrintStream and PrintWriter
 */
static void appendTo(Appendable buf, CharSequence s) {
	try {
		buf.append(s);
	} catch (java.io.IOException e) {
	}
}
@SuppressWarnings("all")
private static final String digits[]={"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
/**
 * Helper method for output with PrintStream and PrintWriter
 */
static void appendTo(Appendable buf, int number) {
	int i;
	int j = 1;
	for (i = number; i >= 10; i /= 10) {
		j *= 10;
	}
	appendTo(buf, digits[i]);
	while (j >= 10) {
		number -= j * i;
		j /= 10;
		i = number / j;
		appendTo(buf, digits[i]);
	}
}
}
