/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2005, 2016  All Rights Reserved.
 */

package java.lang.management;

/**
 * The management and monitoring interface for a virtual machine memory manager.
 * Each memory manager is responsible for managing at least one memory pool in
 * the running virtual machine.
 * <p>
 * Multiple instances of this interface are available to clients. Each may be
 * distinguished by their separate <code>ObjectName</code> value.
 * </p>
 * <p>
 * Accessing this kind of <code>MXBean</code> can be done in one of three
 * ways. <br/>
 * <ol>
 * <li>Invoking the static {@link ManagementFactory#getMemoryManagerMXBeans()}
 * method which returns a {@link java.util.List}of all currently instantiated
 * MemoryManagerBeans.</li>
 * <li>Using a {@link javax.management.MBeanServerConnection}.</li>
 * <li>Obtaining a proxy MXBean from the static
 * {@link ManagementFactory#newPlatformMXBeanProxy}method, passing in the
 * string &quot;java.lang:type=MemoryManager,name= <i>unique manager's name
 * </i>&quot; for the value of the second parameter.</li>
 * </ol>
 * </p>
 *
 */
public interface MemoryManagerMXBean extends PlatformManagedObject {

    /**
     * Returns the names of all of the memory pools managed by this
     * <code>MXBean</code>.
     *
     * @return string array containing the names of all of the managed memory
     *         pools.
     */
    public String[] getMemoryPoolNames();

    /**
     * Returns the name of this particular memory manager.
     *
     * @return the name of this memory manager.
     */
    public String getName();

    /**
     * Returns <code>true</code> if this memory manager is still valid in the
     * virtual machine. That is, the memory manager has not been eliminated from
     * the virtual machine memory.
     *
     * @return <code>true</code> if the memory manager is still valid in the
     *         virtual machine ; otherwise <code>false</code>.
     */
    public boolean isValid();
}
