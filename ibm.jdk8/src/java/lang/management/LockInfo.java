/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2008, 2016  All Rights Reserved.
 */

package java.lang.management;

import javax.management.openmbean.CompositeData;
import com.ibm.lang.management.ManagementUtils;

/**
 * TODO : Type description
 *
 * @since 1.6
 */
public class LockInfo {

    String className;

    int identityHashCode;

    private LockInfo(Object object){
        this.className = object.getClass().getName();
        this.identityHashCode = System.identityHashCode(object);
    }

    /**
     * Creates a new <code>LockInfo</code> instance.
     *
     * @param className
     *            the name (including the package prefix) of the associated lock
     *            object's class
     * @param identityHashCode
     *            the value of the associated lock object's identity hash code.
     *            This amounts to the result of calling
     *            {@link System#identityHashCode(Object)} with the lock object
     *            as the sole argument.
     * @throws NullPointerException
     *             if <code>className</code> is <code>null</code>
     */
    public LockInfo(String className, int identityHashCode) {
        if (className == null) {
            // K0600 = className cannot be null
            throw new NullPointerException(com.ibm.oti.util.Msg.getString("K0600")); //$NON-NLS-1$
        }
        this.className = className;
        this.identityHashCode = identityHashCode;
    }

    /**
     * Returns the name of the lock object's class in fully qualified form (i.e.
     * including the package prefix).
     *
     * @return the associated lock object's class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the value of the associated lock object's identity hash code
     *
     * @return the identity hash code of the lock object
     */
    public int getIdentityHashCode() {
        return identityHashCode;
    }

    /**
     * Returns a {@code LockInfo} object represented by the given
     * {@code CompositeData}. The given {@code CompositeData} must contain the
     * following attributes: <blockquote>
     * <table border summary="The attributes and the types the given CompositeData contains">
     * <tr>
     * <th align=left>Attribute Name</th>
     * <th align=left>Type</th>
     * </tr>
     * <tr>
     * <td>className</td>
     * <td><tt>java.lang.String</tt></td>
     * </tr>
     * <tr>
     * <td>identityHashCode</td>
     * <td><tt>java.lang.Integer</tt></td>
     * </tr>
     * </table>
     * </blockquote>
     *
     * @param compositeData
     *            {@code CompositeData} representing a {@code LockInfo}
     *
     * @throws IllegalArgumentException
     *             if {@code compositeData} does not represent a {@code LockInfo} with the
     *             attributes described above.
     * @return a {@code LockInfo} object represented by {@code compositeData} if {@code compositeData}
     *         is not {@code null}; {@code null} otherwise.
     *
     * @since 1.8
     */
    public static LockInfo from(CompositeData compositeData) {
        if (compositeData == null) {
            return null;
        }
        return ManagementUtils.getLockInfosFromCompositeData(compositeData);
    }

    /**
     * Provides callers with a string value that represents the associated lock.
     * The string will hold both the name of the lock object's class and it's
     * identity hash code expressed as an unsigned hexadecimal. i.e.<br>
     * <p>
     * {@link #getClassName()}&nbsp;+&nbsp;&commat;&nbsp;+&nbsp;Integer.toHexString({@link #getIdentityHashCode()})
     * </p>
     *
     * @return a string containing the key details of the lock
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(className);
        sb.append('@');
        sb.append(Integer.toHexString(identityHashCode));
        return sb.toString();
    }
}
