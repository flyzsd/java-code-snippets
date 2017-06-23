/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1999, 1999. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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


package javax.naming;

import javax.naming.Name;

/**
  * This exception is thrown when a method
  * produces a result that exceeds a size-related limit.
  * This can happen, for example, if the result contains
  * more objects than the user requested, or when the size
  * of the result exceeds some implementation-specific limit.
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @since 1.3
  */
public class SizeLimitExceededException extends LimitExceededException {
    /**
     * Constructs a new instance of SizeLimitExceededException.
     * All fields default to null.
     */
    public SizeLimitExceededException() {
        super();
    }

    /**
     * Constructs a new instance of SizeLimitExceededException using an
     * explanation. All other fields default to null.
     *
     * @param explanation Possibly null detail about this exception.
     */
    public SizeLimitExceededException(String explanation) {
        super(explanation);
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = 7129289564879168579L;
}
