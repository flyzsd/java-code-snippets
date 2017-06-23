/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2000, 2004. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

/**
 * Thrown when code that is dependent on a keyboard, display, or mouse
 * is called in an environment that does not support a keyboard, display,
 * or mouse.
 *
 * @since 1.4
 * @author  Michael Martak
 */
public class HeadlessException extends UnsupportedOperationException {
    /*
     * JDK 1.4 serialVersionUID
     */
    private static final long serialVersionUID = 167183644944358563L;
    public HeadlessException() {}
    public HeadlessException(String msg) {
        super(msg);
    }
    public String getMessage() {
        String superMessage = super.getMessage();
        String headlessMessage = GraphicsEnvironment.getHeadlessMessage();

        if (superMessage == null) {
            return headlessMessage;
        } else if (headlessMessage == null) {
            return superMessage;
        } else {
            return superMessage + headlessMessage;
        }
    }
}
