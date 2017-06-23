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

package javax.swing;

import java.awt.*;
import java.awt.image.*;

/** Color filter for DebugGraphics, used for images only.
  *
  * @author Dave Karlton
  */
class DebugGraphicsFilter extends RGBImageFilter {
    Color color;

    DebugGraphicsFilter(Color c) {
        canFilterIndexColorModel = true;
        color = c;
    }

    public int filterRGB(int x, int y, int rgb) {
        return color.getRGB() | (rgb & 0xFF000000);
    }
}
