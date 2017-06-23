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
 * Checked exception thrown when a file system operation fails because a
 * directory is not empty.
 *
 * @since 1.7
 */

public class DirectoryNotEmptyException
    extends FileSystemException
{
    static final long serialVersionUID = 3056667871802779003L;

    /**
     * Constructs an instance of this class.
     *
     * @param   dir
     *          a string identifying the directory or {@code null} if not known
     */
    public DirectoryNotEmptyException(String dir) {
        super(dir);
    }
}
