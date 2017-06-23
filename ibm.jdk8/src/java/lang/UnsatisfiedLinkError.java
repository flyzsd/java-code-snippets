/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1994, 2008. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1994, 2008, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * Thrown if the Java Virtual Machine cannot find an appropriate
 * native-language definition of a method declared <code>native</code>.
 *
 * @author unascribed
 * @see     java.lang.Runtime
 * @since   JDK1.0
 */
public
class UnsatisfiedLinkError extends LinkageError {
    private static final long serialVersionUID = -4019343241616879428L;

    /**
     * Constructs an <code>UnsatisfiedLinkError</code> with no detail message.
     */
    public UnsatisfiedLinkError() {
        super();
    }

    /**
     * Constructs an <code>UnsatisfiedLinkError</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    public UnsatisfiedLinkError(String s) {
        super(s);
    }
}
