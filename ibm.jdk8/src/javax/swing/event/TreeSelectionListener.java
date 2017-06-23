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

package javax.swing.event;

import java.util.EventListener;

/**
 * The listener that's notified when the selection in a TreeSelectionModel
 * changes.
 * For more information and examples see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/events/treeselectionlistener.html">How to Write a Tree Selection Listener</a>,
 * a section in <em>The Java Tutorial.</em>
 *
 * @see javax.swing.tree.TreeSelectionModel
 * @see javax.swing.JTree
 *
 * @author Scott Violet
 */
public interface TreeSelectionListener extends EventListener
{
    /**
      * Called whenever the value of the selection changes.
      * @param e the event that characterizes the change.
      */
    void valueChanged(TreeSelectionEvent e);
}
