/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2000, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels.spi;

import java.nio.channels.*;


/**
 * Base implementation class for selection keys.
 *
 * <p> This class tracks the validity of the key and implements cancellation.
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class AbstractSelectionKey
    extends SelectionKey
{

    /**
     * Initializes a new instance of this class.
     */
    protected AbstractSelectionKey() { }

    private volatile boolean valid = true;

    public final boolean isValid() {
        return valid;
    }

    void invalidate() {                                 // package-private
        valid = false;
    }
    private final Object cancelLock = new Object();                             //IBM-net_perf
    /**
     * Cancels this key.
     *
     * <p> If this key has not yet been cancelled then it is added to its
     * selector's cancelled-key set while synchronized on that set.  </p>
     */
    public final void cancel() {
        // Synchronizing "this" to prevent this key from getting canceled
        // multiple times by different threads, which might cause race
        // condition between selector's select() and channel's close().
        /* ibm@104278 - In order to make cancel() atomic, the fix for Sun bug   //IBM-net_perf
         * 4744057 synchronizes on "this", which conflicts with our fix for     //IBM-net_perf
         * 60554. We synchronize on something else to avoid deadlocking with    //IBM-net_perf
         * the fix for 60554.                                                   //IBM-net_perf
         */                                                                     //IBM-net_perf
        synchronized (cancelLock) {                                             //IBM-net_perf
            if (valid) {
                valid = false;
                ((AbstractSelector)selector()).cancel(this);
            }
        }
    }
}
//IBM-net_perf
