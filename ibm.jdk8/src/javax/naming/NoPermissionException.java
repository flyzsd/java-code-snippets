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

/**
  * This exception is thrown when attempting to perform an operation
  * for which the client has no permission. The access control/permission
  * model is dictated by the directory/naming server.
  *
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */

public class NoPermissionException extends NamingSecurityException {
    /**
     * Constructs a new instance of NoPermissionException using an
     * explanation. All other fields default to null.
     *
     * @param   explanation     Possibly null additional detail about this exception.
     */
    public NoPermissionException(String explanation) {
        super(explanation);
    }

    /**
      * Constructs a new instance of NoPermissionException.
      * All fields are initialized to null.
      */
    public NoPermissionException() {
        super();
    }
    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = 8395332708699751775L;
}
