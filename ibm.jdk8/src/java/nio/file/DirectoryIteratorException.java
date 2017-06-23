/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2010, 2011. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InvalidObjectException;

/**
 * Runtime exception thrown if an I/O error is encountered when iterating over
 * the entries in a directory. The I/O error is retrieved as an {@link
 * IOException} using the {@link #getCause() getCause()} method.
 *
 * @since 1.7
 * @see DirectoryStream
 */

public final class DirectoryIteratorException
    extends ConcurrentModificationException
{
    private static final long serialVersionUID = -6012699886086212874L;

    /**
     * Constructs an instance of this class.
     *
     * @param   cause
     *          the {@code IOException} that caused the directory iteration
     *          to fail
     *
     * @throws  NullPointerException
     *          if the cause is {@code null}
     */
    public DirectoryIteratorException(IOException cause) {
        super(Objects.requireNonNull(cause));
    }

    /**
     * Returns the cause of this exception.
     *
     * @return  the cause
     */
    @Override
    public IOException getCause() {
        return (IOException)super.getCause();
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
