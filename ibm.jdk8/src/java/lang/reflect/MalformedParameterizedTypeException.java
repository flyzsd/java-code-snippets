/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 2003, 2008. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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


package java.lang.reflect;

/**
 * Thrown when a semantically malformed parameterized type is
 * encountered by a reflective method that needs to instantiate it.
 * For example, if the number of type arguments to a parameterized type
 * is wrong.
 *
 * @since 1.5
 */
public class MalformedParameterizedTypeException extends RuntimeException {
    private static final long serialVersionUID = -5696557788586220964L;
}
