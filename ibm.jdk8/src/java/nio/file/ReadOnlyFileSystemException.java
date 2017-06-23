/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2007, 2009. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Unchecked exception thrown when an attempt is made to update an object
 * associated with a {@link FileSystem#isReadOnly() read-only} {@code FileSystem}.
 */

public class ReadOnlyFileSystemException
    extends UnsupportedOperationException
{
    static final long serialVersionUID = -6822409595617487197L;

    /**
     * Constructs an instance of this class.
     */
    public ReadOnlyFileSystemException() {
    }
}
