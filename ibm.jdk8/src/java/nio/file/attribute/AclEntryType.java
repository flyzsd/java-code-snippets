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

package java.nio.file.attribute;

/**
 * A typesafe enumeration of the access control entry types.
 *
 * @since 1.7
 */

public enum AclEntryType {
    /**
     * Explicitly grants access to a file or directory.
     */
    ALLOW,

    /**
     * Explicitly denies access to a file or directory.
     */
    DENY,

    /**
     * Log, in a system dependent way, the access specified in the
     * permissions component of the ACL entry.
     */
    AUDIT,

    /**
     * Generate an alarm, in a system dependent way, the access specified in the
     * permissions component of the ACL entry.
     */
    ALARM
}
