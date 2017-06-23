/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2003, 2003. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.synth;

import sun.swing.DefaultLookup;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * SynthDefaultLookup redirects all lookup calls to the SynthContext.
 *
 * @author Scott Violet
 */
class SynthDefaultLookup extends DefaultLookup {
    public Object getDefault(JComponent c, ComponentUI ui, String key) {
        if (!(ui instanceof SynthUI)) {
            Object value = super.getDefault(c, ui, key);
            return value;
        }
        SynthContext context = ((SynthUI)ui).getContext(c);
        Object value = context.getStyle().get(context, key);
        context.dispose();
        return value;
    }
}
