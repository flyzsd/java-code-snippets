/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1997, 1999. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1997, 1999, Oracle and/or its affiliates. All rights reserved.
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
 * Defines a listener for menu events.
 *
 * @author Georges Saab
 */
public interface MenuListener extends EventListener {
    /**
     * Invoked when a menu is selected.
     *
     * @param e  a MenuEvent object
     */
    void menuSelected(MenuEvent e);
    /**
     * Invoked when the menu is deselected.
     *
     * @param e  a MenuEvent object
     */
    void menuDeselected(MenuEvent e);
    /**
     * Invoked when the menu is canceled.
     *
     * @param e  a MenuEvent object
     */
    void menuCanceled(MenuEvent e);
}
