/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2001, 2005. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute;

/**
 * Thrown to indicate that the requested operation cannot be performed
 * because the set is unmodifiable.
 *
 * @author  Phil Race
 * @since   1.4
 */
public class UnmodifiableSetException extends RuntimeException {
    /**
     * Constructs an UnsupportedOperationException with no detail message.
     */
    public UnmodifiableSetException() {
    }

    /**
     * Constructs an UnmodifiableSetException with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public UnmodifiableSetException(String message) {
        super(message);
    }
}
