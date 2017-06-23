/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2012, 2016  All Rights Reserved.
 */

package java.lang.management;

import javax.management.ObjectName;

/**
 * Super-interface of all platform <code>MXBean</code> exported by the Java Virtual
 * Machine. Each PlatformManagedObject instance can be uniquely identified
 * by its object name.
 *
 * @since 1.7
 */
public interface PlatformManagedObject {

	/**
	 * Returns the ObjectName for this PlatformManagedObject.
	 *
	 * @return the ObjectName for this PlatformManagedObject.
	 */
	public ObjectName getObjectName();
}
