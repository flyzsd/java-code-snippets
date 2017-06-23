/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2005, 2016  All Rights Reserved.
 */

package java.lang.management;

import java.security.BasicPermission;

/**
 * This is the security permission that code running with a Java security
 * manager will be verified against when attempts are made to invoke methods in
 * the platform's management interface.
 * <p>
 * Instances of this type are normally created by security code.
 * </p>
 *
 * @since 1.5
 */
public final class ManagementPermission extends BasicPermission {

    /**
     * Helps to determine if a de-serialized file is compatible with this type.
     */
    private static final long serialVersionUID = 1897496590799378737L;

    private static final String CONTROL = "control"; //$NON-NLS-1$
    private static final String MONITOR = "monitor";  //$NON-NLS-1$

    /**
     * Creates a new instance of <code>ManagementPermission</code> with
     * the given name.
     * @param name the name of the permission. The only acceptable values
     * are the strings &quot;control&quot; or &quot;monitor&quot;.
     * @throws IllegalArgumentException if <code>name</code> is not one of
     * the string values &quot;control&quot; or &quot;monitor&quot;.
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     */
    public ManagementPermission(String name) {
        this(name, null);
    }

    /**
     * Creates a new instance of <code>ManagementPermission</code> with
     * the given name and permitted actions.
     * @param name the name of the permission. The only acceptable values
     * are the strings &quot;control&quot; or &quot;monitor&quot;.
     * @param actions this argument must either be an empty string or
     * <code>null</code>.
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     */
    public ManagementPermission(String name, String actions) {
        super(name, actions);
        if (actions != null && actions.length() != 0) {
            // K0606 = The actions argument must be either null or the empty string.
            throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0606")); //$NON-NLS-1$
        }
        if ((name == null) || (!CONTROL.equals(name) && !MONITOR.equals(name))) {
            // K0607 = Only control or monitor values expected for ManagementPermission name.
            throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0607")); //$NON-NLS-1$
        }
    }
}
