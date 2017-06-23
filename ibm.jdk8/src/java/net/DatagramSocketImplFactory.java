/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1999, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

/**
 * This interface defines a factory for datagram socket implementations. It
 * is used by the classes {@code DatagramSocket} to create actual socket
 * implementations.
 *
 * @author  Yingxian Wang
 * @see     java.net.DatagramSocket
 * @since   1.3
 */
public
interface DatagramSocketImplFactory {
    /**
     * Creates a new {@code DatagramSocketImpl} instance.
     *
     * @return  a new instance of {@code DatagramSocketImpl}.
     * @see     java.net.DatagramSocketImpl
     */
    DatagramSocketImpl createDatagramSocketImpl();
}
