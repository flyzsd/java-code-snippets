/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2005, 2016  All Rights Reserved.
 */

package java.lang.management;

/**
 * The management and monitoring interface for the virtual machine's class
 * loading functionality.
 * <p>
 * Precisely one instance of this interface will be made
 * available to management clients.
 * </p>
 * <p>
 * Accessing this <code>MXBean</code> can be done in one of three ways. <br/>
 * <ol>
 * <li>Invoking the static ManagementFactory.getClassLoadingMXBean() method.
 * </li>
 * <li>Using a javax.management.MBeanServerConnection.</li>
 * <li>Obtaining a proxy MXBean from the static
 * ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection connection,
 * String mxbeanName, Class <T>mxbeanInterface) method, passing in
 * &quot;java.lang:type=ClassLoading&quot; for the value of the mxbeanName
 * parameter.</li>
 * </ol>
 * </p>
 *
 */
public interface ClassLoadingMXBean extends PlatformManagedObject {

    /**
     * Returns the number of classes currently loaded by the virtual machine.
     * @return the number of loaded classes
     */
    public int getLoadedClassCount();

    /**
     * Returns a figure for the total number of classes that have been
     * loaded by the virtual machine during its lifetime.
     * @return the total number of classes that have been loaded
     */
    public long getTotalLoadedClassCount();

    /**
     * Returns a figure for the total number of classes that have
     * been unloaded by the virtual machine over its lifetime.
     * @return the total number of unloaded classes
     */
    public long getUnloadedClassCount();

    /**
     * Returns a boolean indication of whether the virtual
     * machine's class loading system is producing verbose output.
     * @return true if running in verbose mode
     */
    public boolean isVerbose();

    /**
     * Updates the virtual machine's class loading system to
     * operate in verbose or non-verbose mode.
     * @param value true to put the class loading system into verbose
     * mode, false to take the class loading system out of verbose mode.
     */
    public void setVerbose(boolean value);
}
