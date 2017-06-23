/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1997, 2000. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1997, 2000, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.color;

/**
 * This exception is thrown when an error occurs in accessing or
 * processing an ICC_Profile object.
 */

public class ProfileDataException extends java.lang.RuntimeException {

    /**
     *  Constructs a ProfileDataException with the specified detail message.
     *  @param s the specified detail message
     */
    public ProfileDataException(String s) {
        super (s);
    }
}
