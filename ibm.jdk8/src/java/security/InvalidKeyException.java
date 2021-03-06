/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1996, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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


package java.security;

/**
 * This is the exception for invalid Keys (invalid encoding, wrong
 * length, uninitialized, etc).
 *
 * @author Benjamin Renaud
 */

public class InvalidKeyException extends KeyException {

    private static final long serialVersionUID = 5698479920593359816L;

    /**
     * Constructs an InvalidKeyException with no detail message. A
     * detail message is a String that describes this particular
     * exception.
     */
    public InvalidKeyException() {
        super();
    }

    /**
     * Constructs an InvalidKeyException with the specified detail
     * message. A detail message is a String that describes this
     * particular exception.
     *
     * @param msg the detail message.
     */
    public InvalidKeyException(String msg) {
        super(msg);
    }

    /**
     * Creates a {@code InvalidKeyException} with the specified
     * detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A {@code null} value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     * @since 1.5
     */
    public InvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a {@code InvalidKeyException} with the specified cause
     * and a detail message of {@code (cause==null ? null : cause.toString())}
     * (which typically contains the class and detail message of
     * {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A {@code null} value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     * @since 1.5
     */
    public InvalidKeyException(Throwable cause) {
        super(cause);
    }
}
