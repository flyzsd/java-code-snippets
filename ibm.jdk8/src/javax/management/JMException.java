/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1999, 2003. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;


/**
 * Exceptions thrown by JMX implementations.
 * It does not include the runtime exceptions.
 *
 * @since 1.5
 */
public class JMException extends java.lang.Exception   {

    /* Serial version */
    private static final long serialVersionUID = 350520924977331825L;

    /**
     * Default constructor.
     */
    public JMException() {
        super();
    }

    /**
     * Constructor that allows a specific error message to be specified.
     *
     * @param msg the detail message.
     */
    public JMException(String msg) {
        super(msg);
    }

}
