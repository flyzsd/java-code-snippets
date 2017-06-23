/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1999, 2006. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1999, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * Thrown when an attempt is made to apply either of the following: A
 * subquery expression to an MBean or a qualified attribute expression
 * to an MBean of the wrong class.  This exception is used internally
 * by JMX during the evaluation of a query.  User code does not
 * usually see it.
 *
 * @since 1.5
 */
public class InvalidApplicationException extends Exception   {


    /* Serial version */
    private static final long serialVersionUID = -3048022274675537269L;

    /**
     * @serial The object representing the class of the MBean
     */
    private Object val;


    /**
     * Constructs an <CODE>InvalidApplicationException</CODE> with the specified <CODE>Object</CODE>.
     *
     * @param val the detail message of this exception.
     */
    public InvalidApplicationException(Object val) {
        this.val = val;
    }
}
