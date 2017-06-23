/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
 * ===========================================================================
 *  Licensed Materials - Property of IBM
 *  "Restricted Materials of IBM"
 *
 *  IBM SDK, Java(tm) Technology Edition, v8
 *  (C) Copyright IBM Corp. 2007, 2012. All Rights Reserved
 *
 *  US Government Users Restricted Rights - Use, duplication or disclosure
 *  restricted by GSA ADP Schedule Contract with IBM Corp.
 * ===========================================================================
 *
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
 *
 */

// -- This file was mechanically generated: Do not edit! -- //

package java.nio.channels;


/**
 * Unchecked exception thrown when the {@link SocketChannel#finishConnect
 * finishConnect} method of a {@link SocketChannel} is invoked without first
 * successfully invoking its {@link SocketChannel#connect connect} method.
 *
 * @since 1.4
 */

public class NoConnectionPendingException
    extends IllegalStateException
{

    private static final long serialVersionUID = -8296561183633134743L;

    /**
     * Constructs an instance of this class.
     */
    public NoConnectionPendingException() { }

}
