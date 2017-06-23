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
 * Defines the standard copy options.
 *
 * @since 1.7
 */

public enum StandardCopyOption implements CopyOption {
    /**
     * Replace an existing file if it exists.
     */
    REPLACE_EXISTING,
    /**
     * Copy attributes to the new file.
     */
    COPY_ATTRIBUTES,
    /**
     * Move the file as an atomic file system operation.
     */
    ATOMIC_MOVE;
}
