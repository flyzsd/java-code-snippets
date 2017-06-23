/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2005, 2005. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

import java.util.*;

/**
 * Enumeration for RowId life-time values.
 *
 * @since 1.6
 */

public enum RowIdLifetime {

    /**
     * Indicates that this data source does not support the ROWID type.
     */
    ROWID_UNSUPPORTED,

    /**
     * Indicates that the lifetime of a RowId from this data source is indeterminate;
     * but not one of ROWID_VALID_TRANSACTION, ROWID_VALID_SESSION, or,
     * ROWID_VALID_FOREVER.
     */
    ROWID_VALID_OTHER,

    /**
     * Indicates that the lifetime of a RowId from this data source is at least the
     * containing session.
     */
    ROWID_VALID_SESSION,

    /**
     * Indicates that the lifetime of a RowId from this data source is at least the
     * containing transaction.
     */
    ROWID_VALID_TRANSACTION,

    /**
     * Indicates that the lifetime of a RowId from this data source is, effectively,
     * unlimited.
     */
    ROWID_VALID_FOREVER
}
