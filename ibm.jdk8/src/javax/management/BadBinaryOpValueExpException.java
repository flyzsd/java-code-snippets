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
 * Thrown when an invalid expression is passed to a method for
 * constructing a query.  This exception is used internally by JMX
 * during the evaluation of a query.  User code does not usually see
 * it.
 *
 * @since 1.5
 */
public class BadBinaryOpValueExpException extends Exception   {


    /* Serial version */
    private static final long serialVersionUID = 5068475589449021227L;

    /**
     * @serial the {@link ValueExp} that originated this exception
     */
    private ValueExp exp;


    /**
     * Constructs a <CODE>BadBinaryOpValueExpException</CODE> with the specified <CODE>ValueExp</CODE>.
     *
     * @param exp the expression whose value was inappropriate.
     */
    public BadBinaryOpValueExpException(ValueExp exp) {
        this.exp = exp;
    }


    /**
     * Returns the <CODE>ValueExp</CODE> that originated the exception.
     *
     * @return the problematic {@link ValueExp}.
     */
    public ValueExp getExp()  {
        return exp;
    }

    /**
     * Returns the string representing the object.
     */
    public String toString()  {
        return "BadBinaryOpValueExpException: " + exp;
    }

 }
