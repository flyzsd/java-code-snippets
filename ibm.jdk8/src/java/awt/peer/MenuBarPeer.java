/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1995, 1998. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1995, 1998, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.peer;

import java.awt.Menu;
import java.awt.MenuBar;

/**
 * The peer interface for {@link MenuBar}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface MenuBarPeer extends MenuComponentPeer {

    /**
     * Adds a menu to the menu bar.
     *
     * @param m the menu to add
     *
     * @see MenuBar#add(Menu)
     */
    void addMenu(Menu m);

    /**
     * Deletes a menu from the menu bar.
     *
     * @param index the index of the menu to remove
     *
     * @see MenuBar#remove(int)
     */
    void delMenu(int index);

    /**
     * Adds a help menu to the menu bar.
     *
     * @param m the help menu to add
     *
     * @see MenuBar#setHelpMenu(Menu)
     */
    void addHelpMenu(Menu m);
}
