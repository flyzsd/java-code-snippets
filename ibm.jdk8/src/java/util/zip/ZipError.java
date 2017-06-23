/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2006, 2006. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

package java.util.zip;

/**
 * Signals that an unrecoverable error has occurred.
 *
 * @author  Dave Bristor
 * @since   1.6
 */
public class ZipError extends InternalError {
    private static final long serialVersionUID = 853973422266861979L;

    /**
     * Constructs a ZipError with the given detail message.
     * @param s the {@code String} containing a detail message
     */
    public ZipError(String s) {
        super(s);
    }
}
