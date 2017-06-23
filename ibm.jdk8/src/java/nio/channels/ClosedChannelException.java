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
 * Checked exception thrown when an attempt is made to invoke or complete an
 * I/O operation upon channel that is closed, or at least closed to that
 * operation.  That this exception is thrown does not necessarily imply that
 * the channel is completely closed.  A socket channel whose write half has
 * been shut down, for example, may still be open for reading.
 *
 * @since 1.4
 */

public class ClosedChannelException
    extends java.io.IOException
{

    private static final long serialVersionUID = 882777185433553857L;

    /**
     * Constructs an instance of this class.
     */
    public ClosedChannelException() { }

}
