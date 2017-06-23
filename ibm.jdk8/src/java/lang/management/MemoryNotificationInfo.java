/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2005, 2016  All Rights Reserved.
 */

package java.lang.management;

import javax.management.openmbean.CompositeData;

import com.ibm.lang.management.ManagementUtils;

/**
 * Encapsulates the details of a memory notification emitted by a
 * {@link java.lang.management.MemoryMXBean}when a memory pool exceeds a
 * threshold value.
 * <p>
 * If the memory usage of a memory pool increases such that it reaches or
 * exceeds the memory usage threshold, a {@link #MEMORY_THRESHOLD_EXCEEDED}
 * notification is sent.
 * </p>
 * <p>
 * If, upon the completion of a run of the garbage collector, a memory pool
 * exceeds its collection usage threshold, a
 * {@link #MEMORY_COLLECTION_THRESHOLD_EXCEEDED}notification is sent.
 * </p>
 *
 * @since 1.5
 */
public class MemoryNotificationInfo {

    /**
     * Notification type that signifies a memory pool has grown such that it now
     * reaches or exceeds the usage threshold value set for it.
     */
    public static final String MEMORY_THRESHOLD_EXCEEDED = "java.management.memory.threshold.exceeded"; //$NON-NLS-1$

    /**
     * Notification type that signifies a memory pool's memory usage has grown
     * to reach or exceed the collection usage threshold value set for it after
     * a run of the garbage collector.
     */
    public static final String MEMORY_COLLECTION_THRESHOLD_EXCEEDED = "java.management.memory.collection.threshold.exceeded"; //$NON-NLS-1$

    /**
     * Comment for <code>poolName</code>
     */
    private String poolName;

    /**
     * Comment for <code>usage</code>
     */
    private MemoryUsage usage;

    /**
     * Comment for <code>count</code>
     */
    private long count;

    /**
     * Creates a new <code>MemoryNotificationInfo</code> instance.
     *
     * @param poolName
     *            the name of the memory pool that the notification relates to.
     * @param usage
     *            the memory usage for the named pool
     * @param count
     *            the number of times that the memory usage of the memory pool
     *            has met or exceeded the relevant threshold. For notifications
     *            of the {@link #MEMORY_THRESHOLD_EXCEEDED}type, this will a
     *            count of the number of times the memory usage threshold has
     *            been met or exceeded. For
     *            {@link #MEMORY_COLLECTION_THRESHOLD_EXCEEDED}notifications,
     *            this will be the number of times that the collection usage
     *            threshold was passed.
     */
    public MemoryNotificationInfo(String poolName, MemoryUsage usage, long count) {
        this.poolName = poolName;
        this.usage = usage;
        this.count = count;
    }

    /**
     * Returns the number of times that the memory usage has crossed the
     * threshold relevant to the type of notification when the notification was
     * constructed. For notifications of the {@link #MEMORY_THRESHOLD_EXCEEDED}
     * type, this will a count of the number of times the memory usage threshold
     * has been met or exceeded. For
     * {@link #MEMORY_COLLECTION_THRESHOLD_EXCEEDED}notifications, this will be
     * the number of times that the collection usage threshold was passed.
     *
     * @return the number of times the related memory usage was passed at the
     *         time of the notification construction.
     */
    public long getCount() {
        return this.count;
    }

    /**
     * Returns the name of the memory pool that the notification relates to.
     *
     * @return the name of the associated memory pool.
     */
    public String getPoolName() {
        return this.poolName;
    }

    /**
     * Returns an instance of {@link MemoryUsage}that encapsulates the memory
     * usage of the memory pool that gave rise to this notification at the time
     * the notification was created. The <code>MemoryUsage</code> may be
     * interrogated by the caller to find out the details of the memory usage.
     *
     * @return the memory usage of the related memory pool at the point when
     *         this notification was created.
     */
    public MemoryUsage getUsage() {
        return this.usage;
    }

    /**
     * Receives a {@link CompositeData}representing a
     * <code>MemoryNotificationInfo</code> object and attempts to return
     * the root <code>MemoryNotificationInfo</code> instance.
     *
     * @param cd
     *            a <code>CompositeDate</code> that represents a
     *            <code>MemoryNotificationInfo</code>.
     * @return if <code>cd</code> is non- <code>null</code>, returns a new
     *         instance of <code>MemoryNotificationInfo</code>.
     *         If <code>cd</code> is <code>null</code>, returns <code>null</code>.
     * @throws IllegalArgumentException
     *             if argument <code>cd</code> does not correspond to a
     *             <code>MemoryNotificationInfo</code> with the following
     *             attributes:
     *             <ul>
     *             <li><code>poolName</code>(<code>java.lang.String</code>)
     *             <li><code>usage</code>(
     *             <code>javax.management.openmbean.CompositeData</code>)
     *             <li><code>count</code>(
     *             <code>java.lang.Long</code>)
     *             </ul>
     * <p>
     * The <code>usage</code> attribute must represent a {@link MemoryUsage}
     * instance which encapsulates the memory usage of a memory pool.
     * </p>
     */
    public static MemoryNotificationInfo from(CompositeData cd) {
        MemoryNotificationInfo result = null;

        if (cd != null) {
            // Does cd meet the necessary criteria to create a new
            // MemoryNotificationInfo ? If not then exit on an
            // IllegalArgumentException

            ManagementUtils.verifyFieldNumber(cd, 3);
            String[] attributeNames = { "poolName", "usage", "count" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            ManagementUtils.verifyFieldNames(cd, attributeNames);
            String[] attributeTypes = { "java.lang.String", //$NON-NLS-1$
                    CompositeData.class.getName(), "java.lang.Long" }; //$NON-NLS-1$
            ManagementUtils
                    .verifyFieldTypes(cd, attributeNames, attributeTypes);

            // Extract the values of the attributes and use them to construct
            // a new MemoryNotificationInfo.
            Object[] attributeVals = cd.getAll(attributeNames);
            String poolNameVal = (String) attributeVals[0];
            MemoryUsage usageVal = MemoryUsage
                    .from((CompositeData) attributeVals[1]);
            long countVal = ((Long) attributeVals[2]).longValue();
            result = new MemoryNotificationInfo(poolNameVal, usageVal, countVal);
        }// end if cd is not null
        return result;
    }
}
