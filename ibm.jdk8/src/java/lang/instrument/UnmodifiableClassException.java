/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2004, 2008. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2004, 2008, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.instrument;

/**
 * Thrown by an implementation of
 * {@link java.lang.instrument.Instrumentation#redefineClasses Instrumentation.redefineClasses}
 * when one of the specified classes cannot be modified.
 *
 * @see     java.lang.instrument.Instrumentation#redefineClasses
 * @since   1.5
 */
public class UnmodifiableClassException extends Exception {
    private static final long serialVersionUID = 1716652643585309178L;

    /**
     * Constructs an <code>UnmodifiableClassException</code> with no
     * detail message.
     */
    public
    UnmodifiableClassException() {
        super();
    }

    /**
     * Constructs an <code>UnmodifiableClassException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    public
    UnmodifiableClassException(String s) {
        super(s);
    }
}
