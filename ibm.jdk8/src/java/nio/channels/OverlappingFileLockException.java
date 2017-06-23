/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
 * ===========================================================================
 *  Licensed Materials - Property of IBM
 *  "Restricted Materials of IBM"
 *
 *  IBM SDK, Java(tm) Technology Edition, v8
 *  (C) Copyright IBM Corp. 2007, 2012. All Rights Reserved
 *
 *  US Government Users Restricted Rights - Use, duplication or disclosure
 *  restricted by GSA ADP Schedule Contract with IBM Corp.
 * ===========================================================================
 *
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
 *
 */

// -- This file was mechanically generated: Do not edit! -- //

package java.nio.channels;


/**
 * Unchecked exception thrown when an attempt is made to acquire a lock on a
 * region of a file that overlaps a region already locked by the same Java
 * virtual machine, or when another thread is already waiting to lock an
 * overlapping region of the same file.
 *
 * @since 1.4
 */

public class OverlappingFileLockException
    extends IllegalStateException
{

    private static final long serialVersionUID = 2047812138163068433L;

    /**
     * Constructs an instance of this class.
     */
    public OverlappingFileLockException() { }

}
