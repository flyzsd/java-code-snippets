/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1997, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * <p> This interface represents a guard, which is an object that is used
 * to protect access to another object.
 *
 * <p>This interface contains a single method, {@code checkGuard},
 * with a single {@code object} argument. {@code checkGuard} is
 * invoked (by the GuardedObject {@code getObject} method)
 * to determine whether or not to allow access to the object.
 *
 * @see GuardedObject
 *
 * @author Roland Schemers
 * @author Li Gong
 */

public interface Guard {

    /**
     * Determines whether or not to allow access to the guarded object
     * {@code object}. Returns silently if access is allowed.
     * Otherwise, throws a SecurityException.
     *
     * @param object the object being protected by the guard.
     *
     * @exception SecurityException if access is denied.
     *
     */
    void checkGuard(Object object) throws SecurityException;
}
