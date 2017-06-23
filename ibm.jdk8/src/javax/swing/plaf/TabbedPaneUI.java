/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1997, 1998. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf;

import java.awt.Rectangle;
import javax.swing.JTabbedPane;

/**
 * Pluggable look and feel interface for JTabbedPane.
 *
 * @author Dave Moore
 * @author Amy Fowler
 */
public abstract class TabbedPaneUI extends ComponentUI {
    public abstract int tabForCoordinate(JTabbedPane pane, int x, int y);
    public abstract Rectangle getTabBounds(JTabbedPane pane, int index);
    public abstract int getTabRunCount(JTabbedPane pane);
}
