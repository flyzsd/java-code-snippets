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
 * Exception which occurs when trying to register an  object in the MBean server that is not a JMX compliant MBean.
 *
 * @since 1.5
 */
public class NotCompliantMBeanException  extends OperationsException {


    /* Serial version */
    private static final long serialVersionUID = 5175579583207963577L;

    /**
     * Default constructor.
     */
    public NotCompliantMBeanException()  {
        super();
    }

    /**
     * Constructor that allows a specific error message to be specified.
     *
     * @param message the detail message.
     */
    public NotCompliantMBeanException(String message)  {
        super(message);
    }

 }
