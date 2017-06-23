/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2005, 2007. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.SystemTray;
import java.awt.TrayIcon;

/**
 * The peer interface for the {@link TrayIcon}. This doesn't need to be
 * implemented if {@link SystemTray#isSupported()} returns false.
 */
public interface TrayIconPeer {

    /**
     * Disposes the tray icon and releases and resources held by it.
     *
     * @see TrayIcon#removeNotify()
     */
    void dispose();

    /**
     * Sets the tool tip for the tray icon.
     *
     * @param tooltip the tooltip to set
     *
     * @see TrayIcon#setToolTip(String)
     */
    void setToolTip(String tooltip);

    /**
     * Updates the icon image. This is supposed to display the current icon
     * from the TrayIcon component in the actual tray icon.
     *
     * @see TrayIcon#setImage(java.awt.Image)
     * @see TrayIcon#setImageAutoSize(boolean)
     */
    void updateImage();

    /**
     * Displays a message at the tray icon.
     *
     * @param caption the message caption
     * @param text the actual message text
     * @param messageType the message type
     *
     * @see TrayIcon#displayMessage(String, String, java.awt.TrayIcon.MessageType)
     */
    void displayMessage(String caption, String text, String messageType);

    /**
     * Shows the popup menu of this tray icon at the specified position.
     *
     * @param x the X location for the popup menu
     * @param y the Y location for the popup menu
     */
    void showPopupMenu(int x, int y);
}
