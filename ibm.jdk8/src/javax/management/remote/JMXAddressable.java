/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2005, 2005. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package javax.management.remote;

/**
 * <p>Implemented by objects that can have a {@code JMXServiceURL} address.
 * All {@link JMXConnectorServer} objects implement this interface.
 * Depending on the connector implementation, a {@link JMXConnector}
 * object may implement this interface too.  {@code JMXConnector}
 * objects for the RMI Connector are instances of
 * {@link javax.management.remote.rmi.RMIConnector RMIConnector} which
 * implements this interface.</p>
 *
 * <p>An object implementing this interface might not have an address
 * at a given moment.  This is indicated by a null return value from
 * {@link #getAddress()}.</p>
 *
 * @since 1.6
 */
public interface JMXAddressable {
    /**
     * <p>The address of this object.</p>
     *
     * @return the address of this object, or null if it
     * does not have one.
     */
    public JMXServiceURL getAddress();
}
