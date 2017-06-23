/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2012, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.io;

import java.util.Objects;

/**
 * Wraps an {@link IOException} with an unchecked exception.
 *
 * @since   1.8
 */
public class UncheckedIOException extends RuntimeException {
    private static final long serialVersionUID = -8134305061645241065L;

    /**
     * Constructs an instance of this class.
     *
     * @param   message
     *          the detail message, can be null
     * @param   cause
     *          the {@code IOException}
     *
     * @throws  NullPointerException
     *          if the cause is {@code null}
     */
    public UncheckedIOException(String message, IOException cause) {
        super(message, Objects.requireNonNull(cause));
    }

    /**
     * Constructs an instance of this class.
     *
     * @param   cause
     *          the {@code IOException}
     *
     * @throws  NullPointerException
     *          if the cause is {@code null}
     */
    public UncheckedIOException(IOException cause) {
        super(Objects.requireNonNull(cause));
    }

    /**
     * Returns the cause of this exception.
     *
     * @return  the {@code IOException} which is the cause of this exception.
     */
    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }

    /**
     * Called to read the object from a stream.
     *
     * @throws  InvalidObjectException
     *          if the object is invalid or has a cause that is not
     *          an {@code IOException}
     */
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        Throwable cause = super.getCause();
        if (!(cause instanceof IOException))
            throw new InvalidObjectException("Cause must be an IOException");
    }
}
