/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1995, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Signals that an Abstract Window Toolkit exception has occurred.
 *
 * @author      Arthur van Hoff
 */
public class AWTException extends Exception {

    /*
     * JDK 1.1 serialVersionUID
     */
     private static final long serialVersionUID = -1900414231151323879L;

    /**
     * Constructs an instance of <code>AWTException</code> with the
     * specified detail message. A detail message is an
     * instance of <code>String</code> that describes this particular
     * exception.
     * @param   msg     the detail message
     * @since   JDK1.0
     */
    public AWTException(String msg) {
        super(msg);
    }
}
