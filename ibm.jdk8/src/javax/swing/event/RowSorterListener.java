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
package javax.swing.event;

/**
 * <code>RowSorterListener</code>s are notified of changes to a
 * <code>RowSorter</code>.
 *
 * @see javax.swing.RowSorter
 * @since 1.6
 */
public interface RowSorterListener extends java.util.EventListener {
    /**
     * Notification that the <code>RowSorter</code> has changed.  The event
     * describes the scope of the change.
     *
     * @param e the event, will not be null
     */
    public void sorterChanged(RowSorterEvent e);
}
