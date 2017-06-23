/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1998, 2006. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.MouseAdapter;

/**
 * An empty implementation of the {@code MouseInputListener} interface, provided
 * as a convenience to simplify the task of creating listeners, by extending
 * and implementing only the methods of interest. This class also provides an
 * empty implementation of the {@code MouseWheelListener} interface, through
 * its extension from AWT's {@code MouseAdapter}.
 *
 * @author Philip Milne
 * @author Shannon Hickey
 */
public abstract class MouseInputAdapter extends MouseAdapter
                                        implements MouseInputListener {
}
