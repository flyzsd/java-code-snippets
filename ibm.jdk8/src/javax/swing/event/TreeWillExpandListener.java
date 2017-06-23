/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1998, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.tree.ExpandVetoException;

/**
  * The listener that's notified when a tree expands or collapses
  * a node.
  * For further information and examples see
  * <a href="https://docs.oracle.com/javase/tutorial/uiswing/events/treewillexpandlistener.html">How to Write a Tree-Will-Expand Listener</a>,
  * a section in <em>The Java Tutorial.</em>
  *
  * @author Scott Violet
  */

public interface TreeWillExpandListener extends EventListener {
    /**
     * Invoked whenever a node in the tree is about to be expanded.
     */
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException;

    /**
     * Invoked whenever a node in the tree is about to be collapsed.
     */
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException;
}
