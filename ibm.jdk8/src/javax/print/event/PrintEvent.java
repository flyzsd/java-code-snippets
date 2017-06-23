/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2000, 2003. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.event;

/**
 *
 * Class PrintEvent is the super class of all Print Service API events.
 */

public class PrintEvent extends java.util.EventObject {

    private static final long serialVersionUID = 2286914924430763847L;

    /**
     * Constructs a PrintEvent object.
     * @param source is the source of the event
     * @throws IllegalArgumentException if <code>source</code> is
     *         <code>null</code>.
     */
    public PrintEvent (Object source) {
        super(source);
    }

    /**
     * @return a message describing the event
     */
    public String toString() {
        return ("PrintEvent on " + getSource().toString());
    }

}
