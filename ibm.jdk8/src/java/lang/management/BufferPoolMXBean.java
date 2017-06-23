/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2016  All Rights Reserved.
 */

package java.lang.management;

/**
 * The interface for the management buffer pool.
 *
 */

public interface BufferPoolMXBean extends PlatformManagedObject {

    /**
     * Returns the name of the buffer pool.
     *
     */
    String getName();

    /**
     * Returns an number of buffers of the pool.
     *
     */
    long getCount();

    /**
     * Returns amount of the total capacity of the buffers in this pool
     */
    long getTotalCapacity();

    /**
     * Returns the count of used memory
     *
     */
    long getMemoryUsed();
}
