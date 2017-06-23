package java.lang;

/*******************************************************************************
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 *
 *     Copyright IBM Corp. 1998, 2016 All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/

/**
 * RuntimePermission objects represent access to runtime
 * support.
 *
 * @author		OTI
 * @version		initial
 */
public final class RuntimePermission extends java.security.BasicPermission {
	private static final long serialVersionUID = 7399184964622342223L;

	/**
	 * Constants for runtime permissions used in this package.
	 */
	static final RuntimePermission permissionToSetSecurityManager =
		new RuntimePermission("setSecurityManager"); //$NON-NLS-1$
	static final RuntimePermission permissionToCreateSecurityManager =
		new RuntimePermission("createSecurityManager"); //$NON-NLS-1$
	static final RuntimePermission permissionToGetProtectionDomain =
		new RuntimePermission("getProtectionDomain"); //$NON-NLS-1$
	static final RuntimePermission permissionToGetClassLoader =
		new RuntimePermission("getClassLoader"); //$NON-NLS-1$
	static final RuntimePermission permissionToCreateClassLoader =
		new RuntimePermission("createClassLoader"); //$NON-NLS-1$
	static final RuntimePermission permissionToModifyThread =
		new RuntimePermission("modifyThread"); //$NON-NLS-1$
	static final RuntimePermission permissionToModifyThreadGroup =
		new RuntimePermission("modifyThreadGroup"); //$NON-NLS-1$
	static final RuntimePermission permissionToExitVM =
		new RuntimePermission("exitVM"); //$NON-NLS-1$
	static final RuntimePermission permissionToReadFileDescriptor =
		new RuntimePermission("readFileDescriptor"); //$NON-NLS-1$
	static final RuntimePermission permissionToWriteFileDescriptor =
		new RuntimePermission("writeFileDescriptor"); //$NON-NLS-1$
	static final RuntimePermission permissionToQueuePrintJob =
		new RuntimePermission("queuePrintJob"); //$NON-NLS-1$
	static final RuntimePermission permissionToSetFactory =
		new RuntimePermission("setFactory"); //$NON-NLS-1$
	static final RuntimePermission permissionToSetIO =
		new RuntimePermission("setIO"); //$NON-NLS-1$
	static final RuntimePermission permissionToStopThread =
		new RuntimePermission("stopThread"); //$NON-NLS-1$
	static final RuntimePermission permissionToSetContextClassLoader =
		new RuntimePermission("setContextClassLoader"); //$NON-NLS-1$
	static final RuntimePermission permissionToAccessDeclaredMembers =
			new RuntimePermission("accessDeclaredMembers"); //$NON-NLS-1$

/**
 * Creates an instance of this class with the given name.
 *
 * @author		OTI
 * @version		initial
 *
 * @param		permissionName String
 *					the name of the new permission.
 */
public RuntimePermission(java.lang.String permissionName)
{
	super(permissionName);
}

/**
 * Creates an instance of this class with the given name and
 * action list. The action list is ignored.
 *
 * @author		OTI
 * @version		initial
 *
 * @param		name String
 *					the name of the new permission.
 * @param		actions String
 *					ignored.
 */
public RuntimePermission(java.lang.String name, java.lang.String actions)
{
	super(name, actions);
}

}
